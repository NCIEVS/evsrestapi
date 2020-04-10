package gov.nih.nci.evs.api.model;

import java.util.List;

public class Path {
	private int direction;
	private List <ConceptNode> concepts;

	public Path() {}

	public Path( int direction, List <ConceptNode>concepts) {
		this.direction = direction;
		this.concepts = concepts;
	}

	public void setDirection(int direction) {
		this.direction = direction;
	}


	public void setConcepts(List <ConceptNode> concepts) {
		this.concepts = concepts;
	}
	
	public int getDirection() {
		return this.direction;
	}

	public List <ConceptNode> getConcepts() {
		return this.concepts;
	}
}