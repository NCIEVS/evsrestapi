# Custom Swagger UI updating

1. Run `gradle dependencies` in the base level of the repository.
2. Find the version of `org.webjars:swagger-ui` that you need to upgrade to.
3. Download that jar from https://mvnrepository.com/artifact/org.webjars/swagger-ui/{that version}.
4. Rename it to a zip file.
5. Refresh the bundled Maven/manifest metadata from that jar and move the customized index.html and documentation-links.css into src/main/resources/META-INF/resources/webjars/swagger-ui/{new version}.
6. Delete the old version. Other UI assets are supplied by the dependency WebJar.
7. Align springdoc.swagger-ui.version in src/main/resources/application.yml and src/test/resources/application-test.yml, and run VersionControllerTests and FhirSwaggerUiTests.

// add test to make sure current version matches classpath in VersionController
