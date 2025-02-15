FROM openjdk:21
COPY target/runelite-version-checker-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT java -jar app.jar