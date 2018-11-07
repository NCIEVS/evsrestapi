package gov.nih.nci.evs.api.model.evs;

public interface EvsSynonym {
	public String getCode();
	public void setCode(String code);
	public String getLabel();
	public void setLabel(String label);
	public String getTermName();
	public void setTermName(String termName);
	public String getTermGroup();
	public void setTermGroup(String termGroup);
	public String getTermSource();
	public void setTermSource(String termSource);
	public String getSourceCode();
	public void setSourceCode(String sourceCode);
	public String getSubsourceName();
	public void setSubsourceName(String subsourceName);
}
