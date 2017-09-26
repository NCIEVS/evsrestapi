package gov.nih.nci.evs.api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;


import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.evs.EvsAssociation;
import gov.nih.nci.evs.api.model.evs.EvsAxiom;
import gov.nih.nci.evs.api.model.evs.EvsConcept;
import gov.nih.nci.evs.api.model.evs.EvsProperty;
import gov.nih.nci.evs.api.model.evs.EvsRelationships;
import gov.nih.nci.evs.api.model.evs.EvsSubconcept;
import gov.nih.nci.evs.api.model.evs.EvsSuperconcept;
import gov.nih.nci.evs.api.model.evs.Paths;
import gov.nih.nci.evs.api.properties.TestProperties;

import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.json.JacksonTester;

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
        
        baseUrl = "/api/v1/ctrp/concept/";
    }
    
    
    @Test
    public void testGetEvsConceptDetail() throws Exception {
        // @formatter:off

        log.info("Testing");
    	String conceptCodeList = testProperties.getConceptCodeList();
    	
        conceptCodeList= "C7834,C3058,C125890,C2924,C4896,C54705,C7057,C2991,C3262,C4897,C7834,C48232";
    	
    	String[] concepts = conceptCodeList.split(",");
    	
    	List conceptsList = Arrays.asList(concepts); 
        
        
        String url = "";
       for(Object concept: conceptsList) {   
           url = baseUrl + (String)concept;
           log.info("url-" + url);
    	   MvcResult result = this.mvc.perform(get(url))
                                   .andExpect(status().isOk())
                                   .andReturn();
    	   String content = result.getResponse().getContentAsString();
    	   
    	   //log.info("content *******" + content);
           assertThat(content).isNotNull();
           EvsConcept evsConcept =  objectMapper.readValue(content, EvsConcept.class);
           assertThat(evsConcept).isNotNull(); 
          
           log.info("preferred name " + evsConcept.getPreferredName());
           
           if (evsConcept.getCode().equalsIgnoreCase("C7834")) {
        	   assertThat(evsConcept.getIsSubtype()).isFalse();
        	   assertThat(evsConcept.getIsDiseaseStage()).isTrue();
        	   assertThat(evsConcept.getIsDiseaseGrade()).isFalse();
        	   assertThat(evsConcept.getIsDisease()).isTrue();
        	   assertThat(evsConcept.getSubconcepts().size() > 0).isTrue();
        	   assertThat(evsConcept.getSuperconcepts().size() > 0).isTrue();
        	   assertThat(evsConcept.getSynonyms().size() > 0).isTrue();
        	   assertThat(evsConcept.getAdditionalProperties().size() > 0).isTrue();
           }
           
           if (evsConcept.getCode().equalsIgnoreCase("C3058")) {
        	   assertThat(evsConcept.getIsMainType()).isTrue();
        	   assertThat(evsConcept.getIsSubtype()).isTrue();
        	   assertThat(evsConcept.getIsDiseaseStage()).isFalse();
        	   assertThat(evsConcept.getIsDiseaseGrade()).isTrue();
        	   assertThat(evsConcept.getIsDisease()).isTrue();
           }
           
           if (evsConcept.getCode().equalsIgnoreCase("C125890")) {
        	   assertThat(evsConcept.getIsMainType()).isFalse();
        	   assertThat(evsConcept.getIsSubtype()).isTrue();
        	   assertThat(evsConcept.getIsDiseaseStage()).isFalse();
        	   assertThat(evsConcept.getIsDiseaseGrade()).isTrue();
        	   assertThat(evsConcept.getIsDisease()).isTrue();
           }
           
           if (evsConcept.getCode().equalsIgnoreCase("C2924")) {
        	   assertThat(evsConcept.getIsMainType()).isFalse();
        	   assertThat(evsConcept.getIsSubtype()).isTrue();
        	   assertThat(evsConcept.getIsDiseaseStage()).isTrue();
        	   assertThat(evsConcept.getIsDiseaseGrade()).isTrue();
        	   assertThat(evsConcept.getIsDisease()).isTrue();
           }
           
           if (evsConcept.getCode().equalsIgnoreCase("C4896")) {
        	   assertThat(evsConcept.getIsMainType()).isTrue();
        	   assertThat(evsConcept.getIsSubtype()).isFalse();
        	   assertThat(evsConcept.getIsDiseaseStage()).isFalse();
        	   assertThat(evsConcept.getIsDiseaseGrade()).isFalse();
        	   assertThat(evsConcept.getIsDisease()).isTrue();
           }
           
           if (evsConcept.getCode().equalsIgnoreCase("C54705")) {
        	   assertThat(evsConcept.getIsMainType()).isTrue();
        	   assertThat(evsConcept.getIsSubtype()).isFalse();
        	   assertThat(evsConcept.getIsDiseaseStage()).isFalse();
        	   assertThat(evsConcept.getIsDiseaseGrade()).isFalse();
        	   assertThat(evsConcept.getIsDisease()).isTrue();
           }
           
           if (evsConcept.getCode().equalsIgnoreCase("C7057")) {
        	   assertThat(evsConcept.getIsMainType()).isFalse();
        	   assertThat(evsConcept.getIsSubtype()).isFalse();
        	   assertThat(evsConcept.getIsDiseaseStage()).isFalse();
        	   assertThat(evsConcept.getIsDiseaseGrade()).isFalse();
        	   assertThat(evsConcept.getIsDisease()).isTrue();
           }
           
           if (evsConcept.getCode().equalsIgnoreCase("C2991")) {
        	   assertThat(evsConcept.getIsMainType()).isTrue();
        	   assertThat(evsConcept.getIsSubtype()).isFalse();
        	   assertThat(evsConcept.getIsDiseaseStage()).isFalse();
        	   assertThat(evsConcept.getIsDiseaseGrade()).isFalse();
        	   assertThat(evsConcept.getIsDisease()).isTrue();
           }
           
           if (evsConcept.getCode().equalsIgnoreCase("C3262")) {
        	   assertThat(evsConcept.getIsMainType()).isTrue();
        	   assertThat(evsConcept.getIsSubtype()).isFalse();
        	   assertThat(evsConcept.getIsDiseaseStage()).isFalse();
        	   assertThat(evsConcept.getIsDiseaseGrade()).isFalse();
        	   assertThat(evsConcept.getIsDisease()).isTrue();
           }
           
           if (evsConcept.getCode().equalsIgnoreCase("C4897")) {
        	   assertThat(evsConcept.getIsMainType()).isFalse();
        	   assertThat(evsConcept.getIsSubtype()).isTrue();
        	   assertThat(evsConcept.getIsDiseaseStage()).isFalse();
        	   assertThat(evsConcept.getIsDiseaseGrade()).isFalse();
        	   assertThat(evsConcept.getIsDisease()).isTrue();
           }
           
           if (evsConcept.getCode().equalsIgnoreCase("C7834")) {
        	   assertThat(evsConcept.getIsMainType()).isFalse();
        	   assertThat(evsConcept.getIsSubtype()).isFalse();
        	   assertThat(evsConcept.getIsDiseaseStage()).isTrue();
        	   assertThat(evsConcept.getIsDiseaseGrade()).isFalse();
        	   assertThat(evsConcept.getIsDisease()).isTrue();
           }
           
           if (evsConcept.getCode().equalsIgnoreCase("C48232")) {
        	   assertThat(evsConcept.getIsMainType()).isFalse();
        	   assertThat(evsConcept.getIsSubtype()).isFalse();
        	   assertThat(evsConcept.getIsDiseaseStage()).isFalse();
        	   assertThat(evsConcept.getIsDiseaseGrade()).isFalse();
        	   assertThat(evsConcept.getIsDisease()).isFalse();
        	   assertThat(evsConcept.getSubconcepts().size() > 0).isTrue();
        	   assertThat(evsConcept.getSuperconcepts().size() > 0).isTrue();
        	   assertThat(evsConcept.getSynonyms().size() > 0).isTrue();
        	   assertThat(evsConcept.getAdditionalProperties().size() > 0).isTrue();
           }
           
           
        
       }
     // @formatter:on
        
   }
    
    
    @Test
    public void testGetEvsRelationships() throws Exception {
       

      
        
        
        String url = baseUrl + "C7834/relationships";
        
        MvcResult result = this.mvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString();


        assertThat(content).isNotNull();
        EvsRelationships evsRelationships =  objectMapper.readValue(content, EvsRelationships.class);
        assertThat(evsRelationships).isNotNull(); 
        assertThat(evsRelationships.getAssociations().size() > 0 ).isTrue();
        assertThat(evsRelationships.getRoles().size() > 0 ).isTrue();
        assertThat(evsRelationships.getInverseAssociations().size() == 0 ).isTrue();
    }
    
    
    @Test
    public void testGetPathToRoot() throws Exception {
        
        
        String url = baseUrl + "C48232/pathToRoot";
        
        MvcResult result = this.mvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString();


        assertThat(content).isNotNull();
        Paths paths =  objectMapper.readValue(content, Paths.class);
        assertThat(paths).isNotNull(); 
        assertThat(paths.getPaths().size() > 0 ).isTrue();
        
    }
    
    @Test
    public void testGetPropertiesForCode() throws Exception {
        
        
        String url = baseUrl + "C7834/properties";
        
        MvcResult result = this.mvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString();


        assertThat(content).isNotNull();
        List<EvsProperty> evsPropertyList =  objectMapper.readValue(content,  objectMapper.getTypeFactory().constructCollectionType(List.class, EvsProperty.class));
      
        assertThat(evsPropertyList).isNotNull(); 
        assertThat(evsPropertyList.size() > 0 ).isTrue();
        
    }
    
    @Test
    public void testGetAxiomsForCode() throws Exception {
        
        
        String url = baseUrl + "C2924/axioms";
        
        MvcResult result = this.mvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString();


        assertThat(content).isNotNull();
        List<EvsAxiom> evsAxiomList =  objectMapper.readValue(content,  objectMapper.getTypeFactory().constructCollectionType(List.class, EvsAxiom.class));
      
        assertThat(evsAxiomList).isNotNull(); 
        assertThat(evsAxiomList.size() > 0 ).isTrue();
        
    }
    
    @Test
    public void testGetSubconceptsForCode() throws Exception {
        
        
        String url = baseUrl + "C4897/subconcepts";
        
        MvcResult result = this.mvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString();


        assertThat(content).isNotNull();
        List <EvsSubconcept> evsSubconceptList =  objectMapper.readValue(content,  objectMapper.getTypeFactory().constructCollectionType(List.class, EvsSubconcept.class));
      
        assertThat(evsSubconceptList).isNotNull(); 
        assertThat(evsSubconceptList.size() > 0 ).isTrue();
        
    }
    
    @Test
    public void testGetSuperconceptsForCode() throws Exception {
        
        
        String url = baseUrl + "C4897/superconcepts";
        
        MvcResult result = this.mvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString();


        assertThat(content).isNotNull();
        List <EvsSuperconcept> evsSuperconceptList =  objectMapper.readValue(content,  objectMapper.getTypeFactory().constructCollectionType(List.class, EvsSuperconcept.class));
      
        assertThat(evsSuperconceptList).isNotNull(); 
        assertThat(evsSuperconceptList.size() > 0 ).isTrue();
        
    }
    
    @Test
    public void testGetAssociationsForCode() throws Exception {
        
        
        String url = baseUrl + "C2991/associations";
        
        MvcResult result = this.mvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString();


        assertThat(content).isNotNull();
        List <EvsAssociation> evsAssociationList =  objectMapper.readValue(content,  objectMapper.getTypeFactory().constructCollectionType(List.class, EvsAssociation.class));
      
        assertThat(evsAssociationList).isNotNull(); 
        assertThat(evsAssociationList.size() > 0 ).isTrue();
        
    }
    
    @Test
    public void testGetInverseAssociationsForCode() throws Exception {
        
        
        String url = baseUrl + "C2991/inverseassociations";
        
        MvcResult result = this.mvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString();


        assertThat(content).isNotNull();
        List <EvsAssociation> evsAssociationList =  objectMapper.readValue(content,  objectMapper.getTypeFactory().constructCollectionType(List.class, EvsAssociation.class));
      
        assertThat(evsAssociationList).isNotNull(); 
        assertThat(evsAssociationList.size() > 0 ).isTrue();
        
    }
    
    @Test
    public void testGetRolesForCode() throws Exception {
        
        
        String url = baseUrl + "C2924/roles";
        
        MvcResult result = this.mvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString();


        assertThat(content).isNotNull();
        List <EvsAssociation> evsAssociationList =  objectMapper.readValue(content,  objectMapper.getTypeFactory().constructCollectionType(List.class, EvsAssociation.class));
      
        assertThat(evsAssociationList).isNotNull(); 
        assertThat(evsAssociationList.size() > 0 ).isTrue();
        
    }
    
    @Test
    public void testgetInverseRolesForCode() throws Exception {
        
        
        String url = baseUrl + "C2924/inverseroles";
        
        MvcResult result = this.mvc.perform(get(url))
                .andExpect(status().isOk())
                .andReturn();
        String content = result.getResponse().getContentAsString();


        assertThat(content).isNotNull();
        List <EvsAssociation> evsAssociationList =  objectMapper.readValue(content,  objectMapper.getTypeFactory().constructCollectionType(List.class, EvsAssociation.class));
      
        assertThat(evsAssociationList).isNotNull(); 
        assertThat(evsAssociationList.size() > 0 ).isTrue();
        
    }

}
