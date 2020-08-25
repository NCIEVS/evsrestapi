package gov.nih.nci.evs.api.support.es;

/**
 * Elasticsearch index load input configuration
 * 
 * @author Arun
 *
 */
public class ElasticLoadConfig {
  
  /** the terminology **/
  private String terminology;
  
  /** download only and skip loading **/
  private boolean downloadOnly;
  
  /** skip download and load using downloaded files **/
  private boolean skipDownload;
  
  /** real time loading - directly from stardog **/
  private boolean realTime;
  
  /** force index deletion from elasticsearch, if already exists **/
  private boolean forceDeleteIndex;

  /** folder location where concepts are to be download, if required **/
  private String location;
  
  public ElasticLoadConfig() {
    downloadOnly = false;
    skipDownload = false;
    realTime = true;
    forceDeleteIndex = true;
  }

  /**
   * get terminology
   * 
   * @return the terminology
   */
  public String getTerminology() {
    return terminology;
  }

  /**
   * set terminology
   * 
   * @param terminology the terminology
   */
  public void setTerminology(String terminology) {
    this.terminology = terminology;
  }

  /**
   * get download only
   * 
   * @return download only boolean
   */
  public boolean isDownloadOnly() {
    return downloadOnly;
  }
  
  /**
   * set download only
   * 
   * @param downloadOnly download only boolean
   */
  public void setDownloadOnly(boolean downloadOnly) {
    this.downloadOnly = downloadOnly;
  }
  
  /**
   * get skip download
   * 
   * @return skip download boolean
   */
  public boolean isSkipDownload() {
    return skipDownload;
  }
  
  /**
   * set skip download
   * 
   * @param skipDownload skip download boolean
   */
  public void setSkipDownload(boolean skipDownload) {
    this.skipDownload = skipDownload;
  }
  
  /**
   * get real time
   * 
   * @return real time boolean
   */
  public boolean isRealTime() {
    return realTime;
  }
  
  /**
   * set real time
   * 
   * @param realTime real time boolean
   */
  public void setRealTime(boolean realTime) {
    this.realTime = realTime;
  }
  
  /**
   * get delete index
   * 
   * @return delete index boolean
   */
  public boolean isForceDeleteIndex() {
    return forceDeleteIndex;
  }
  
  /**
   * set force delete index
   * 
   * @param forceDeleteIndex delete index boolean
   */
  public void setForceDeleteIndex(boolean forceDeleteIndex) {
    this.forceDeleteIndex = forceDeleteIndex;
  }
  
  /**
   * get location
   * 
   * @return the location
   */
  public String getLocation() {
    return this.location;
  }
  
  /**
   * set the location
   * 
   * @param location the location where concepts are downloaded
   */
  public void setLocation(String location) {
    this.location = location;
  }
}
