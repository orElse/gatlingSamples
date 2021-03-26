# Gatling Quick Start Samples

This is a sample project to illustrate some basic [gatling](https://gatling.io/) features. 
It uses [sbt](https://www.scala-sbt.org/) as build tool.

Most examples should include links to relevant documentation sections.

## Samples

* s1.MinimalSetupSimulation
  * just the basic setup of a simulation
* s2.ExtractorsValidationAndSessionSimulation
  * checking server result
  * access session values  
* s3.ErrorHandlingAndControlFlowSimulation
  * control flows and looping
* s4.FeederSimulation
  * feeders for injection multiple user data
* z.OriginalSample
  * original gatling sample relying on gatling test service
  * work with html response different checks (liks css)

## Execution

Most samples will start a small akka http server locally for testing on port 8080.

It is possible to run single Simulations by e.g:

starting sbt
```
sbt
```
and execute within the session and execute:
```
sbt:gatlingSamples> gatling:testOnly s1.*
```

or directly from the shell:
```
sbt "gatling:testOnly s1.*"
```

## Test Results

each simulation will create a result in the shell prompt and also generate a report file which can be opened in the browser
```
Parsing log file(s) done
Generating reports...

================================================================================
---- Global Information --------------------------------------------------------
> request count                                          1 (OK=1      KO=0     )
> min response time                                    267 (OK=267    KO=-     )
> max response time                                    267 (OK=267    KO=-     )
> mean response time                                   267 (OK=267    KO=-     )
> std deviation                                          0 (OK=0      KO=-     )
> response time 50th percentile                        267 (OK=267    KO=-     )
> response time 75th percentile                        267 (OK=267    KO=-     )
> response time 95th percentile                        267 (OK=267    KO=-     )
> response time 99th percentile                        267 (OK=267    KO=-     )
> mean requests/sec                                      1 (OK=1      KO=-     )
---- Response Time Distribution ------------------------------------------------
> t < 800 ms                                             1 (100%)
> 800 ms < t < 1200 ms                                   0 (  0%)
> t > 1200 ms                                            0 (  0%)
> failed                                                 0 (  0%)
================================================================================
Reports generated in 0s.
Please open the following file: /....../gatlingSamples/target/gatling/minimalsetupsimulation-20210326145053601/index.html
Global: count of failed events is 0.0 : true
[info] Simulation MinimalSetupSimulation successful.
[info] Simulation(s) execution ended.
[success] Total time: 8 s, completed Mar 26, 2021 3:50:55 PM

```