node {
    stage('SCM Checkout') {
			git 'https://github.com/vigneshsuryah/taskmanager-casestudy.git'
    }
	stage('Compile-Package') {
		def mvnHome = tool name: 'maven', type: 'maven'
		sh "${mvnHome}/bin/mvn clean install"
    }
}
