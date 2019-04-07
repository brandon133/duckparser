// Copyright (C) 2018-2019 Brandon Lee. BSD-3-Clause Licensed, see LICENSE.
package com.newbuck.duckparser;
import org.junit.Test;
import static org.junit.Assert.*;

// basic overall config test
public class DuckParserTest {

	// the example from README

	interface Props {
		int numThreads();
		String executorName();
	}

	@Test public void testExample() {
		Props props = DuckParser.build()
			.with("test", $ -> {
				$.addProps = String.join("\n",
						"numThreads = 8",
						"executor.name = my_executor");
			})
			.create(Props.class);

		// debug:
		System.out.println(props.toString());
		System.out.println(TestUtility.reflToString(props));

		assertEquals("my_executor",  props.executorName());
		assertEquals(8,  props.numThreads());
	}

	// basic primitive and boxed types
	// loaded from classpath in src/test/resources/test[-default].properties

	interface PrimitiveProps {
		String stringVal();
		String stringVal2();
		byte byteVal();
		short shortVal();
		int intVal();
		long longVal();
		float floatVal();
		double doubleVal();
		boolean booleanVal();
	}

	@Test public void testPrimitivesLoadedFromClasspath() {
		// loaded from classpath
		PrimitiveProps props = DuckParser.build()
			.with("test", $ -> {})
			.create(PrimitiveProps.class);

		assertEquals("to be or not to be", props.stringVal());
		assertEquals("that is the question", props.stringVal2());
		assertEquals(8, props.byteVal());
		assertEquals(42, props.shortVal());
		assertEquals(12345, props.intVal());
		assertEquals(67890, props.longVal());
		assertEquals(3.14159, props.floatVal(), 0.0001);
		assertEquals(2.99792458E10, props.doubleVal(), 0.0001);
		assertEquals(true, props.booleanVal());
	}

	interface BoxedProps {
		String stringVal();
		String stringVal2();
		Byte byteVal();
		Short shortVal();
		Integer intVal();
		Long longVal();
		Float floatVal();
		Double doubleVal();
		Boolean booleanVal();
	}

	@Test public void testBoxedsLoadedFromClasspath() {
		// loaded from classpath
		BoxedProps props = DuckParser.build()
			.with("test", $ -> {})
			.create(BoxedProps.class);

		assertEquals(new String("to be or not to be"), props.stringVal());
		assertEquals(new String("that is the question"), props.stringVal2());
		assertEquals(Byte.valueOf((byte) 8), props.byteVal());
		assertEquals(Short.valueOf((short) 42), props.shortVal());
		assertEquals(Integer.valueOf(12345), props.intVal());
		assertEquals(Long.valueOf(67890), props.longVal());
		assertEquals(Float.valueOf(3.14159f), props.floatVal(), 0.0001);
		assertEquals(Double.valueOf(2.99792458E10), props.doubleVal(), 0.0001);
		assertEquals(Boolean.TRUE, props.booleanVal());
	}
}
