package com.nickstephen.opensnap.util.misc;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;

/**
 * A custom implementation of a JSON parser. No claims are made about its general
 * correctness or speed. But it works for me :P
 * @author Nick Stephen (a.k.a. saltisgood)
 *
 */
public class CustomJSON {
	/**
	 * The root of the JSON file. Shouldn't be directly accessed by any other classes
	 */
	private JSONNode mRootNode;
	/**
	 * The original JSON string used to create the instance. Could be removed later
	 * if it proves worthless.
	 */
	private String mJson;
	
	/**
	 * Create a new CustomJSON instance by passing in a string in a json format.
	 * Can also throw a NullPointerException on a null string parameter.
	 * @param json The JSON string to be parsed
	 * @throws JSONException Thrown because of an invalid json string
	 */
	public CustomJSON(String json) throws JSONException{
		if (json == null)
			throw new NullPointerException("The parameter is null");
		mJson = json;
		mRootNode = new JSONNode(json);
	}
	
	/**
	 * Check if a key exists in the JSON
	 * @param key The key to check for
	 * @return True if the key does exist, false if it doesn't
	 */
	public Boolean CheckKeyExists(String key) {
		return mRootNode.KeyExists(key);
	}
	
	/**
	 * Get the type of a value from inside the JSON given a valid key.
	 * NOTE: Check the existence of the key with {@link #CheckKeyExists(String)} first, 
	 * otherwise the behaviour is undefined.
	 * @param key The key to get the type for
	 * @return A string representation of the type of the value
	 */
	public String GetType(String key) {
		return mRootNode.GetType(key);
	}
	
	/**
	 * Get the value of something from the JSON given a valid key.
	 * NOTE: Check the existence of the key with {@link #CheckKeyExists(String)} first,
	 * otherwise the behaviour is undefined.
	 * NOTE 2: This function returns a generic object type which should then be properly
	 * cast to a particular type. Call {@link #GetType(String)} to check which type to use.
	 * @param key The key to get the value for
	 * @return The value cast to a generic class
	 */
	public Object GetValue(String key) {
		return mRootNode.GetValue(key);
	}
	
	/**
	 * Serialise the original JSON string to a byte array. Useful if you
	 * want to write to file or whatever.
	 * @return The byte array value of the JSON
	 */
	public byte[] Serialise() {
		return mJson.getBytes();
	}
	
	/**
	 * The JSONNode class used to store the values of the JSON object. Can
	 * contain recursive definitions of itself for nested JSONs.
	 * @author Nick's Laptop
	 *
	 */
	public class JSONNode {
		/**
		 * The mapping of keys to types. Used to keep track of what each key-value
		 * pair actually is.
		 */
		private Map<String, Type> Vals;
		/**
		 * The list of keys present in the JSON
		 */
		private List<String> Keys;
		/**
		 * The mapping of keys to objects. Since they are stored as generics the
		 * {@link #Vals} object is necessary for keeping track of their types.
		 */
		private Map<String, Object> Values;
		/**
		 * The final mapping of keys to sub-types of list type values
		 */
		private Map<String, Type> ListTypes;
		
		/**
		 * Construct a JSONNode from a string representation
		 * @param json The string representation of the JSONNode
		 * @throws JSONException Throw by incorrect starting/ ending chars
		 */
		public JSONNode(String json) throws JSONException {
			if (json == null)
				throw new NullPointerException("The parameter is null");
			json = json.trim();
			if (!json.startsWith("{"))
				throw new JSONException("Incorrect json starting char");
			else if (!json.endsWith("}"))
				throw new JSONException("Incorrect json ending char");
			
			Vals = new HashMap<String, Type>();
			Keys = new ArrayList<String>();
			ListTypes = new HashMap<String, Type>();
			Values = new HashMap<String, Object>();
			
			json = json.substring(1, json.length() - 1);
			InterpretJSON(json);
		}
		
