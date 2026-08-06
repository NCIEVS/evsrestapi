# Build the deployable WAR in an isolated Linux Gradle environment.
FROM gradle:8.14.2-jdk17 AS gradle-build

WORKDIR /workspace

COPY --chown=gradle:gradle build.gradle gradle.properties ./
RUN gradle --no-daemon dependencies

COPY --chown=gradle:gradle src/main ./src/main
RUN gradle --no-daemon bootWar -x test

# Run only the packaged application as an unprivileged user.
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN groupadd --system evsapi \
    && useradd --system --gid evsapi --home-dir /app --shell /usr/sbin/nologin evsapi

COPY --from=gradle-build --chown=evsapi:evsapi /workspace/build/libs/evsrestapi-*.war /app/evsrestapi.war

USER evsapi

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/evsrestapi.war"]