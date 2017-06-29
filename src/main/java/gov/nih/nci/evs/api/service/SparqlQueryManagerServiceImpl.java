package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.nci.evs.api.model.evs.EvsAssociation;
import gov.nih.nci.evs.api.model.evs.EvsAxiom;
import gov.nih.nci.evs.api.model.evs.EvsConcept;
import gov.nih.nci.evs.api.model.evs.EvsProperty;
import gov.nih.nci.evs.api.model.evs.EvsRelationships;
import gov.nih.nci.evs.api.model.evs.EvsSubconcept;
import gov.nih.nci.evs.api.model.evs.EvsSuperconcept;
import gov.nih.nci.evs.api.model.sparql.Bindings;
import gov.nih.nci.evs.api.model.sparql.Sparql;
import gov.nih.nci.evs.api.properties.StardogProperties;
import gov.nih.nci.evs.api.util.EVSUtils;
import gov.nih.nci.evs.api.util.RESTUtils;

@Service
public class SparqlQueryManagerServiceImpl implements SparqlQueryManagerService {

	private static final Logger log = LoggerFactory.getLogger(SparqlQueryManagerServiceImpl.class);

	@Autowired
	StardogProperties stardogProperties;

	@Autowired
	QueryBuilderService queryBuilderService;

	private RESTUtils restUtils = null;
	
	private HashMap<String,String> diseaseconcepts;

	@PostConstruct
	public void postInit() throws IOException{
		restUtils = new RESTUtils(stardogProperties.getQueryUrl(), stardogProperties.getUsername(),
				stardogProperties.getPassword(),stardogProperties.getReadTimeout(),stardogProperties.getConnectTimeout());
		diseaseconcepts = getDiseaseIsStageSourceCodes();
		
	}
	
	public String getNamedGraph() {
		return stardogProperties.getGraphName();
	}
	
	
	public boolean checkConceptExists(String conceptCode) throws JsonMappingException,JsonParseException ,IOException{
		
		String queryPrefix = queryBuilderService.contructPrefix();
		String namedGraph = getNamedGraph();
		String query = queryBuilderService.constructConceptLabelQuery(conceptCode, namedGraph);
		String res = restUtils.runSPARQL(queryPrefix + query);
		

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		boolean conceptExists = false;
		
		Sparql sparqlResult = mapper.readValue(res, Sparql.class);
		Bindings[] bindings = sparqlResult.getResults().getBindings();
			
		for (Bindings b : bindings) {
			String conceptLabel = b.getConceptLabel().getValue();
			if (conceptLabel != null && !conceptLabel.equalsIgnoreCase("")){
				conceptExists = true;
			}
		}
		
		return conceptExists;
		
	}

	public String getEvsConceptLabel(String conceptCode) throws JsonMappingException,JsonParseException ,IOException{
		
		String queryPrefix = queryBuilderService.contructPrefix();
		String namedGraph = getNamedGraph();
		String query = queryBuilderService.constructConceptLabelQuery(conceptCode, namedGraph);
		String res = restUtils.runSPARQL(queryPrefix + query);
		

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		String conceptLabel = null;

		Sparql sparqlResult = mapper.readValue(res, Sparql.class);
		Bindings[] bindings = sparqlResult.getResults().getBindings();
		if (bindings.length == 1) {
			conceptLabel =  bindings[0].getConceptLabel().getValue();
		}
		
		return conceptLabel;
		
	}

	public List<EvsProperty> getEvsProperties(String conceptCode) throws JsonMappingException,JsonParseException ,IOException {

		String queryPrefix = queryBuilderService.contructPrefix();
		String namedGraph = getNamedGraph();
		String query = queryBuilderService.constructPropertyQuery(conceptCode,namedGraph);
		String res = restUtils.runSPARQL(queryPrefix + query);

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		ArrayList<EvsProperty> evsProperties = new ArrayList<EvsProperty>();
	
		Sparql sparqlResult = mapper.readValue(res, Sparql.class);
		Bindings[] bindings = sparqlResult.getResults().getBindings();
		for (Bindings b : bindings) {
			EvsProperty evsProperty = new EvsProperty();
			evsProperty.setCode(b.getPropertyCode().getValue());
			evsProperty.setLabel(b.getPropertyLabel().getValue());
			evsProperty.setValue(b.getPropertyValue().getValue());
			evsProperties.add(evsProperty);
		}
		
		return evsProperties;
	}

