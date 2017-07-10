package gov.nih.nci.evs.api.model.evs;

import java.util.ArrayList;
import java.util.List;

public class Paths {// Variable declaration
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


	public int getPathCount() {
		return this.paths.size();
	}
}
