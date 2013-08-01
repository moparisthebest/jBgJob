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

import com.moparisthebest.jbgjob.AbstractScheduler;
import com.moparisthebest.jbgjob.RedisScheduler;
import com.moparisthebest.jbgjob.ScheduledItem;
import com.moparisthebest.jbgjob.ScheduledItemExecutor;
import com.moparisthebest.jbgjob.result.ExecutionResult;
import com.moparisthebest.jbgjob.result.PrintStackTraceExecutionResult;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Arrays;
import java.util.List;

/**
 * Simply reads from the specified queue with BRPOP and processes the job, no errors are recorded and there is no visibility into which jobs are currently being processed.
 */
public class RedisThread extends RedisScheduler implements Runnable {

	public static final int defaultTimeout;
	public static final int maxTimeoutsBeforeClose;

	public static int getIntSystemProperty(final String property, final int timeout) {
		try {
			return Integer.parseInt(System.getProperty(property, timeout + ""));
		} catch (Throwable e) {
			e.printStackTrace();
			return timeout;
		}
	}

	static {
		defaultTimeout = getIntSystemProperty("redis.timeout", 5);// 5 seconds by default
		maxTimeoutsBeforeClose = getIntSystemProperty("redis.maxTimeoutsBeforeClose", 0);// 0 by default, never close
	}

	private int timeoutCounter = 0;

	protected final String queue;
	protected final String shutdownKey;
	protected final ScheduledItemExecutor executor = new ScheduledItemExecutor();

	public RedisThread() {
		this(AbstractScheduler.defaultQueue);
	}

	public RedisThread(String queue) {
		this(null, queue);
	}

	public RedisThread(JedisPool pool, String queue) {
		this(defaultQueuePrefix, pool, queue);
	}

	public RedisThread(String queuePrefix, JedisPool pool, String queue) {
		super(queuePrefix, pool);
		this.queue = this.queuePrefix + queue;
		this.shutdownKey = this.queuePrefix + "shutdown";
	}

	protected String pollRedis(final Jedis jedis, final int timeout) {
		if (debug) System.out.printf("redis>  BRPOP %s %d\n", queue, timeout);
		final List<String> items = jedis.brpop(timeout, queue);
		//System.out.println("items: " + items);
		return (items == null || items.size() < 2) ? null : items.get(1);
	}

	private static final ExecutionResult noop = new PrintStackTraceExecutionResult();

	protected ExecutionResult getExecutionResult(final String scheduledItemString) {
		return noop;
	}

	@Override
	public final void run() {
		Jedis jedis = null;
		outer:
		while (true)
			try {
				jedis = pool.getResource();
				while (true) {
					if (debug && maxTimeoutsBeforeClose > 0) System.out.printf("maxTimeoutsBeforeClose: %d timeoutCounter: %d\n", maxTimeoutsBeforeClose, timeoutCounter);
					// check to see if we should shutdown
					if ("shutdown".equals(jedis.get(this.shutdownKey)))
						break outer;
					// grab an item, if it's null (probably timed out) try again
					final String scheduledItemString = pollRedis(jedis, defaultTimeout);
					if (scheduledItemString == null) {
						// timed out
						if (maxTimeoutsBeforeClose > 0 && ++timeoutCounter >= maxTimeoutsBeforeClose)
							break outer;
						continue;
					}
					timeoutCounter = 0;
					if (debug) System.out.println("scheduledItemString: " + scheduledItemString);
					final ScheduledItem scheduledItem = om.readValue(scheduledItemString, ScheduledItem.class);
					if (debug) System.out.println("scheduledItem object: " + scheduledItem);
					executor.execute(scheduledItem, getExecutionResult(scheduledItemString));
				}
			} catch (Throwable e) {
				e.printStackTrace();
			} finally {
				tryReturn(jedis);
			}
		this.close();
	}

	public final void tryReturn(final Jedis jedis) {
		if (jedis != null)
			try {
				pool.returnResource(jedis);
			} catch (Throwable e) {
				e.printStackTrace();
			}
	}

	@Override
	public void close() {
		super.close();
		executor.close();
	}

	protected void deleteQueue(final String... queue) {
		Jedis jedis = null;
		try {
			jedis = pool.getResource();
			if (debug) System.out.printf("redis>  DEL %s\n", Arrays.toString(queue));
			jedis.del(queue);
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			tryReturn(jedis);
		}
	}

	/**
	 * Only meant to be used by testing frameworks like JUnit
	 */
	void deleteQueue() {
		deleteQueue(queue);
	}

	public static void main(String[] args) {
		// set all needed arguments with system properties
		new RedisThread().run();
	}
}
