// Copyright (C) 2018-2019 Brandon Lee. BSD-3-Clause Licensed, see LICENSE.
package com.newbuck.duckparser.parse;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.*;
import com.newbuck.duckparser.parse.TypeParser;
import static com.newbuck.duckparser.parse.TypeParser.*;

public class TypeParserTest {
	@Test public void testStringParser() {
		final String str = "Agate Christie Robin";
		assertEquals(str, STRING.as(str, String.class));
	}

	@Test public void testPrimitives() {
		assertEquals(true,       (boolean) PRIMITIVE.as("true", Boolean.TYPE));
		assertEquals(63,         (byte) PRIMITIVE.as("63", Byte.TYPE));
		assertEquals(8192,       (short) PRIMITIVE.as("8192", Short.TYPE));
		assertEquals(12345,      (int) PRIMITIVE.as("12345", Integer.TYPE));
		assertEquals(1234567890, (long) PRIMITIVE.as("1234567890", Long.TYPE));
		assertEquals(9.001E-8,   (double) PRIMITIVE.as("9.001e-8", Double.TYPE), .0000001);
		assertEquals(13.333,     (float) PRIMITIVE.as("13.333", Float.TYPE), .001);
	}

	@Test public void testBoxedAndStaticCreator() {
		assertEquals(Boolean.valueOf(true),       (Boolean) PRIMITIVE.as("true", Boolean.class));
		assertEquals(Byte.valueOf((byte) 63),     (Byte) PRIMITIVE.as("63", Byte.class));
		assertEquals(Short.valueOf((short) 8192), (Short) PRIMITIVE.as("8192", Short.class));
		assertEquals(Integer.valueOf(12345),      (Integer) PRIMITIVE.as("12345", Integer.class));
		assertEquals(Long.valueOf(1234567890),    (Long) PRIMITIVE.as("1234567890", Long.class));
		assertEquals(Double.valueOf(9.001e-8),    (Double) PRIMITIVE.as("9.001e-8", Double.class), .0000001);
		assertEquals(Float.valueOf(13.333f),      (Float) PRIMITIVE.as("13.333", Float.class), .001);

		assertEquals(Boolean.valueOf(true),       (Boolean) STATIC_CREATOR.as("true", Boolean.class));
		assertEquals(Byte.valueOf((byte) 63),     (Byte) STATIC_CREATOR.as("63", Byte.class));
		assertEquals(Short.valueOf((short) 8192), (Short) STATIC_CREATOR.as("8192", Short.class));
		assertEquals(Integer.valueOf(12345),      (Integer) STATIC_CREATOR.as("12345", Integer.class));
		assertEquals(Long.valueOf(1234567890),    (Long) STATIC_CREATOR.as("1234567890", Long.class));
		assertEquals(Double.valueOf(9.001e-8),    (Double) STATIC_CREATOR.as("9.001e-8", Double.class), .0000001);
		assertEquals(Float.valueOf(13.333f),      (Float) STATIC_CREATOR.as("13.333", Float.class), .001);
	}

	enum Fruit { Apple, Orange, Banana, Pear };

	@Test public void testEnum() {
		assertEquals(Fruit.Banana, STATIC_CREATOR.as("Banana", Fruit.class));
	}

	@Test public void testStringConstructor() {
		assertEquals("raboof", STRING_CTR.as("foobar", RString.class).toString());
	}

	@Test public void testPrimitiveArray() {
		assertArrayEquals(new boolean[]{true,false},        ARRAY.as("true,false", boolean[].class));
		assertArrayEquals(new byte[]{63,31,15},             ARRAY.as("63,31,15", byte[].class));
		assertArrayEquals(new short[]{8192,16001},          ARRAY.as("8192,16001", short[].class));
		assertArrayEquals(new int[]{1,2,3,4,5},             ARRAY.as("1,2,3,4,5", int[].class));
		assertArrayEquals(new long[]{1234567890,-999},      ARRAY.as("1234567890,-999", long[].class));
		assertArrayEquals(new double[]{9.001E-8,2.99792e8}, ARRAY.as("9.001E-8,2.99792e8", double[].class), .000001);
		assertArrayEquals(new float[]{13.333f,4.4f},        ARRAY.as("13.333,4.4f", float[].class), .001f);
	}

	@Test public void testBoxedArray() {
		assertArrayEquals(new Boolean[]{true,false},        ARRAY.as("true,false", Boolean[].class));
		assertArrayEquals(new Byte[]{63,31,15},             ARRAY.as("63,31,15", Byte[].class));
		assertArrayEquals(new Short[]{8192,16001},          ARRAY.as("8192,16001", Short[].class));
		assertArrayEquals(new Integer[]{1,2,3,4,5},         ARRAY.as("1,2,3,4,5", Integer[].class));
		assertArrayEquals(new Long[]{1234567890L,-999L},    ARRAY.as("1234567890,-999", Long[].class));
		assertArrayEquals(new Double[]{9.001E-8,2.99792e8}, ARRAY.as("9.001E-8,2.99792e8", Double[].class));
		assertArrayEquals(new Float[]{13.333f,4.4f},        ARRAY.as("13.333,4.4f", Float[].class));
	}

