// Copyright (C) 2018-2019 Brandon Lee. BSD-3-Clause Licensed, see LICENSE.
package com.newbuck.duckparser;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target({METHOD})
@interface Alias {
	String[] value();
}
