// Copyright (C) 2018-2019 Brandon Lee. BSD-3-Clause Licensed, see LICENSE.
package com.newbuck.duckparser.parse;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.logging.Logger;
import static com.newbuck.duckparser.parse.TypeParser.Constants.NO_MATCH;

/**
 * Convert to types using both implicit rules and explicit rules. Explicit rules are implemented by
 * the <code>Parser</code> interface and added statically using the `addParsers()` method. The
 * implicit rules are by convention and defined in the enums and in order of precedence.
 */
// https://github.com/lviggiano/owner/blob/master/owner/src/main/java/org/aeonbits/owner/Converters.java
// http://qussay.com/2013/09/28/handling-java-generic-types-with-reflection/
// https://www.artima.com/weblogs/viewpost.jsp?thread=208860 (great article re generic refl)
public enum TypeParser {
	// Explicitly defined parsers. Always take highest precedence, even over primitive types.
	PARSERS {
		@Override
		Object parseAs(String val, Class<?> targetType, Method targetMethod) {
			Object parsed = PARSER_STORE.parse(val, targetType);
			if (parsed != null) {
				return parsed;
			}
			return NO_MATCH;
		}
	},

	STRING {
		@Override
		Object parseAs(String val, Class<?> targetType, Method targetMethod) {
			if (targetType == String.class) {
				return val;
			}
			return NO_MATCH;
		}
	},

	// Primitives and their boxed types.
	// VALUE_OF and/or PARSE_TYPE would take care of primitives/boxed types using reflection but
	// this is optimized since these are the most commonly used types.
	PRIMITIVE {
		@Override
		Object parseAs(String val, Class<?> targetType, Method targetMethod) {
			Function<String, Object> fn = map.get(targetType);
			if (fn != null) {
				return fn.apply(val);
			}
			return NO_MATCH;
		}

		private final Map<Class<?>, Function<String, Object>> map = new HashMap<Class<?>, Function<String, Object>>() {{
			put(Boolean.TYPE,  (val) -> { return Boolean.parseBoolean(val); });
			put(Boolean.class, (val) -> { return Boolean.valueOf(val); });
			put(Byte.TYPE,     (val) -> { return Byte.parseByte(val); });
			put(Byte.class,    (val) -> { return Byte.valueOf(val); });
			put(Short.TYPE,    (val) -> { return Short.parseShort(val); });
			put(Short.class,   (val) -> { return Short.valueOf(val); });
			put(Integer.TYPE,  (val) -> { return Integer.parseInt(val); });
			put(Integer.class, (val) -> { return Integer.valueOf(val); });
			put(Long.TYPE,     (val) -> { return Long.parseLong(val); });
			put(Long.class,    (val) -> { return Long.valueOf(val); });
			put(Double.TYPE,   (val) -> { return Double.parseDouble(val); });
			put(Double.class,  (val) -> { return Double.valueOf(val); });
			put(Float.TYPE,    (val) -> { return Float.parseFloat(val); });
			put(Float.class,   (val) -> { return Float.valueOf(val); });
		}};
	},

	// Looks for these static creator methods: (limit to known common static creators)
	//  1 valueOf(String)
	//  2 parse<TYPE>, where <TYPE> is type of targetType
	STATIC_CREATOR {
		@Override
		Object parseAs(String val, Class<?> targetType, Method targetMethod) {
			List<Method> methods = findStaticCreatorMethods(targetType);
			for (Method method : methods) {
				try {
					return method.invoke(null, val);
				} catch (IllegalAccessException|InvocationTargetException e) {
					LOG.info(String.format("Error calling static `%s.%s(String)` -- %s",
								targetType.getName(), method.getName(), e.getMessage()));
				}
			}
			return NO_MATCH;
		}

		List<Method> findStaticCreatorMethods(Class<?> targetType) {
			List<Method> methods = new ArrayList<>();

			List<String> names = new ArrayList<>();
			String name = targetType.getSimpleName();
			if (targetType.getName().equals("java.lang.Integer")) {
				// parseInt instead of parseInteger
				name = "Int";
			}
			names.add("parse" + name);
			names.add("valueOf");

			for (Method method : targetType.getMethods()) {
				if (!Modifier.isStatic(method.getModifiers())) {
					continue;
				}
				for (String methodName : names) {
					if (methodName.equals(method.getName())) {
						Class<?> clz = method.getReturnType();
						if (targetType.isAssignableFrom(clz)) {
							Class<?>[] ptypes  = method.getParameterTypes();
							if (ptypes.length == 1 && ptypes[0].equals(String.class)) {
								methods.add(method);
							}
						}
					}
				}
			}
			return methods;
		}
	},

