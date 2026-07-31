FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN groupadd --system evsapi \
    && useradd --system --gid evsapi --home-dir /app --shell /usr/sbin/nologin evsapi

COPY build/libs/evsrestapi-*.war /app/evsrestapi.war

USER evsapi

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/evsrestapi.war"]
