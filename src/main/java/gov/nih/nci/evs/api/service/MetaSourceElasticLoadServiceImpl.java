
package gov.nih.nci.evs.api.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.Association;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Definition;
import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.model.Qualifier;
import gov.nih.nci.evs.api.model.Synonym;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.TerminologyMetadata;
import gov.nih.nci.evs.api.support.es.ElasticLoadConfig;
import gov.nih.nci.evs.api.support.es.ElasticObject;
import gov.nih.nci.evs.api.util.ConceptUtils;
import gov.nih.nci.evs.api.util.HierarchyUtils;
import gov.nih.nci.evs.api.util.PushBackReader;
import gov.nih.nci.evs.api.util.RrfReaders;

/**
 * The implementation for {@link MetaSourceElasticLoadServiceImpl}.
 */
@Service
public class MetaSourceElasticLoadServiceImpl extends BaseLoaderService {

  /** the logger *. */
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(MetaSourceElasticLoadServiceImpl.class);

  /** the concepts download location *. */
  @Value("${nci.evs.bulkload.conceptsDir}")
  private String CONCEPTS_OUT_DIR;

  /** the lock file name *. */
  @Value("${nci.evs.bulkload.lockFile}")
  private String LOCK_FILE;

  /** download batch size *. */
  @Value("${nci.evs.bulkload.downloadBatchSize}")
  private int DOWNLOAD_BATCH_SIZE;

  /** index batch size *. */
  @Value("${nci.evs.bulkload.indexBatchSize}")
  private int INDEX_BATCH_SIZE;

  /** file path *. */
  private File filepath;

  /** The name map. */
  private Map<String, String> nameMap = new HashMap<>();

  /** The name rank. */
  private Map<String, Integer> nameRankMap = new HashMap<>();

  /** The code cuis map. */
  private Map<String, Set<String>> codeCuisMap = new HashMap<>();

  /** The code auis map. */
  private Map<String, Set<String>> codeAuisMap = new HashMap<>();

  /** The aui code map. */
  private Map<String, String> auiCodeMap = new HashMap<>();

  /** The rui qual map. */
  private Map<String, Set<String>> ruiQualMap = new HashMap<>();

  /** The parent child. */
  private List<String> parentChild = new ArrayList<>();

  /** The src auis. */
  private Set<String> srcAuis = new HashSet<>();

  /** The code concept map. */
  private Map<String, Concept> codeConceptMap = new HashMap<>();

  /** The mapsets. */
  @SuppressWarnings("unused")
  private Map<String, String> mapsets = new HashMap<>();

  /** The maps. */
  private Map<String, Set<gov.nih.nci.evs.api.model.Map>> maps = new HashMap<>();

  /** The rui inverse map. */
  private Map<String, String> ruiInverseMap = new HashMap<>();

  /** The rel inverse map. */
  private Map<String, String> relInverseMap = new HashMap<>();

  /** The rela inverse map. */
  private Map<String, String> relaInverseMap = new HashMap<>();

  /** The tty map. */
  private Map<String, String> ttyMap = new HashMap<>();

  /** The rel map. */
  private Map<String, String> relMap = new HashMap<>();

  /** The atn map. */
  private Map<String, String> atnMap = new HashMap<>();

  /** The source map. */
  private Map<String, String> sourceMap = new HashMap<>();

  /** The column map. */
  private Map<String, String> colMap = new HashMap<>();

  /** The atn set. */
  private Set<String> atnSet = new HashSet<>();

  /** The rel set. */
  private Set<String> relSet = new HashSet<>();

  /** The qual set. */
  private Map<String, Set<String>> qualMap = new HashMap<>();

  /** The batch size. */
  private int batchSize = 0;

  /** The definition ct. */
  private int definitionCt = 0;

  /** the environment *. */
  @Autowired
  Environment env;

  /** The Elasticsearch operations service instance *. */
  @Autowired
  ElasticOperationsService operationsService;

  /**
   * Returns the filepath.
   *
   * @return filepath
   */
  public File getFilepath() {
    return filepath;
  }

  /**
   * Sets the filepath.
   *
   * @param filepath the filepath
   */
  public void setFilepath(File filepath) {
    this.filepath = filepath;
  }

