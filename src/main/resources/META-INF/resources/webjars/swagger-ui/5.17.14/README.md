# Custom Swagger UI updating

1. Run `gradle dependencies` in the base level of the repository.
2. Find the version of `org.webjars:swagger-ui` that you need to upgrade to.
3. Download that jar from https://mvnrepository.com/artifact/org.webjars/swagger-ui/{that version}.
4. Rename it to a zip file.
5. Replace src/main/resources/META-INF with the unzipped download, and copy over the old index.html found in src/main/resources/META-INF/resources/webjars/swagger-ui/{current version}.
6. Delete the old version, and anything in the new version except the copied over index.html.
7. Make any changes you need to in index.html.

// add test to make sure current version matches classpath in VersionController