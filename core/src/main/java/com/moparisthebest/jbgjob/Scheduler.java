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

import java.io.Closeable;

/**
 * This schedules a DTO to be processed by an instance of a particular BackgroundJob
 * <p/>
 * This class needs to be entirely Thread-Safe, it is generally meant to be declared final and static and accessed from a single place.
 */
public interface Scheduler extends Closeable {
	/**
	 * This schedules a DTO to be processed by an instance of a particular BackgroundJob in the default queue.
	 *
	 * @param bgClass BackgroundJob to process the DTO
	 * @param dto     to be processed in the background
	 * @param <T>     Type of DTO
	 * @return true if scheduling was successful, false otherwise
	 */
	public <T> boolean schedule(final Class<? extends BackgroundJob<T>> bgClass, final T dto);

	/**
	 * This schedules a DTO to be processed by an instance of a particular BackgroundJob in the specified queue.
	 *
	 * @param bgClass BackgroundJob to process the DTO
	 * @param dto     to be processed in the background
	 * @param <T>     Type of DTO
	 * @return true if scheduling was successful, false otherwise
	 */
	public <T> boolean schedule(final String queue, final Class<? extends BackgroundJob<T>> bgClass, final T dto);

	public <T> boolean schedule(final String queue, final ScheduledItem<T> scheduledItem);
}
