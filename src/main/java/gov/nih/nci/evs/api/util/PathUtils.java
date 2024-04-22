package gov.nih.nci.evs.api.util;

import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.Path;
import gov.nih.nci.evs.api.model.Paths;
import java.util.HashSet;

/**
 * Path related utility functions
 *
 * @author Arun
 */
public class PathUtils {
  /**
   * Removes the duplicate paths.
   *
   * @param paths the paths
   * @return the paths
   */
  public static Paths removeDuplicatePaths(Paths paths) {
    final Paths uniquePaths = new Paths();
    final HashSet<String> seenPaths = new HashSet<String>();
    for (final Path path : paths.getPaths()) {
      final StringBuffer strPath = new StringBuffer();
      for (final ConceptMinimal concept : path.getConcepts()) {
        strPath.append(concept.getCode());
        strPath.append("|");
      }
      String pathString = strPath.toString();
      if (!seenPaths.contains(pathString)) {
        seenPaths.add(pathString);
        uniquePaths.add(path);
      }
    }

    return uniquePaths;
  }
}
