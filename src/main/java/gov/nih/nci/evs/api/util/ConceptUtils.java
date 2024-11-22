package gov.nih.nci.evs.api.util;

import gov.nih.nci.evs.api.model.BaseModel;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptMinimal;
import gov.nih.nci.evs.api.model.Definition;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Path;
import gov.nih.nci.evs.api.model.Paths;
import gov.nih.nci.evs.api.model.Property;
import gov.nih.nci.evs.api.model.Synonym;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.ElasticQueryService;
import gov.nih.nci.evs.api.service.SparqlQueryManagerService;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.EnglishStemmer;

/** Utilities for handling the "include" flag, and converting EVSConcept to Concept. */
public final class ConceptUtils {

  /** The Constant logger. */
  private static final Logger logger = LoggerFactory.getLogger(ConceptUtils.class);

  /** Instantiates an empty {@link ConceptUtils}. */
  private ConceptUtils() {
    // n/a
  }

  /** The Constant PUNCTUATION_REGEX. */
  public static final String PUNCTUATION_REGEX =
      "[ \\t\\-\\(\\{\\[\\)\\}\\]_!@#%&\\*\\\\:;\\\"',\\.\\?\\/~\\+=\\|<>$`^]";

  /**
   * Normalize.
   *
   * @param value the value
   * @return the string
   */
  public static String normalize(final String value) {
    if (value == null) {
      return null;
    }
    // If the value is in the style of GO\:12345, then return it as-is.
    if (value.matches("^[A-Z]+\\\\:\\d+$")) {
      return value;
    }

    return value
        .replaceFirst("^[^\\p{IsAlphabetic}\\p{IsDigit}]*", "")
        .toLowerCase()
        .replaceAll(PUNCTUATION_REGEX, " ")
        .replaceAll("\\s+", " ")
        .trim();
  }

  /**
   * get word stem.
   *
   * @param value the value
   * @return the string
   */
  public static String normalizeWithStemming(final String value) {
    final SnowballStemmer stemmer = new EnglishStemmer();
    String norm = normalize(value);
    // split by spaces and stem everything, then rejoin
    return norm != null
        ? Arrays.stream(norm.split(" "))
            .map(
                w -> {
                  stemmer.setCurrent(w);
                  stemmer.stem();
                  return stemmer.getCurrent();
                })
            .collect(Collectors.joining(" "))
        : "";
  }

  /**
   * Apply highlights.
   *
   * @param concept the concept
   * @param highlights the highlights
   * @throws Exception the exception
   */
  public static void applyHighlights(final Concept concept, final Map<String, String> highlights)
      throws Exception {

    // concept
    if (highlights.containsKey(concept.getName())) {
      concept.setHighlight(highlights.get(concept.getName()));
    } else if (highlights.containsKey(concept.getCode())) {
      concept.setHighlight(highlights.get(concept.getCode()));
    }

    // synonyms
    for (final Synonym sn : concept.getSynonyms()) {
      String value = highlights.get(sn.getName());
      if (value != null) {
        sn.setHighlight(value);
      }
      value = highlights.get(sn.getCode());
      if (value != null) {
        sn.setHighlight(value + " - " + sn.getName());
      }
    }

    // definitions
    for (final Definition def : concept.getDefinitions()) {
      String value = null;
      for (final Map.Entry<String, String> highlight : highlights.entrySet()) {
        if (def.getDefinition().contains(highlight.getKey())) {
          value = highlight.getValue();
        }
      }
      if (value != null) {
        def.setHighlight(value);
      }
    }

    // properties
    for (final Property prop : concept.getProperties()) {
      String value = highlights.get(prop.getValue());
      if (value != null) {
        prop.setHighlight(value);
      } else {
        value = highlights.get(prop.getType());
        if (value != null) {
          prop.setHighlight(value);
        }
      }
    }

    // TODO - when role search is supported
    // TODO - when association search is supported

  }

