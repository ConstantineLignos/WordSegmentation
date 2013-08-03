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

package edu.upenn.ircs.lignos.cats;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class Utils {
	private static final String UNIT_DELIM = "|";
	
	/**
	 * Join the given Objects' string representations using a delimiter.
	 * @param items an array of Objects to join
	 * @param delim the delimiter to use
	 * @return a string containing the joined elements
	 */
	public static String join(Object[] items, String delim) {
		// Return an empty string for an empty list
		if (items.length == 0)
			return "";
		
		// Otherwise build up the joined string
		StringBuilder out = new StringBuilder();
		// Put the delimiter after all but the last item
		for (int i = 0; i < items.length - 1; i++) {
			out.append(items[i].toString());
			out.append(delim);
		}
		// Add the last element
		out.append(items[items.length - 1]);
		
		return out.toString();
	}
	
	/**
	 * Returns a formatted string of units and their stresses.
	 * @param units units to format
	 * @param stresses stresses for the given units
	 * @return a formatted string
	 */
	public static String formatUnits(String[] units, Boolean[] stresses) {
		StringBuilder output = new StringBuilder();
		// Divide all up to the next-to-last unit with the delimeter
		int i;
		for (i = 0; i < units.length - 1; i++) {
			output.append(units[i] + (stresses[i] ? "(1)" : ""));
			output.append(UNIT_DELIM);
		}
		// Add the last unit
		output.append(units[i] + (stresses[i] ? "(1)" : ""));
		
		return output.toString();
	}
	
	/**
	 * Read a Properties object from the specified path.
	 * @param path the path to read properties from
	 * @return a Properties object or null if the file could not be read
	 */
	public static Properties loadProps(String path) {
		Properties props = new Properties();
		try {
			props.load(new FileReader(path));
			return props;
		} catch (FileNotFoundException e) {
			return null;
		} catch (IOException e) {
			return null;
		}
	}
}
