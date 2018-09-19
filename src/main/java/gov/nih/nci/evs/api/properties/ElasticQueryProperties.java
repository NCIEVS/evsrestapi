package gov.nih.nci.evs.api.properties;

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
    
    //Property fields
    private String P108Default;
    private String P108Exact;
    private String P108Contains;
    
    private String P107Default; 
    private String P107Exact;
    private String P107Contains;
    
    private String P90Default;
    private String P90Exact;
    private String P90Contains;
    
    private String NHC0Default;
    private String NHC0Exact;
    private String NHC0Contains;
    
    private String P97Default;
    private String P97Exact;
    private String P97Contains;
    
    private String statusDefault;
    
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
    private String mainQueryWithoutHighlights;
    private String mainNestedQueryWithoutHighlights;
   
    
    //highlight tags	
    private String highlightTags;
      
    
    
    
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

	public String getP108Default() {
		return P108Default;
	}

	public void setP108Default(String p108Default) {
		P108Default = p108Default;
	}

	public String getP108Exact() {
		return P108Exact;
	}

	public void setP108Exact(String p108Exact) {
		P108Exact = p108Exact;
	}

	public String getP108Contains() {
		return P108Contains;
	}

	public void setP108Contains(String p108Contains) {
		P108Contains = p108Contains;
	}

	public String getP107Default() {
		return P107Default;
	}

	public void setP107Default(String p107Default) {
		P107Default = p107Default;
	}

	public String getP107Exact() {
		return P107Exact;
	}

	public void setP107Exact(String p107Exact) {
		P107Exact = p107Exact;
	}

	public String getP107Contains() {
		return P107Contains;
	}

	public void setP107Contains(String p107Contains) {
		P107Contains = p107Contains;
	}

	public String getP90Default() {
		return P90Default;
	}

	public void setP90Default(String p90Default) {
		P90Default = p90Default;
	}

	public String getP90Exact() {
		return P90Exact;
	}

	public void setP90Exact(String p90Exact) {
		P90Exact = p90Exact;
	}

	public String getP90Contains() {
		return P90Contains;
	}

	public void setP90Contains(String p90Contains) {
		P90Contains = p90Contains;
	}

	public String getNHC0Default() {
		return NHC0Default;
	}

	public void setNHC0Default(String nHC0Default) {
		NHC0Default = nHC0Default;
	}

	public String getNHC0Exact() {
		return NHC0Exact;
	}

	public void setNHC0Exact(String nHC0Exact) {
		NHC0Exact = nHC0Exact;
	}

	public String getNHC0Contains() {
		return NHC0Contains;
	}

	public void setNHC0Contains(String nHC0Contains) {
		NHC0Contains = nHC0Contains;
	}

	public String getP97Default() {
		return P97Default;
	}

	public void setP97Default(String p97Default) {
		P97Default = p97Default;
	}

	public String getP97Exact() {
		return P97Exact;
	}

	public void setP97Exact(String p97Exact) {
		P97Exact = p97Exact;
	}

	public String getP97Contains() {
		return P97Contains;
	}

	public void setP97Contains(String p97Contains) {
		P97Contains = p97Contains;
	}

	public static Logger getLog() {
		return log;
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

	public String getStatusDefault() {
		return statusDefault;
	}

	public void setStatusDefault(String statusDefault) {
		this.statusDefault = statusDefault;
	}

}
