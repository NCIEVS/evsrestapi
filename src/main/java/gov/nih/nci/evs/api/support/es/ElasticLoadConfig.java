package gov.nih.nci.evs.api.support.es;

/**
 * Elasticsearch index load input configuration
 * 
 * @author Arun
 *
 */
public class ElasticLoadConfig {
  /** download only and skip loading **/
  private boolean downloadOnly;
  
  /** skip download and load using downloaded files **/
  private boolean skipDownload;
  
  /** real time loading - directly from stardog **/
  private boolean realTime;
  
  /** delete index from elasticsearch, if already exists **/
  private boolean deleteIndex;

  public ElasticLoadConfig() {
    downloadOnly = false;
    skipDownload = false;
    realTime = true;
    deleteIndex = true;
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
  public boolean isDeleteIndex() {
    return deleteIndex;
  }
  
  /**
   * set delete index
   * 
   * @param deleteIndex delete index boolean
   */
  public void setDeleteIndex(boolean deleteIndex) {
    this.deleteIndex = deleteIndex;
  }
}