	// Look for (public static) constructor that takes a string argument
	STRING_CTR {
		@Override
		Object parseAs(String val, Class<?> targetType, Method targetMethod) {
			Constructor<?> ctr = matchCtr(targetType);
			if (ctr != null) {
				try {
					return ctr.newInstance(val);
				} catch (IllegalAccessException|InvocationTargetException|InstantiationException e) {
					LOG.info(String.format("Error calling ctr for `%s(String)` -- %s",
								targetType.getName(), e.getMessage()));
				}
			}
			return NO_MATCH;
		}

		private Constructor<?> matchCtr(Class<?> targetType) {
			Constructor<?>[] ctrs = targetType.getConstructors();
			for (Constructor<?> ctr : ctrs) {
				Class<?>[] ptypes  = ctr.getParameterTypes();
				if (ptypes.length == 1 && ptypes[0].equals(String.class)) {
					return ctr;
				}
			}
			return null;
		}
	},

	ARRAY {
		@Override
		Object parseAs(String csv, Class<?> targetType, Method targetMethod) {
			if (!targetType.isArray()) {
				return NO_MATCH;
			}
			Class<?> type = targetType.getComponentType();

			// usees CSV parser to split csv
			List<String> vals;
			try {
				vals = CsvParser.readLine(new StringReader(csv.trim()));
			} catch (IOException e) {
				return NO_MATCH;
			}
			if (vals == null) {
				return Array.newInstance(type, 0);
			}

			Parsed parsed = doParse(vals.get(0), type, targetMethod);
			Object array = Array.newInstance(type, vals.size());
			Array.set(array, 0, parsed.val());
			for (int i = 1; i < vals.size(); i++) {
				String val = vals.get(i);
				Object obj = parsed.typeParser().parseAs(val, type, targetMethod);
				Array.set(array, i, obj);
			}
			return array;
		}
	},

	// convert to collection, _but_ targetMethod cannot be null, supports:
	//  List (default ArrayList), Set (default LinkedHashSet), SortedSet (default TreeSet)
	COLLECTION {
		@Override
		Object parseAs(String val, Class<?> collectionType, Method targetMethod) {
			if (targetMethod == null || !Collection.class.isAssignableFrom(collectionType)) {
				return NO_MATCH; // must be (or subclass of) a collection
			}
			Object[] array = convertToArray(val, collectionType, targetMethod);
			Collection<Object> co = Arrays.asList(array);
			Collection<Object> actual = instantiateCollection(collectionType);
			actual.addAll(co);
			return actual;
		}

		// Probably not generally useful, but alternate for collection which requires the
		// parameterized type (and not the targetMethod.)
		@SuppressWarnings("unchecked")
		public <T> Collection<T> as(String val, Class<? extends Collection> collectionType, Class<T> parameterizedType) {
			Object stub = (T) Array.newInstance(parameterizedType, 0);
			T[] array = (T[]) ARRAY.parseAs(val, stub.getClass(), null);
			Collection<T> co = Arrays.asList(array);
			Collection<T> actual = (Collection<T>) instantiateCollection(collectionType);
			actual.addAll(co);
			return actual;
		}

		private Object[] convertToArray(String val, Class<?> targetType, Method targetMethod) {
			Class<?> type = getGenericType(targetMethod);
			Object stub = Array.newInstance(type, 0);
			return (Object[]) ARRAY.parseAs(val, stub.getClass(), targetMethod);
		}

		private Class<?> getGenericType(Method targetMethod) {
			if (targetMethod.getGenericReturnType() instanceof ParameterizedType) {
				ParameterizedType ptype = (ParameterizedType) targetMethod.getGenericReturnType();
				return (Class<?>) ptype.getActualTypeArguments()[0];
			}
			// default generic type for raw collections?
			return String.class;
		}

		private <T> Collection<T> instantiateCollection(Class<? extends T> targetType) {
			return targetType.isInterface()
				? instantiateCollectionFromInterface(targetType)
				: instantiateCollectionFromClass(targetType);
		}

		@SuppressWarnings("unchecked")
		private <T> Collection<T> instantiateCollectionFromClass(Class<? extends T> targetType) {
			try {
				return (Collection<T>) targetType.newInstance();
			} catch (Exception e) {
				throw new UnsupportedOperationException("No parser method to type " + targetType.getName());
			}
		}

		private <T> Collection<T> instantiateCollectionFromInterface(Class<? extends T> targetType) {
			if (List.class.isAssignableFrom(targetType)) {
				return new ArrayList<T>();
			} else if (SortedSet.class.isAssignableFrom(targetType)) {
				return new TreeSet<T>();
			} else if (Set.class.isAssignableFrom(targetType)) {
				return new LinkedHashSet<T>();
			}
			return new ArrayList<T>();
		}
	},

