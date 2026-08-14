# syntax=docker/dockerfile:1

# ------------------------------------------------------------------
# Builds the :spring-playground Spring Boot module as a runnable container.
#
# Stage 1: build the fat jar using Gradle + JDK 25 (matches the repo's
#          toolchain). Build context must be the repo ROOT so that the
#          wrapper, root build files and the module sources are present
#          (same convention as mock-server/Dockerfile).
# Stage 2: ship only the self-contained jar on a JRE image.
# ------------------------------------------------------------------

FROM gradle:9.5.1-jdk25 AS build
WORKDIR /workspace
COPY . .
RUN gradle --no-daemon :spring-playground:bootJar -x test

# ------------------------------------------------------------------
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/spring-playground/build/libs/spring-playground-*.jar app.jar

ENV SERVER_PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
