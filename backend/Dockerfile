# Build
FROM openjdk:8-jdk-alpine as build
COPY /build.gradle settings.gradle gradlew ./
COPY /gradle gradle
COPY /backend backend
COPY /app app
RUN chmod +x ./gradlew
RUN ./gradlew clean build

# Run
FROM openjdk:8-jre-alpine

ENV ARTIFACT_PATH=app/build/libs/app-0.0.1-RELEASE.jar
COPY --from=build $ARTIFACT_PATH app.jar

ENV GC_OPTS="-Xmx2048m –Xms2048m -XX:MaxMetaspaceSize=512m -XX:+UseG1GC -XX:+DisableExplicitGC -XX:+UseStringDeduplication"
ENV JAVA_OPTS="$GC_OPTS -Dspring.profiles.active=production -Dreactor.netty.http.server.accessLogEnabled=true"
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar", "-jar", "app.jar"]
