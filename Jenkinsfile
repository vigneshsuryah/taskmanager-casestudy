pipeline {
    agent any
    stages {
        stage('SCM Checkout') {
	    git 'https://github.com/vigneshsuryah/taskmanager-casestudy.git'
        }
	stage('Compile-Package') {
	    sh 'mvn package'
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
