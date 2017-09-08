package gov.nih.nci.evs.api.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
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
import gov.nih.nci.evs.api.util.MainTypeHierarchyUtils;
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
	
	private HashMap<String,String> diseaseStageConcepts;
	private HashMap<String,String> diseaseGradeConcepts;
	private HashMap<String,String> diseaseMainConcepts;
	private Paths paths;
	private Long classCount;
	
	public static String DISEASES_AND_DISORDERS_CODE = "C2991";
	public static String NEOPLASM_CODE = "C3262";
	static String CTS_API_Disease_Broad_Category_Terminology_Code = "C138189";
	static String CTS_API_Disease_Main_Type_Terminology_Code = "C138190";
	
	/*
	public static final String[] CTRP_MAIN_CANCER_TYPES = new String[] {
			"C4715", "C4536", "C35850", "C54293", "C9315", "C8990", "C9272", "C9466", "C3871", "C9465",
			"C9105", "C4855", "C4815", "C8054", "C4035", "C3879", "C4906", "C7569", "C3513", "C4911",
			"C3850", "C7927", "C3099", "C27814", "C4436", "C36077", "C35417", "C7109", "C3844", "C3908",
			"C7724", "C9330", "C2955", "C4910", "C9382", "C9291", "C4878", "C2926", "C4917", "C4872",
			"C4908", "C3867", "C7558", "C9039", "C3917", "C4866", "C9061", "C4863", "C40022", "C7352",
			"C9325", "C9385", "C6142", "C8993", "C4912", "C9106", "C4914", "C9312", "C9145", "C2946",
			"C9306", "C3158", "C3359", "C3194", "C3088", "C9087", "C3230", "C3059", "C3224", "C3510",
			"C8711", "C4817", "C3270", "C3790", "C9344", "C2947", "C3716", "C5669", "C3728", "C3267",
			"C6906", "C3403", "C3752", "C3753", "C2996", "C9309", "C3011", "C4290", "C9063", "C3422",
			"C2948", "C4699", "C3161", "C3172", "C3171", "C3174", "C3178", "C7539", "C3167", "C3163",
			"C9290", "C3247", "C4665", "C9349", "C3242", "C3208", "C9357", "C38661", "C3211", "C3773",
			"C7541", "C3411", "C3234", "C61574", "C2991", "C3262", "C2916", "C9118", "C3268", "C3264",
			"C3708", "C27134", "C3809", "C3058", "C3017","C9384", "C4767",  "C4016", "C7073", "C7515",
			 "C4896","C54705"};
	*/
	private List <String> CTRP_MAIN_CANCER_TYPES = new ArrayList<String>();
	
	private HierarchyUtils hierarchy = null;
	private MainTypeHierarchyUtils mainTypeHierarchyUtils = null;

	@PostConstruct
	public void postInit() throws IOException{
		restUtils = new RESTUtils(stardogProperties.getQueryUrl(), stardogProperties.getUsername(),
				stardogProperties.getPassword(),stardogProperties.getReadTimeout(),stardogProperties.getConnectTimeout());
		
		populateCache();
		
		
	}
	
	@Scheduled(cron = "${nci.evs.stardog.populateCacheCron}")
	public void callCronJob() throws IOException{
		LocalDateTime currentTime = LocalDateTime.now();
		log.info("callCronJob at " + currentTime);
		Long classCountValueNow = getGetClassCounts();
		
		log.info("class count Value now at " + classCountValueNow);
		log.info("previous class count value at " + classCount);
		log.info("ForcePopulateCache " + stardogProperties.getForcePopulateCache());
		if ((classCountValueNow.longValue() != classCount.longValue()) || stardogProperties.getForcePopulateCache().equalsIgnoreCase("Y")){
			log.info("****repopulating cache***");
			populateCache();
			log.info("****repopulating cache done***");
		}
	}
	
	
	public void populateCache() throws IOException{
		LocalDateTime currentTime = LocalDateTime.now();
        log.info("populating the cache at " + currentTime);
        classCount = getGetClassCounts();
		List <String> parentchild = getHierarchy();
		hierarchy = new HierarchyUtils(parentchild);
		//hierarchy.testLoading();
		PathFinder pathFinder = new PathFinder(hierarchy);
		paths = pathFinder.findPaths();
		
		System.out.println("MainTypes");
		HashSet <String> mainTypeSet = new HashSet <String>();
		List <Concept> mainTypes = getConceptInSubset(CTS_API_Disease_Main_Type_Terminology_Code);
		System.out.println("Count: " + mainTypes.size());
		for (Concept concept: mainTypes) {
			CTRP_MAIN_CANCER_TYPES.add(concept.getCode());
		    mainTypeSet.add(concept.getCode());
		}

		System.out.println("categories");
		ArrayList <String> categoryList = new ArrayList <String>();
		List <Concept> categories = getConceptInSubset(CTS_API_Disease_Broad_Category_Terminology_Code);
		System.out.println("Count: " + categories.size());
		for (Concept concept: categories) {
			CTRP_MAIN_CANCER_TYPES.add(concept.getCode());
			categoryList.add(concept.getCode());
			mainTypeSet.add(concept.getCode());
		}

		diseaseStageConcepts = getDiseaseIsStageSourceCodes();
		diseaseGradeConcepts = getDiseaseIsGradeSourceCodes();
		diseaseMainConcepts = getMainConcepts();

		/*
		HashSet <String> mainTypeSet = new HashSet <String>();
		for (int i = 0; i < CTRP_MAIN_CANCER_TYPES.length; i++) {
		    mainTypeSet.add(CTRP_MAIN_CANCER_TYPES[i]);
		}

		ArrayList <String> categoryList = new ArrayList <String>();
		categoryList.add("C2991");
		categoryList.add("C3262");
		categoryList.add("C2916");
		*/

		mainTypeHierarchyUtils = new MainTypeHierarchyUtils(parentchild,mainTypeSet,categoryList,
				diseaseStageConcepts,diseaseGradeConcepts);
		
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
	
	
	public Long getGetClassCounts() throws JsonMappingException,JsonParseException ,IOException{
		
		String queryPrefix = queryBuilderService.contructPrefix();
		String namedGraph = getNamedGraph();
		String query = queryBuilderService.constructGetClassCountsQuery(namedGraph);
		String res = restUtils.runSPARQL(queryPrefix + query);
		

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		String count = "0";

		Sparql sparqlResult = mapper.readValue(res, Sparql.class);
		Bindings[] bindings = sparqlResult.getResults().getBindings();
		if (bindings.length == 1) {
			count =  bindings[0].getCount().getValue();
		}
		
		
		return Long.parseLong(count);
		
	}

	public List<EvsProperty> getEvsProperties(String conceptCode) throws JsonMappingException,JsonParseException ,IOException {

		String queryPrefix = queryBuilderService.contructPrefix();
		String namedGraph = getNamedGraph();
		String query = queryBuilderService.constructPropertyQuery(conceptCode,namedGraph);
		String res = restUtils.runSPARQL(queryPrefix + query);

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		ArrayList<EvsProperty> evsProperties = new ArrayList<EvsProperty>();
	
		/*
		 * Because the original SPARQL query that filtered out the Annotations
		 * was too slow, we will be filtering them out in the post processing.
		 */
		Sparql sparqlResult = mapper.readValue(res, Sparql.class);
		Bindings[] bindings = sparqlResult.getResults().getBindings();
		for (Bindings b : bindings) {
			if (!b.getPropertyCode().getValue().startsWith("A")) {
				EvsProperty evsProperty = new EvsProperty();
				evsProperty.setCode(b.getPropertyCode().getValue());
				evsProperty.setLabel(b.getPropertyLabel().getValue());
				evsProperty.setValue(b.getPropertyValue().getValue());
				evsProperties.add(evsProperty);
			}
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
		EvsConcept evsConcept = new EvsConcept();
		return getConcept(evsConcept,conceptCode);
	}
	
	public EvsConcept getConcept(EvsConcept evsConcept,String conceptCode) throws IOException{
		
		evsConcept.setLabel(getEvsConceptLabel(conceptCode));		
		List <EvsProperty> properties = getEvsProperties(conceptCode);
		evsConcept.setCode(EVSUtils.getConceptCode(properties));
		evsConcept.setPreferredName(EVSUtils.getPreferredName(properties));
		evsConcept.setDisplayName(EVSUtils.getDisplayName(properties));
		evsConcept.setNeoplasticStatus(EVSUtils.getNeoplasticStatus(properties));
		evsConcept.setSemanticTypes(EVSUtils.getSemanticType(properties));
		evsConcept.setConceptStatus(EVSUtils.getConceptStatus(properties));

		List <EvsAxiom> axioms = getEvsAxioms(conceptCode);
		evsConcept.setDefinitions(EVSUtils.getFullDefinitions(axioms));

		List <EvsSubconcept> subconcepts = getEvsSubconcepts(conceptCode);
		List <EvsSuperconcept> superconcepts = getEvsSuperconcepts(conceptCode);
		evsConcept.setSubconcepts(subconcepts);
		evsConcept.setSuperconcepts(superconcepts);
		evsConcept.setSynonyms(EVSUtils.getFullSynonym(axioms));
		evsConcept.setAdditionalProperties(EVSUtils.getAdditionalProperties(properties));
		
		/*
		if (diseaseStageConcepts.containsKey(conceptCode)){		
			evsConcept.setIsDiseaseStage(true);
		} else{
			evsConcept.setIsDiseaseStage(false);
		}
		*/
		if (mainTypeHierarchyUtils.isDiseaseStage(conceptCode)){		
			evsConcept.setIsDiseaseStage(true);
		} else{
			evsConcept.setIsDiseaseStage(false);
		}
		
		/*
		if (diseaseGradeConcepts.containsKey(conceptCode)){		
			evsConcept.setIsDiseaseGrade(true);
		} else{
			evsConcept.setIsDiseaseGrade(false);
		}
		*/
		
		if (mainTypeHierarchyUtils.isDiseaseGrade(conceptCode)){		
			evsConcept.setIsDiseaseGrade(true);
		} else{
			evsConcept.setIsDiseaseGrade(false);
		}
		
		/*
		if (diseaseMainConcepts.containsKey(conceptCode)){		
			evsConcept.setIsMainType(true);
		} else{
			evsConcept.setIsMainType(false);
		}
		*/
		if (mainTypeHierarchyUtils.isMainType(conceptCode)){		
			evsConcept.setIsMainType(true);
		} else{
			evsConcept.setIsMainType(false);
		}
		
		if (mainTypeHierarchyUtils.isSubtype(conceptCode)) {
			evsConcept.setIsSubtype(true);
		} else {
			evsConcept.setIsSubtype(false);
		}		
		
		if (mainTypeHierarchyUtils.isDisease(conceptCode)) {
			evsConcept.setIsDisease(true);
		} else {
			evsConcept.setIsDisease(false);
		}		
		
		
		List <Paths> paths = mainTypeHierarchyUtils.getMainMenuAncestors(conceptCode);
		if (paths != null) {
			paths = removeDuplicatePathsList(paths);
			evsConcept.setMainMenuAncestors(paths);
		} else {
			evsConcept.setMainMenuAncestors(null);
		}
		
		return evsConcept;
		
	}
	
	/*
	 * This "isSubtype is temporary, will use new one developed by
	 * Kim in next release
	 */
	public boolean isSubtype(String code) {
		if (diseaseStageConcepts.containsKey(code)) {
			String label = hierarchy.getLabel(code).toLowerCase();
			if (label.indexOf("stage") == -1) {
				return true;
			} else {
				return false;
			}
		}
		
		if (diseaseGradeConcepts.containsKey(code)) {
			return false;
		}

		if (diseaseMainConcepts.containsKey(code)) {
			if (code.compareTo(DISEASES_AND_DISORDERS_CODE) != 0) {
				return true;
			} else {
				return false;
			}
		}

		return true;
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
		}
		
		return diseaseGradeConcepts;
	}

	public List <Concept> getConceptInSubset(String code) throws JsonMappingException,JsonParseException, IOException {
		log.info("***** In getConceptInSubset******");
		String queryPrefix = queryBuilderService.contructPrefix();
		String namedGraph = getNamedGraph();
		String query = queryBuilderService.constructConceptInSubsetQuery(code,namedGraph);
		String res = restUtils.runSPARQL(queryPrefix + query);
		

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		List <Concept> concepts = new ArrayList<Concept>();
		
		Sparql sparqlResult = mapper.readValue(res, Sparql.class);
		Bindings[] bindings = sparqlResult.getResults().getBindings();
		for (Bindings b : bindings) {
			Concept concept = new Concept();
			concept.setLabel(b.getConceptLabel().getValue());
			concept.setCode(b.getConceptCode().getValue());
			concepts.add(concept);
		}
		
		return concepts;
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

	private List <Paths> removeDuplicatePathsList(List <Paths> paths) {
        List <Paths> uniquePaths = new ArrayList <Paths>();
		for (Paths newPaths: paths) {
  			Paths newUniquePaths = new Paths();
  			HashSet <String> seenPaths = new HashSet<String>();
  			for (Path path: newPaths.getPaths()) {
				StringBuffer strPath = new StringBuffer();
				for (Concept concept: path.getConcepts()) {
					strPath.append(concept.getCode());
					strPath.append("|");
				}
				String pathString = strPath.toString();
				if (!seenPaths.contains(pathString))  {
					seenPaths.add(pathString);
					newUniquePaths.add(path);
				}
  			}
  			uniquePaths.add(newUniquePaths);
		}
		
		return uniquePaths;
	}
}
