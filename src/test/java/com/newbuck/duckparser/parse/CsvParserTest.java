// Copyright (C) 2018-2019 Brandon Lee. BSD-3-Clause Licensed, see LICENSE.
package com.newbuck.duckparser.parse;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import com.newbuck.duckparser.parse.CsvParser;

// tests from https://www.mkyong.com/java/how-to-read-and-parse-csv-file-in-java/
public class CsvParserTest {

	// sample file reader that reads CSV file and prints it as separate fields
	public static void main(String[] args) throws Exception {
		for (String arg : args) {
			File file = new File(arg);
			try (FileInputStream fis = new FileInputStream(file);
			     Reader fr = new InputStreamReader(fis, "UTF-8")) {
				while (true) {
					List<String> fields = CsvParser.readLine(fr);
					if (fields == null)
						break;
					System.out.println(fields);
				}
			}
		}
	}

	private void doTest(String line, String... expected) throws Exception {
		List<String> csv = CsvParser.readLine(new StringReader(line));
		assertNotNull(csv);
		assertEquals(expected.length, csv.size());
		for (int i = 0; i < expected.length; ++i) {
			assertEquals(expected[i], csv.get(i));
		}
	}

	@Test
	public void testNoQuotes() throws Exception {
		doTest("10,AU,Australia", "10", "AU", "Australia");
	}

	@Test
	public void testQuoted() throws Exception {
		doTest("\"10\",\"AU\",\"Australia\"", "10", "AU", "Australia");
		// with comma
		doTest("\"10\",\"AU\",\"Aus,tralia\"", "10", "AU", "Aus,tralia");
	}

	@Test
	public void testDblQuoted() throws Exception {
		doTest("\"10\",\"AU\",\"Aus\"\"tralia\"", "10", "AU", "Aus\"tralia");
	}

	@Test
	public void testUnescapedDblQuotesIgnored() throws Exception {
		doTest("10,AU,Aus\"tralia,foo", "10", "AU", "Australia,foo");
		doTest("10,AU,Aus\"\"tralia",   "10", "AU", "Australia");

		doTest("a\"b,c\"\"d\"", "ab,c\"d");
		doTest("ab\"",          "ab");
		doTest("a\"\"b,c",      "ab", "c");
		doTest("a\"\"\"\"b,c",  "a\"b", "c");
		doTest("a\"\"\"b,c",    "a\"b,c");
	}

	@Test
	public void testTrimmedSpaces() throws Exception {
		doTest("10, AU,  Aus ,  Australia ", "10", "AU", "Aus", "Australia");
		doTest("a,\" b\",\"c \",\" d \"", "a", " b", "c ", " d ");
	}

	@Test
	public void testEmpty() throws Exception {
		doTest("\n",  "");
		doTest(" ",   "");
		doTest(",",   "", "");
		doTest(",,,", "", "", "", "");
		doTest("a,,", "a", "", "");
		doTest(",a,", "", "a", "");
		doTest(",,a", "", "", "a");
	}

	@Test
	public void testUnclosed() throws Exception {
		doTest("\"a\"b\",c", "ab,c");
	}

	@Test
	public void testRead() throws Exception {
		StringReader r = new StringReader(" ");
		List<String> csv = CsvParser.readLine(r);
		csv = CsvParser.readLine(r);
		csv = CsvParser.readLine(r);
	}
}
