// Copyright (C) 2018-2019 Brandon Lee. BSD-3-Clause Licensed, see LICENSE.
package com.newbuck.duckparser;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/**
 * Parse arguments with optional specificaton string.
 *
 * Default: (no spec)
 *
 *  `-a arg1 arg2`   =>  a=true,         _=[arg1, arg2]
 *  `-ab arg1 arg2`  =>  a=true, b=true, _=[arg1, arg2]
 *  `--a arg1 arg2`  =>  a=arg1,         _=[arg2]
 *  `--ab arg1 arg2` =>  ab=arg1,        _=[arg2]
 *
 *  `-a=arg1 arg2`   =>  a=arg1, _=[arg2]
 *  `--a=arg1 arg2`  =>  a=arg1, _=[arg2]
 *
 * Spec: `a? b? c* d* ba*`
 *
 *  `-ab arg1 arg2`     => a=true, b=true, _=[arg1, arg2] *single dash flag opts take precedence
 *  `-ba arg1 arg2`     => ba=arg1,        _=[arg2]
 *  `--ab arg1 arg2`    => ab=arg1,        _=[arg2]       *dbl dash opt is always a single word
 *  `--a --b arg1 arg2` => a=true, b=true, _=[arg1, arg2]
 *  `--a --c arg1 arg2` => a=true, c=arg1, _=[arg2]
 *  `--c --a arg1 arg2` => a=true, c=null, _=[arg1, arg2] *c is optional `c*` so takes argument
 *
 *  `-c arg1 arg2`      => c=arg1          _=[arg2]
 *  `-carg1 arg2`       => c=arg1          _=[arg2]
 *      *Note this case is a special case for ONLY single-dash AND single-char
 *       and must be explicitly OPT or REQ. There's still 2 ambiguities here:
 *         a an explicit `oval` option
 *         b 4 flag options: o,v,a,l
 *       In both cases, the original interp takes precedence
 *  `--carg1 arg2`      => carg1=arg2                *carg1 is the interpreted opt name
 *  `--c arg1 arg2`     => c=arg1          _=[arg2]
 *
 * Specs are implied by return types of interface and any annotations:
 *  - Boolean/boolean return type imples FLAG
 *  - Any method annotated with `@Default("val")` implies OPTIONAL
 *  - All other methods implies REQUIRED
 *
 *  - FLAG options have special rules:
 *    - all FLAG options return false unless explicitly specified (even if not in option)
 *      this is _only_ for the argv creator (Props.create() from that takes argv)
 *    - multiple FLAG options are set to last FLAG option seen
 *    - Example:
 *      interface Props { boolean flag(); }
 *       `arg`                    => props.flag() is false
 *       `-flag=false -flag=true` => props.flag() is true
 *
 *  - OPTIONAL and REQUIRED options that are specified multiple times returns the CSV of all values:
 *    `-opt=abc -opt=1"2"3 -opt=x,y` => opt=abc,"1""2""3","x,y"
 *    `-opt=1"2"3                    => opt=1"2"3 // note not quoted and singular value
 *
 *  - Special cases:
 *    - _dash=true means a single dash option `-` was seen
 *    - _args=string[] are the arguments (non-options) of the command line (`_` in examples above)
 *    - Double dash `--` is the standard meaning of stop processing options, read by parser only
 */
class ArgvParser {
	private final Map<String, Character> specs = new LinkedHashMap<>();
	private final Map<String, String> aliases = new HashMap<>();
	private final Map<String, String> defaults = new HashMap<>();
	private final Map<String, List<String>> props = new LinkedHashMap<>();

	private ArgvParser() {
	}

	private ArgvParser(String spec) {
		parseStringSpec(spec);
	}

	private ArgvParser(Class<?> clz) {
		parseClassSpec(clz);
	}

	Map<String, Character> specs() {
		return specs;
	}
	Map<String, String> aliases() {
		return aliases;
	}
	Map<String, String> defaults() {
		return defaults;
	}
	Map<String, List<String>> props() {
		return props;
	}

	public static ArgvParser parse(String[] args) {
		ArgvParser argv = new ArgvParser();
		argv.parseArgs(args);
		return argv;
	}

	public static ArgvParser parse(String[] args, String argspec) {
		ArgvParser argv = new ArgvParser(argspec);
		argv.parseArgs(args);
		return argv;
	}

	public static ArgvParser parse(String[] args, Class<?> clz) {
		ArgvParser argv = new ArgvParser(clz);
		argv.parseArgs(args);
		return argv;
	}