  /**
   * Apply include.
   *
   * @param concepts the list of concepts
   * @param ip the include param
   * @return the result concepts
   */
  public static List<Concept> applyInclude(final List<Concept> concepts, final IncludeParam ip) {
    if (CollectionUtils.isEmpty(concepts)) {
      return Collections.emptyList();
    }

    final List<Concept> result = new ArrayList<>(concepts.size());

    for (final Concept concept : concepts) {
      final Concept newConcept = new Concept();
      newConcept.setCode(concept.getCode());
      newConcept.setName(concept.getName());
      newConcept.setTerminology(concept.getTerminology());
      newConcept.setVersion(concept.getVersion());
      newConcept.setLeaf(concept.getLeaf());

      if (ip.isSynonyms()) {
        newConcept.setSynonyms(concept.getSynonyms());
      }
      if (ip.isDefinitions()) {
        newConcept.setDefinitions(concept.getDefinitions());
      }
      if (ip.isHistory()) {
        newConcept.setHistory(concept.getHistory());
      }
      if (ip.isProperties()) {
        newConcept.setProperties(concept.getProperties());
      }
      if (ip.isChildren()) {
        newConcept.setChildren(concept.getChildren());
      }
      if (ip.isParents()) {
        newConcept.setParents(concept.getParents());
      }
      if (ip.isAssociations()) {
        newConcept.setAssociations(concept.getAssociations());
      }
      if (ip.isInverseAssociations()) {
        newConcept.setInverseAssociations(concept.getInverseAssociations());
      }
      if (ip.isRoles()) {
        newConcept.setRoles(concept.getRoles());
      }
      if (ip.isInverseRoles()) {
        newConcept.setInverseRoles(concept.getInverseRoles());
      }
      if (ip.isDisjointWith()) {
        newConcept.setDisjointWith(concept.getDisjointWith());
      }
      if (ip.isMaps()) {
        newConcept.setMaps(concept.getMaps());
      }
      if (ip.isPaths()) {
        newConcept.setPaths(concept.getPaths());
      }
      if (ip.isExtensions()) {
        newConcept.setExtensions(concept.getExtensions());
      }
      if (ip.isDescendants()) {
        newConcept.setDescendants(concept.getDescendants());
      }
      if (ip.isSubsetLink()) {
        newConcept.setSubsetLink(concept.getSubsetLink());
      }
      if (ip.isMapsetLink()) {
        newConcept.setMapsetLink(concept.getMapsetLink());
      }
      // Using @WriteOnlyProperty instead
      //      newConcept.clearHidden();
      result.add(newConcept);
    }

    return result;
  }

  /**
   * Apply include.
   *
   * @param concept the concept
   * @param limit the limit
   * @throws Exception the exception
   */
  public static void applyLimit(final Concept concept, final int limit) throws Exception {

    concept.setAssociations(sublist(concept.getAssociations(), 0, limit));
    concept.setChildren(sublist(concept.getChildren(), 0, limit));
    concept.setDefinitions(sublist(concept.getDefinitions(), 0, limit));
    concept.setHistory(sublist(concept.getHistory(), 0, limit));
    concept.setDescendants(sublist(concept.getDescendants(), 0, limit));
    concept.setDisjointWith(sublist(concept.getDisjointWith(), 0, limit));
    concept.setInverseAssociations(sublist(concept.getInverseAssociations(), 0, limit));
    concept.setInverseRoles(sublist(concept.getInverseRoles(), 0, limit));
    concept.setMaps(sublist(concept.getMaps(), 0, limit));
    concept.setParents(sublist(concept.getParents(), 0, limit));
    if (concept.getPaths() != null) {
      concept.getPaths().setPaths(sublist(concept.getPaths().getPaths(), 0, limit));
    }
    concept.setProperties(sublist(concept.getProperties(), 0, limit));
    concept.setQualifiers(sublist(concept.getQualifiers(), 0, limit));
    concept.setRoles(sublist(concept.getRoles(), 0, limit));
    concept.setSynonyms(sublist(concept.getSynonyms(), 0, limit));
  }

  /**
   * Sublist.
   *
   * @param <T> the
   * @param list the list
   * @param fromIndex the from index
   * @param maxElements the max elements
   * @return the list
   * @throws Exception the exception
   */
  @SuppressWarnings("unchecked")
  public static <T extends BaseModel> List<T> sublist(
      List<T> list, final int fromIndex, final int maxElements) throws Exception {

    if (fromIndex >= list.size()) {
      return new ArrayList<>();
    }
    List<T> result =
        new ArrayList<>(list).subList(fromIndex, Math.min(fromIndex + maxElements, list.size()));

    // Add a placeholder "last element" with a "ct" for the total.
    if (fromIndex == 0 && maxElements < list.size() && result.size() > 0) {

      final T obj =
          (T)
              result
                  .get(0)
                  .getClass()
                  .getDeclaredConstructor(new Class[0])
                  .newInstance(new Object[0]);
      obj.setCt(list.size());
      result.add(obj);
    }

    return result;
  }

