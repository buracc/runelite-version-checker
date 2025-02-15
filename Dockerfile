FROM alpine/java:21-jre
RUN apk add --no-cache openrc busybox-extras && \
    apk add --no-cache cron
COPY target/runelite-version-checker-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT java -jar app.jar