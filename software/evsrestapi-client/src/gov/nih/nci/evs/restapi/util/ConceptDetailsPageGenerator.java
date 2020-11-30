package gov.nih.nci.evs.restapi.util;
import gov.nih.nci.evs.restapi.model.*;
import gov.nih.nci.evs.restapi.bean.*;

import java.io.*;
import java.text.*;
import java.util.*;

public class ConceptDetailsPageGenerator {

	public ConceptDetailsPageGenerator() {

	}

	public void writeHeader(PrintWriter out, String title) {
		out.println("<!DOCTYPE html>");
		out.println("<html lang=\"en\">");
		out.println("<head>");
		out.println("<meta charset=\"utf-8\">");
		out.println("<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">");
		out.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
		out.println("<title>" + title + "</title>");
		out.println("<link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css\">");
		out.println("<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js\"></script>");
		out.println("<script src=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js\"></script>");
		out.println("<style type=\"text/css\">");
		out.println("    .bs-example{");
		out.println("    	margin: 50px;");
		out.println("    }");
		out.println("</style>");
		out.println("</head>");
	}

	public void writeBody(PrintWriter out, String title, gov.nih.nci.evs.restapi.model.ConceptDetails c) {
		out.println("<body>");
		out.println("<div class=\"bs-example\">");
		out.println("    <div class=\"panel panel-default\">");
		out.println("		<!-- Default panel contents -->");
		out.println("		<center><b><h3>" + title + "</h3></b></center>");
		out.println("<p></p>");
        out.println("<div");
        List synonyms = c.getSynonyms();

        if (synonyms == null || synonyms.size() == 0) {
			out.println("		<h5><b>&nbsp;&nbsp;Synonyms</b>: none</h5>");
		} else {
			out.println("		<h5><b>&nbsp;&nbsp;Synonyms</b>:</h5>");
			out.println("			<table width=100%>");
			out.println("				<tbody>");

			out.println("					<tr>");

			out.println("						<th></th>");
			out.println("						<th>Name</th>");
			out.println("						<th>Type</th>");
			out.println("						<th>Term Group</th>");
			out.println("						<th>Source</th>");
			out.println("						<th>Subsource</th>");
			out.println("						<th>Source Code</th>");

			out.println("					</tr>");

			for (int i=0; i<synonyms.size(); i++) {
				gov.nih.nci.evs.restapi.model.Synonym synonym = (gov.nih.nci.evs.restapi.model.Synonym) synonyms.get(i);

				String termgroup = synonym.getTermGroup();
				if (termgroup == null || termgroup.compareTo("null") == 0) {
					termgroup = "";
				}

				String source = synonym.getSource();
				if (source == null || source.compareTo("null") == 0) {
					source = "";
				}

				String subsource = synonym.getSubSource();
				if (subsource == null || subsource.compareTo("null") == 0) {
					subsource = "";
				}

				String sourcecode = synonym.getCode();
				if (sourcecode == null || sourcecode.compareTo("null") == 0) {
					sourcecode = "";
				}

				out.println("					<tr>");
				out.println("						<td>&nbsp;</td>");
				out.println("						<td>" + synonym.getName() + "</td>");
				out.println("						<td>" + synonym.getType() + "</td>");
				out.println("						<td>" + termgroup + "</td>");
				out.println("						<td>" + source + "</td>");
				out.println("						<td>" + subsource + "</td>");
				out.println("						<td>" + sourcecode + "</td>");
				out.println("					</tr>");
			}

			out.println("				</tbody>");
			out.println("			</table>");
	    }
	    out.println("</div");
	    out.println("<p></p>");

	    out.println("<div>");
        List defs = c.getDefinitions();
        if (defs == null || defs.size() == 0) {
			out.println("		<h5><b>&nbsp;&nbsp;Definitions</b>: none</h5>");
		} else {
			out.println("		<h5><b>&nbsp;&nbsp;Definitions</b>:</h5>");
			out.println("			<table width=100%>");
			out.println("				<thead>");
			out.println("					<tr>");
			out.println("						<th></th>");
			out.println("						<th>Type</th>");
			out.println("						<th>Description</th>");
			out.println("						<th>Source</th>");

			out.println("					</tr>");
			out.println("				</thead>");
			out.println("				<tbody>");
			for (int i=0; i<defs.size(); i++) {
				gov.nih.nci.evs.restapi.model.Definition a = (gov.nih.nci.evs.restapi.model.Definition) defs.get(i);
				String type = a.getType();
				if (type == null || type.compareTo("null") == 0) {
					type = "DEFINITION";
				}

				out.println("					<tr>");
				out.println("						<td>&nbsp;</td>");
				out.println("						<td>" + type + "</td>");
				out.println("						<td>" + a.getDefinition() + "</td>");
				out.println("						<td>" + a.getSource() + "</td>");
				out.println("					</tr>");
			}
			out.println("				</tbody>");
			out.println("			</table>");
	    }
	    out.println("</div>");
	    out.println("<p></p>");

	    out.println("<div>");
        List properties = c.getProperties();
        if (properties == null || properties.size() == 0) {
			out.println("		<h5><b>&nbsp;&nbsp;Properties</b>: none</h5>");
		} else {
			out.println("		<h5><b>&nbsp;&nbsp;Properties</b>:</h5>");
			out.println("			<table width=100%>");
			out.println("				<thead>");
			out.println("					<tr>");
			out.println("						<th></th>");
			out.println("						<th>Type</th>");
			out.println("						<th>Value</th>");

			out.println("					</tr>");
			out.println("				</thead>");
			out.println("				<tbody>");
			for (int i=0; i<properties.size(); i++) {
				gov.nih.nci.evs.restapi.model.Property a = (gov.nih.nci.evs.restapi.model.Property) properties.get(i);

				out.println("					<tr>");
				out.println("						<td>&nbsp;</td>");
				out.println("						<td>" + a.getType() + "</td>");
				out.println("						<td>" + a.getValue() + "</td>");
				out.println("					</tr>");
			}
			out.println("				</tbody>");
			out.println("			</table>");
	    }
	    out.println("</div>");
	    out.println("<p></p>");

	    out.println("<div>");

        List superconcepts = c.getParents();
        if (superconcepts == null || superconcepts.size() == 0) {
			out.println("		<h5><b>&nbsp;&nbsp;Superconcepts</b>: none</h5>");
		} else {
			out.println("		<h5><b>&nbsp;&nbsp;Superconcepts</b>:</h5>");
			out.println("			<table width=100%>");
			out.println("				<tbody>");

			out.println("					<tr>");
			out.println("						<td>&nbsp;</td>");
			out.println("						<td><b>Code</b></td>");
			out.println("						<td><b>Name</b></td>");
			out.println("					</tr>");


			for (int i=0; i<superconcepts.size(); i++) {
				Superclass superconcept = (Superclass) superconcepts.get(i);
				out.println("					<tr>");
				out.println("						<td>&nbsp;</td>");
				out.println("						<td>" + superconcept.getCode() + "</td>");
				out.println("						<td>" + superconcept.getName() + "</td>");
				out.println("					</tr>");
			}
			out.println("				</tbody>");
			out.println("			</table>");
	    }
	    out.println("<p></p>");
	    out.println("<div>");
        out.println("<div>");
        List subconcepts = c.getChildren();
        if (subconcepts == null || subconcepts.size() == 0) {
			out.println("		<h5><b>&nbsp;&nbsp;Subconcepts</b>: none</h5>");
		} else {
			out.println("		<h5><b>&nbsp;&nbsp;Subconcepts</b>:</h5>");
			out.println("			<table width=100%>");
			out.println("				<tbody>");

			out.println("					<tr>");
			out.println("						<td>&nbsp;</td>");
			out.println("						<td><b>Code</b></td>");
			out.println("						<td><b>Name</b></td>");
			out.println("					</tr>");

			for (int i=0; i<subconcepts.size(); i++) {
				Subclass subconcept = (Subclass) subconcepts.get(i);
				out.println("					<tr>");
				out.println("						<td>&nbsp;</td>");
				out.println("						<td>" + subconcept.getCode() + "</td>");
				out.println("						<td>" + subconcept.getName() + "</td>");
				out.println("					</tr>");
			}
			out.println("				</tbody>");
			out.println("			</table>");
	    }
	    out.println("</div>");
	    out.println("<p></p>");

	    out.println("<div>");
	    /////////////////////////////////////////////////////////////////////////////////
        List associations = c.getAssociations();
        if (associations == null || associations.size() == 0) {
			out.println("		<h5><b>&nbsp;&nbsp;Associations</b>: none</h5>");
		} else {
			out.println("		<h5><b>&nbsp;&nbsp;Associations</b>:</h5>");
			out.println("			<table width=100%>");
			out.println("				<thead>");
			out.println("					<tr>");
			out.println("						<th></th>");

			out.println("						<th>Name</th>");
			out.println("						<th>Related Name</th>");
			out.println("						<th>Related Code</th>");

			out.println("					</tr>");
			out.println("				</thead>");
			out.println("				<tbody>");
			for (int i=0; i<associations.size(); i++) {
				gov.nih.nci.evs.restapi.model.Association a = (gov.nih.nci.evs.restapi.model.Association) associations.get(i);
				out.println("					<tr>");
				out.println("						<td>&nbsp;</td>");
				out.println("						<td>" + a.getType() + "</td>");
				out.println("						<td>" + a.getRelatedName() + "</td>");
				out.println("						<td>" + a.getRelatedCode() + "</td>");
				out.println("					</tr>");
			}
			out.println("				</tbody>");
			out.println("			</table>");
	    }
	    out.println("</div>");
	    out.println("<p></p>");

	    out.println("<div>");
        List inv_associations = c.getInverseAssociations();
        if (inv_associations == null || inv_associations.size() == 0) {
			out.println("		<h5><b>&nbsp;&nbsp;Inverse Associations</b>: none</h5>");
		} else {
			out.println("		<h5><b>&nbsp;&nbsp;Inverse Associations</b>:</h5>");
			out.println("			<table width=100%>");
			out.println("				<thead>");
			out.println("					<tr>");
			out.println("						<th></th>");

			out.println("						<th>Related Name</th>");
			out.println("						<th>Related Code</th>");
			out.println("						<th>Name</th>");

			out.println("					</tr>");
			out.println("				</thead>");
			out.println("				<tbody>");
			for (int i=0; i<inv_associations.size(); i++) {
				gov.nih.nci.evs.restapi.model.InverseAssociation a = (gov.nih.nci.evs.restapi.model.InverseAssociation) inv_associations.get(i);
				out.println("					<tr>");
				out.println("						<td>&nbsp;</td>");

				out.println("						<td>" + a.getRelatedName() + "</td>");
				out.println("						<td>" + a.getRelatedCode() + "</td>");
				out.println("						<td>" + a.getType() + "</td>");

				out.println("					</tr>");
			}
			out.println("				</tbody>");
			out.println("			</table>");
	    }


	    out.println("<div>");
	    out.println("<p></p>");
        List roles = c.getRoles();
        if (roles == null || roles.size() == 0) {
			out.println("		<h5><b>&nbsp;&nbsp;Roles</b>: none</h5>");
		} else {
			out.println("		<h5><b>&nbsp;&nbsp;Roles</b>:</h5>");
			out.println("			<table width=100%>");
			out.println("				<thead>");
			out.println("					<tr>");
			out.println("						<th></th>");

			out.println("						<th>Name</th>");
			out.println("						<th>Related Name</th>");
			out.println("						<th>Related Code</th>");

			out.println("					</tr>");
			out.println("				</thead>");
			out.println("				<tbody>");
			for (int i=0; i<roles.size(); i++) {
				gov.nih.nci.evs.restapi.model.Role a = (gov.nih.nci.evs.restapi.model.Role) roles.get(i);
				out.println("					<tr>");
				out.println("						<td>&nbsp;</td>");
				out.println("						<td>" + a.getType() + "</td>");
				out.println("						<td>" + a.getRelatedName() + "</td>");
				out.println("						<td>" + a.getRelatedCode() + "</td>");
				out.println("					</tr>");
			}
			out.println("				</tbody>");
			out.println("			</table>");
	    }
	    out.println("</div");
	    out.println("<p></p>");
	    out.println("<div>");
        List inv_roles = c.getInverseRoles();
        if (inv_roles == null || inv_roles.size() == 0) {
			out.println("		<h5><b>&nbsp;&nbsp;Inverse Roles</b>: none</h5>");
		} else {
			out.println("		<h5><b>&nbsp;&nbsp;Inverse Roles</b>:</h5>");
			out.println("			<table width=100%>");
			out.println("				<thead>");
			out.println("					<tr>");
			out.println("						<th></th>");

			out.println("						<th>Related Name</th>");
			out.println("						<th>Related Code</th>");
			out.println("						<th>Name</th>");

			out.println("					</tr>");
			out.println("				</thead>");
			out.println("				<tbody>");
			for (int i=0; i<inv_roles.size(); i++) {
				gov.nih.nci.evs.restapi.model.InverseRole a = (gov.nih.nci.evs.restapi.model.InverseRole) inv_roles.get(i);
				out.println("					<tr>");
				out.println("						<td>&nbsp;</td>");

				out.println("						<td>" + a.getRelatedName() + "</td>");
				out.println("						<td>" + a.getRelatedCode() + "</td>");
				out.println("						<td>" + a.getType() + "</td>");

				out.println("					</tr>");
			}
			out.println("				</tbody>");
			out.println("			</table>");
	    }
	    out.println("</div");
        out.println("<p></p>");

	    out.println("<div>");
        List maps = c.getMaps();
        if (maps == null || maps.size() == 0) {
			out.println("		<h5><b>&nbsp;&nbsp;MapsTo</b>: none</h5>");
		} else {
			out.println("		<h5><b>&nbsp;&nbsp;MapsTo</b>:</h5>");
			out.println("			<table width=100%>");
			out.println("				<thead>");
			out.println("					<tr>");
			out.println("						<th></th>");

			out.println("						<th>Type</th>");
			out.println("						<th>Target Name</th>");
			out.println("						<th>Target Term Group</th>");
			out.println("						<th>Target Code</th>");
			out.println("						<th>Target Termonology</th>");
			out.println("						<th>Target Termonology Version</th>");

			out.println("					</tr>");
			out.println("				</thead>");
			out.println("				<tbody>");
			for (int i=0; i<maps.size(); i++) {
				gov.nih.nci.evs.restapi.model.MapsTo a = (gov.nih.nci.evs.restapi.model.MapsTo) maps.get(i);
				out.println("					<tr>");
				out.println("						<td>&nbsp;</td>");

				out.println("						<td>" + a.getType() + "</td>");
				out.println("						<td>" + a.getTargetName() + "</td>");
				out.println("						<td>" + a.getTargetTermGroup() + "</td>");
				out.println("						<td>" + a.getTargetCode() + "</td>");
				out.println("						<td>" + a.getTargetTerminology() + "</td>");
				out.println("						<td>" + a.getTargetTerminologyVersion() + "</td>");

				out.println("					</tr>");
			}
			out.println("				</tbody>");
			out.println("			</table>");
	    }

		out.println("</div>");
		out.println("</body>");
	}

	public void writeFooter(PrintWriter out) {
		out.println("</html>");
	}

    public void generate(PrintWriter out, String title, gov.nih.nci.evs.restapi.model.ConceptDetails c) {
        writeHeader(out, title);
        writeBody(out, title, c);
        writeFooter(out);
	}

	public void generate(String outputfile, String title, gov.nih.nci.evs.restapi.model.ConceptDetails c) {
		long ms = System.currentTimeMillis();
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			generate(pw, title, c);

		} catch (Exception ex) {

		} finally {
			try {
				pw.close();
				System.out.println("Output file " + outputfile + " generated.");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}

	public static void main(String[] args) {
	    Vector v = null;
	    try {
            //String terminology = "ncit";
            //String code = "C3224";
            String terminology = args[0];
            String code = args[1];

            gov.nih.nci.evs.restapi.model.ConceptDetails c = EVSRESTAPIClient.getConceptDetails(terminology, code);
            /*
            String json = c.toJson();
            //System.out.println(c.toJson());
            String flattened_json = EVSRESTAPIClient.flattenJSON(json);
            System.out.println(flattened_json);
            gov.nih.nci.evs.restapi.model.ConceptDetails c2 = (gov.nih.nci.evs.restapi.model.ConceptDetails) EVSRESTAPIClient.deserialize("ConceptDetails", flattened_json);
            System.out.println(c2.toJson());
            */

			ConceptDetailsPageGenerator generator = new ConceptDetailsPageGenerator();
			String outputfile = "concept_details_" + code + ".html";
			String title = c.getName() + " (" + code + ")";
			generator.generate(outputfile, title, c);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}


/*
	private String code;
	private String name;
	private String terminology;
	private String version;
	private List<Synonym> synonyms;
	private List<Definition> definitions;
	private List<Property> properties;
	private List<Superclass> parents;
	private List<Subclass> children;
	private List<Association> associations;
	private List<InverseAssociation> inverseAssociations;
	private List<Role> roles;
	private List<InverseRole> inverseRoles;
	private List<MapsTo> maps;
*/