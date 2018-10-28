pipeline {
    agent any
    stages {
    stage('SCM Checkout') {
		steps {
			git 'https://github.com/vigneshsuryah/taskmanager-casestudy.git'
		}
    }
	stage('Compile-Package') {
		steps {
			def mvnHome = tool name: 'maven', type: 'maven'
			sh "${mvnHome}/bin/mvn package'
		}
        }
        stage('Test') {
            steps {
                echo 'Testing..'
            }
        }
        stage('Deploy') {
            steps {
                echo 'Deploying....'
            }
        }
    }
}
