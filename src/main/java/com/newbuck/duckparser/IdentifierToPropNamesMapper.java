// Copyright (C) 2018-2019 Brandon Lee. BSD-3-Clause Licensed, see LICENSE.
package com.newbuck.duckparser;
import java.util.ArrayList;
import java.util.List;

/**
 * Map identifiers to list of potential properties names.
 *
 * Field name to property key name mapping rules, in order of precedence (below examples
 * are field name =&gt; property resource key):
 *  - Underscores are as is (no conversion)
 *    - `db_userName` =&gt; `db_userName`
 *  - Underscores are converted to dots
 *    - `db_userName` =&gt; `db.userName`
 *
 *  - All camel case are as is (no conversion)
 *    - `dbUsername` =&gt; `dbUsername`
 *  - All camel case are converted on case transitions to dots. Lower to upper case
 *    transitions are lower cased <i>unless</i> for consecutive upper case characters which are left
 *    in upper case. Also, any letter to number (or vice-versa) is considered a transition. The only
 *    other legal character in a Java variable is `$` which is converted to a dot.
 *
 *    - `dbUsername` =&gt; `db.username`
 *    - `restAPI` =&gt; `rest.API`
 *    - `restApi` =&gt; `rest.api`
 *    - `RestApi` =&gt; `rest.api`
 *    - `RESTApi` =&gt; `RESTA.pi` (should not combine)
 *    - `db$user`  =&gt; `db.user`
 *
 * Note that numbers can either be part of the previous word or a separate word by itself.
 * Both are tried in order of single word then separate words:
 *  - `db1user`  =&gt; `db1.user` OR `db.1.user`
 *  - `db12user` =&gt; `db12.user` OR `db.12.user`
 *
 * Note that fields can either be snake case or camel case, but _not_ both. The presence of any
 * underscore means to use only the snake case rules (1,2) above.
 *
 * If params.ignoreCase, then name comparisons _ignore_ case in both field names _and_ property
 * resource keys.
 */
class IdentifierToPropNamesMapper {
	/**
	 * Return list of possible prop names from the identifier.
	 * Can be camel case or underscore, but not both - any underscore `_` means snake case; i.e.
	 * cannot be combined. If the identifier has a dot `.` (obviously not a Java identifier),
	 * then the identifier is taken literally.
	 */
	public static List<String> map(String identifier) {
		List<String> li = new ArrayList<>();
		li.add(identifier); // all strategies use identifier as is as 1st try

		boolean isSnake = false;
		boolean isDot = false;
		for (int i = 0; i < identifier.length(); ++i) {
			if (identifier.charAt(i) == '_') {
				isSnake = true;
			} else if (identifier.charAt(i) == '.') {
				isDot = true;
			}
		}
		if (isDot) { // use literal mapping
			li.add(identifier);
		}
		else if (isSnake) {
			li.add(identifier.replaceAll("_", "."));
		} else {
			// camel case has potentially multiple candidate names
			li.addAll(camelCaseToPropNames(identifier));
		}
		return li;
	}

	private static List<String> camelCaseToPropNames(String identifier) {
		List<String> parts = new ArrayList<>();
		for (int i = 0; i < identifier.length();) {
			char ch = identifier.charAt(i);
			if (ch == '$') {
				++i;
				continue; // ignore
			}

			if (Character.isLetter(ch)) {
				// ch is a letter:
				//  (1) if upper, and next is upper, then look for all uppers, append
				//  (2) if upper, and next is lower, then look for all lowers, append lc
				//  (3) if lower, then look for all lowers, append
				int j = i + 1;
				if (j == identifier.length()) {
					parts.add(String.valueOf(ch));
				} else if (Character.isUpperCase(ch)) {
					if (Character.isUpperCase(identifier.charAt(j))) { // (1)
						while (++j < identifier.length()) {
							if (!Character.isUpperCase(identifier.charAt(j)))
								break;
						}
						parts.add(identifier.substring(i, j));
					} else { // is lower (2)
						while (++j < identifier.length()) {
							if (!Character.isLowerCase(identifier.charAt(j)))
								break;
						}
						parts.add(String.valueOf(Character.toLowerCase(ch))
								+ identifier.substring(i+1, j));
					}
				} else { // is lower (3)
					while (++j < identifier.length()) {
						if (!Character.isLowerCase(identifier.charAt(j)))
							break;
					}
					parts.add(identifier.substring(i, j));
				}
				i = j;
			} else if (Character.isDigit(ch)) { // append all numbers
				int j = i;
				while (++j < identifier.length()) {
					if (!Character.isDigit(identifier.charAt(j)))
						break;
				}
				parts.add(identifier.substring(i, j));
				i = j;
			} else {
				throw new IllegalStateException("Illegal Java identifier char `" + ch + "`");
			}
		}


		// now join parts w/ _and_ w/o numbers as part of previous word
		StringBuilder combined = new StringBuilder(parts.get(0)); // with
		for (int i = 1; i < parts.size(); ++i) {
			String word = parts.get(i);
			if (word.chars().allMatch(Character::isDigit)) {
				combined.append(word);
			} else {
				combined.append('.').append(word);
			}
		}

		List<String> li = new ArrayList<>();
		String separated = String.join(".", parts);

		// combined (\d+ w/ previous word) if different will be shorter, has precedence
		if (combined.length() < separated.length()) {
			li.add(combined.toString());
		}
		li.add(separated);
		return li;
	}
}
