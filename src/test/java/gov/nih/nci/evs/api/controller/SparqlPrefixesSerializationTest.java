package gov.nih.nci.evs.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.nci.evs.api.configuration.JacksonConfiguration;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.service.OpensearchQueryService;
import gov.nih.nci.evs.api.service.QueryBuilderService;
import gov.nih.nci.evs.api.util.TerminologyUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Verifies the SPARQL prefix response without requiring indexed terminology data. */
@ExtendWith(MockitoExtension.class)
class SparqlPrefixesSerializationTest {

  /** The controller under test. */
  @InjectMocks private SearchController controller;

  /** Terminology lookup boundary. */
  @Mock private TerminologyUtils termUtils;

  /** The index lookup service passed to terminology lookup. */
  @Mock private OpensearchQueryService osQueryService;

  /** Prefix generation boundary. */
  @Mock private QueryBuilderService queryBuilderService;

  /**
   * One JSON parse should recover the original prefix string, including newlines.
   *
   * @throws Exception if the request fails
   */
  @Test
  @SuppressWarnings("removal")
  void prefixesAreSerializedExactlyOnce() throws Exception {
    final Terminology terminology = new Terminology();
    terminology.setSource("test");
    final String prefixes =
        "PREFIX :<http://example.org/>\nPREFIX owl:<http://www.w3.org/2002/07/owl#>";
    when(termUtils.getIndexedTerminology("ncit", osQueryService, true)).thenReturn(terminology);
    when(queryBuilderService.constructPrefix(terminology)).thenReturn(prefixes);
    final ObjectMapper mapper = new JacksonConfiguration().objectMapper();
    final MockMvc mvc =
        MockMvcBuilders.standaloneSetup(controller)
            .addPlaceholderValue("nci.evs.application.contextPath", "/api/v1")
            .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
            .build();

    final String body =
        mvc.perform(get("/api/v1/sparql/ncit/prefixes"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    final JsonNode parsed = mapper.readTree(body);
    assertTrue(parsed.isTextual());
    assertEquals(prefixes, parsed.asText());
  }
}
