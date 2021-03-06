
package gov.nih.nci.evs.api.util.ext;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * The Class Utils.
 */
@SuppressWarnings({
    "unchecked", "rawtypes"
})
public class Utils {

  /**
   * Dump hash map.
   *
   * @param label the label
   * @param hmap the hmap
   */
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
    for (int i = 0; i < key_vec.size(); i++) {
      String key = (String) key_vec.elementAt(i);
      String value = (String) (String) hmap.get(key);
      System.out.println(key + " --> " + value);
    }
    System.out.println("\n");
  }

  /**
   * Dump multi valued hash map.
   *
   * @param label the label
   * @param hmap the hmap
   */
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
    for (int k = 0; k < key_vec.size(); k++) {
      String nv = (String) key_vec.elementAt(k);
      System.out.println("\n");
      Vector v = (Vector) hmap.get(nv);
      for (int i = 0; i < v.size(); i++) {
        String q = (String) v.elementAt(i);
        System.out.println(nv + " --> " + q);
      }
    }
    System.out.println("\n");
  }

  /**
   * Dump vector.
   *
   * @param label the label
   * @param v the v
   */
  public static void dumpVector(String label, Vector v) {
    System.out.println("\n" + label + ":");
    if (v == null)
      return;
    if (v.size() == 0) {
      System.out.println("\tNone");
      return;
    }
    for (int i = 0; i < v.size(); i++) {
      String t = (String) v.elementAt(i);
      int j = i + 1;
      System.out.println("\t(" + j + ") " + t);
    }
    System.out.println("\n");
  }

  /**
   * Dump vector.
   *
   * @param label the label
   * @param v the v
   * @param display_label the display label
   * @param display_index the display index
   */
  public static void dumpVector(String label, Vector v, boolean display_label,
    boolean display_index) {
    if (display_label) {
      System.out.println("\n" + label + ":");
    }
    if (v == null || v.size() == 0) {
      System.out.println("\tNone");
      return;
    }
    for (int i = 0; i < v.size(); i++) {
      String t = (String) v.elementAt(i);
      int j = i + 1;
      if (display_index) {
        System.out.println("\t(" + j + ") " + t);
      } else {
        System.out.println("\t" + t);
      }
    }
    System.out.println("\n");
  }

  /**
   * Dump array list.
   *
   * @param label the label
   * @param list the list
   */
  public static void dumpArrayList(String label, ArrayList list) {
    System.out.println("\n" + label + ":");
    if (list == null || list.size() == 0) {
      System.out.println("\tNone");
      return;
    }
    for (int i = 0; i < list.size(); i++) {
      String t = (String) list.get(i);
      int j = i + 1;
      System.out.println("\t(" + j + ") " + t);
    }
    System.out.println("\n");
  }

  /**
   * Dump list.
   *
   * @param label the label
   * @param list the list
   */
  public static void dumpList(String label, List list) {
    System.out.println("\n" + label + ":");
    if (list == null || list.size() == 0) {
      System.out.println("\tNone");
      return;
    }
    for (int i = 0; i < list.size(); i++) {
      String t = (String) list.get(i);
      int j = i + 1;
      System.out.println("\t(" + j + ") " + t);
    }
    System.out.println("\n");
  }

  /**
   * Replace filename.
   *
   * @param filename the filename
   * @return the string
   */
  public static String replaceFilename(String filename) {
    return filename.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
  }

  /**
   * Find files in directory.
   *
   * @param directory the directory
   * @return the list
   */
  public static List<String> findFilesInDirectory(String directory) {
    return findFilesInDirectory(new File(directory));
  }

  /**
   * Find files in directory.
   *
   * @param dir the dir
   * @return the list
   */
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

  /**
   * Save to file.
   *
   * @param outputfile the outputfile
   * @param t the t
   */
  public static void saveToFile(String outputfile, String t) {
    Vector v = new Vector();
    v.add(t);
    saveToFile(outputfile, v);
  }

  /**
   * Save to file.
   *
   * @param outputfile the outputfile
   * @param v the v
   */
  public static void saveToFile(String outputfile, Vector v) {
    if (outputfile.indexOf(" ") != -1) {
      outputfile = replaceFilename(outputfile);
    }
    PrintWriter pw = null;
    try {
      pw = new PrintWriter(outputfile, "UTF-8");
      if (v != null && v.size() > 0) {
        for (int i = 0; i < v.size(); i++) {
          String t = (String) v.elementAt(i);
          pw.println(t);
        }
      }
    } catch (Exception ex) {

    } finally {
      try {
        pw.close();
        System.out.println("Output file " + outputfile + " generated.");
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }

  /**
   * Save to file.
   *
   * @param pw the pw
   * @param v the v
   */
  public static void saveToFile(PrintWriter pw, Vector v) {
    if (v != null && v.size() > 0) {
      for (int i = 0; i < v.size(); i++) {
        String t = (String) v.elementAt(i);
        pw.println(t);
      }
    }
  }

  /**
   * Read file.
   *
   * @param filename the filename
   * @return the vector
   */
  public static Vector readFile(String filename) {
    Vector v = new Vector();
    try {
      BufferedReader in =
          new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF8"));
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

  /**
   * Parses the data.
   *
   * @param line the line
   * @param delimiter the delimiter
   * @return the vector
   */
  public static Vector parseData(String line, char delimiter) {
    if (line == null)
      return null;
    Vector w = new Vector();
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < line.length(); i++) {
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

  /**
   * Creates the static variable.
   *
   * @param v the v
   * @param variableName the variable name
   * @return the string
   */
  public static String createStaticVariable(Vector v, String variableName) {
    StringBuffer buf = new StringBuffer();
    buf.append("public static final String[] " + variableName + " = new String[] {").append("\n");
    for (int i = 0; i < v.size(); i++) {
      int j = i + 1;
      String t = (String) v.elementAt(i);
      buf.append("\"" + t + "\", ");
      if (j % 10 == 0) {
        buf.append("\n");
      }
    }
    String s = buf.toString();
    s = s.trim();
    s = s.substring(0, s.length() - 1) + "};";
    return s;
  }

  /**
   * Vector 2 hash set.
   *
   * @param v the v
   * @return the hash set
   */
  public static HashSet vector2HashSet(Vector v) {
    if (v == null)
      return null;
    HashSet hset = new HashSet();
    for (int i = 0; i < v.size(); i++) {
      String t = (String) v.elementAt(i);
      hset.add(t);
    }
    return hset;
  }

  /**
   * Hash set 2 vector.
   *
   * @param hset the hset
   * @return the vector
   */
  public static Vector hashSet2Vector(HashSet hset) {
    if (hset == null)
      return null;
    Vector v = new Vector();
    Iterator it = hset.iterator();
    while (it.hasNext()) {
      String t = (String) it.next();
      v.add(t);
    }
    v = new SortUtils().quickSort(v);
    return v;
  }

  /**
   * Change file extension.
   *
   * @param filename the filename
   * @param ext the ext
   * @return the string
   */
  public static String changeFileExtension(String filename, String ext) {
    int n = filename.lastIndexOf(".");
    if (n != -1) {
      return filename.substring(0, n) + "." + ext;
    }
    return filename;
  }

  /**
   * Returns the inverse hash map.
   *
   * @param hmap the hmap
   * @return the inverse hash map
   */
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

  /**
   * List files.
   *
   * @param directory the directory
   * @return the vector
   */
  public static Vector listFiles(String directory) {
    Vector w = new Vector();
    Collection<File> c = listFileTree(new File(directory));
    Iterator it = c.iterator();
    while (it.hasNext()) {
      File t = (File) it.next();
      w.add(t.getName());
    }
    w = new SortUtils().quickSort(w);
    return w;
  }

  /**
   * List file tree.
   *
   * @param dir the dir
   * @return the collection
   */
  public static Collection<File> listFileTree(File dir) {
    Set<File> fileTree = new HashSet<File>();
    if (dir == null || dir.listFiles() == null) {
      return fileTree;
    }
    for (File entry : dir.listFiles()) {
      if (entry.isFile())
        fileTree.add(entry);
      else
        fileTree.addAll(listFileTree(entry));
    }
    return fileTree;
  }

  /**
   * Check if file exists.
   *
   * @param filename the filename
   * @return true, if successful
   */
  public static boolean checkIfFileExists(String filename) {
    String currentDir = System.getProperty("user.dir");
    File f = new File(currentDir + "\\" + filename);
    if (f.exists() && !f.isDirectory()) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Creates the relationship hash map.
   *
   * @param v the v
   * @return the hash map
   */
  public static HashMap createRelationshipHashMap(Vector v) {
    HashMap hmap = new HashMap();
    for (int i = 0; i < v.size(); i++) {
      String t = (String) v.elementAt(i);
      Vector u = StringUtils.parseData(t, '|');
      String src = (String) u.elementAt(0);
      String rel = (String) u.elementAt(1);
      String target = (String) u.elementAt(2);
      if (u.size() == 6) {
        src = (String) u.elementAt(1);
        rel = (String) u.elementAt(3);
        target = (String) u.elementAt(5);
      }
      HashMap sub_map = new HashMap();
      if (hmap.containsKey(src)) {
        sub_map = (HashMap) hmap.get(src);
      }
      Vector w = new Vector();
      if (sub_map.containsKey(rel)) {
        w = (Vector) sub_map.get(rel);
      }
      if (!w.contains(target)) {
        w.add(target);
      }
      sub_map.put(rel, w);
      hmap.put(src, sub_map);

    }
    return hmap;
  }

  /**
   * Creates the inverse relationship hash map.
   *
   * @param v the v
   * @return the hash map
   */
  public static HashMap createInverseRelationshipHashMap(Vector v) {
    HashMap hmap = new HashMap();
    for (int i = 0; i < v.size(); i++) {
      String t = (String) v.elementAt(i);
      Vector u = StringUtils.parseData(t, '|');
      String src = (String) u.elementAt(0);
      String rel = (String) u.elementAt(1);
      String target = (String) u.elementAt(2);
      if (u.size() == 6) {
        src = (String) u.elementAt(1);
        rel = (String) u.elementAt(3);
        target = (String) u.elementAt(5);
      }
      HashMap sub_map = new HashMap();
      if (hmap.containsKey(target)) {
        sub_map = (HashMap) hmap.get(target);
      }
      Vector w = new Vector();
      if (sub_map.containsKey(rel)) {
        w = (Vector) sub_map.get(rel);
      }
      if (!w.contains(src)) {
        w.add(src);
      }
      sub_map.put(rel, w);
      hmap.put(target, sub_map);
    }
    return hmap;
  }

  /**
   * Generate construct statement.
   *
   * @param method_name the method name
   * @param params the params
   * @param filename the filename
   */
  public static void generate_construct_statement(String method_name, String params,
    String filename) {
    Vector w = create_construct_statement(method_name, params, filename);
    StringUtils.dumpVector(w);
  }

  /**
   * Creates the construct statement.
   *
   * @param method_name the method name
   * @param params the params
   * @param filename the filename
   * @return the vector
   */
  public static Vector create_construct_statement(String method_name, String params,
    String filename) {
    Vector u = Utils.readFile(filename);
    Vector w = new Vector();
    w.add("public String " + method_name + "(" + params + ") {");
    // w.add("\tString prefixes = getPrefixes();");
    w.add("\tStringBuffer buf = new StringBuffer();");
    // w.add("\tbuf.append(prefixes);");
    for (int i = 0; i < u.size(); i++) {
      String t = (String) u.elementAt(i);
      t = StringUtils.trimLeadingBlanksOrTabs(t);
      if (!t.startsWith("#")) {
        t = StringUtils.escapeDoubleQuotes(t);
        t = "\"" + t + "\"";
        t = "buf.append(" + t + ").append(\"\\n\");";
        w.add("\t" + t);
      }
    }
    w.add("\treturn buf.toString();");
    w.add("}");
    return w;
  }

  /**
   * Delete file.
   *
   * @param filename the filename
   * @return true, if successful
   */
  public static boolean deleteFile(String filename) {
    File file = new File(filename);
    if (file.delete()) {
      return true;
    } else {
      return false;
    }
  }

  // public static Table constructTable(String label, Vector heading_vec, Vector
  // data_vec) {
  // return Table.construct_table(label, heading_vec, data_vec);
  // }

}
