package gov.nih.nci.evs.api.util;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This sample generator takes a list of CUIs and a list of SABs and it computes a subset of a
 * Metathesaurus. It resolves all SABs in the CUI set with paths to the root and includes all
 * content from that extended set of CUIs. It also has options for also including distance-one
 * relationships from the original CUI set OR keeping all descendants of codes in the original CUI
 * set.
 */
public class RrfSampleGenerator {

  /** The logger. */
  private static Logger logger = LoggerFactory.getLogger(RrfSampleGenerator.class);

  /** The input path. */
  private String inputPath = null;

  /** The codes file. */
  private String cuisFile;

  /** The terminology. */
  private Set<String> terminologies;

  /** The readers. */
  private RrfReaders readers;

  /** The keep descendants. */
  private boolean keepDescendants = false;

  /** The distance one. */
  private boolean distanceOne = true;

  /** The chd par map. */
  private Map<String, Set<String>> chdParMap = new HashMap<>();

  /** The chd par map. */
  private Map<String, Set<String>> parChdMap = new HashMap<>();

  /** The other map. */
  private Map<String, Set<String>> otherMap = new HashMap<>();

  /** The aui -> cui map. */
  private Map<String, String> auiCodesabMap = new HashMap<>();

  /** The cui -> aui map. */
  private Map<String, String> codesabAuiMap = new HashMap<>();

  /** The code cuis map. */
  private Map<String, Set<String>> codesabCuisMap = new HashMap<>();

  /** The cuis code map. */
  private Map<String, Set<String>> cuiCodesabsMap = new HashMap<>();

  /** The src codes. */
  private Set<String> srcCodesabs = new HashSet<>();

  /** The cuis. */
  private Set<String> allcuis = new HashSet<>();

  /**
   * Instantiates an empty {@link RrfSampleGenerator}.
   *
   * @throws Exception if anything goes wrong
   */
  public RrfSampleGenerator() throws Exception {
    super();
  }

  /**
   * Sets the readers.
   *
   * @param readers the readers
   */
  public void setReaders(final RrfReaders readers) {
    this.readers = readers;
  }

