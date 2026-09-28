# Build the deployable WAR in an isolated Linux Gradle environment.
FROM gradle:9.3.0-jdk25 AS gradle-build

WORKDIR /workspace

COPY --chown=gradle:gradle build.gradle settings.gradle gradle.properties ./
# RUN gradle --no-daemon dependencies

COPY --chown=gradle:gradle src/main ./src/main
RUN gradle --no-daemon bootWar -x test -x javadoc

# Run only the packaged application as an unprivileged user.
FROM eclipse-temurin:25-jre-noble

WORKDIR /app

RUN groupadd --system evsapi \
    && useradd --system --gid evsapi --home-dir /app --shell /usr/sbin/nologin evsapi

COPY --from=gradle-build --chown=evsapi:evsapi /workspace/build/libs/ /app/

USER evsapi

EXPOSE 8082

CMD ["sh", "-c", "java -jar /app/evsrestapi*.war"]
