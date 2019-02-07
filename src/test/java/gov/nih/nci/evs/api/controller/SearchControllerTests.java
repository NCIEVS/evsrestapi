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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

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
public class SearchControllerTests {
	
	 /** The logger. */
    private static final Logger log = LoggerFactory.getLogger(SearchControllerTests.class);

    
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
        
        baseUrl = "/api/v1/search";
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
    public void getSearchSimple() throws Exception {
        
    	        
        String url = baseUrl;
        log.info("Testing url - " + url);
       
        
        MvcResult result = this.mvc.perform(get(url).param("term","melanoma"))
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString();
       
        assertThat(content).isNotNull();
        
		JsonNode jsonNode = this.objectMapper.readTree(content);
		jsonNode = jsonNode.path("hits");		
		jsonNode = jsonNode.path("total");
		assertThat(jsonNode.intValue() > 0).isTrue();
               
       
        log.info("Done Testing getSearchSimple ");
        
    }
    
    
    @Test
    public void getSearchFormatClean() throws Exception {
        
    	        
        String url = baseUrl;
        log.info("Testing url - " + url);
       
        
        MvcResult result = this.mvc.perform(get(url).param("term","carcinoma").param("format","clean"))
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString();
       
        assertThat(content).isNotNull();
        
		JsonNode jsonNode = this.objectMapper.readTree(content);
		JsonNode jsonNodeTotal = jsonNode.path("total");
		assertThat(jsonNodeTotal.intValue() > 0).isTrue();
		JsonNode jsonNodeSize = jsonNode.path("size");
		assertThat(jsonNodeSize.intValue() > 0).isTrue();
		
		JsonNode jsonNodeHits = jsonNode.path("hits");		
        Iterator<JsonNode> iterator = jsonNodeHits.elements();   
        assertThat(iterator.hasNext()).isTrue();
        
        JsonNode node = iterator.next();
        node = node.path("Label");
        log.info(node.textValue());
        assertThat(node.textValue()).isEqualToIgnoringCase("Carcinoma");

        
               
       
        log.info("Done Testing getSearchFormatClean ");
        
    }
    
    
    @Test
    public void getSearchTypeMatch() throws Exception {
        
    	        
        String url = baseUrl;
        log.info("Testing url - " + url);
       
        
        MvcResult result = this.mvc.perform(get(url).param("term","Lung Carcinoma").param("format","clean").param("type", "match"))
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString();
       
        assertThat(content).isNotNull();
        
		JsonNode jsonNode = this.objectMapper.readTree(content);
		JsonNode jsonNodeTotal = jsonNode.path("total");
		assertThat(jsonNodeTotal.intValue() > 0).isTrue();
		JsonNode jsonNodeSize = jsonNode.path("size");
		assertThat(jsonNodeSize.intValue() > 0).isTrue();
		
		JsonNode jsonNodeHits = jsonNode.path("hits");		
        Iterator<JsonNode> iterator = jsonNodeHits.elements();   
        assertThat(iterator.hasNext()).isTrue();
        
        JsonNode node = iterator.next();
        node = node.path("Label");
        log.info(node.textValue());
        assertThat(node.textValue()).isEqualToIgnoringCase("Lung Carcinoma");

        
               
        
        log.info("Done Testing getSearchTypeMatch ");
        
    }
    
