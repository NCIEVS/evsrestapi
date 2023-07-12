
package gov.nih.nci.evs.api.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

import gov.nih.nci.evs.api.properties.TestProperties;
import gov.nih.nci.evs.api.util.ConceptUtils;

/**
 * Unit test for {@link ConceptUtils}.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ConceptUtilsTest {

  /** The logger. */
  @SuppressWarnings("unused")
  private static final Logger log = LoggerFactory.getLogger(ConceptUtilsTest.class);

  /** The test properties. */
  @Autowired
  TestProperties testProperties;

  @Test
  public void testIsCode() {
    assert (ConceptUtils.isCode(null) == false);
    assert (ConceptUtils.isCode("C3224") == true);
    assert (ConceptUtils.isCode("C19255") == true);
    assert (ConceptUtils.isCode("10038921") == true);
    assert (ConceptUtils.isCode("10003674") == true);
    assert (ConceptUtils.isCode("Hello my name is C3224") == false);
    assert (ConceptUtils.isCode("9737/3") == true);
    assert (ConceptUtils.isCode("8550/0") == true);
    assert (ConceptUtils.isCode("theraputic_agents") == true);
    assert (ConceptUtils.isCode("1,2- Hydroxybenzene") == false);
    assert (ConceptUtils.isCode("1,,3-dichloropropene") == false);
    assert (ConceptUtils.isCode("BAD,CODE,3224") == false);
    assert (ConceptUtils.isCode("MA:0000182") == true);
    assert (ConceptUtils.isCode("MA:0000879") == true);
    assert (ConceptUtils.isCode("GO:0016887") == true);
    assert (ConceptUtils.isCode("GO:0042113") == true);
    assert (ConceptUtils.isCode("HGNC:7") == true);
    assert (ConceptUtils.isCode("HGNC:13666") == true);
    assert (ConceptUtils.isCode("CHEBI:39478") == true);
    assert (ConceptUtils.isCode("CHEBI:47032") == true);
    assert (ConceptUtils.isCode("B20") == true);
    assert (ConceptUtils.isCode("R19.30") == true);
    assert (ConceptUtils.isCode("793.19") == true);
    assert (ConceptUtils.isCode("207.0") == true);
    assert (ConceptUtils.isCode("HIJK:L?5678/AB_C_") == false);
    assert (ConceptUtils.isCode("MNO:1234/5678.90.12") == false);

  }

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

    assert (ConceptUtils.normalizeWithStemming("appendices").equals("appendic"));
    assert (ConceptUtils.normalizeWithStemming("connecting").equals("connect"));
    assert (ConceptUtils.normalizeWithStemming("connective").equals("connect"));
    assert (ConceptUtils.normalizeWithStemming("connecting tissue").equals("connect tissu"));
    assert (ConceptUtils.normalizeWithStemming("All sites").equals("all site"));
    assert (ConceptUtils.normalizeWithStemming("cancerous").equals("cancer"));
  }

}
