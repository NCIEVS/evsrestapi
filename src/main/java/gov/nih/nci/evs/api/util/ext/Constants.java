
package gov.nih.nci.evs.api.util.ext;

/**
 * Constants for computing CTRP extensions.
 */
public class Constants {

  /** The Constant INVERSE_IS_A. */
  public static final String INVERSE_IS_A = "inverse_is_a";

  /** The Constant TRAVERSE_UP. */
  public static final int TRAVERSE_UP = 1;

  /** The Constant TRAVERSE_DOWN. */
  public static final int TRAVERSE_DOWN = 0;

  /** The Constant NEW_CODE. */
  public static final String NEW_CODE = "NHC0";

  /** The Constant NCIT_NS. */
  public static final String NCIT_NS = "<http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#>";

  /** The Constant XML_DECLARATION. */
  public static final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";

  /** The Constant INVERSE_IS_OF. */
  public static final String INVERSE_IS_OF = "inverseIsA";

  /** The Constant ROLE. */
  public static final String ROLE = "Role";

  /** The Constant ASSOCIATION. */
  public static final String ASSOCIATION = "Association";

  /** The Constant HIERARCHICAL. */
  public static final String HIERARCHICAL = "Hierarchical";

  /** The Constant FULL_SYN. */
  public static final String FULL_SYN = "FULL_SYN";

  /** The Constant DEFINITION. */
  public static final String DEFINITION = "DEFINITION";

  /** The Constant ALT_DEFINITION. */
  public static final String ALT_DEFINITION = "ALT_DEFINITION";

  /** The Constant EXACT_MATCH. */
  public static final String EXACT_MATCH = "exactMatch";

  /** The Constant STARTS_MATCH. */
  public static final String STARTS_MATCH = "startsWith";

  /** The Constant ENDS_MATCH. */
  public static final String ENDS_MATCH = "endsWith";

  /** The Constant CONTAINS. */
  public static final String CONTAINS = "contains";

  /** The disease is stage. */
  public static String DISEASE_IS_STAGE = "Disease_Is_Stage";

  /** The maximum level. */
  public static int MAXIMUM_LEVEL = 1000;

  /** The Constant EVSRESTAPI_BEAN. */
  public static final String EVSRESTAPI_BEAN = "gov.nih.nci.evs.restapi.bean";

  /** The common properties. */
  public static String[] COMMON_PROPERTIES = {
      "code", "label", "Preferred_Name", "Display_Name", "DEFINITION", "ALT_DEFINITION", "FULL_SYN",
      "Concept_Status", "Semantic_Type"
  };

  /** The default version predicate. */
  public static String DEFAULT_VERSION_PREDICATE = "owl:versionInfo";

  /** The obo version predicate. */
  public static String OBO_VERSION_PREDICATE = "oboInOwl:hasOBOFormatVersion";

  /** The version predicate. */
  public static String[] VERSION_PREDICATE = new String[] {
      DEFAULT_VERSION_PREDICATE, OBO_VERSION_PREDICATE
  };

  /** The association name. */
  public static String ASSOCIATION_NAME = "inverse_is_a";

  /** The Constant TYPE_ROLE. */
  public static final String TYPE_ROLE = "type_role";

  /** The Constant TYPE_ASSOCIATION. */
  public static final String TYPE_ASSOCIATION = "type_association";

  /** The Constant TYPE_SUPERCONCEPT. */
  public static final String TYPE_SUPERCONCEPT = "type_superconcept";

  /** The Constant TYPE_SUBCONCEPT. */
  public static final String TYPE_SUBCONCEPT = "type_subconcept";

  /** The Constant TYPE_INVERSE_ROLE. */
  public static final String TYPE_INVERSE_ROLE = "type_inverse_role";

  /** The Constant TYPE_INVERSE_ASSOCIATION. */
  public static final String TYPE_INVERSE_ASSOCIATION = "type_inverse_association";

  /** The starts with. */
  public static String STARTS_WITH = "startsWith";

  /** The ends with. */
  public static String ENDS_WITH = "endsWith";

  /** The value set uri prefix. */
  public static String VALUE_SET_URI_PREFIX = "http://evs.nci.nih.gov/valueset/";

  /** The terminology subset code. */
  public static String TERMINOLOGY_SUBSET_CODE = "C54443"; // Terminology Subset
                                                           // (Code C54443)

  /** The concept in subset. */
  public static String CONCEPT_IN_SUBSET = "Concept_In_Subset";

  /** The contributing source. */
  public static String CONTRIBUTING_SOURCE = "Contributing_Source";

  /** The nci thesaurus. */
  public static String NCI_THESAURUS = "NCI_Thesaurus";

  /** The default limit. */
  public static int DEFAULT_LIMIT = 15000;

  /**
   * Constructor.
   */
  private Constants() {

  }

} // Class Constants
