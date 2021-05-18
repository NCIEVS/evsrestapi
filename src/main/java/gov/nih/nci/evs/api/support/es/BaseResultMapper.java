package gov.nih.nci.evs.api.support.es;

import java.util.Map;

import gov.nih.nci.evs.api.model.IncludeParam;

/**
 * The base elasticsearch result mapper class with common helper methods
 * 
 * @author Arun
 *
 */
public abstract class BaseResultMapper {
  void applyIncludeParam(final Map<String, Object> sourceMap, IncludeParam ip) {
    if (ip == null) return;
    
    if (!ip.isSynonyms()) {
      sourceMap.put("synonyms", null);
    }
    if (!ip.isDefinitions()) {
      sourceMap.put("definitions", null);
    }
    if (!ip.isProperties()) {
      sourceMap.put("properties", null);
    }
    if (!ip.isChildren()) {
      sourceMap.put("children", null);
    }
    if (!ip.isDescendants()) {
      sourceMap.put("descendants", null);
    }
    if (!ip.isParents()) {
      sourceMap.put("parents", null);
    }
    if (!ip.isAssociations()) {
      sourceMap.put("associations", null);
    }
    if (!ip.isInverseAssociations()) {
      sourceMap.put("inverseAssociations", null);
    }
    if (!ip.isRoles()) {
      sourceMap.put("roles", null);
    }
    if (!ip.isInverseRoles()) {
      sourceMap.put("inverseRoles", null);
    }
    if (!ip.isDisjointWith()) {
      sourceMap.put("disjointWith", null);
    }
    if (!ip.isMaps()) {
      sourceMap.put("maps", null);
    }
    if (!ip.isPaths()) {
      sourceMap.put("paths", null);
    }
    if (!ip.isExtensions()) {
      sourceMap.put("extensions", null);
    }
  }
}
