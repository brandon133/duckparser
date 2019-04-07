// Copyright (C) 2018-2019 Brandon Lee. BSD-3-Clause Licensed, see LICENSE.
package com.newbuck.duckparser;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;
import static org.junit.Assert.*;
import com.newbuck.duckparser.parse.RString;

public class ConversionTest {

	interface StringProps {
		String strProp();
	}

	@Test public void testStringParser() {
		final String str = "Agate Christie Robin";
		StringProps props = DuckParser.build()
			.with($ -> {
				$.addProps = String.join("\n",
						"str.prop = " + str);
			})
			.create(StringProps.class);

		assertEquals(str, props.strProp());
	}

	interface PrimitiveProps {
		boolean boolProp();
		byte byteProp();
		short shortProp();
		int intProp();
		long longProp();
		double doubleProp();
		float floatProp();
	}

	@Test public void testPrimitives() {
		PrimitiveProps props = DuckParser.build()
			.with($ -> {
				$.addProps = String.join("\n",
						"bool.prop = true",
						"byte.prop = 63",
						"short.prop = 8192",
						"int.prop = 12345",
						"long.prop = 1234567890",
						"double.prop = 9.001E-8",
						"float.prop = 13.333");
			})
			.create(PrimitiveProps.class);

		assertEquals(true, props.boolProp());
		assertEquals(63, props.byteProp());
		assertEquals(8192, props.shortProp());
		assertEquals(12345, props.intProp());
		assertEquals(1234567890, props.longProp());
		assertEquals(9.001e-8, props.doubleProp(), .0000001);
		assertEquals(13.333, props.floatProp(), .001);
	}

	interface BoxedAndStaticProps {
		Boolean boolProp();
		Byte byteProp();
		Short shortProp();
		Integer intProp();
		Long longProp();
		Double doubleProp();
		Float floatProp();
		Month theCruellestMonth(); // enum
	}

	@Test public void testBoxedAndStaticCreator() {
		BoxedAndStaticProps props = DuckParser.build()
			.with($ -> {
				$.addProps = String.join("\n",
						"bool.prop = true",
						"byte.prop = 63",
						"short.prop = 8192",
						"int.prop = 12345",
						"long.prop = 1234567890",
						"double.prop = 9.001E-8",
						"float.prop = 13.333",
						"the.cruellest.month = APRIL");
			})
			.create(BoxedAndStaticProps.class);

		assertEquals(Boolean.valueOf(true), props.boolProp());
		assertEquals(Byte.valueOf((byte) 63), props.byteProp());
		assertEquals(Short.valueOf((short) 8192), props.shortProp());
		assertEquals(Integer.valueOf(12345), props.intProp());
		assertEquals(Long.valueOf(1234567890), props.longProp());
		assertEquals(Double.valueOf(9.001e-8), props.doubleProp(), .0000001);
		assertEquals(Float.valueOf(13.333f), props.floatProp(), .001);
		assertEquals(Month.APRIL, props.theCruellestMonth());
	}

	interface StringCtrProps {
		RString rstrProp();
	}

	@Test public void testStringCtr() {
		StringCtrProps props = DuckParser.build()
			.with($ -> {
				$.addProps = String.join("\n",
						"rstr.prop = foobar");
			})
			.create(StringCtrProps.class);

		assertEquals("raboof", props.rstrProp().toString());
	}

	interface PrimitiveArrayProps {
		boolean[] boolArray();
		byte[] byteArray();
		short[] shortArray();
		int[] intArray();
		long[] longArray();
		double[] doubleArray();
		float[] floatArray();
	}

	@Test public void testPrimitiveArray() {
		PrimitiveArrayProps props = DuckParser.build()
			.with($ -> {
				$.addProps = String.join("\n",
						"bool.array = true, false",
						"byte.array = 63, 31, 15",
						"short.array = 8192, 16001",
						"int.array = 1, 2, 3, 4, 5",
						"long.array = 1234567890, -999",
						"double.array = 9.001E-8, 2.99792e8",
						"float.array = 13.333, 4.4f");
			})
			.create(PrimitiveArrayProps.class);

		assertArrayEquals(new boolean[]{true,false}, props.boolArray());
		assertArrayEquals(new byte[]{63,31,15}, props.byteArray());
		assertArrayEquals(new short[]{8192,16001}, props.shortArray());
		assertArrayEquals(new int[]{1,2,3,4,5}, props.intArray());
		assertArrayEquals(new long[]{1234567890,-999}, props.longArray());
		assertArrayEquals(new double[]{9.001E-8,2.99792e8}, props.doubleArray(), .000001);
		assertArrayEquals(new float[]{13.333f,4.4f}, props.floatArray(), .001f);
	}

