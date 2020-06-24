pipeline {
    agent any

        stages {

            stage('Build') {
                steps {
                    withMaven(
                        options: [artifactsPublisher(disabled: true)],
                        maven: 'Maven 3.6.3',
                        mavenSettingsConfig: '9ff5ed8e-79e5-4010-b7e2-f137f16176dd') {
                            
                            sh 'mvn -P ci clean package -DskipTests'
                    }
                }
            }

            stage('Unit Tests') {
                steps {
                    withMaven(
                        options: [artifactsPublisher(disabled: true)],
                        maven: 'Maven 3.6.3',
                        mavenSettingsConfig: '9ff5ed8e-79e5-4010-b7e2-f137f16176dd') {
                            
                            sh 'mvn -P ci test'
                    }
                }
            }

           stage('Check style') {
               steps {
                    withMaven(
                        options: [artifactsPublisher(disabled: true)],
                        maven: 'Maven 3.6.3',
                        mavenSettingsConfig: '9ff5ed8e-79e5-4010-b7e2-f137f16176dd') {
                            
                            sh 'mvn -P ci com.coveo:fmt-maven-plugin:check -DskipFormatPlugin=false'
                    }
                }
            }

            stage ('Deploy') {
                when { 
                    anyOf { 
                           branch 'master'; 
                           branch 'develop' } 
                    }

                 steps {
                    withMaven(
                        options: [artifactsPublisher(disabled: true)],
                        maven: 'Maven 3.6.3',
                        mavenSettingsConfig: '9ff5ed8e-79e5-4010-b7e2-f137f16176dd') {
                            
                            sh 'mvn deploy -P ci -DskipTests'
                    }
                }
            }
        }
}