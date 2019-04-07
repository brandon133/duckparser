// Copyright (C) 2018-2019 Brandon Lee. BSD-3-Clause Licensed, see LICENSE.
package com.newbuck.duckparser;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import com.newbuck.duckparser.parse.TypeParser;

class DuckParserInvocationHandler implements InvocationHandler {
	private final PropsMap propsMap;
	private final List<DelegateMethod> delegates;

	DuckParserInvocationHandler(PropsMap propsMap) {
		this.propsMap = propsMap;
		this.delegates = DelegateMethod.findDelegates(propsMap);
	}

	public Object invoke(Object proxy, Method invokedMethod, Object... args) throws Throwable {
		DelegateMethod delegate = DelegateMethod.findDelegateMethod(delegates, invokedMethod);
		if (delegate != null) {
			return delegate.invoke(args);
		}

		String methodName = invokedMethod.getName();
		String val = propsMap.getPropValue(methodName);
		if (val == null) {
			Default def = invokedMethod.getAnnotation(Default.class);
			if (def != null) {
				val = def.value();
			}
		}
		if (val == null) {
			return null;
		}

		Object parsed = TypeParser.parse(val, invokedMethod.getReturnType(), invokedMethod);
		if (parsed == TypeParser.Constants.NO_MATCH) {
			return null;
		}
		return parsed;
	}
}