	public List<EvsAxiom> getEvsAxioms(String conceptCode) throws JsonMappingException,JsonParseException ,IOException {

		String queryPrefix = queryBuilderService.contructPrefix();
		String namedGraph = getNamedGraph();
		String query = queryBuilderService.constructAxiomQuery(conceptCode,namedGraph);
		String res = restUtils.runSPARQL(queryPrefix + query);

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		ArrayList<EvsAxiom> evsAxioms = new ArrayList<EvsAxiom>();
		
		Sparql sparqlResult = mapper.readValue(res, Sparql.class);
		Bindings[] bindings = sparqlResult.getResults().getBindings();
		EvsAxiom evsAxiom = new EvsAxiom();
		Boolean sw = false;
		String oldAxiom = "";
		for (Bindings b : bindings) {
			String axiom = b.getAxiom().getValue();
			String property = b.getAxiomProperty().getValue().split("#")[1];
			String value = b.getAxiomValue().getValue();
			if (value.contains("#")) {
				value = value.split("#")[1];
			}

			if (sw && !axiom.equals(oldAxiom)) {
				evsAxioms.add(evsAxiom);
				evsAxiom = new EvsAxiom();
			}
			sw = true;
			oldAxiom = axiom;

			switch (property) {
			case "annotatedSource":
				evsAxiom.setAnnotatedSource(value);
				break;
			case "annotatedTarget":
				evsAxiom.setAnnotatedTarget(value);
				break;
			case "annotatedProperty":
				evsAxiom.setAnnotatedProperty(value);
				break;
			case "type":
				evsAxiom.setType(value);
				break;
			case "P380":
				evsAxiom.setDefinitionReviewDate(value);
				break;
			case "P379":
				evsAxiom.setDefinitionReviewerName(value);
				break;
			case "P393":
				evsAxiom.setRelationshipToTarget(value);
				break;
			case "P395":
				evsAxiom.setTargetCode(value);
				break;
			case "P394":
				evsAxiom.setTargetTermType(value);
				break;
			case "P396":
				evsAxiom.setTargetTerminology(value);
				break;
			case "P381":
				evsAxiom.setAttr(value);
				break;
			case "P378":
				evsAxiom.setDefSource(value);
				break;
			case "P389":
				evsAxiom.setGoEvi(value);
				break;
			case "P387":
				evsAxiom.setGoId(value);
				break;
			case "P390":
				evsAxiom.setGoSource(value);
				break;
			case "P385":
				evsAxiom.setSourceCode(value);
				break;
			case "P391":
				evsAxiom.setSourceDate(value);
				break;
			case "P386":
				evsAxiom.setSubsourceName(value);
				break;
			case "P383":
				evsAxiom.setTermGroup(value);
				break;
			case "P384":
				evsAxiom.setTermSource(value);
				break;
			default:
				break;

			}
			evsAxioms.add(evsAxiom);
		}
		
		return evsAxioms;
	}

	public List<EvsSubconcept> getEvsSubconcepts(String conceptCode) throws JsonMappingException,JsonParseException ,IOException {

		String queryPrefix = queryBuilderService.contructPrefix();
		String namedGraph = getNamedGraph();
		String query = queryBuilderService.constructSubconceptQuery(conceptCode,namedGraph);
		String res = restUtils.runSPARQL(queryPrefix + query);

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		ArrayList<EvsSubconcept> evsSubclasses = new ArrayList<EvsSubconcept>();
	
		Sparql sparqlResult = mapper.readValue(res, Sparql.class);
		Bindings[] bindings = sparqlResult.getResults().getBindings();
		for (Bindings b : bindings) {
			EvsSubconcept evsSubclass = new EvsSubconcept();
			evsSubclass.setSubclass(b.getSubclass().getValue());
			evsSubclass.setLabel(b.getSubclassLabel().getValue());
			evsSubclass.setCode(b.getSubclassCode().getValue());
			evsSubclasses.add(evsSubclass);
		}
		
		return evsSubclasses;
	}

	public List<EvsSuperconcept> getEvsSuperconcepts(String conceptCode) throws JsonMappingException,JsonParseException ,IOException {

		String queryPrefix = queryBuilderService.contructPrefix();
		String namedGraph = getNamedGraph();
		String query = queryBuilderService.constructSuperconceptQuery(conceptCode,namedGraph);
		String res = restUtils.runSPARQL(queryPrefix + query);

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		ArrayList<EvsSuperconcept> evsSuperclasses = new ArrayList<EvsSuperconcept>();
		
		Sparql sparqlResult = mapper.readValue(res, Sparql.class);
		Bindings[] bindings = sparqlResult.getResults().getBindings();
		for (Bindings b : bindings) {
			EvsSuperconcept evsSuperclass = new EvsSuperconcept();
			evsSuperclass.setSuperclass(b.getSuperclass().getValue());
			evsSuperclass.setLabel(b.getSuperclassLabel().getValue());
			evsSuperclass.setCode(b.getSuperclassCode().getValue());
			evsSuperclasses.add(evsSuperclass);
		}
		
		return evsSuperclasses;
	}
	