		/**
		 * Only called from the constructor to initialise the JSONNode.
		 * This function actually reads the JSON string
		 * @param json The string to read minus the starting and ending braces
		 * @throws JSONException Thrown by invalid characters
		 */
		private void InterpretJSON(String json) throws JSONException {
			int index = 0;
			while (index < json.length()) { // Read until end
				char c = json.charAt(index);
				if (c <= 32 || c > 126 || c == ',') {
					// Skip whitespace characters. Shouldn't happen for SnapChat at least
					if (c == '\t' || c == '\r' || c == '\n' || c == ' ' || c == ','){
						index++;
						continue;
					}
					// Other non-printing characters throw the exception
					throw new JSONException("Invalid character value");
				}
				if (c == '"') { // Should start with a " to indicate a string value of a key
					int tempint = json.indexOf('"', index + 1);
					String key = json.substring(index + 1, tempint); // Read until the next "
					Keys.add(key);
					index = ++tempint;
					if ((c = json.charAt(index++)) != ':') // Slightly hacky. If there was whitespace this would cause issues
						throw new JSONException("Invalid character value");
					c = json.charAt(index++);
					// Switch on the next character
					if (c == '"'){ // A string value
						Vals.put(key, String.class);
						tempint = json.indexOf('"', index);
						String val = json.substring(index, tempint);
						Values.put(key, val);
						index = ++tempint;
					} else if ((c >= '0' && c <= '9') || c == '-'){ // A numerical value (float)
						Vals.put(key, Float.class);
						tempint = json.indexOf(',', index);
						if (tempint > json.indexOf('}', index) && json.indexOf('}', index) != -1)
							tempint = json.indexOf('}', index);
						if (tempint == -1)
							tempint = json.length();
						String val = json.substring(index - 1, tempint);
						Float val2 = Float.valueOf(val);
						Values.put(key, val2);
						index = ++tempint;
					} else if (c == '[') { // A list of values
						Vals.put(key, List.class);
						c = json.charAt(index++);
						// Switch on the list type
						if (c == '"' || c == ']') { // Either a string type or an empty array
							List<String> strs = new ArrayList<String>();
							ListTypes.put(key, String.class);
							while (c != ']') { // Read until end of array
								tempint = json.indexOf('"', index + 1);
								String val = json.substring(index, tempint);
								if (val.startsWith("\"")) {
									val = val.substring(1);
								}
								strs.add(val);
								index = ++tempint;
								c = json.charAt(index++);
								if (c != ',' && c != ']')
									throw new JSONException("Invalid character value");
							}
							Values.put(key, strs);
						} else if (c == '{') { // List of sub-nodes
							ListTypes.put(key, CustomJSON.JSONNode.class);
							List<JSONNode> nodes = new ArrayList<JSONNode>();
							while (c != ']') { // Read until end of list, making sure there are no nested arrays
								tempint = index;
								int depth = 0;
								while (true) {
									c = json.charAt(tempint);
									if (c == '{')
										depth++;
									else if (c == '}' && depth == 0)
										break;
									else if (c == '}')
										depth--;
									tempint++;
								}
								tempint++;
								String subnodestr = json.substring(index - 1, tempint);
								nodes.add(new JSONNode(subnodestr));
								c = json.charAt(tempint++);
								if (c != ',' && c != ']')
									throw new JSONException("Invalid character value");
								if (c == ',' && (c = json.charAt(tempint++)) != '{')
									throw new JSONException("Invalid character value");
								
								index = tempint;
							}
							Values.put(key, nodes);
						} else {
							throw new JSONException("Invalid character value");
						}
					} else if (c == 'f') { // False boolean value
						String val = json.substring(index - 1, index + 4);
						if (val.compareTo("false") != 0)
							throw new JSONException("Invalid character value");
						Vals.put(key, Boolean.class);
						Values.put(key, false);
						index += 4;
					} else if (c == 't') { // True boolean value
						String val = json.substring(index - 1, index + 3);
						if (val.compareTo("true") != 0)
							throw new JSONException("Invalid character value");
						Vals.put(key, Boolean.class);
						Values.put(key, true);
						index += 3;
					} else if (c == '{') { // Sub-node. Only used for an empty node for now
                        tempint = json.indexOf('}', index);
                        if (tempint == index) {
                            Vals.put(key, Void.class);
                            Values.put(key, null);
                            //index = subIndex + 1;
                            index++;
                        } else {
                            throw new JSONException("Not implemented yet");
                        }
                    } else {
						throw new JSONException("Invalid character value");
					}
				} else {
					throw new JSONException("Invalid character value");
				}
			}
		}
		
		/**
		 * Check whether a given key exists in the JSONNode
		 * @param key The key to check for
		 * @return True if the key exists, false if it does not
		 */
		public Boolean KeyExists(String key) {
			for (int i = 0; i < Keys.size(); i++) {
				if (key.compareTo(Keys.get(i)) == 0)
					return true;
			}
			return false;
		}
		
		/**
		 * Get the type of the value of a particular key-value pair.
		 * NOTE: Call {@link #KeyExists(String)} with the key to check for its
		 * existence first, otherwise the behaviour is undefined.
		 * @param key The key to use
		 * @return A string representation of the type
		 */
		public String GetType(String key) {
			Type val = Vals.get(key);
			if (val.equals(String.class))
				return "String";
			else if (val.equals(Boolean.class))
				return "Boolean";
			else if (val.equals(Float.class))
				return "Float";
			else if (val.equals(List.class)) {
				Type subval = ListTypes.get(key);
				if (subval.equals(String.class))
					return "List<String>";
				else if (subval.equals(Float.class))
					return "List<Float>";
				else if (subval.equals(Boolean.class))
					return "List<Boolean>";
				else if (subval.equals(CustomJSON.JSONNode.class))
					return "List<JSONNode>";
				else
					return null;
			}
			else
				return null;
		}
		
		/**
		 * Get the value of a particular key-value pair.
		 * NOTE: Call {@link #KeyExists(String)} with the key to check for its
		 * existence first, otherwise the behaviour is undefined. 
		 * NOTE 2: Call {@link #GetType(String)} with the key to get the type
		 * of the value so you know what to cast it as.
		 * @param key The key to use
		 * @return The value as a generic type
		 */
		public Object GetValue(String key) {
			return Values.get(key);
		}
		
		/**
		 * Get a list of the keys in the JSONNode
		 * @return The list of keys
		 */
		public List<String> GetKeys() {
			return new ArrayList<String>(Keys);
		}
	}
}
