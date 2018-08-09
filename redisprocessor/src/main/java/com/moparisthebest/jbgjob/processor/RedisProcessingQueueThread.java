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

import com.moparisthebest.jbgjob.ScheduledItemExecutor;
import com.moparisthebest.jbgjob.result.ExecutionResult;
import com.moparisthebest.jbgjob.result.PrintStackTraceExecutionResult;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Reads from the queue with BRPOPLPUSH, pushing into a 'processing' queue and then removes the job from the processing queue when finished, either after success or an error.
 */
public class RedisProcessingQueueThread extends RedisThread {

	public static final String defaultProcessingQueueSuffix = defaultIfEmpty(System.getProperty("redis.processingQueueSuffix"), "-processing");

	public final String processingQueue;

	public RedisProcessingQueueThread() {
		this(null, null, null, null, null);
	}
	public RedisProcessingQueueThread(Stop stop) {
		this(null, null, null, null, null, stop);
	}

	public RedisProcessingQueueThread(String queue) {
		this(queue, null, null, null, null);
	}
	public RedisProcessingQueueThread(String queue, Stop stop) {
		this(queue, null, null, null, null, stop);
	}

	public RedisProcessingQueueThread(JedisPool pool, Stop stop) {
		this(null, null, null, null, pool, stop);
	}

	public RedisProcessingQueueThread(String queue, ScheduledItemExecutor executor) {
		this(queue, executor, null, null, null);
	}

	public RedisProcessingQueueThread(String queue, ScheduledItemExecutor executor, Stop stop) {
		this(queue, executor, null, null, null, stop);
	}
	public RedisProcessingQueueThread(String queue, ScheduledItemExecutor executor, JedisPool pool, Stop stop) {
		this(queue, executor, null, null, pool, stop);
	}

	public RedisProcessingQueueThread(String queue, ScheduledItemExecutor executor, String processingQueueSuffix) {
		this(queue, executor, processingQueueSuffix, null, null);
	}

	public RedisProcessingQueueThread(String queue, ScheduledItemExecutor executor, String processingQueueSuffix, String queuePrefix) {
		this(queue, executor, processingQueueSuffix, queuePrefix, null);
	}

	public RedisProcessingQueueThread(String queue, ScheduledItemExecutor executor, String processingQueueSuffix, String queuePrefix, JedisPool pool) {
		this(queue, executor, processingQueueSuffix, queuePrefix, pool, null);
	}

	public RedisProcessingQueueThread(String queue, ScheduledItemExecutor executor, String processingQueueSuffix, String queuePrefix, JedisPool pool, Stop stop) {
		this(queue, executor, processingQueueSuffix, queuePrefix, pool, stop, null);
	}

	public RedisProcessingQueueThread(String queue, ScheduledItemExecutor executor, String processingQueueSuffix, String queuePrefix, JedisPool pool, Stop stop, Iterable<String> noWaitQueues) {
		super(queue, executor, queuePrefix, pool, stop, noWaitQueues);
		this.processingQueue = this.queue + defaultIfEmpty(processingQueueSuffix, defaultProcessingQueueSuffix);
	}

	@Override
	protected String pollRedisNoWait(final Jedis jedis, final String queueName) {
		if (debug) System.out.printf("redis>  RPOPLPUSH %s %s\n", queueName, processingQueue);
		return jedis.rpoplpush(queueName, processingQueue);
	}

	@Override
	protected String pollRedisBlock(final Jedis jedis, final int timeout) {
		if (debug) System.out.printf("redis>  BRPOPLPUSH %s %s %d\n", queue, processingQueue, timeout);
		return jedis.brpoplpush(queue, processingQueue, timeout);
	}

	protected ExecutionResult getExecutionResult(final String scheduledItemString) {
		return new RemoveFromProcessingQueueOnCompletion(scheduledItemString);
	}

	protected class RemoveFromProcessingQueueOnCompletion extends PrintStackTraceExecutionResult {
		protected final String scheduledItemString;

		public RemoveFromProcessingQueueOnCompletion(String scheduledItemString) {
			this.scheduledItemString = scheduledItemString;
		}

		protected void removeFromProcessingQueue(Jedis jedis) {
			boolean returnJedis = jedis == null;
			try {
				if (returnJedis)
					jedis = pool.getResource();
				if (debug) System.out.printf("redis>  LREM %s 1 \"%s\"\n", processingQueue, scheduledItemString);
				jedis.lrem(processingQueue, 1, scheduledItemString);
			} catch (Throwable e) {
				e.printStackTrace();
			} finally {
				if (returnJedis)
					tryReturn(jedis);
			}
		}

		@Override
		public void success() {
			// success, so remove 1 from processing queue
			removeFromProcessingQueue(null);
		}

		@Override
		public void error(Throwable e) {
			super.error(e);
			removeFromProcessingQueue(null);
		}
	}

	@Override
	void deleteQueue() {
		deleteQueue(queue, processingQueue);
	}

	public static void main(String[] args) {
		// set all needed arguments with system properties
		new RedisProcessingQueueThread().run();
	}
}