  /**
   * Cache preferred names and REL/RELA inverses.
   *
   * @param terminology the terminology
   * @throws Exception the exception
   */
  private void cacheMaps(Terminology terminology) throws Exception {

    // Don't run a second time
    if (!colMap.isEmpty()) {
      return;
    }

    logger.info("  START cache maps");
    RrfReaders readers = new RrfReaders(this.getFilepath());
    readers.openOriginalReaders("MR");
    try (final PushBackReader mrconso = readers.getReader(RrfReaders.Keys.MRCONSO);
        final PushBackReader mrrel = readers.getReader(RrfReaders.Keys.MRREL);
        final PushBackReader mrsat = readers.getReader(RrfReaders.Keys.MRSAT);
        final PushBackReader mrmap = readers.getReader(RrfReaders.Keys.MRMAP);
        final PushBackReader mrdoc = readers.getReader(RrfReaders.Keys.MRDOC);
        final PushBackReader mrcols = readers.getReader(RrfReaders.Keys.MRCOLS);) {

      if (terminology.getMetadata().getPreferredTermTypes().isEmpty()) {
        throw new Exception("No preferred term types are specified, meaning no preferred names can be chosen");
      }

      String line = null;
      // Loop through concept lines until we reach "the end"
      while ((line = mrconso.readLine()) != null) {
        final String[] fields = line.split("\\|", -1);

        // CUI,LAT,TS,LUI,STT,SUI,ISPREF,AUI,SAUI,SCUI,SDUI,SAB,TTY,CODE,STR,SRL,SUPPRESS,CVF

        // Determine the code
        final String code = getCode(terminology, fields);

        // Skip NOCODE
        if (code.equals("NOCODE")) {
          continue;
        }

        // Cache SRC AUIs
        if (fields[11].equals("SRC")) {
          srcAuis.add(fields[7]);
        }

        // Correct MTHICD9 to ICD9CM
        if (fields[11].equals("MTHICD9")) {
          fields[11] = "ICD9CM";
        }

        // Cache concept preferred names
        int rank = terminology.getMetadata().getPreferredTermTypes().indexOf(fields[12]);
        if (sabMatch(fields[11], terminology.getTerminology()) && rank != -1) {
          // If the new rank is lower than the previously assigned rank
          // or we've never assigned a name, assign the name
          // Lower index in preferred term types is better rank
          if (!nameRankMap.containsKey(code) || (nameRankMap.containsKey(code) && rank < nameRankMap.get(code))) {
            nameMap.put(code, fields[14]);
            nameRankMap.put(code, rank);
          }
        }

        // If this MRCONSO entry has a "code" in this CUI, remember it
        if (sabMatch(fields[11], terminology.getTerminology())) {
          if (!codeCuisMap.containsKey(code)) {
            codeCuisMap.put(code, new HashSet<>());
            codeAuisMap.put(code, new HashSet<>());
          }
          codeCuisMap.get(code).add(fields[0]);
          // AUIs that are part of a code
          codeAuisMap.get(code).add(fields[7]);
          // CODE for an AUI
          auiCodeMap.put(fields[7], code);
        }

        // Cache mapsets
        if (fields[11].equals("SNOMEDCT_US") && fields[12].equals("XM") && fields[14].contains("ICD10")) {
          // |SNOMEDCT_US_2020_09_01 to ICD10CM_2021 Mappings
          // |SNOMEDCT_US_2022_03_01 to ICD10_2016 Mappings
          mapsets.put(fields[0], fields[14].replaceFirst(".* to ([^ ]+).*", "$1"));
        }

      }

      // Loop through map lines and cache map objects
      while ((line = mrmap.readLine()) != null) {
        // MAPSETCUI,MAPSETSAB,MAPSUBSETID,MAPRANK,MAPID,MAPSID,FROMID,FROMSID,
        // FROMEXPR,FROMTYPE,FROMRULE,FROMRES,REL,RELA,TOID,TOSID,TOEXPR,TOTYPE,
        // TORULE,TORES,MAPRULE,MAPRES,MAPTYPE,MAPATN,MAPATV,CVF
        final String[] fields = line.split("\\|", -1);

        // Skip non-matching SAB lines
        if (!sabMatch(fields[1], terminology.getTerminology())) {
          continue;
        }

        // Skip map sets not being tracked
        if (!mapsets.containsKey(fields[0])) {
          continue;
        }

        // Skip empty maps (or maps to the empty target: 100051
        if (fields[16].isEmpty() || fields[16].equals("100051")) {
          continue;
        }

        // Skip entries with descendant rules
        // OK: IFA 445518008 &#x7C; Age at onset of clinical finding (observable entity) &#x7C;
        // OK: IFA 248152002 &#x7C; Female (finding) &#x7C;
        // OK: IFA 248153007 &#x7C; Male (finding) &#x7C; 
        if (fields[20].startsWith("IFA") &&
            !fields[20].startsWith("IFA 445518008") &&
            !fields[20].startsWith("IFA 248152002") &&
            !fields[20].startsWith("IFA 248153007")) {
          continue;
        }
                
        final gov.nih.nci.evs.api.model.Map map = new gov.nih.nci.evs.api.model.Map();
        map.setSourceCode(fields[8]);
        map.setSourceTerminology(fields[1]);
        map.setTargetCode(fields[16]);
        // map.setTargetName(nameMap.get(fields[16]));
        map.setTargetTermType("PT");
        map.setTargetTerminology(mapsets.get(fields[0]).split("_")[0]);
        map.setTargetTerminologyVersion(mapsets.get(fields[0]).split("_")[1]);
        map.setType(fields[12]);
        map.setGroup(fields[2]);
        map.setRank(fields[3]);
        map.setRule(fields[20]);

        // Fix target name if null
        if (map.getTargetName() == null) {
          map.setTargetName("Unable to determine name, code not present");
        }

        if (!maps.containsKey(map.getSourceCode())) {
          maps.put(map.getSourceCode(), new HashSet<>());
        }
        maps.get(map.getSourceCode()).add(map);
      }

      // Always read inverses from MRDOC
      while ((line = mrdoc.readLine()) != null) {
        final String[] fields = line.split("\\|", -1);
        if (fields[2].equals("rel_inverse")) {
          relInverseMap.put(fields[1], fields[3]);
        } else if (fields[2].equals("rela_inverse")) {
          relaInverseMap.put(fields[1], fields[3]);
        } else if (fields[0].equals("TTY") && fields[2].equals("expanded_form")) {
          ttyMap.put(fields[1], fields[3]);
        } else if (fields[0].equals("REL") && fields[2].equals("expanded_form")) {
          relMap.put(fields[1], fields[3]);
        } else if (fields[0].equals("ATN") && fields[2].equals("expanded_form")) {
          atnMap.put(fields[1], fields[3]);
        }
      }
      relaInverseMap.put("", "");

      // Always read column Metadata
      while ((line = mrcols.readLine()) != null) {
        final String[] fields = line.split("\\|", -1);
        // ATN|Attribute name||2|10.94|62|MRSAT.RRF|varchar(100)|
        colMap.put(fields[0], fields[1]);
      }

      // Handle RUI attributes
      while ((line = mrsat.readLine()) != null) {
        final String[] fields = line.split("\\|", -1);
        // e.g.
        // C4227882|||R114264673|RUI||AT148954088||SMQ_TERM_LEVEL|MDR|5|N||

        if (!sabMatch(fields[9], terminology.getTerminology())) {
          continue;
        }

        // Skip certain high-volume not very useful qualifiers
        if (fields[4].equals("RUI") && !fields[8].equals("MODIFIER_ID")
            && !fields[8].equals("CHARACTERISTIC_TYPE_ID")) {
          final String atn = fields[8];
          final String atv = fields[10];
          if (!qualMap.containsKey(atn)) {
            qualMap.put(atn, new HashSet<>());
          }
          qualMap.get(atn).add(atv);

          if (!ruiQualMap.containsKey(fields[3])) {
            ruiQualMap.put(fields[3], new HashSet<>(4));
          }
          ruiQualMap.get(fields[3]).add(atn + "|" + atv);
        }

      }

      final Map<String, String> helper = new HashMap<>();
      // Prepare parent/child relationships for getHierarchyUtils
      while ((line = mrrel.readLine()) != null) {
        final String[] fields = line.split("\\|", -1);
        // e.g.
        // C4229995|A5970983|AUI|PAR|C4229995|A5963886|AUI||R91875256||MDR|MDR|||N||
        // Looking for matching terminology
        // REL=PAR
        // parent is not an SRC atom
        // codes of AUI1 and AUI2 do not match (self-referential)

        // Correct MTHICD9 to ICD9CM
        if (fields[10].equals("MTHICD9")) {
          fields[10] = "ICD9CM";
        }

        if (!sabMatch(fields[10], terminology.getTerminology())) {
          continue;
        }

        if (fields[3].equals("PAR") && !srcAuis.contains(fields[5])
            && !auiCodeMap.get(fields[5]).equals(auiCodeMap.get(fields[1]))) {
          final StringBuffer str = new StringBuffer();
          str.append(auiCodeMap.get(fields[5]));
          str.append("\t");
          str.append(nameMap.get(auiCodeMap.get(fields[5])));
          str.append("\t");
          str.append(auiCodeMap.get(fields[1]));
          str.append("\t");
          str.append(nameMap.get(auiCodeMap.get(fields[1])));
          str.append("\n");
          parentChild.add(str.toString());
        }

        // for non parent/children, build inverse rui map
        // (but only if ruiQualMap has entries)
        if (ruiQualMap.size() > 0 && !fields[3].equals("PAR") && !fields[3].equals("CHD")) {
          // C4229995|A5970983|AUI|PAR|C4229995|A5963886|AUI||R91875256||MDR|MDR|||N||
          final String key = fields[1] + fields[5] + fields[3] + fields[7];
          helper.put(key, fields[8]);
          final String key2 = fields[5] + fields[1] + relInverseMap.get(fields[3]) + relaInverseMap.get(fields[7]);
          if (helper.containsKey(key2)) {
            ruiInverseMap.put(fields[8], helper.get(key2));
            ruiInverseMap.put(helper.get(key2), fields[8]);
            helper.remove(key);
            helper.remove(key2);
          }
        }
      }

      // Remove any entries ruiInverseMap where the inverse RUIs do not have RUI
      // qualifiers
      for (final String key : new HashSet<>(ruiInverseMap.keySet())) {
        final String value = ruiInverseMap.get(key);
        if (!ruiQualMap.containsKey(value)) {
          ruiInverseMap.remove(key);
        }
      }

    } finally {
      readers.closeReaders();
    }
    logger.info("  FINISH cache maps");
    logger.info("    mapsets = " + mapsets.size());
    logger.info("    maps = " + mapsets.size());
    logger.info("    parentChild = " + parentChild.size());
    logger.info("    ruiInverseMap = " + ruiInverseMap.size());
    logger.info("    ruiQualMap = " + ruiQualMap.size());

  }

