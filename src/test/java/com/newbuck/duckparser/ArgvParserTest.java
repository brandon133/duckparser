// Copyright (C) 2018-2019 Brandon Lee. BSD-3-Clause Licensed, see LICENSE.
package com.newbuck.duckparser;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArgvParserTest {
	void testSpec(String spec, String expected) {
		ArgvParser args = ArgvParser.parse(new String[0], spec);
		assertEquals(expected, args.specs().toString());
	}

	@Test public void testSpec() {
		// parseSpec returns ordered map
		testSpec("a? b? c* d* ba*",
				"{a=?, b=?, c=*, d=*, ba=*}");
		testSpec("a?b?c*d*ba*",
				"{a=?, b=?, c=*, d=*, ba=*}");
		testSpec("a?,b?,c*,d*,ba*",
				"{a=?, b=?, c=*, d=*, ba=*}");
		testSpec(";a?;,;,b?,c*,d*,ba*,,",
				"{a=?, b=?, c=*, d=*, ba=*}");

		testSpec("fred* barney+ wilma* bambam?",
				"{fred=*, barney=+, wilma=*, bambam=?}");
		testSpec("fred*barney+wilma*bambam?",
				"{fred=*, barney=+, wilma=*, bambam=?}");
	}

	// parse returns ordered map
	void testParse(String[] argv, String expected) {
		ArgvParser args = ArgvParser.parse(argv);
		assertEquals(expected, args.props().toString());
	}

	@Test public void testWithNoSpec() {
		testParse(new String[] {"-a", "arg1", "arg2"},
					"{a=[true], _args=[arg1, arg2]}");
		testParse(new String[] {"-a=arg1", "arg2"},
					"{a=[arg1], _args=[arg2]}");
		testParse(new String[] {"-ab", "arg1", "arg2"},
					"{a=[true], b=[true], _args=[arg1, arg2]}");
		testParse(new String[] {"-ab", "arg1", "arg2"},
					"{a=[true], b=[true], _args=[arg1, arg2]}");
		testParse(new String[] {"--a", "arg1", "arg2"},
					"{a=[arg1], _args=[arg2]}");
		testParse(new String[] {"--ab", "arg1", "arg2"},
					"{ab=[arg1], _args=[arg2]}");
	}

	// parse returns ordered map
	void testParse(String[] argv, String spec, String expected) {
		ArgvParser args = ArgvParser.parse(argv, spec);
		if (false) {
			System.out.println(">> " + args.specs()
					+ ", A" + args.aliases()
					+ ", D" + args.defaults()
					+ "\n   " + java.util.Arrays.toString(argv)
					+ "\n   => " + args.props());
		}
		assertEquals(expected, args.props().toString());
	}

	@Test public void testWithSpec() {
		String spec = "a? b? c* d* ba*";

		testParse(new String[] {"-ab", "arg1", "arg2"}, spec,
					"{a=[true], b=[true], _args=[arg1, arg2]}");
		testParse(new String[] {"-ba", "arg1", "arg2"}, spec,
					"{ba=[arg1], _args=[arg2]}");
		testParse(new String[] {"--a", "--b", "arg1", "arg2"}, spec,
					"{a=[true], b=[true], _args=[arg1, arg2]}");
		testParse(new String[] {"--a", "--c", "arg1", "arg2"}, spec,
					"{a=[true], c=[arg1], _args=[arg2]}");
		testParse(new String[] {"--c", "--a", "arg1", "arg2"}, spec,
					"{c=[null], a=[true], _args=[arg1, arg2]}");

		testParse(new String[] {"-c", "arg1", "arg2"}, spec,
					"{c=[arg1], _args=[arg2]}");
		testParse(new String[] {"-carg1", "arg2"}, spec,
					"{c=[arg1], _args=[arg2]}");
		testParse(new String[] {"-c=arg1", "arg2"}, spec,
					"{c=[arg1], _args=[arg2]}");
		testParse(new String[] {"--carg1", "arg2"}, spec,
					"{carg1=[arg2]}");
		testParse(new String[] {"--c", "arg1", "arg2"}, spec,
					"{c=[arg1], _args=[arg2]}");
	}

	@Test public void testWithAliases() {
		String spec = "alpha,a1,a2? beta,b? batman,robin,alfred*";

		testParse(new String[] {"-alpha", "-b", "-batman", "arg1", "-robin=arg2"}, spec,
					"{alpha=[true], beta=[true], batman=[arg1, arg2]}");
		testParse(new String[] {"-a1", "-a2=false", "-beta", "-batman", "arg1", "-alfred", "arg2"}, spec,
					"{alpha=[false], beta=[true], batman=[arg1, arg2]}");
	}

	@Test public void testWithKebabCase() {
		String spec = "al-pha,a1,a-2? batMan,rob-in,al-fred*";

		testParse(new String[] {"-alPha=false", "-a2", "-bat-man", "arg1", "-al-fred=arg2"}, spec,
					"{alPha=[true], batMan=[arg1, arg2]}");
	}

	@Test public void testOptionalDefaultValue() {
		String spec = "foo*{bar} a,a1,app-len*{10}";

		testParse(new String[] {"--foo"}, spec,
					"{foo=[bar]}");
		testParse(new String[] {"-foo"}, spec,
					"{foo=[bar]}");
		testParse(new String[] {"-a", "-a1"}, spec,
					"{a=[10, 10]}");
		testParse(new String[] {"--a-1", "--appLen", "-app-len", "999"}, spec,
					"{a=[10, 10, 999]}");
	}
}
