package gov.nih.nci.evs.api.properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticQueryProperties {
	
	/** The logger. */
    private static final Logger log = LoggerFactory.getLogger(ElasticQueryProperties.class);

    private String exactstartswithfields;
    private String containsfields;
    private String andorfields;
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
    private String highlightexact;
    private String highlightcontains;
    private String highlightandor;
    private String mainQuery;
    private String mainQueryWithoutHighlights;
    private String shortsourcefields;
    private String defaultsourcefields;
    private String highlightTags;
    private String exactstartswithsynonymfields;
    private String containssynonymfields;
    private String andorsynonymfields;
    private String highlightsynonymexact;
    private String highlightsynonymcontains;
    private String highlightsynonymandor;
    private String  mainSynonymQuery;
    private String mainSynonymQueryWithoutHighlights;
    
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

	public String getMainSynonymQuery() {
		return mainSynonymQuery;
	}

	public void setMainSynonymQuery(String mainSynonymQuery) {
		this.mainSynonymQuery = mainSynonymQuery;
	}

	public String getMainSynonymQueryWithoutHighlights() {
		return mainSynonymQueryWithoutHighlights;
	}

	public void setMainSynonymQueryWithoutHighlights(String mainSynonymQueryWithoutHighlights) {
		this.mainSynonymQueryWithoutHighlights = mainSynonymQueryWithoutHighlights;
	}

}