  /* see superclass */
  @Override
  public int loadConcepts(ElasticLoadConfig config, Terminology terminology, HierarchyUtils hierarchy)
    throws Exception {
    logger.info("Loading Concepts (index batch size = " + INDEX_BATCH_SIZE + ")");

    // Put the mapping
    boolean result = operationsService.createIndex(terminology.getIndexName(), config.isForceDeleteIndex());
    if (result) {
      operationsService.getElasticsearchOperations().putMapping(terminology.getIndexName(),
          ElasticOperationsService.CONCEPT_TYPE, Concept.class);
    }

    // Cache the concept preferred names so when we resolve relationships we
    // know the "related" name
    cacheMaps(terminology);
    RrfReaders readers = new RrfReaders(this.getFilepath());
    readers.openOriginalReaders("MR");
    try (final PushBackReader mrconso = readers.getReader(RrfReaders.Keys.MRCONSO);
        final PushBackReader mrdef = readers.getReader(RrfReaders.Keys.MRDEF);
        final PushBackReader mrsty = readers.getReader(RrfReaders.Keys.MRSTY);
        final PushBackReader mrsat = readers.getReader(RrfReaders.Keys.MRSAT);
        final PushBackReader mrrel = readers.getReader(RrfReaders.Keys.MRREL);) {

      // Set up vars
      String line = null;
      String prevCui = null;
      Set<String> codes = new HashSet<>(4);
      Concept concept = null;
      List<Concept> batch = new ArrayList<>();
      int totalConcepts = 0;
      int sourceConcepts = 0;

      // CUI,LAT,TS,LUI,STT,SUI,ISPREF,AUI,SAUI,SCUI,SDUI,SAB,TTY,CODE,STR,SRL,SUPPRESS,CVF

      // Loop through concept lines until we reach "the end"
      while ((line = mrconso.readLine()) != null) {
        final String[] fields = line.split("\\|", -1);

        // Correct MTHICD9 to ICD9CM
        if (fields[11].equals("MTHICD9")) {
          fields[11] = "ICD9CM";
        }

        final String cui = fields[0];
        final String sab = fields[11];

        
        // Test assumption that the file is in order (when considering
        // |)
        if (prevCui != null && (cui + "|").compareTo(prevCui + "|") < 0) {
          throw new Exception("File is unexpectedly out of order = " + prevCui + ", " + cui);
        }

        // check if we've hit a new concept then process the other files for the
        // "prevCui"
        if (!cui.equals(prevCui)) {

          // Count total number of CUIs processed
          if (totalConcepts++ % 5000 == 0) {
            logger.info("    count = " + totalConcepts);
          }

          // Process data from this CUI
          handleDefinitions(terminology, codes, mrdef, prevCui);
          handleSemanticTypes(codes, mrsty, prevCui);
          handleAttributes(terminology, codes, mrsat, prevCui);
          handleRelationships(hierarchy, terminology, codes, mrrel, prevCui);

          // Remove the prevCui from the codeCuisMap because
          // they have all been processed
          for (final String code : codes) {
            codeCuisMap.get(code).remove(prevCui);

            // If codeCuisMap is completely processed, handle the concept
            if (codeCuisMap.get(code).isEmpty()) {

              concept = codeConceptMap.get(code);
              concept.setLeaf(concept.getChildren().size() > 0);
              concept.setDescendants(hierarchy.getDescendants(code));

              concept.setPaths(hierarchy.getPaths(terminology, concept.getCode()));
              // Clear space/memory
              hierarchy.getPathsMap(terminology).remove(concept.getCode());
              if (maps.containsKey(concept.getCode())) {
                concept.getMaps().addAll(maps.get(concept.getCode()));
              }
              handleConcept(terminology, concept, batch, false, terminology.getIndexName());
              maps.remove(concept.getCode());

              // Count number of source concepts
              if (sourceConcepts++ % 5000 == 0) {
                logger.info("    " + terminology.getTerminology() + " = " + sourceConcepts);
              }

              // Free up some memory
              codeConceptMap.remove(code);
              codeCuisMap.remove(code);
              codeAuisMap.remove(code);

            }
          }

          // Start codes again for the next CUI
          codes = new HashSet<>(4);

        }

        // If this is a line for the source we care about
        if (sabMatch(sab, terminology.getTerminology())) {

          // Lookup the code
          final String code = getCode(terminology, fields);

          // Skip NOCODE
          if (code.equals("NOCODE")) {
            continue;
          }

          if (!codeConceptMap.containsKey(code)) {
            concept = new Concept();
            concept.setCode(code);
            concept.setName(nameMap.get(code));
            concept.setNormName(ConceptUtils.normalize(nameMap.get(code)));
            concept.setTerminology(terminology.getTerminology());
            concept.setVersion(terminology.getVersion());
            concept.setActive(true);
            if (concept.getName() == null) {
              throw new Exception("Unable to find preferred name, likely TTY issue (" + terminology.getTerminology()
                  + ".json) = " + terminology.getTerminology().toUpperCase() + ", " + code);
            }
            codeConceptMap.put(code, concept);
          }

          // Each line of MRCONSO is a synonym
          concept = codeConceptMap.get(code);
          addSynonym(terminology, concept, fields);

          codes.add(code);

        }

        // Save prev cui for next round
        prevCui = cui;
      }

      // Process the final concept and all it's connected data
      handleDefinitions(terminology, codes, mrdef, prevCui);
      handleSemanticTypes(codes, mrsty, prevCui);
      handleAttributes(terminology, codes, mrsat, prevCui);
      handleRelationships(hierarchy, terminology, codes, mrrel, prevCui);

      if (codes.size() > 0) {
        for (final String code : codes) {
          codeCuisMap.get(code).remove(prevCui);

          // If codeCuisMap is completely processed, handle the concept
          if (codeCuisMap.get(code).isEmpty()) {

            concept = codeConceptMap.get(code);
            concept.setLeaf(concept.getChildren().size() > 0);
            concept.setDescendants(hierarchy.getDescendants(code));
            concept.setPaths(hierarchy.getPaths(terminology, concept.getCode()));
            if (maps.containsKey(concept.getCode())) {
              concept.getMaps().addAll(maps.get(concept.getCode()));
            }
            handleConcept(terminology, concept, batch, true, terminology.getIndexName());
            maps.remove(concept.getCode());

            // Count number of source concepts
            if (sourceConcepts++ % 5000 == 0) {
              logger.info("    " + terminology.getTerminology() + " = " + sourceConcepts);
            }

          } else {
            // If not, then something went wrong, should not be possible
            throw new Exception("Unexpected additional CUIs for code not covered = " + codeCuisMap.get(code));
          }
        }
      }
      // force index
      else {
        logger.info("    BATCH index = " + batchSize + ", " + batch.size());
        operationsService.bulkIndex(new ArrayList<>(batch), terminology.getIndexName(),
            ElasticOperationsService.CONCEPT_TYPE, Concept.class);
      }

      totalConcepts++;

      logger.info("TOTAL concepts = " + totalConcepts);
      logger.info("TOTAL " + terminology.getTerminology() + " = " + sourceConcepts);

      return totalConcepts;

    } finally {
      readers.closeReaders();
    }

  }

