package gov.nih.nci.evs.api.util;

import gov.nih.nci.evs.api.util.RrfReaders.Keys;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copier for RRF files. Takes a terminology a nd a set of cuis and subsets RRF files based on that.
 * Keeps all MRDOC entries.
 */
public class RrfFileCopier {

  /** The logger. */
  private static Logger logger = LoggerFactory.getLogger(RrfFileCopier.class);

  /** The active only. */
  private boolean activeOnly;

  /**
   * Instantiates an empty {@link RrfFileCopier}.
   *
   * @throws Exception if anything goes wrong
   */
  public RrfFileCopier() throws Exception {
    // do nothing
  }

  /**
   * Sort files.
   *
   * @param inputDir the input dir
   * @param outputDir the output dir
   * @param terminologies the terminologies
   * @param cuis the cuis
   * @throws Exception the exception
   */
  public void copyFiles(
      final File inputDir,
      final File outputDir,
      final Set<String> terminologies,
      final Set<String> cuis)
      throws Exception {
    logger.info("Start copying files");

    // Remove and remake output dir
    logger.info("  Remove and remake output dir");
    FileUtils.deleteDirectory(outputDir);
    if (!outputDir.mkdirs()) {
      throw new Exception("Problem making output dir: " + outputDir);
    }

    // Check preconditions
    if (!inputDir.exists()) {
      throw new Exception("Input dir does not exist: " + inputDir);
    }

    // Copy files
    for (final Keys key : RrfReaders.Keys.values()) {

      final String file = key.name() + (key == Keys.SRDEF ? "" : ".RRF");
      logger.info("  Copying for " + file);

      final File inputFile = new File(inputDir, file);
      if (!inputFile.exists()) {
        logger.debug("    SKIP FILE does not exists. {}", file);
        continue;
      }

      final File outputFile = new File(outputDir, inputFile.getName());
      logger.info("    input file = " + inputFile);
      logger.info("    output file = " + outputFile);
      copyFile(inputFile, outputFile, key, cuis, terminologies);
    }

    // release.dat, cnfig.prop
    for (final String file : new String[] {"release.dat", "config.prop"}) {
      final File inputFile = new File(inputDir, file);
      if (!inputFile.exists()) {
        logger.debug("    SKIP FILE does not exists. {}", file);
        continue;
      }

      final File outputFile = new File(outputDir, inputFile.getName());
      logger.info("    input file = " + inputFile);
      logger.info("    output file = " + outputFile);
      copyFile(inputFile, outputFile, null, null, null);
    }
  }

  /**
   * Copy file.
   *
   * @param inputFile the input file
   * @param outputFile the output file
   * @param key the key
   * @param cuis the cuis
   * @param sabs the sabs
   * @throws Exception the exception
   */
  private void copyFile(
      final File inputFile,
      final File outputFile,
      Keys key,
      final Set<String> cuis,
      final Set<String> sabs)
      throws Exception {

    final int[] cuiFields = key == null ? null : key.getCuiFields();
    // final int sabField = key == null ? -1 : key.getSabField();

    // Now, iterate through input file and copy lines with headers
    // or where the "keyMap" field is in concepts/descriptions
    try (final BufferedReader in = new BufferedReader(new FileReader(inputFile));
        PrintWriter out = new PrintWriter(new FileWriter(outputFile)); ) {
      String line;
      OUTER:
      while ((line = in.readLine()) != null) {
        final String[] fields = line.split("\\|", -1);

        // Skip non-matching CUI
        if (cuiFields != null && cuis != null && !cuis.isEmpty()) {
          for (final int i : cuiFields) {
            if (!cuis.contains(fields[i])) {
              continue OUTER;
            }
          }
        }

        // ACTUALLY don't do this, keep all lines for files without CUIs
        // and for fiels with CUIs, keep lines with matching CUIs

        // // Skip non-matching SAB (keep SRC data for CUIs specified)
        // if (sabField != -1 && sabs != null && !sabs.isEmpty() && !sabs.contains(fields[sabField])
        // && !fields[sabField].equals("SRC")) {
        // continue;
        // }

        // Otherwise, copy line.
        out.print(line + "\n");
      }
    }
  }

  /**
   * Indicates whether or not active only is the case.
   *
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  public boolean isActiveOnly() {
    return activeOnly;
  }

  /**
   * Sets the active only.
   *
   * @param activeOnly the active only
   */
  public void setActiveOnly(final boolean activeOnly) {
    this.activeOnly = activeOnly;
  }
}
