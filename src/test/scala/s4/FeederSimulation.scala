package s4

import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import sample.MinimalServer

import scala.concurrent.Await

class FeederSimulation extends Simulation {

  val minimalServer = new MinimalServer

  // basic setup used for all requests unless overwritten
  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .disableWarmUp

  // feeder https://gatling.io/docs/current/session/feeder/#feeder & https://gatling.io/docs/current/advanced_tutorial/?highlight=feeder
  // many types like json,csv,jdbc (some subtypes are exclusive to frontline)
  val feeder = Array(
    Map("markerId" -> "i1", "name" -> "foo1", "execMe" -> "true"),
    Map("markerId" -> "i2", "name" -> "foo2", "execMe" -> "false"),
    Map("markerId" -> "i3", "name" -> "foo3", "execMe" -> "true")
  ).circular


  // the scenario
  val testScenario =
    scenario("Feeder")
      // puts values on the session - has to be compatible with users in setup
      .feed(feeder)
      .group("Feature for user ${markerId}") {
        exec(http("initializationQuery").get("/test/items").check(status is 200)).exitHereIfFailed
          .doIf("${execMe}") {
            exec(
              http("addItem ${markerId}")
                .post("/test/items")
                .body(StringBody("""{ "name": "${name}" }""")).asJson
                .check(
                  status is 201,
                  jsonPath("$..id").saveAs("newItemId")
                )
            )
          }
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

  // use 3 users since we have 3 items in the dataset
  setUp(
    testScenario.inject(atOnceUsers(3)).protocols(httpProtocol)
  ).assertions(
    global.failedRequests.count.is(0)
  )

}
