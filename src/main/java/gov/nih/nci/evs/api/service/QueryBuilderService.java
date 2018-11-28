package gov.nih.nci.evs.api.service;

public interface QueryBuilderService {
	public String constructAllPropertyQuery(String conceptCode,String namedGraph);
	public String constructAllGraphNamesQuery();
    public String constructAxiomQuery(String conceptCode, String namedGraph);
	public String constructClassCountsQuery(String namedGraph);
	public String constructConceptLabelQuery(String conceptCode, String namedGraph);
	public String contructPrefix();
	public String constructPropertyQuery(String conceptCode,String namedGraph);
	public String constructPropertyNoRestrictionsQuery(String conceptCode,String namedGraph);
	public String constructSubconceptQuery(String conceptCode, String namedGraph);
	public String constructSuperconceptQuery(String conceptCode, String namedGraph);
	public String constructAssociationsQuery(String conceptCode, String namedGraph);
	public String constructInverseAssociationsQuery(String conceptCode, String namedGraph);
	public String constructRolesQuery(String conceptCode, String namedGraph);
	public String constructInverseRolesQuery(String conceptCode, String namedGraph);
	public String constructDisjointWithQuery(String conceptCode, String namedGraph);
	public String constructHierarchyQuery(String namedGraph);
	public String constructAllPropertiesQuery(String namedGraph);
	public String constructAllAssociationsQuery(String namedGraph);
	public String constructAllRolesQuery(String namedGraph);
	public String constructVersionInfoQuery(String namedGraph);
}
