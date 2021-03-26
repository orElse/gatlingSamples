package s3

import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import sample.MinimalServer

import scala.concurrent.Await

class ErrorHandlingAndControlFlowSimulation extends Simulation {

  val minimalServer = new MinimalServer

  // basic setup used for all requests unless overwritten
  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")

  // the scenario
  val items = Array(
    Map("markerId" -> "i1", "name" -> "foo1", "execMe" -> "true"),
    Map("markerId" -> "i2", "name" -> "foo2", "execMe" -> "false"),
    Map("markerId" -> "i3", "name" -> "foo3", "execMe" -> "true")
  ).readRecords


  val testScenario =
    scenario("Error Handling & Control Flow")
      // https://gatling.io/docs/current/general/scenario/#errors-handling
      // stop the scenario if we cannot initialize
      .exec(http("initializationQuery").get("/test/items").check(status is 200)).exitHereIfFailed
      .group("Feature 1") {
        // will stop execution if the block on the first error
        exitBlockOnFail {
          exec(http("initializationQueryInBlock").get("/test/items").check(status is 200))
        }
      }
      .group("Feature 2") {
        // https://gatling.io/docs/current/general/scenario/#loop-statements
        foreach(items, "item", "itemCounter") {
          // https://gatling.io/docs/3.4/general/scenario/?highlight=doif#conditional-statements
          doIf("${item.execMe}") {
            exec(
              http("addItem ${item.markerId}")
                .post("/test/items")
                .body(StringBody("""{ "name": "${item.name}" }""")).asJson
                .check(status is 201)
            )
          }
        }
          .exec(
            http("validate added items")
              .get("/test/items")
              .check(
                status is 200,
                jsonPath("$.items[*]").count.is("2")
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
    testScenario.inject(atOnceUsers(1))
      .protocols(httpProtocol)
  ).assertions(
    // global.responseTime.max.lt(50),
    // global.successfulRequests.percent.gt(95)
    global.failedRequests.count.is(0)
  )

}
