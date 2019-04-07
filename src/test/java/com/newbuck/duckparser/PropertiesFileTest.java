// Copyright (C) 2018-2019 Brandon Lee. BSD-3-Clause Licensed, see LICENSE.
package com.newbuck.duckparser;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import org.junit.Test;
import static org.junit.Assert.*;

public class PropertiesFileTest {
	/* These are in src/test/resources/test[-default].properties:
	 *
	 * test-defaults.properties --
	 *  foo.1=default-one
	 *  foo.2=default-two
	 *  foo.3=default-three
	 *
	 * test.properties --
	 *  foo.2=two
	 *  foo.3=three
	 */

	interface DefaultProps {
		String foo_1();
		String foo_2();
		String foo_3();
	}

	@Test public void testDefaultOverrides() {
		// loaded from classpath
		DefaultProps props = DuckParser.build()
			.with("test", $ -> {
				$.addProps = String.join("\n",
						"foo.3=string-3");
			})
			.create(DefaultProps.class);

		// test-default.properties
		assertEquals("default-one", props.foo_1());
		// test.properties
		assertEquals("two", props.foo_2());
		// addProps
		assertEquals("string-3", props.foo_3());
	}

	@Test public void testSearchClasspath() {
		DefaultProps props = DuckParser.build()
			.with("test", $ -> {
				$.searchClasspath = false;
			})
			.create(DefaultProps.class);

		assertNull(props.foo_1());
		assertNull(props.foo_2());
	}

	@Test public void testSearchDefault() {
		DefaultProps props = DuckParser.build()
			.with("test", $ -> {
				$.searchDefault = false;
			})
			.create(DefaultProps.class);

		assertNull(props.foo_1());
		assertEquals("two", props.foo_2());
	}

	interface Props {
		String val1();
		String val2();
		String val3();
	}

	@Test public void testSearchDir() throws Exception {
		try {
			try (FileOutputStream fs = new FileOutputStream("/tmp/searchDir-default.properties");
				OutputStreamWriter os = new OutputStreamWriter(fs, "utf-8");
				Writer w = new BufferedWriter(os);
			) {
				w.write("f.val1 = fred\n");
				w.write("f.val2 = wilma\n");
			}
			try (FileOutputStream fs = new FileOutputStream("/tmp/searchDir.properties");
				OutputStreamWriter os = new OutputStreamWriter(fs, "utf-8");
				Writer w = new BufferedWriter(os);
			) {
				w.write("f.val2 = barney\n");
				w.write("f.val3 = betty\n");
			}

			Props props = DuckParser.build()
				.with("searchDir", $ -> {
					$.prefix = "f";
					$.searchDirs = new Path[] { Paths.get("/tmp") };
				})
				.create(Props.class);

			//System.out.println(props.toString());

			assertEquals("fred", props.val1());
			assertEquals("barney", props.val2());
			assertEquals("betty", props.val3());

		} finally {
			Path f = Paths.get("/tmp/searchDir-default.properties");
			if (Files.exists(f)) {
				Files.delete(f);
			}
			f = Paths.get("/tmp/searchDir.properties");
			if (Files.exists(f)) {
				Files.delete(f);
			}
		}
	}
}
