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

import java.util.ArrayList;
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
	protected final Stop stop;
	protected final ScheduledItemExecutor executor;
	protected final Iterable<String> noWaitQueues;

	public RedisThread() {
		this(null, null, (String)null, null);
	}

	public RedisThread(Stop stop) {
		this(null, null, null, null, stop);
	}

	public RedisThread(String queue) {
		this(queue, null, (String)null, null);
	}

	public RedisThread(String queue, Stop stop) {
		this(queue, null, null, null, stop);
	}

	public RedisThread(JedisPool pool, Stop stop) {
		this(null, null, null, pool, stop);
	}

	public RedisThread(String queue, ScheduledItemExecutor executor) {
		this(queue, executor, (String)null, null);
	}

	public RedisThread(String queue, ScheduledItemExecutor executor, Stop stop) {
		this(queue, executor, null, null, stop);
	}

	public RedisThread(String queue, ScheduledItemExecutor executor, JedisPool pool, Stop stop) {
		this(queue, executor, null, pool, stop);
	}

	public RedisThread(String queue, ScheduledItemExecutor executor, String queuePrefix) {
		this(queue, executor, queuePrefix, null);
	}

	public RedisThread(String queue, ScheduledItemExecutor executor, String queuePrefix, JedisPool pool) {
		this(queue, executor, queuePrefix, pool, null);
	}

	public RedisThread(String queue, ScheduledItemExecutor executor, String queuePrefix, JedisPool pool, Stop stop) {
		this(queue, executor, queuePrefix, pool, stop, null);
	}

	public RedisThread(String queue, ScheduledItemExecutor executor, String queuePrefix, JedisPool pool, Stop stop, Iterable<String> noWaitQueues) {
		super(queuePrefix, pool);
		this.queue = this.queuePrefix + defaultIfEmpty(queue, AbstractScheduler.defaultQueue);
		this.executor = executor != null ? executor : new ScheduledItemExecutor();

		List<String> nwq = null;
		if(noWaitQueues != null) {
			nwq = new ArrayList<String>();
			for(final String q : noWaitQueues)
				nwq.add(this.queuePrefix + q);
			if(nwq.isEmpty())
				nwq = null;
		}
		this.noWaitQueues = nwq;

		if(stop == null){
			final String shutdownKey = this.queuePrefix + "shutdown";
			stop = new Stop(){
				public boolean stop(final Jedis jedis){
					return "shutdown".equals(jedis.get(shutdownKey));
				}
			};
		}
		this.stop = stop;
	}

	protected String pollRedisNoWait(final Jedis jedis, final String queueName) {
		if (debug) System.out.printf("redis>  RPOP %s\n", queueName);
		return jedis.rpop(queueName);
	}

	protected String pollRedisBlock(final Jedis jedis, final int timeout) {
		if (debug) System.out.printf("redis>  BRPOP %s %d\n", queue, timeout);
		final List<String> items = jedis.brpop(timeout, queue);
		//System.out.println("items: " + items);
		return (items == null || items.size() < 2) ? null : items.get(1);
	}

	protected String pollRedis(final Jedis jedis, final int timeout) {
		String ret = pollRedisBlock(jedis, timeout);
		if(ret != null || noWaitQueues == null)
			return ret;
		for(final String queueName : noWaitQueues) {
			ret = pollRedisNoWait(jedis, queueName);
			if(ret != null)
				return ret;
		}
		return null;
	}

	private static final ExecutionResult noop = new PrintStackTraceExecutionResult();

	protected ExecutionResult getExecutionResult(final String scheduledItemString) {
		return noop;
	}

	public final void run() {
		Jedis jedis = null;
		outer:
		while (true)
			try {
				jedis = pool.getResource();
				while (true) {
					if (debug && maxTimeoutsBeforeClose > 0) System.out.printf("maxTimeoutsBeforeClose: %d timeoutCounter: %d\n", maxTimeoutsBeforeClose, timeoutCounter);
					// check to see if we should shutdown
					if (this.stop.stop(jedis))
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
					final ExecutionResult executionResult = getExecutionResult(scheduledItemString);
					try{
						final ScheduledItem scheduledItem = om.readValue(scheduledItemString, ScheduledItem.class);
						if (debug) System.out.println("scheduledItem object: " + scheduledItem);
						executor.execute(scheduledItem, executionResult);
					}catch(Throwable e){
						if(executionResult != null)
							executionResult.error(e);
					}
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

	public interface Stop {
		public boolean stop(final Jedis jedis);
	}
}
