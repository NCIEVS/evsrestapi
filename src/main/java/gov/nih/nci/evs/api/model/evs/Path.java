package gov.nih.nci.evs.api.model.evs;

import java.util.List;

public class Path {
	private int direction;
	private List <Concept> concepts;

	public Path() {}

	public Path( int direction, List <Concept>concepts) {
		this.direction = direction;
		this.concepts = concepts;
	}

	public void setDirection(int direction) {
		this.direction = direction;
	}


	public void setConcepts(List <Concept> concepts) {
		this.concepts = concepts;
	}
	
	public int getDirection() {
		return this.direction;
	}

	public List <Concept> getConcepts() {
		return this.concepts;
	}
}