	private void addProp(String k, String v) {
		if (aliases.containsKey(k)) {
			k = aliases.get(k);
		}
		List<String> li = props.get(k);
		if (li == null) {
			li = new ArrayList<>();
			props.put(k, li);
		}
		if (v == null && defaults.containsKey(k)) {
			v = defaults.get(k);
		}
		if (specs.containsKey(k) && specs.get(k) == '?') { // FLAG
			if (li.isEmpty())
				li.add(v);
			else
				li.set(0, v);
		} else { // OPTIONAL or REQUIRED
			li.add(v);
		}
	}

	// optional spec must be parsed first
	private void parseArgs(String[] args) {
		Predicate<String> isFlagArg = (arg) -> arg != null && (arg.equals("true") || arg.equals("false"));
		Predicate<String> hasOpt = (opt) -> specs.containsKey(opt) || aliases.containsKey(opt);
		Function<String, Character> getOpt = (opt) -> specs.containsKey(opt) ?  specs.get(opt)
			: (aliases.containsKey(opt) ?  specs.get(aliases.get(opt)) : null);

		// stop at '--' or length
		int stop = IntStream.range(0, args.length)
			.filter(i -> args[i].equals("--")).findFirst()
			.orElse(args.length);

		for (int idx = 0; idx < stop; ++idx) {
			String arg = args[idx];
			int isOpt = arg.startsWith("-") ? (arg.startsWith("--") ? 2 : 1) : 0;

			if (isOpt == 0) {
				addProp("_args", arg);
				continue;
			}

			String opt = null;
			String val = null;

			// `-` by itself
			if (arg.equals("-")) {
				addProp("_dash", "true");
				continue;
			}
			assert !arg.equals("--"); // stop checks for this

			assert arg.length() > 1;    // `-` already checked for
			arg = arg.substring(isOpt); // strip leading dashes

			// handle `=` opt which always explicitly defines opt/val
			int pos = arg.indexOf("=");
			if (pos == 0) {
				throw new IllegalStateException("Bad option syntax: `" + args[idx] + "`");
			}
			if (pos > 0) {
				opt = cvtKebabToCamel(arg.substring(0, pos));
				val = pos == arg.length()-1
					? null // `-opt=` is same as `-opt=<null>`
					: arg.substring(pos+1);
				addProp(opt, val);
				continue;
			}

			// these are the single `-` cases where entire arg is not the option
			if (isOpt == 1 && !hasOpt.test(cvtKebabToCamel(arg))) {
				Character argtype = getOpt.apply(arg.substring(0, 1));
				if (argtype != null && argtype != '?') { // -o<arg>
					val = arg.substring(1);
					addProp(arg.substring(0, 1), val);
				} else { // each letter is single char flag
					assert arg.length() > 0;
					for (int i = 0; i < arg.length(); ++i) {
						String ch = arg.substring(i, i+1);
						if (hasOpt.test(ch) && getOpt.apply(ch) != '?') {
							String msg = "Opt `" + ch + "` in arg `" + args[idx]
								+ "` must be a flag option, but is `"
								+ getOpt.apply(ch) + "`";
							throw new IllegalStateException(msg);
						}
						addProp(ch, "true");
					}
				}
				continue;
			}

			// the only remaining is the entire arg
			arg = cvtKebabToCamel(arg);
			boolean hasVal = idx+1 < stop && !args[idx+1].startsWith("-");
			boolean isFlag = hasOpt.test(arg) && getOpt.apply(arg) == '?';
			if (isFlag) {
				// only eat val if true|false, o/w set to true and leave it
				val = hasVal && isFlagArg.test(args[idx+1]) ? args[++idx] : "true";
			} else if (hasVal) {
				val = args[++idx];
			}
			addProp(arg, val);
		}
		// remaining args after stop are regular
		for (int idx = stop; idx < args.length; ++idx) {
			if (!args[idx].equals("--")) {
				addProp("_args", args[idx]);
			}
		}
	}

