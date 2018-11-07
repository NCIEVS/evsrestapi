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
import gov.nih.nci.evs.api.model.evs.Paths;
import gov.nih.nci.evs.api.properties.TestProperties;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class EvsControllerTests {
	
	 /** The logger. */
    private static final Logger log = LoggerFactory.getLogger(EvsControllerTests.class);

    
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
        
        baseUrl = "/api/v1/concept/";
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
    public void getEvsConceptDetail() throws Exception {
        // @formatter:off

        log.info("Started Testing getEvsConceptDetail");
    	String conceptCodeList = testProperties.getConceptCodeList();
    	
        //conceptCodeList= "C7834,C3058,C125890,C2924,C4896,C54705,C7057,C2991,C3262,C4897,C7834,C48232";
        conceptCodeList= "C7834,C3058,C125890,C2924,C4896";
    	
    	String[] concepts = conceptCodeList.split(",");
    	List conceptsList = Arrays.asList(concepts); 
        
        
        String url = "";
       for(Object concept: conceptsList) {   
           url = baseUrl + (String)concept;
           log.info("Testing url - " + url);
    	   MvcResult result = this.mvc.perform(get(url))
                                   .andExpect(status().isOk())
                                   .andReturn();
    	   String content = result.getResponse().getContentAsString();
    	   int count = 0;
    	   
    	   //log.info("content *******" + content);
           assertThat(content).isNotNull();
           //EvsConceptByLabel evsConcept =  objectMapper.readValue(content, EvsConceptByLabel.class);
           //assertThat(evsConcept).isNotNull(); 
           // convert to Tree Node for manipulation
      	   ObjectMapper mapper = new ObjectMapper();
   		   JsonNode jsonNode = mapper.readTree(content);
   		   String code = jsonNode.findValue("Code").asText();
          
   		   if (code.equals("C7834")) {
   			   assertThat(code.equalsIgnoreCase("C7834")).isTrue();
   			   String label = jsonNode.findValue("Label").asText();
   			   assertThat(label.equalsIgnoreCase("Recurrent Childhood Brain Neoplasm")).isTrue();
   		   }
           
           if (code.equalsIgnoreCase("C3058")) {
   			   String label = jsonNode.findValue("Label").asText();
        	   assertThat(label.equalsIgnoreCase("Glioblastoma")).isTrue();
        	   List <JsonNode> synonyms = jsonNode.findValues("FULL_SYN");
        	   assertThat(synonyms.size() > 0).isTrue();
           }
           
           if (code.equalsIgnoreCase("C125890")) {
   			   String label = jsonNode.findValue("Label").asText();
        	   assertThat(label.equalsIgnoreCase("Small Cell Glioblastoma")).isTrue();
        	   JsonNode ns = jsonNode.get("Neoplastic_Status");
        	   Iterator<JsonNode> it = ns.elements();
        	   while (it.hasNext()) {
        		   JsonNode s = it.next();
        		   assertThat(s.asText().equals("Malignant"));
        		   
        	   }
   			   ns = jsonNode.get("Role");
   			   count = getCount(ns);
   			   assertThat(count > 0).isTrue();
           }

           if (code.equalsIgnoreCase("C2924")) {
   			   String p = jsonNode.findValue("Preferred_Name").asText();
        	   assertThat(p.equalsIgnoreCase("Ductal Breast Carcinoma In Situ")).isTrue();
        	   JsonNode ns = jsonNode.get("Semantic_Type");
        	   Iterator<JsonNode> it = ns.elements();
        	   while (it.hasNext()) {
        		   JsonNode s = it.next();
        		   assertThat(s.asText().equals("Neoplastic Process"));
        		   
        	   }
   			   ns = jsonNode.get("Association");
   			   count = getCount(ns);
   			   assertThat(count > 0).isTrue();
           }
           
           if (code.equalsIgnoreCase("C4896")) {
   			   String p = jsonNode.findValue("Preferred_Name").asText();
        	   assertThat(p.equalsIgnoreCase("Leukemia in Remission")).isTrue();
   			   JsonNode ns = jsonNode.get("FULL_SYN");
   			   count = getCount(ns);
   			   assertThat(count > 0).isTrue();
           }
           
           log.info("Successfully tested url - " + url);
        
       }
       log.info("Done Testing getEvsConceptDetail ");
     // @formatter:on
        
   }
    
    
    @Test
    public void getPathToRoot() throws Exception {
        
    	log.info("Started Testing getPathToRoot");        
        String url = baseUrl + "C48232/pathToRoot";
        log.info("Testing url - " + url);
        
        MvcResult result = this.mvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString();


        assertThat(content).isNotNull();
        Paths paths =  objectMapper.readValue(content, Paths.class);
        assertThat(paths).isNotNull(); 
        assertThat(paths.getPaths().size() > 0 ).isTrue();
        log.info("Successfully tested url - " + url);
        log.info("Done Testing getPathToRoot ");
        
    }

}
