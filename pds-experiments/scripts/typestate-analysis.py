#!/usr/bin/python

import sys, os, subprocess
from subprocess import call

PROJECTS = "projects.txt"
TYPESTATES = "typestate.txt"
POINTS_TO_JAR = "../build/pds-experiments-0.0.1-SNAPSHOT-jar-with-dependencies.jar"
MAIN_CLASS = "dacapo.FinkOrIDEALDacapoRunner"
TIMEOUT_IN_MINUTES = 1*60 #1 hour per project
ALIASING = "true"
STRONG_UPDATES = "true"

MAX_MEMORY = sys.argv[1]

# Read in the file
with open(PROJECTS, 'r') as file:
	projects = file.readlines()

with open(TYPESTATES, 'r') as file:
	typestates = file.readlines()
 
args = []
for project in projects:
	if project.startswith("#"): 
		continue
	project = project.strip()
	for typestate in typestates:
		if typestate.startswith("#"): 
			continue
		typestate = typestate.strip()
		cmd = ["java", "-Xss128m","-Xms"+MAX_MEMORY+"g", "-Xmx"+MAX_MEMORY+"g","-cp",os.path.abspath(POINTS_TO_JAR), MAIN_CLASS, "ideal", typestate, os.path.abspath("../dacapo")+"/",project,"run",ALIASING,STRONG_UPDATES]
		f = open("log-"+project, "w")
		call(cmd, stdout=f,stderr=subprocess.STDOUT, timeout=TIMEOUT_IN_MINUTES*60)

		

