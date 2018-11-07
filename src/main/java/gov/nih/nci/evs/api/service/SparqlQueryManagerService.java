package gov.nih.nci.evs.api.service;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import gov.nih.nci.evs.api.model.evs.EvsConcept;
import gov.nih.nci.evs.api.model.evs.EvsProperty;
import gov.nih.nci.evs.api.model.evs.HierarchyNode;
import gov.nih.nci.evs.api.model.evs.Paths;

public interface SparqlQueryManagerService {
	
	public boolean checkConceptExists(String conceptCode, String dbType) throws JsonMappingException,JsonParseException,IOException;
	public EvsConcept getEvsConceptByLabel(String conceptCode, String dbType) throws JsonMappingException,JsonParseException,IOException;
	public EvsConcept getEvsConceptByCode(String conceptCode, String dbType) throws JsonMappingException,JsonParseException,IOException;

	public List<EvsConcept> getAllProperties(String dbType, String format) throws JsonParseException, JsonMappingException, IOException;
	public List<EvsConcept> getAllAssociations(String dbType, String format) throws JsonParseException, JsonMappingException, IOException;
	public List<EvsConcept> getAllRoles(String dbType, String format) throws JsonParseException, JsonMappingException, IOException;
	public EvsConcept getEvsPropertyByLabel(String conceptCode, String dbType) throws JsonMappingException,JsonParseException,IOException;
	public EvsConcept getEvsPropertyByCode(String conceptCode, String dbType) throws JsonMappingException,JsonParseException,IOException;

	public List<HierarchyNode> getRootNodes(String dbType);
	public List<HierarchyNode> getChildNodes(String parent, String dbType);
	public List<HierarchyNode> getChildNodes(String parent, int maxLevel, String dbType);
	public List <HierarchyNode> getPathInHierarchy(String code, String dbType);
	
	public Paths getPathToRoot(String conceptCode, String dbType) throws JsonMappingException, JsonParseException, IOException;
	public Paths getPathToParent(String conceptCode, String parentConceptCode, String dbType) throws JsonMappingException, JsonParseException, IOException;
	
	public List<String> getAllGraphNames(String dbType)throws JsonParseException, JsonMappingException, IOException;
}
