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
	        		[configFile(fileId: 'd8345989-7f12-4d8f-ae12-0fe9ce025188', variable: 'MAVEN_SETTINGS')]) {
	      		  		sh 'mvn -s $MAVEN_SETTINGS clean deploy -DskipTests'
				}
	        }
		}

    }
}