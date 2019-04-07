// Copyright (C) 2018-2019 Brandon Lee. BSD-3-Clause Licensed, see LICENSE.
package com.newbuck.duckparser;

/**
 * Interface to access the underlying properties store.
 */
public interface ParserStore {
	public <T> T as(String identifier, Class<T> clz);
}
