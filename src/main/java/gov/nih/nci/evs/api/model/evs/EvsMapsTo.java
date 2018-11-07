package gov.nih.nci.evs.api.model.evs;

public interface EvsMapsTo {
	public String getAnnotatedTarget();
	public void setAnnotatedTarget(String annotatedTarget);
	public String getRelationshipToTarget();
	public void setRelationshipToTarget(String relationshipToTarget);
	public String getTargetTermType();
	public void setTargetTermType(String targetTermType);
	public String getTargetCode();
	public void setTargetCode(String targetCode);
	public String getTargetTerminology();
	public void setTargetTerminology(String targetTerminology);
}
