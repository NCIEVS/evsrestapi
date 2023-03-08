package gov.nih.nci.evs.restapi.config;

import gov.nih.nci.evs.restapi.util.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author EVS Team
 * @version 1.0
 *
 * Modification history:
 *     Initial implementation kim.ong@nih.gov
 *
 */

abstract public class ConfigurationController {
	/** The sys prop. */
	private static Properties sysProp = System.getProperties();

	/** The dom. */
	private static Document dom;

	/** The properties. */
	private static Properties properties = loadProperties();

	/** The Constants. */
	public final static String owlfile = properties.getProperty("owlfile");

	public final static String username = properties.getProperty("username");
	public final static String password = properties.getProperty("password");
	public final static String serviceUrl = properties.getProperty("serviceUrl");
	public final static String namedGraph = properties.getProperty("namedGraph");
	public final static String restURL = properties.getProperty("restURL");
	public final static String serviceUrl_ctrp = properties.getProperty("serviceUrl_ctrp");
	public final static String serviceUrls = properties.getProperty("serviceUrls");

	public final static String masterSubsetCodeColumnNumber = properties.getProperty("masterSubsetCodeColumnNumber");
	public final static String masterConceptCodeColumnNumber = properties.getProperty("masterConceptCodeColumnNumber");
	public final static String subsetCodeColumnNumber = properties.getProperty("subsetCodeColumnNumber");
	public final static String conceptCodeColumnNumber = properties.getProperty("conceptCodeColumnNumber");

	public final static String compositeFile = properties.getProperty("compositeFile");
	public final static String projectFile = properties.getProperty("projectFile");

	public final static String mapping_source_shortname = properties.getProperty("mapping_source_shortname");
	public final static String mapping_target_shortname = properties.getProperty("mapping_target_shortname");
	public final static String mapping_source_graphname = properties.getProperty("mapping_source_graphname");
	public final static String mapping_target_graphname = properties.getProperty("mapping_target_graphname");
	public final static String mapping_datafile = properties.getProperty("mapping_datafile");
    public final static String mapping_filename = properties.getProperty("mapping_filename");
	public final static String mapping_source_id = properties.getProperty("mapping_source_id");
    public final static String mapping_target_id = properties.getProperty("mapping_target_id");

	public final static String source_coding_scheme = properties.getProperty("source_coding_scheme");
	public final static String source_coding_scheme_version = properties.getProperty("source_coding_scheme_version");
	public final static String target_coding_scheme =  properties.getProperty("target_coding_scheme");
	public final static String target_coding_scheme_version = properties.getProperty("target_coding_scheme_version");
	public final static String mapping_name = properties.getProperty("mapping_name");
	public final static String mapping_version = properties.getProperty("mapping_version");

	public final static String source_ns = properties.getProperty("source_ns");
	public final static String target_ns = properties.getProperty("target_ns");

	public final static String ontology_display_label = properties.getProperty("ontology_display_label");
	public final static String ontology_version_info = properties.getProperty("ontology_version_info");
	public final static String ontology_release_date = properties.getProperty("ontology_release_date");
	public final static String ontology_description = properties.getProperty("ontology_description");

	public final static String termfiles = properties.getProperty("termfiles");
	public final static String term_file_heading = properties.getProperty("term_file_heading");
	public final static String term_file_delim = properties.getProperty("term_file_delim");
	public final static String match_file_heading = properties.getProperty("match_file_heading");
	public final static String term_column = properties.getProperty("term_column");

	/**
	 * To be implemented by each descendant testcase.
	 *
	 * @return String
	 */
	protected String getTestID(){
		return "Test Case";
	}


	/**
	 * Load properties.
	 *
	 * @return the properties
	 */
	private static Properties loadProperties() {
		try{
			String propertyFile = "resources/Test.properties";
			Properties lproperties = new Properties();
			FileInputStream fis = new FileInputStream(new File(propertyFile));
			lproperties.load(fis);
			return lproperties;
		} catch (Exception e){
			System.out.println("Error reading properties file");
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Parses the xml file.
	 *
	 * @param filename the filename
	 */
	private static void parseXMLFile(String filename)
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try{
			DocumentBuilder db = dbf.newDocumentBuilder();
			dom=db.parse(filename);
		}
		catch (Exception e){
			e.printStackTrace();
		}
	}

}


