# Use an official OpenJDK runtime as a parent image
FROM openjdk:17-jdk-alpine

EXPOSE 8080

WORKDIR /app

COPY target/PlaceAndPlay.jar /app/PlaceAndPlay.jar

ENTRYPOINT ["java", "-Xms512m", "-Xmx1024m", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=200", "-jar", "PlaceAndPlay.jar"]