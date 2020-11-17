
package gov.nih.nci.evs.api.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Definition;
import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.model.Synonym;
import gov.nih.nci.evs.api.model.Terminology;
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

  /** the environment *. */
  @Autowired
  Environment env;

  /** The Elasticsearch operations service instance *. */
  @Autowired
  ElasticOperationsService operationsService;

  /** The sparql query manager service. */
  @Autowired
  private SparqlQueryManagerService sparqlQueryManagerService;

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

  /* see superclass */
  @Override
  public int loadConcepts(ElasticLoadConfig config, Terminology terminology,
    HierarchyUtils hierarchy, CommandLine cmd) throws Exception {
    RrfReaders readers = new RrfReaders(this.getFilepath());
    readers.openOriginalReaders("MR");
    try (final PushBackReader reader = readers.getReader(RrfReaders.Keys.MRCONSO);
        final PushBackReader readerDef = readers.getReader(RrfReaders.Keys.MRDEF);
        final PushBackReader readerProp = readers.getReader(RrfReaders.Keys.MRSTY);) {
      String line = null;
      String defLine = null;
      String propLine = null;
      Concept concept = new Concept();
      List<Concept> batch = new ArrayList<>();
      String prevCui = null;
      List<Synonym> synList = new ArrayList<Synonym>();
      List<Definition> defList = new ArrayList<Definition>();
      List<Property> propList = new ArrayList<Property>();
      int totalConcepts = 0;
      while ((line = reader.readLine()) != null) {
        final String[] fields = line.split("\\|", -1);
        final String cui = fields[0];
        // Test assumption that the file is in order (when considering
        // |)
        if (prevCui != null && (cui + "|").compareTo(prevCui + "|") < 0) {
          throw new Exception("File is unexpectedly out of order = " + prevCui + ", " + cui);
        }
        // check if we've hit a new concept
        if (!cui.equals(prevCui)) {
          while ((defLine = readerDef.readLine()) != null) {
            if (!defLine.split("\\|", -1)[0].equals(prevCui)) {
              readerDef.push(defLine);
              break;
            }
            // handle definitions
            defList.add(buildDefinition(defLine.split("\\|", -1)[5], defLine.split("\\|", -1)[4]));
          }
          while ((propLine = readerProp.readLine()) != null) {
            if (!propLine.split("\\|", -1)[0].equals(prevCui)) {
              readerProp.push(propLine);
              break;
            }
            // handle properties (hard code to Semantic_Type for now)
            propList.add(buildProperty("Semantic_Type", propLine.split("\\|", -1)[3]));
          }
          handleConcept(concept, batch, false, terminology.getIndexName(), synList, defList,
              propList);
          if (totalConcepts++ % 5000 == 0) {
            logger.info("    count = " + totalConcepts);
          }
          synList = new ArrayList<Synonym>();
          defList = new ArrayList<Definition>();
          propList = new ArrayList<Property>();
          concept = new Concept();
          concept.setCode(cui);
          concept.setTerminology(terminology.getTerminology());
          concept.setVersion(terminology.getVersion());
          // NO hierarchy, so this is best set to null
          concept.setLeaf(null);

        }
        // find the proper name
        if (fields[2].equalsIgnoreCase("P") && fields[4].equalsIgnoreCase("PF")
            && fields[6].equalsIgnoreCase("Y")) {
          concept.setName(fields[14]);
        }
        // build out synonym in concept
        Synonym syn = new Synonym();
        if (!fields[13].equals("NOCODE"))
          syn.setCode(fields[10]);
        syn.setSource(fields[11]);
        syn.setTermGroup(fields[12]);
        syn.setName(fields[14]);
        synList.add(syn);
        prevCui = cui;
      }
      // make sure to deal with the last concept in file
      if (defLine != null)
        defList.add(buildDefinition(defLine.split("\\|", -1)[5], defLine.split("\\|", -1)[4]));
      if (propLine != null)
        propList.add(buildProperty("Semantic_Type", propLine.split("\\|", -1)[3]));
      handleConcept(concept, batch, true, terminology.getIndexName(), synList, defList, propList);
      totalConcepts++;
      return totalConcepts;
    } finally {
      readers.closeReaders();
    }

  }

  /**
   * Handle building concept property.
   *
   * @param type the property type
   * @param value the property value
   */
  private Property buildProperty(String type, String value) {
    Property newProp = new Property();
    newProp.setValue(value);
    newProp.setType(type);
    return newProp;
  }

  /**
   * Handle building concept definition.
   *
   * @param definition the definition text
   * @param source the definition source
   */
  private Definition buildDefinition(String definition, String source) {
    Definition newDef = new Definition();
    newDef.setDefinition(definition);
    newDef.setSource(source);
    return newDef;
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
    semType.setVersion("202008");
    // property synonym info
    Synonym semTypeSyn = new Synonym();
    semTypeSyn.setName("Semantic_Type");
    semTypeSyn.setType("Preferred_Name");
    // add synonym as list to property
    semType.setSynonyms(Arrays.asList(semTypeSyn));
    ElasticObject propertiesObject = new ElasticObject("properties");
    // add properties to the object
    propertiesObject.setConcepts(Arrays.asList(semType));

    operationsService.index(propertiesObject, indexName, ElasticOperationsService.OBJECT_TYPE,
        ElasticObject.class);

    // TODO: figure out indexing
  }

  /**
   * Handle concept.
   *
   * @param concept the concept
   * @param batch the batch
   * @param flag the flag
   * @param indexName the index name
   * @param synList the syn list
   * @param defList
   * @param propList
   * @throws IOException Signals that an I/O exception has occurred.
   */
  private void handleConcept(Concept concept, List<Concept> batch, boolean flag, String indexName,
    List<Synonym> synList, List<Definition> defList, List<Property> propList) throws IOException {
    concept.setSynonyms(synList);
    concept.setDefinitions(defList);
    concept.setProperties(propList);
    batch.add(concept);
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
      if (forceDelete) {
        logger.info("DELETE TERMINOLOGY = " + term.getIndexName());
        findAndDeleteTerminology(term.getIndexName());
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
   * @throws Exception the exception
   */
  @Override
  public void cleanStaleIndexes() throws Exception {
    // do nothing
  }
}
