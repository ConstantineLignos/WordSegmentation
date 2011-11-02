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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The representation for a single utterance, including its text and any known
 * word boundaries.
 *
 */
public class Utterance {
	private static final Pattern primaryStressPattern = Pattern.compile("(.*)1(.*)");
	private static final Pattern anyStressPattern = Pattern.compile("\\d");
	
	// We use primitive arrays where possible, but since units and stresses
	// should be able to be easily slices, we have to use Boolean
	private boolean[] boundaries;
	private String[] units;
	private Boolean[] stresses;
	private String prettyString;
	public final int length;
	
	// Pattern for finding boundaries
	private final static Pattern boundaryPattern = Pattern.compile("([^| ]+)([ |]|$)");
	
	/**
	 * Create an utterance from text, noting whether we should use gold standard
	 * information or not.
	 * @param text Text of the utterance
	 * @param reduceStress Whether stress should be reduced
	 * @param possBoundaries Possible segmentation points. If 
	 */
	public Utterance(String text, boolean gold, boolean reduceStress){
		// Strip any word boundaries if this isn't gold standard
		if (!gold) {
			text = text.replaceAll(" ", "|");
		}
			
		// Parse the word boundaries and set length
		parseWordBoundaries(text, gold);
		length = units.length;
	
		// Reduce stress if needed
		if (reduceStress) 
			reduceStresses();
	}
	
	/**
	 * Create an utterance from given boundaries and units
	 * @param units Units of the utterance
	 * @param stresses Stresses of the utterance
	 * @param boundaries Boundaries in the utterance
	 */
	public Utterance(String[] units, Boolean[] stresses, boolean[] boundaries){
		this.units = units;
		this.stresses = stresses;
		this.boundaries = boundaries;
		length = units.length;
	}

	
	/**
	 * Reduce the stresses in the utterance
	 */
	private void reduceStresses() {
		// Do a look-ahead stress reduction- if current and next unit have stress,
		// reduce the current
		for (int i=0; i < stresses.length - 1; i++) {
			if (stresses[i] && stresses[i + 1])
				stresses[i] = false;
		}
	}


	/**
	 * Return a string that reflects the actual segmentation of the utterance.
	 * @return the text representing the segmented version of the utterance
	 */
	public String getSegText(){
		return makeSegText(units, stresses, boundaries);
	}

	/**
	 * @return a copy of the boundaries
	 */
	public boolean[] getBoundariesCopy() {
		return Arrays.copyOf(boundaries, boundaries.length);
	}
	
	
	/**
	 * @return the units that make up the utterance. 
	 */
	public String[] getUnits() {
		return units;
	}

	
	/**
	 * @return the stress of the units that make up the utterance.
	 */
	public Boolean[] getStresses() {
		return stresses;
	}



	/**
	 * @param boundaries the boundaries to set
	 */
	public void setBoundaries(boolean[] boundaries) {
		this.boundaries = boundaries;
	}


	/**
	 * Scans through a string, marking which boundaries are word boundaries
	 * (marked by space) as opposed to other boundaries (marked by |). A boundary
	 * at 0 means after the first item, for example "bih1g drah1m" has a boundary
	 * at 0. A boundary at a position is noted by a 1, and a non-boundary is noted
	 * by a 0. Thus the boundaries for "bih1g drah1m" are [1] and for "pley1 cheh1|ker0z"
	 * are [1, 0]. Note that utterance-initial and final boundaries are not noted.
	 */
	private void parseWordBoundaries(String text, boolean gold) {
		List<String> unitList = new LinkedList<String>();
		List<Boolean> stressList = new LinkedList<Boolean>();
		List<Boolean> boundaryList = new LinkedList<Boolean>();

		Matcher boundaryMatcher = boundaryPattern.matcher(text);
		// Find each boundary. If this is gold, mark any boundary that's a space.
		// Otherwise don't mark any boundaries as non word boundaries.
		while (boundaryMatcher.find()) {
			if (gold && " ".equals(boundaryMatcher.group(2))) {
				// Space is boundary
				boundaryList.add(true);
			}
			else if ("".equals(boundaryMatcher.group(2))) {
				// We're at the end of the string, do nothing, we'll break just
				// after the unit gets added
				;
			}
			else {
				// This is not a word boundary, so put zero
				boundaryList.add(false);
			}
			// Add each unit (syllable, character, etc.), noting the stress
			// but stripping it out of the unit itself
			String unit = boundaryMatcher.group(1);
			Matcher stressMatcher = anyStressPattern.matcher(unit);
			unitList.add(stressMatcher.replaceAll(""));
			stressList.add(hasPrimaryStress(unit));
		}
		// Convert into arrays for fast access later.
		boundaries = new boolean[boundaryList.size()];
		for (int i = 0; i < boundaries.length; i++) {
			boundaries[i] = boundaryList.remove(0);
		}
		units = new String[unitList.size()];
		for (int i = 0; i < units.length; i++) {
			units[i] = unitList.remove(0);
		}
		stresses = new Boolean[stressList.size()];
		for (int i = 0; i < stresses.length; i++) {
			stresses[i] = stressList.remove(0);
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (prettyString == null) {
			prettyString = Utils.formatUnits(units, stresses);
		}
		return prettyString;
	}
	
	
	/**
	 * Returns whether the given unit has the primary stress marker.
	 * @param unit the string to check for primary stress
	 * @return true if the unit has the primary stress marker, false otherwise
	 */
	private static boolean hasPrimaryStress(String unit) {
		return primaryStressPattern.matcher(unit).matches();
	}
	
	/**
	 * Return a string that reflects the actual segmentation of the utterance,
	 * joining non-boundaries with "|" and boundaries with " ".
	 * @param units the units of the utterance
	 * @param stresses the stresses of the utterance
	 * @param segmentation the segmentation used in the utterance
	 * @return the text representing the segmented version of the utterance
	 */
	public static String makeSegText(String[] units, Boolean[] stresses, 
			boolean[] segmentation) {
		StringBuilder out = new StringBuilder();
		// Connect all but the last unit with the right connection, then add on 
		// the final one
		for (int i=0; i < units.length - 1; i++) {
			out.append(units[i]);
			if (stresses[i]) out.append("(1)");
			out.append(segmentation[i] ? " " : "|");
		}
		// Stick on final unit and stress
		out.append(units[units.length - 1]);
		if (stresses[units.length - 1]) out.append("(1)");
		
		return out.toString();
	}
	
}
