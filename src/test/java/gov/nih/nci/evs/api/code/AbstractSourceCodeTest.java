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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Abstract superclass for source code tests. */
public class AbstractSourceCodeTest {

  /** The logger. */
  @SuppressWarnings("unused")
  private final Logger logger = LoggerFactory.getLogger(AbstractSourceCodeTest.class);

  /**
   * Returns the method.
   *
   * @param match the match
   * @param file the file
   * @return the method
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public String getMethodText(final String match, final File file) throws IOException {
    final List<String> lines = FileUtils.readLines(file, "UTF-8");
    final StringBuilder sb = new StringBuilder();
    boolean inMethod = false;
    for (final String line : lines) {

      // Method start
      if (line.contains(match)) {
        inMethod = true;
      }

      if (inMethod) {
        sb.append(line);
      }
      // Method end
      // this could be better, relies on formatter
      if (inMethod && line.startsWith("  }")) {
        inMethod = false;
        break;
      }
    }
    return sb.toString();
  }

  /**
   * Checks for class annotation.
   *
   * @param annotation the annotation
   * @param file the file
   * @return true, if successful
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public boolean hasClassAnnotation(final String annotation, final File file) throws IOException {
    final List<String> lines = FileUtils.readLines(file, Charset.forName("UTF-8"));
    for (final String line : lines) {

      if (line.equals(annotation)) {
        return true;
      }
      if (line.matches(".* class .*")
          || line.matches(".* interface .*")
          || line.matches(".* enum .*")) {
        return false;
      }
    }
    return false;
  }

  /**
   * Grep.
   *
   * @param match the match
   * @param file the file
   * @return the list
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public List<String> grep(final String match, final File file) throws IOException {
    final List<String> lines = FileUtils.readLines(file, "UTF-8");
    final List<String> matches = new ArrayList<>();
    for (final String line : lines) {

      // Method start
      if (line.contains(match)) {
        matches.add(line);
      }
    }
    return matches;
  }
}
