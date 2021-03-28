package s5

import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import sample.MinimalServer

import scala.concurrent.Await


class RedirectsAndEtagSimulation extends Simulation {

  val minimalServer = new MinimalServer

  // basic setup used for all requests unless overwritten
  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .disableWarmUp

  val httpProtocolNotCached = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .disableWarmUp
    .disableCaching

  // the scenario - different protocols can be mixed
  val testScenario =
    scenario("Redirection&ETag")
      .group("Redirect") {
        exec(
          http("getItems - stop at redirect")
            .get("/test/redirect/items")
            .disableFollowRedirect
            .check(
              status is 308
            )
        )
          .exec(
            // by default will follow the redirect
            http("getItems - follow redirects")
              .get("/test/redirect/items")
              .check(
                status is 200,
                // https://gatling.io/docs/current/http/http_check/?highlight=redirect#page-location
                currentLocation.saveAs("redirectedTo")
              )
          )
      }
      .group("ETag") {
        exec(
          http("addItem")
            .post("/test/items")
            .body(StringBody("""{ "name": "add-me" }""")).asJson
            .check(
              status is 201,
              jsonPath("$.id").saveAs("newItemId")
            )
        )
          .exec(
            http("getItem - first")
              .get("/test/items/${newItemId}")
              .check(
                status is 200,
                jsonPath("$.name").is("add-me")
              )
          )
          .exec(
            http("getItem - cached")
              .get("/test/items/${newItemId}")
              .check(
                status is 304,
                // not evaluated since we get a not modified
                jsonPath("$.name").is("bogus")
              )
          )
          .exec(flushHttpCache) // will flush content & redirect knowledge from the cache
          .exec(
            http("getItem - disable caching")
              .get("/test/items/${newItemId}")
              // .disableCaching -> only works on the protocol
              .check(
                status is 200,
                // not evaluated since we get a not modified
                jsonPath("$.name").not("bogus")
              )
          )
      }

  // setup code
  before {
    Await.result(minimalServer.run(), 5.seconds)
    println("Simulation is about to start!")
  }

  // teardown code
  after {
    minimalServer.shutdown
    println("Simulation is finished!")
  }

  // test setup - since we only want a contract test we use 1
  setUp(
    testScenario.inject(atOnceUsers(1)).protocols(httpProtocol)
  ).assertions(
    global.failedRequests.count.is(0)
  )

}
