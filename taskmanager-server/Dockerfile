FROM openjdk:8
EXPOSE 8085
ADD taskmanager-server-0.0.1-SNAPSHOT.jar taskmanager-server-0.0.1-SNAPSHOT.jar
ENTRYPOINT ["java", "-jar", "taskmanager-server-0.0.1-SNAPSHOT.jar"]
RUN sh -c 'touch /taskmanager-server-0.0.1-SNAPSHOT.jar'