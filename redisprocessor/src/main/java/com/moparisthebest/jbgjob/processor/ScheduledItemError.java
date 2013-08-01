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

package com.moparisthebest.jbgjob.processor;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Represents an Exception and a serialized ScheduledItem to put in the Redis error queue
 */
public class ScheduledItemError {
	private final long date = System.currentTimeMillis();
	private final String exception;
	private final String scheduledItemString;

	public ScheduledItemError(final Throwable e, final String scheduledItemString) {
		this.scheduledItemString = scheduledItemString;
		final StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		this.exception = sw.toString();
	}

	public long getDate() {
		return date;
	}

	public String getException() {
		return exception;
	}

	public String getScheduledItemString() {
		return scheduledItemString;
	}

	@Override
	public String toString() {
		return "ScheduledItemError{" +
				"date=" + date +
				", exception='" + exception + '\'' +
				", scheduledItemString='" + scheduledItemString + '\'' +
				"} " + super.toString();
	}
}
