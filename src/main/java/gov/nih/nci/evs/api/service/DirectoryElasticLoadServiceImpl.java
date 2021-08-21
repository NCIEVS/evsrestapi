
package gov.nih.nci.evs.api.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
 *
 * @author Arun
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
        final PushBackReader mrdoc = readers.getReader(RrfReaders.Keys.MRDOC);) {

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
        }
      }
      relaInverseMap.put("", "");

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
          handleDefinitions(concept, mrdef, prevCui);
          handleSemanticTypes(concept, mrsty, prevCui);
          handleAttributes(concept, mrsat, prevCui);
          handleRelationships(concept, mrrel, prevCui);
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
        final Synonym syn = new Synonym();
        if (!fields[13].equals("NOCODE")) {
          syn.setCode(fields[10]);
        }
        syn.setSource(fields[11]);
        syn.setTermGroup(fields[12]);
        syn.setName(fields[14]);
        concept.getSynonyms().add(syn);

        // Save prev cui for next round
        prevCui = cui;
      }

      // Process the final concept and all it's connected data
      handleDefinitions(concept, mrdef, prevCui);
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
    if (source != null) {
      prop.setSource(source.toString());
    }
    concept.getProperties().add(prop);
  }

  /**
   * Handle definitions.
   *
   * @param concept the concept
   * @param mrdef the mrdef
   * @param prevCui the prev cui
   * @throws Exception the exception
   */
  private void handleDefinitions(final Concept concept, final PushBackReader mrdef,
    final String prevCui) throws Exception {
    String line;
    while ((line = mrdef.readLine()) != null) {
      final String[] fields = line.split("\\|", -1);
      if (!fields[0].equals(prevCui)) {
        mrdef.push(line);
        break;
      }
      // handle definitions
      buildDefinition(concept, fields);
    }
  }

  /**
   * Handle building concept definition.
   *
   * @param concept the concept
   * @param fields the fields
   * @return the definition
   */
  private void buildDefinition(final Concept concept, final String[] fields) {
    final String definition = fields[5];
    final String source = fields[4];

    final Definition newDef = new Definition();
    newDef.setDefinition(definition);
    newDef.setSource(source);
    concept.getDefinitions().add(newDef);
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
      if (fromCode.equals(toCode) || rel.equals("SY")) {
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
    concept2.getQualifiers().add(new Qualifier("RELA", rela));
    if (!aui2.isEmpty()) {
      concept2.getQualifiers().add(new Qualifier("AUI2", aui2));
      concept2.getQualifiers().add(new Qualifier("STYPE2", stype2));
    }
    concept2.setSource(sab);
    // concept2.getQualifiers().add(new Qualifier("RUI", rui));
    // if (!srui.isEmpty()) {
    // concept2.getQualifiers().add(new Qualifier("SRUI", srui));
    // }
    concept2.getQualifiers().add(new Qualifier("RG", rg));
    concept2.getQualifiers().add(new Qualifier("DIR", dir));
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
    association.getQualifiers().add(new Qualifier("RELA", rela));
    // association.getQualifiers().add(new Qualifier("RUI", rui));
    // if (!srui.isEmpty()) {
    // association.getQualifiers().add(new Qualifier("SRUI", srui));
    // }
    association.getQualifiers().add(new Qualifier("RG", rg));
    association.getQualifiers().add(new Qualifier("DIR", dir));
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
   * @param propertyInfo the property info
   * @return the concept
   */
  private Concept buildMRDOCMetadata(final Terminology terminology, final String propertyInfo) {
    Concept propMeta = new Concept();
    String[] splitPropInfo = propertyInfo.split("\\|", -1);
    propMeta.setCode(splitPropInfo[1]);
    propMeta.setName(splitPropInfo[3]);
    propMeta.setTerminology(terminology.getTerminology());
    propMeta.setVersion(terminology.getVersion());

    // property synonym info
    Synonym propMetaSyn = new Synonym();
    propMetaSyn.setName(splitPropInfo[3]);
    propMetaSyn.setType("Preferred_Name");
    // add synonym as list to property
    propMeta.setSynonyms(Arrays.asList(propMetaSyn));
    return propMeta;
  }

  /* see superclass */
  @Override
  public void loadObjects(ElasticLoadConfig config, Terminology terminology,
    HierarchyUtils hierarchy) throws Exception {
    String indexName = terminology.getObjectIndexName();
    logger.info("Loading Elastic Objects");
    logger.info("object index name: {}", indexName);
    boolean result = operationsService.createIndex(indexName, config.isForceDeleteIndex());
    logger.debug("index result: {}", result);

    // first level property info
    Concept semType = new Concept("ncim", "STY", "Semantic_Type");
    semType.setTerminology(terminology.getTerminology());
    semType.setVersion(terminology.getVersion());

    // property synonym info
    Synonym semTypeSyn = new Synonym();
    semTypeSyn.setName("Semantic_Type");
    semTypeSyn.setType("Preferred_Name");
    // add synonym as list to property
    semType.setSynonyms(Arrays.asList(semTypeSyn));

    ElasticObject propertiesObject = new ElasticObject("properties");
    List<Concept> propertiesMetaDataList = new ArrayList<>();
    propertiesMetaDataList.add(semType);

    // add the MRDOC properties
    RrfReaders readers = new RrfReaders(this.getFilepath());
    readers.openOriginalReaders("MR");
    String MRDOCMetaLine = null;
    LinkedHashMap<String, String> propsMeta = new LinkedHashMap<>();
    try (final PushBackReader MRDOCMeta = readers.getReader(RrfReaders.Keys.MRDOC);) {
      while ((MRDOCMetaLine = MRDOCMeta.readLine()) != null) {
        if (!propsMeta.containsKey(MRDOCMetaLine.split("\\|", -1)[1])) {
          // only get unique properties
          propsMeta.put(MRDOCMetaLine.split("\\|", -1)[1], MRDOCMetaLine);
        }
      }
    }

    propsMeta.forEach((key, value) -> {
      // add properties to the list
      propertiesMetaDataList.add(buildMRDOCMetadata(terminology, value));
    });
    propertiesObject = new ElasticObject("properties");
    propertiesObject.setConcepts(propertiesMetaDataList);

    operationsService.index(propertiesObject, indexName, ElasticOperationsService.OBJECT_TYPE,
        ElasticObject.class);

    // TODO: additional qualifiers to build - from relationships
    final ElasticObject qualifiersObject = new ElasticObject("qualifiers");
    // if (!aui1.isEmpty()) {
    // association.getQualifiers().add(new Qualifier("AUI1", aui1));
    // association.getQualifiers().add(new Property("STYPE1", stype1));
    // }
    // if (!aui2.isEmpty()) {
    // association.getQualifiers().add(new Qualifier("AUI2", aui1));
    // association.getQualifiers().add(new Qualifier("STYPE2", stype2));
    // }
    // association.getQualifiers().add(new Qualifier("RELA", rela));
    // association.getQualifiers().add(new Qualifier("RG", rg));
    // association.getQualifiers().add(new Qualifier("DIR", dir));
    // association.getQualifiers().add(new Qualifier("SUPPRESS", suppress));

    operationsService.index(qualifiersObject, indexName, ElasticOperationsService.OBJECT_TYPE,
        ElasticObject.class);

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
      while ((line = in.readLine()) != null) {
        if (line.split("\\|", -1)[3].equals("NCIMTH")) {
          break;
        }
      }
      Properties p = new Properties();
      p.load(input);
      Terminology term = new Terminology();
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

      logger.info("  ADD terminology = " + term);
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
