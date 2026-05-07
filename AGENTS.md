# AGENTS.md

## Project

EVS REST API built with Java 17, Gradle, and Spring Boot.

## Important Docs

- `README.md`
- `JENA.md`
- `OPENSEARCH.md`
- `src/main/resources/application-local.yml`

## Common Commands

- Build: `make clean build`
- Test: `make test`
- Format check: `./gradlew spotlessCheck`
- Format fix: `./gradlew spotlessApply`
- Scan for vulnerabilities: `make scan` (look for HIGH or CRITICAL entries in report.html)

## Local Run Notes

- Use the local Spring profile: `-Dspring.profiles.active=local`
- The application depends on Jena/Fuseki and OpenSearch being available locally
  - Test that jena is running by using `curl localhost:3030`
  - Test that opensearch is running by using `curl localhost:9200`
- Some tests and local workflows depend on data and setup steps described in `README.md`

## Agent Guidelines

- Prefer minimal, targeted changes
- Do not modify unrelated files
- Do not commit secrets or local credentials
- Preserve existing project structure and naming conventions
- Format code according to the spotless configuration
- Run the smallest useful verification for the files changed
- Check the project docs before changing config or infrastructure assumptions