  /**
   * Apply include.
   *
   * @param concept the concept
   * @param ip the include param
   * @return the result concepts
   */
  public static void applyInclude(final Concept concept, final IncludeParam ip) {

    if (concept == null) {
      return;
    }

    if (!ip.isSynonyms()) {
      concept.setSynonyms(null);
    }
    if (!ip.isDefinitions()) {
      concept.setDefinitions(null);
    }
    if (!ip.isHistory()) {
      concept.setHistory(null);
    }
    if (!ip.isProperties()) {
      concept.setProperties(null);
    }
    if (!ip.isChildren()) {
      concept.setChildren(null);
    }
    if (!ip.isParents()) {
      concept.setParents(null);
    }
    if (!ip.isAssociations()) {
      concept.setAssociations(null);
    }
    if (!ip.isInverseAssociations()) {
      concept.setInverseAssociations(null);
    }
    if (!ip.isRoles()) {
      concept.setRoles(null);
    }
    if (!ip.isInverseRoles()) {
      concept.setInverseRoles(null);
    }
    if (!ip.isDisjointWith()) {
      concept.setDisjointWith(null);
    }
    if (!ip.isMaps()) {
      concept.setMaps(null);
    }
    if (!ip.isPaths()) {
      concept.setPaths(null);
    }
    if (!ip.isDescendants()) {
      concept.setDescendants(null);
    }
  }

  /**
   * Apply list.
   *
   * @param concepts the evs concepts
   * @param ip the ip
   * @param list the list
   * @return the list
   * @throws Exception the exception
   */
  public static List<Concept> applyList(
      final List<Concept> concepts, final IncludeParam ip, final String list) throws Exception {
    final Set<String> codes =
        (list == null || list.isEmpty())
            ? null
            : Arrays.stream(list.split(",")).collect(Collectors.toSet());

    return concepts.stream()
        .filter(c -> codes == null || codes.contains(c.getCode()) || codes.contains(c.getName()))
        // Handled by @WriteOnlyProperty
        //        .peek(c -> c.clearHidden())
        .collect(Collectors.toList());
  }

  /**
   * Apply include with children.
   *
   * @param concepts the evs concepts
   * @param ip the ip
   * @param list the list
   * @return the list
   * @throws Exception the exception
   */
  public static List<Concept> applyListWithChildren(
      final List<Concept> concepts, final IncludeParam ip, final String list) throws Exception {
    final Set<String> codes =
        (list == null || list.isEmpty())
            ? null
            : Arrays.stream(list.split(",")).collect(Collectors.toSet());

    return concepts.stream()
        .flatMap(Concept::streamSelfAndChildren)
        .filter(c -> codes == null || codes.contains(c.getCode()) || codes.contains(c.getName()))
        // Handled by @WriteOnlyProperty
        //        .peek(c -> c.clearHidden())
        .collect(Collectors.toList());
  }

  /**
   * Convert concepts fy with include.
   *
   * @param service the service
   * @param ip the ip
   * @param terminology the terminology
   * @param concepts the concepts
   * @return the list
   * @throws Exception the exception
   */
  public static List<Concept> convertConceptsWithInclude(
      final SparqlQueryManagerService service,
      final IncludeParam ip,
      final Terminology terminology,
      final List<Concept> concepts)
      throws Exception {

    // final List<Concept> concepts = convertConcepts(list);
    if (ip.hasAnyTrue()) {
      for (final Concept concept : concepts) {
        final Integer level = concept.getLevel();
        final Boolean leaf = concept.getLeaf();
        concept.populateFrom(service.getConcept(concept.getCode(), terminology, ip));
        concept.setLevel(level);
        concept.setLeaf(leaf);
      }
    }
    return concepts;
  }

  /**
   * Convert paths.
   *
   * @param paths the paths
   * @param reverse the reverse
   * @return the list
   */
  public static List<List<Concept>> convertPaths(final Paths paths, final boolean reverse) {
    final List<List<Concept>> list = new ArrayList<>();
    if (paths == null || paths.getPaths() == null || paths.getPaths().isEmpty()) {
      return list;
    }
    for (final Path path : paths.getPaths()) {
      final List<Concept> concepts = new ArrayList<Concept>();
      for (final ConceptMinimal cn : path.getConcepts()) {
        concepts.add(new Concept(cn));
      }
      // Reverse if indicated
      if (reverse) {
        Collections.reverse(concepts);
      }
      for (int i = 0; i < concepts.size(); i++) {
        concepts.get(i).setLevel(i);
      }
      list.add(concepts);
    }
    return list;
  }

