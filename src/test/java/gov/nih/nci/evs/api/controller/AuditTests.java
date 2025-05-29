package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.model.Audit;
import gov.nih.nci.evs.api.model.SearchCriteria;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.properties.TestProperties;
import gov.nih.nci.evs.api.service.OpensearchQueryServiceImpl;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** audit tests. */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class AuditTests {

  /** The logger. */
  private static final Logger log = LoggerFactory.getLogger(AuditTests.class);

  /** The mvc. */
  @Autowired private MockMvc mvc;

  /** The test properties. */
  @Autowired TestProperties testProperties;

  /** The opensearch query service. */
  @Autowired private OpensearchQueryServiceImpl opensearchQueryService;

  /**
   * Test get all audits and check against terminologies.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetAllAudits() throws Exception {
    List<Audit> audits = opensearchQueryService.getAllAudits(new SearchCriteria());
    assertNotNull(audits);
    assertThat(audits.size()).isGreaterThan(0);

    // get terminologies to check against
    String url = "/api/v1/metadata/terminologies";
    log.info("Testing url - " + url);

    MvcResult result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
    String content = result.getResponse().getContentAsString();
    log.info("  content = " + content);
    List<Terminology> terminologies =
        new ObjectMapper()
            .readValue(
                content,
                new TypeReference<List<Terminology>>() {
                  // n/a
                });
    // check for corresponding terminology entries in audit list
    assertThat(terminologies).isNotEmpty();
    for (Terminology terminology : terminologies) {
      assertThat(
              audits.stream()
                  .anyMatch(
                      audit ->
                          (audit.getTerminology() != null
                                  && audit.getTerminology().equals(terminology.getTerminology()))
                              && (audit.getType() != null && audit.getType().equals("reindex"))))
          .isTrue();
    }
  }

  /**
   * Test get audits by terminology.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetAuditsByTerminology() throws Exception {
    List<Audit> audits = opensearchQueryService.getAuditsByTerminology("ncit");
    // filter errors and warnings
    audits =
        audits.stream()
            .filter(audit -> audit.getType().equals("reindex"))
            .collect(Collectors.toList());
    assertNotNull(audits);
    for (Audit audit : audits) {
      assertThat(audit.getTerminology()).isEqualTo("ncit");
      assertThat(audit.getType()).isEqualTo("reindex");
    }
    // check unique versions
    assertEquals(audits.stream().map(Audit::getVersion).distinct().count(), audits.size());
  }

  /**
   * Test get audits by type.
   *
   * @throws Exception the exception
   */
  @Test
  public void testGetAuditsByType() throws Exception {

    // Act
    List<Audit> audits = opensearchQueryService.getAuditsByType("reindex");
    assertNotNull(audits);
    // all entries are REINDEX
    assertThat(audits.stream().allMatch(audit -> audit.getType().equals("reindex"))).isTrue();
  }
}
