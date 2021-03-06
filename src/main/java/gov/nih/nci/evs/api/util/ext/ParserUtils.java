package gov.nih.nci.evs.api.util.ext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import gov.nih.nci.evs.api.model.Concept;
import gov.nih.nci.evs.api.model.Path;
import gov.nih.nci.evs.api.model.Paths;

/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright 2008-2017 NGIS. This software was developed in conjunction
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
 *      "This product includes software developed by NGIS and the National
 *      Cancer Institute."   If no such end-user documentation is to be
 *      included, this acknowledgment shall appear in the software itself,
 *      wherever such third-party acknowledgments normally appear.
 *   3. The names "The National Cancer Institute", "NCI" and "NGIS" must
 *      not be used to endorse or promote products derived from this software.
 *   4. This license does not authorize the incorporation of this software
 *      into any third party proprietary programs. This license does not
 *      authorize the recipient to use any trademarks owned by either NCI
 *      or NGIS
 *   5. THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESSED OR IMPLIED
 *      WARRANTIES, (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *      OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE) ARE
 *      DISCLAIMED. IN NO EVENT SHALL THE NATIONAL CANCER INSTITUTE,
 *      NGIS, OR THEIR AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT,
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
 *          Modification history: Initial implementation kim.ong@ngc.com
 *
 */
@SuppressWarnings({
    "unchecked", "rawtypes"
})

public class ParserUtils {
  public ParserUtils() {

  }

  public Vector getValues(Vector v, int k) {
    if (v == null)
      return null;
    Vector w = new Vector();
    for (int i = 0; i < v.size(); i++) {
      String line = (String) v.elementAt(i);
      Vector u = StringUtils.parseData(line, '|');
      String value = (String) u.elementAt(k);
      w.add(value);
    }
    return w;
  }

  public String getValue(String t) {
    if (t == null)
      return null;
    Vector v = StringUtils.parseData(t, '|');
    if (v.size() == 0)
      return null;
    return (String) v.elementAt(v.size() - 1);
  }

  public String parseLabel(Vector v) {
    if (v == null)
      return null;
    String t = (String) v.elementAt(0);
    return getValue(t);
  }

  public HashMap parseSuperclasses(Vector v) {
    if (v == null)
      return null;
    return parse(v, 2, 1, 0);
  }

  public HashMap parseSubclasses(Vector v) {
    if (v == null)
      return null;
    return parse(v, 2, 1, 0);
  }

  public HashMap parseProperties(Vector v) {
    if (v == null)
      return null;
    return parse(v, 2, 0, 1);
  }

  public HashMap parse(Vector v, int m, int d1, int d2) {
    if (v == null)
      return null;
    HashMap hmap = new HashMap();
    if (m == 0)
      return hmap;
    int n = v.size() / m;
    for (int i = 0; i < n; i++) {
      int i0 = i * m;
      int i1 = i0 + d1;
      int i2 = i0 + d2;
      String t1 = (String) v.elementAt(i1);
      String t2 = (String) v.elementAt(i2);
      String key = getValue(t1);
      String value = getValue(t2);
      Vector u = new Vector();
      if (hmap.containsKey(key)) {
        u = (Vector) hmap.get(key);
      }
      if (!u.contains(value)) {
        u.add(value);
        u = new SortUtils().quickSort(u);
      }
      hmap.put(key, u);
    }
    return hmap;
  }

  public int findNumberOfVariables(Vector v) {
    if (v == null)
      return 0;
    HashSet hset = new HashSet();
    for (int i = 0; i < v.size(); i++) {
      String line = (String) v.elementAt(i);
      Vector u = StringUtils.parseData(line, '|');
      String var = (String) u.elementAt(0);
      if (!hset.contains(var)) {
        hset.add(var);
      }
    }
    return hset.size();
  }

  public Vector parse(Vector v) {
    if (v == null)
      return null;
    int m = findNumberOfVariables(v);
    return parse(v, m);
  }

