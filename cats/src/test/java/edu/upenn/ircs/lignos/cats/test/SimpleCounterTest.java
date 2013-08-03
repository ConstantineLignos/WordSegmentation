/*
 Copyright (C) 2011 Constantine Lignos

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

import edu.upenn.ircs.lignos.cats.counters.SimpleCounter;
import edu.upenn.ircs.lignos.cats.counters.SubSeqCounter;
import junit.framework.TestCase;

public class SimpleCounterTest extends TestCase{
	String word = "word";
	String[] i = {"I"};
	String[] like = {"like"};
	String[] pie = {"pie"};
	String[] iLike = {"I", "like"};
	String[] likePie = {"like", "pie"};
	String[] iLikePie = {"I", "like", "pie"};
	String[] iLikePieBad = {"Il", "ike", "pie"};

	/**
	 * Unseen items should have zero count.
	 */
	public void testZeroCount() {
		SimpleCounter c = new SimpleCounter();
		assertEquals(0, c.get(word));
	}

	/**
	 * Incrementing should increment.
	 */
	public void testInc() {
		SimpleCounter c = new SimpleCounter();
		c.inc(word);
		assertEquals(1, c.get(word));
		c.inc(word);
		assertEquals(2, c.get(word));
		c.inc(word);
		assertEquals(3, c.get(word));
	}

	/**
	 * Unseen subsequences should have zero count.
	 */
	public void testSubSeqZeroCount() {
		SubSeqCounter c = new SubSeqCounter();
		assertEquals(0, c.get(iLikePie));
	}

	/**
	 * Incrementing subsequences should increment them.
	 */
	public void testSubSeqInc() {
		SubSeqCounter c = new SubSeqCounter();
		c.inc(iLikePie);
		assertEquals(1, c.get(iLikePie));
		c.inc(iLikePie);
		assertEquals(2, c.get(iLikePie));
		c.inc(iLikePie);
		assertEquals(3, c.get(iLikePie));
	}

	/**
	 * Test that two string combinations with different boundaries are stored separately.
	 */
	public void testUnitSep() {
		SubSeqCounter c = new SubSeqCounter();
		c.inc(iLikePie);
		assertEquals(1, c.get(iLikePie));
		// Same string, different unit boundaries
		c.inc(iLikePieBad);
		assertEquals(1, c.get(iLikePieBad));
		assertEquals(1, c.get(iLikePie));

	}

	/**
	 * Incrementing all subsequences, length 1.
	 */
	public void testAllSubSeqInc1() {
		SubSeqCounter c = new SubSeqCounter();
		c.incAllSubSeqs(i);
		assertEquals(1, c.get(i));
		c.incAllSubSeqs(i);
		assertEquals(2, c.get(i));
	}

	/**
	 * Incrementing all subsequences, length 2.
	 */
	public void testAllSubSeqInc2() {
		SubSeqCounter c = new SubSeqCounter();
		c.incAllSubSeqs(iLike);
		assertEquals(1, c.get(i));
		assertEquals(1, c.get(like));
		assertEquals(1, c.get(iLike));

		c.incAllSubSeqs(iLike);
		assertEquals(2, c.get(i));
		assertEquals(2, c.get(like));
		assertEquals(2, c.get(iLike));
	}

	/**
	 * Incrementing all subsequences, length 3.
	 */
	public void testAllSubSeqInc3() {
		SubSeqCounter c = new SubSeqCounter();
		c.incAllSubSeqs(iLikePie);
		assertEquals(1, c.get(i));
		assertEquals(1, c.get(like));
		assertEquals(1, c.get(pie));
		assertEquals(1, c.get(iLike));
		assertEquals(1, c.get(likePie));
		assertEquals(1, c.get(iLikePie));

		c.incAllSubSeqs(iLikePie);
		assertEquals(2, c.get(i));
		assertEquals(2, c.get(like));
		assertEquals(2, c.get(pie));
		assertEquals(2, c.get(iLike));
		assertEquals(2, c.get(likePie));
		assertEquals(2, c.get(iLikePie));
	}
}