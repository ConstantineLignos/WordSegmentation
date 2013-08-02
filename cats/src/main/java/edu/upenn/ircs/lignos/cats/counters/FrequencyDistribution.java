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

package edu.upenn.ircs.lignos.cats.counters;

import gnu.trove.map.hash.TObjectLongHashMap;

/**
 * A basic frequency distribution backed by a hashmap.
 *
 * @param <K> The type of events to be recorded, which must be hashable.
 */
public class FrequencyDistribution<K> {
	private TObjectLongHashMap<K> counts;
	private long total;

	/**
	 * Creates an empty frequency distribution.
	 */
	public FrequencyDistribution() {
		counts = new TObjectLongHashMap<K>();
		total = 0;
	}

	/**
	 * Increments the count of the specified event by one.
	 * @param event the event to be incremented
	 * @return the count of the key after incrementing
	 */
	public long inc(K event) {
		total += 1;
		return counts.adjustOrPutValue(event, 1, 1);
	}

	/**
	 * Increments the count of the specified event by the specified amount.
	 * @param event the event to be incremented
	 * @param amount the amount to increment the count of the event
	 * @return the count of the key after incrementing
	 */
	public long inc(K event, long amount) {
		assert(amount > 0);
		total += amount;
		return counts.adjustOrPutValue(event, amount, amount);
	}

	/**
	 * Returns the count of the specified event.
	 * @param event the event whose associated count is to be returned
	 * @return the count of the specified event
	 */
	public long getCount(K event) {
		return counts.get(event);
	}

	/**
	 * Returns the frequency (count divided by the count of all events) of the specified event.
	 * @param event the event whose associated frequency is to be returned
	 * @return the frequency of the specified event
	 */
	public double getFreq(K event) {
		return (total > 0) ? counts.get(event) / (float) total : 0.0; 
	}
	
	/**
	 * Returns the total count of all events recorded, the sum of the counts of each event.
	 * @return the total count of all events
	 */
	public long getTotal() {
		return total;
	}
	
	/**
	 * Return the number of unique events recorded.
	 * @return the number of unique events
	 */
	public int size() {
		return counts.size();
	}
}