  public Vector parse(Vector v, int m) {
    if (v == null)
      return null;
    Vector w = new Vector();
    int n = v.size() / m;
    for (int i = 0; i < n; i++) {
      StringBuffer buf = new StringBuffer();
      for (int j = 0; j < m; j++) {
        int i0 = i * m + j;
        String t = (String) v.elementAt(i0);
        t = getValue(t);
        buf.append(t);
        if (j < m - 1) {
          buf.append("|");
        }
      }
      String s = buf.toString();
      w.add(s);
    }
    return w;
  }

  public Vector sortAxiomData(Vector v) {
    if (v == null)
      return null;
    Vector w = new Vector();
    int n = v.size() / 6;
    Vector key_vec = new Vector();
    for (int i = 0; i < n; i++) {
      String t = (String) v.elementAt(i * 6);
      if (!key_vec.contains(t)) {
        key_vec.add(t);
      }
    }

    key_vec = new SortUtils().quickSort(key_vec);
    for (int i = 0; i < key_vec.size(); i++) {
      String key = (String) key_vec.elementAt(i);
      for (int j = 0; j < v.size(); j++) {
        String value = (String) v.elementAt(j);
        if (value.compareTo(key) == 0) {
          w.add(value);
          w.add((String) v.elementAt(j + 1));
          w.add((String) v.elementAt(j + 2));
          w.add((String) v.elementAt(j + 3));
          w.add((String) v.elementAt(j + 4));
          w.add((String) v.elementAt(j + 5));
        }
      }
    }
    return w;
  }

  public HashMap getCode2LabelHashMap(Vector v) {
    if (v == null)
      return null;
    HashMap hmap = new HashMap();
    int n = v.size() / 2;
    for (int i = 0; i < n; i++) {
      String label = getValue((String) v.elementAt(i * 2));
      String code = getValue((String) v.elementAt(i * 2 + 1));
      hmap.put(code, label);
    }
    return hmap;
  }

  public Vector toDelimited(Vector v, int m, char delim) {
    if (v == null)
      return null;
    Vector w = new Vector();
    int n = v.size() / m;
    for (int i = 0; i < n; i++) {
      StringBuffer buf = new StringBuffer();
      for (int k = 0; k < m; k++) {
        int j = i * m + k;
        buf.append(getValue((String) v.elementAt(j)));
        if (k < m - 1) {
          buf.append(delim);
        }
      }
      String line = buf.toString();
      w.add(line);
    }
    return w;
  }

  /*
   * public Path( int direction, List concepts) { this.direction = direction;
   * this.concepts = concepts; }
   */

  public Paths trimPaths(Paths paths, String code) {
    if (paths == null)
      return null;
    Paths new_paths = new Paths();
    List path_list = paths.getPaths();
    for (int i = 0; i < path_list.size(); i++) {
      Path path = (Path) path_list.get(i);
      int direction = path.getDirection();
      List concepts = path.getConcepts();

      int idx = -1;
      for (int j = 0; j < concepts.size(); j++) {
        Concept concept = (Concept) concepts.get(j);
        if (concept.getCode().compareTo(code) == 0) {
          idx = concept.getLevel();
        }
      }
      List list = new ArrayList();
      if (idx == -1) {
        idx = concepts.size() - 1;
      }
      for (int k = 0; k <= idx; k++) {
        Concept c = (Concept) concepts.get(k);
        list.add(c);
      }
      new_paths.add(new Path(direction, list));
    }
    return new_paths;
  }

  public Vector getResponseVariables(Vector v) {
    if (v == null)
      return null;
    Vector w = new Vector();
    for (int i = 0; i < v.size(); i++) {
      String line = (String) v.elementAt(i);
      Vector u = StringUtils.parseData(line, '|');
      String var = (String) u.elementAt(0);
      if (!w.contains(var)) {
        w.add(var);
      }
    }
    return w;
  }

  public String getVariableName(String line) {
    Vector u = StringUtils.parseData(line, '|');
    return (String) u.elementAt(0);
  }

