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

import com.moparisthebest.jbgjob.BackgroundJob;

import java.util.concurrent.atomic.AtomicInteger;

public class PrintingJob implements BackgroundJob<PrintDTO> {

	private static final AtomicInteger instanceCount = new AtomicInteger(-1);

	private final int instance;

	public PrintingJob() {
		System.out.println("new instance of PrintingJob created in thread: " + Thread.currentThread().getName());
		instance = instanceCount.incrementAndGet();
	}

	@Override
	public void process(PrintDTO dto) {
		System.out.printf("++++++++++++\nPrintingJob(%d): %s\ncurrent thread: %s\n--------------\n", instance, dto, Thread.currentThread().getName());
		try {
			Thread.sleep(dto.getSleep());
		} catch (InterruptedException e) {
		}
		if (dto.isThrowException())
			throw new RuntimeException("oh my, something quite unexpected happened!");
	}

	@Override
	public String toString() {
		return String.format("PrintingJob(%d)", instance);
	}
}