  /**
   * Adds the synonym.
   *
   * @param terminology the terminology
   * @param concept the concept
   * @param fields the fields
   * @throws Exception the exception
   */
  public void addSynonym(final Terminology terminology, Concept concept, final String[] fields) throws Exception {
    // Each line of MRCONSO is a synonym
    final Synonym sy = new Synonym();
    // Put the AUI into the URI field for the time being (to be removed later)
    sy.setUri(fields[7]);
    sy.setType(
        fields[14].equals(concept.getName()) && terminology.getMetadata().getPreferredTermTypes().contains(fields[12])
            ? "Preferred_Name" : "Synonym");
    if (!concept.getCode().equals(fields[13])) {
      sy.setCode(fields[13]);
    }
    sy.setSource(fields[11]);
    terminology.getMetadata().getSynonymSourceSet().add(fields[11]);
    sy.setTermType(fields[12]);
    terminology.getMetadata().getTermTypes().put(fields[12], ttyMap.get(fields[12]));
    sy.setName(fields[14]);
    sy.setNormName(ConceptUtils.normalize(fields[14]));
    
    if (fields[16].equals("O")) {
        sy.setActive(false);
    } else {
        sy.setActive(true);
    }
    
    concept.getSynonyms().add(sy);
  }

  /**
   * Handle semantic types.
   *
   * @param codes the codes
   * @param mrsty the mrsty
   * @param prevCui the prev cui
   * @throws Exception the exception
   */
  public void handleSemanticTypes(final Set<String> codes, final PushBackReader mrsty, final String prevCui)
    throws Exception {
    String line;
    Set<String> seen = new HashSet<>(4);
    while ((line = mrsty.readLine()) != null) {
      final String[] fields = line.split("\\|", -1);
      if (!fields[0].equals(prevCui)) {
        mrsty.push(line);
        break;
      }

      // For each concept related to this CUI, add the STY
      for (final String code : codes) {
        Concept concept = codeConceptMap.get(code);
        // hard code to Semantic_Typ
        buildProperty(concept, "Semantic_Type", fields[3], null);
        if (!seen.contains(code + fields[0])) {
          buildProperty(concept, "NCI_META_CUI", fields[0], null);
        }
        seen.add(code + fields[0]);
      }

    }
  }

