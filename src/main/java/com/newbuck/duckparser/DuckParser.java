// Copyright (C) 2018-2019 Brandon Lee. BSD-3-Clause Licensed, see LICENSE.
package com.newbuck.duckparser;
import java.lang.reflect.Proxy;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import static com.newbuck.duckparser.parse.TypeParser.Parser;

/**
 * Load configuration properties.
 * There are 2 things you need to do:
 *  1 SPECIFY your properties
 *    a _Type_ of property
 *    b is the property _Required_?
 *    c if the property is not required, does it have a _Default_Value_?
 *  2 LOAD and USE your properties
 *
 * Properties substitution is supported. A config has a {name} and {configDir}.
 * By convention, the loading happens:
 *  1 Loads getClassLoader():{name}-default.properties
 *  2 Loads {configDir}/{name}-default.properties
 *  3 Loads from {configDir}/{name}.properties - overwrites earlier definitions.
 *
 * Always returns properties object but will be empty if no resources exist.
 */
public class DuckParser {
	private static final Logger LOG = Logger.getLogger(DuckParser.class.getName());

	// builder provides params
	private Builder params;

	// either/both of baseName or initPropsMap/addProps must be specified
	DuckParser(Builder params) {
		this.params = params;
	}

	public static Builder build() {
		return new Builder();
	}

	/**
	 * Builder, holds the params/settings for the config loader.
	 * Ctr requires the properties name. The name will be the name of the props resources.
	 * See documentation for detailed explanation of settings.
	 */
	public static class Builder {
		public String baseName;
		public String prefix;
		public Parser<?>[] parsers;
		public Map<String, String> initPropsMap;
		public String addProps;
		public Path[] searchDirs = new Path[0];
		public boolean searchClasspath = true;
		public boolean searchDefault = true;
		public boolean ignoreCase = true;
		public boolean resolveVarsWithEnv = true;

		Builder() { }

		public Builder with(String baseName, Consumer<Builder> builder) {
			this.baseName = baseName;
			builder.accept(this);
			return this;
		}
		public Builder with(Consumer<Builder> builder) {
			builder.accept(this);
			return this;
		}

		public Builder clone(Builder rhs) {
			Builder lhs = new Builder();
			lhs.baseName = rhs.baseName;
			prefix = rhs.prefix;
			if (rhs.parsers != null) {
				lhs.parsers = rhs.parsers.clone();
			}
			if (rhs.initPropsMap != null) {
				lhs.initPropsMap = new LinkedHashMap<>(initPropsMap); // preserve order
			}
			lhs.addProps = rhs.addProps;
			assert rhs.searchDirs != null;
			lhs.searchDirs = rhs.searchDirs.clone();
			lhs.searchClasspath = rhs.searchClasspath;
			lhs.searchDefault = rhs.searchDefault;
			lhs.ignoreCase = rhs.ignoreCase;
			lhs.resolveVarsWithEnv = rhs.resolveVarsWithEnv;
			return lhs;
		}

		public DuckParser build() {
			// clone here since caller could change the params
			return new DuckParser(clone(this));
		}

		public <T> T create(Class<? extends T> clz) {
			return new DuckParser(this).create(clz);
		}

		public <T> T create(Class<? extends T> clz, String[] argv) {
			return new DuckParser(this).create(clz, argv);
		}
	}

	// loads from params (builder) in order:
	//  - classpath:<baseName>-default.properties
	//  - <searchDirs*>/<baseName>-default.properties
	//  - <searchDirs*>/<baseName>.properties
	private PropsMap loadFromParams() {
		PropsMap propsMap = new PropsMap(
				params.prefix,
				params.ignoreCase,
				params.resolveVarsWithEnv);

		if (params.baseName == null
				&& params.addProps == null
				&& params.initPropsMap == null) {
			return propsMap;
		}

		try {
			if (params.initPropsMap != null) {
				propsMap.loadProperties(params.initPropsMap, "initmap");
			}

			if (params.baseName != null) {
				String resDefName = params.baseName + "-default.properties";
				String resName = params.baseName + ".properties";

				if (params.searchClasspath) {
					if (params.searchDefault) {
						loadClasspathResource(propsMap, resDefName);
					}
					loadClasspathResource(propsMap, resName);
				}

				for (Path dir : params.searchDirs) {
					if (params.searchDefault) {
						Path f = dir.resolve(resDefName);
						loadPathResource(propsMap, f);
					}
					Path f = dir.resolve(resName);
					loadPathResource(propsMap, f);
				}
			}

			if (params.addProps != null) {
				loadString(propsMap, params.addProps);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return propsMap;
	}

	private void loadClasspathResource(PropsMap propsMap, String name) throws IOException {
		try (InputStream in = this.getClass().getClassLoader().getResourceAsStream(name)) {
			if (in != null) {
				String loc = "classpath:" + name;
				propsMap.loadProperties(in, loc);
				LOG.info("Loaded properties from: " + loc);
				return;
			}
		}
	}

	private void loadPathResource(PropsMap propsMap, Path file) throws IOException {
		if (Files.isRegularFile(file)) {
			try (InputStream in = new FileInputStream(file.toFile())) {
				String loc = "file:" + file;
				propsMap.loadProperties(in, loc);
				LOG.info("Loaded properties from: " + loc);
				return;
			}
		}
	}

	private void loadString(PropsMap propsMap, String strProps) throws IOException {
		try (InputStream in = new ByteArrayInputStream(strProps.getBytes())) {
			propsMap.loadProperties(in, "string");
			LOG.info("Loaded properties from: (string)");
		}
	}


	@SuppressWarnings("unchecked")
	public <T> T create(Class<? extends T> clz) {
		PropsMap propsMap = loadFromParams();
		propsMap.initMappings();
		DuckParserInvocationHandler handler = new DuckParserInvocationHandler(propsMap);

		Class<?>[] interfaces = new Class<?>[] { clz };
		return (T) Proxy.newProxyInstance(clz.getClassLoader(), interfaces, handler);
	}

	@SuppressWarnings("unchecked")
	public <T> T create(Class<? extends T> clz, String[] argv) {
		ArgvParser args = ArgvParser.parse(argv, clz);

		PropsMap propsMap = loadFromParams();
		addProps(propsMap, args.props());
		propsMap.initMappings();
		DuckParserInvocationHandler handler = new DuckParserInvocationHandler(propsMap);

		Class<?>[] interfaces = new Class<?>[] { clz };
		return (T) Proxy.newProxyInstance(clz.getClassLoader(), interfaces, handler);
	}

	// add props directly to propsMap:
	// 1 all null values are filtered out of props value
	// 2 if the filtered list has multiple values, it is converted to csv
	// XXX: are multiple vals handled correctly? (converted to csv)
	private void addProps(PropsMap propsMap, Map<String, List<String>> props) {
		for (Map.Entry<String, List<String>> ent : props.entrySet()) {
			// filter null values out
			List<String> li = ent.getValue().stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

			StringBuilder buf = new StringBuilder();
			if (li.size() == 1) {
				buf.append(li.get(0));
			} else {
				for (String val : li) {
					if (buf.length() > 0) {
						buf.append(",");
					}
					buf.append(quote(val));
				}
			}
			propsMap.put(ent.getKey(), buf.toString());
		}
	}

	// quote for csv value
	private String quote(String str) {
		boolean quote = false;
		if (str.indexOf('"') >= 0) {
			str = str.replaceAll("\"", "\"\"");
			quote = true;
		}
		if (str.indexOf(',') >= 0) {
			quote = true;
		}
		if (quote) {
			str = "\"" + str + "\"";
		}
		return str;
	}
}
