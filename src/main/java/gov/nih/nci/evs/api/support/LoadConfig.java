package gov.nih.nci.evs.api.support;

public class LoadConfig {
  private boolean downloadOnly;
  private boolean skipDownload;
  private boolean realTime;

  public boolean isDownloadOnly() {
    return downloadOnly;
  }
  public void setDownloadOnly(boolean downloadOnly) {
    this.downloadOnly = downloadOnly;
  }
  public boolean isSkipDownload() {
    return skipDownload;
  }
  public void setSkipDownload(boolean skipDownload) {
    this.skipDownload = skipDownload;
  }
  public boolean isRealTime() {
    return realTime;
  }
  public void setRealTime(boolean realTime) {
    this.realTime = realTime;
  }
}
