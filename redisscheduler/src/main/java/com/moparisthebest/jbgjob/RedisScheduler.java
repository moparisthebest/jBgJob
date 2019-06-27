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

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.util.*;

/**
 * This implementation of Scheduler that serializes the DTO into a redis list for processing elsewhere.
 */
public class RedisScheduler extends AbstractScheduler {

	public static final boolean debug = "true".equalsIgnoreCase(System.getProperty("redis.debug")); // print debug statements, for development only

	public static final String defaultQueuePrefix;

	static {
		String prefix = System.getProperty("redis.queuePrefix");
		if (isEmpty(prefix))
			try {
				prefix = java.net.InetAddress.getLocalHost().getHostName() + "-";
			} catch (Throwable e) {
			}
		defaultQueuePrefix = defaultIfEmpty(prefix, "");
	}

	protected final String queuePrefix;

	protected final ObjectMapper om = new ObjectMapper().enableDefaultTyping();
	protected final JedisPool pool;

	public RedisScheduler() {
		this(null, null);
	}

	public RedisScheduler(JedisPool pool) {
		this(null, pool);
	}

	public RedisScheduler(String queuePrefix) {
		this(queuePrefix, null);
	}

	public RedisScheduler(String queuePrefix, JedisPool pool) {
		this.queuePrefix = defaultIfEmpty(queuePrefix, defaultQueuePrefix);
		this.pool = pool != null ? pool : new JedisPool(new JedisPoolConfig(), System.getProperty("redis.host", "localhost"));
	}

	@Override
	public <T> boolean schedule(final String queue, final ScheduledItem<T> scheduledItem) {
		super.schedule(queue, scheduledItem);
		Jedis jedis = null;
		try {
			final String scheduledItemString = om.writeValueAsString(scheduledItem);
			if (debug) System.out.printf("redis>  LPUSH %s \"%s\"\n", queuePrefix + queue, scheduledItemString);
			jedis = pool.getResource();
			return jedis.lpush(queuePrefix + queue, scheduledItemString) > 0;
		} catch (Throwable e) {
			e.printStackTrace();
			return false;
		} finally {
			if (jedis != null)
				try {
					pool.returnResource(jedis);
				} catch (Throwable e) {
					e.printStackTrace();
				}
		}
	}

	@Override
	@SuppressWarnings({"unchecked"})
	public <T> T testSerialization(final T t) throws IOException {
		final String singleton = om.writeValueAsString(new ScheduledItem<T>(null, t));
		System.out.printf("singleton: '%s', dto.getClass: '%s'", singleton, t.getClass());
		final ScheduledItem<T> singletonCol = (ScheduledItem<T>) om.readValue(singleton, ScheduledItem.class);
		System.out.printf(", singletonCol: '%s', dto.getClass: '%s'", singletonCol, singletonCol.getDto().getClass());
		if (singletonCol.getDto() instanceof Collection)
			for (Object ob : ((Collection) singletonCol.getDto()))
				System.out.printf(", val: '%s', val.getClass: '%s'", ob, ob.getClass());
		else if (singletonCol.getDto() instanceof Map)
			for (Map.Entry entry : (Set<Map.Entry>) ((Map) singletonCol.getDto()).entrySet())
				System.out.printf(", key: '%s', key.getClass: '%s', val: '%s', val.getClass: '%s'", entry.getKey(), entry.getKey().getClass(), entry.getValue(), entry.getValue().getClass());
		System.out.println();
		return singletonCol.getDto();
	}
}