  /**
   * Convert paths with include.
   *
   * @param service the service
   * @param ip the ip
   * @param terminology the terminology
   * @param paths the paths
   * @param reverse the reverse
   * @return the list
   * @throws Exception the exception
   */
  public static List<List<Concept>> convertPathsWithInclude(
      final ElasticQueryService service,
      final IncludeParam ip,
      final Terminology terminology,
      final Paths paths,
      final boolean reverse)
      throws Exception {

    final List<List<Concept>> list = convertPaths(paths, reverse);

    // Get all the codes an unique them, to make it faster.
    // Most of the top level concepts are all the same
    final Set<String> codes =
        list.stream().flatMap(l -> l.stream()).map(c -> c.getCode()).collect(Collectors.toSet());
    final Map<String, Concept> conceptMap = service.getConceptsAsMap(codes, terminology, ip);
    // final Map<String, Concept> cache = new HashMap<>();
    for (final List<Concept> concepts : list) {
      for (final Concept concept : concepts) {
        final int level = concept.getLevel();
        // if (cache.containsKey(concept.getCode())) {
        // concept.populateFrom(cache.get(concept.getCode()));
        // } else {
        concept.populateFrom(conceptMap.get(concept.getCode()));
        // cache.put(concept.getCode(), concept);
        // }
        concept.setLevel(level);
      }
    }

    return list;
  }

  /**
   * As map.
   *
   * @param values the values
   * @return the map
   */
  public static Map<String, String> asMap(final String... values) {
    final Map<String, String> map = new HashMap<>();
    if (values.length % 2 != 0) {
      throw new RuntimeException("Unexpected odd number of parameters");
    }
    for (int i = 0; i < values.length; i += 2) {
      // Patch for default namespace where appropriate
      if (values[i].endsWith("Code")
          && !values[i].equals("conceptCode")
          && !values[i + 1].contains(":")) {
        map.put(values[i], ":" + values[i + 1]);
      } else if (values[i].endsWith("Code") && values[i + 1].startsWith("http")) {
        map.put(values[i], "<" + values[i + 1] + ">");
      } else {
        map.put(values[i], values[i + 1]);
      }
    }
    return map;
  }

  /**
   * As tree map.
   *
   * @param values the values
   * @return the map
   */
  public static Map<String, String> asLinkedMap(final String... values) {
    final Map<String, String> map = new LinkedHashMap<>();
    if (values.length % 2 != 0) {
      throw new RuntimeException("Unexpected odd number of parameters");
    }
    for (int i = 0; i < values.length; i += 2) {
      // Patch for default namespace where appropriate
      if (values[i].endsWith("Code")
          && !values[i].equals("conceptCode")
          && !values[i + 1].contains(":")) {
        map.put(values[i], ":" + values[i + 1]);
      } else if (values[i].endsWith("Code") && values[i + 1].startsWith("http")) {
        map.put(values[i], "<" + values[i + 1] + ">");
      } else {
        map.put(values[i], values[i + 1]);
      }
    }
    return map;
  }

  /**
   * Substr.
   *
   * @param string the string
   * @param len the len
   * @return the string
   */
  public static String substr(final String string, final int len) {
    if (len >= string.length()) {
      return string;
    }
    return string.substring(0, Math.min(len, string.length()))
        + (string.length() > len ? "..." : "");
  }

  /**
   * Wordind.
   *
   * @param name the name
   * @return the list
   */
  public static List<String> wordind(final String name) {
    final String[] tokens = name.split(PUNCTUATION_REGEX);
    return Arrays.asList(tokens).stream().filter(s -> s.length() > 0).collect(Collectors.toList());
  }

  /**
   * isCode.
   *
   * @param code the code
   * @return is given string a code
   */
  public static boolean isCode(final String code) {
    return code != null
        && code.toUpperCase().matches("[A-Z]{0,5}:?\\d*[-\\.X\\?]?\\d*/?\\d*[A-Za-z_]*[A-Z]?");
  }

  /**
   * Go through map and cap the number of values for each key to the stated max size.
   *
   * @param qualMap the qual map
   * @param maxSize the max size
   */
  public static void limitQualMap(Map<String, Set<String>> qualMap, final int maxSize) {

    for (final String key : qualMap.keySet()) {
      // Truncate additional values
      if (qualMap.get(key).size() > maxSize) {
        logger.info(
            "      truncate qualifier values list at 1000 = "
                + key
                + ", "
                + qualMap.get(key).size());
        qualMap.put(
            key,
            qualMap.get(key).stream().collect(Collectors.toList()).subList(0, 1000).stream()
                .collect(Collectors.toSet()));
        qualMap.get(key).add("... additional values ...");
      }
    }
  }

