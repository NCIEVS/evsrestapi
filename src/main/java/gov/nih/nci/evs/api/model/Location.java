
package gov.nih.nci.evs.api.model;

/**
 * Represents a path in a hierarchy (as a list of concepts with a direction
 * flag).
 */
public class Location extends BaseModel {

  /** The latitude. */
  private String lat;

  /** The longitude. */
  private String lon;

  /**
   * Instantiates an empty {@link Location}.
   */
  public Location() { }

  /**
   * Instantiates a {@link Location} from the specified parameters.
   *
   * @param lat the latitude
   * @param lon the longitude
   */
  public Location(String lat, String lon) {
    this.lat = lat;
    this.lon = lon;
  }

  /**
   * Sets the lat.
   *
   * @param lat the lat
   */
  public void setLat(String lat) {
    this.lat = lat;
  }

  /**
   * Sets the longitude.
   *
   * @param lon the longitude
   */
  public void setLon(String lon) {
    this.lon = lon;
  }

  /**
   * Returns the latitude.
   *
   * @return the latitude
   */
  public String getLat() {
    return this.lat;
  }

  /**
   * Returns the longitude.
   *
   * @return the longitude
   */
  public String getLon() {
    return this.lon;
  }
}