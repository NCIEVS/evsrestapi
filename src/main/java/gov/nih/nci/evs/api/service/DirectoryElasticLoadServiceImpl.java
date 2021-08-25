
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
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

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
import gov.nih.nci.evs.api.util.HierarchyUtils;
import gov.nih.nci.evs.api.util.PushBackReader;
import gov.nih.nci.evs.api.util.RrfReaders;

/**
 * The implementation for {@link DirectoryElasticLoadServiceImpl}.
 */
@Service
public class DirectoryElasticLoadServiceImpl extends BaseLoaderService {

  /** the logger *. */
  @SuppressWarnings("unused")
  private static final Logger logger =
      LoggerFactory.getLogger(DirectoryElasticLoadServiceImpl.class);

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
   * @throws Exception the exception
   */
  private void cacheMaps() throws Exception {

    logger.info("  START cache maps");
    RrfReaders readers = new RrfReaders(this.getFilepath());
    readers.openOriginalReaders("MR");
    try (final PushBackReader mrconso = readers.getReader(RrfReaders.Keys.MRCONSO);
        final PushBackReader mrdoc = readers.getReader(RrfReaders.Keys.MRDOC);
        final PushBackReader mrcols = readers.getReader(RrfReaders.Keys.MRCOLS);) {

      String line = null;
      // Loop through concept lines until we reach "the end"
      while ((line = mrconso.readLine()) != null) {
        final String[] fields = line.split("\\|", -1);

        if (fields[2].equalsIgnoreCase("P") && fields[4].equalsIgnoreCase("PF")
            && fields[6].equalsIgnoreCase("Y")) {
          // Save the name map to dereference it while processing relationships
          nameMap.put(fields[0], fields[14]);
        }
      }

      // read inverses from MRDOC
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

      // Column Metadata
      while ((line = mrcols.readLine()) != null) {
        final String[] fields = line.split("\\|", -1);
        // ATN|Attribute name||2|10.94|62|MRSAT.RRF|varchar(100)|
        colMap.put(fields[0], fields[1]);
      }

    } finally {
      readers.closeReaders();
    }
    logger.info("  FINISH cache maps");

  }

