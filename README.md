# taskmanager-casestudy
Build and package spring boot and angular6 into a jar file

taskmanager-server - contains the REST end-points for application
taskmanager-web - contains the angular 6 cli code for application

Commands for final build:

Maven build: clean install -e	[The UI code is build using "frontend-maven-plugin" and is packed inside the JAR artifacts itself. Refer taskmanager-web pom.xml]
Docker build: package docker:build	[spotify "docker-maven-plugin" is used to create image in the remote docker. <dockerHost> configuration : pom.xml of taskmanager-server]

Commands for Docker execution:

dockerx run -p 8090:8085 vs-taskmanager-app-image:latest	[to start the image, we generated]
dockerx exec -it <container id> sh	[to check the below URL's are working in the curl]
	
	To check whether application loaded correctly: curl http://localhost:8090/hometest
	Service: curl http://localhost:8090/api/tasks
	UI Home page: curl http://localhost:8090/index.html
	
The above URLs can be accessed in the host machine with the Port Forwarding settings of VM toolbox [Settings -> Network -> Port Forwarding -> HOSTIP/HOSTPORT 127.0.0.1:8090 to GUESTIP/GUESTPORT :8090]

Commands for local development:

spring boot in taskmanager-server folder: spring-boot:run
angular ui in web folder of taskmanager-web: npm install -> npm start

Note: 
Have used free hosting public MySQL database jdbc:mysql://sql12.freemysqlhosting.net:3306/dbname. This is to access database layer when deployed in docker (instead of setting mysql in docker).
Refer dockerfile, application screenshot document in the repo folder.
Refer mysql-script-to-execute.txt for the database DDL and DML's
Before the maven build, change the port number in envioronment.prod.ts to change the port number in which the jar is gonna get deployed.


