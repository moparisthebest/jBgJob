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

import com.moparisthebest.jbgjob.pool.ObjectPool;
import com.moparisthebest.jbgjob.result.ExecutionResult;

import java.io.Closeable;
import java.util.concurrent.*;

/**
 * Using a ThreadPool with a configurable number of Threads and an ObjectPool, schedules jobs to be executed in the future.
 */
public class ScheduledItemExecutor implements Closeable {

	public static final int defaultNumThreads;

	static {
		int numThreads = 5; // 5 threads by default
		try {
			numThreads = Integer.parseInt(System.getProperty("scheduler.executor.numThreads", numThreads + ""));
		} catch (Throwable e) {
			e.printStackTrace();
		}
		defaultNumThreads = numThreads;
	}

	private final ExecutorService executor;
	private final ObjectPool pool;

	public ScheduledItemExecutor() {
		this(true);
	}
	public ScheduledItemExecutor(final boolean blockAddWhenSaturated) {
		this(defaultNumThreads, blockAddWhenSaturated);
	}

	public ScheduledItemExecutor(final int numThreads, final boolean blockAddWhenSaturated) {
		this.pool = new ObjectPool(numThreads);
		// grr...
		// http://stackoverflow.com/questions/2001086/how-to-make-threadpoolexecutors-submit-method-block-if-it-is-saturated
		// https://today.java.net/pub/a/today/2008/10/23/creating-a-notifying-blocking-thread-pool-executor.html
		// http://stackoverflow.com/questions/3446011/threadpoolexecutor-block-when-queue-is-full/3518588#3518588
		this.executor = !blockAddWhenSaturated ? Executors.newFixedThreadPool(numThreads) :
				new ThreadPoolExecutor(numThreads, numThreads,
						0L, TimeUnit.MILLISECONDS,
						new LinkedBlockingQueue<Runnable>()) {
					private final Semaphore semaphore = new Semaphore(numThreads);

					@Override
					public void execute(final Runnable command) {
						// acquire a lock
						boolean lockAcquired = false;
						do {
							try {
								semaphore.acquire();
								lockAcquired = true;
							} catch (Throwable e) {
							}
						} while (!lockAcquired);
						// run it
						try {
							super.execute(new Runnable() {
								@Override
								public void run() {
									try {
										command.run();
									} finally {
										semaphore.release();
									}
								}
							});
						} catch (RuntimeException e) {
							semaphore.release();
							throw e;
						}
					}
				};
	}

	public <T> void execute(final ScheduledItem<T> scheduledItem) {
		this.execute(scheduledItem, null);
	}

	public <T> void execute(final ScheduledItem<T> scheduledItem, final ExecutionResult result) {
		scheduledItem.setPool(pool);
		scheduledItem.setResult(result);
		executor.execute(scheduledItem);
	}

	@Override
	public void close() {
		executor.shutdown();
	}
}
