/*
 Copyright (C) 2010, 2011 Constantine Lignos

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

package edu.upenn.ircs.lignos.cats.segmenters;

import java.util.Arrays;

import junit.framework.TestCase;

public class UtilTest extends TestCase {
	String[] pieSent = {"I", "like", "pie."};
	boolean[] noBound = {false, false};
	String[] i = {"I"};
	boolean[] iBound = {true, false};
	String[] like = {"like"};
	boolean[] likeBound = {true, true};
	String[] ilike = {"I", "like"};
	boolean[] ilikeBound = {false, true};
	String[] likepie = {"like", "pie."};
	boolean[] likepieBound = {true, false};
	String[] pie = {"pie."};
	
	String[][] allWords = {i, like, pie};
	String[][] twoWords1 = {ilike, pie};
	String[][] twoWords2 = {i, likepie};
	String[][] oneWord = {pieSent};
	
	/**
	 * Test taking off one unit from the start.
	 */
	public void testwordFromLastBoundaryFirstWord() {
		assertTrue(Arrays.equals(i, SegUtil.sliceFromLastBoundary(pieSent, iBound)));
	}
	
	/**
	 * Test taking off a word in the middle.
	 */
	public void testwordFromLastBoundarySecWord() {
		assertTrue(Arrays.equals(like, SegUtil.sliceFromLastBoundary(pieSent, likeBound)));
	}
	
	/**
	 * Test taking off more than one unit from the start.
	 */
	public void testwordFromLastBoundaryFirstSecWord() {
		assertTrue(Arrays.equals(ilike, SegUtil.sliceFromLastBoundary(pieSent, ilikeBound)));
	}
	
	
	/**
	 * Test running with no boundaries produces an exception
	 */
	public void testwordFromLastBoundaryNoBound() {
		try {
			SegUtil.sliceFromLastBoundary(ilike, noBound);
			fail("wordFromLastBoundary should fail on a boundary array without any boundaries");
		}
		catch (RuntimeException e) {
			;
		}
	}
	
	/**
	 * Test removing the final unit with boundaries present, both of these must 
	 * produce the same result
	 */
	public void testwordFromFinalBoundaryWithBound() {
		assertTrue(Arrays.equals(pie, SegUtil.sliceFromFinalBoundary(pieSent, likeBound)));
		assertTrue(Arrays.equals(pie, SegUtil.sliceFromFinalBoundary(pieSent, ilikeBound)));
	}
	
	/**
	 * Test removing the final unit with no boundaries present, both of these must 
	 * produce the same result
	 */
	public void testwordFromFinalBoundaryNoBound() {
		assertTrue(Arrays.equals(pieSent, SegUtil.sliceFromFinalBoundary(pieSent, noBound)));
	}
	
	
	/**
	 * Test with more than one boundary
	 */
	public void testslicesFromAllBoundariesMoreBound(){
		assertTrue(Arrays.deepEquals(SegUtil.slicesFromAllBoundaries(pieSent, likeBound), allWords));
	}
	
	/**
	 * Test with one boundary
	 */
	public void testslicesFromAllBoundariesOneBound(){
		assertTrue(Arrays.deepEquals(SegUtil.slicesFromAllBoundaries(pieSent, ilikeBound), twoWords1));
		assertTrue(Arrays.deepEquals(SegUtil.slicesFromAllBoundaries(pieSent, likepieBound), twoWords2));
	}
	
	/**
	 * Test with zero boundaries
	 */
	public void testslicesFromAllBoundariesNoBound(){
		assertTrue(Arrays.deepEquals(SegUtil.slicesFromAllBoundaries(pieSent, noBound), oneWord));
	}	
}
