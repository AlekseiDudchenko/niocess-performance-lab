FROM eclipse-temurin:21-jdk AS build

WORKDIR /app
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY libs ./libs
COPY src ./src
RUN --mount=type=cache,target=/root/.gradle ./gradlew -q -x test bootJar

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /app/build/libs/client-app-0.1.0.jar app.jar

ENV SERVER_PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
