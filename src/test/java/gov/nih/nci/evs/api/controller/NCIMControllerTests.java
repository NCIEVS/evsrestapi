
package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Terminology;
import gov.nih.nci.evs.api.properties.TestProperties;

/**
 * The Class SearchControllerTests.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class NCIMControllerTests {

	/** The logger. */
	private static final Logger log = LoggerFactory.getLogger(NCIMControllerTests.class);

	/** The mvc. */
	@Autowired
	private MockMvc mvc;

	/** The test properties. */
	@Autowired
	TestProperties testProperties;

	/** The object mapper. */
	private ObjectMapper objectMapper;

	/** The base url. */
	private String baseUrl = "";

	/**
	 * Sets the up.
	 */
	@Before
	public void setUp() {
		/*
		 * Configure the JacksonTester object
		 */
		this.objectMapper = new ObjectMapper();
		JacksonTester.initFields(this, objectMapper);

		baseUrl = "/api/v1/concept";
	}

	/**
	 * NCIM terminology basic tests
	 *
	 * @throws Exception the exception
	 */
	@Test
	public void testNCIMTerminology() throws Exception {
		String url = null;
		MvcResult result = null;
		String content = null;

		// test if ncim term exists
		url = "/api/v1/metadata/terminologies";
		log.info("Testing url - " + url);
		result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
		content = result.getResponse().getContentAsString();
		final List<Terminology> terminologies = new ObjectMapper().readValue(content,
				new TypeReference<List<Terminology>>() {
				});
		assertThat(terminologies.size()).isGreaterThan(1);
		assertThat(terminologies.get(1).getTerminology().equals("ncim"));
		assertThat(terminologies.get(1).getLatest()).isTrue();
	}

	/**
	 * MRCONSO basic tests
	 *
	 * @throws Exception the exception
	 */
	@Test
	public void testMRCONSO() throws Exception {
		String url = null;
		MvcResult result = null;
		String content = null;
		Concept concept = null;

		// first concept in MRCONSO
		url = baseUrl + "/ncim/C0000005";
		log.info("Testing url - " + url);
		result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
		content = result.getResponse().getContentAsString();
		log.info(" content = " + content);
		concept = new ObjectMapper().readValue(content, Concept.class);
		assertThat(concept).isNotNull();
		assertThat(concept.getCode()).isEqualTo("C0000005");
		assertThat(concept.getName()).isEqualTo("(131)I-Macroaggregated Albumin");
		assertThat(concept.getTerminology()).isEqualTo("ncim");

		// last concept in MRCONSO
		url = baseUrl + "/ncim/CL990362";
		log.info("Testing url - " + url + "?terminology=ncim&code=CL990362");
		result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
		content = result.getResponse().getContentAsString();
		log.info(" content = " + content);
		concept = new ObjectMapper().readValue(content, Concept.class);
		assertThat(concept).isNotNull();
		assertThat(concept.getCode()).isEqualTo("CL990362");
		assertThat(concept.getName()).isEqualTo("Foundational Model of Anatomy Ontology, 4_15");
		assertThat(concept.getTerminology()).isEqualTo("ncim");

		// test NOCODES properly applying
		url = baseUrl + "/ncim/C0000985";
		log.info("Testing url - " + url + "?terminology=ncim&code=C0000985");
		result = mvc.perform(get(url)).andExpect(status().isOk()).andReturn();
		content = result.getResponse().getContentAsString();
		log.info(" content = " + content);
		concept = new ObjectMapper().readValue(content, Concept.class);
		assertThat(concept).isNotNull();
		assertThat(concept.getCode()).isEqualTo("C0000985");
		assertThat(concept.getName()).isEqualTo("Acetic Acids");
		assertThat(concept.getTerminology()).isEqualTo("ncim");
		assertThat(concept.getSynonyms().get(1).getCode()).isNull();

	}

}
