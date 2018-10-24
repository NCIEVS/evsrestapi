package gov.nih.nci.evs.api.model.evs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "definition", "defSource", "attr"})
public class EvsDefinitionByCode {
	
	@JsonProperty("definition")
	private String definition;
	
	@JsonProperty("P381")
	private String attr;
	
	@JsonProperty("P378")
	private String defSource;
	
	
	public String getDefinition() {
		return definition;
	}
	public void setDefinition(String definition) {
		this.definition = definition;
	}
	public String getAttr() {
		return attr;
	}
	public void setAttr(String attr) {
		this.attr = attr;
	}
	public String getDefSource() {
		return defSource;
	}
	public void setDefSource(String defSource) {
		this.defSource = defSource;
	}
}