	@Test public void testArray() {
		assertArrayEquals(new String[]{"barney","fred"}, ARRAY.as("barney,fred", String[].class));
		assertArrayEquals(new RString[]{new RString("foo"),new RString("bar")},
				ARRAY.as("foo,bar", RString[].class));
	}

	interface IfacePkg {
		List<Integer> intList();
		Set<Integer> intSet();
		SortedSet<Integer> intSortedSet();
	}
	@Test public void testIfaceCollections() throws Exception {
		// collection type parsing requires field, you wouldn't create collections this way
		// (via field obj) but here for testing

		Method method = IfacePkg.class.getDeclaredMethod("intList");
		Collection<?> co = COLLECTION.as("1,2", List.class, method);
		assertArrayEquals(new Integer[]{1,2}, co.toArray());

		method = IfacePkg.class.getDeclaredMethod("intSet");
		Set<?> set = COLLECTION.as("2,1", Set.class, method);
		assertArrayEquals(new Integer[]{2,1}, set.toArray());
		assertTrue(set.size() == 2 && set.contains(1) && set.contains(2));

		method = IfacePkg.class.getDeclaredMethod("intSortedSet");
		set = COLLECTION.as("2,1", SortedSet.class, method);
		assertArrayEquals(new Integer[]{1,2}, set.toArray()); // sorted
		assertTrue(set.size() == 2 && set.contains(1) && set.contains(2));

		// the as() method only works on COLLECTION enum
		co = COLLECTION.as("1,2", List.class, Integer.class);
		assertArrayEquals(new Integer[]{1,2}, co.toArray());

		set = (Set) COLLECTION.as("2,1", Set.class, Integer.class);
		assertArrayEquals(new Integer[]{2,1}, set.toArray());
		assertTrue(set.size() == 2 && set.contains(1) && set.contains(2));

		set = (Set) COLLECTION.as("2,1", SortedSet.class, Integer.class);
		assertArrayEquals(new Integer[]{1,2}, set.toArray()); // sorted
		assertTrue(set.size() == 2 && set.contains(1) && set.contains(2));
	}

	interface ConcretePkg {
		ArrayList<String> strList();
		LinkedHashSet<RString> rstrSet();
		TreeSet<Double> dblSortedSet();
	}

	@Test public void testConcreteCollections() throws Exception {
		// collection type parsing requires field, you wouldn't create collections this way
		// (via field obj) but here for testing

		Method method = ConcretePkg.class.getDeclaredMethod("strList");
		Collection<?> co = COLLECTION.as("Frankie,and,Johnny,were,sweethearts", ArrayList.class, method);
		assertTrue(co instanceof List);
		assertArrayEquals(new String[]{"Frankie","and","Johnny","were","sweethearts"}, co.toArray());
		co = COLLECTION.as("Frankie,and,Johnny,were,sweethearts", ArrayList.class, String.class);
		assertArrayEquals(new String[]{"Frankie","and","Johnny","were","sweethearts"}, co.toArray());

		method = ConcretePkg.class.getDeclaredMethod("rstrSet");
		co = COLLECTION.as("Frankie,and,Johnny,were,sweethearts", LinkedHashSet.class, method);
		assertTrue(co instanceof Set);
		Set<String> set = co.stream().map(it -> it.toString()).collect(Collectors.toSet());
		assertTrue(set.size() == 5 && set.contains("straehteews") && set.contains("erew")
				&& set.contains("ynnhoJ") && set.contains("dna") && set.contains("eiknarF"));
		//assertArrayEquals(new String[]{"Frankie","and","Johnny","were","sweethearts"}, stra);
		co = COLLECTION.as("Frankie,and,Johnny,were,sweethearts", LinkedHashSet.class, RString.class);
		assertTrue(co instanceof Set);
		set = co.stream().map(it -> it.toString()).collect(Collectors.toSet());
		assertTrue(set.size() == 5 && set.contains("straehteews") && set.contains("erew")
				&& set.contains("ynnhoJ") && set.contains("dna") && set.contains("eiknarF"));

		method = ConcretePkg.class.getDeclaredMethod("dblSortedSet");
		co = COLLECTION.as("1.2,3.4e-1,5.6e-2", TreeSet.class, method);
		assertTrue(co instanceof SortedSet);
		System.out.println(co);
		Double[] dbla = co.stream().collect(Collectors.toList()).toArray(new Double[co.size()]);
		assertArrayEquals(new Double[]{5.6e-2,3.4e-1,1.2}, dbla);
	}
}
