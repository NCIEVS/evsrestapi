package gov.nih.nci.evs.api.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.IncludeParam;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.model.TerminologyMetadata;
import gov.nih.nci.evs.api.service.GraphOpensearchLoadServiceImpl;
import gov.nih.nci.evs.api.service.OpensearchQueryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/** Unit test for TerminologyUtils. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class TerminologyUtilsTest {

  /** The term utils. */
  @Autowired private TerminologyUtils termUtils;

  /** The opensearch query service *. */
  @Autowired private OpensearchQueryService osQueryService;

  /** The graph loader service. */
  @Autowired private GraphOpensearchLoadServiceImpl graphOpensearchLoadServiceImpl;

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
        termUtils.getIndexedTerminologies(osQueryService).stream()
            .filter(
                t ->
                    t.getLatest() != null
                        && t.getLatest()
                        && t.getTags().containsKey("monthly")
                        && t.getTags().get("monthly").equals("true"))
            .findFirst()
            .get();
    final IncludeParam ip = new IncludeParam((String) null);

    final List<Concept> list = osQueryService.getQualifiers(ncit, ip);

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
    final List<Terminology> list = termUtils.getIndexedTerminologies(osQueryService);
    assertThat(list.size()).isGreaterThan(10);
  }

  /**
   * Test terminology metadata reading.
   *
   * @throws Exception the exception
   */
  @Test
  public void testMetadataReading() throws Exception {
    final String terminology = "ncit";
    final TerminologyMetadata metadata =
        new ObjectMapper()
            .treeToValue(
                graphOpensearchLoadServiceImpl.getMetadataAsNodeLocal(terminology),
                TerminologyMetadata.class);
    assertThat(metadata.getExtraSubsets()).isNotEmpty();
  }
}