  /**
   * Handle attributes.
   *
   * @param terminology the terminology
   * @param codes the codes
   * @param mrsat the mrsat
   * @param prevCui the prev cui
   * @throws Exception the exception
   */
  public void handleAttributes(final Terminology terminology, final Set<String> codes, final PushBackReader mrsat,
    final String prevCui) throws Exception {

    // Track seen to avoid duplicates
    final Set<String> seen = new HashSet<>();

    String line;
    while ((line = mrsat.readLine()) != null) {
      final String[] fields = line.split("\\|", -1);
      if (!fields[0].equals(prevCui)) {
        mrsat.push(line);
        break;
      }

      // Skip SUBSET_MEMBER
      if (fields[8].equals("SUBSET_MEMBER")) {
        continue;
      }

      // Skip non-matching SAB lines
      if (!sabMatch(fields[9], terminology.getTerminology())) {
        continue;
      }

      // CUI,LUI,SUI,METAUI,STYPE,CODE,ATUI,SATUI,ATN,SAB,ATV,SUPPRESS,CVF|
      final String atn = fields[8];
      final String sab = fields[9];
      final String atv = fields[10];

      // Handle non- RUI attributes as qualifiers on relationships
      if (!fields[4].equals("RUI")) {
        // Determine which concept this definition applies to
        Concept concept = null;
        for (final String code : codes) {
          // if matching AUI attribute
          if (codeAuisMap.get(code).contains(fields[3])) {
            concept = codeConceptMap.get(code);
            break;
          }
        }

        // This is unexpected
        if (concept == null) {
          throw new Exception("Concept for attribute cannot be resolved = " + line);
        }

        // Handle AUI attributes as qualifiers on synonyms
        if (fields[4].equals("AUI")) {
          // Add entry to qualifier map for metadata
          if (!qualMap.containsKey(atn)) {
            qualMap.put(atn, new HashSet<>());
          }
          qualMap.get(atn).add(atv);

          // find synonym
          final Synonym syn =
              concept.getSynonyms().stream().filter(s -> s.getUri().equals(fields[3])).findFirst().orElse(null);
          if (syn == null) {
            throw new Exception("Synonym for attribute cannot be resolved = " + line);
          }
          syn.getQualifiers().add(new Qualifier(atn, ConceptUtils.substr(atv, 1000)));
        }

        // Otherwise handle as a concept attribute
        else {
          // De-duplicate concept attributes
          final String key = concept.getCode() + sab + atn + atv;
          if (seen.contains(key)) {
            continue;
          }
          seen.add(key);

          buildProperty(concept, atn, atv, sab);
          
          if (atn.equalsIgnoreCase("active") && atv.equals("0")) {
              setConceptInactive(terminology, concept);
          }
        }
      }
    }
  }

  /**
   * Handle building concept property.
   *
   * @param concept the concept
   * @param type the property type
   * @param value the property value
   * @param source the source
   * @return the property
   */
  private void buildProperty(final Concept concept, final String type, final String value, final String source) {
    final Property prop = new Property();
    prop.setValue(value);
    prop.setType(type);
    atnSet.add(type);
    if (source != null) {
      prop.setSource(source.toString());
    }
    // Skip if STY exists already
    if (type.equals("Semantic_Type") && !concept.getProperties().stream()
        .filter(p -> p.getType().equals("Semantic_Type") && p.getValue().equals(value)).findFirst().isEmpty()) {
      return;
    }
    
    concept.getProperties().add(prop);
  }

  /**
   * Handle definitions.
   *
   * @param terminology the terminology
   * @param codes the codes
   * @param mrdef the mrdef
   * @param prevCui the prev cui
   * @throws Exception the exception
   */
  private void handleDefinitions(final Terminology terminology, final Set<String> codes, final PushBackReader mrdef,
    final String prevCui) throws Exception {
    String line;

    // CUI,AUI,ATUI,SATUI,SAB,DEF,SUPPRESS,CVF
    while ((line = mrdef.readLine()) != null) {
      final String[] fields = line.split("\\|", -1);
      if (!fields[0].equals(prevCui)) {
        mrdef.push(line);
        break;
      }

      // Skip non-matching SAB lines
      if (!sabMatch(fields[4], terminology.getTerminology())) {
        continue;
      }

      // Determine which concept this definition applies to
      final Concept concept = codeConceptMap.get(auiCodeMap.get(fields[1]));

      // should not be possible
      if (concept == null) {
        throw new Exception("Unable to find concept for MRDEF line = " + line);
      }

      // handle definitions
      buildDefinition(terminology, concept, fields);
    }
  }

  /**
   * Handle building concept definition.
   *
   * @param terminology the terminology
   * @param concept the concept
   * @param fields the fields
   * @return the definition
   */
  private void buildDefinition(final Terminology terminology, final Concept concept, final String[] fields) {
    final String definition = fields[5];
    final String source = fields[4];

    final Definition def = new Definition();
    def.setDefinition(definition);
    def.setSource(source);
    def.setType("DEFINITION");
    terminology.getMetadata().getDefinitionSourceSet().add(source);
    concept.getDefinitions().add(def);
    definitionCt++;
  }
  
