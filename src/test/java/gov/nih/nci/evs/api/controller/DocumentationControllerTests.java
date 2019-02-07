package gov.nih.nci.evs.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Iterator;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.evs.EvsConcept;
import gov.nih.nci.evs.api.model.evs.EvsConceptByLabel;
import gov.nih.nci.evs.api.model.evs.HierarchyNode;
import gov.nih.nci.evs.api.model.evs.Paths;
import gov.nih.nci.evs.api.properties.TestProperties;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class DocumentationControllerTests {
	
	 /** The logger. */
    private static final Logger log = LoggerFactory.getLogger(DocumentationControllerTests.class);

    
    @Autowired
    private MockMvc mvc;

    @Autowired
    TestProperties testProperties;
    
    private ObjectMapper objectMapper;
    
    private String baseUrl = "";
    
    
    @Before
    public void setUp() {
         /*
         * Configure the JacksonTester object
         */
        this.objectMapper = new ObjectMapper();
        JacksonTester.initFields(this, objectMapper);
        
        baseUrl = "/api/v1/documentation";
    }
    
    public int getCount(JsonNode ns) {
    	int count = 0;
        Iterator<JsonNode> it = ns.elements();
        it = ns.iterator();
        while (it.hasNext()) {
            it.next();
            count++;
        }
    	return count;
    }
    
   
    @Test
    public void getAssociations() throws Exception {
        
    	        
        String url = baseUrl + "/associations";
        log.info("Testing url - " + url);
        
        MvcResult result = this.mvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString();
        assertThat(content).isNotNull();       
        log.info("Successfully tested url - " + url);
        log.info("Done Testing getAssociations ");
        
    }
    
    
    @Test
    public void getRoles() throws Exception {
        
    	        
        String url = baseUrl + "/roles";
        log.info("Testing url - " + url);
        
        MvcResult result = this.mvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString();
        assertThat(content).isNotNull();       
        log.info("Successfully tested url - " + url);
        log.info("Done Testing getRoles ");
        
    }
    

    @Test
    public void getProperties() throws Exception {
        
    	        
        String url = baseUrl + "/properties";
        log.info("Testing url - " + url);
        
        MvcResult result = this.mvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString();
        assertThat(content).isNotNull();       
        log.info("Successfully tested url - " + url);
        log.info("Done Testing getProperties");
        
    }
    
    

    @Test
    public void getGraphNames() throws Exception {        
    	        
        String url = baseUrl + "/graphnames";
        log.info("Testing url - " + url);
        
        MvcResult result = this.mvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString();
        assertThat(content).isNotNull();      
        log.info("Successfully tested url - " + url);
        log.info("Done Testing getGraphNames");
        
    }
    

}
