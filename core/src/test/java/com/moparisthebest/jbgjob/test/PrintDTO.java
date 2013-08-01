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

package com.moparisthebest.jbgjob.test;

public class PrintDTO {
	private static final int maxSleep = 10000;
	private static final int minSleep = 2000;

	private final String message;
	private final int sleep = minSleep + (int) (Math.random() * ((maxSleep - minSleep) + 1));
	private final boolean throwException;

	public PrintDTO() {
		this(false);
	}

	public PrintDTO(boolean throwException) {
		this("default test message", throwException);
	}

	public PrintDTO(String message) {
		this(message, false);
	}

	public PrintDTO(String message, boolean throwException) {
		this.message = message;
		this.throwException = throwException;
	}

	public String getMessage() {
		return message;
	}

	public int getSleep() {
		return sleep;
	}

	public boolean isThrowException() {
		return throwException;
	}

	@Override
	public String toString() {
		return "PrintDTO{" +
				"message='" + message + '\'' +
				", sleep=" + sleep +
				", throwException=" + throwException +
				"} " + super.toString();
	}
}
