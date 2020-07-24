# WALA Bindings for Boomerang

This folder contains experimental bindings that allow Boomerang to be used with WALA. 

# Example Usage

A minimal example on how the setup for an analysis with WALA may look like is provided in src/test/java/example/ExampleMain1.java 
which will execute an analysis on src/test/java/example/BoomerangExampleTarget1.java

Note: Prior usages, you need to 
 
A) compile the test classes of this project
B) Set the path in 

src/test/resources/testScope.txt

i.e. specify <ADD_ABSOLUTE_PATH_PREFIX>/target/test-classes to point to the location of the compiled classes. 