  public Vector getResponseValues(Vector v) {
    if (v == null || v.size() == 0)
      return null;
    ParserUtils parser = new ParserUtils();
    Vector w = new Vector();
    Vector vars = getResponseVariables(v);
    String firstVar = (String) vars.elementAt(0);
    String[] values = new String[vars.size()];
    for (int i = 0; i < vars.size(); i++) {
      values[i] = null;
    }
    for (int i = 0; i < v.size(); i++) {
      String line = (String) v.elementAt(i);
      String var = getVariableName(line);
      if (var.compareTo(firstVar) == 0 && values[0] != null) {
        StringBuffer buf = new StringBuffer();
        for (int j = 0; j < vars.size(); j++) {
          String t = values[j];
          if (t == null) {
            t = "null";
          }
          buf.append(t);
          if (j < vars.size() - 1) {
            buf.append("|");
          }
        }
        String s = buf.toString();
        w.add(s);

        for (int k = 0; k < vars.size(); k++) {
          values[k] = null;
        }
      }
      String value = parser.getValue(line);
      for (int k = 0; k < vars.size(); k++) {
        if (var.compareTo((String) vars.elementAt(k)) == 0) {
          values[k] = value;
        }
      }

    }
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < vars.size(); i++) {
      String t = values[i];
      if (t == null) {
        t = "null";
      }
      buf.append(t);
      if (i < vars.size() - 1) {
        buf.append("|");
      }
    }
    String s = buf.toString();
    w.add(s);
    return w;
  }

  public Vector filterPropertyQualifiers(Vector v, String type) { // FULL_SYN,
                                                                  // DEFINITION,
                                                                  // ALT_DEFINITION
    if (v == null)
      return null;
    Vector w = new Vector();
    int n = v.size() / 9;
    Vector key_vec = new Vector();
    for (int i = 0; i < n; i++) {
      String t0 = (String) v.elementAt(i * 9);
      String t3 = (String) v.elementAt(i * 9 + 3);
      t3 = getValue(t3);
      if (t3.compareTo(type) == 0) {
        if (!key_vec.contains(t0)) {
          key_vec.add(t0);
        }
      }
    }

    key_vec = new SortUtils().quickSort(key_vec);
    for (int i = 0; i < key_vec.size(); i++) {
      String key = (String) key_vec.elementAt(i);
      for (int j = 0; j < v.size(); j++) {
        String value = (String) v.elementAt(j);
        if (value.compareTo(key) == 0) {
          w.add(value);
          w.add((String) v.elementAt(j + 1));
          w.add((String) v.elementAt(j + 2));
          w.add((String) v.elementAt(j + 5));
          w.add((String) v.elementAt(j + 6));
          w.add((String) v.elementAt(j + 8));
        }
      }
    }
    return w;
  }

  public String extractLabel(String line) {
    if (line == null)
      return null;
    int n = line.lastIndexOf("#");
    if (n == -1)
      return line;
    return line.substring(n + 1, line.length());
  }

  public static Vector formatOutput(Vector v) {
    if (v == null)
      return null;
    if (v.size() == 0)
      return new Vector();
    v = new ParserUtils().getResponseValues(v);
    v = new SortUtils().quickSort(v);
    return v;
  }

  // (10) Wireless Communication
  // Problem|http://www.w3.org/2000/01/rdf-schema#label|label|Wireless
  // Communication Problem

  public static Vector excludePropertyType(Vector v, String exclusion) {
    if (v == null)
      return null;
    Vector target_vec = StringUtils.parseData(exclusion, '|');
    Vector w = new Vector();
    for (int i = 0; i < v.size(); i++) {
      String t = (String) v.elementAt(i);
      Vector u = StringUtils.parseData(t, '|');
      String x = (String) u.elementAt(1);
      boolean retval = true;
      for (int k = 0; k < target_vec.size(); k++) {
        String target = (String) target_vec.elementAt(k);
        if (x.contains(target)) {
          retval = false;
          break;
        }
      }
      if (retval) {
        String s0 = (String) u.elementAt(0);
        String s2 = (String) u.elementAt(2);
        String s3 = (String) u.elementAt(3);
        String s = s0 + "|" + s2 + "|" + s3;
        w.add(s);
      }
    }
    return w;
  }

}
