
package gov.nih.nci.evs.api.util.ext;

/**
 * The Class Node.
 */
public class Node {

  /** The label. */
  // Variable declaration
  private String label;

  /** The code. */
  private String code;

  /**
   * Instantiates an empty {@link Node}.
   */
  // Default constructor
  public Node() {
  }

  /**
   * Instantiates a {@link Node} from the specified parameters.
   *
   * @param label the label
   * @param code the code
   */
  // Constructor
  public Node(String label, String code) {

    this.label = label;
    this.code = code;
  }

  /**
   * Sets the label.
   *
   * @param label the label
   */
  // Set methods
  public void setLabel(String label) {
    this.label = label;
  }

  /**
   * Sets the code.
   *
   * @param code the code
   */
  public void setCode(String code) {
    this.code = code;
  }

  /**
   * Returns the label.
   *
   * @return the label
   */
  // Get methods
  public String getLabel() {
    return this.label;
  }

  /**
   * Returns the code.
   *
   * @return the code
   */
  public String getCode() {
    return this.code;
  }

  /**
   * Escape double quotes.
   *
   * @param inputStr the input str
   * @return the string
   */
  public String escapeDoubleQuotes(String inputStr) {
    char doubleQ = '"';
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < inputStr.length(); i++) {
      char c = inputStr.charAt(i);
      if (c == doubleQ) {
        buf.append(doubleQ).append(doubleQ);
      }
      buf.append(c);
    }
    return buf.toString();
  }
}
