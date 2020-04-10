package gov.nih.nci.evs.api.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Paths {
	private List <Path> paths = null;

	public Paths() {
		paths = new ArrayList <Path>();
	}

	public Paths(List <Path> paths) {
			this.paths = paths;
	}

	public void setPath(List <Path>paths) {
		this.paths = paths;
	}

	public void add(Path path) {
		this.paths.add(path);
	}


	public List<Path> getPaths() {
		return this.paths;
	}

	@JsonIgnore
	public int getPathCount() {
		return this.paths.size();
	}
}
