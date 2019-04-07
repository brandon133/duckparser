// Copyright (C) 2018-2019 Brandon Lee. BSD-3-Clause Licensed, see LICENSE.
package com.newbuck.duckparser;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArgvTest {
	interface Props {
		boolean includeEmpty();

		@Alias({"f", "fileName"})
		String filename();

		@Default("10")
		@Alias({"n"})
		int numLines();

		String[] _args();
	}

	@Test public void testArgv() {
		String[] argv = new String[] { "--include-empty", "-f", "/path/to/file.txt" };
		Props props = DuckParser.build()
			.create(Props.class, argv);

		assertTrue(props.includeEmpty());
		assertEquals("/path/to/file.txt", props.filename());
		assertEquals(10, props.numLines());
		assertNull(props._args());
	}

	@Test public void testNotFlag() {
		String[] argv = new String[] { "-f", "/path/to/file.txt" };
		Props props = DuckParser.build()
			.create(Props.class, argv);

		assertEquals("/path/to/file.txt", props.filename());
		assertFalse(props.includeEmpty());
	}

	@Test public void testMultipleFlag() {
		String[] argv = new String[] { "-includeEmpty=true", "-includeEmpty=false" };
		Props props = DuckParser.build()
			.create(Props.class, argv);

		assertFalse(props.includeEmpty());
	}

	@Test public void testMultipleFlag2() {
		String[] argv = new String[] { "-includeEmpty=false", "-includeEmpty" };
		Props props = DuckParser.build()
			.create(Props.class, argv);

		assertTrue(props.includeEmpty());
	}

	@Test public void testArgs() {
		String[] argv = new String[] { "-file-name", "/path/to/file.txt", "arg1" };
		Props props = DuckParser.build()
			.create(Props.class, argv);

		assertEquals("/path/to/file.txt", props.filename());
		assertNotNull(props._args());
		assertEquals(1, props._args().length);
		assertEquals("arg1", props._args()[0]);
	}

	@Test public void testMultipleArgs() {
		String[] argv = new String[] { "arg1", "arg2", "arg3" };
		Props props = DuckParser.build()
			.create(Props.class, argv);

		assertNotNull(props._args());
		assertEquals(3, props._args().length);
		assertEquals("arg1", props._args()[0]);
		assertEquals("arg2", props._args()[1]);
		assertEquals("arg3", props._args()[2]);
	}

	@Test public void testDoubleDash() {
		String[] argv = new String[] { "-includeEmpty", "--", "-f", "filename" };
		Props props = DuckParser.build()
			.create(Props.class, argv);

		assertTrue(props.includeEmpty());
		assertNotNull(props._args());
		assertEquals(2, props._args().length);
		assertEquals("-f", props._args()[0]);
		assertEquals("filename", props._args()[1]);
	}
}