  /**
   * Compute.
   *
   * @throws Exception the exception
   */
  public void compute() throws Exception {
    try {

      logger.info("Start RRF sampling");
      logger.info("  inputPath = " + inputPath);
      logger.info("  cuisFile = " + cuisFile);
      logger.info("  terminologies = " + terminologies);
      logger.info("  keepDescendants = " + keepDescendants);
      logger.info("  distanceOne = " + distanceOne);

      // Verify input path
      final File file = new File(inputPath);
      if (!file.exists()) {
        throw new Exception("Input path does not exist = " + inputPath);
      }
      if (!file.isDirectory()) {
        throw new Exception("Input path is not a directory = " + inputPath);
      }

      if (!new File(cuisFile).exists()) {
        throw new Exception("Codes file does not exist = " + cuisFile);
      }
      final Set<String> inputCuis =
          new HashSet<>(
              FileUtils.readLines(new File(cuisFile), "UTF-8").stream()
                  .filter(s -> !s.isEmpty() && !s.startsWith("#"))
                  .collect(Collectors.toList()));
      logger.info("  input cuis = " + inputCuis);

      // Open the readers
      readers = new RrfReaders(new File(inputPath));
      readers.openOriginalReaders("MR");

      // load maps
      logger.info("  Load maps");
      loadMaps(inputCuis);

      // Verify input codes
      if (inputCuis.stream().filter(c -> !allcuis.contains(c)).count() > 0) {
        throw new Exception(
            "INVALID input cuis = "
                + inputCuis.stream()
                    .filter(c -> !allcuis.contains(c))
                    .collect(Collectors.toList()));
      }
      logger.info("    chdPar count = " + chdParMap.size());
      // logger.info(" chdPar = " + chdParMap);
      logger.info("    other count = " + otherMap.size());
      // logger.info(" other = " + otherMap);

      logger.info("  Find initial codes");
      // 1. Find initial concepts (keep only things from initial CUI list)
      final Set<String> codesabs = new HashSet<>();
      codesabs.addAll(
          inputCuis.stream()
              .flatMap(c -> cuiCodesabsMap.get(c).stream())
              .collect(Collectors.toSet()));
      logger.info("    count = " + codesabs.size());

      // 1b. Get descendants if indicated
      final Set<String> descendants = new HashSet<>();
      if (keepDescendants) {
        for (final String codesab : new HashSet<>(codesabs)) {
          // Only do this if the SAB part is in terminologies
          if (!terminologies.contains(codesab.replaceFirst(".*\\|", ""))) {
            continue;
          }
          descendants.addAll(getDescendantsHelper(codesabAuiMap.get(codesab)));
          logger.info("    desc count = " + descendants.size());
        }
      }

      // 2. Find other related concepts
      if (distanceOne) {
        logger.info("  Add distance 1 related concepts");
        for (final String codesab : new HashSet<>(codesabs)) {

          // Only do this if the SAB part is in terminologies
          if (!terminologies.contains(codesab.replaceFirst(".*\\|", ""))) {
            continue;
          }
          if (otherMap.containsKey(codesabAuiMap.get(codesab))) {
            logger.info("    add auis = " + otherMap.get(codesabAuiMap.get(codesab)));
            // Map auis back to codesab
            codesabs.addAll(
                otherMap.get(codesabAuiMap.get(codesab)).stream()
                    .map(a -> auiCodesabMap.get(a))
                    .collect(Collectors.toSet()));
          }
        }
        logger.info("    distance one count = " + codesabs.size());
      }

      // Add SRC codes
      codesabs.addAll(srcCodesabs);
      logger.info("    with src codes count = " + codesabs.size());

      // Walk to root
      int prevCt = -1;
      do {
        prevCt = codesabs.size();

        // 4. Find all concepts on path to root (e.g. walk up ancestors)
        // Assumes no cycles
        for (final String codesab : new HashSet<>(codesabs)) {

          // If codesab is null (which can happen while collecting them above)
          if (codesab == null) {
            continue;
          }

          // Only do this if the SAB part is in terminologies
          if (!terminologies.contains(codesab.replaceFirst(".*\\|", ""))) {
            continue;
          }

          final String aui = codesabAuiMap.get(codesab);
          if (aui == null) {
            throw new Exception("Code missing from codeAuiMap = " + codesab);
          }
          if (!chdParMap.containsKey(aui)) {
            logger.info("      encountered root code = " + codesab + ", " + aui);
            continue;
          }
          for (final String parAui : chdParMap.get(aui)) {
            final String par = auiCodesabMap.get(parAui);
            codesabs.add(par);
          }
        }

        logger.info("    count (after ancestors) = " + codesabs.size());
        logger.info("    prev count = " + prevCt);

        // Iterate until the codes list stops growing.
      } while (codesabs.size() != prevCt);

      // Add descendants of original code list back in here (empty if false)
      codesabs.addAll(
          descendants.stream().map(a -> auiCodesabMap.get(a)).collect(Collectors.toSet()));

      // Now, for each codesab, find the CUIs that it is part of
      // If those CUIs have any codesabs, include them as well before
      // we get the final CUI list.  We are not gathering any extra
      // descendants/parents/distance one, just making sure every
      // codesab has the CUI containing its "preferred name"
      logger.info("  Compute closure on final codesabs set");
      prevCt = -1;
      do {
        prevCt = codesabs.size();

        for (final String codesab : new HashSet<>(codesabs)) {
          for (final String cui : codesabCuisMap.get(codesab)) {
            codesabs.addAll(cuiCodesabsMap.get(cui));
          }
        }
        logger.info("    count (after closure) = " + codesabs.size());
        logger.info("    prev count = " + prevCt);

      } while (codesabs.size() != prevCt);

      // Map codes to CUIs (this will pick up some source codes in extra
      // CUIs that are not themselves in codesabs and so we wind up with
      // some extra fake "root" concepts
      final Set<String> cuis =
          codesabs.stream()
              .filter(s -> s != null)
              .flatMap(c -> codesabCuisMap.get(c).stream())
              .collect(Collectors.toSet());

      // Add in original CUIs
      cuis.addAll(inputCuis);

      logger.info("    cuis = " + cuis.size());

      readers.closeReaders();

      // Copy Files
      final RrfFileCopier copier = new RrfFileCopier();
      // Parameterize this!
      final File outputDir = new File(inputPath, "/RRF-subset/");
      copier.setActiveOnly(false);
      // TODO: we probably should pass in codesabs here and keep atoms
      // that match them and then other downstream data matching those AUIs only.
      copier.copyFiles(new File(inputPath), outputDir, terminologies, cuis);

      logger.info("Done ...");

    } catch (final Exception e) {
      if (readers != null) {
        readers.closeReaders();
      }
      throw e;
    }
  }

