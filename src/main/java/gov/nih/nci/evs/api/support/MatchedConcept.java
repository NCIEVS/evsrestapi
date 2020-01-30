package gov.nih.nci.evs.api.support;




public class MatchedConcept
{

// Variable declaration
	private String label;
	private String code;
	private String conceptStatus;
	private String preferredName;
	private String propertyName;
	private String propertyValue;
	private double score;

// Default constructor
	public MatchedConcept() {
	}

// Constructor
	public MatchedConcept(
		final String label,
		final String code,
		final String conceptStatus,
		final String preferredName,
		final String propertyName,
		final String propertyValue,
		final int score) {

		this.label = label;
		this.code = code;
		this.propertyName = propertyName;
		this.propertyValue = propertyValue;
		this.score = score;
	}

	public MatchedConcept(
		final String label,
		final String code,
		final String conceptStatus,
		final String preferredName,
		final String propertyName,
		final String propertyValue
		) {

		this.label = label;
		this.code = code;
		this.propertyName = propertyName;
		this.propertyValue = propertyValue;
		this.score = 0;
	}

// Set methods
	public void setLabel(final String label) {
		this.label = label;
	}

	public void setCode(final String code) {
		this.code = code;
	}

	public void setPropertyName(final String propertyName) {
		this.propertyName = propertyName;
	}

	public void setPropertyValue(final String propertyValue) {
		this.propertyValue = propertyValue;
	}

	public void setScore(final double score) {
		this.score = score;
	}


// Get methods
	public String getLabel() {
		return this.label;
	}

	public String getCode() {
		return this.code;
	}

	public String getConceptStatus() {
		return conceptStatus;
	}

	public void setConceptStatus(final String conceptStatus) {
		this.conceptStatus = conceptStatus;
	}

	public String getPreferredName() {
		return preferredName;
	}

	public void setPreferredName(final String preferredName) {
		this.preferredName = preferredName;
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
