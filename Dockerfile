FROM alpine/java:21-jre
COPY target/runelite-version-checker-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT java -jar app.jar