  /**
   * Handle relationships.
   *
   * @param hierarchy the hierarchy
   * @param terminology the terminology
   * @param codes the codes
   * @param mrrel the mrrel
   * @param prevCui the prev cui
   * @throws Exception the exception
   */
  public void handleRelationships(final HierarchyUtils hierarchy, final Terminology terminology,
    final Set<String> codes, final PushBackReader mrrel, final String prevCui) throws Exception {
    Set<String> seen = new HashSet<>();
    String line;
    while ((line = mrrel.readLine()) != null) {
      final String[] fields = line.split("\\|", -1);
      if (!fields[0].equals(prevCui)) {
        mrrel.push(line);
        break;
      }

      // Correct MTHICD9 to ICD9CM
      if (fields[10].equals("MTHICD9")) {
        fields[10] = "ICD9CM";
      }

      // Skip non-matching SAB lines
      if (!sabMatch(fields[10], terminology.getTerminology())) {
        continue;
      }

      // CUI1,AUI1,STYPE1,REL,CUI2,AUI2,STYPE2,RELA,RUI,SRUI,SAB,SL,RG,DIR,SUPPRESS,CVF
      // e.g.
      // C0000039|A13650014|AUI|RO|C0364349|A10774117|AUI|measures|R108296692||LNC|LNC|||N||
      final String rel = fields[3];
      final String rela = fields[7];

      // Skip certain situations
      if (rel.equals("SY") || rel.equals("AQ") || rel.equals("QB") || rel.equals("BRO") || rel.equals("BRN")
          || rel.equals("BRB") || rel.equals("XR")) {
        continue;
      }

      // Skip SRC AUI lines
      if (srcAuis.contains(fields[1]) || srcAuis.contains(fields[5])) {
        continue;
      }

      // Determine concept1 (lookup the concept for the AUI and it must still be
      // a code we are processing)
      Concept concept1 = null;
      for (final String code : codes) {
        // if matching STYPE1/AUI1 attribute
        if (!fields[1].isEmpty() && codeAuisMap.get(code).contains(fields[1])) {
          concept1 = codeConceptMap.get(code);
          break;
        }
      }

      // This is unexpected, except for cross-terminology mapping rels
      if (concept1 == null && !rela.contains("map")) {
        throw new Exception("AUI1 for relationship cannot be resolved = " + line);
      }

      // Determine concept2 (lookup the code for the AUI)
      String concept2 = null;
      // if matching STYPE2/AUI2 attribute
      if (!fields[5].isEmpty() && auiCodeMap.containsKey(fields[5])) {
        concept2 = auiCodeMap.get(fields[5]);

        // This is unexpected, except for cross-terminology mapping rels
        if (concept2 == null && !rela.contains("map")) {
          throw new Exception("AUI2 for relationship cannot be resolved = " + line);
        }
      }

      // Skip combinations already seen (all will have the same SAB here)
      final String key = concept1.getCode() + "," + concept2 + "," + rel + rela;
      if (seen.contains(key)) {
        continue;
      }
      seen.add(key);

      // CUI2 "child of" CUI1
      // CUI1 has child CUI2
      if (rel.equals("CHD")) {
        buildChild(hierarchy, concept1, fields);
      }

      // CUI2 "parent of" CUI1
      // CUI1 has parent CUI2
      else if (rel.equals("PAR")) {
        buildParent(concept1, fields);
      }

      // Other "association" and "inverse association"
      else {

        buildAssociations(concept1, fields);
      }

    }
  }

  /**
   * Handle building concept property.
   *
   * @param concept the concept
   * @param fields the fields
   * @return the property
   * @throws Exception the exception
   */
  private void buildParent(final Concept concept, final String[] fields) throws Exception {
    final Concept parent = buildParentChildHelper(concept, fields);
    if (!concept.getCode().equals(parent.getCode())) {
      concept.getParents().add(parent);
    }
  }

  /**
   * Builds the child.
   *
   * @param hierarchy the hierarchy
   * @param concept the concept
   * @param fields the fields
   * @throws Exception the exception
   */
  private void buildChild(HierarchyUtils hierarchy, Concept concept, final String[] fields) throws Exception {
    final Concept child = buildParentChildHelper(concept, fields);
    if (!concept.getCode().equals(child.getCode())) {
      concept.getChildren().add(child);
      // Compute "leaf"
      child.setLeaf(hierarchy.getChildNodes(child.getCode(), 1).isEmpty());
    }
  }

  /**
   * Builds the parent/child helper.
   *
   * @param concept the concept
   * @param fields the fields
   * @return the concept
   * @throws Exception the exception
   */
  private Concept buildParentChildHelper(final Concept concept, final String[] fields) throws Exception {

    // C2584594|A9570455|SCUI|PAR|C0203464|A3803453|SCUI|inverse_isa|R105418833|4727926024|SNOMEDCT_US|SNOMEDCT_US||N|N||
    final String aui2 = fields[5];
    final String rela = fields[7];

    final Concept concept2 = new Concept();
    concept2.setCode(auiCodeMap.get(aui2));
    concept2.setName(nameMap.get(auiCodeMap.get(aui2)));
    // concept2.setTerminology(concept.getTerminology());
    // concept2.setVersion(concept.getVersion());
    concept2.setLeaf(null);
    if (!rela.isEmpty()) {
      concept2.getQualifiers().add(new Qualifier("RELA", relaInverseMap.get(rela)));
    }

    return concept2;
  }

  /**
   * Builds the associations.
   *
   * @param concept the concept
   * @param fields the fields
   * @throws Exception the exception
   */
  private void buildAssociations(final Concept concept, final String[] fields) throws Exception {

    // e.g.
    // C0000039|A13650014|AUI|RO|C0364349|A10774117|AUI|measures|R108296692||LNC|LNC|||N||
    final String rel = fields[3];
    final String aui2 = fields[5];
    final String rela = fields[7];
    final String sab = fields[10];

    // Build and add association
    final Association association = new Association();
    association.setType(relInverseMap.get(rel));
    association.setRelatedCode(auiCodeMap.get(aui2));
    association.setRelatedName(nameMap.get(auiCodeMap.get(aui2)));
    association.setSource(sab);
    if (!rela.isEmpty()) {
      association.getQualifiers().add(new Qualifier("RELA", relaInverseMap.get(rela)));
    }

    final String inverseRui = ruiInverseMap.get(fields[8]);
    if (ruiQualMap.containsKey(inverseRui)) {
      for (final String atnatv : ruiQualMap.get(inverseRui)) {
        final String[] parts = atnatv.split("\\|");
        association.getQualifiers().add(new Qualifier(parts[0], ConceptUtils.substr(parts[1], 1000)));
      }
    }

    // Avoid self-referential rels
    if (!association.getRelatedCode().equals(concept.getCode())) {
      concept.getAssociations().add(association);
    }

    // Build and add an inverse association
    final Association iassociation = new Association();
    iassociation.setType(rel);
    relSet.add(rel);

    iassociation.setRelatedCode(auiCodeMap.get(aui2));
    iassociation.setRelatedName(nameMap.get(auiCodeMap.get(aui2)));
    iassociation.setSource(sab);
    if (!rela.isEmpty()) {
      iassociation.getQualifiers().add(new Qualifier("RELA", rela));
    }

    // Add RUI qualifiers on inverse associations also
    if (ruiQualMap.containsKey(inverseRui)) {
      for (final String atnatv : ruiQualMap.get(inverseRui)) {
        final String[] parts = atnatv.split("\\|");
        iassociation.getQualifiers().add(new Qualifier(parts[0], ConceptUtils.substr(parts[1], 1000)));
      }
      ruiQualMap.remove(inverseRui);
      ruiInverseMap.remove(fields[8]);
    }

    // avoid self-referential rels
    if (!iassociation.getRelatedCode().equals(concept.getCode())) {
      concept.getInverseAssociations().add(iassociation);
    }

  }

