package gov.nih.nci.evs.api.properties;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticQueryProperties {
	
	/** The logger. */
    private static final Logger log = LoggerFactory.getLogger(ElasticQueryProperties.class);

    //source fields
    private String shortsourcefields;
    private String defaultsourcefields;
    private String associationsourcefields;
    private String definitionsourcefields;
    
    //exact and starts with fields
    private String exactstartswithfields;
    private String exactstartswithsynonymfields;
    private String exactstartswithdefinitionfields;
    private String exactstartswithassociationfields;
    
    //contains fields
    private String containsfields;
    private String containssynonymfields;
    private String containsdefinitionfields;
    private String containsassociationfields;
    
    //andor fields
    private String andorfields;
    private String andorsynonymfields;
    private String andordefinitionfields;
    private String andorassociationfields;
    
    //highlight fields for exact
    private String highlightexact;
    private String highlightsynonymexact;
    private String highlightdefinitionexact;
    private String highlightassociationexact;
    
    //highlight fields for contains
    private String highlightcontains;
    private String highlightsynonymcontains;
    private String highlightdefinitioncontains;
    private String highlightassociationcontains;
    
    //highlight fields for and or
    private String highlightandor;
    private String highlightsynonymandor;
    private String highlightdefinitionandor;
    private String highlightassociationandor;
    
    //main query and main nested query
    private String mainQuery;
    private String mainNestedQuery;
    private String mainMultipleNestedQuery;
    private String mainQueryWithoutHighlights;
    private String mainNestedQueryWithoutHighlights;
    private String mainMultipleNestedQueryWithoutHighlights;
   
    
    //highlight tags	
    private String highlightTags;
      
    //Property fields    
    private HashMap<String, String> propertyToQuery;
    private HashMap<String, String> propertyToQueryExact;
    private HashMap<String, String> propertyToQueryContains;
    
    
   
    
    public String getExactstartswithassociationfields() {
		return exactstartswithassociationfields;
	}

	public void setExactstartswithassociationfields(String exactstartswithassociationfields) {
		this.exactstartswithassociationfields = exactstartswithassociationfields;
	}

	public String getContainsassociationfields() {
		return containsassociationfields;
	}

	public void setContainsassociationfields(String containsassociationfields) {
		this.containsassociationfields = containsassociationfields;
	}

	public String getAndorassociationfields() {
		return andorassociationfields;
	}

	public void setAndorassociationfields(String andorassociationfields) {
		this.andorassociationfields = andorassociationfields;
	}

	public String getHighlightassociationexact() {
		return highlightassociationexact;
	}

	public void setHighlightassociationexact(String highlightassociationexact) {
		this.highlightassociationexact = highlightassociationexact;
	}

	public String getHighlightassociationcontains() {
		return highlightassociationcontains;
	}

	public void setHighlightassociationcontains(String highlightassociationcontains) {
		this.highlightassociationcontains = highlightassociationcontains;
	}

	public String getHighlightassociationandor() {
		return highlightassociationandor;
	}

	public void setHighlightassociationandor(String highlightassociationandor) {
		this.highlightassociationandor = highlightassociationandor;
	}

	
	public String getHighlightsynonymexact() {
		return highlightsynonymexact;
	}

	public void setHighlightsynonymexact(String highlightsynonymexact) {
		this.highlightsynonymexact = highlightsynonymexact;
	}

	public String getHighlightsynonymcontains() {
		return highlightsynonymcontains;
	}

	public void setHighlightsynonymcontains(String highlightsynonymcontains) {
		this.highlightsynonymcontains = highlightsynonymcontains;
	}

	public String getHighlightsynonymandor() {
		return highlightsynonymandor;
	}

	public void setHighlightsynonymandor(String highlightsynonymandor) {
		this.highlightsynonymandor = highlightsynonymandor;
	}

	public String getExactstartswithsynonymfields() {
		return exactstartswithsynonymfields;
	}

	public void setExactstartswithsynonymfields(String exactstartswithsynonymfields) {
		this.exactstartswithsynonymfields = exactstartswithsynonymfields;
	}

	public String getContainssynonymfields() {
		return containssynonymfields;
	}

	public void setContainssynonymfields(String containssynonymfields) {
		this.containssynonymfields = containssynonymfields;
	}

	public String getAndorsynonymfields() {
		return andorsynonymfields;
	}

	public void setAndorsynonymfields(String andorsynonymfields) {
		this.andorsynonymfields = andorsynonymfields;
	}

	public String getHighlightTags() {
		return highlightTags;
	}

	public void setHighlightTags(String highlightTags) {
		this.highlightTags = highlightTags;
	}

	public String getHighlightexact() {
		return highlightexact;
	}

	public void setHighlightexact(String highlightexact) {
		this.highlightexact = highlightexact;
	}

	public String getHighlightcontains() {
		return highlightcontains;
	}

	public void setHighlightcontains(String highlightcontains) {
		this.highlightcontains = highlightcontains;
	}

	public String getHighlightandor() {
		return highlightandor;
	}

	public void setHighlightandor(String highlightandor) {
		this.highlightandor = highlightandor;
	}

	

	public String getMainQuery() {
		return mainQuery;
	}

	public void setMainQuery(String mainQuery) {
		this.mainQuery = mainQuery;
	}

	public String getExactstartswithfields() {
		return exactstartswithfields;
	}

	public void setExactstartswithfields(String exactstartswithfields) {
		this.exactstartswithfields = exactstartswithfields;
	}

	public String getContainsfields() {
		return containsfields;
	}

	public void setContainsfields(String containsfields) {
		this.containsfields = containsfields;
	}

	public String getAndorfields() {
		return andorfields;
	}

	public void setAndorfields(String andorfields) {
		this.andorfields = andorfields;
	}

	public String getShortsourcefields() {
		return shortsourcefields;
	}

	public void setShortsourcefields(String shortsourcefields) {
		this.shortsourcefields = shortsourcefields;
	}

	public String getDefaultsourcefields() {
		return defaultsourcefields;
	}

	public void setDefaultsourcefields(String defaultsourcefields) {
		this.defaultsourcefields = defaultsourcefields;
	}

	public String getMainQueryWithoutHighlights() {
		return mainQueryWithoutHighlights;
	}

	public void setMainQueryWithoutHighlights(String mainQueryWithoutHighlights) {
		this.mainQueryWithoutHighlights = mainQueryWithoutHighlights;
	}

	

	public String getExactstartswithdefinitionfields() {
		return exactstartswithdefinitionfields;
	}

	public void setExactstartswithdefinitionfields(String exactstartswithdefinitionfields) {
		this.exactstartswithdefinitionfields = exactstartswithdefinitionfields;
	}

	public String getContainsdefinitionfields() {
		return containsdefinitionfields;
	}

	public void setContainsdefinitionfields(String containsdefinitionfields) {
		this.containsdefinitionfields = containsdefinitionfields;
	}

	public String getAndordefinitionfields() {
		return andordefinitionfields;
	}

	public void setAndordefinitionfields(String andordefinitionfields) {
		this.andordefinitionfields = andordefinitionfields;
	}

	public String getHighlightdefinitionexact() {
		return highlightdefinitionexact;
	}

	public void setHighlightdefinitionexact(String highlightdefinitionexact) {
		this.highlightdefinitionexact = highlightdefinitionexact;
	}

	public String getHighlightdefinitioncontains() {
		return highlightdefinitioncontains;
	}

	public void setHighlightdefinitioncontains(String highlightdefinitioncontains) {
		this.highlightdefinitioncontains = highlightdefinitioncontains;
	}

	public String getHighlightdefinitionandor() {
		return highlightdefinitionandor;
	}

	public void setHighlightdefinitionandor(String highlightdefinitionandor) {
		this.highlightdefinitionandor = highlightdefinitionandor;
	}

	public String getMainNestedQuery() {
		return mainNestedQuery;
	}

	public void setMainNestedQuery(String mainNestedQuery) {
		this.mainNestedQuery = mainNestedQuery;
	}

	public String getMainNestedQueryWithoutHighlights() {
		return mainNestedQueryWithoutHighlights;
	}

	public void setMainNestedQueryWithoutHighlights(String mainNestedQueryWithoutHighlights) {
		this.mainNestedQueryWithoutHighlights = mainNestedQueryWithoutHighlights;
	}

	public String getAssociationsourcefields() {
		return associationsourcefields;
	}

	public void setAssociationsourcefields(String associationsourcefields) {
		this.associationsourcefields = associationsourcefields;
	}

	public String getDefinitionsourcefields() {
		return definitionsourcefields;
	}

	public void setDefinitionsourcefields(String definitionsourcefields) {
		this.definitionsourcefields = definitionsourcefields;
	}

	

	public HashMap<String, String> getPropertyToQuery() {
		return propertyToQuery;
	}

	public void setPropertyToQuery(HashMap<String, String> propertyToQuery) {
		this.propertyToQuery = propertyToQuery;
	}

	public HashMap<String, String> getPropertyToQueryExact() {
		return propertyToQueryExact;
	}

	public void setPropertyToQueryExact(HashMap<String, String> propertyToQueryExact) {
		this.propertyToQueryExact = propertyToQueryExact;
	}

	public HashMap<String, String> getPropertyToQueryContains() {
		return propertyToQueryContains;
	}

	public void setPropertyToQueryContains(HashMap<String, String> propertyToQueryContains) {
		this.propertyToQueryContains = propertyToQueryContains;
	}

	public String getMainMultipleNestedQuery() {
		return mainMultipleNestedQuery;
	}

	public void setMainMultipleNestedQuery(String mainMultipleNestedQuery) {
		this.mainMultipleNestedQuery = mainMultipleNestedQuery;
	}

	public String getMainMultipleNestedQueryWithoutHighlights() {
		return mainMultipleNestedQueryWithoutHighlights;
	}

	public void setMainMultipleNestedQueryWithoutHighlights(String mainMultipleNestedQueryWithoutHighlights) {
		this.mainMultipleNestedQueryWithoutHighlights = mainMultipleNestedQueryWithoutHighlights;
	}

	
	

	

}
