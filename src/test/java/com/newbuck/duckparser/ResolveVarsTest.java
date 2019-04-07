// Copyright (C) 2018-2019 Brandon Lee. BSD-3-Clause Licensed, see LICENSE.
package com.newbuck.duckparser;
import java.util.HashMap;
import org.junit.Test;
import static org.junit.Assert.*;

public class ResolveVarsTest {
	interface Props {
		String val1();
		String val2();
	}

	@Test public void testResolveVars() {
		HashMap<String, String> map = new HashMap<String, String>() {{
			put("val.1", "Hello ${USER}");
			put("val.2", "java.version=${java.version}");
		}};

		String user = System.getenv("USER");
		String vers = System.getProperty("java.version");

		Props props = DuckParser.build()
			.with($ -> {
				$.initPropsMap = map;
			})
			.create(Props.class);

		assertEquals("Hello " + user, props.val1());
		assertEquals("java.version=" + vers, props.val2());
	}

	@Test public void testResolveVarsDisabled() {
		HashMap<String, String> map = new HashMap<String, String>() {{
			put("val.1", "Hello ${USER}");
			put("val.2", "java.version=${java.version}");
		}};

		String user = System.getenv("USER");
		String vers = System.getProperty("java.version");

		Props props = DuckParser.build()
			.with($ -> {
				$.initPropsMap = map;
				$.resolveVarsWithEnv = false;
			})
			.create(Props.class);

		assertEquals("Hello ${USER}", props.val1());
		assertEquals("java.version=${java.version}", props.val2());
	}
}
