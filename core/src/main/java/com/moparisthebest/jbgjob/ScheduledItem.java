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

package com.moparisthebest.jbgjob;

import com.moparisthebest.jbgjob.pool.ObjectPool;
import com.moparisthebest.jbgjob.result.ExecutionResult;

/**
 * Storage class that can be used by an implementation of Scheduler
 */
public class ScheduledItem<T> implements Runnable {
	public final Class<? extends BackgroundJob<T>> bgClass;
	//@com.fasterxml.jackson.annotation.JsonTypeInfo(use = com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS)
	public final T dto;

	private ObjectPool pool;
	private ExecutionResult result;

	public ScheduledItem() {
		this(null, null);
	}

	public ScheduledItem(Class<? extends BackgroundJob<T>> bgClass, T dto) {
		this.bgClass = bgClass;
		this.dto = dto;
	}

	@Override
	public void run() {
		BackgroundJob<T> bgJob = null;
		try {
			bgJob = pool == null ? bgClass.newInstance() : pool.getResource(bgClass);
			bgJob.process(dto);
			if (result != null)
				result.success();
		} catch (Throwable e) {
			if (result != null)
				result.error(e);
		} finally {
			if (pool != null && bgJob != null)
				try {
					pool.releaseResource(bgJob);
				} catch (Throwable e) {
					e.printStackTrace();
				}
		}
	}

	public void setPool(ObjectPool pool) {
		this.pool = pool;
	}

	public void setResult(ExecutionResult result) {
		this.result = result;
	}

	@Override
	public String toString() {
		return "ScheduledItem{" +
				"bgClass=" + bgClass +
				", dto=" + dto +
				"} " + super.toString();
	}
}