  /* see superclass */
  @Override
  public int loadConcepts(ElasticLoadConfig config, Terminology terminology,
    HierarchyUtils hierarchy) throws Exception {
    logger.info("Loading Concepts");

    // Put the mapping
    boolean result =
        operationsService.createIndex(terminology.getIndexName(), config.isForceDeleteIndex());
    if (result) {
      operationsService.getElasticsearchOperations().putMapping(terminology.getIndexName(),
          ElasticOperationsService.CONCEPT_TYPE, Concept.class);
    }

    // Cache the concept preferred names so when we resolve relationships we
    // know the "related" name
    cacheMaps();

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
      Concept concept = new Concept();
      List<Concept> batch = new ArrayList<>();
      int totalConcepts = 0;

      // Loop through concept lines until we reach "the end"
      while ((line = mrconso.readLine()) != null) {
        final String[] fields = line.split("\\|", -1);
        final String cui = fields[0];
        // Test assumption that the file is in order (when considering
        // |)
        if (prevCui != null && (cui + "|").compareTo(prevCui + "|") < 0) {
          throw new Exception("File is unexpectedly out of order = " + prevCui + ", " + cui);
        }

        // check if we've hit a new concept then process the other files for the
        // "prevCui"
        if (!cui.equals(prevCui)) {

          // Process data
          handleDefinitions(terminology, concept, mrdef, prevCui);
          handleSemanticTypes(concept, mrsty, prevCui);
          handleAttributes(concept, mrsat, prevCui);
          handleRelationships(concept, mrrel, prevCui);
          // There are not useful mappings in the UMLS at this point in time
          // handleMapping(concept, mrmap, prevCui);
          handleConcept(concept, batch, false, terminology.getIndexName());

          if (totalConcepts++ % 5000 == 0) {
            logger.info("    count = " + totalConcepts);
          }

          // Prep concept for the next CUI
          concept = new Concept();

        }

        // If we haven't set up the concept yet, do so now
        if (concept.getCode() == null) {
          concept.setCode(cui);
          concept.setName(nameMap.get(cui));
          concept.setTerminology(terminology.getTerminology());
          concept.setVersion(terminology.getVersion());
          // NO hierarchies for NCIM concepts, so set leaf to null
          concept.setLeaf(null);
        }

        // Each line of MRCONSO is a synonym
        final Synonym sy = new Synonym();
        sy.setType(fields[14].equals(concept.getName()) ? "Preferred_Name" : "Synonym");
        if (!fields[13].equals("NOCODE")) {
          sy.setCode(fields[10]);
        }
        sy.setSource(fields[11]);
        terminology.getMetadata().getSynonymSourceSet().add(fields[11]);
        sy.setTermGroup(fields[12]);
        terminology.getMetadata().getTermTypes().put(fields[12], ttyMap.get(fields[12]));
        sy.setName(fields[14]);
        concept.getSynonyms().add(sy);

        // Save prev cui for next round
        prevCui = cui;
      }

      // Process the final concept and all it's connected data
      handleDefinitions(terminology, concept, mrdef, prevCui);
      handleSemanticTypes(concept, mrsty, prevCui);
      handleAttributes(concept, mrsat, prevCui);
      handleRelationships(concept, mrrel, prevCui);
      handleConcept(concept, batch, true, terminology.getIndexName());

      totalConcepts++;

      return totalConcepts;

    } finally {
      readers.closeReaders();
    }

  }

  /**
   * Handle semantic types.
   *
   * @param concept the concept
   * @param mrsty the mrsty
   * @param prevCui the prev cui
   * @throws Exception the exception
   */
  public void handleSemanticTypes(final Concept concept, final PushBackReader mrsty,
    final String prevCui) throws Exception {
    String line;
    while ((line = mrsty.readLine()) != null) {
      final String[] fields = line.split("\\|", -1);
      if (!fields[0].equals(prevCui)) {
        mrsty.push(line);
        break;
      }
      // hard code to Semantic_Type for now
      buildProperty(concept, "Semantic_Type", fields[3], null);
    }
  }

  /**
   * Handle attributes.
   *
   * @param concept the concept
   * @param mrsat the mrsat
   * @param prevCui the prev cui
   * @throws Exception the exception
   */
  public void handleAttributes(final Concept concept, final PushBackReader mrsat,
    final String prevCui) throws Exception {
    String line;
    while ((line = mrsat.readLine()) != null) {
      final String[] fields = line.split("\\|", -1);
      if (!fields[0].equals(prevCui)) {
        mrsat.push(line);
        break;
      }
      // handle properties (include dynamic type)
      buildProperty(concept, fields[8], fields[9], fields[10]);
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
  private void buildProperty(final Concept concept, final String type, final String value,
    final String source) {
    final Property prop = new Property();
    prop.setValue(value);
    prop.setType(type);
    atnSet.add(type);
    if (source != null) {
      prop.setSource(source.toString());
    }
    concept.getProperties().add(prop);
  }

  /**
   * Handle definitions.
   *
   * @param terminology the terminology
   * @param concept the concept
   * @param mrdef the mrdef
   * @param prevCui the prev cui
   * @throws Exception the exception
   */
  private void handleDefinitions(final Terminology terminology, final Concept concept,
    final PushBackReader mrdef, final String prevCui) throws Exception {
    String line;
    while ((line = mrdef.readLine()) != null) {
      final String[] fields = line.split("\\|", -1);
      if (!fields[0].equals(prevCui)) {
        mrdef.push(line);
        break;
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
  private void buildDefinition(final Terminology terminology, final Concept concept,
    final String[] fields) {
    final String definition = fields[5];
    final String source = fields[4];

    final Definition def = new Definition();
    def.setDefinition(definition);
    def.setSource(source);
    def.setType("DEFINITION");
    terminology.getMetadata().getDefinitionSourceSet().add(source);
    concept.getDefinitions().add(def);
  }

  /**
   * Handle relationships.
   *
   * @param concept the concept
   * @param mrrel the mrrel
   * @param prevCui the prev cui
   * @throws Exception the exception
   */
  public void handleRelationships(final Concept concept, final PushBackReader mrrel,
    final String prevCui) throws Exception {
    String line;
    while ((line = mrrel.readLine()) != null) {
      final String[] fields = line.split("\\|", -1);
      if (!fields[0].equals(prevCui)) {
        mrrel.push(line);
        break;
      }

      // e.g.
      // C0000039|A13650014|AUI|RO|C0364349|A10774117|AUI|measures|R108296692||LNC|LNC|||N||
      final String rel = fields[3];
      final String fromCode = fields[4];
      final String toCode = fields[0];

      // Skip certain situations
      if (fromCode.equals(toCode) || rel.equals("SY") || rel.equals("AQ") || rel.equals("QB")
          || rel.equals("BRO")) {
        continue;
      }
      // CUI1 has parent CUI2
      else if (rel.equals("PAR")) {
        buildParent(concept, fields);
      }

      // CUI1 has child CUI2
      else if (rel.equals("CHD")) {
        buildChild(concept, fields);
      }

      // Other "association" and "inverse association"
      else {
        buildAssociations(concept, fields);
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
    concept.getParents().add(parent);
  }

  /**
   * Builds the child.
   *
   * @param concept the concept
   * @param fields the fields
   * @throws Exception the exception
   */
  private void buildChild(final Concept concept, final String[] fields) throws Exception {
    final Concept child = buildParentChildHelper(concept, fields);
    concept.getChildren().add(child);
  }

  /**
   * Builds the parent child helper.
   *
   * @param concept the concept
   * @param fields the fields
   * @return the concept
   * @throws Exception the exception
   */
  private Concept buildParentChildHelper(final Concept concept, final String[] fields)
    throws Exception {

    // C2584594|A9570455|SCUI|PAR|C0203464|A3803453|SCUI|inverse_isa|R105418833|4727926024|SNOMEDCT_US|SNOMEDCT_US||N|N||
    // final String cui1 = fields[0];
    final String aui1 = fields[1];
    final String stype1 = fields[2];
    // final String rel = fields[3];
    final String cui2 = fields[4];
    final String aui2 = fields[5];
    final String stype2 = fields[6];
    final String rela = fields[7];
    // final String rui = fields[8];
    // final String srui = fields[9];
    final String sab = fields[10];
    final String rg = fields[12];
    final String dir = fields[13];
    final String suppress = fields[14];
    // final String cvf = fields[15];

    final Concept concept2 = new Concept();
    concept2.setCode(cui2);
    concept2.setName(nameMap.get(cui2));
    concept2.setTerminology(concept.getTerminology());
    concept2.setVersion(concept2.getVersion());
    concept2.setLeaf(false);
    if (!aui1.isEmpty()) {
      concept2.getQualifiers().add(new Qualifier("AUI1", aui1));
      concept2.getQualifiers().add(new Qualifier("STYPE1", stype1));
    }
    if (!rela.isEmpty()) {
      concept2.getQualifiers().add(new Qualifier("RELA", rela));
    }
    if (!aui2.isEmpty()) {
      concept2.getQualifiers().add(new Qualifier("AUI2", aui2));
      concept2.getQualifiers().add(new Qualifier("STYPE2", stype2));
    }
    concept2.setSource(sab);
    // concept2.getQualifiers().add(new Qualifier("RUI", rui));
    // if (!srui.isEmpty()) {
    // concept2.getQualifiers().add(new Qualifier("SRUI", srui));
    // }
    if (!rg.isEmpty()) {
      concept2.getQualifiers().add(new Qualifier("RG", rg));
    }
    if (!dir.isEmpty()) {
      concept2.getQualifiers().add(new Qualifier("DIR", dir));
    }
    concept2.getQualifiers().add(new Qualifier("SUPPRESS", suppress));

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
    // final String cui1 = fields[0];
    final String aui1 = fields[1];
    final String stype1 = fields[2];
    final String rel = fields[3];
    final String cui2 = fields[4];
    final String aui2 = fields[5];
    final String stype2 = fields[6];
    final String rela = fields[7];
    // final String rui = fields[8];
    // final String srui = fields[9];
    final String sab = fields[10];
    final String rg = fields[12];
    final String dir = fields[13];
    final String suppress = fields[14];
    // final String cvf = fields[15];

    // Build and add association
    final Association association = new Association();
    association.setType(rel);
    relSet.add(rel);

    association.setRelatedCode(cui2);
    association.setRelatedName(nameMap.get(cui2));
    association.setSource(sab);
    if (!aui1.isEmpty()) {
      association.getQualifiers().add(new Qualifier("AUI1", aui1));
      association.getQualifiers().add(new Qualifier("STYPE1", stype1));
    }
    if (!aui2.isEmpty()) {
      association.getQualifiers().add(new Qualifier("AUI2", aui1));
      association.getQualifiers().add(new Qualifier("STYPE2", stype2));
    }
    if (!rela.isEmpty()) {
      association.getQualifiers().add(new Qualifier("RELA", rela));
    }
    // association.getQualifiers().add(new Qualifier("RUI", rui));
    // if (!srui.isEmpty()) {
    // association.getQualifiers().add(new Qualifier("SRUI", srui));
    // }
    if (!rg.isEmpty()) {
      association.getQualifiers().add(new Qualifier("RG", rg));
    }
    if (!dir.isEmpty()) {
      association.getQualifiers().add(new Qualifier("DIR", dir));
    }
    association.getQualifiers().add(new Qualifier("SUPPRESS", suppress));
    concept.getAssociations().add(association);

    // Build and add an inverse association
    final Association iassociation = new Association();
    iassociation.setType(relInverseMap.get(rel));
    iassociation.setRelatedCode(cui2);
    iassociation.setRelatedName(nameMap.get(cui2));
    iassociation.setSource(sab);
    if (!aui1.isEmpty()) {
      iassociation.getQualifiers().add(new Qualifier("AUI1", aui2));
      iassociation.getQualifiers().add(new Qualifier("STYPE1", stype1));
    }
    if (!aui2.isEmpty()) {
      iassociation.getQualifiers().add(new Qualifier("AUI2", aui2));
      iassociation.getQualifiers().add(new Qualifier("STYPE2", stype1));
    }
    iassociation.getQualifiers().add(new Qualifier("RELA", relaInverseMap.get(rela)));
    // RUI and SRUI in the other direction are different.
    iassociation.getQualifiers().add(new Qualifier("RG", rg));
    iassociation.getQualifiers().add(new Qualifier("DIR", dir));
    iassociation.getQualifiers().add(new Qualifier("SUPPRESS", suppress));
    concept.getInverseAssociations().add(iassociation);

  }

  /**
   * Handle building MRDOC property metadata.
   *
   * @param terminology the terminology
   * @param code the code
   * @param name the name
   * @return the concept
   */
  private Concept buildMetadata(final Terminology terminology, final String code,
    final String name) {
    final Concept propMeta = new Concept();
    propMeta.setCode(code);
    propMeta.setName(name);
    propMeta.setTerminology(terminology.getTerminology());
    propMeta.setVersion(terminology.getVersion());

    // property synonym info
    final Synonym propMetaSyn = new Synonym();
    propMetaSyn.setType("Preferred_Name");
    propMetaSyn.setName(name);
    // add synonym as list to property
    propMeta.getSynonyms().add(propMetaSyn);
    return propMeta;
  }

  /* see superclass */
  @Override
  public void loadObjects(ElasticLoadConfig config, Terminology terminology,
    HierarchyUtils hierarchy) throws Exception {

    final String indexName = terminology.getObjectIndexName();

    logger.info("Loading Elastic Objects");
    logger.debug("object index name: {}", indexName);

    // Create index
    boolean result = operationsService.createIndex(indexName, config.isForceDeleteIndex());
    logger.debug("index result: {}", result);

    // Use default elasticsearch mapping

    // Set the "sources" map of the terminology metadata
    terminology.getMetadata().setSources(sourceMap);

    //
    // Handle associations
    //
    final ElasticObject associations = new ElasticObject("associations");
    // MRSAT: association metadata for MRREL
    for (final String rel : relSet) {
      associations.getConcepts().add(buildMetadata(terminology, rel, relMap.get(rel)));
    }
    operationsService.index(associations, indexName, ElasticOperationsService.OBJECT_TYPE,
        ElasticObject.class);

    // Hanlde "concept statuses" - n/a

    //
    // Handle definitionSources - n/a - handled inline
    //

    //
    // Handle definitionTypes
    //
    final ElasticObject defTypes = new ElasticObject("definitionTypes");
    defTypes.getConcepts().add(buildMetadata(terminology, "DEFINITION", "Definition"));
    operationsService.index(defTypes, indexName, ElasticOperationsService.OBJECT_TYPE,
        ElasticObject.class);

    //
    // Handle properties
    //
    final ElasticObject properties = new ElasticObject("properties");

    // MRSTY: Semantic_Type property
    final Concept semType = new Concept(terminology.getTerminology(), "STY", "Semantic_Type");
    semType.setTerminology(terminology.getTerminology());
    semType.setVersion(terminology.getVersion());
    final Synonym semTypeSy = new Synonym();
    semTypeSy.setType("Preferred_Name");
    semTypeSy.setName("Semantic_Type");
    semType.getSynonyms().add(semTypeSy);
    properties.getConcepts().add(semType);

    // MRSAT: property metadata for MRSAT
    for (final String atn : atnSet) {
      properties.getConcepts().add(buildMetadata(terminology, atn, atnMap.get(atn)));
    }

    operationsService.index(properties, indexName, ElasticOperationsService.OBJECT_TYPE,
        ElasticObject.class);

    //
    // Handle qualifiers
    //
    final ElasticObject qualifiers = new ElasticObject("qualifiers");

    // qualifiers to build - from relationships
    for (final String col : new String[] {
        "AUI1", "STYPE1", "AUI2", "STYPE2", "RELA", "RG", "DIR", "SUPPRESS"
    }) {
      qualifiers.getConcepts().add(buildMetadata(terminology, col, colMap.get(col)));
    }
    operationsService.index(qualifiers, indexName, ElasticOperationsService.OBJECT_TYPE,
        ElasticObject.class);

    //
    // Handle roles - n/a
    //
    final ElasticObject roles = new ElasticObject("roles");
    operationsService.index(roles, indexName, ElasticOperationsService.OBJECT_TYPE,
        ElasticObject.class);

    //
    // Handle subsets - n/a
    //
    final ElasticObject subsets = new ElasticObject("subsets");
    operationsService.index(subsets, indexName, ElasticOperationsService.OBJECT_TYPE,
        ElasticObject.class);

    //
    // Handle synonymSources - n/a - handled inline
    //

    //
    // Handle synonymTypes
    //
    final ElasticObject syTypes = new ElasticObject("synonymTypes");
    syTypes.getConcepts().add(buildMetadata(terminology, "Preferred_Name", "Preferred name"));
    syTypes.getConcepts().add(buildMetadata(terminology, "Synonym", "Synonym"));
    operationsService.index(syTypes, indexName, ElasticOperationsService.OBJECT_TYPE,
        ElasticObject.class);

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
  private void handleConcept(Concept concept, List<Concept> batch, boolean flag, String indexName)
    throws IOException {
    batch.add(concept);

    // IF we've reached the index batch size, send it to elasticsearch
    if (flag || batch.size() == INDEX_BATCH_SIZE) {
      operationsService.bulkIndex(new ArrayList<>(batch), indexName,
          ElasticOperationsService.CONCEPT_TYPE, Concept.class);
      batch.clear();
    }
  }

  /* see superclass */
  @Override
  public Terminology getTerminology(ApplicationContext app, ElasticLoadConfig config,
    String filepath, String terminology, boolean forceDelete) throws Exception {
    // will eventually read and build differently
    this.setFilepath(new File(filepath));
    if (!this.getFilepath().exists()) {
      throw new Exception("Given filepath does not exist");
    }
    try (InputStream input = new FileInputStream(this.getFilepath() + "/release.dat");
        final BufferedReader in =
            new BufferedReader(new FileReader(this.getFilepath() + "/MRSAB.RRF"));) {

      String line;
      Terminology term = new Terminology();
      while ((line = in.readLine()) != null) {
        // VCUI,RCUI,VSAB,RSAB,SON,SF,SVER,VSTART,VEND,IMETA,RMETA,SLC,SCC,SRL,
        // TFR,CFR,CXTY,TTYL,ATNL,LAT,CENC,CURVER,SABIN,SSN,SCIT
        final String[] fields = line.split("\\|", -1);
        sourceMap.put(fields[3], fields[4]);

        if (fields[3].equals("NCIMTH")) {
          Properties p = new Properties();
          p.load(input);
          term.setTerminology(terminology);
          term.setVersion(p.getProperty("umls.release.name"));
          term.setDate(p.getProperty("umls.release.date"));
          if (line != null) {
            term.setName(line.split("\\|", -1)[4]);
            term.setDescription(line.split("\\|", -1)[24]);
          }
          term.setGraph(null);
          term.setSource(null);
          term.setTerminologyVersion(term.getTerminology() + "_" + term.getVersion());
          term.setIndexName("concept_" + term.getTerminologyVersion());
          term.setLatest(true);
          term.setSparqlFlag(false);
          // if (forceDelete) {
          // logger.info(" DELETE TERMINOLOGY = " + term.getIndexName());
          // findAndDeleteTerminology(term.getIndexName());
          // }
        }
      }

      // Attempt to read the config, if anything goes wrong
      // the config file is probably not there
      final String resource = "metadata/" + term.getTerminology() + ".json";
      try {
        TerminologyMetadata metadata = new ObjectMapper().readValue(IOUtils
            .toString(term.getClass().getClassLoader().getResourceAsStream(resource), "UTF-8"),
            TerminologyMetadata.class);
        term.setMetadata(metadata);
      } catch (Exception e) {
        throw new Exception("Unexpected error trying to load = " + resource, e);
      }

      return term;
    } catch (IOException ex) {
      throw new Exception("Could not load terminology ncim");
    }

  }

  /* see superclass */
  @Override
  public HierarchyUtils getHierarchyUtils(Terminology term) {
    // Don't need hierarchy utils in this indexing
    return null;
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

}
