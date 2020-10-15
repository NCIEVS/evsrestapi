
package gov.nih.nci.evs.api.controller;

import org.junit.Before;
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

import com.fasterxml.jackson.databind.ObjectMapper;

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

		baseUrl = "/api/v1/concept/search";
	}

}
