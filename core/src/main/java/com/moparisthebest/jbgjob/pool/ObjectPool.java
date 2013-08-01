/*
 * jBgJob (Java Background Job) lets you schedule Java jobs to be ran in the background.  They can run in any
 * combination of other threads in the same JVM, other JVMs, or multiple other JVMs, even on different machines.
 * Copyright (C) 2013 Travis Burtrum (moparisthebest)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package com.moparisthebest.jbgjob.pool;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents pools of objects grouped by Class, if one isn't available when requested a new one is constructed with a default constructor.
 */
public class ObjectPool {

	protected final int queueSize;
	protected final Map<Class, Deque> pool = new HashMap<Class, Deque>();

	public ObjectPool() {
		this(16);
	}

	public ObjectPool(int queueSize) {
		this.queueSize = queueSize;
	}

	@SuppressWarnings({"unchecked"})
	public <T> T getResource(Class<T> obClass) throws IllegalAccessException, InstantiationException {
		if (obClass == null)
			throw new NullPointerException("Class cannot be null!");
		synchronized (pool) {
			Deque deque = pool.get(obClass);
			if (deque == null) {
				deque = new ArrayDeque(queueSize);
				pool.put(obClass, deque);
			}
			return deque.isEmpty() ? obClass.newInstance() : (T) deque.pop();
		}

	}

	@SuppressWarnings({"unchecked"})
	public <T> void releaseResource(T resource) {
		if (resource == null)
			throw new NullPointerException("Released resource cannot be null!");
		synchronized (pool) {
			pool.get(resource.getClass()).push(resource);
			System.out.println("pool: " + pool);
		}
	}

	@Override
	public String toString() {
		return "ObjectPool{" +
				"queueSize=" + queueSize +
				", pool=" + pool +
				"} " + super.toString();
	}
}
