/*
 Copyright (C) 2010-2013 Constantine Lignos

 This file is a part of CATS.

 CATS is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 CATS is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with CATS.  If not, see <http://www.gnu.org/licenses/>.
*/

package edu.upenn.ircs.lignos.cats.test;

import edu.upenn.ircs.lignos.cats.counters.FrequencyDistribution;
import junit.framework.TestCase;

public class FrequencyDistributionTest extends TestCase {
	private FrequencyDistribution<String> fd;

	public void setUp() {
		fd = new FrequencyDistribution<String>();
	}

	public void testInc() {
		fd.inc("a");
		assertEquals(1, fd.getCount("a"));
		fd.inc("a");
		assertEquals(2, fd.getCount("a"));
	}

	public void testIncAmount() {
		fd.inc("a", 2);
		assertEquals(2, fd.getCount("a"));
		fd.inc("a", 3);
		assertEquals(5, fd.getCount("a"));
	}

	public void testTotal() {
		fd.inc("a");
		assertEquals(1, fd.getTotal());
		fd.inc("a", 3);
		assertEquals(4, fd.getTotal());
		fd.inc("b", 2);
		assertEquals(6, fd.getTotal());
	}

	public void testSize() {
		fd.inc("a");
		assertEquals(1, fd.size());
		fd.inc("a", 3);
		assertEquals(1, fd.size());
		fd.inc("b", 2);
		assertEquals(2, fd.size());
	}

	public void testFreq() {
		fd.inc("a", 1);
		assertEquals(1.0, fd.getFreq("a"));
		fd.inc("b", 1);
		assertEquals(0.5, fd.getFreq("a"));
		assertEquals(0.5, fd.getFreq("b"));
		fd.inc("a", 1);
		assertEquals(2.0 / 3L, fd.getFreq("a"), 0.001);
		assertEquals(1.0 / 3L, fd.getFreq("b"), 0.001);
	}

	public void testGetUnseenCount() {
		assertEquals(0, fd.getCount("a"));
		fd.inc("b", 2);
		assertEquals(0, fd.getCount("a"));
		fd.inc("b");
		assertEquals(0, fd.getCount("a"));
	}

	public void testGetUnseenFreq() {
		assertEquals(0.0, fd.getFreq("a"));
		fd.inc("b", 2);
		assertEquals(0.0, fd.getFreq("a"));
		fd.inc("b");
		assertEquals(0.0, fd.getFreq("a"));
	}
}
