/*
 * Copyright 2025 West Coast Informatics - All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property of West Coast Informatics
 * The intellectual and technical concepts contained herein are proprietary to
 * West Coast Informatics and may be covered by U.S. and Foreign Patents, patents in process,
 * and are protected by trade secret or copyright law.  Dissemination of this information
 * or reproduction of this material is strictly forbidden.
 */
package gov.nih.nci.evs.api.code;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Source code test to verify that "R4" classes do not import .r5. classes and that "R5" classes do
 * not import ".r4" classes.
 */
public class FhirReferencesSourceCodeTest extends AbstractSourceCodeTest {

  /** The logger. */
  private final Logger logger = LoggerFactory.getLogger(FhirReferencesSourceCodeTest.class);

  /** The paths. */
  private static Set<Path> paths;

  /**
   * Setup class.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @SuppressWarnings("resource")
  @BeforeAll
  public static void setupClass() throws IOException {
    // Find all java model objects
    paths =
        Files.find(
                Paths.get("src/main/java"),
                Integer.MAX_VALUE,
                (filePath, fileAttr) -> filePath.toString().endsWith(".java"))
            .collect(Collectors.toSet());
    paths.addAll(
        Files.find(
                Paths.get("src/test/java"),
                Integer.MAX_VALUE,
                (filePath, fileAttr) -> filePath.toString().endsWith(".java"))
            .collect(Collectors.toSet()));
  }

  /**
   * Test equals methods for offending fields.
   *
   * @throws Exception the exception
   */
  @Test
  public void testR4() throws Exception {

    boolean failed = false;
    for (final Path path : paths) {
      if (!path.toString().toUpperCase().contains("R4")) {
        continue;
      }
      // Exception
      if (path.toString().contains("OpenApiInterceptor")) {
        continue;
      }
      final List<String> lines = FileUtils.readLines(path.toFile(), "UTF-8");

      for (final String line : lines) {
        if (line.matches("import.*\\.r5\\..*") || line.matches("import.*\\.R5\\..*")) {
          logger.error("Import for R4 references an r5 import - " + path.toFile().getName());
          failed = true;
          break;
        } else if (line.matches(".*\\.fhir\\.r5\\..*")) {
          logger.error("Line in R4 references an r5 class - " + path.toFile().getName());
          failed = true;
          break;
        }
      }
    }
    // A failure here means that a file with R4 in the filename
    // makes use of an *.r5.* import.
    if (failed) {
      fail("Errors with use of logger, see console for details.");
    }
  }

  @Test
  public void testR5() throws Exception {

    boolean failed = false;
    for (final Path path : paths) {
      if (!path.toString().toUpperCase().contains("R5")) {
        continue;
      }
      // Exception
      if (path.toString().contains("OpenApiInterceptor")) {
        continue;
      }

      final List<String> lines = FileUtils.readLines(path.toFile(), "UTF-8");

      for (final String line : lines) {
        if (line.matches("import.*\\.r4\\..*") || line.matches("import.*\\.R4\\..*")) {
          logger.error("Import for R5 references an r4 import - " + path.toFile().getName());
          failed = true;
          break;
        } else if (line.matches(".*\\.fhir\\.r4\\..*")) {
          logger.error("Line in R5 references an r4 class - " + path.toFile().getName());
          failed = true;
          break;
        }
      }
    }
    // A failure here means that a file with R5 in the filename
    // makes use of an *.r4.* import.
    if (failed) {
      fail("Errors with use of logger, see console for details.");
    }
  }
}
