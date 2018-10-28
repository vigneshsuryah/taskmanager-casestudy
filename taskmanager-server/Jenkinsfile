node {
    stage('SCM Checkout') {
			git 'https://github.com/vigneshsuryah/taskmanager-casestudy.git'
    }
	stage('Compile-Package') {
		def mvnHome = tool name: 'maven', type: 'maven'
		sh 'cd taskmanager-server'
		sh "${mvnHome}/bin/mvn clean install"
    }
}
