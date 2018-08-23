
# WPDS

[![Build Status](http://soot-build.cs.uni-paderborn.de/jenkins/job/boomerang/job/WPDS/badge/icon)](http://soot-build.cs.uni-paderborn.de/jenkins/job/boomerang/job/WPDS-Snapshot/)

This repository contains a Java implementation of Weighted Pushdown Systems.
Additionally, it contains an implementation of [Boomerang](boomerangPDS) and [IDEal](idealPDS) based on a Weighted Pushdown System.

# Checkout, Build and Install

To build and install WPDS into you local repository, run 

``mvn clean install -DskipTests``

in the root directory of this git repository. If you do not want to skip the test cases, remove the last flag.

# Examples

Boomerang code examples can be found [here](https://github.com/CROSSINGTUD/WPDS/blob/master/boomerangPDS/src/main/java/boomerang/example/ExampleMain.java). Code examples for IDEal are given [here](https://github.com/CROSSINGTUD/WPDS/tree/master/idealPDS/src/main/java/inference/example).

# Visualization

It is possible to visualize the results of a Boomerang or IDEal analysis interactively within the browser as shown below.

![Visualization](https://github.com/CROSSINGTUD/WPDS/blob/master/boomerangPDS/visualization/example2.png)

The visualization is readily setup on the test cases, but disabled by default. To explore its functionality do:

1. Enable the [visualization flag](https://github.com/CROSSINGTUD/WPDS/blob/6ce1e84a9736d59b077478f3f17227d461ba3a51/boomerangPDS/src/test/java/test/core/AbstractBoomerangTest.java#L82) and execute some test cases. 
2. Find the `.json` file for the executed test method below the folder `target/IDEViz/<fullyQualifiedClassNameOfTest>/`. 
3. Open the [index.html](https://github.com/CROSSINGTUD/WPDS/tree/master/boomerangPDS/visualization) in a browser (tested with Chrome).
4. Drop any of the .json files in the lower right box "Drop IDEViz File here" and start browsing the exploded supergraph interactively.


# Notes on the Test Cases

The projects Boomerang and IDEal contain JUnit test suites. As for JUnit, the test methods are annotated with @Test and can be run as normal JUnit tests.
However, these methods are *not* executed but only statically analyzed. When one executes the JUnit tests, the test method bodies are supplied as input to Soot 
and a static analysis is triggered. All this happens in JUnit's @Before test time. The test method itself is never run, may throw NullPointerExceptions or may not even terminate.

If the static analysis succeeded, JUnit will officially label the test method as skipped. However, the test will not be labeled as Error or Failure. 
Even though the test was skipped, it succeeded. Note, JUnit outputs a message:

``org.junit.AssumptionViolatedException: got: <false>, expected: is <true>``

This is ok! The test passed!
