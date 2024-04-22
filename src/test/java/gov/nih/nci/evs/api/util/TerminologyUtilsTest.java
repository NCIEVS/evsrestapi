package gov.nih.nci.evs.api.util;

import static org.assertj.core.api.Assertions.assertThat;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.ElasticQueryService;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

/** Unit test for TerminologyUtils. */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class TerminologyUtilsTest {

  /** The term utils. */
  @Autowired private TerminologyUtils termUtils;

  /** The elastic query service *. */
  @Autowired private ElasticQueryService esQueryService;

  /** The logger. */
  @SuppressWarnings("unused")
  private static final Logger log = LoggerFactory.getLogger(TerminologyUtilsTest.class);

  /**
   * Test remodeled qualifiers.
   *
   * @throws Exception the exception
   */
  @Test
  public void testRemodeledQualifiers() throws Exception {

    final Terminology ncit =
        termUtils.getIndexedTerminologies(esQueryService).stream()
            .filter(
                t ->
                    t.getLatest() != null
                        && t.getLatest()
                        && t.getTags().containsKey("monthly")
                        && t.getTags().get("monthly").equals("true"))
            .findFirst()
            .get();
    final IncludeParam ip = new IncludeParam((String) null);

    final List<Concept> list = esQueryService.getQualifiers(ncit, ip);

    assertThat(
            list.stream().filter(c -> ncit.getMetadata().isRemodeledQualifier(c.getCode())).count())
        .isEqualTo(10);
  }

  /**
   * Test paging.
   *
   * @throws Exception the exception
   */
  @Test
  public void testPaging() throws Exception {

    // Verify more than one default page of data comes back
    final List<Terminology> list = termUtils.getIndexedTerminologies(esQueryService);
    assertThat(list.size()).isGreaterThan(10);
  }
}
