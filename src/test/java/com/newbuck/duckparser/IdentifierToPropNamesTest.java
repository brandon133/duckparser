// Copyright (C) 2018-2019 Brandon Lee. BSD-3-Clause Licensed, see LICENSE.
package com.newbuck.duckparser;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class IdentifierToPropNamesTest {
	static class Transform {
		String fldName;
		String[] propNames;
		Transform(String fldName, String... propNames) {
			this.fldName = fldName;
			this.propNames = propNames;
		}
	}

	@Test
	public void testSnakeCase() {
		Transform[] tests = new Transform[] {
			new Transform("db_userName", "db_userName", "db.userName")
		};
		for (Transform test : tests) {
			List<String> propNames = IdentifierToPropNamesMapper.map(test.fldName);
			assertArrayEquals(test.propNames, propNames.toArray(new String[propNames.size()]));
		}
	}

	@Test
	public void testCamelCase() {
		Transform[] tests = new Transform[] {
			new Transform("dbUserName", "dbUserName", "db.user.name"),
			new Transform("restAPI", "restAPI", "rest.API"),
			new Transform("restApi", "restApi", "rest.api"),
			new Transform("RestApi", "RestApi", "rest.api"),
			new Transform("RESTApi", "RESTApi", "RESTA.pi"),
			new Transform("db$user", "db$user", "db.user"),
			new Transform("db$$$user", "db$$$user", "db.user"),
			new Transform("db$user$", "db$user$", "db.user"),
			new Transform("$db$user$", "$db$user$", "db.user"),
		};
		for (Transform test : tests) {
			List<String> propNames = IdentifierToPropNamesMapper.map(test.fldName);
			assertArrayEquals(test.propNames, propNames.toArray(new String[propNames.size()]));
		}
	}

	@Test
	public void testCamelCaseWithNumbers() {
		Transform[] tests = new Transform[] {
			new Transform("appUser1", "appUser1", "app.user1", "app.user.1"),
			new Transform("appUSER1", "appUSER1", "app.USER1", "app.USER.1"),
			new Transform("db1user", "db1user", "db1.user", "db.1.user"),
			new Transform("db123user", "db123user", "db123.user", "db.123.user"),
			// at beginning, numbers are always a separate word
			new Transform("99db1user", "99db1user", "99.db1.user", "99.db.1.user"),
		};
		for (Transform test : tests) {
			List<String> propNames = IdentifierToPropNamesMapper.map(test.fldName);
			assertArrayEquals(test.propNames, propNames.toArray(new String[propNames.size()]));
		}
	}
}
