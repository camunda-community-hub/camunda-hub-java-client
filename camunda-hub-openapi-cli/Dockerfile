FROM openjdk:21
# install camunda hub cli to a nice location
WORKDIR /opt
RUN mkdir camunda-hub-cli
WORKDIR /opt/camunda-hub-cli
ADD target/camunda-hub-openapi-cli-1.0.1-SNAPSHOT-jar-with-dependencies.jar camunda-hub-cli.jar
# set the jar as entrypoint
ENTRYPOINT ["java", "-jar", "/opt/camunda-hub-cli/camunda-hub-cli.jar"]