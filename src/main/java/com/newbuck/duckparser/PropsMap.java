// Copyright (C) 2018-2019 Brandon Lee. BSD-3-Clause Licensed, see LICENSE.
package com.newbuck.duckparser;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import com.newbuck.duckparser.parse.TypeParser;

// internal props storage impl
class PropsMap extends LinkedHashMap<String, String> {
	// maps props to the location loaded from
	private Map<String, List<String>> propLocations = new HashMap<>();
	// maps name -> key in propLocations using xform function (depending on options)
	private Map<String, String> propNameToKeyMap = null;

	private final String prefix;

	// default gets from store, then sys, then env
	private final BiFunction<Map<String, String>, String, String> DEF_VAR_RESOLVER =
		(store, variable) -> {
			String val = store.get(variable);
			if (val == null) {
				val = System.getProperty(variable);
				if (val == null) {
					val = System.getenv(variable);
				}
			}
			return val;
		};
	// only resolve with store
	private final BiFunction<Map<String, String>, String, String> STORE_VAR_RESOLVER =
		(store, variable) -> store.get(variable);

	private final UnaryOperator<String> XFORM_LC = (key) -> key.toLowerCase();
	private final UnaryOperator<String> XFORM_IDENT = (key) -> key;

	private BiFunction<Map<String, String>, String, String> varResolver;
	private UnaryOperator<String> propNameToKeyMapper;

	public PropsMap(String prefix, boolean ignoreCase, boolean resolveVarsWithEnv) {
		this.prefix = prefix;
		this.propNameToKeyMapper = ignoreCase ? XFORM_LC : XFORM_IDENT;

		varResolver = resolveVarsWithEnv ? DEF_VAR_RESOLVER : STORE_VAR_RESOLVER;
	}

	// the op thats run if a variable couldn't be found in the loaded properties map
	// default resolves from sys then env
	public void setVarResolver(BiFunction<Map<String, String>, String, String> op) {
		if (op == null) {
			op = DEF_VAR_RESOLVER;
		}
		varResolver = op;
	}

	// the final mapping, including mapper and prefix
	private String mapToPropKey(String name) {
		assert propNameToKeyMap != null; // initMappings
		return propNameToKeyMap.get(propNameToKeyMapper.apply(name));
	}

	/**
	 * Creates the prop name to properties resources keys mapping.
	 * MUST be called before using (after loading all properties)
	 */
	void initMappings() {
		// create the map to LC if required
		propNameToKeyMap = new LinkedHashMap<>(size());
		for (Map.Entry<String, String> ent : entrySet()) {
			propNameToKeyMap.put(propNameToKeyMapper.apply(ent.getKey()), ent.getKey());
		}
	}

	/**
	 * Loads the properties map with variables from a properties file. 
	 * If resolveVarsWithEnv is true, then property variables are resolved with, in order:
	 * 1 already seen props 2 sys vars 3 env vars. The property file is represented by the input
	 * stream. The location is the identifier for the stream and is stored for properties
	 * loaded. All properties are merged into this object.
	 */
	void loadProperties(InputStream is, String location) throws IOException {
		final List<String> keys = new ArrayList<>();

		// preserve order read from file
		final PropsMap outer = this;
		Properties loader = new Properties() {
			@Override
			public synchronized Object put(Object obj, Object value) {
				String key = (String) obj;
				keys.add(key);
				addPropLocation(key, location);
				outer.put(key, (String) value); // the props store put
				return super.put(key, value);   // this properties obj put (temp)
			}
		};
		loader.load(is);

		// since props are in order see, we go through each prop in order expanding vars
		for (String key : keys) {
			String val = get(key);
			put(key, expandVariables(val));
		}
	}

	// load properties from strng map, if there is var subst, then use an ordered map type
	void loadProperties(Map<String, String> props, String location) throws IOException {
		for (Map.Entry<String, String> ent : props.entrySet()) {
			String key = ent.getKey();
			addPropLocation(key, location);
			this.put(key, expandVariables(ent.getValue()));
		}
	}

	private void addPropLocation(String key, String location) {
		List<String> li = propLocations.get(key);
		if (li == null) {
			li = new ArrayList<>();
			propLocations.put(key, li);
		}
		li.add(location);
	}

	// return the value with all substututions expanded using props
	// a property var starts with `${` and ends with `}`, these chars are escape by preceding
	//  the `$` char with a backslash `\`
	private String expandVariables(String val) {
		StringBuilder buf = new StringBuilder(val.length());
		int p = 0, q = 0;
		while (true) {
			q = val.indexOf("${", q);
			if (q == -1) {
				break;
			} else {
				assert q >= 0;
				if (q == 0 || val.charAt(q-1) != '\\') {
					if (q > p) {
						buf.append(val.substring(p, q));
						p = q;
					}
					q += 2; // make sure q advances
					int r = val.indexOf("}", q);
					if (r > q) {
						String variable = val.substring(q, r);
						String expanded = varResolver.apply(this, variable);
						if (expanded != null) {
							buf.append(expanded);
							p = r+1;
						}
						// else could not find expanded value, ignore
					}
					// else closing `} not found, ignore
				} else {
					// escaped `\${`, just add that to buf
					q += 2;
					buf.append(val.substring(p, q));
					p = q;
				}
			}
		}
		if (p < val.length()) {
			buf.append(val.substring(p));
		}
		return buf.toString();
	}

	// map (method) name to the property value, uses IdentifierToPropNamesMapper to get best (or
	// first) fit of name to property value
	public String getPropValue(String identifier) {
		List<String> li = IdentifierToPropNamesMapper.map(identifier);
		for (String name : li) {
			if (prefix != null) {
				name = prefix + "." + name;
			}
			String key = mapToPropKey(name);
			if (key != null && containsKey(key)) {
				//List<String> locs = propLocations.get(key);
				//assert locs != null && locs.size() > 0;
				return get(key);
			}
		}
		return null;
	}

	// does not work on collection types or any other type the TypeParser needs method info for
	@Delegate
	public <T> T as(String identifier, Class<T> clz) {
		String val = getPropValue(identifier);
		if (val == null) {
			return null;
		}
		T parsed = TypeParser.parse(val, clz, null);
		if (parsed == TypeParser.Constants.NO_MATCH) {
			return null;
		}
		return parsed;
	}

	@Delegate
	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder("PropsMap[\n")
			.append("  locations");
		Map<String, List<String>> locToEntry = new HashMap<>();
		for (Map.Entry<String, List<String>> ent : propLocations.entrySet()) {
			for (String loc : ent.getValue()) {
				List<String> entries = locToEntry.get(loc);
				if (entries == null) {
					entries = new ArrayList<>();
					locToEntry.put(loc, entries);
				}
				entries.add(ent.getKey());
			}
		}
		for (Map.Entry<String, List<String>> ent : locToEntry.entrySet()) {
			buf.append("\n")
				.append("    ").append(ent.getKey()).append(" ").append(ent.getValue());
		}
		buf.append("\n").append("  values")
			.append("\n    ").append(super.toString());
		buf.append("\n").append("]");
		return buf.toString();
	}
}
