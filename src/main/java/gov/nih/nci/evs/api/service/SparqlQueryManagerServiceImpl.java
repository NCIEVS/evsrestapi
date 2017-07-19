package gov.nih.nci.evs.api.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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

import gov.nih.nci.evs.api.model.evs.Concept;
import gov.nih.nci.evs.api.model.evs.EvsAssociation;
import gov.nih.nci.evs.api.model.evs.EvsAxiom;
import gov.nih.nci.evs.api.model.evs.EvsConcept;
import gov.nih.nci.evs.api.model.evs.EvsConceptWithMainType;
import gov.nih.nci.evs.api.model.evs.EvsProperty;
import gov.nih.nci.evs.api.model.evs.EvsRelationships;
import gov.nih.nci.evs.api.model.evs.EvsSubconcept;
import gov.nih.nci.evs.api.model.evs.EvsSuperconcept;
import gov.nih.nci.evs.api.model.evs.Path;
import gov.nih.nci.evs.api.model.evs.Paths;
import gov.nih.nci.evs.api.model.sparql.Bindings;
import gov.nih.nci.evs.api.model.sparql.Sparql;
import gov.nih.nci.evs.api.properties.StardogProperties;
import gov.nih.nci.evs.api.util.EVSUtils;
import gov.nih.nci.evs.api.util.HierarchyUtils;
import gov.nih.nci.evs.api.util.PathFinder;
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
	private HashMap<String,String> diseaseGradeconcepts;
	private HashMap<String,String> diseaseMainconcepts;
	private Paths paths;
	
	 public static final String[] CTRP_MAIN_CANCER_TYPES = new String[] {
             "C27814", "C2916", "C2946", "C2947", "C2948", "C2955", "C2991", "C2996", "C3011", "C3059",
             "C3088", "C3099", "C3158", "C3161", "C3163", "C3167", "C3171", "C3172", "C3174", "C3178",
             "C3194", "C3208", "C3211", "C3224", "C3230", "C3234", "C3242", "C3247", "C3263", "C3267",
             "C3270", "C3367", "C3403", "C3411", "C3422", "C34448", "C3510", "C3513", "C35850", "C3708",
             "C3716", "C3728", "C3752", "C3753", "C3773", "C3790", "C3809", "C3844", "C3850", "C38661",
             "C3867", "C3917", "C40022", "C4290", "C4436", "C4536", "C4665", "C4699", "C4715", "C4741",
             "C4815", "C4817", "C4855", "C4863", "C4866", "C4872", "C4878", "C4896", "C4906", "C4908",
             "C4910", "C4911", "C4912", "C4914", "C5669", "C6142", "C61574", "C7062", "C7352", "C7539",
             "C7541", "C7558", "C7569", "C7724", "C7927", "C8711", "C8990", "C8993", "C9039", "C9061",
             "C9063", "C9087", "C9106", "C9118", "C9145", "C9272", "C9290", "C9291", "C9306", "C9309",
             "C9312", "C9325", "C9344", "C9349", "C9357", "C9382", "C9385", "C9466", "C9474", "C3908",
             "C9330", "C3868", "C9465", "C9315", "C54293", "C3871", "C9105"};
	
	private HierarchyUtils hierarchy = null;

	@PostConstruct
	public void postInit() throws IOException{
		restUtils = new RESTUtils(stardogProperties.getQueryUrl(), stardogProperties.getUsername(),
				stardogProperties.getPassword(),stardogProperties.getReadTimeout(),stardogProperties.getConnectTimeout());
		diseaseconcepts = getDiseaseIsStageSourceCodes();
		diseaseGradeconcepts = getDiseaseIsGradeSourceCodes();
		diseaseMainconcepts = getMainConcepts();
		List <String> parentchild = getHierarchy();
		hierarchy = new HierarchyUtils(parentchild);
		PathFinder pathFinder = new PathFinder(hierarchy);
		paths = pathFinder.findPaths();
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
			case "term-source":
				evsAxiom.setTermSource(value);
				break;
			case "term-group":
				evsAxiom.setTermGroup(value);
				break;			 	
			default:
				break;

			}
			
		}
		evsAxioms.add(evsAxiom);
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
		
		if (diseaseMainconcepts.containsKey(conceptCode)){	
			EvsConceptWithMainType evsConcept = new EvsConceptWithMainType();
			evsConcept.setIsMainType(true);
			return getConcept(evsConcept,conceptCode);
		} else{
			EvsConcept evsConcept = new EvsConcept();
			evsConcept = new EvsConcept();
			return getConcept(evsConcept,conceptCode);
		}
		/*evsConcept.setLabel(getEvsConceptLabel(conceptCode));
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
		
		
		if (diseaseconcepts.containsKey(conceptCode)){		
			evsConcept.setIsDiseaseStage(true);
		} else{
			evsConcept.setIsDiseaseStage(false);
		}
		
		
		//isGrade
		if (diseaseGradeconcepts.containsKey(conceptCode)){		
			evsConcept.setIsDiseaseGrade(true);
		} else{
			evsConcept.setIsDiseaseGrade(false);
		}
		
		
		
		
		
		return evsConcept;*/
	}
	
	public EvsConcept getConcept(EvsConcept evsConcept,String conceptCode) throws IOException{
		
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
			evsConcept.setIsDiseaseStage(true);
		} else{
			evsConcept.setIsDiseaseStage(false);
		}
		
		
		//isGrade
		if (diseaseGradeconcepts.containsKey(conceptCode)){		
			evsConcept.setIsDiseaseGrade(true);
		} else{
			evsConcept.setIsDiseaseGrade(false);
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
	
	public HashMap<String,String> getDiseaseIsGradeSourceCodes() throws JsonMappingException,JsonParseException, IOException {
		log.info("***** In getDiseaseIsGradeSourceCodes******");
		String queryPrefix = queryBuilderService.contructPrefix();
		String namedGraph = getNamedGraph();
		String query = queryBuilderService.constructDiseaseIsGradeSourceCodesQuery(namedGraph);
		String res = restUtils.runSPARQL(queryPrefix + query);
		

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		HashMap<String,String> diseaseGradeConcepts = new HashMap<String,String>();
		
		Sparql sparqlResult = mapper.readValue(res, Sparql.class);
		Bindings[] bindings = sparqlResult.getResults().getBindings();
		for (Bindings b : bindings) {
			diseaseGradeConcepts.put(b.getConceptCode().getValue(), b.getConceptLabel().getValue());
			/*if (b.getConceptCode().getValue().equalsIgnoreCase(conceptCode)){
				isStage = true;
				break;
			}*/
			//EvsConcept concept = new EvsConcept();
			//concept.setLabel(b.getConceptLabel().getValue());
			//concept.setCode(b.getConceptCode().getValue());
			//concepts.add(concept);
		}
		
		return diseaseGradeConcepts;
	}
	
	public HashMap<String,String> getMainConcepts(){
		
		HashMap<String,String> diseaseMainConcepts = new HashMap<String,String>();
		
		for (String s: CTRP_MAIN_CANCER_TYPES) {           
			diseaseMainConcepts.put(s,s);
	    }
		
		return diseaseMainConcepts;
		
	}
	
	
	public ArrayList <String> getHierarchy() throws JsonMappingException, JsonParseException, IOException {
		ArrayList<String> parentchild = new ArrayList <String>();
		log.info("***** In getHierarchy******");
		String queryPrefix = queryBuilderService.contructPrefix();
		String namedGraph = getNamedGraph();
		String query = queryBuilderService.constructHierarchyQuery(namedGraph);
		String res = restUtils.runSPARQL(queryPrefix + query);

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		
		Sparql sparqlResult = mapper.readValue(res, Sparql.class);
		Bindings[] bindings = sparqlResult.getResults().getBindings();
		for (Bindings b : bindings) {
			StringBuffer str = new StringBuffer();
			str.append(b.getParentCode().getValue());
			str.append("\t");
			str.append(b.getParentLabel().getValue());
			str.append("\t");
			str.append(b.getChildCode().getValue());
			str.append("\t");
			str.append(b.getChildLabel().getValue());
			str.append("\n");
			parentchild.add(str.toString());
		}
		
		return parentchild;
	}
	
	public Paths getPathToRoot(String code) {
		Paths conceptPaths = new Paths();
		for (Path path: paths.getPaths()) {
			Boolean sw = false;
			int idx = -1;
			List <Concept> concepts = path.getConcepts();
			for (int i = 0; i < concepts.size(); i++) {
				Concept concept = concepts.get(i);
				if (concept.getCode().equals(code)) {
					sw = true;
					idx = concept.getIdx();
				}
			}
			if (sw) {
				List <Concept>trimed_concepts = new ArrayList <Concept>();
				if (idx == -1) {
					idx = concepts.size()-1;
				}
				int j = 0;
				for (int i=idx; i >= 0; i--) {
					Concept c = new Concept();
					c.setCode(concepts.get(i).getCode());
					c.setLabel(concepts.get(i).getLabel());
					c.setIdx(j);
					j++;
					trimed_concepts.add(c);
				}
				conceptPaths.add(new Path(1,trimed_concepts));
			}
		}
		conceptPaths = removeDuplicatePaths(conceptPaths);
		return conceptPaths;
	}

	public Paths getPathToParent(String code, String parentCode) {
		Paths conceptPaths = new Paths();
		for (Path path: paths.getPaths()) {
			Boolean codeSW = false;
			Boolean parentSW = false;
			int idx = -1;
			List <Concept> concepts = path.getConcepts();
			for (int i = 0; i < concepts.size(); i++) {
				Concept concept = concepts.get(i);
				if (concept.getCode().equals(code)) {
					codeSW = true;
					idx = concept.getIdx();
				}
				if (concept.getCode().equals(parentCode)) {
					parentSW = true;
				}
			}
			if (codeSW && parentSW) {
				List <Concept>trimed_concepts = new ArrayList <Concept>();
				if (idx == -1) {
					idx = concepts.size()-1;
				}
				int j = 0;
				for (int i=idx; i >= 0; i--) {
					Concept c = new Concept();
					c.setCode(concepts.get(i).getCode());
					c.setLabel(concepts.get(i).getLabel());
					c.setIdx(j);
					c.setIdx(j);
					j++;
					trimed_concepts.add(c);
					if (c.getCode().equals(parentCode)) {
						break;
					}
				}
				conceptPaths.add(new Path(1,trimed_concepts));
			}
		}
		conceptPaths = removeDuplicatePaths(conceptPaths);
		return conceptPaths;
	}
		
	private Paths removeDuplicatePaths(Paths paths) {
		Paths uniquePaths = new Paths();
		HashSet <String> seenPaths = new HashSet<String>();
		for (Path path: paths.getPaths()) {
			StringBuffer strPath = new StringBuffer();
		    for (Concept concept: path.getConcepts()) {
			    strPath.append(concept.getCode());
				strPath.append("|");
    		}
			String pathString = strPath.toString();
			if (!seenPaths.contains(pathString))  {
				seenPaths.add(pathString);
				uniquePaths.add(path);
			}
		}
		
		return uniquePaths;
	}
}