  /**
   * Load maps.
   *
   * @param inputCuis the input cuis
   * @throws Exception the exception
   */
  private void loadMaps(final Set<String> inputCuis) throws Exception {
    String line = "";
    final Map<String, String> codesabTtyMap = new HashMap<>();
    final Map<String, Integer> ttyMap = new HashMap<>();

    // Lookup term ranks
    try (final PushBackReader reader = readers.getReader(RrfReaders.Keys.MRRANK); ) {
      while ((line = reader.readLine()) != null) {

        // 0096|RXNORM|SY|N|
        final String[] fields = line.split("\\|", -1);
        final String rank = fields[0];
        final String sabtty = fields[1] + fields[2];

        // Keep all SABs
        // if (!terminologies.contains(sab) && !sab.equals("SRC")) {
        // continue;
        // }

        // Add rank, higher is better
        final int irank = Integer.parseInt(rank);
        logger.info("  ttyMap = " + sabtty + ", " + rank);
        ttyMap.put(sabtty, irank);
      }
    }

    // Cache atom/code/cui info
    try (final PushBackReader reader = readers.getReader(RrfReaders.Keys.MRCONSO); ) {
      while ((line = reader.readLine()) != null) {

        // CUI,LAT,TS,LUI,STT,SUI,ISPREF,AUI,SAUI,SCUI,SDUI,
        // SAB,TTY,CODE,STR,SRL,SUPPRESS,CVF
        final String[] fields = line.split("\\|", -1);
        final String cui = fields[0];
        final String aui = fields[7];
        final String sab = fields[11];
        final String sabtty = fields[11] + fields[12];
        final String code = fields[13];
        final String codesab = fields[13] + "|" + sab;

        // Match this source OR the SRC for this terminology (or input cuis)
        if (!inputCuis.contains(cui)
            && !terminologies.contains("*")
            && !terminologies.contains(sab)
            && !(sab.equals("SRC")
                && terminologies.stream().filter(t -> code.contains(t)).count() > 0)) {
          continue;
        }

        // Things with NCIMTH and NOCODE are causing an issue
        if (code.equals("NOCODE")) {
          continue;
        }

        allcuis.add(cui);

        if (sab.equals("SRC") && terminologies.stream().filter(t -> code.contains(t)).count() > 0) {
          srcCodesabs.add(codesab);
        }
        auiCodesabMap.put(aui, codesab);

        if (!codesabCuisMap.containsKey(codesab)) {
          codesabCuisMap.put(codesab, new HashSet<>());
        }
        codesabCuisMap.get(codesab).add(cui);

        if (!cuiCodesabsMap.containsKey(cui)) {
          cuiCodesabsMap.put(cui, new HashSet<>());
        }
        cuiCodesabsMap.get(cui).add(codesab);

        // Support situations where the sabtty exist in
        if (!ttyMap.containsKey(sabtty)) {
          ttyMap.put(sabtty, 0);
        }

        // Compute highest ranking AUI for the code
        if (!codesabAuiMap.containsKey(codesab)) {
          // logger.info("  init codesab->aui = " + codesab + ", " + aui + ", " + sabtty);
          codesabAuiMap.put(codesab, aui);
          codesabTtyMap.put(codesab, sabtty);
        } else if (ttyMap.get(sabtty) > ttyMap.get(codesabTtyMap.get(codesab))) {
          // logger.info("  repl codesab->aui = " + codesab + ", " + aui + ", " + sabtty);
          codesabAuiMap.put(codesab, aui);
          codesabTtyMap.put(codesab, sabtty);
        }
      }
    }

    // Cache relationship info
    try (final PushBackReader reader = readers.getReader(RrfReaders.Keys.MRREL); ) {
      // Iterate over relationships
      while ((line = reader.readLine()) != null) {

        // CUI1,AUI1,STYPE1,REL,CUI2,AUI2,STYPE2,RELA,RUI,SRUI,
        // SAB,SL,RG,DIR,SUPPRESS,CVF

        // Split line
        final String[] fields = line.split("\\|", -1);

        final String cui1 = fields[0];
        final String aui1 = fields[1];
        final String rel = fields[3];
        final String rela = fields[7];
        final String cui2 = fields[4];
        final String aui2 = fields[5];
        final String sab = fields[10];
        final String reldir = fields[13];

        // Skip "mapping" rela values
        if (rela.contains("map")) {
          continue;
        }

        // Keep only non-suppressed entries
        if (!fields[14].equals("N")) {
          continue;
        }

        // In PAR, AUI1 is the child, AUI2 is the parent
        if (rel.equals("PAR")) {

          if (!chdParMap.containsKey(aui1)) {
            chdParMap.put(aui1, new HashSet<>());
          }
          chdParMap.get(aui1).add(aui2);

          if (!parChdMap.containsKey(aui2)) {
            parChdMap.put(aui2, new HashSet<>());
          }
          parChdMap.get(aui2).add(aui1);
        }

        // Keep only entries matching terminology (or input cuis)
        // Do this after par/chd so we keep all par/chd
        if (!inputCuis.contains(cui1)
            && !inputCuis.contains(cui2)
            && !terminologies.contains("*")
            && !terminologies.contains(sab)
            && !sab.equals("SRC")) {
          continue;
        }

        // Other rels
        if (!rel.equals("CHD")) {

          // Skip self-referential. some par/chd maybe CUI self-ref
          if (cui1.equals(cui2)) {
            continue;
          }

          // Skip different STYPE1/STYPE2
          if (!fields[2].equals(fields[6])) {
            continue;
          }

          // Skip not-asserted-directional rels (blank or "Y" are ok)
          if (reldir.equals("N")) {
            continue;
          }

          // skip if the rel is for a terminology we don't care about
          if (!terminologies.contains(sab)) {
            continue;
          }

          if (!otherMap.containsKey(aui2)) {
            otherMap.put(aui2, new HashSet<>());
          }
          otherMap.get(aui2).add(aui1);
        }
      }
    }
  }

