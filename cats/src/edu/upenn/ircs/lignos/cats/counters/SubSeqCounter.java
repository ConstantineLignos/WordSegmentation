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

package edu.upenn.ircs.lignos.cats.counters;

import java.util.Arrays;

import edu.upenn.ircs.lignos.cats.Utils;

/**
 * A counter particularly for sequences of String[] that wraps SimpleCounter. This does not inherit 
 * from SimpleCounter to prevent accidental use of the underlying SimpleCounter methods, which can 
 * have disastrous consequences as the object type of the SimpleCounter arguments makes silent type
 * safety violations easy.
 */
public class SubSeqCounter {

	private static final String KEY_DELIM = "|";
	
	private SimpleCounter counter;
	
	public SubSeqCounter() {
		counter = new SimpleCounter();
	}

	/**
	 * Concatenates the specified units into a key string.
	 * @param units the units to form a key from
	 * @return the key
	 */
	private String makeKey(String[] units) {
		return Utils.join(units, KEY_DELIM);
	}
	
	/**
	 * Increments the count of the specified units as a whole.
	 * @param units the units to increment the count of
	 */
	public void inc(String[] units) {
		counter.inc(makeKey(units));
	}

	/**
	 * Returns the count of the specified units as a whole.
	 * @param units the units to return the count of
	 * @return count of those units as a whole
	 */
	public int get(String[] units) {
		return counter.get(makeKey(units));
	}

	/**
	 * Increment the count of all possible subsequences of the specified units.
	 * @param units the units to count subsequences of
	 */
	public void incAllSubSeqs(String[] units) {
		// Loop over possible subsequence lengths
		for (int length=1; length <= units.length; length++) {
			// Loop over beginning and start indices
			for (int i=0; i <= (units.length - length); i++) {
				inc(Arrays.copyOfRange(units, i, i + length));
			}
		}		
	}
}
