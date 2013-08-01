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

import com.moparisthebest.jbgjob.result.ExecutionResult;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * If the job ends in an error (throws Throwable), a serialized ScheduledItemError is placed into the 'error' queue with
 * the currentTimeMillis the exception occurred, the full stack trace, and the job that caused it.  This can then be
 * examined programmatically or manually later with the possibility of fixing the issue and re-running the job.
 */
public class RedisErrorQueueThread extends RedisProcessingQueueThread {

	public static final String defaultErrorQueueSuffix;

	static {
		final String suffix = System.getProperty("redis.errorQueueSuffix");
		defaultErrorQueueSuffix = (suffix == null || suffix.isEmpty()) ? "-error" : ("-" + suffix);
	}

	public final String errorQueue;

	public RedisErrorQueueThread() {
		this(defaultErrorQueueSuffix);
	}

	public RedisErrorQueueThread(String errorQueueSuffix) {
		this(defaultProcessingQueueSuffix, errorQueueSuffix);
	}

	public RedisErrorQueueThread(String processingQueueSuffix, String errorQueueSuffix) {
		this(defaultQueue, processingQueueSuffix, errorQueueSuffix);
	}

	public RedisErrorQueueThread(String queue, String processingQueueSuffix, String errorQueueSuffix) {
		this(null, queue, processingQueueSuffix, errorQueueSuffix);
	}

	public RedisErrorQueueThread(JedisPool pool, String queue, String processingQueueSuffix, String errorQueueSuffix) {
		this(defaultQueuePrefix, pool, queue, processingQueueSuffix, errorQueueSuffix);
	}

	public RedisErrorQueueThread(String queuePrefix, JedisPool pool, String queue, String processingQueueSuffix, String errorQueueSuffix) {
		super(queuePrefix, pool, queue, processingQueueSuffix);
		this.errorQueue = this.queue + errorQueueSuffix;
	}

	protected ExecutionResult getExecutionResult(final String scheduledItemString) {
		return new RemoveFromProcessingQueuePutErrorQueue(scheduledItemString);
	}

	protected class RemoveFromProcessingQueuePutErrorQueue extends RemoveFromProcessingQueueOnCompletion {
		public RemoveFromProcessingQueuePutErrorQueue(String scheduledItemString) {
			super(scheduledItemString);
		}

		@Override
		public void error(Throwable e) {
			e.printStackTrace();
			// success, so remove 1 from processing queue
			Jedis jedis = null;
			try {
				jedis = pool.getResource();
				// push to error queue
				final String error = om.writeValueAsString(new ScheduledItemError(e, scheduledItemString));
				if (debug) System.out.printf("redis>  LPUSH %s \"%s\"\n", errorQueue, error);
				jedis.lpush(errorQueue, error);
				// remove from processing queue
				removeFromProcessingQueue(jedis);
			} catch (Throwable e2) {
				e2.printStackTrace();
			} finally {
				tryReturn(jedis);
			}
		}
	}

	@Override
	void deleteQueue() {
		deleteQueue(queue, processingQueue, errorQueue);
	}

	public static void main(String[] args) {
		// set all needed arguments with system properties
		new RedisErrorQueueThread().run();
	}
}
