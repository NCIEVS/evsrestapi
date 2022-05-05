package gov.nih.nci.evs.restapi.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class JSONParser {

    private JSONParser() { }

	public static Map<String, Object> parse(String json) {
		Map<String, Object> map = new HashMap<String, Object>();

		try {
			JSONObject jsonObject = new JSONObject(json);
			for (Iterator iter = jsonObject.keys(); iter.hasNext();) {
				String key = (String) iter.next();
				Object value = jsonObject.get(key);
				if (value instanceof JSONObject) {
					map.put(key, convertToMap((JSONObject) value));
				}
				else if (value instanceof JSONArray) {
					map.put(key, convertToList((JSONArray) value));
				}
				else {
					map.put(key, value);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return map;
	}

	private static Map<String, Object> convertToMap(JSONObject jsonObject) {
		Map<String, Object> map = new HashMap<String, Object>();
		try {
			for (Iterator iter = jsonObject.keys(); iter.hasNext();) {
				String key = (String) iter.next();
				Object value = jsonObject.get(key);

				if (value instanceof JSONObject) {
					map.put(key, convertToMap((JSONObject) value));
				}
				else if (value instanceof JSONArray) {
					map.put(key, convertToList((JSONArray) value));
				}
				else {
					map.put(key, value);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return map;
	}

	private static List convertToList(JSONArray jsonArray) {
		List<Object> list = new ArrayList<Object>();
		try {
			for (int i = 0; i < jsonArray.length(); i++) {
				Object value = jsonArray.get(i);
				if (value instanceof JSONObject) {
					list.add(convertToMap((JSONObject) value));
				}
				else if (value instanceof JSONArray) {
					list.add(convertToList((JSONArray) value));
				}
				else {
					list.add(value);
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return list;
	}


	public static void explore(String json) {
		System.out.println(json);
		try {
			Map map = JSONParser.parse(json);
			System.out.println(map.toString());

			Iterator it = map.keySet().iterator();
			while (it.hasNext()) {
				String key = (String) it.next();
				Object obj = map.get(key);
				if (obj instanceof List) {
					List list = (List) map.get(key);
					if (list != null && list.size() > 0) {
						Object obj1 = list.get(0);
						if (obj1 instanceof String) {
							for (int i=0; i<list.size(); i++) {
								String t = (String) list.get(i);
								System.out.println("\t" + key + ": " +  t);
							}
						}
					}

				} else if (obj instanceof Map) {
					Map map_obj = (Map) map.get(key);
					if (map_obj != null && map_obj.keySet().size() > 0) {
						Iterator it2 = map_obj.keySet().iterator();
						Object map_obj_key1 = it2.next();
						Object map_obj_value1 = map_obj.get(map_obj_key1);
						if (map_obj_value1 instanceof String) {
							Iterator it3 = map_obj.keySet().iterator();
							while (it3.hasNext()) {
								String key3 = (String) it3.next();
								Object obj1 = map_obj.get(key3);
								if (obj1 instanceof org.json.JSONObject) {
									System.out.println("\t" + key3 + ": " + obj1.toString());
								} else if (obj1 instanceof String) {
									System.out.println("\t" + key3 + ": " + obj1);
								}
  						    }
						}
					} else {
						System.out.println("KEY: " + key + " value: Map");
					}
				} else {
					System.out.println("\t" + key + ": " + obj.toString());
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static Vector run(String json) {
		boolean sort = true;
		return run(json, sort);
	}


	public static Vector run(String json, boolean sort) {
		Vector v = new Vector();
		try {
			Map map = parse(json);
			Iterator it = map.keySet().iterator();
			while (it.hasNext()) {
				String key = (String) it.next();
				Object obj = map.get(key);
				if (obj instanceof List) {
					List list = (List) map.get(key);
					if (list != null && list.size() > 0) {
						Object obj1 = list.get(0);
						if (obj1 instanceof String) {
							for (int i=0; i<list.size(); i++) {
								String t = (String) list.get(i);
								v.add(key + ": " +  t);
							}
						} else {
							for (int i=0; i<list.size(); i++) {
								Object t = list.get(i);
								v.add(key + ": " +  t.toString());
							}
						}
					}

				} else if (obj instanceof Map) {
					Map map_obj = (Map) map.get(key);
					if (map_obj != null && map_obj.keySet().size() > 0) {
						Iterator it2 = map_obj.keySet().iterator();
						Object map_obj_key1 = it2.next();
						Object map_obj_value1 = map_obj.get(map_obj_key1);
						if (map_obj_value1 instanceof String) {
							Iterator it3 = map_obj.keySet().iterator();
							while (it3.hasNext()) {
								String key3 = (String) it3.next();

								Object obj1 = map_obj.get(key3);
								if (obj1 instanceof org.json.JSONObject) {
									String value = obj1.toString();
									v.add(key3 + ": " + value);
								} else if (obj1 instanceof String) {
									v.add(key3 + ": " + obj1);
								}

							}
						} else {
							Iterator it3 = map_obj.keySet().iterator();
							while (it3.hasNext()) {
								String key3 = (String) it3.next();
								Object value = map_obj.get(key3);
								v.add(key3 + ": " + value.toString());
							}
						}
					}
				} else {
					v.add(key + ": " + obj.toString());
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		if (sort) {
			return new SortUtils().quickSort(v);
		}
		return v;
	}

    public static String flatten(Vector w) {
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			buf.append(line);
		}
		return buf.toString();
	}

	public static boolean isBoolean(String var) {
		if (var.length() < 3) return false;
		if (var.startsWith("is")) {
			String thirdChar = "" + var.charAt(2);
			String thirdCharUpper = thirdChar.toUpperCase();
			if (thirdChar.compareTo(thirdCharUpper) == 0) {
				return true;
			}
		}
		return false;
	}

	public static Vector getPOJOSpec(Vector w) {
		Vector v = new Vector();
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, ':');
			String var = (String) u.elementAt(0);
			var = var.trim();

			String className = getClassName(var);
			if (!v.contains(className)) {
				String value = (String) u.elementAt(1);
				value = value.trim();
				if (value.startsWith("{") && value.endsWith("}")) {
					v.addAll(getPOJOSpec(line));
				}
			}
		}
		return v;
	}

	public static Vector getPOJOSpec(String json) {
		Vector w = new Vector();
		Vector u = StringUtils.parseData(json, ':');
		String var = (String) u.elementAt(0);
		String value = (String) u.elementAt(1);
		var = var.trim();
		String className = getClassName(var);
		//w.add("\n");
		w.add(className);
		value = value.trim();
		if (value.startsWith("{") && value.endsWith("}")) {
			 value = " " + value.substring(1, value.length()-1);
			 int n = value.indexOf("=");
			 while (n != -1) {
				 String t = value.substring(1, n);
				 int m = t.lastIndexOf(" ");
				 String s = t.substring(m+1, t.length());
				 w.add("String|" + s);
				 value = value.substring(n+1, value.length());
				 n = value.indexOf("=");
			 }
		}
		return w;
	}

	public static String getClassName(String var) {
		if (var.endsWith("ies")) {
			var = var.replace("ies", "y");
			String firstChar = "" + var.charAt(0);
			firstChar = firstChar.toUpperCase();
			return firstChar + var.substring(1, var.length());
		} else if (var.endsWith("s")) {
			var = var.substring(0, var.length()-1);
			String firstChar = "" + var.charAt(0);
			firstChar = firstChar.toUpperCase();
			return firstChar + var.substring(1, var.length());
		}

		return var;
	}

	public static Vector parsedJSON2POJO(Vector w,  Vector complexVars) {
		Vector v = new Vector();
		v.add("gov.nih.nci.evs.restapi.bean");
		v.add("Main");
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, ':');
			String var = (String) u.elementAt(0);
			var = var.trim();
			if (complexVars.contains(var) || (var.endsWith("s")||var.endsWith("es"))) {
				String className = getClassName(var);
				String s = "List<" + className + ">|" + var;
				if (!v.contains(s)) v.add(s);
			} else {
				boolean isBool = isBoolean(var);
				String s = "String|" + var;
				if (isBool) {
					s = "boolean|" + var;
				}
				if (!v.contains(s)) v.add(s);
			}
		}
		v.addAll(getPOJOSpec(w));
		return v;
	}

	public static Vector identifyComplexVariables(Vector w) {
		Vector v = new Vector();
		for (int i=0; i<w.size(); i++) {
			String line = (String) w.elementAt(i);
			Vector u = StringUtils.parseData(line, ':');
			String var = (String) u.elementAt(0);
			var = var.trim();
			String value = (String) u.elementAt(1);
			value = value.trim();
			if (value.startsWith("{") && value.endsWith("}")) {
				if (!v.contains(var)) v.add(var);
			}
		}
		return v;
	}

	public static String createPOJODatafile(Vector w) {
		String json = flatten(w);
		JSONParser.explore(json);
		boolean sort = false;
		Vector v = JSONParser.run(json, sort);
		Vector w1 = identifyComplexVariables(v);
		w1 = parsedJSON2POJO(v, w1);
		String outputifle = "datafile.txt";
		Utils.saveToFile(outputifle, w1);
		return outputifle;
	}
}