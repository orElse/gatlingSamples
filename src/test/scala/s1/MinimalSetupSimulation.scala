package s1

import scala.concurrent.duration._
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import sample.MinimalServer

import scala.concurrent.Await

class MinimalSetupSimulation extends Simulation {

  val minimalServer = new MinimalServer

  // basic setup used for all requests unless overwritten
  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .disableWarmUp

  // the scenario - different protocols can be mixed
  val testScenario =
    scenario("Simple Setup Scenario")
      .exec(
        http("getItems").get("/test/items")
        // checks https://gatling.io/docs/3.4/http/http_check/?highlight=check#concepts
        .check(
          status is 200,
        )
      )

  // https://gatling.io/docs/3.4/general/simulation_structure/#hooks
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
  // https://gatling.io/docs/3.4/general/simulation_setup/
  setUp(
    testScenario.inject(atOnceUsers(1)).protocols(httpProtocol)
  ).assertions(
    // https://gatling.io/docs/3.4/general/assertions/
    global.failedRequests.count.is(0)
  )

}
