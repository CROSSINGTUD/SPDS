#!/usr/bin/python

import sys, os, subprocess
from subprocess import call

PROJECTS = "projects.txt"
POINTS_TO_JAR = "../build/pds-experiments-0.0.1-SNAPSHOT-jar-with-dependencies.jar"
MAIN_CLASS = "dacapo.WholeProgramPointsToAnalysis"
TIMEOUT_IN_MINUTES = 12*60 #12 hours per project

MAX_MEMORY = sys.argv[1]

# Read in the file
with open(PROJECTS, 'r') as file:
	lines = file.readlines()
 
args = []
for line in lines:
	if line.startswith("#"): 
		continue
	line = line.strip()
	cmd = ["java", "-Xss128m","-Xms"+MAX_MEMORY+"g", "-Xmx"+MAX_MEMORY+"g","-cp",os.path.abspath(POINTS_TO_JAR), MAIN_CLASS, os.path.abspath("../dacapo")+"/",line]
	f = open("log-"+line, "w")
	call(cmd, stdout=f,stderr=subprocess.STDOUT, timeout=TIMEOUT_IN_MINUTES*60)

		

