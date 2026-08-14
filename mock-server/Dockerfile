# syntax=docker/dockerfile:1

# ------------------------------------------------------------------
# Builds the :mock-server Spring Boot module as a runnable container.
#
# Stage 1: build the fat jar using Gradle + JDK 25 (matches the repo's
#          toolchain). Build context must be the repo ROOT so that the
#          wrapper, root build files and the module sources are present.
# Stage 2: ship only the self-contained jar on a JRE image.
# ------------------------------------------------------------------

FROM gradle:9.5.1-jdk25 AS build
WORKDIR /workspace
COPY . .
RUN gradle --no-daemon :mock-server:bootJar -x test

# ------------------------------------------------------------------
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/mock-server/build/libs/mock-server-*.jar app.jar

ENV SERVER_PORT=8081
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
