package gov.nih.nci.evs.restapi.util;
import java.io.*;
import java.util.*;
import gov.nih.nlm.nls.lvg.Lib.*;
import gov.nih.nlm.nls.lvg.Flows.*;
import gov.nih.nlm.nls.lvg.Api.*;

public class LvgApiUtils {
	private LvgApi lvg = null;

	public LvgApiUtils(String currentDirectory) {
		Hashtable<String, String> properties = new Hashtable<String, String>();
		if (currentDirectory == null) {
			currentDirectory = System.getProperty("user.dir");
		}
		properties.put("LVG_DIR", currentDirectory + "/");
		lvg = new LvgApi(properties);
	}

	public void cleanUp() {
		lvg.CleanUp();
	}

	public static Vector readFile(String filename)
	{
		Vector v = new Vector();
		try {
			BufferedReader in = new BufferedReader(
			   new InputStreamReader(
						  new FileInputStream(filename), "UTF8"));
			String str;
			while ((str = in.readLine()) != null) {
				v.add(str);
			}
            in.close();
		} catch (Exception ex) {
            ex.printStackTrace();
		}
		return v;
	}

	public Vector run(String inputfile) {
	    Vector v = readFile(inputfile);
	    Vector w = runLVG(v);
	    return w;
	}

	// private methods
	public Vector<LexItem> Mutate(LexItem in, LvgApi lvg) {
		Vector<LexItem> outs = new Vector<LexItem>();
		Vector<LexItem> out1 = ToLowerCase.Mutate(in, false, false);
		for(int i = 0 ; i < out1.size(); i++)
		{
			LexItem temp = out1.elementAt(i);
			LexItem tempIn = LexItem.TargetToSource(temp);
			Vector<LexItem> out2 = ToInflection.Mutate(tempIn,
				lvg.GetConnection(), lvg.GetInflectionTrie(),
				OutputFilter.LVG_OR_ALL, false, false);
			outs.addAll(out2);
		}
		return outs;
	}

	public void PrintResult(Vector<LexItem> result)
	{
		for(int i = 0; i < result.size(); i++)
		{
			LexItem temp = result.elementAt(i);
			System.out.println(temp.GetOriginalTerm() + "|"
				+ temp.GetSourceTerm() + "|"
				+ temp.GetSourceCategory().GetValue() + "|"
				+ temp.GetSourceInflection().GetValue() + "|"
				+ temp.GetTargetTerm() + "|"
				+ temp.GetTargetCategory().GetValue() + "|"
				+ temp.GetTargetInflection().GetValue() + "|"
				+ temp.GetMutateInformation());
		}
	}

    private Vector getGetTargetTerms(Vector<LexItem> result) {
		HashSet hset = new HashSet();
		Vector w = new Vector();
		for(int i = 0; i < result.size(); i++) {
			LexItem temp = result.elementAt(i);
			if (!hset.contains(temp.GetTargetTerm())) {
                w.add(temp.GetTargetTerm());
                hset.add(temp.GetTargetTerm());
			}
		}
		return new SortUtils().quickSort(w);
	}


    public HashMap createVariantHashMap(Vector v) {
		Vector w = runLVG(v);
		HashMap hmap = new HashMap();
		for (int i=0; i<w.size(); i++) {
			String t = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(t, '|');
			String key = (String) u.elementAt(0);
			String values = (String) u.elementAt(1);
			Vector v2 = StringUtils.parseData(values, '$');
			hmap.put(key, v2);
		}
		return hmap;
	}

    private String result2DelimetedString(Vector<LexItem> result) {
		Vector v = getGetTargetTerms(result);
		StringBuffer buf = new StringBuffer();
		HashSet hset = new HashSet();
		String term = null;
		for(int i = 0; i < v.size(); i++)
		{
            String s = (String) v.elementAt(i);
			buf.append(s).append("$");
		}
		String t = buf.toString();
		t = t.substring(0, t.length()-1);
		return result.elementAt(0).GetOriginalTerm() + "|" + t;
	}

	public Vector runLVG(Vector v) {
		Vector w = new Vector();
		for (int k=0; k<v.size(); k++) {
			LexItem in = new LexItem((String) v.elementAt(k));
			Vector<LexItem> result = Mutate(in, lvg);
			String t = result2DelimetedString(result);
            w.add(t);
		} return w;
	}

	public Vector tokenize(String t) {
		Vector w = new Vector();
		StringTokenizer st = new StringTokenizer("this is a test");
		while (st.hasMoreTokens()) {
		    w.add(st.nextToken());
		}
		return w;
	}

	 public static void saveToFile(String outputfile, String t) {
		 Vector v = new Vector();
		 v.add(t);
		 saveToFile(outputfile, v);
	 }

	 public static void saveToFile(String outputfile, Vector v) {
		if (outputfile == null) return;
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(outputfile, "UTF-8");
			if (pw != null) {
				if (v != null && v.size() > 0) {
					for (int i=0; i<v.size(); i++) {
						String t = (String) v.elementAt(i);
						pw.println(t);
					}
				}
			}
		} catch (Exception ex) {

		} finally {
			try {
				if (pw != null) pw.close();
				System.out.println("Output file " + outputfile + " generated.");
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

}
