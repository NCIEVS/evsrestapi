package gov.nih.nci.evs.api.util;

import java.util.HashSet;

import gov.nih.nci.evs.api.model.ConceptNode;
import gov.nih.nci.evs.api.model.Path;
import gov.nih.nci.evs.api.model.Paths;

/**
 * Path related utility functions
 * 
 * @author Arun
 *
 */
public class PathUtils {
  /**
   * Removes the duplicate paths.
   *
   * @param paths the paths
   * @return the paths
   */
  public static Paths removeDuplicatePaths(Paths paths) {
    Paths uniquePaths = new Paths();
    HashSet<String> seenPaths = new HashSet<String>();
    for (Path path : paths.getPaths()) {
      StringBuffer strPath = new StringBuffer();
      for (ConceptNode concept : path.getConcepts()) {
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
