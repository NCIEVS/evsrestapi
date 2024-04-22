package gov.nih.nci.evs.api.support.es;

/**
 * Elasticsearch index load input configuration.
 *
 * @author Arun
 */
public class ElasticLoadConfig {

  /** the terminology *. */
  private String terminology;

  /** force index deletion from elasticsearch, if already exists *. */
  private boolean forceDeleteIndex;

  /** folder location where concepts are to be download, if required *. */
  private String location;

  /** tells app to load concepts from directory *. */
  private boolean loadConceptsFromDir;

  /** tells app where concepts are located *. */
  private String conceptDir;

  /** Instantiates an empty {@link ElasticLoadConfig}. */
  public ElasticLoadConfig() {
    forceDeleteIndex = true;
  }

  /**
   * get terminology.
   *
   * @return the terminology
   */
  public String getTerminology() {
    return terminology;
  }

  /**
   * set terminology.
   *
   * @param terminology the terminology
   */
  public void setTerminology(String terminology) {
    this.terminology = terminology;
  }

  /**
   * get delete index.
   *
   * @return delete index boolean
   */
  public boolean isForceDeleteIndex() {
    return forceDeleteIndex;
  }

  /**
   * set force delete index.
   *
   * @param forceDeleteIndex delete index boolean
   */
  public void setForceDeleteIndex(boolean forceDeleteIndex) {
    this.forceDeleteIndex = forceDeleteIndex;
  }

  /**
   * get location.
   *
   * @return the location
   */
  public String getLocation() {
    return this.location;
  }

  /**
   * set the location.
   *
   * @param location the location where concepts are downloaded
   */
  public void setLocation(String location) {
    this.location = location;
  }

  /**
   * Indicates whether or not load concepts from dir is the case.
   *
   * @return the loadConceptsFromDir
   */
  public boolean isLoadConceptsFromDir() {
    return loadConceptsFromDir;
  }

  /**
   * Sets the load concepts from dir.
   *
   * @param loadConceptsFromDir the loadConceptsFromDir to set
   */
  public void setLoadConceptsFromDir(boolean loadConceptsFromDir) {
    this.loadConceptsFromDir = loadConceptsFromDir;
  }

  /**
   * Returns the concept dir.
   *
   * @return the conceptDir
   */
  public String getConceptDir() {
    return conceptDir;
  }

  /**
   * Sets the concept dir.
   *
   * @param conceptDir the conceptDir to set
   */
  public void setConceptDir(String conceptDir) {
    this.conceptDir = conceptDir;
  }
}
