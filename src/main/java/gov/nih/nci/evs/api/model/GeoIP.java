
package gov.nih.nci.evs.api.model;

/**
 * Represents a path in a hierarchy (as a list of concepts with a direction
 * flag).
 */
public class GeoIP extends BaseModel {

  /** The city name. */
  private String city_name;

  /** the state name */
  private String state_name;

  /** The country name. */
  private String country_name;

  /** The continent name. */
  private String continent_name;

  /** The location object */
  private Location location;

  /**
   * Instantiates an empty {@link GeoIP}.
   */
  public GeoIP() {
  }

  /**
   * Instantiates a {@link GeoIP} from the specified parameters.
   *
   * @param city_name the city name
   * @param state_name the state name
   * @param country_name the country name
   * @param continent_name the continent name
   * @param location the lat/long object
   */
  public GeoIP(String city_name, String country_name, String state_name, String continent_name, Location location) {
    this.city_name = city_name;
    this.state_name = state_name;
    this.country_name = country_name;
    this.continent_name = continent_name;
    this.location = location;
  }

  /**
   * Sets the city name.
   *
   * @param city_name the city name
   */
  public void setCity_name(String city_name) {
    this.city_name = city_name;
  }

  /**
   * Sets the state name.
   *
   * @param state_name the state name
   */
  public void setState_name(String state_name) {
    this.state_name = state_name;
  }

  /**
   * Sets the country name.
   *
   * @param country_name the country name
   */
  public void setCountry_name(String country_name) {
    this.country_name = country_name;
  }

  /**
   * Sets the continent name.
   *
   * @param continent_name the continent name
   */
  public void setContinent_name(String continent_name) {
    this.continent_name = continent_name;
  }

  /**
   * Sets the location.
   *
   * @param location the location
   */
  public void setLocation(Location location) {
    this.location = location;
  }

  /**
   * Returns the city name.
   *
   * @return the city name
   */
  public String getCity_name() {
    return this.city_name;
  }

  /**
   * Returns the state name.
   *
   * @return state_name the state name
   */
  public String getState_name() {
    return this.state_name;
  }

  /**
   * Returns the country name.
   *
   * @return the country_name
   */
  public String getCountry_name() {
    return this.country_name;
  }

  /**
   * Returns the continent name.
   *
   * @return the continent name
   */
  public String getContinent_name() {
    return continent_name;
  }

  /**
   * Gets the latLon.
   *
   * @return the latLon
   */
  public Location getLocation() {
    return location;
  }

}