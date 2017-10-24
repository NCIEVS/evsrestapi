package gov.nih.nci.evs.api.support;




public class MatchedConcept
{

// Variable declaration
	private String label;
	private String code;
	private String propertyName;
	private String propertyValue;
	private double score;

// Default constructor
	public MatchedConcept() {
	}

// Constructor
	public MatchedConcept(
		String label,
		String code,
		String propertyName,
		String propertyValue,
		int score) {

		this.label = label;
		this.code = code;
		this.propertyName = propertyName;
		this.propertyValue = propertyValue;
		this.score = score;
	}

	public MatchedConcept(
		String label,
		String code,
		String propertyName,
		String propertyValue
		) {

		this.label = label;
		this.code = code;
		this.propertyName = propertyName;
		this.propertyValue = propertyValue;
		this.score = 0;
	}

// Set methods
	public void setLabel(String label) {
		this.label = label;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public void setPropertyValue(String propertyValue) {
		this.propertyValue = propertyValue;
	}

	public void setScore(double score) {
		this.score = score;
	}


// Get methods
	public String getLabel() {
		return this.label;
	}

	public String getCode() {
		return this.code;
	}

	public String getPropertyName() {
		return this.propertyName;
	}

	public String getPropertyValue() {
		return this.propertyValue;
	}

	public double getScore() {
		return this.score;
	}

}