	// Throws UnsupportedOperationException exception.
	UNSUPPORTED {
		@Override
		Object parseAs(String val, Class<?> targetType, Method targetMethod) {
			throw new UnsupportedOperationException("No parser method for `" + val + "` to type " + targetType.getName());
		}
	}
	;

	private static final Logger LOG = Logger.getLogger(TypeParser.class.getName());
	public enum Constants { NO_MATCH; }
	private static final ParserStore PARSER_STORE = new ParserStore();

	/**
	 * Parse string value to targetType. The parser works in order of values() of this class.
	 */
	public static <T> T parse(String val, Class<T> targetType, Method targetMethod) {
		return doParse(val, targetType, targetMethod).val();
	}
	public static <T> T parse(String val, Class<T> targetType) {
		return doParse(val, targetType, null).val();
	}
	@SuppressWarnings("unchecked")
	public static <T> T parse(String val, Method targetMethod) {
		return (T) doParse(val, targetMethod.getReturnType(), targetMethod).val();
	}

	public <T> T as(String val, Class<T> targetType) {
		return as(val, targetType, (Method) null);
	}

	@SuppressWarnings("unchecked")
	public <T> T as(String val, Class<T> targetType, Method targetMethod) {
		T parsed = (T) parseAs(val, targetType, targetMethod); // unchecked
		return parsed == NO_MATCH ? null : parsed;
	}

	// returns NO_MATCH if cannot parse the val to targetType
	abstract Object parseAs(String val, Class<?> targetType, Method targetMethod);

	public <T> Collection<T> as(String val, Class<? extends Collection> collectionType, Class<T> parameterizedType) {
		return null;
	}

	/**
	 * Parser interface to parse a string representation to a type.
	 */
	public interface Parser<T> {
		T parse(String val);
	}

	/**
	 * Add parsers to parser store.
	 */
	public static void addParsers(Parser<?>... parsers) {
		for (Parser parser : parsers) {
			PARSER_STORE.add(parser);
		}
	}

	static class Parsed<T> {
		private final TypeParser typeParser;
		private final T val;

		Parsed(TypeParser typeParser, T val) {
			this.typeParser = typeParser;
			this.val = val;
		}
		TypeParser typeParser() {
			return this.typeParser;
		}
		public T val() {
			return this.val;
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> Parsed<T> doParse(String val, Class<T> targetType, Method targetMethod) {
		/*if (val == null) {
			return new Parsed<>(null, null);
		}*/

		for (TypeParser parser : values()) {
			T parsed = (T) parser.parseAs(val, targetType, targetMethod); // unchecked
			if (parsed != NO_MATCH) {
				return new Parsed<T>(parser, parsed);
			}
		}
		throw new AssertionError("Unreachable");
	}

	static class ParserStore {
		static List<Parser<?>> parsers = new ArrayList<Parser<?>>();

		// only add this parser object once, will add the same class since could have different parameters
		void add(Parser<?> parser) {
			for (Parser<?> existing : parsers) {
				if (parser.getClass().equals(existing.getClass())) {
					LOG.warning("Parser object `" + parser + "` already added");
					return;
				}
			}
			parsers.add(parser);
		}

		<T> Object parse(String val, Class<T> targetType) {
			for (Parser<T> parser : matchParser(targetType)) {
				T parsed = (T) parser.parse(val);
				if (parsed != null) {
					return parsed;
				}
			}
			return null;
		}

		// return any matching parser
		@SuppressWarnings("unchecked")
		<T> List<Parser<T>> matchParser(Class<T> targetType) {
			List<Parser<T>> li = new ArrayList<>();
			for (Parser<?> parser : parsers) {
				Type[] generics = parser.getClass().getGenericInterfaces();
				assert generics.length == 1;                     // Parser has a single interface
				assert generics[0] instanceof ParameterizedType; // Parser is parameterized

				Type[] types = ((ParameterizedType) generics[0]).getActualTypeArguments();
				assert types.length == 1;                        // Parser has a single type
				if (((Class<?>) types[0]).isAssignableFrom(targetType)) {
					li.add((Parser<T>) parser); // unchecked
				}
			}
			return li;
		}
	}
}
