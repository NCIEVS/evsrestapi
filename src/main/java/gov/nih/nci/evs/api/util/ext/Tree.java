
package gov.nih.nci.evs.api.util.ext;

import java.util.List;

/**
 * The Class Tree.
 */
public class Tree {

  /** The root. */
  // Variable declaration
  private String root;

  /** The nodes. */
  private List<Node> nodes;

  /**
   * Instantiates an empty {@link Tree}.
   */
  // Default constructor
  public Tree() {
  }

  /**
   * Instantiates a {@link Tree} from the specified parameters.
   *
   * @param root the root
   * @param nodes the nodes
   */
  // Constructor
  public Tree(String root, List<Node> nodes) {

    this.root = root;
    this.nodes = nodes;
  }

  /**
   * Sets the root.
   *
   * @param root the root
   */
  // Set methods
  public void setRoot(String root) {
    this.root = root;
  }

  /**
   * Sets the nodes.
   *
   * @param nodes the nodes
   */
  public void setNodes(List<Node> nodes) {
    this.nodes = nodes;
  }

  /**
   * Returns the root.
   *
   * @return the root
   */
  // Get methods
  public String getRoot() {
    return this.root;
  }

  /**
   * Returns the nodes.
   *
   * @return the nodes
   */
  public List<Node> getNodes() {
    return this.nodes;
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
