// Copyright (C) 2018-2019 Brandon Lee. BSD-3-Clause Licensed, see LICENSE.
package com.newbuck.duckparser;
import java.lang.reflect.Field;
import java.util.Arrays;

public class TestUtility {
	// builds a verbose descr of obj
	public static String reflToString(Object obj) {
		StringBuilder buf = new StringBuilder();
		buf.append(obj.getClass().getName())
			.append(" {");

		// determine fields declared in this class only (no fields of superclass)
		Field[] fields = obj.getClass().getDeclaredFields();

		int max = 0;
		for (Field fld : fields) {
			int len = fld.getName().length();
			if (len > max)
				max = len;
		}
		String fmt = "%-" + max + "s  %s";

		// print field names paired with their values
		for (int i = 0; i < fields.length; ++i) {
			Field fld = fields[i];
			buf.append("\n\t");
			try {
				fld.setAccessible(true);
				Object fldval = fld.get(obj);
				String strval = null;
				if (fldval != null) {
					if (fld.getType() == String.class) {
						strval = "\"" + fldval + "\"";
					} else if (fld.getType().isArray()) {
						strval = Arrays.toString((Object[]) fldval);
					} else {
						strval = fldval.toString();
					}
				}
				buf.append(String.format(fmt, fld.getName(), strval));
			} catch (IllegalAccessException e) {
				throw new RuntimeException("Illegal Access on field: " + fld, e);
			}
		}
		buf.append("\n}");
		return buf.toString();
	}
}
