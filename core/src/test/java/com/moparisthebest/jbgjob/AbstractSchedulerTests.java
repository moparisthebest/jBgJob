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

import com.moparisthebest.jbgjob.test.PrintDTO;
import com.moparisthebest.jbgjob.test.PrintDTOChild;
import com.moparisthebest.jbgjob.test.PrintingJob;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

public class AbstractSchedulerTests {
	public static Scheduler bg;


	@AfterClass
	public static void closeBg() throws Throwable {
		if (bg != null)
			bg.close();
	}

	@Test
	public void testScheduleDTO() throws Throwable {
		Assert.assertTrue(bg.schedule(PrintingJob.class, new PrintDTO()));
	}

	@Test
	public void testScheduleChildDTO() throws Throwable {
		Assert.assertTrue(bg.schedule(PrintingJob.class, new PrintDTOChild()));
	}

	@Test
	public void testScheduleErrorCausingDTO() throws Throwable {
		Assert.assertTrue(bg.schedule(PrintingJob.class, new PrintDTO(true)));
	}

	@Test
	public void testFastJobs() throws Throwable {
		int x = 0;
		for (; x < 10; ++x)
			Assert.assertTrue(bg.schedule(PrintingJob.class, new PrintDTO("fastJob " + x)));
		Thread.sleep(5000);
		for (; x < 20; ++x)
			Assert.assertTrue(bg.schedule(PrintingJob.class, new PrintDTO("fastJob " + x)));
	}
}
