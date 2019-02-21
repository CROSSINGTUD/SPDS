pipeline {
    agent {
        docker {
            image 'maven:3-alpine'
            args '-v $HOME/.m2:/root/.m2'
        }
    }
    stages {

        stage('Build') {
            steps {
                sh 'mvn clean compile'
            }
        }


		stage('Deploy'){
		    when {
			    branch 'jenkins'
			}
	        steps {
				configFileProvider(
	        		[configFile(fileId: 'MyGlobalSettings', variable: 'MAVEN_SETTINGS')]) {
	      		  		sh 'mvn -s $MAVEN_SETTINGS clean package'
				}
	        }
		}

    }
}