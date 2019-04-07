// Copyright (C) 2018-2019 Brandon Lee. BSD-3-Clause Licensed, see LICENSE.
package com.newbuck.duckparser;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class DelegateMethod {
	private final Object target;
	private final Method method;

	public DelegateMethod(Object target, Method method) {
		this.target = target;
		this.method = method;
	}

	public Object invoke(Object[] args) throws Throwable {
		try {
			return method.invoke(target, args);
		} catch (InvocationTargetException e) {
			throw e.getTargetException();
		}
	}

	public static List<DelegateMethod> findDelegates(Object... targets) {
		List<DelegateMethod> result = new ArrayList<DelegateMethod>();
		for (Object target : targets) {
			if (target == null) {
				continue;
			}
			Method[] methods = target.getClass().getMethods();
			for (Method method : methods) {
				if (method.getAnnotation(Delegate.class) != null) {
					result.add(new DelegateMethod(target, method));
				}
			}
		}
		return result;
	}

	public static DelegateMethod findDelegateMethod(List<DelegateMethod> delegates, Method invokedMethod) {
		for (DelegateMethod delegate : delegates)
		if (delegate.matches(invokedMethod)) {
			return delegate;
		}
		return null;
	}

	private boolean matches(Method invokedMethod) {
		return invokedMethod.getName().equals(method.getName())
			&& invokedMethod.getReturnType().equals(method.getReturnType())
			&& Arrays.equals(invokedMethod.getParameterTypes(), method.getParameterTypes());
	}
}