	public List<EvsAssociation> getEvsAssociations(String conceptCode) throws JsonMappingException,JsonParseException ,IOException {

		String queryPrefix = queryBuilderService.contructPrefix();
		String namedGraph = getNamedGraph();
		String query = queryBuilderService.constructAssociationsQuery(conceptCode,namedGraph);
		String res = restUtils.runSPARQL(queryPrefix + query);

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		ArrayList<EvsAssociation> evsAssociations = new ArrayList<EvsAssociation>();
		
		Sparql sparqlResult = mapper.readValue(res, Sparql.class);
		Bindings[] bindings = sparqlResult.getResults().getBindings();
		for (Bindings b : bindings) {
			EvsAssociation evsAssociation = new EvsAssociation();
			evsAssociation.setRelationship(b.getRelationship().getValue());
			evsAssociation.setRelatedConceptCode(b.getRelatedConceptCode().getValue());
			evsAssociation.setRelatedConceptLabel(b.getRelatedConceptLabel().getValue());
			evsAssociations.add(evsAssociation);
		}
		
		return evsAssociations;
	}

	public List<EvsAssociation> getEvsInverseAssociations(String conceptCode) throws JsonMappingException,JsonParseException ,IOException {

		String queryPrefix = queryBuilderService.contructPrefix();
		String namedGraph = getNamedGraph();
		String query = queryBuilderService.constructInverseAssociationsQuery(conceptCode,namedGraph);
		String res = restUtils.runSPARQL(queryPrefix + query);

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		ArrayList<EvsAssociation> evsAssociations = new ArrayList<EvsAssociation>();
		
		Sparql sparqlResult = mapper.readValue(res, Sparql.class);
		Bindings[] bindings = sparqlResult.getResults().getBindings();
		for (Bindings b : bindings) {
			EvsAssociation evsAssociation = new EvsAssociation();
			evsAssociation.setRelationship(b.getRelationship().getValue());
			evsAssociation.setRelatedConceptCode(b.getRelatedConceptCode().getValue());
			evsAssociation.setRelatedConceptLabel(b.getRelatedConceptLabel().getValue());
			evsAssociations.add(evsAssociation);
		}
		
		return evsAssociations;
	}

	public List<EvsAssociation> getEvsInverseRoles(String conceptCode) throws JsonMappingException,JsonParseException ,IOException {

		String queryPrefix = queryBuilderService.contructPrefix();
		String namedGraph = getNamedGraph();
		String query = queryBuilderService.constructInverseRolesQuery(conceptCode,namedGraph);
		String res = restUtils.runSPARQL(queryPrefix + query);

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		ArrayList<EvsAssociation> evsAssociations = new ArrayList<EvsAssociation>();
		
		Sparql sparqlResult = mapper.readValue(res, Sparql.class);
		Bindings[] bindings = sparqlResult.getResults().getBindings();
		for (Bindings b : bindings) {
			EvsAssociation evsAssociation = new EvsAssociation();
			evsAssociation.setRelationship(b.getRelationship().getValue());
			evsAssociation.setRelatedConceptCode(b.getRelatedConceptCode().getValue());
			evsAssociation.setRelatedConceptLabel(b.getRelatedConceptLabel().getValue());
			evsAssociations.add(evsAssociation);
		}
		
		return evsAssociations;
	}

