package gov.nih.nci.evs.api.model.evs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "description", "defSource", "attr"})
public class EvsDefinitionFull {
	
	@JsonView(ConceptViews.FullClass.class)
	@JsonProperty("description")
	private String description;
	
	@JsonView(ConceptViews.FullClass.class)
	@JsonProperty("attr")
	private String attr;
	
	@JsonView(ConceptViews.FullClass.class)
	@JsonProperty("def-source")
	private String defSource;
	
	
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
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
