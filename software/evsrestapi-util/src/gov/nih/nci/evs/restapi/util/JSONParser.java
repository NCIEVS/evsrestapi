package gov.nih.nci.evs.restapi.util;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
							System.out.println("KEY: " + key + " VALUE: List  OBJECT: String");
							for (int i=0; i<list.size(); i++) {
								String t = (String) list.get(i);
								System.out.println("\t" + t);
							}
						}
					} else {
						System.out.println("KEY: " + key + " VALUE: List");
					}

				} else if (obj instanceof Map) {
					Map map_obj = (Map) map.get(key);
					if (map_obj != null && map_obj.keySet().size() > 0) {
						Iterator it2 = map_obj.keySet().iterator();
						Object map_obj_key1 = it2.next();
						Object map_obj_value1 = map_obj.get(map_obj_key1);
						if (map_obj_value1 instanceof String) {
							System.out.println("KEY: " + key + " VALUE: Map  OBJECT: String");
							Iterator it3 = map_obj.keySet().iterator();
							while (it3.hasNext()) {
								String key3 = (String) it3.next();
								String value = (String) map_obj.get(key3);
								System.out.println("\t" + key3 + " --> " + value);
							}
						}
					} else {
						System.out.println("KEY: " + key + " value: Map");
					}
				} else {
					System.out.println("KEY: " + key + " VALUE: String");
					System.out.println("\t" + (String) obj);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}