	public List<EvsAssociation> getEvsRoles(String conceptCode) throws JsonMappingException,JsonParseException ,IOException {

		String queryPrefix = queryBuilderService.contructPrefix();
		String namedGraph = getNamedGraph();
		String query = queryBuilderService.constructRolesQuery(conceptCode,namedGraph);
		String res = restUtils.runSPARQL(queryPrefix + query);
		System.out.println(queryPrefix + query);

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		ArrayList<EvsAssociation> evsAssociations = new ArrayList<EvsAssociation>();
		
		Sparql sparqlResult = mapper.readValue(res, Sparql.class);
		Bindings[] bindings = sparqlResult.getResults().getBindings();
		for (Bindings b : bindings) {
			EvsAssociation evsAssociation = new EvsAssociation();
			evsAssociation.setRelationship(b.getRelationship().getValue());
			evsAssociation.setRelatedConceptCode(b.getRelatedConceptCode().getValue());
			evsAssociation.setRelatedConceptLabel(b.getRelatedConceptLabel().getValue());
			evsAssociations.add(evsAssociation);
		}
		
		return evsAssociations;
	}
	
	
	public EvsConcept getEvsConceptDetail(String conceptCode) throws JsonMappingException,JsonParseException ,IOException{
		EvsConcept evsConcept = new EvsConcept();
		evsConcept.setLabel(getEvsConceptLabel(conceptCode));
		List <EvsProperty> properties = getEvsProperties(conceptCode);
		List <EvsAxiom> axioms = getEvsAxioms(conceptCode);
		evsConcept.setCode(EVSUtils.getConceptCode(properties));
		evsConcept.setDefinitions(EVSUtils.getFullDefinitions(axioms));
		evsConcept.setPreferredName(EVSUtils.getPreferredName(properties));
		evsConcept.setDisplayName(EVSUtils.getDisplayName(properties));
		evsConcept.setNeoplasticStatus(EVSUtils.getNeoplasticStatus(properties));
		evsConcept.setSemanticTypes(EVSUtils.getSemanticType(properties));
		List <EvsSubconcept> subconcepts = getEvsSubconcepts(conceptCode);
		List <EvsSuperconcept> superconcepts = getEvsSuperconcepts(conceptCode);
		evsConcept.setSubconcepts(subconcepts);
		evsConcept.setSuperconcepts(superconcepts);
		evsConcept.setSynonyms(EVSUtils.getFullSynonym(axioms));
		evsConcept.setAdditionalProperties(EVSUtils.getAdditionalProperties(properties));
		
		/*
		 * THIS SECTION FOR TESTING WILL BE REMOVED AT SOME POINT
		 */
		/*
		String queryPrefix = queryBuilderService.contructPrefix();
		String namedGraph = getNamedGraph();
		String query = queryBuilderService.constructRolesQuery(conceptCode, namedGraph);
		System.out.println(queryPrefix + query);
		*/
		
		//log.info(stardogProperties.getGraphName());
		//HashMap<String,String> diseaseConcepts = getDiseaseIsStageSourceCodes(stardogProperties.getGraphName());
		if (diseaseconcepts.containsKey(conceptCode)){		
			evsConcept.setIsStage(true);
		} else{
			evsConcept.setIsStage(false);
		}
		
		/*
		try {
            ObjectMapper writer = new ObjectMapper();
            System.out.println(writer.writerWithDefaultPrettyPrinter().writeValueAsString(concepts));
        } catch (Exception ex){
        	System.err.println(ex);
        }	
        */
		
		return evsConcept;
	}

	public EvsRelationships getEvsRelationships(String conceptCode) throws JsonMappingException,JsonParseException ,IOException{
		EvsRelationships relationships = new EvsRelationships();
		relationships.setSubconcepts(getEvsSubconcepts(conceptCode));
		relationships.setSuperconcepts(getEvsSuperconcepts(conceptCode));
		relationships.setAssociations(getEvsAssociations(conceptCode));
		relationships.setInverseAssociations(getEvsInverseAssociations(conceptCode));
		relationships.setRoles(getEvsRoles(conceptCode));
		relationships.setInverseRoles(getEvsInverseRoles(conceptCode));
		
		return relationships; 
	}
	
	
	
	public HashMap<String,String> getDiseaseIsStageSourceCodes() throws JsonMappingException,JsonParseException, IOException {
		log.info("***** In getDiseaseIsStageSourceCodes******");
		String queryPrefix = queryBuilderService.contructPrefix();
		String namedGraph = getNamedGraph();
		String query = queryBuilderService.constructDiseaseIsStageSourceCodesQuery(namedGraph);
		String res = restUtils.runSPARQL(queryPrefix + query);
		

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		HashMap<String,String> diseaseConcepts = new HashMap<String,String>();
		
			Sparql sparqlResult = mapper.readValue(res, Sparql.class);
			Bindings[] bindings = sparqlResult.getResults().getBindings();
			for (Bindings b : bindings) {
				diseaseConcepts.put(b.getConceptCode().getValue(), b.getConceptLabel().getValue());
				/*if (b.getConceptCode().getValue().equalsIgnoreCase(conceptCode)){
					isStage = true;
					break;
				}*/
				//EvsConcept concept = new EvsConcept();
				//concept.setLabel(b.getConceptLabel().getValue());
				//concept.setCode(b.getConceptCode().getValue());
				//concepts.add(concept);
			}
		
		return diseaseConcepts;
	}
}
