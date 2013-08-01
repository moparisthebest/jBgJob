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

/**
 * This implementation of Scheduler has a thread pool with a configurable number of threads (default 5) with which to process the DTO inside this JVM.
 * <p/>
 * This implementation ignores 'queue', all Jobs are put in the same queue.
 */
public class ThreadScheduler extends AbstractScheduler {

	private static final ScheduledItemExecutor executor = new ScheduledItemExecutor(false);

	@Override
	public <T> boolean schedule(final String queue, final ScheduledItem<T> scheduledItem) {
		super.schedule(queue, scheduledItem);
		try {
			executor.execute(scheduledItem);
			return true;
		} catch (Throwable e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public void close() {
		super.close();
		executor.close();
	}
}
