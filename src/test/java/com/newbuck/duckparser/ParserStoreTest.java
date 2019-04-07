// Copyright (C) 2018-2019 Brandon Lee. BSD-3-Clause Licensed, see LICENSE.
package com.newbuck.duckparser;
import org.junit.Test;
import static org.junit.Assert.*;

public class ParserStoreTest {
	interface Props extends ParserStore {
		String strVal();
	}

	@Test public void testAs() {
		Props props = DuckParser.build()
			.with($ -> {
				$.addProps = String.join("\n",
						"str.val = the answer",
						"int.val = 42");
			})
			.create(Props.class);

		// the string value is from the interface
		assertEquals("the answer", props.strVal());
		// the int value is from the ParserStore.as() method
		assertEquals(42, (int) props.as("intVal", int.class));
		assertEquals(42, (int) props.as("int_val", int.class));
		assertEquals(42, (int) props.as("int.val", int.class));
	}
}
