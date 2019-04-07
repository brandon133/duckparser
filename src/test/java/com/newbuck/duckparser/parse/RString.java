// Copyright (C) 2018-2019 Brandon Lee. BSD-3-Clause Licensed, see LICENSE.
package com.newbuck.duckparser.parse;
import java.util.Objects;

// reversed string
public class RString {
	private String rstr;
	public RString(String str) {
		rstr = new StringBuilder(str).reverse().toString();
	}
	@Override
	public boolean equals(Object rhs) {
		if (rhs == this)
			return true;
		return (rhs instanceof RString)
			? Objects.equals(rstr, rhs.toString())
			: false;
	}
	@Override
	public int hashCode() {
		return Objects.hash(rstr);
	}
	@Override
	public String toString() {
		return rstr;
	}
}
