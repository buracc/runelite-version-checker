FROM alpine/java:21-jre
RUN echo java -jar /app.jar > /etc/periodic/15min/run
RUN chmod +x /etc/periodic/15min/run
RUN run-parts --test /etc/periodic/15min
COPY target/runelite-version-checker-1.0-SNAPSHOT.jar app.jar
ENTRYPOINT /bin/sh -c crond -f