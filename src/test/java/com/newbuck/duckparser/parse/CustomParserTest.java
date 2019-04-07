// Copyright (C) 2018-2019 Brandon Lee. BSD-3-Clause Licensed, see LICENSE.
package com.newbuck.duckparser.parse;
import java.time.Instant;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.newbuck.duckparser.parse.TypeParser.*;

public class CustomParserTest {
	@Test public void testStringCtr() {
		assertEquals("!dlrow olleH", STRING_CTR.as("Hello world!", RString.class).toString());
	}

	static class InstantParser implements Parser<Instant> {
		@Override
		public Instant parse(String val) {
			return Instant.parse(val);
		}

	}

	@Test public void testAddParser() {
		final String INST = "2007-12-03T10:15:30.00Z";
		Instant inst = Instant.parse(INST);

		Instant parsed = PARSERS.as(INST, Instant.class);
		assertNull(parsed);

		TypeParser.addParsers(new InstantParser());
		parsed = PARSERS.as(INST, Instant.class);
		assertEquals(inst, parsed);
	}
}
