package s2

import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import sample.MinimalServer

import scala.concurrent.Await

class ExtractorsValidationAndSessionSimulation extends Simulation {

  val minimalServer = new MinimalServer

  // basic setup used for all requests unless overwritten
  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .disableWarmUp

  // the scenario
  val testScenario =
    scenario("Extractors&Validations")
      .group("Adding Items") {
        exec(
          http("addItem")
            .post("/test/items")
            .body(StringBody("""{ "name": "add-me" }""")).asJson
            // checks https://gatling.io/docs/3.4/http/http_check/?highlight=check#concepts
            .check(
              // response code check
              status is 201,
              // checks on response body: e.g. jsonPath, regex, substring, ...
              jsonPath("$.name").is("add-me"),
              jsonPath("$.id").exists,
              jsonPath("$.id").saveAs("newItemId"),
              // can extract values by type and also perform transformations on it
              jsonPath("$.id").ofType[String].transform(idString => idString.reverse).saveAs("revertedItemId")
            )
        )
          // sessions https://gatling.io/docs/3.4/session/
          .exec { session =>
            // print the session - it should contain newItemId
            println(
              s"""|<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
                  |<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
                  |
                  |$session
                  |
                  |<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
                  |<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
                  |""".stripMargin)
            session
          }
          .exec(
            http("getItem")
              .get("/test/items/${newItemId}")
              .check(
                status is 200,
                jsonPath("$.name").is("add-me")
              )
          )
          .exec(
            http("getItems")
              .get("/test/items")
              .check(
                status is 200,
                jsonPath("$.items[?(@.id=='${newItemId}')].name").is("add-me")
              )
          )
          // manually add values to the session https://gatling.io/docs/3.4/session/
          .exec(_.set("doesNotExistId", "bogus"))
          .exec(
            httpGetItem("${doesNotExistId}")
              .check(
                status is 404
              )
          )
      }


  def httpGetItem(itemId: String) = http("getItem").get(s"/test/items/${itemId}")


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
