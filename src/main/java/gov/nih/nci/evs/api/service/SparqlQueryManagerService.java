package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import gov.nih.nci.evs.api.model.evs.EvsConcept;
import gov.nih.nci.evs.api.model.evs.EvsProperty;
import gov.nih.nci.evs.api.model.evs.EvsRelationships;
import gov.nih.nci.evs.api.model.evs.EvsVersionInfo;
import gov.nih.nci.evs.api.model.evs.HierarchyNode;
import gov.nih.nci.evs.api.model.evs.Paths;

public interface SparqlQueryManagerService {
	
	public boolean checkConceptExists(String conceptCode, String dbType) throws JsonMappingException,JsonParseException,IOException;
	public EvsConcept getEvsConceptByLabel(String conceptCode, String dbType) throws JsonMappingException,JsonParseException,IOException;
	public EvsConcept getEvsConceptByCode(String conceptCode, String dbType) throws JsonMappingException,JsonParseException,IOException;
	public EvsConcept getEvsConceptByLabelShort(String conceptCode, String dbType) throws JsonMappingException,JsonParseException,IOException;
	public EvsConcept getEvsConceptByCodeShort(String conceptCode, String dbType) throws JsonMappingException,JsonParseException,IOException;
	
	public EvsRelationships getEvsRelationships(String conceptCode, String dbType, String format) throws JsonMappingException,JsonParseException,IOException;

	public List<EvsConcept> getAllProperties(String dbType, String format) throws JsonParseException, JsonMappingException, IOException;
	public List<EvsProperty> getAllPropertiesList(String dbType, String format) throws JsonParseException, JsonMappingException, IOException;
	public List<String> getAllPropertiesForDocumentation(String dbType) throws JsonParseException, JsonMappingException, IOException;
	public List<EvsConcept> getAllAssociations(String dbType, String format) throws JsonParseException, JsonMappingException, IOException;
	public List<String> getAllAssociationsForDocumentation(String dbType) throws JsonParseException, JsonMappingException, IOException;
	public List<EvsConcept> getAllRoles(String dbType, String format) throws JsonParseException, JsonMappingException, IOException;
	public List<String> getAllRolesForDocumentation(String dbType) throws JsonParseException, JsonMappingException, IOException;
	public EvsConcept getEvsPropertyByLabel(String conceptCode, String dbType) throws JsonMappingException,JsonParseException,IOException;
	public EvsConcept getEvsPropertyByCode(String conceptCode, String dbType) throws JsonMappingException,JsonParseException,IOException;
	

	public List<String> getContributingSourcesForDocumentation() throws JsonMappingException,JsonParseException,IOException;
	public List<String> getConceptStatusForDocumentation() throws JsonMappingException,JsonParseException,IOException;
	
	public List<HierarchyNode> getRootNodes(String dbType);
	public List<HierarchyNode> getChildNodes(String parent, String dbType);
	public List<HierarchyNode> getChildNodes(String parent, int maxLevel, String dbType);
	public List <HierarchyNode> getPathInHierarchy(String code, String dbType);
	
	public Paths getPathToRoot(String conceptCode, String dbType) throws JsonMappingException, JsonParseException, IOException;
	public Paths getPathToParent(String conceptCode, String parentConceptCode, String dbType) throws JsonMappingException, JsonParseException, IOException;
	
	public List<String> getAllGraphNames(String dbType)throws JsonParseException, JsonMappingException, IOException;
	
	public EvsVersionInfo getEvsVersionInfo(String dbType) throws JsonParseException, JsonMappingException, IOException;

	public EvsConcept getEvsConceptByLabelProperties(String conceptCode, String dbType, List <String> properties) throws JsonMappingException,JsonParseException,IOException;
	public EvsConcept getEvsConceptByCodeProperties(String conceptCode, String dbType, List <String> properties) throws JsonMappingException,JsonParseException,IOException;
}