	interface BoxedArrayProps {
		Boolean[] boolArray();
		Byte[] byteArray();
		Short[] shortArray();
		Integer[] intArray();
		Long[] longArray();
		Double[] doubleArray();
		Float[] floatArray();
	}

	@Test public void testBoxedArray() {
		BoxedArrayProps props = DuckParser.build()
			.with($ -> {
				$.addProps = String.join("\n",
						"bool.array = true, false",
						"byte.array = 63, 31, 15",
						"short.array = 8192, 16001",
						"int.array = 1, 2, 3, 4, 5",
						"long.array = 1234567890, -999",
						"double.array = 9.001E-8, 2.99792e8",
						"float.array = 13.333, 4.4f");
			})
			.create(BoxedArrayProps.class);

		assertArrayEquals(new Boolean[]{true,false}, props.boolArray());
		assertArrayEquals(new Byte[]{63,31,15}, props.byteArray());
		assertArrayEquals(new Short[]{8192,16001}, props.shortArray());
		assertArrayEquals(new Integer[]{1,2,3,4,5}, props.intArray());
		assertArrayEquals(new Long[]{1234567890L,-999L}, props.longArray());
		assertArrayEquals(new Double[]{9.001E-8,2.99792e8}, props.doubleArray());
		assertArrayEquals(new Float[]{13.333f,4.4f}, props.floatArray());
	}

	interface ArrayProps {
		String[] strArray();
		RString[] rstrArray();
	}

	@Test public void testArray() {
		ArrayProps props = DuckParser.build()
			.with($ -> {
				$.addProps = String.join("\n",
						"str.array = barney, fred",
						"rstr.array = foo, bar");
			})
			.create(ArrayProps.class);

		assertArrayEquals(new String[]{"barney", "fred"}, props.strArray());
		assertArrayEquals(new RString[]{new RString("foo"), new RString("bar")}, props.rstrArray());
	}

	interface CollectionsProps {
		// interfaces
		List<Integer> intList();
		Set<Integer> intSet();
		SortedSet<String> strSortedSet();
		// concrete
		ArrayList<Long> longList();
		HashSet<Double> dblSet();
		TreeSet<Double> dblSortedSet();
	}

	@Test public void testCollections() {
		CollectionsProps props = DuckParser.build()
			.with($ -> {
				$.addProps = String.join("\n",
						"int.list = 1, 2, 3, 4, 5",
						"int.set = 1, 3, 5",
						"str.sorted.set = 5,2,3,1,4",
						"long.list = 1234567890, -999",
						"dbl.set = 9.001E-8, 2.99792e8",
						"dbl.sorted.set = 2.99792e8, 9.001E-8");
			})
			.create(CollectionsProps.class);

		assertArrayEquals(new Integer[]{1,2,3,4,5}, props.intList().toArray(new Integer[props.intList().size()]));
		assertTrue(props.intSet().size() == 3);
		assertTrue(props.intSet().contains(1));
		assertTrue(props.intSet().contains(3));
		assertTrue(props.intSet().contains(5));
		assertArrayEquals(new String[]{"1","2","3","4","5"}, props.strSortedSet().toArray(new String[props.strSortedSet().size()]));
		assertArrayEquals(new Long[]{1234567890L,-999L}, props.longList().toArray(new Long[props.longList().size()]));
		assertArrayEquals(new Double[]{9.001E-8,2.99792e8}, props.dblSet().toArray(new Double[props.dblSet().size()]));
		assertArrayEquals(new Double[]{9.001E-8,2.99792e8}, props.dblSortedSet().toArray(new Double[props.dblSortedSet().size()]));
	}
}