    @Test
    public void getSearchTypeFuzzy() throws Exception {
        
    	        
        String url = baseUrl;
        log.info("Testing url - " + url);
       
        
        MvcResult result = this.mvc.perform(get(url).param("term","enzymi").param("format","clean").param("type", "fuzzy"))
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString();
       
        assertThat(content).isNotNull();
       
		JsonNode jsonNode = this.objectMapper.readTree(content);
		JsonNode jsonNodeTotal = jsonNode.path("total");
		assertThat(jsonNodeTotal.intValue() > 0).isTrue();
		JsonNode jsonNodeSize = jsonNode.path("size");
		assertThat(jsonNodeSize.intValue() > 0).isTrue();
		
		JsonNode jsonNodeHits = jsonNode.path("hits");		
        Iterator<JsonNode> iterator = jsonNodeHits.elements();   
        assertThat(iterator.hasNext()).isTrue();
        
        JsonNode node = iterator.next();
        node = node.path("Label");
        log.info(node.textValue());
        assertThat(node.textValue()).isEqualToIgnoringCase("Enzyme");

        
               
      
        log.info("Done Testing getSearchTypeFuzzy ");
        
    }
    
    @Test
    public void getSearchProperty() throws Exception {
        
    	        
        String url = baseUrl;
        log.info("Testing url - " + url);
       
        
        MvcResult result = this.mvc.perform(get(url).param("term","XAV05295I5").param("format","cleanWithHighlights").param("property", "fda_unii_code"))
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString();
        log.info("content -" + content);
        assertThat(content).isNotNull();
        
		JsonNode jsonNode = this.objectMapper.readTree(content);
		JsonNode jsonNodeTotal = jsonNode.path("total");
		assertThat(jsonNodeTotal.intValue() > 0).isTrue();
		assertThat(jsonNodeTotal.intValue()).isEqualTo(1);
		
		
		JsonNode jsonNodeHits = jsonNode.path("hits");		
        Iterator<JsonNode> iterator = jsonNodeHits.elements();   
        assertThat(iterator.hasNext()).isTrue();
        
        JsonNode node = iterator.next();
        JsonNode nodeLabel = node.path("Label");
        log.info(nodeLabel.textValue());
        assertThat(nodeLabel.textValue()).isEqualToIgnoringCase("Sivifene");
        JsonNode nodeFDA = node.path("FDA_UNII_Code");
        Iterator<JsonNode> iteratorFDA = nodeFDA.elements();
        JsonNode nodeFDAValue = iteratorFDA.next();
        assertThat(nodeFDAValue.textValue()).isEqualToIgnoringCase("XAV05295I5");
        
               
      
        log.info("Done Testing getSearchProperty ");
        
    }

    
    @Test
    public void getSearchReturnProperty() throws Exception {
        
    	        
        String url = baseUrl;
        log.info("Testing url - " + url);
       
        
        MvcResult result = this.mvc.perform(get(url).param("term","carcinoma").param("format","clean").param("returnProperties", "roles"))
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString();
        //log.info("content -" + content);
        assertThat(content).isNotNull();
        
		JsonNode jsonNode = this.objectMapper.readTree(content);
		JsonNode jsonNodeTotal = jsonNode.path("total");
		assertThat(jsonNodeTotal.intValue() > 0).isTrue();
		
		
		
		JsonNode jsonNodeHits = jsonNode.path("hits");		
        Iterator<JsonNode> iterator = jsonNodeHits.elements();   
        assertThat(iterator.hasNext()).isTrue();
        
        JsonNode node = iterator.next();
       
        JsonNode nodeRole = node.path("Role");
        Iterator<JsonNode> iteratorRole = nodeRole.elements();
        assertThat(iterator.hasNext()).isTrue();
        
        JsonNode nodeDefintion = node.path("DEFINITION");
        assertThat(nodeDefintion).isEmpty();
        
        
        
        
        
      
        log.info("Done Testing getSearchReturnProperty ");
        
    }
    
