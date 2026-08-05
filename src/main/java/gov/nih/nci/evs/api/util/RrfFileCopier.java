package gov.nih.nci.evs.api.util;

import gov.nih.nci.evs.api.util.RrfReaders.Keys;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashSet;
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
    copyFiles(inputDir, outputDir, terminologies, cuis, null);
  }

  /**
   * Copy files for the specified code/SAB values.
   *
   * @param inputDir the input dir
   * @param outputDir the output dir
   * @param terminologies the terminologies
   * @param codesabs the code/SAB values to keep, encoded as {@code CODE|SAB}
   * @throws Exception the exception
   */
  public void copyFilesByCodeSabs(
      final File inputDir,
      final File outputDir,
      final Set<String> terminologies,
      final Set<String> codesabs)
      throws Exception {
    copyFiles(
        inputDir,
        outputDir,
        terminologies,
        null,
        buildCodeSabCopyScope(inputDir, codesabs == null ? Set.of() : codesabs));
  }

  /**
   * Copy files.
   *
   * @param inputDir the input dir
   * @param outputDir the output dir
   * @param terminologies the terminologies
   * @param cuis the cuis
   * @param codeSabCopyScope the code/SAB copy scope
   * @throws Exception the exception
   */
  private void copyFiles(
      final File inputDir,
      final File outputDir,
      final Set<String> terminologies,
      final Set<String> cuis,
      final CodeSabCopyScope codeSabCopyScope)
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
      copyFile(inputFile, outputFile, key, cuis, terminologies, codeSabCopyScope);
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
      copyFile(inputFile, outputFile, null, null, null, null);
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
   * @param codeSabCopyScope the code/SAB copy scope
   * @throws Exception the exception
   */
  private void copyFile(
      final File inputFile,
      final File outputFile,
      Keys key,
      final Set<String> cuis,
      final Set<String> sabs,
      final CodeSabCopyScope codeSabCopyScope)
      throws Exception {

    final int[] cuiFields = key == null ? null : key.getCuiFields();
    // final int sabField = key == null ? -1 : key.getSabField();

    // Now, iterate through input file and copy lines with headers
    // or where the "keyMap" field is in concepts/descriptions
    try (final BufferedReader in = new BufferedReader(new FileReader(inputFile));
        PrintWriter out = new PrintWriter(new FileWriter(outputFile)); ) {
      String line;
      long lineNumber = 0;
      OUTER:
      while ((line = in.readLine()) != null) {
        lineNumber++;
        final String[] fields = line.split("\\|", -1);
        validateFieldCount(inputFile, key, fields, lineNumber, getPreFilterRequiredFieldCount(key));

        // If MRCUI and it is a DEL entry, then keep
        if (key == Keys.MRCUI && "DEL".equals(fields[2])) {
          out.print(line + "\n");
          continue;
        }

        validateFieldCount(
            inputFile, key, fields, lineNumber, getRequiredFieldCount(key, cuis, codeSabCopyScope));

        // In code/SAB mode, keep only matching atoms and downstream rows scoped to those atoms.
        if (codeSabCopyScope != null && !matchesCodeSabScope(key, fields, codeSabCopyScope)) {
          continue;
        }

        // Skip non-matching CUI
        if (codeSabCopyScope == null && cuiFields != null && cuis != null && !cuis.isEmpty()) {
          for (final int i : cuiFields) {
            if (!cuis.contains(fields[i])) {
              continue OUTER;
            }
          }
        }

        // ACTUALLY don't do this, keep all lines for files without CUIs
        // and for files with CUIs, keep lines with matching CUIs

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
   * Builds the code/SAB copy scope from matching MRCONSO atoms.
   *
   * @param inputDir the input dir
   * @param codesabs the code/SAB values
   * @return the code/SAB copy scope
   * @throws Exception the exception
   */
  private CodeSabCopyScope buildCodeSabCopyScope(final File inputDir, final Set<String> codesabs)
      throws Exception {
    final CodeSabCopyScope scope = new CodeSabCopyScope(codesabs);
    final File inputFile = new File(inputDir, Keys.MRCONSO.name() + ".RRF");
    if (!inputFile.exists()) {
      return scope;
    }

    try (final BufferedReader in = new BufferedReader(new FileReader(inputFile)); ) {
      String line;
      long lineNumber = 0;
      while ((line = in.readLine()) != null) {
        lineNumber++;
        final String[] fields = line.split("\\|", -1);
        validateFieldCount(inputFile, Keys.MRCONSO, fields, lineNumber, 14);
        if (!scope.containsCodeSab(fields[13], fields[11])) {
          continue;
        }
        scope.addCui(fields[0]);
        scope.addLui(fields[3]);
        scope.addSui(fields[5]);
        scope.addAui(fields[7]);
        scope.addSaui(fields[8], fields[11]);
        scope.addScui(fields[9], fields[11]);
        scope.addSdui(fields[10], fields[11]);
      }
    }
    logger.info("  Code/SAB copy scope codesabs = " + scope.getCodeSabs().size());
    logger.info("  Code/SAB copy scope cuis = " + scope.getCuis().size());
    logger.info("  Code/SAB copy scope auis = " + scope.getAuis().size());
    return scope;
  }

  /**
   * Indicates whether or not an RRF row matches the code/SAB copy scope.
   *
   * @param key the key
   * @param fields the fields
   * @param scope the code/SAB copy scope
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  private boolean matchesCodeSabScope(
      final Keys key, final String[] fields, final CodeSabCopyScope scope) {
    if (key == null) {
      return true;
    }

    switch (key) {
      case MRCONSO:
        return scope.containsCodeSab(fields[13], fields[11]);
      case MRDEF:
        return scope.containsAui(fields[1]);
      case MRREL:
        return matchesRelationshipEndpoint(fields[1], fields[2], fields[0], fields[10], scope)
            && matchesRelationshipEndpoint(fields[5], fields[6], fields[4], fields[10], scope);
      case MRHIER:
        return scope.containsAui(fields[1]);
      case MRSAT:
        return matchesAttribute(fields, scope);
      case MRSTY:
        return scope.containsCui(fields[0]);
      case MRCUI:
        return scope.containsCui(fields[5]);
      case MRMAP:
        // MRMAP rows are CUI/mapset scoped, so code/SAB mode keeps all map rows for retained CUIs.
        return scope.containsCui(fields[0]);
      default:
        return matchesCuiFields(key, fields, scope);
    }
  }

  /**
   * Returns the field count needed before the main copy filters run.
   *
   * @param key the key
   * @return the field count
   */
  private int getPreFilterRequiredFieldCount(final Keys key) {
    if (key == Keys.MRCUI) {
      return 3;
    }
    return 0;
  }

  /**
   * Returns the field count needed for the active copy filters.
   *
   * @param key the key
   * @param cuis the CUIs
   * @param codeSabCopyScope the code/SAB copy scope
   * @return the field count
   */
  private int getRequiredFieldCount(
      final Keys key, final Set<String> cuis, final CodeSabCopyScope codeSabCopyScope) {
    if (key == null) {
      return 0;
    }

    int requiredFieldCount = 0;
    if (activeOnly && key == Keys.MRCONSO) {
      requiredFieldCount = 17;
    }

    if (codeSabCopyScope != null) {
      return Math.max(requiredFieldCount, getCodeSabRequiredFieldCount(key));
    }

    if (key.getCuiFields() != null && cuis != null && !cuis.isEmpty()) {
      for (final int field : key.getCuiFields()) {
        requiredFieldCount = Math.max(requiredFieldCount, field + 1);
      }
    }
    return requiredFieldCount;
  }

  /**
   * Returns the field count needed to apply code/SAB copy filters.
   *
   * @param key the key
   * @return the field count
   */
  private int getCodeSabRequiredFieldCount(final Keys key) {
    switch (key) {
      case MRCONSO:
        return 14;
      case MRDEF:
      case MRHIER:
        return 2;
      case MRREL:
        return 11;
      case MRSAT:
        return 10;
      case MRCUI:
        return 6;
      case MRMAP:
      case MRSTY:
        return 1;
      default:
        return getCuiRequiredFieldCount(key);
    }
  }

  /**
   * Returns the field count needed to read all configured CUI fields.
   *
   * @param key the key
   * @return the field count
   */
  private int getCuiRequiredFieldCount(final Keys key) {
    int requiredFieldCount = 0;
    final int[] cuiFields = key.getCuiFields();
    if (cuiFields != null) {
      for (final int field : cuiFields) {
        requiredFieldCount = Math.max(requiredFieldCount, field + 1);
      }
    }
    return requiredFieldCount;
  }

  /**
   * Validates that an RRF row has enough fields for this copier's filters.
   *
   * @param inputFile the input file
   * @param key the key
   * @param fields the fields
   * @param lineNumber the line number
   * @param requiredFieldCount the required field count
   * @throws Exception the exception
   */
  private void validateFieldCount(
      final File inputFile,
      final Keys key,
      final String[] fields,
      final long lineNumber,
      final int requiredFieldCount)
      throws Exception {
    if (fields.length < requiredFieldCount) {
      throw new Exception(
          "Malformed RRF row in "
              + inputFile.getName()
              + " ("
              + key
              + ") at line "
              + lineNumber
              + ": expected at least "
              + requiredFieldCount
              + " fields, found "
              + fields.length);
    }
  }

  /**
   * Indicates whether or not an RRF row matches all CUI fields.
   *
   * @param key the key
   * @param fields the fields
   * @param scope the code/SAB copy scope
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  private boolean matchesCuiFields(
      final Keys key, final String[] fields, final CodeSabCopyScope scope) {
    final int[] cuiFields = key.getCuiFields();
    if (cuiFields == null) {
      return true;
    }
    for (final int field : cuiFields) {
      if (!scope.containsCui(fields[field])) {
        return false;
      }
    }
    return true;
  }

  /**
   * Indicates whether or not an MRSAT row matches the code/SAB copy scope.
   *
   * @param fields the fields
   * @param scope the code/SAB copy scope
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  private boolean matchesAttribute(final String[] fields, final CodeSabCopyScope scope) {
    final String metaUi = fields[3];
    final String stype = fields[4];
    final String code = fields[5];
    final String sab = fields[9];

    switch (stype) {
      case "AUI":
        return scope.containsAui(metaUi) || scope.containsAui(code);
      case "CODE":
        return scope.containsCodeSab(code, sab);
      case "CUI":
        return scope.containsCui(fields[0]) || scope.containsCui(code);
      case "LUI":
        return scope.containsLui(fields[1]) || scope.containsLui(metaUi) || scope.containsLui(code);
      case "SUI":
        return scope.containsSui(fields[2]) || scope.containsSui(metaUi) || scope.containsSui(code);
      case "SAUI":
        return scope.containsSaui(metaUi, sab) || scope.containsSaui(code, sab);
      case "SCUI":
        return scope.containsScui(metaUi, sab) || scope.containsScui(code, sab);
      case "SDUI":
        return scope.containsSdui(metaUi, sab) || scope.containsSdui(code, sab);
      default:
        return scope.containsCui(fields[0]);
    }
  }

  /**
   * Indicates whether or not an MRREL endpoint matches the code/SAB copy scope.
   *
   * @param identifier the endpoint identifier
   * @param stype the endpoint identifier type
   * @param cui the endpoint CUI
   * @param sab the SAB
   * @param scope the code/SAB copy scope
   * @return <code>true</code> if so, <code>false</code> otherwise
   */
  private boolean matchesRelationshipEndpoint(
      final String identifier,
      final String stype,
      final String cui,
      final String sab,
      final CodeSabCopyScope scope) {
    if (identifier == null || identifier.isEmpty()) {
      return scope.containsCui(cui);
    }

    switch (stype) {
      case "AUI":
        return scope.containsAui(identifier);
      case "CODE":
        return scope.containsCodeSab(identifier, sab);
      case "CUI":
        return scope.containsCui(identifier) || scope.containsCui(cui);
      case "SAUI":
        return scope.containsSaui(identifier, sab);
      case "SCUI":
        return scope.containsScui(identifier, sab);
      case "SDUI":
        return scope.containsSdui(identifier, sab);
      default:
        return scope.containsAui(identifier) || scope.containsCui(cui);
    }
  }

  /** Code/SAB copy scope derived from matching MRCONSO rows. */
  private static class CodeSabCopyScope {

    /** The code/SAB values. */
    private final Set<String> codeSabs;

    /** The CUIs. */
    private final Set<String> cuis = new HashSet<>();

    /** The LUIs. */
    private final Set<String> luis = new HashSet<>();

    /** The SUIs. */
    private final Set<String> suis = new HashSet<>();

    /** The AUIs. */
    private final Set<String> auis = new HashSet<>();

    /** The SAUI/SAB values. */
    private final Set<String> sauis = new HashSet<>();

    /** The SCUI/SAB values. */
    private final Set<String> scuis = new HashSet<>();

    /** The SDUI/SAB values. */
    private final Set<String> sduis = new HashSet<>();

    /**
     * Instantiates a new code/SAB copy scope.
     *
     * @param codeSabs the code/SAB values
     */
    private CodeSabCopyScope(final Set<String> codeSabs) {
      this.codeSabs = new HashSet<>(codeSabs);
    }

    /**
     * Returns the code/SAB values.
     *
     * @return the code/SAB values
     */
    private Set<String> getCodeSabs() {
      return codeSabs;
    }

    /**
     * Returns the CUIs.
     *
     * @return the CUIs
     */
    private Set<String> getCuis() {
      return cuis;
    }

    /**
     * Returns the AUIs.
     *
     * @return the AUIs
     */
    private Set<String> getAuis() {
      return auis;
    }

    /**
     * Adds a CUI.
     *
     * @param cui the CUI
     */
    private void addCui(final String cui) {
      addIfPresent(cuis, cui);
    }

    /**
     * Adds an LUI.
     *
     * @param lui the LUI
     */
    private void addLui(final String lui) {
      addIfPresent(luis, lui);
    }

    /**
     * Adds an SUI.
     *
     * @param sui the SUI
     */
    private void addSui(final String sui) {
      addIfPresent(suis, sui);
    }

    /**
     * Adds an AUI.
     *
     * @param aui the AUI
     */
    private void addAui(final String aui) {
      addIfPresent(auis, aui);
    }

    /**
     * Adds an SAUI/SAB value.
     *
     * @param saui the SAUI
     * @param sab the SAB
     */
    private void addSaui(final String saui, final String sab) {
      addSourceIdIfPresent(sauis, saui, sab);
    }

    /**
     * Adds an SCUI/SAB value.
     *
     * @param scui the SCUI
     * @param sab the SAB
     */
    private void addScui(final String scui, final String sab) {
      addSourceIdIfPresent(scuis, scui, sab);
    }

    /**
     * Adds an SDUI/SAB value.
     *
     * @param sdui the SDUI
     * @param sab the SAB
     */
    private void addSdui(final String sdui, final String sab) {
      addSourceIdIfPresent(sduis, sdui, sab);
    }

    /**
     * Indicates whether or not the code/SAB value is included.
     *
     * @param code the code
     * @param sab the SAB
     * @return <code>true</code> if so, <code>false</code> otherwise
     */
    private boolean containsCodeSab(final String code, final String sab) {
      if (code == null || code.isEmpty() || sab == null || sab.isEmpty()) {
        return false;
      }
      return codeSabs.contains(toCodeSab(code, sab));
    }

    /**
     * Indicates whether or not the CUI is included.
     *
     * @param cui the CUI
     * @return <code>true</code> if so, <code>false</code> otherwise
     */
    private boolean containsCui(final String cui) {
      return cuis.contains(cui);
    }

    /**
     * Indicates whether or not the LUI is included.
     *
     * @param lui the LUI
     * @return <code>true</code> if so, <code>false</code> otherwise
     */
    private boolean containsLui(final String lui) {
      return luis.contains(lui);
    }

    /**
     * Indicates whether or not the SUI is included.
     *
     * @param sui the SUI
     * @return <code>true</code> if so, <code>false</code> otherwise
     */
    private boolean containsSui(final String sui) {
      return suis.contains(sui);
    }

    /**
     * Indicates whether or not the AUI is included.
     *
     * @param aui the AUI
     * @return <code>true</code> if so, <code>false</code> otherwise
     */
    private boolean containsAui(final String aui) {
      return auis.contains(aui);
    }

    /**
     * Indicates whether or not the SAUI/SAB value is included.
     *
     * @param saui the SAUI
     * @param sab the SAB
     * @return <code>true</code> if so, <code>false</code> otherwise
     */
    private boolean containsSaui(final String saui, final String sab) {
      return containsSourceId(sauis, saui, sab);
    }

    /**
     * Indicates whether or not the SCUI/SAB value is included.
     *
     * @param scui the SCUI
     * @param sab the SAB
     * @return <code>true</code> if so, <code>false</code> otherwise
     */
    private boolean containsScui(final String scui, final String sab) {
      return containsSourceId(scuis, scui, sab);
    }

    /**
     * Indicates whether or not the SDUI/SAB value is included.
     *
     * @param sdui the SDUI
     * @param sab the SAB
     * @return <code>true</code> if so, <code>false</code> otherwise
     */
    private boolean containsSdui(final String sdui, final String sab) {
      return containsSourceId(sduis, sdui, sab);
    }

    /**
     * Adds the value to the set when it is present.
     *
     * @param values the values
     * @param value the value
     */
    private void addIfPresent(final Set<String> values, final String value) {
      if (value != null && !value.isEmpty()) {
        values.add(value);
      }
    }

    /**
     * Adds the source identifier/SAB value to the set when both parts are present.
     *
     * @param values the values
     * @param sourceId the source identifier
     * @param sab the SAB
     */
    private void addSourceIdIfPresent(
        final Set<String> values, final String sourceId, final String sab) {
      if (sourceId != null && !sourceId.isEmpty() && sab != null && !sab.isEmpty()) {
        values.add(toCodeSab(sourceId, sab));
      }
    }

    /**
     * Indicates whether or not the source identifier/SAB value is included.
     *
     * @param values the values
     * @param sourceId the source identifier
     * @param sab the SAB
     * @return <code>true</code> if so, <code>false</code> otherwise
     */
    private boolean containsSourceId(
        final Set<String> values, final String sourceId, final String sab) {
      if (sourceId == null || sourceId.isEmpty() || sab == null || sab.isEmpty()) {
        return false;
      }
      return values.contains(toCodeSab(sourceId, sab));
    }

    /**
     * Returns a code/SAB value.
     *
     * @param code the code
     * @param sab the SAB
     * @return the code/SAB value
     */
    private String toCodeSab(final String code, final String sab) {
      return code + "|" + sab;
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
