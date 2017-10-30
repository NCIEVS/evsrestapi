package gov.nih.nci.evs.api.model.sparql;

public class Bindings
{
    private Property property;
    private Property propertyLabel;
    private Property propertyCode;  
    private Property conceptCode;
    private Property conceptLabel;
    private Property propertyValue;
    private Property axiom;
    private Property axiomProperty;
    private Property axiomValue;
    private Property subclass;
    private Property subclassCode;
    private Property subclassLabel;
    private Property superclass;
    private Property superclassCode;
    private Property superclassLabel;
    private Property relationship;
    private Property relatedConceptCode;
    private Property relatedConceptLabel;
    private Property parentCode;
    private Property parentLabel;
    private Property childCode;
    private Property childLabel;
    private Property count;
    private Property score;
    private Property preferredName;
    private Property conceptStatus;

    public Property getPropertyValue ()
    {
        return propertyValue;
    }

    public void setPropertyValue (Property propertyValue)
    {
        this.propertyValue = propertyValue;
    }

    public Property getProperty ()
    {
        return property;
    }

    public void setProperty (Property property)
    {
        this.property = property;
    }

    public Property getConceptCode()
    {
        return conceptCode;
    }

    public void setConceptCode (Property conceptCode)
    {
        this.conceptCode = conceptCode;
    }


    public Property getPropertyLabel ()
    {
        return propertyLabel;
    }

    public void setPropertyLabel (Property propertyLabel)
    {
        this.propertyLabel = propertyLabel;
    }

    public Property getPropertyCode ()
    {
        return propertyCode;
    }

    public void setPropertyCode (Property propertyCode)
    {
        this.propertyCode = propertyCode;
    }

    public Property getAxiom() {
		return axiom;
	}

	public void setAxiom(Property axiom) {
		this.axiom = axiom;
	}

	public Property getAxiomProperty() {
		return axiomProperty;
	}

	public void setAxiomProperty(Property axiomProperty) {
		this.axiomProperty = axiomProperty;
	}

	public Property getAxiomValue() {
		return axiomValue;
	}

	public void setAxiomValue(Property axiomValue) {
		this.axiomValue = axiomValue;
	}


	public Property getSubclass() {
		return subclass;
	}

	public void setSubclass(Property subclass) {
		this.subclass = subclass;
	}

	public Property getSubclassCode() {
		return subclassCode;
	}

	public void setSubclassCode(Property subclassCode) {
		this.subclassCode = subclassCode;
	}

	public Property getSubclassLabel() {
		return subclassLabel;
	}

	public void setSubclassLabel(Property subclassLabel) {
		this.subclassLabel = subclassLabel;
	}
	
	public Property getSuperclass() {
		return superclass;
	}

	public void setSuperclass(Property superclass) {
		this.superclass = superclass;
	}

	
	public Property getSuperclassCode() {
		return superclassCode;
	}

	public void setSuperclassCode(Property superclassCode) {
		this.superclassCode = superclassCode;
	}

	public Property getSuperclassLabel() {
		return superclassLabel;
	}

	public void setSuperclassLabel(Property superclassLabel) {
		this.superclassLabel = superclassLabel;
	}

	@Override
    public String toString()
    {
        return "ClassPojo [propertyValue = "+propertyValue+", property = "+property+", propertyLabel = "+propertyLabel+", propertyCode = "+propertyCode+"]";
    }

	public Property getConceptLabel() {
		return conceptLabel;
	}

	public void setConceptLabel(Property conceptLabel) {
		this.conceptLabel = conceptLabel;
	}

	public Property getRelationship() {
		return relationship;
	}

	public void setRelationship(Property relationship) {
		this.relationship = relationship;
	}

	public Property getRelatedConceptCode() {
		return relatedConceptCode;
	}

	public void setRelatedConceptCode(Property relatedConceptCode) {
		this.relatedConceptCode = relatedConceptCode;
	}

	public Property getRelatedConceptLabel() {
		return relatedConceptLabel;
	}

	public void setRelatedConceptLabel(Property relatedConceptLabel) {
		this.relatedConceptLabel = relatedConceptLabel;
	}

	public Property getParentCode() {
		return parentCode;
	}

	public void setParentCode(Property parentCode) {
		this.parentCode = parentCode;
	}

	public Property getParentLabel() {
		return parentLabel;
	}

	public void setParentLabel(Property parentLabel) {
		this.parentLabel = parentLabel;
	}

	public Property getChildCode() {
		return childCode;
	}

	public void setChildCode(Property childCode) {
		this.childCode = childCode;
	}

	public Property getChildLabel() {
		return childLabel;
	}

	public void setChildLabel(Property childLabel) {
		this.childLabel = childLabel;
	}

	public Property getCount() {
		return count;
	}

	public void setCount(Property count) {
		this.count = count;
	}

	public Property getScore() {
		return score;
	}

	public void setScore(Property score) {
		this.score = score;
	}


	public Property getPreferredName() {
		return preferredName;
	}

	public void setPreferredName(Property preferredName) {
		this.preferredName = preferredName;
	}

	public Property getConceptStatus() {
		return conceptStatus;
	}

	public void setConceptStatus(Property conceptStatus) {
		this.conceptStatus = conceptStatus;
	}
}