  /**
   * Sets the keep descendants.
   *
   * @param keepDescendants the keep descendants
   */
  public void setKeepDescendants(final boolean keepDescendants) {
    this.keepDescendants = keepDescendants;
  }

  /**
   * Sets the terminology.
   *
   * @return the terminologies
   */
  public Set<String> getTerminologies() {
    if (terminologies == null) {
      terminologies = new HashSet<>();
    }
    return terminologies;
  }

  /**
   * Sets the terminologies.
   *
   * @param terminologies the new terminologies
   */
  public void setTerminologies(final Set<String> terminologies) {
    this.terminologies = terminologies;
  }

  /**
   * Sets the distance one.
   *
   * @param distanceOne the distance one
   */
  public void setDistanceOne(final boolean distanceOne) {
    this.distanceOne = distanceOne;
  }

  /**
   * Sets the input path.
   *
   * @param inputPath the input path
   */
  public void setInputPath(final String inputPath) {
    this.inputPath = inputPath;
  }

  /**
   * Returns the descendant concepts.
   *
   * @param aui the concept
   * @return the descendant concepts
   */
  public Set<String> getDescendantsHelper(final String aui) {
    // Assumes no cycles
    final Set<String> descendants = new HashSet<>();
    if (!parChdMap.containsKey(aui)) {
      descendants.add(aui);
      return descendants;
    }
    for (final String chd : parChdMap.get(aui)) {
      descendants.addAll(getDescendantsHelper(chd));
    }
    return descendants;
  }

  /**
   * Sets the codes file.
   *
   * @param cuisFile the codes file
   */
  public void setCuisFile(final String cuisFile) {
    this.cuisFile = cuisFile;
  }
}
