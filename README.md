# Changes between 2.x and 3.0-ALPHA

This is an alpha release branch for version 3.0. At the current state, IDEal is excluded from this branch.
If you want to use IDEal, for now, stick to version 2.x.

* Performance Optimization. Fixed a lapsed listener problem
* Added strategies for data-flows of static fields
* Added options to control depth (k-limit) of Field-PDS and Call-PDS. The default option, -1, means k = \infty. (under-approximaton. Analysis turns unsound afterwards).
* *API change:* Introduced the wrapper class `boomerang.callgraph.SootCallGraph`. Pass `new SootCallGraph()` to Boomerang (and do not use `ObservableDynamicICFG` or `ObservableStaticICFG` anymore).
* *API change:* Added interface `boomerang.DataFlowScope` which can be used to exclude methods during the data-flow analysis (but shall not be excluded during call graph construction from Soot)
* Other minor internal refactoring 


# WPDS

[![Build Status](https://soot-build.cs.uni-paderborn.de/jenkins/buildStatus/icon?job=boomerang%2FWPDS-Multibranch%2Fmaster)](https://soot-build.cs.uni-paderborn.de/jenkins/job/boomerang/job/WPDS-Multibranch/job/master/)

This repository contains a Java implementation of Weighted Pushdown Systems.
Additionally, it contains an implementation of [Boomerang](boomerangPDS) and [IDEal](idealPDS) based on a Weighted Pushdown System.

# Checkout, Build and Install

To build and install WPDS into your local repository, run 

``mvn clean install -DskipTests``

in the root directory of this git repository. If you do not want to skip the test cases, remove the last flag.

# Examples

Boomerang code examples can be found [here](https://github.com/CROSSINGTUD/WPDS/blob/master/boomerangPDS/src/main/java/boomerang/example/ExampleMain.java). Code examples for IDEal are given [here](https://github.com/CROSSINGTUD/WPDS/tree/master/idealPDS/src/main/java/inference/example).


# Notes on the Test Cases

The projects Boomerang and IDEal contain JUnit test suites. As for JUnit, the test methods are annotated with @Test and can be run as normal JUnit tests.
However, these methods are *not* executed but only statically analyzed. When one executes the JUnit tests, the test method bodies are supplied as input to Soot 
and a static analysis is triggered. All this happens in JUnit's @Before test time. The test method itself is never run, may throw NullPointerExceptions or may not even terminate.

If the static analysis succeeded, JUnit will officially label the test method as skipped. However, the test will not be labeled as Error or Failure. 
Even though the test was skipped, it succeeded. Note, JUnit outputs a message:

``org.junit.AssumptionViolatedException: got: <false>, expected: is <true>``

This is ok! The test passed!
