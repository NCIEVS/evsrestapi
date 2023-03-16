package gov.nih.nci.evs.restapi.util;
import gov.nih.nci.evs.restapi.bean.*;
import java.io.*;
import java.text.*;
import java.util.*;


public class ASCII2HTMLTreeConverter {

    public static void generateHTMLTree(String asciitreefile, String title, String root) {
		Vector parent_child_vec = Utils.readFile(asciitreefile);
		parent_child_vec = new ASCIITreeUtils().get_parent_child_vec(parent_child_vec);
		String datafile = "flattened_" + asciitreefile;
		Utils.saveToFile(datafile, parent_child_vec);
		HTMLHierarchy.run(datafile, title, root);
    }

    public static void generateDynamicHTMLTree(String parent_child_file) {
		Vector parent_child_vec = Utils.readFile(parent_child_file);
        TreeItem ti = new ASCIITreeUtils().createTreeItem(parent_child_vec);
        HashMap hmap = new HashMap();
        hmap.put("<Root>", ti);
        String url = "https://nciterms.nci.nih.gov/ncitbrowser/ConceptReport.jsp?dictionary=NCI%20Thesaurus&code=";
        new SimpleTreeUtils().writeTree2HTML(hmap, url, "test.html");
    }
}