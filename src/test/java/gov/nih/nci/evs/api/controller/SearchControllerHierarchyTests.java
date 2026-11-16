package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.ConceptResultList;
import gov.nih.nci.evs.api.model.SearchCriteriaWithoutTerminology;
import gov.nih.nci.evs.api.util.ThreadLocalMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Integration tests for hierarchy-constrained search (descendantOf, notDescendantOf, ancestorOf,
 * ancestor, descendant, maxLevel, includeSelf). Expected results are computed from the existing
 * hierarchy endpoints (/descendants, /children, /pathsToRoot, plain search), so the tests pass
 * whether a terminology was indexed with ancestor fields or is served by the query-time fallback.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class SearchControllerHierarchyTests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(SearchControllerHierarchyTests.class);

  /** The anchor used for NCIt branch tests (Breast Carcinoma). */
  private static final String ANCHOR = "C4872";

  /** The anchor used for MA tests (organ system), from an index with ancestors. */
  private static final String MA_ANCHOR = "MA:0000003";

  /** The mvc. */
  @Autowired private MockMvc mvc;

  /**
   * Test that an empty-term branch search returns exactly the branch, with and without the anchor.
   *
   * @throws Exception the exception
   */
  @Test
  public void testDescendantOfListsBranch() throws Exception {
    final Set<String> descendants = descendants("ncit", ANCHOR);
    assertThat(descendants).isNotEmpty();

    final SearchResult withSelf = searchAll("ncit", Map.of("descendantOf", ANCHOR));
    assertThat(withSelf.total).isEqualTo(descendants.size() + 1);
    assertThat(new HashSet<>(withSelf.codes)).isEqualTo(union(descendants, Set.of(ANCHOR)));
    assertThat(withSelf.codes).doesNotHaveDuplicates();

    final SearchResult noSelf =
        searchAll("ncit", Map.of("descendantOf", ANCHOR, "includeSelf", "false"));
    assertThat(noSelf.total).isEqualTo(descendants.size());
    assertThat(new HashSet<>(noSelf.codes)).isEqualTo(descendants);
  }

  /**
   * Test that a term search within a branch returns exactly the plain search hits that are in the
   * branch, in the same order.
   *
   * @throws Exception the exception
   */
  @Test
  public void testDescendantOfWithTerm() throws Exception {
    final Set<String> branch = union(descendants("ncit", ANCHOR), Set.of(ANCHOR));
    final List<String> plain = searchAll("ncit", Map.of("term", "adenocarcinoma")).codes;
    final List<String> expected =
        plain.stream().filter(branch::contains).collect(Collectors.toList());
    assertThat(expected).isNotEmpty();

    final SearchResult result =
        searchAll("ncit", Map.of("term", "adenocarcinoma", "descendantOf", ANCHOR));
    assertThat(result.total).isEqualTo(expected.size());
    assertThat(result.codes).isEqualTo(expected);
  }

  /**
   * Test that maxLevel=1 returns the children, and maxLevel=2 adds the grandchildren.
   *
   * @throws Exception the exception
   */
  @Test
  public void testDescendantOfMaxLevel() throws Exception {
    final Set<String> children = children("ncit", ANCHOR);
    final SearchResult level1 =
        searchAll("ncit", Map.of("descendantOf", ANCHOR, "maxLevel", "1", "includeSelf", "false"));
    assertThat(new HashSet<>(level1.codes)).isEqualTo(children);

    final Set<String> grandchildren = new HashSet<>();
    for (final String child : children) {
      grandchildren.addAll(children("ncit", child));
    }
    final SearchResult level2 =
        searchAll("ncit", Map.of("descendantOf", ANCHOR, "maxLevel", "2", "includeSelf", "false"));
    assertThat(new HashSet<>(level2.codes)).isEqualTo(union(children, grandchildren));
  }

  /**
   * Test maxLevel with codes containing ':' and a polyhierarchy concept reachable at several
   * depths. GO:0098754 is a direct child of GO:0008150 that is also deeper on another path.
   *
   * @throws Exception the exception
   */
  @Test
  public void testDescendantOfMaxLevelColonCodes() throws Exception {
    final Set<String> children = children("go", "GO:0008150");
    final SearchResult result =
        searchAll(
            "go", Map.of("descendantOf", "GO:0008150", "maxLevel", "1", "includeSelf", "false"));
    assertThat(new HashSet<>(result.codes)).isEqualTo(children);
  }

  /**
   * Test that several anchors return the union of their branches.
   *
   * @throws Exception the exception
   */
  @Test
  public void testDescendantOfMultipleAnchors() throws Exception {
    final String other = "C3224";
    final Set<String> expected =
        union(
            union(descendants("ncit", ANCHOR), Set.of(ANCHOR)),
            union(descendants("ncit", other), Set.of(other)));
    final SearchResult result = searchAll("ncit", Map.of("descendantOf", ANCHOR + "," + other));
    assertThat(new HashSet<>(result.codes)).isEqualTo(expected);
  }

  /**
   * Test that notDescendantOf removes a sub-branch, and keeps the sub-branch anchor when
   * includeSelf is false.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNotDescendantOf() throws Exception {
    final Set<String> branch = union(descendants("ncit", ANCHOR), Set.of(ANCHOR));
    final String sub = subBranch("ncit", ANCHOR);
    final Set<String> subDescendants = descendants("ncit", sub);

    final SearchResult result =
        searchAll("ncit", Map.of("descendantOf", ANCHOR, "notDescendantOf", sub));
    final Set<String> expected = new HashSet<>(branch);
    expected.removeAll(subDescendants);
    expected.remove(sub);
    assertThat(new HashSet<>(result.codes)).isEqualTo(expected);

    final SearchResult keepSub =
        searchAll(
            "ncit", Map.of("descendantOf", ANCHOR, "notDescendantOf", sub, "includeSelf", "false"));
    assertThat(keepSub.codes).contains(sub).doesNotContainAnyElementsOf(subDescendants);
  }

  /**
   * Test that ancestorOf returns the codes on the concept's paths to root.
   *
   * @throws Exception the exception
   */
  @Test
  public void testAncestorOf() throws Exception {
    final String code = deepDescendant("ncit", ANCHOR);
    final Set<String> ancestors = pathsToRootCodes("ncit", code);
    ancestors.remove(code);
    assertThat(ancestors).contains(ANCHOR);

    final SearchResult result =
        searchAll("ncit", Map.of("ancestorOf", code, "includeSelf", "false"));
    assertThat(new HashSet<>(result.codes)).isEqualTo(ancestors);

    // Combined with descendantOf, only the concepts on paths between the two remain
    final Set<String> branch = union(descendants("ncit", ANCHOR), Set.of(ANCHOR));
    final Set<String> between =
        union(ancestors, Set.of(code)).stream()
            .filter(branch::contains)
            .collect(Collectors.toSet());
    final SearchResult combined =
        searchAll("ncit", Map.of("ancestorOf", code, "descendantOf", ANCHOR));
    assertThat(new HashSet<>(combined.codes)).isEqualTo(between);
  }

  /**
   * Test that ancestor returns the code's subtree: the code itself and its descendants. Uses MA,
   * which is indexed with ancestors.
   *
   * @throws Exception the exception
   */
  @Test
  public void testAncestor() throws Exception {
    final Set<String> subtree = new HashSet<>(descendants("ma", MA_ANCHOR));
    assertThat(subtree).isNotEmpty();
    subtree.add(MA_ANCHOR);

    final SearchResult result = searchAll("ma", Map.of("ancestor", MA_ANCHOR));
    assertThat(result.total).isEqualTo(subtree.size());
    assertThat(new HashSet<>(result.codes)).isEqualTo(subtree);
  }

  /**
   * Test that descendant returns the code itself and the codes on its paths to root. MA:0002450 has
   * two parents.
   *
   * @throws Exception the exception
   */
  @Test
  public void testDescendant() throws Exception {
    final String code = "MA:0002450";
    final Set<String> ancestorsAndSelf = pathsToRootCodes("ma", code);
    assertThat(ancestorsAndSelf).contains(code).hasSizeGreaterThan(2);

    final SearchResult result = searchAll("ma", Map.of("descendant", code));
    assertThat(result.total).isEqualTo(ancestorsAndSelf.size());
    assertThat(new HashSet<>(result.codes)).isEqualTo(ancestorsAndSelf);
  }

  /**
   * Test that ancestor and descendant together return the concepts between the two codes, including
   * both codes.
   *
   * @throws Exception the exception
   */
  @Test
  public void testAncestorAndDescendant() throws Exception {
    final String code = "MA:0000922";
    final Set<String> subtree = new HashSet<>(descendants("ma", MA_ANCHOR));
    subtree.add(MA_ANCHOR);
    final Set<String> between = pathsToRootCodes("ma", code);
    between.retainAll(subtree);
    assertThat(between).contains(MA_ANCHOR, code);

    final SearchResult result = searchAll("ma", Map.of("ancestor", MA_ANCHOR, "descendant", code));
    assertThat(new HashSet<>(result.codes)).isEqualTo(between);
  }

  /**
   * Test validation errors for the ancestor and descendant parameters.
   *
   * @throws Exception the exception
   */
  @Test
  public void testAncestorAndDescendantValidation() throws Exception {
    assertBadRequest(
        get("/api/v1/concept/ma/search").param("ancestor", "MA:9999999"),
        "'ancestor' has an invalid code = MA:9999999");
    assertBadRequest(
        get("/api/v1/concept/ma/search").param("descendant", "MA:9999999"),
        "'descendant' has an invalid code = MA:9999999");
    assertBadRequest(
        get("/api/v1/concept/ncim/search").param("ancestor", "C0006826"),
        "does not support hierarchy parameters");
    assertBadRequest(
        get("/api/v1/concept/search").param("terminology", "ma,go").param("descendant", MA_ANCHOR),
        "require exactly one terminology");
  }

  /**
   * Test that paging through a branch search is stable and complete, including an unaligned
   * fromRecord.
   *
   * @throws Exception the exception
   */
  @Test
  public void testDescendantOfPaging() throws Exception {
    final List<String> full = searchAll("ncit", Map.of("descendantOf", ANCHOR)).codes;
    final List<String> paged = new ArrayList<>();
    for (int from = 0; from < full.size(); from += 50) {
      paged.addAll(searchPage("ncit", Map.of("descendantOf", ANCHOR), from, 50).codes);
    }
    assertThat(paged).isEqualTo(full);

    final List<String> unaligned = searchPage("ncit", Map.of("descendantOf", ANCHOR), 7, 10).codes;
    assertThat(unaligned).isEqualTo(full.subList(7, Math.min(17, full.size())));
  }

  /**
   * Test that requests without hierarchy parameters are unaffected, and that includeSelf alone is a
   * no-op.
   *
   * @throws Exception the exception
   */
  @Test
  public void testNoHierarchyParametersUnchanged() throws Exception {
    final SearchResult plain = searchPage("ncit", Map.of("term", "melanoma"), 0, 10);
    final SearchResult includeSelfOnly =
        searchPage("ncit", Map.of("term", "melanoma", "includeSelf", "false"), 0, 10);
    assertThat(includeSelfOnly.total).isEqualTo(plain.total);
    assertThat(includeSelfOnly.codes).isEqualTo(plain.codes);
  }

  /**
   * Test validation errors for hierarchy parameters.
   *
   * @throws Exception the exception
   */
  @Test
  public void testHierarchyValidation() throws Exception {
    assertBadRequest(
        get("/api/v1/concept/search").param("terminology", "ncit,go").param("descendantOf", ANCHOR),
        "require exactly one terminology");
    assertBadRequest(
        get("/api/v1/concept/ncim/search").param("descendantOf", "C0006826"),
        "does not support hierarchy parameters");
    assertBadRequest(
        get("/api/v1/concept/ncit/search").param("descendantOf", "C9999999"),
        "'descendantOf' has an invalid code = C9999999");
    assertBadRequest(
        get("/api/v1/concept/ncit/search").param("notDescendantOf", "C9999999"),
        "'notDescendantOf' has an invalid code = C9999999");
    assertBadRequest(
        get("/api/v1/concept/ncit/search").param("ancestorOf", "C9999999"),
        "'ancestorOf' has an invalid code = C9999999");
    assertBadRequest(
        get("/api/v1/concept/ncit/search").param("descendantOf", ANCHOR).param("maxLevel", "0"),
        "must be >= 1");
    assertBadRequest(
        get("/api/v1/concept/ncit/search").param("term", "lung").param("maxLevel", "2"),
        "requires 'descendantOf'");
    final String tooMany =
        String.join(
            ",",
            Collections.nCopies(SearchCriteriaWithoutTerminology.MAX_HIERARCHY_CODES + 1, ANCHOR));
    assertBadRequest(get("/api/v1/concept/ncit/search").param("descendantOf", tooMany), "at most");
  }

  /** A search result: total and codes in returned order. */
  private static class SearchResult {

    /** The total. */
    private long total;

    /** The codes. */
    private List<String> codes = new ArrayList<>();
  }

  /**
   * Runs one search page.
   *
   * @param terminology the terminology
   * @param params the params
   * @param fromRecord the from record
   * @param pageSize the page size
   * @return the search result
   * @throws Exception the exception
   */
  private SearchResult searchPage(
      final String terminology,
      final Map<String, String> params,
      final int fromRecord,
      final int pageSize)
      throws Exception {
    MockHttpServletRequestBuilder request =
        get("/api/v1/concept/" + terminology + "/search")
            .param("fromRecord", String.valueOf(fromRecord))
            .param("pageSize", String.valueOf(pageSize));
    for (final Map.Entry<String, String> entry : new LinkedHashMap<>(params).entrySet()) {
      request = request.param(entry.getKey(), entry.getValue());
    }
    log.info("Testing url - /api/v1/concept/" + terminology + "/search " + params);
    final MvcResult result = mvc.perform(request).andExpect(status().isOk()).andReturn();
    final ConceptResultList list =
        ThreadLocalMapper.get()
            .readValue(result.getResponse().getContentAsString(), ConceptResultList.class);
    final SearchResult searchResult = new SearchResult();
    searchResult.total = list.getTotal();
    if (list.getConcepts() != null) {
      searchResult.codes =
          list.getConcepts().stream().map(Concept::getCode).collect(Collectors.toList());
    }
    return searchResult;
  }

  /**
   * Runs a search and pages to completion.
   *
   * @param terminology the terminology
   * @param params the params
   * @return the search result with all codes
   * @throws Exception the exception
   */
  private SearchResult searchAll(final String terminology, final Map<String, String> params)
      throws Exception {
    final SearchResult all = new SearchResult();
    for (int from = 0; ; from += 1000) {
      final SearchResult page = searchPage(terminology, params, from, 1000);
      all.total = page.total;
      all.codes.addAll(page.codes);
      if (page.codes.isEmpty() || from + 1000 >= page.total) {
        return all;
      }
    }
  }

  /**
   * Returns codes from a hierarchy endpoint that returns a list of concepts.
   *
   * @param url the url
   * @return the codes
   * @throws Exception the exception
   */
  private Set<String> conceptCodes(final String url) throws Exception {
    final MvcResult result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    final Concept[] concepts =
        ThreadLocalMapper.get()
            .readValue(result.getResponse().getContentAsString(), Concept[].class);
    return Arrays.stream(concepts).map(Concept::getCode).collect(Collectors.toSet());
  }

  /**
   * Returns the descendant codes from the /descendants endpoint.
   *
   * @param terminology the terminology
   * @param code the code
   * @return the descendants
   * @throws Exception the exception
   */
  private Set<String> descendants(final String terminology, final String code) throws Exception {
    return conceptCodes("/api/v1/concept/" + terminology + "/" + code + "/descendants");
  }

  /**
   * Returns the child codes from the /children endpoint.
   *
   * @param terminology the terminology
   * @param code the code
   * @return the children
   * @throws Exception the exception
   */
  private Set<String> children(final String terminology, final String code) throws Exception {
    return conceptCodes("/api/v1/concept/" + terminology + "/" + code + "/children");
  }

  /**
   * Returns all codes on the concept's paths to root, including the concept itself.
   *
   * @param terminology the terminology
   * @param code the code
   * @return the codes
   * @throws Exception the exception
   */
  private Set<String> pathsToRootCodes(final String terminology, final String code)
      throws Exception {
    final MvcResult result =
        mvc.perform(get("/api/v1/concept/" + terminology + "/" + code + "/pathsToRoot"))
            .andExpect(status().isOk())
            .andReturn();
    final Concept[][] paths =
        ThreadLocalMapper.get()
            .readValue(result.getResponse().getContentAsString(), Concept[][].class);
    return Arrays.stream(paths)
        .flatMap(Arrays::stream)
        .map(Concept::getCode)
        .collect(Collectors.toSet());
  }

  /**
   * Returns a child of the anchor that has its own descendants.
   *
   * @param terminology the terminology
   * @param anchor the anchor
   * @return the child code
   * @throws Exception the exception
   */
  private String subBranch(final String terminology, final String anchor) throws Exception {
    for (final String child : children(terminology, anchor).stream().sorted().toList()) {
      if (!descendants(terminology, child).isEmpty()) {
        return child;
      }
    }
    throw new IllegalStateException("No child of " + anchor + " has descendants");
  }

  /**
   * Returns a descendant of the anchor that is at least two levels below it.
   *
   * @param terminology the terminology
   * @param anchor the anchor
   * @return the descendant code
   * @throws Exception the exception
   */
  private String deepDescendant(final String terminology, final String anchor) throws Exception {
    final Set<String> children = children(terminology, anchor);
    return descendants(terminology, anchor).stream()
        .filter(code -> !children.contains(code))
        .sorted()
        .findFirst()
        .orElseThrow();
  }

  /**
   * Asserts that the request returns 400 with a message containing the text.
   *
   * @param request the request
   * @param text the expected message text
   * @throws Exception the exception
   */
  private void assertBadRequest(final MockHttpServletRequestBuilder request, final String text)
      throws Exception {
    final MvcResult result = mvc.perform(request).andExpect(status().isBadRequest()).andReturn();
    assertThat(result.getResponse().getErrorMessage()).contains(text);
  }

  /**
   * Returns the union of two sets.
   *
   * @param a the first set
   * @param b the second set
   * @return the union
   */
  private Set<String> union(final Set<String> a, final Set<String> b) {
    final Set<String> result = new HashSet<>(a);
    result.addAll(b);
    return result;
  }
}
