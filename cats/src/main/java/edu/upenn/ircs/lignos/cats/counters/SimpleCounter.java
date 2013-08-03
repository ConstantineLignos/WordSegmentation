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

import java.util.Map;
import gnu.trove.map.hash.THashMap;

/**
 * A simple counter class.
 */
public class SimpleCounter {

	private Map<Object, Integer> counts;

	public SimpleCounter() {
		counts = new THashMap<Object, Integer>();
	}

	/**
	 * Increment the count of the specified item.
	 * @param item the item to count
	 */
	public void inc(Object item) {
		Integer count = counts.get(item);
		counts.put(item, count == null ? 1 : ++count);
	}

	/**
	 * Returns the count of the specified item, or zero if it has never been counted.
	 * @param item the item whose associated count should be returned
	 * @return count of the specified item
	 */
	public int get(Object item) {
		Integer count = counts.get(item);
		return count == null ? 0 : count;
	}
}
