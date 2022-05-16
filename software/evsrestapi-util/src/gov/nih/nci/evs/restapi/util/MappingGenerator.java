import gov.nih.nci.evs.restapi.util.*;

import java.io.*;
import java.text.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;
import java.nio.charset.Charset;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright 2022 Guidehouse. This software was developed in conjunction
 * with the National Cancer Institute, and so to the extent government
 * employees are co-authors, any rights in such works shall be subject
 * to Title 17 of the United States Code, section 105.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *   1. Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the disclaimer of Article 3,
 *      below. Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials provided
 *      with the distribution.
 *   2. The end-user documentation included with the redistribution,
 *      if any, must include the following acknowledgment:
 *      "This product includes software developed by Guidehouse and the National
 *      Cancer Institute."   If no such end-user documentation is to be
 *      included, this acknowledgment shall appear in the software itself,
 *      wherever such third-party acknowledgments normally appear.
 *   3. The names "The National Cancer Institute", "NCI" and "Guidehouse" must
 *      not be used to endorse or promote products derived from this software.
 *   4. This license does not authorize the incorporation of this software
 *      into any third party proprietary programs. This license does not
 *      authorize the recipient to use any trademarks owned by either NCI
 *      or GUIDEHOUSE
 *   5. THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESSED OR IMPLIED
 *      WARRANTIES, (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *      OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE) ARE
 *      DISCLAIMED. IN NO EVENT SHALL THE NATIONAL CANCER INSTITUTE,
 *      GUIDEHOUSE, OR THEIR AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT,
 *      INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *      BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *      LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *      CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *      LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *      ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *      POSSIBILITY OF SUCH DAMAGE.
 * <!-- LICENSE_TEXT_END -->
 */

/**
 * @author EVS Team
 * @version 1.0
 *
 * Modification history:
 *     Initial implementation kim.ong@nih.gov
 *
 */


public class MappingGenerator {
	static String HEADER = "Source Code,Source Name,Source Coding Scheme,Source Coding Scheme Version,Source Coding Scheme Namespace,Association Name,REL,Map Rank,Target Code,Target Name,Target Coding Scheme,Target Coding Scheme Version,Target Coding Scheme Namespace";
    static String EVS = "http://ncicb.nci.nih.gov/xml/owl/EVS/";
	static String BASE = "http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl";
	static String LG_CON ="http://LexGrid.org/schema/2010/01/LexGrid/concepts";

    static final String MAPPING_NAME = "met:concise_name";
    static final String MAPPING_VERSION = "met:version";
    static final String RELEASE_DATE = "met:version_releaseDate";
    static final String MAPPING_DESCRIPTION = "met:description";
    static final String MAPPING_DISPLAY_NAME = "met:display_name";
    static final String MAPPING_RANK_APPLICABLE = "met:map_rank_applicable";

    static HashMap metadataHashMap = null;

    static String SOURCE_NS = "";
    static String TARGET_NS = "";

    static String[] METADATA = new String[]{
		MAPPING_NAME, MAPPING_VERSION, RELEASE_DATE, MAPPING_DESCRIPTION, MAPPING_DISPLAY_NAME, MAPPING_RANK_APPLICABLE
	};