  /**
   * Handle building MRDOC property metadata.
   *
   * @param terminology the terminology
   * @param code the code
   * @param name the name
   * @return the concept
   */
  private Concept buildMetadata(final Terminology terminology, final String code, final String name) {
    final Concept propMeta = new Concept();
    propMeta.setCode(code);
    propMeta.setName(code);
    propMeta.setTerminology(terminology.getTerminology());
    propMeta.setVersion(terminology.getVersion());

    // property synonym info
    final Synonym propMetaSyn = new Synonym();
    propMetaSyn.setType("Preferred_Name");
    propMetaSyn.setName(code);
    // add synonym as list to property
    propMeta.getSynonyms().add(propMetaSyn);
    final Definition def = new Definition();
    def.setType("DEFINITION");
    def.setDefinition(name);
    // add definition
    propMeta.getDefinitions().add(def);

    return propMeta;
  }

  /* see superclass */
  @Override
  public void loadObjects(ElasticLoadConfig config, Terminology terminology, HierarchyUtils hierarchy)
    throws Exception {

    final String indexName = terminology.getObjectIndexName();

    logger.info("Loading Elastic Objects");
    logger.debug("object index name: {}", indexName);

    // Create index
    boolean result = operationsService.createIndex(indexName, config.isForceDeleteIndex());
    logger.debug("index result: {}", result);

    //
    // Handle hierarchy
    //
    if (terminology.getMetadata().getHierarchy() != null && terminology.getMetadata().getHierarchy()) {
      ElasticObject hierarchyObject = new ElasticObject("hierarchy");
      hierarchyObject.setHierarchy(hierarchy);
      operationsService.index(hierarchyObject, indexName, ElasticOperationsService.OBJECT_TYPE, ElasticObject.class);
      logger.info("  Hierarchy loaded");
    } else {
      logger.info("  Hierarchy skipped");
    }

    //
    // Handle associations
    //
    final ElasticObject associations = new ElasticObject("associations");
    // MRSAT: association metadata for MRREL
    for (final String rel : relSet) {
      associations.getConcepts().add(buildMetadata(terminology, rel, relMap.get(rel)));
    }
    operationsService.index(associations, indexName, ElasticOperationsService.OBJECT_TYPE, ElasticObject.class);

    // Hanlde "concept statuses" - n/a

    //
    // Handle definitionSources - n/a - handled inline
    //

    //
    // Handle definitionTypes
    //
    final ElasticObject defTypes = new ElasticObject("definitionTypes");
    // Only create definition metadata if there are actualy definitions
    if (definitionCt > 0) {
      defTypes.getConcepts().add(buildMetadata(terminology, "DEFINITION", "Definition"));
    }
    operationsService.index(defTypes, indexName, ElasticOperationsService.OBJECT_TYPE, ElasticObject.class);

    //
    // Handle properties
    //
    final ElasticObject properties = new ElasticObject("properties");

    // NCI_META_CUI
    atnMap.put("NCI_META_CUI", "CUI assignment in the NCI Metatehsaurus");

    // MRSTY: Semantic_Type property
    atnMap.put("Semantic_Type", "Semantic Type");

    // MRSAT: property metadata for MRSAT
    for (final String atn : atnSet) {
      properties.getConcepts().add(buildMetadata(terminology, atn, atnMap.get(atn)));
    }

    operationsService.index(properties, indexName, ElasticOperationsService.OBJECT_TYPE, ElasticObject.class);

    //
    // Handle qualifiers
    //
    final ElasticObject qualifiers = new ElasticObject("qualifiers");

    // qualifiers to build - from relationships
    // removed for now: "AUI1", "STYPE1", "AUI2", "STYPE2", "SUPPRESS", "RG",
    // "DIR"
    for (final String col : new String[] {
        "RELA"// , "RG", "DIR"
    }) {
      qualifiers.getConcepts().add(buildMetadata(terminology, col, colMap.get(col)));
    }
    for (final String qual : qualMap.keySet()) {
      qualifiers.getConcepts().add(buildMetadata(terminology, qual, atnMap.get(qual)));
    }
    qualifiers.setMap(qualMap);
    operationsService.index(qualifiers, indexName, ElasticOperationsService.OBJECT_TYPE, ElasticObject.class);

    //
    // Handle roles - n/a
    //
    final ElasticObject roles = new ElasticObject("roles");
    operationsService.index(roles, indexName, ElasticOperationsService.OBJECT_TYPE, ElasticObject.class);

    //
    // Handle subsets - n/a
    //
    final ElasticObject subsets = new ElasticObject("subsets");
    operationsService.index(subsets, indexName, ElasticOperationsService.OBJECT_TYPE, ElasticObject.class);

    //
    // Handle synonymSources - n/a - handled inline
    //

    //
    // Handle synonymTypes
    //
    final ElasticObject syTypes = new ElasticObject("synonymTypes");
    syTypes.getConcepts().add(buildMetadata(terminology, "Preferred_Name", "Preferred name"));
    syTypes.getConcepts().add(buildMetadata(terminology, "Synonym", "Synonym"));
    operationsService.index(syTypes, indexName, ElasticOperationsService.OBJECT_TYPE, ElasticObject.class);

    //
    // Handle termTypes - n/a - handled inline
    //

  }