  /**
   * Intersection.
   *
   * @param s1 the s 1
   * @param s2 the s 2
   * @return the sets the
   */
  public static Set<String> intersection(final Set<String> s1, final Set<String> s2) {
    final Set<String> s1copy = new HashSet<>(s1);
    s1copy.retainAll(s2);
    return s1copy;
  }

  /**
   * Difference.
   *
   * @param s1 the s 1
   * @param s2 the s 2
   * @return the sets the
   */
  public static Set<String> difference(final Set<String> s1, final Set<String> s2) {
    final Set<String> s1copy = new HashSet<>(s1);
    s1copy.removeAll(s2);
    return s1copy;
  }

  /**
   * Compose query string from map of clauses containing the query terms.
   *
   * @param clauses the map of clauses to query on
   * @return the query string
   * @throws Exception if the query string cannot be composed
   */
  public static String composeQueryString(final Map<String, String> clauses) throws Exception {
    final StringBuilder sb = new StringBuilder();
    for (final String key : clauses.keySet()) {
      // Skip empty key or value
      if (ConceptUtils.isEmpty(key) || ConceptUtils.isEmpty(clauses.get(key))) {
        continue;
      }
      if (sb.length() > 1) {
        sb.append("&");
      }
      sb.append(key).append("=");
      final String value = clauses.get(key);
      if (value.matches("^[0-9a-zA-Z\\-\\.]*$")) {
        sb.append(value);
      } else {
        sb.append(URLEncoder.encode(value, "UTF-8").replaceAll("\\+", "%20"));
      }
    }
    return (sb.length() > 0 ? "?" + sb.toString() : "");
  }

  /**
   * Compose query from map of clauses containing the query terms and the operators.
   *
   * @param operator the query operator to use
   * @param clauses the map of clauses to query on
   * @return the query string
   */
  public static String composeQuery(final String operator, final List<String> clauses) {
    final StringBuilder sb = new StringBuilder();
    if (operator.equals("OR")) {
      sb.append("(");
    }
    for (final String clause : clauses) {
      if (ConceptUtils.isEmpty(clause)) {
        continue;
      }
      if (sb.length() > 0 && !operator.equals("OR")) {
        sb.append(" ").append(operator).append(" ");
      }
      if (sb.length() > 1 && operator.equals("OR")) {
        sb.append(" ").append(operator).append(" ");
      }
      sb.append(clause);
    }
    if (operator.equals("OR")) {
      sb.append(")");
    }
    if (operator.equals("OR") && sb.toString().equals("()")) {
      return "";
    }

    return sb.toString();
  }

  /**
   * Compose query from a variable length of map of clauses containing the query terms and the
   * operators.
   *
   * @param operator the query operator to use
   * @param clauses the map of clauses to query on (can be variable length)
   * @return the query string
   */
  public static String composeQuery(final String operator, final String... clauses) {
    final StringBuilder sb = new StringBuilder();
    if (operator.equals("OR")) {
      sb.append("(");
    }
    for (final String clause : clauses) {
      if (ConceptUtils.isEmpty(clause)) {
        continue;
      } else if (sb.length() > 0 && !operator.equals("OR")) {
        sb.append(" ").append(operator).append(" ");
      } else if (sb.length() > 1 && operator.equals("OR")) {
        sb.append(" ").append(operator).append(" ");
      }

      sb.append(clause);
    }
    if (operator.equals("OR")) {
      sb.append(")");
    }
    if (operator.equals("OR") && sb.toString().equals("()")) {
      return "";
    }

    return sb.toString();
  }

  /**
   * Compose a string clause for a query
   *
   * @param fieldName the query field name
   * @param fieldValue the query field value
   * @param escapeValue the flag to escape the value
   * @return the query string
   * @throws Exception if the query string cannot be composed
   */
  public static String composeClause(
      final String fieldName, final String fieldValue, final boolean escapeValue) throws Exception {

    if (!ConceptUtils.isEmpty(fieldValue)) {
      if (escapeValue) {
        return fieldName + ":\"" + QueryParserBase.escape(fieldValue) + "\"";
      } else {
        return fieldName + ":" + fieldValue;
      }
    } else {
      return "NOT " + fieldName + ":[* TO *]";
    }
  }

  /**
   * Check if a string is empty.
   *
   * @param str the string to check
   * @return true if the string is null or empty
   */
  public static boolean isEmpty(final String str) {
    return str == null || str.isEmpty();
  }
}
