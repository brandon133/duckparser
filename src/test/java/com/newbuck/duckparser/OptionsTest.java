// Copyright (C) 2018-2019 Brandon Lee. BSD-3-Clause Licensed, see LICENSE.
package com.newbuck.duckparser;
import java.util.HashMap;
import org.junit.Test;
import static org.junit.Assert.*;

// options tests that aren't tested in other tests
public class OptionsTest {
	interface Props {
		String myVal1();
		String myVal2();
		String myVal3();
	}

	@Test public void testPrefix() {
		Props props = DuckParser.build()
			.with($ -> {
				$.prefix = "str";
				$.addProps = String.join("\n",
						"str.my.val1 = yabba dabba doo",
						"str.my.val2 = charge it",
						"str.my.val3 = bam bam");
			})
			.create(Props.class);

		assertEquals("yabba dabba doo", props.myVal1());
		assertEquals("charge it", props.myVal2());
		assertEquals("bam bam", props.myVal3());
	}

	@Test public void testInitPropsMap() {
		HashMap<String, String> map = new HashMap<String, String>() {{
			put("myVal2", "fred");
		}};

		Props props = DuckParser.build()
			.with($ -> {
				$.initPropsMap = map;
			})
			.create(Props.class);

		assertEquals("fred", props.myVal2());
	}

	@Test public void testIgnoreCase() {
		HashMap<String, String> map = new HashMap<String, String>() {{
			put("my.Val.2", "fred");   // note upper case `V`
			put("my.val.3", "barney");
		}};

		Props props = DuckParser.build()
			.with($ -> {
				$.initPropsMap = map;
			})
			.create(Props.class);
		assertEquals("fred", props.myVal2());

		props = DuckParser.build()
			.with($ -> {
				$.initPropsMap = map;
				$.ignoreCase = false;
			})
			.create(Props.class);
		assertNull(props.myVal2());
		assertEquals("barney", props.myVal3());
	}

	@Test public void testIgnoreCase2() {
		HashMap<String, String> map = new HashMap<String, String>() {{
			put("myVal1", "wilma");   // note upper case `V`
			put("myval2", "betty");
		}};

		Props props = DuckParser.build()
			.with($ -> {
				$.initPropsMap = map;
			})
			.create(Props.class);
		assertEquals("wilma", props.myVal1());
		assertEquals("betty", props.myVal2());

		props = DuckParser.build()
			.with($ -> {
				$.initPropsMap = map;
				$.ignoreCase = false;
			})
			.create(Props.class);
		assertEquals("wilma", props.myVal1());
		assertNull(props.myVal2());
	}
}