    public static Vector parseData(String line, char delimiter) {
		if(line == null) return null;
		Vector w = new Vector();
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<line.length(); i++) {
			char c = line.charAt(i);
			if (c == delimiter) {
				w.add(buf.toString());
				buf = new StringBuffer();
			} else {
				buf.append(c);
			}
		}
		w.add(buf.toString());
		return w;
	}

    public static void dumpHashMap(String label, HashMap hmap) {
		System.out.println("\n" + label + ":");
		if (hmap == null) {
			System.out.println("\tNone");
			return;
		}
		Vector key_vec = new Vector();
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			key_vec.add(key);
		}
		key_vec = new SortUtils().quickSort(key_vec);
		for (int i=0; i<key_vec.size(); i++) {
			String key = (String) key_vec.elementAt(i);
			String value = (String) (String) hmap.get(key);
			System.out.println(key + " --> " + value);
		}
		System.out.println("\n");
	}

    public static void dumpMultiValuedHashMap(String label, HashMap hmap) {
   	    System.out.println("\n" + label + ":");
		if (hmap == null) {
			System.out.println("\tNone");
			return;
		}
		Vector key_vec = new Vector();
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String nv = (String) it.next();
			key_vec.add(nv);
		}
		key_vec = new SortUtils().quickSort(key_vec);
		for (int k=0; k<key_vec.size(); k++) {
			String nv = (String) key_vec.elementAt(k);
			System.out.println("\n");
			Vector v = (Vector) hmap.get(nv);
			for (int i=0; i<v.size(); i++) {
				String q = (String) v.elementAt(i);
				System.out.println(nv + " --> " + q);
			}
		}
		System.out.println("\n");
	}

    public static void dumpVector(String label, Vector v) {
		System.out.println("\n" + label + ":");
		if (v == null) return;
		if (v.size() == 0) {
			System.out.println("\tNone");
			return;
		}
        for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			int j = i+1;
			System.out.println("\t(" + j + ") " + t);
		}
		System.out.println("\n");
	}

    public static void dumpVector(String label, Vector v, boolean display_label, boolean display_index) {
		if (display_label) {
			System.out.println("\n" + label + ":");
		}
		if (v == null || v.size() == 0) {
			System.out.println("\tNone");
			return;
		}
        for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			int j = i+1;
			if (display_index) {
				System.out.println("\t(" + j + ") " + t);
			} else {
				System.out.println("\t" + t);
			}
		}
		System.out.println("\n");
	}

    public static void dumpArrayList(String label, ArrayList list) {
		System.out.println("\n" + label + ":");
		if (list == null || list.size() == 0) {
			System.out.println("\tNone");
			return;
		}
        for (int i=0; i<list.size(); i++) {
			String t = (String) list.get(i);
			int j = i+1;
			System.out.println("\t(" + j + ") " + t);
		}
		System.out.println("\n");
	}

    public static void dumpList(String label, List list) {
		System.out.println("\n" + label + ":");
		if (list == null || list.size() == 0) {
			System.out.println("\tNone");
			return;
		}
        for (int i=0; i<list.size(); i++) {
			String t = (String) list.get(i);
			int j = i+1;
			System.out.println("\t(" + j + ") " + t);
		}
		System.out.println("\n");
	}


	 public static String replaceFilename(String filename) {
	    return filename.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
	 }


	public static List<String> findFilesInDirectory(String directory) {
		return findFilesInDirectory(new File(directory));
	}

	public static List<String> findFilesInDirectory(File dir) {
		List<String> list = new ArrayList<String>();
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				List<String> list2 = findFilesInDirectory(file);
				list.addAll(list2);
			} else {
				list.add(file.getAbsolutePath());
			}
		}
		return list;
	}


	 public static void saveToFile(String outputfile, String t) {
		 Vector v = new Vector();
		 v.add(t);
		 saveToFile(outputfile, v);
	 }

	public static void saveToFile(PrintWriter pw, Vector v) {
		if (v != null && v.size() > 0) {
			for (int i=0; i<v.size(); i++) {
				String t = (String) v.elementAt(i);
				pw.println(t);
			}
		}
	}

	public static Vector readFile(String datafile) {
		Vector v = new Vector();
        try {
			File file = new File(datafile);
			FileInputStream fis = new FileInputStream(file);
			BufferedInputStream bis = new BufferedInputStream(fis);
			BufferedReader br = null;
			try {
				br = br = new BufferedReader(new InputStreamReader(bis));
			} catch (Exception ex) {
				return null;
			}

            while (true) {
                String line = br.readLine();
				if (line == null) {
					break;
				}
				v.add(line);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return v;
	}

	public static HashSet vector2HashSet(Vector v) {
		if (v == null) return null;
		HashSet hset = new HashSet();
		for (int i=0; i<v.size(); i++) {
			String t = (String) v.elementAt(i);
			hset.add(t);
		}
		return hset;
	}

	public static Vector hashSet2Vector(HashSet hset) {
		if (hset == null) return null;
		Vector v = new Vector();
		Iterator it = hset.iterator();
		while (it.hasNext()) {
			String t = (String) it.next();
			v.add(t);
		}
		v = new SortUtils().quickSort(v);
		return v;
	}

    public static String changeFileExtension(String filename, String ext) {
		int n = filename.lastIndexOf(".");
		if (n != -1) {
			return filename.substring(0, n) + "." + ext;
		}
		return filename;
	}

	public static HashMap getInverseHashMap(HashMap hmap) {
		HashMap inv_hmap = new HashMap();
		Iterator it = hmap.keySet().iterator();
		while (it.hasNext()) {
			String key = (String) it.next();
			String value = (String) hmap.get(key);
			Vector v = new Vector();
			if (inv_hmap.containsKey(value)) {
				v = (Vector) inv_hmap.get(value);
			}
			v.add(key);
			inv_hmap.put(value, v);
		}
		return inv_hmap;
	}

    public static Vector listFiles(String directory) {
		Vector w = new Vector();
		Collection<File> c = listFileTree(new File(directory));
		int k = 0;
		Iterator it = c.iterator();
		while (it.hasNext()) {
			File t = (File) it.next();
			k++;
			w.add(t.getName());
		}
		w = new SortUtils().quickSort(w);
		return w;
	}


	public static Collection<File> listFileTree(File dir) {
		Set<File> fileTree = new HashSet<File>();
		if(dir==null||dir.listFiles()==null){
			return fileTree;
		}
		for (File entry : dir.listFiles()) {
			if (entry.isFile()) fileTree.add(entry);
			else fileTree.addAll(listFileTree(entry));
		}
		return fileTree;
	}

    public static boolean checkIfFileExists(String filename) {
		String currentDir = System.getProperty("user.dir");
		File f = new File(currentDir + "\\" + filename);
		if(f.exists() && !f.isDirectory()) {
			return true;
		} else {
			return false;
		}
	}

    public static boolean deleteFile(String filename) {
        File file = new File(filename);
        if(file.delete())
        {
            return true;
        }
        else
        {
            return false;
        }
    }


	public static void copyDirectory(String sourceDirectoryLocation, String destinationDirectoryLocation)
	  throws IOException {
	    Files.walk(java.nio.file.Paths.get(sourceDirectoryLocation))
	      .forEach(source -> {
		  java.nio.file.Path destination = java.nio.file.Paths.get(destinationDirectoryLocation, source.toString()
		    .substring(sourceDirectoryLocation.length()));
		  try {
		      Files.copy(source, destination);
		  } catch (IOException e) {
		      e.printStackTrace();
		  }
	      });
	}

	public static String encode(String toEncode) {
		try {
			return java.net.URLEncoder.encode(toEncode.trim(), "UTF-8");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static boolean isPureAscii(String v) {
		return Charset.forName("US-ASCII").newEncoder().canEncode(v);
	}

	public String xmlEscapeText(String t) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < t.length(); i++){
			char c = t.charAt(i);
			switch(c){
				case '<': sb.append("&lt;"); break;
				case '>': sb.append("&gt;"); break;
				case '\"': sb.append("&quot;"); break;
				case '&': sb.append("&amp;"); break;
				case '\'': sb.append("&apos;"); break;
				default:
					if (c>0x7e) {
						sb.append("&#"+((int)c)+";");
					} else {
						sb.append(c);
					}
			}
		}
		return sb.toString();
	}

	public static Vector loadFileLineByLine(String filename) {
		Vector w = new Vector();
		StringBuffer buf = new StringBuffer();
		try {
			InputStream input = new FileInputStream(filename);
			int data = input.read();
			while(data != -1) {
				char c = (char) data;
				if (c == '\n') {
					String s = buf.toString();
					s = s.trim();
					w.add(s);
					buf = new StringBuffer();
				} else {
					buf.append("" + c);
				}
				data = input.read();
			}
			String s = buf.toString();
			s = s.trim();
			w.add(s);
			input.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return w;
	}

    public static void saveToFile(String outputfile, Vector v) {
        try {
            FileOutputStream output = new FileOutputStream(outputfile);
            for (int i=0; i<v.size(); i++) {
				String data = (String) v.elementAt(i);
				if (i < v.size()) {
					data = data + "\n";
				}
				byte[] array = data.getBytes();
				output.write(array);
			}
            output.close();
        } catch(Exception e) {
            e.getStackTrace();
        }
    }

	public static void writeMetadata(PrintWriter out, HashMap hmap) {

		String mappingName=(String) hmap.get(MAPPING_NAME);
		String mappingVersion=(String) hmap.get(MAPPING_VERSION);
		String releaseDate=(String) hmap.get(RELEASE_DATE);
		String mappingDescription=(String) hmap.get(MAPPING_DESCRIPTION);

		out.println("<?xml version=\"1.0\"?>");
		out.println("<rdf:RDF xmlns=\"http://ncicb.nci.nih.gov/xml/owl/EVS/" + mappingName + ".owl#\"");
		out.println("     xml:base=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl\"");
		out.println("     xmlns:ncit=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl\"");
		out.println("     xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"");
		out.println("     xmlns:owl=\"http://www.w3.org/2002/07/owl#\"");
		out.println("     xmlns:oboInOwl=\"http://www.geneontology.org/formats/oboInOwl#\"");
		out.println("     xmlns:Thesaurus=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#\"");
		out.println("     xmlns:xml=\"http://www.w3.org/XML/1998/namespace\"");
		out.println("     xmlns:protege=\"http://protege.stanford.edu/plugins/owl/protege#\"");
		out.println("     xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\"");
		out.println("     xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"");
		out.println("     xmlns:dc=\"http://purl.org/dc/elements/1.1/\">");
		out.println("     xmlns:obo=\"http://purl.obolibrary.org/obo/\">");
		out.println("     xmlns:go=\"http://purl.obolibrary.org/obo/go#\"");

		out.println("     xmlns=\"http://LexGrid.org/schema/2010/01/LexGrid/codingSchemes\"");
		out.println("     xmlns:lgBuiltin=\"http://LexGrid.org/schema/2010/01/LexGrid/builtins\"");
		out.println("     xmlns:lgCommon=\"http://LexGrid.org/schema/2010/01/LexGrid/commonTypes\"");
		out.println("     xmlns:lgCon=\"http://LexGrid.org/schema/2010/01/LexGrid/concepts\"");
		out.println("     xmlns:lgRel=\"http://LexGrid.org/schema/2010/01/LexGrid/relations\"");
		out.println("     xmlns:lgCS=\"http://LexGrid.org/schema/2010/01/LexGrid/codingSchemes\"");
		out.println("     xmlns:lgLDAP=\"http://LexGrid.org/schema/2010/01/LexGrid/ldap\"");
		out.println("     xmlns:lgNaming=\"http://LexGrid.org/schema/2010/01/LexGrid/naming\"");
		out.println("     xmlns:lgService=\"http://LexGrid.org/schema/2010/01/LexGrid/service\"");
		out.println("     xmlns:lgVD=\"http://LexGrid.org/schema/2010/01/LexGrid/valueDomains\"");
		out.println("     xmlns:lgVer=\"http://LexGrid.org/schema/2010/01/LexGrid/versions\"");
		out.println("     xmlns:NCIHistory=\"http://LexGrid.org/schema/2010/01/LexGrid/NCIHistory\"");
		out.println("");


		out.println("    <owl:Ontology rdf:about=\"" + mappingName + "\">");
		out.println("        <owl:versionInfo>" + mappingVersion + "</owl:versionInfo>");
		out.println("        <protege:defaultLanguage>en</protege:defaultLanguage>");
		out.println("        <dc:date>" + releaseDate + "</dc:date>");
		out.println("        <rdfs:comment>" + mappingDescription + "</rdfs:comment>");
		out.println("    </owl:Ontology>");
		out.println("    ");
	}


	public static void writeAnnotationProperties(PrintWriter out) {
		if (!isMappingRankApplicable()) {
			return;
		}

		out.println("    <!-- ");
		out.println("    ///////////////////////////////////////////////////////////////////////////////////////");
		out.println("    //");
		out.println("    // Annotation properties");
		out.println("    //");
		out.println("    ///////////////////////////////////////////////////////////////////////////////////////");
		out.println("     -->");
		out.println("     ");
		out.println("    <!-- http://LexGrid.org/schema/2010/01/LexGrid/naming#P001 -->");
		out.println("");
		out.println("    <owl:AnnotationProperty rdf:about=\"http://http://LexGrid.org/schema/2010/01/LexGrid/naming#P001\">");
		out.println("        <ncit:NHC0>P001</ncit:NHC0>");
		out.println("        <ncit:P106>Conceptual Entity</ncit:P106>");
		out.println("        <ncit:P108>Map Rank</ncit:P108>");
		out.println("        <rdfs:label>score</rdfs:label>");
		out.println("    </owl:AnnotationProperty>");
		out.println("    ");
	}

	public static void writeSourceConcepts(PrintWriter out, Vector mappingData) {
		out.println("    <!-- ");
		out.println("    ///////////////////////////////////////////////////////////////////////////////////////");
		out.println("    //");
		out.println("    // Classes - Source Terminology");
		out.println("    //");
		out.println("    ///////////////////////////////////////////////////////////////////////////////////////");
		out.println("     -->");
		out.println("");

/*
Source Code,Source Name,Source Coding Scheme,Source Coding Scheme Version,Source Coding Scheme Namespace,Association Name,REL,Map Rank,Target Code,Target Name,Target Coding Scheme,Target Coding Scheme Version,Target Coding Scheme Namespace
C1003,Ansamycin Antineoplastic Antibiotic,NCI_Thesaurus,22.04d,NCI_Thesaurus,mapsTo,mapsTo,1,CHEBI:22565,ansamycin,ChEBI,v210,ChEBI
        (1) Source Code
        (2) Source Name
        (3) Source Coding Scheme
        (4) Source Coding Scheme Version
        (5) Source Coding Scheme Namespace
        (6) Association Name
        (7) REL
        (8) Map Rank
        (9) Target Code
        (10) Target Name
        (11) Target Coding Scheme
        (12) Target Coding Scheme Version
        (13) Target Coding Scheme Namespace

        (1) C104002
        (2) Deoxyuridine Triphosphate
        (3) NCI_Thesaurus
        (4) 22.04d
        (5) NCI_Thesaurus
        (6) mapsTo
        (7) mapsTo
        (8) 1
        (9) CHEBI:17625
        (10) dUTP
        (11) ChEBI
        (12) v210
        (13) ChEBI
*/
        for (int i=1; i<mappingData.size(); i++) {
			String line = (String) mappingData.elementAt(i);
			Vector u = parseData(line, '\t');
			String sourceCode = (String) u.elementAt(0);
			String sourceName = (String) u.elementAt(1);
			String sourceCodingScheme = (String) u.elementAt(2);

			out.println("");
			out.println("    <!-- http://" + SOURCE_NS + sourceCode + " -->");
            out.println("");
			out.println("    <owl:Class rdf:about=\"" + SOURCE_NS + sourceCode + "\">");
			out.println("        <rdfs:label>" + sourceName + "</rdfs:label>");
			out.println("        <ncit:P375>" + sourceName + "</ncit:P375>");
            out.println("    </owl:Class>");

			writeAxiom(out, line);

			out.println("");
		}
	}


	public static void writeTargetConcepts(PrintWriter out, Vector mappingData) {
		out.println("    <!-- ");
		out.println("    ///////////////////////////////////////////////////////////////////////////////////////");
		out.println("    //");
		out.println("    // Classes - Target Terminology");
		out.println("    //");
		out.println("    ///////////////////////////////////////////////////////////////////////////////////////");
		out.println("     -->");
		out.println("");


        for (int i=1; i<mappingData.size(); i++) {
			String line = (String) mappingData.elementAt(i);
			Vector u = parseData(line, '\t');
			String targetCode = (String) u.elementAt(8);
			String targetName = (String) u.elementAt(9);
			String argetCodingScheme = (String) u.elementAt(10);

			out.println("");
			out.println("    <!-- http://" + TARGET_NS + targetCode + " -->");

			out.println("    <owl:Class rdf:about=\"" + TARGET_NS + targetCode + "\">");
			out.println("        <rdfs:label>" + targetName + "</rdfs:label>");
			out.println("    </owl:Class>");
			out.println("");
		}
	}

	public static boolean isMappingRankApplicable() {
		String value = (String) metadataHashMap.get(MAPPING_RANK_APPLICABLE);
		if (value == null) {
			return false;
		}
		if (value.compareTo("true") == 0) {
			return true;
		}
		return false;
	}

	public static void createMetadataHashMap(String metadataXML) {
		metadataHashMap = new HashMap();
		// Instantiate the Factory
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try (InputStream is = XmlDomParser.readXmlFileIntoInputStream(metadataXML)) {
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(is);

			for (int i=0; i<METADATA.length; i++) {
				String nodeName = METADATA[i];
				Node node = XmlDomParser.searchNode(doc.getChildNodes(), nodeName);
				if (node != null) {
					metadataHashMap.put(nodeName, node.getTextContent());
					System.out.println(nodeName + ":" + node.getTextContent());
				}
			}

		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}
	}

    public static void run(String metadataXML, String mappingdata, String sourceNS, String targetNS) {
		SOURCE_NS = sourceNS;
		TARGET_NS = targetNS;
		createMetadataHashMap(metadataXML);
		int n = mappingdata.lastIndexOf(".");
		String outputfile = mappingdata.substring(0, n) + "_" + (String) metadataHashMap.get(MAPPING_VERSION) + ".owl";
		Vector v = readFile(mappingdata);
        long ms = System.currentTimeMillis();
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			writeMetadata(pw, metadataHashMap);
			writeAnnotationProperties(pw);
			writeSourceConcepts(pw, v);
			writeTargetConcepts(pw, v);
			writeFooter(pw);

		} catch (Exception ex) {

		} finally {
			try {
				pw.close();
				System.out.println("Output file " + outputfile + " generated.");
				System.out.println("Graph name: " + "http://ncicb.nci.nih.gov/xml/owl/EVS/" + (String) metadataHashMap.get(MAPPING_NAME) + ".owl");

			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

    public static void writeAxiom(PrintWriter out, String data) {
		Vector u = parseData(data, '\t');
		String source_Code = (String) u.elementAt(0);
		String source_Name = (String) u.elementAt(1);
		String sourceCodingScheme = (String) u.elementAt(2);
		String target_Term_Type = (String) u.elementAt(6);
		String rank = (String) u.elementAt(7);
		String target_Terminology_Version = (String) u.elementAt(11);
		// patch
		if (target_Term_Type.compareTo("mapsTo") == 0) {
			target_Term_Type = "SY";
		}
		String target_Code = (String) u.elementAt(8);
		String target_Name = (String) u.elementAt(9);
		String target_Coding_Scheme = (String) u.elementAt(10);

		out.println("    <owl:Axiom>");
		out.println("        <owl:annotatedSource rdf:resource=\"" + SOURCE_NS + source_Code + "\"/>");
		out.println("        <owl:annotatedProperty rdf:resource=\"http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#P375\"/>");
		out.println("        <owl:annotatedTarget>" + source_Name + "</owl:annotatedTarget>");
		out.println("        <ncit:P393>Has Synonym</ncit:P393>");
		if (isMappingRankApplicable()) {
			out.println("        <lgNaming:P001>" + rank + "</lgNaming:P001>");
	    }
		out.println("        <ncit:P394>" + target_Term_Type + "</ncit:P394>");
		out.println("        <ncit:P395>" + target_Code + "</ncit:P395>");
		out.println("        <ncit:P396>" + target_Coding_Scheme + "</ncit:P396>");
		out.println("        <ncit:P397>" + target_Terminology_Version + "</ncit:P397>");
		out.println("    </owl:Axiom>");
	}

    public static void writeFooter(PrintWriter out) {
		out.println("    ");
		out.println("</rdf:RDF>");
		out.println("");
		out.println("<!-- Generated by MappingGenerator (" + StringUtils.getToday() + ") -->");
		out.println("    ");
	}

	public static void main(String[] args) {
		long ms = System.currentTimeMillis();
		String metadatafile = args[0];
		String mappingdatafile = args[1];

		String sourceNS = args[2];
		String targetNS = args[3];

		run(metadatafile, mappingdatafile, sourceNS, targetNS);
		System.out.println("Total run time (ms): " + (System.currentTimeMillis() - ms));
	}
}

