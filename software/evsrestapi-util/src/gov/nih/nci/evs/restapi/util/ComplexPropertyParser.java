package gov.nih.nci.evs.restapi.util;

import gov.nih.nci.evs.restapi.bean.*;
import java.io.*;
import java.util.*;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

/*<ncicp:ComplexTerm xmlns:ncicp="http://ncicb.nci.nih.gov/xml/owl/EVS/ComplexProperties.xsd#"><ncicp:term-name>Acinar Cell</ncicp:term-name><ncicp:term-group>SY</ncicp:term-group><ncicp:term-source>NCI</ncicp:term-source><ncicp:source-code>TCGA</ncicp:source-code><ncicp:subsource-name>caDSR</ncicp:subsource-name></ncicp
:ComplexTerm>
*/

public class ComplexPropertyParser {
    public static String reformat(String str) {
		str = str.replaceAll("< ncicp", "<ncicp");
		str = str.replaceAll("< /ncicp", "</ncicp");
		int n = str.indexOf("xmlns");
		str = str.substring(0, n-1) + str.substring(n+73, str.length());
		str = str.replaceAll("ncicp:", "");
		str = str.replaceAll("def-", "");
		str = str.replaceAll("term-", "");
		str = str.replaceAll("subsource-name", "subsource_name");
		str = str.replaceAll("source-", "");
		str = str.replaceAll("ComplexTerm", "gov.nih.nci.evs.restapi.bean.ComplexTerm");
		str = str.replaceAll("ComplexDefinition", "gov.nih.nci.evs.restapi.bean.ComplexDefinition");
		return str;
	}

    public static ComplexDefinition convertToComplexDefinition(String definition) {
		System.out.println(definition);
        XStream xStream = new XStream();
		xStream.alias("gov.nih.nci.evs.restapi.bean.ComplexDefinition", gov.nih.nci.evs.restapi.bean.ComplexDefinition.class);
		String xml = reformat(definition);
        gov.nih.nci.evs.restapi.bean.ComplexDefinition complexDefinition = (gov.nih.nci.evs.restapi.bean.ComplexDefinition) xStream.fromXML(xml);
        return complexDefinition;
	}

    public static ComplexTerm convertToComplexTerm(String full_syn) {
		System.out.println(full_syn);
        XStream xStream = new XStream();
		xStream.alias("gov.nih.nci.evs.sparqlbrowser.bean.ComplexTerm", gov.nih.nci.evs.restapi.bean.ComplexTerm.class);
		String xml = reformat(full_syn);
        gov.nih.nci.evs.restapi.bean.ComplexTerm complexTerm = (gov.nih.nci.evs.restapi.bean.ComplexTerm) xStream.fromXML(xml);
        return complexTerm;
	}


    public static void main(String[] args) {
		String full_syn = "<ncicp:ComplexTerm xmlns:ncicp=\"http://ncicb.nci.nih.gov/xml/owl/EVS/ComplexProperties.xsd#\"><ncicp:term-name>Activity</ncicp:term-name><ncicp:term-group>PT</ncicp:term-group><ncicp:term-source>BRIDG</ncicp:term-source></ncicp:ComplexTerm>";
		ComplexTerm term = convertToComplexTerm(full_syn);
		System.out.println(term.toString());
    }

}