	/**
	 * Parse spec in string form.
	 * Rules:
	 *  - Legal option chars are alphanumeric and `-`
	 *  - Option can _not_ start or end with `-`
	 *  - Each option spec is: `[\w-]+(,[\w-]+)*[?*+]({<str>})?[ ,;]*`
	 *                          1      2        3     4        5
	 *    1 the main option name is the properties name
	 *    2 optional alias(es)
	 *    3 one of `?` (FLAG), `*` (OPTIONAL), `+` (REQUIRED)
	 *    4 only if OPTIONAL, a default value can be specified between curly brackets (escape with `\`)
	 *    5 separators are discarded
	 *
	 *    NOTE: no extra spaces are allowed
	 *  - All options are converted to camel case _from_ kebab case
	 *    E.g. `foo-bar` is converted to `fooBar` and there is _no_ difference how these are specified:
	 *     `foo-bar=1 fooBar=2` => `fooBar=[1,2]
	 */
	private void parseStringSpec(final String spec) {
		BiFunction<String, Integer, String> err = (msg, pos) -> msg + " -- pos=" + pos + ", spec=`" + spec + "`";
		Predicate<Character> isSepChar = (ch) -> ch == ' ' || ch == ';' || ch == ',';
		Predicate<Character> isOptChar = (ch) -> Character.isLetterOrDigit(ch) || ch == '-';

		int pos = 0;
		while (pos < spec.length()) {
			// trim
			while (pos < spec.length() && isSepChar.test(spec.charAt(pos))) {
				++pos;
			}
			if (pos == spec.length()) {
				break;
			}
			// opt + aliases
			List<String> opts = new ArrayList<>();
			while (pos < spec.length()) {
				int start = pos;
				while (pos < spec.length() && isOptChar.test(spec.charAt(pos))) {
					++pos;
				}
				if (pos == start) {
					throw new IllegalStateException(err.apply("Illegal char", pos));
				}
				opts.add(spec.substring(start, pos));
				if (spec.charAt(pos) != ',') {
					break;
				}
				++pos; // skip `,`
			}
			if (pos == spec.length()) {
				throw new IllegalStateException(err.apply("Unexpected end", pos));
			}
			if (opts.size() == 0) {
				throw new IllegalStateException(err.apply("No options found", pos));
			}
			char argtype = spec.charAt(pos);
			if (argtype != '?' && argtype != '*' && argtype != '+') {
				throw new IllegalStateException(err.apply("Bad arg-type char `" + argtype + "`", pos));
			}
			++pos;

			// the option
			String opt = cvtKebabToCamel(opts.get(0));
			checkOpt(opt);

			// read OPTIONAL default value
			if (argtype == '*' && pos < spec.length() && spec.charAt(pos) == '{') {
				// read default value up to `}` not preceded by `\`
				++pos; // skip `{`
				StringBuilder buf = new StringBuilder();
				while (pos < spec.length() && spec.charAt(pos) != '}') {
					if (spec.charAt(pos) == '\\' && pos+1 < spec.length()) {
						++pos; // skip backslash
					}
					buf.append(spec.charAt(pos));
					++pos;
				}
				if (pos == spec.length() || spec.charAt(pos) != '}') {
					throw new IllegalStateException(err.apply("Bad OPTIONAL default value syntax", pos));
				}
				++pos; // skip `}`
				defaults.put(opt, buf.toString());
			}

			specs.put(opt, argtype);
			for (int i = 1; i < opts.size(); ++i) {
				String alias = cvtKebabToCamel(opts.get(i));
				checkOpt(alias);
				aliases.put(alias, opt);
			}
		}
	}

	// Parse class for spec, defaults are handled at invocation so handle specs and aliases.
	// The argtype is depends on return: 1. bool => `?` 2. @Default => `*` 3. else `+`
	private <T> void parseClassSpec(final Class<T> clz) {
		Method[] methods = clz.getMethods();
		for (Method method : methods) {
			char argtype = '+';
			if (method.getReturnType() == Boolean.TYPE || method.getReturnType() == Boolean.class) {
				argtype = '?';
			} else if (method.getAnnotation(Default.class) != null) {
				argtype = '*';
			}

			String opt = method.getName();
			checkOpt(opt);
			specs.put(opt, argtype);

			if (argtype == '?') {
				props.put(opt, Arrays.asList("false"));
			}

			Alias annotation = method.getAnnotation(Alias.class);
			if (annotation != null) {
				for (String alias : annotation.value()) {
					alias = cvtKebabToCamel(alias);
					checkOpt(alias);
					aliases.put(alias, opt);
				}
			}
		}
	}

	// make sure opt doesn't already exist
	private void checkOpt(String opt) {
		if (specs.containsKey(opt)) {
			throw new IllegalStateException("Option `" + opt + "` already exists in specs");
		}
		if (aliases.containsKey(opt)) {
			throw new IllegalStateException("Option `" + opt + "` already exists in aliases");
		}
	}

	// convert opt from kebab case to camel case if needed
	private String cvtKebabToCamel(String opt) {
		StringBuilder buf = new StringBuilder(opt.length());
		boolean next = false;
		for (char ch : opt.toCharArray()) {
			if (ch == '-') {
				next = true;
			} else if (next) {
				buf.append(Character.toUpperCase(ch));
				next = false;
			} else {
				buf.append(ch);
			}
		}
		return buf.toString();
	}
}