  /**
   * Handle concept.
   *
   * @param concept the concept
   * @param batch the batch
   * @param flag the flag
   * @param indexName the index name
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private void handleConcept(Terminology terminology, Concept concept, List<Concept> batch, boolean flag, String indexName) throws IOException {

    boolean hasActiveSynonyms = false;
    
    // check for inactive concepts
    for (final Synonym synonym: concept.getSynonyms()) {
        
        if (synonym.isActive()) {
            
            hasActiveSynonyms = true;
            break;
        }
    }
    
    if (!hasActiveSynonyms) {
        setConceptInactive(terminology, concept);
    }
    
    // Remove synonym "uris" as no longer needed
    concept.getSynonyms().forEach(s -> s.setUri(null));

    // Put concept lists in natural sort order
    concept.sortLists();

    batch.add(concept);

    int conceptSize = concept.toString().length();
    if (conceptSize > 10000000) {
      logger.info("    BIG concept = " + concept.getCode() + " = " + concept.toString().length());
    }
    batchSize += conceptSize;

    // Send to elasticsearch if the overall batch size > 9M
    if (flag || batchSize > 9000000) {
      // Log the bytes and number of concepts
      logger.info("    BATCH index = " + batchSize + ", " + batch.size());
      operationsService.bulkIndex(new ArrayList<>(batch), indexName, ElasticOperationsService.CONCEPT_TYPE,
          Concept.class);
      batch.clear();
      batchSize = 0;
    }

  }

  /* see superclass */
  @Override
  public Terminology getTerminology(ApplicationContext app, ElasticLoadConfig config, String filepath,
    String terminology, boolean forceDelete) throws Exception {
    // will eventually read and build differently
    this.setFilepath(new File(filepath));
    if (!this.getFilepath().exists()) {
      throw new Exception("Given filepath does not exist = " + filepath);
    }
    try (InputStream input = new FileInputStream(this.getFilepath() + "/release.dat");
        final BufferedReader in = new BufferedReader(new FileReader(this.getFilepath() + "/MRSAB.RRF"));) {

      String line;
      Terminology term = new Terminology();
      while ((line = in.readLine()) != null) {
        // VCUI,RCUI,VSAB,RSAB,SON,SF,SVER,VSTART,VEND,IMETA,RMETA,SLC,SCC,SRL,
        // TFR,CFR,CXTY,TTYL,ATNL,LAT,CENC,CURVER,SABIN,SSN,SCIT
        final String[] fields = line.split("\\|", -1);

        if (terminology.equals(fields[3]) && !fields[0].isEmpty()) {
          sourceMap.put(fields[3], fields[4]);
          term.setTerminology(terminology.toLowerCase());
          term.setVersion(fields[6]);
          // No info about the date
          term.setDate(null);
          if (line != null) {
            // term.setName(line.split("\\|", -1)[4]);
            term.setDescription(line.split("\\|", -1)[24]);
          }
          term.setGraph(null);
          term.setSource(null);
          term.setTerminologyVersion(term.getTerminology() + "_" + term.getVersion());
          term.setIndexName("concept_" + term.getTerminologyVersion().toLowerCase());
          term.setLatest(true);
          term.setSparqlFlag(false);
          break;
        }
      }

      // Attempt to read the config, if anything goes wrong
      // the config file is probably not there
      try {
        // Load from config
        final JsonNode node = getMetadataAsNode(terminology.toLowerCase());
        final TerminologyMetadata metadata = new ObjectMapper().treeToValue(node, TerminologyMetadata.class);

        // Set term name and description
        term.setName(metadata.getUiLabel() + " " + term.getVersion());
        if (term.getDescription() == null || term.getDescription().isEmpty() && node.get("description") != null) {
          term.setDescription(node.get("description").asText());
        }

        metadata.setLoader("rrf");
        metadata.setSources(sourceMap);
        metadata.setSourceCt(1);
        metadata.setWelcomeText(getWelcomeText(terminology.toLowerCase()));
        term.setMetadata(metadata);
        
      } catch (Exception e) {
        throw new Exception("Unexpected error trying to load metadata = " + applicationProperties.getConfigBaseUri(),
            e);
      }

      return term;
    } catch (IOException ex) {
      throw new Exception("Could not load terminology ncim");
    }

  }

  /* see superclass */
  @Override
  public HierarchyUtils getHierarchyUtils(Terminology term) throws Exception {
    cacheMaps(term);
    final HierarchyUtils utils = new HierarchyUtils(term, parentChild);
    parentChild = null;
    return utils;
  }

  /**
   * Clean stale indexes.
   *
   * @param terminology the terminology
   * @throws Exception the exception
   */
  @Override
  public void cleanStaleIndexes(final Terminology terminology) throws Exception {
    // do nothing - override superclass behavior
  }

  /**
   * Returns the code.
   *
   * @param terminology the terminology
   * @param fields the fields
   * @return the code
   * @throws Exception the exception
   */
  private String getCode(final Terminology terminology, final String[] fields) throws Exception {
    if (terminology.getMetadata().getMetaConceptField().equals("CODE")) {
      return fields[13];
    } else if (terminology.getMetadata().getMetaConceptField().equals("SCUI")) {
      return fields[9];
    } else if (terminology.getMetadata().getMetaConceptField().equals("SDUI")) {
      return fields[10];
    } else {
      throw new Exception("Missing or incorrect metaConceptField = " + terminology.getMetadata().getMetaConceptField());
    }
  }

  /**
   * Indicates whether the two terminology arguments match. Mostly this is about being equal, but
   * SNOMEDCT_US<==>snomedct is an exception.
   *
   * @param sab1 the sab 1
   * @param sab2 the sab 2
   * @return true, if successful
   */
  private boolean sabMatch(final String sab1, final String sab2) {
    return sab1.toLowerCase().equals(sab2) || (sab1.equals("SNOMEDCT_US") && sab2.equals("snomedct"));
  }

}
