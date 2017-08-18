package gov.nih.nci.evs.api.service;

public interface QueryBuilderService {
	
	public String contructPrefix();
	
	
	public String constructPropertyQuery(String conceptCode,String namedGraph);
	
	public String constructAxiomQuery(String conceptCode, String namedGraph);
	
	public String constructSubconceptQuery(String conceptCode, String namedGraph);

	public String constructSuperconceptQuery(String conceptCode, String namedGraph);
	
	public String constructConceptLabelQuery(String conceptCode, String namedGraph);
	
	public String constructGetClassCountsQuery(String namedGraph);

	public String constructAssociationsQuery(String conceptCode, String namedGraph);

	public String constructInverseAssociationsQuery(String conceptCode, String namedGraph);

	public String constructRolesQuery(String conceptCode, String namedGraph);

	public String constructInverseRolesQuery(String conceptCode, String namedGraph);

	public String constructDiseaseIsStageSourceCodesQuery(String namedGraph);
	
	public String constructDiseaseIsGradeSourceCodesQuery(String namedGraph);

	public String constructHierarchyQuery(String namedGraph);
}
