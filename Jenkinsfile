pipeline {
    agent any

    tools {
         jdk 'Oracle JDK 8'
    }

    stages {

        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }
        stage('Test') {
            steps {
                sh 'mvn clean test  -P !defaultTestSet'
            }
	    post {  
    		always {
                		junit 'shippable/testresults/**/*.xml'
            	}
    	    }
        }

		stage('Deploy'){
		    when { 
		    	anyOf { branch 'master'; branch 'on-demand-icfg' } 
			}
	        steps {
				configFileProvider(
	        		[configFile(fileId: 'd8345989-7f12-4d8f-ae12-0fe9ce025188', variable: 'MAVEN_SETTINGS')]) {
	      		  		sh 'mvn -s $MAVEN_SETTINGS clean deploy -DskipTests'
				}
	        }
		}

    }
}