    @Test
    public void getSearchReturnPropertyWithDefinition() throws Exception {
        
    	        
        String url = baseUrl;
        log.info("Testing url - " + url);
       
        
        MvcResult result = this.mvc.perform(get(url).param("term","carcinoma").param("format","clean").param("returnProperties", "roles,definition"))
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString();
        //log.info("content -" + content);
        assertThat(content).isNotNull();
        
		JsonNode jsonNode = this.objectMapper.readTree(content);
		JsonNode jsonNodeTotal = jsonNode.path("total");
		assertThat(jsonNodeTotal.intValue() > 0).isTrue();
		
		
		
		JsonNode jsonNodeHits = jsonNode.path("hits");		
        Iterator<JsonNode> iterator = jsonNodeHits.elements();   
        assertThat(iterator.hasNext()).isTrue();
        
        JsonNode node = iterator.next();
       
        JsonNode nodeRole = node.path("Role");
        Iterator<JsonNode> iteratorRole = nodeRole.elements();
        assertThat(iterator.hasNext()).isTrue();
        
        JsonNode nodeDefintion = node.path("DEFINITION");
        assertThat(nodeDefintion).isNotEmpty();
        
        
        
        
        
        
        log.info("Done Testing getSearchReturnPropertyWithDefinition ");
        
    }
    
    @Test
    public void getSearchSynonym() throws Exception {
        
    	        
        String url = baseUrl;
        log.info("Testing url - " + url);
       
        
        MvcResult result = this.mvc.perform(get(url).param("term","dsDNA").param("format","clean").param("synonymSource", "NCI"))
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString();
        //log.info("content -" + content);
        assertThat(content).isNotNull();
        
		JsonNode jsonNode = this.objectMapper.readTree(content);
		JsonNode jsonNodeTotal = jsonNode.path("total");
		assertThat(jsonNodeTotal.intValue() > 0).isTrue();
		assertThat(jsonNodeTotal.intValue()).isEqualTo(2);
		
		
		JsonNode jsonNodeHits = jsonNode.path("hits");		
        Iterator<JsonNode> iterator = jsonNodeHits.elements();   
        assertThat(iterator.hasNext()).isTrue();
        
        JsonNode node = iterator.next();
       
        JsonNode nodeSynonym = node.path("FULL_SYN");
        Iterator<JsonNode> iteratorSyn = nodeSynonym.elements();
       
        boolean found= false;
        while (iteratorSyn.hasNext()) {
            JsonNode syn = iteratorSyn.next();
            JsonNode synTerm = syn.path("term-name");
            if (synTerm.textValue().indexOf("dsDNA") > -1) {
            	JsonNode synSource = syn.path("term-source");
            	if (synSource.textValue().equalsIgnoreCase("NCI")) {
            		found = true;
            		break;
            	}
            }
         }
        
        
        assertThat(found).isTrue();
          
        
        
       
        log.info("Done Testing getSearchSynonym ");
        
    }
    
    @Test
    public void getSearchFilter() throws Exception {
        
    	        
        String url = baseUrl;
        log.info("Testing url - " + url);
       
        
        MvcResult result = this.mvc.perform(get(url).param("term","melanoma").param("format","clean").param("conceptStatus", "Retired_Concept"))
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString();
        //log.info("content -" + content);
        assertThat(content).isNotNull();
        
		JsonNode jsonNode = this.objectMapper.readTree(content);
		JsonNode jsonNodeTotal = jsonNode.path("total");
		assertThat(jsonNodeTotal.intValue() > 0).isTrue();
		assertThat(jsonNodeTotal.intValue()).isEqualTo(13);
		
		
		JsonNode jsonNodeHits = jsonNode.path("hits");		
        Iterator<JsonNode> iterator = jsonNodeHits.elements();   
        assertThat(iterator.hasNext()).isTrue();
        
        JsonNode node = iterator.next();
       
        JsonNode nodeStatus = node.path("Concept_Status");
        Iterator<JsonNode> iteratorStatus = nodeStatus.elements();
        assertThat(iteratorStatus.hasNext()).isTrue();
       
        
        JsonNode nodeStatusRetired= iteratorStatus.next();
        
        assertThat(nodeStatusRetired.textValue().equalsIgnoreCase("Retired_Concept"));
          
        
        
       
        log.info("Done Testing getSearchFilter ");
        
    }
}
