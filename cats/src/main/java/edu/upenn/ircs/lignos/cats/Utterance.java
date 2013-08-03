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

package edu.upenn.ircs.lignos.cats;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
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
	// should be able to be easily sliced, we have to use Boolean
	private Boolean[] boundaries;
	private String[] units;
	private Boolean[] stresses;
	private String prettyString;
	public final int length;
	
	// Pattern for finding boundaries
	public final static char WORD_BOUNDARY = ' ';
	public final static char SYLL_BOUNDARY = '|';
	
	/**
	 * Create an utterance from text, noting whether we should use gold standard
	 * information or not.
	 * @param text Text of the utterance
	 * @param reduceStress Whether stress should be reduced
	 * @param possBoundaries Possible segmentation points. If 
	 */
	public Utterance(String text, boolean gold, boolean reduceStress){
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
	public Utterance(String[] units, Boolean[] stresses, Boolean[] boundaries){
		this.units = units;
		this.stresses = stresses;
		this.boundaries = boundaries;
		length = units.length;
	}
	
	/**
	 * Create an utterance by copying fields from the specified utterance, copying boundaries if 
	 * specified.
	 * @param utt the utterance to copy from
	 * @param copyBoundaries whether to copy boundaries over  
	 */
	public Utterance(Utterance utt, boolean copyBoundaries){
		units = Arrays.copyOf(utt.units, utt.units.length);
		stresses = Arrays.copyOf(utt.stresses, utt.stresses.length);
		if (copyBoundaries) {
			boundaries = Arrays.copyOf(utt.boundaries, utt.boundaries.length);
		}
		else {
			boundaries = new Boolean[utt.boundaries.length];
			Arrays.fill(boundaries, false);
		}
		length = units.length;
	}

	
	/**
	 * Reduce the stresses in the utterance
	 */
	public void reduceStresses() {
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
	public Boolean[] getBoundariesCopy() {
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
	public void setBoundaries(Boolean[] boundaries) {
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

		// Find each unit/boundary.
		int idx = 0; // Define outside so it carries over
		int unitStart = -1; // -1 marks an invalid start
		for (; idx < text.length(); idx++) {
			char c = text.charAt(idx);
			// If it's a boundary, finish off the unit
			if (c == WORD_BOUNDARY || c == SYLL_BOUNDARY) {
				// Do nothing if we didn't already have a unit built
				if (unitStart == -1) {
					continue;
				}
				
				// Add the new unit
				String unit = text.substring(unitStart, idx);
				unitList.add(anyStressPattern.matcher(unit).replaceAll(""));
				stressList.add(primaryStressPattern.matcher(unit).matches());

				// Place a boundary if we're gold and this was a word boundary
				boundaryList.add(gold && c == WORD_BOUNDARY);
				
				// Reset unit start
				unitStart = -1;
			}
			else {
				// Note the start if this is the beginning of a new unit
				if (unitStart == -1) {
					unitStart = idx;
				}
			}
			
		}
		
		// Put in whatever's left. This code is repeated from above; for optimization
		// these were not refactored out
		String unit = text.substring(unitStart, idx);
		unitList.add(anyStressPattern.matcher(unit).replaceAll(""));
		stressList.add(primaryStressPattern.matcher(unit).matches());	

		// Convert into arrays for fast access later.
		boundaries = boundaryList.toArray(new Boolean[boundaryList.size()]);
		units = unitList.toArray(new String[unitList.size()]);
		stresses = stressList.toArray(new Boolean[stressList.size()]);
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
	 * Return a list of the utterances loaded from the specified path.
	 * @param path the path to load utterances from
	 * @return a List of Utterances
	 */
	public static List<Utterance> loadUtterances(String path) {
		List<Utterance> goldUtterances = new LinkedList<Utterance>();
		Scanner input = null;
		try {
			System.out.println("Loading utterances from " + path + " ...");
			input = new Scanner(new File(path));
			goldUtterances = new LinkedList<Utterance>();

			// Parse each line as an utterance
			String line;
			int lineNum = 0;
			while (input.hasNextLine()) {
				// Strip trailing whitespace
				line = input.nextLine().replaceAll("\\s+$", "");
				lineNum++;
				if (line.length() == 0) {
					System.err.println("Empty line on input line " + lineNum);
					continue;
				}
				// Add the utterance to gold, returning an error if it could not be parsed
				try {
					goldUtterances.add(new Utterance(line, true, false));
				}
				catch (StringIndexOutOfBoundsException e) {
					System.err.println("Could not parse input line " + lineNum);
					// Propagate the error as the file is bad
					throw new FileNotFoundException();
				}
			}
		} 
		catch (FileNotFoundException e) {
			return null;
		}
		finally {
			if (input != null) input.close();
		}
		return goldUtterances;
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
			Boolean[] segmentation) {
		StringBuilder out = new StringBuilder();
		// Connect all but the last unit with the right connection, then add on 
		// the final one
		for (int i=0; i < units.length - 1; i++) {
			out.append(units[i]);
			if (stresses[i]) out.append("(1)");
			out.append(segmentation[i] ? WORD_BOUNDARY : SYLL_BOUNDARY);
		}
		// Stick on final unit and stress
		out.append(units[units.length - 1]);
		if (stresses[units.length - 1]) out.append("(1)");
		
		return out.toString();
	}
	
}
