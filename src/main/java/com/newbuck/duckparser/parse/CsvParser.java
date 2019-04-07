// Copyright (C) 2018-2019 Brandon Lee. BSD-3-Clause Licensed, see LICENSE.
package com.newbuck.duckparser.parse;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Parse CSV into fields.
 * Parses standard CSV which is goofy: https://tools.ietf.org/html/rfc4180
 * More: https://en.wikipedia.org/wiki/Comma-separated_values
 *
 * Default separator `,` (comma) and quote `"` (double quote) from standard. The ctr has arguments
 * to override these.
 *
 * Extensions:
 *   - spaces are _always_ trimmed around the separator:
 *     `a, b`    => [`a`, `b`]
 *     `a, " b"` => [`a`, ` b`]
 *   - quotes can be anywhere in the field and all characters inside quotes, including commas and
 *     newlines are escaped:
 *       `a","b`  => [`a,b`]
 *       `a"\n"b` => [`a\nb`]
 *     BUT, to escape a quote, 2 quotes in a row must be quoted (i.e. _inside_ quotes):
 *       `a""b`   => [`ab`]
 *       `"a""b"` => [`a"b`]
 *       `a""""b` => [`a"b`]
 *
 *       `"a"b",c$` => [`ab,c`] -- note 3 quotes so unclosed and everything is a single field,
 *                                 usually this results in entire or partial file being mismatched
 *
 * See examples in tests.
 *
 * Impl modified from https://agiletribe.wordpress.com/2012/11/23/the-only-class-you-need-for-csv-files/
 * and https://www.mkyong.com/java/how-to-read-and-parse-csv-file-in-java/
 */
public class CsvParser {
	public static final char DEFAULT_SEPARATOR = ',';
	public static final char DEFAULT_QUOTE = '"';

	public static List<String> readLine(Reader r) throws IOException {
		return readLine(r, DEFAULT_SEPARATOR, DEFAULT_QUOTE);
	}

	// returns null if reader started empty
	public static List<String> readLine(Reader r, char sep, char quote) throws IOException {
		List<String> fields = new ArrayList<>();
		StringBuilder buf = new StringBuilder();
		boolean inQuotes = false;
		boolean started = false;
		int numSpaces = 0;

		int prev = -1;
		int ch = r.read();
		if (ch < 0) {
			return null;
		}

		while (ch > 0) {
			if (inQuotes) {
				started = true;
				if (ch == quote) {
					// even number of quotes
					inQuotes = false;
				} else {
					buf.append((char) ch);
				}
			} else {
				if (ch == quote) {
					// odd number of quotes
					inQuotes = true;
					if (started && prev == quote) {
						// escaped quotes (2 quotes in a row w/in a quote
						for (; numSpaces > 0; --numSpaces) {
							buf.append(' ');
						}
						buf.append(quote);
					}
				} else if (ch == ' ') {
					if (started || buf.length() > 0) {
						++numSpaces;
					}
				} else if (ch == sep) {
					fields.add(buf.toString());
					buf = new StringBuilder();
					started = false;
					numSpaces = 0;
				} else if (ch == '\r') {
					//ignore LF characters
				} else if (ch == '\n') {
					//end of a line, break out
					break;
				} else {
					for (; numSpaces > 0; --numSpaces) {
						buf.append(' ');
					}
					buf.append((char) ch);
				}
			}
			prev = ch;
			ch = r.read();
		}
		fields.add(buf.toString());
		return fields;
	}
}
