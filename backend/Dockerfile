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
ENTRYPOINT ["java","-Dfile.encoding=UTF-8","-Dspring.profiles.active=develop","-Dreactor.netty.http.server.accessLogEnabled=true","-jar","app.jar"]

# GC & Heap size option (changable)
CMD ["-Xmx2048m","–Xms2048m","-XX:MaxMetaspaceSize=512m","-XX:+UseG1GC","-XX:+DisableExplicitGC","-XX:+UseStringDeduplication"]