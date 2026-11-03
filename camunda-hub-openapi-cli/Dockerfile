FROM eclipse-temurin:25-jre
# install camunda hub cli to a nice location
WORKDIR /opt
RUN mkdir camunda-hub-cli
WORKDIR /opt/camunda-hub-cli
ADD target/*-jar-with-dependencies.jar camunda-hub-cli.jar
# set the jar as entrypoint
ENTRYPOINT ["java", "-jar", "/opt/camunda-hub-cli/camunda-hub-cli.jar"]
