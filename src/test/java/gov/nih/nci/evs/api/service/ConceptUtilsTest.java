package gov.nih.nci.evs.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.properties.TestProperties;
import gov.nih.nci.evs.api.util.ConceptUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/** Unit test for {@link ConceptUtils}. */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ConceptUtilsTest {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(ConceptUtilsTest.class);

  /** The test properties. */
  @Autowired TestProperties testProperties;

  /** Test is code. */
  @Test
  public void testIsCode() {
    assertFalse(ConceptUtils.isCode(null));
    assertTrue(ConceptUtils.isCode("C3224"));
    assertTrue(ConceptUtils.isCode("C19255"));
    assertTrue(ConceptUtils.isCode("10038921"));
    assertTrue(ConceptUtils.isCode("10003674"));
    assertFalse(ConceptUtils.isCode("Hello my name is C3224"));
    assertTrue(ConceptUtils.isCode("9737/3"));
    assertTrue(ConceptUtils.isCode("8550/0"));
    assertTrue(ConceptUtils.isCode("theraputic_agents"));
    assertFalse(ConceptUtils.isCode("1,2- Hydroxybenzene"));
    assertFalse(ConceptUtils.isCode("1,,3-dichloropropene"));
    assertFalse(ConceptUtils.isCode("BAD,CODE,3224"));
    assertTrue(ConceptUtils.isCode("MA:0000182"));
    assertTrue(ConceptUtils.isCode("MA:0000879"));
    assertTrue(ConceptUtils.isCode("GO:0016887"));
    assertTrue(ConceptUtils.isCode("GO:0042113"));
    assertTrue(ConceptUtils.isCode("HGNC:7"));
    assertTrue(ConceptUtils.isCode("HGNC:13666"));
    assertTrue(ConceptUtils.isCode("CHEBI:39478"));
    assertTrue(ConceptUtils.isCode("CHEBI:47032"));
    assertTrue(ConceptUtils.isCode("B20"));
    assertTrue(ConceptUtils.isCode("R19.30"));
    assertTrue(ConceptUtils.isCode("793.19"));
    assertTrue(ConceptUtils.isCode("207.0"));
    assertFalse(ConceptUtils.isCode("HIJK:L?5678/AB_C_"));
    assertFalse(ConceptUtils.isCode("MNO:1234/5678.90.12"));
  }

  /** Test normalize with stemming. */
  @Test
  public void testNormalizeWithStemming() {
    // change working into asserts
    // two categories of working and not working
    // cancerous > 100 contains
    // connecting tissue none in match, phrase, startswith
    // >100 in AND
    log.info(ConceptUtils.normalizeWithStemming("fungi"));
    log.info(ConceptUtils.normalizeWithStemming("fungal growth"));
    log.info(ConceptUtils.normalizeWithStemming("fungal"));
    log.info(ConceptUtils.normalizeWithStemming("cactus"));
    log.info(ConceptUtils.normalizeWithStemming("cacti"));
    log.info(ConceptUtils.normalizeWithStemming("appendix"));
    log.info(ConceptUtils.normalizeWithStemming("chronic"));
    log.info(ConceptUtils.normalizeWithStemming("granulomatous"));

    assertEquals("appendic", ConceptUtils.normalizeWithStemming("appendices"));
    assertEquals("connect", ConceptUtils.normalizeWithStemming("connecting"));
    assertEquals("connect", ConceptUtils.normalizeWithStemming("connective"));
    assertEquals("connect tissu", ConceptUtils.normalizeWithStemming("connecting tissue"));
    assertEquals("cancer site", ConceptUtils.normalizeWithStemming("cancerous sites"));
    assertEquals("subset display", ConceptUtils.normalizeWithStemming("subsets displays"));
    assertEquals("all site", ConceptUtils.normalizeWithStemming("All sites"));
    assertEquals("cancer", ConceptUtils.normalizeWithStemming("cancerous"));
  }

  /**
   * Test that ancestors are dropped unless included, as descendants are.
   *
   * @throws Exception the exception
   */
  @Test
  public void testApplyIncludeAncestors() throws Exception {
    final Concept concept = getConceptWithAncestors(5);
    ConceptUtils.applyInclude(concept, new IncludeParam("ancestors"));
    assertEquals(5, concept.getAncestors().size());

    ConceptUtils.applyInclude(concept, new IncludeParam("minimal"));
    assertTrue(concept.getAncestors().isEmpty());
  }

  /**
   * Test that the list limit applies to ancestors, adding the "ct" placeholder for the total.
   *
   * @throws Exception the exception
   */
  @Test
  public void testApplyLimitAncestors() throws Exception {
    final Concept concept = getConceptWithAncestors(5);

    ConceptUtils.applyLimit(concept, 2);

    assertEquals(3, concept.getAncestors().size());
    assertEquals(5, concept.getAncestors().get(2).getCt());
  }

  /**
   * Returns a concept with the given number of ancestors.
   *
   * @param count the number of ancestors
   * @return the concept
   */
  private Concept getConceptWithAncestors(final int count) {
    final Concept concept = new Concept("X");
    for (int i = 1; i <= count; i++) {
      concept.getAncestors().add(new Concept("P" + i));
    }
    return concept;
  }
}
