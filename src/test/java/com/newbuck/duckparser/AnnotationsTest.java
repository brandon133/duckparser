// Copyright (C) 2018-2019 Brandon Lee. BSD-3-Clause Licensed, see LICENSE.
package com.newbuck.duckparser;
import org.junit.Test;
import static org.junit.Assert.*;

public class AnnotationsTest {
	interface Props {
		@Default("23")
		int numThreads();

		@Default("defaultName")
		String executorName();

		String foobar();
	}

	@Test public void example() {
		Props props = DuckParser.build()
			.with("foo", $ -> {})
			.create(Props.class);

		assertEquals("defaultName",  props.executorName());
		assertEquals(23,  props.numThreads());
		assertNull(props.foobar());
	}
};
