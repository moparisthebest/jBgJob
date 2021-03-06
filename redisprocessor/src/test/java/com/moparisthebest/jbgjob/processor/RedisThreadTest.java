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

import com.moparisthebest.jbgjob.AbstractSchedulerTests;
import com.moparisthebest.jbgjob.RedisScheduler;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class RedisThreadTest extends AbstractSchedulerTests {

	static RedisThread rt;

	@BeforeClass
	public static void setUpRedisThread() throws Throwable {
		System.setProperty("redis.maxTimeoutsBeforeClose", "1");
		rt = new RedisThread();
		rt.deleteQueue();
	}

	@BeforeClass
	public static void setUp() throws Throwable {
		bg = new RedisScheduler();
	}

	@AfterClass
	public static void testRunRedisThread() throws Throwable {
		rt.run();
		rt.deleteQueue();
	}
}
