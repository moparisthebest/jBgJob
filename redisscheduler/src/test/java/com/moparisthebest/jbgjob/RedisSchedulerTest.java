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

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

public class RedisSchedulerTest extends AbstractSchedulerTests {

	@BeforeClass
	public static void setUp() throws Throwable {
		bg = new RedisScheduler();
	}

	@Test
	public void testSerialization() throws Throwable {
		bg.testSerialization(new JacksonTest(arrayList(new InList(1L))));
		bg.testSerialization(new JacksonTest(Arrays.asList(new InList(1L))));
		bg.testSerialization(new JacksonTest(Collections.singletonList(new InList(1L))));

		bg.testSerialization(new JacksonTest(Collections.singleton(new InList(1L))));

		bg.testSerialization(new JacksonTestMap(Collections.singletonMap(1L, new InList(1L))));
	}


	public static <T> List<T> arrayList(T o) {
		final List<T> ret = new ArrayList<>();
		ret.add(o);
		return ret;
	}

	public static class InList {
		private Long a;

		public InList(Long a) {
			this.a = a;
		}

		public InList() {
		}

		public Long getA() {
			return a;
		}

		public void setA(final Long a) {
			this.a = a;
		}

		@Override
		public String toString() {
			return "InList{" +
					"a=" + a +
					'}';
		}
	}

	public static class JacksonTest {
		public final Collection<InList> inLists;

		public JacksonTest() {
			this(null);
		}

		public JacksonTest(final Collection<InList> inLists) {
			this.inLists = inLists;
		}

		@Override
		public String toString() {
			return "JacksonTest{" +
					"inLists=" + inLists +
					'}';
		}
	}

	public static class JacksonTestMap {
		public final Map<Long, InList> inLists;

		public JacksonTestMap() {
			this(null);
		}

		public JacksonTestMap(final Map<Long, InList> inLists) {
			this.inLists = inLists;
		}

		@Override
		public String toString() {
			return "JacksonTestMap{" +
					"inLists=" + inLists +
					'}';
		}
	}
}
