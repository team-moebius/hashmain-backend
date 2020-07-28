# Build
FROM openjdk:8-jdk-alpine as build
COPY /build.gradle settings.gradle gradlew ./
COPY /gradle gradle
COPY backend backend
COPY /app app
RUN chmod +x ./gradlew
RUN ./gradlew clean build

# Run
FROM openjdk:8-jre-alpine
ENV ARTIFACT_PATH=app/build/libs/app-0.0.1-RELEASE.jar
ENV JAVA_OPTS="-Xmx2048m –Xms2048m -XX:MaxNewSize=768m -XX:MaxPermSize=256m"
COPY --from=build $ARTIFACT_PATH app.jar
ENTRYPOINT ["java","-Dspring.profiles.active=production","-Dreactor.netty.http.server.accessLogEnabled=true","-jar","app.jar"]
