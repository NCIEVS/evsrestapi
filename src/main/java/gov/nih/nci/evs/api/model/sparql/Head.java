package gov.nih.nci.evs.api.model.sparql;

import java.util.List;

/** The Class Property. */
public class Head {

  /** The value. */
  private List<String> vars;

  /**
   * Returns the value.
   *
   * @return the value
   */
  public List<String> getVars() {
    return vars;
  }

  /**
   * Sets the value.
   *
   * @param value the value
   */
  public void setVar(List<String> vars) {
    this.vars = vars;
  }

  /**
   * To string.
   *
   * @return the string
   */
  @Override
  public String toString() {
    return "[vars = " + vars + " ]";
  }
}
