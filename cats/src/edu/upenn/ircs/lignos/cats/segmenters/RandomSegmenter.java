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

import edu.upenn.ircs.lignos.cats.Utterance;
import edu.upenn.ircs.lignos.cats.lexicon.Lexicon;

/**
 * A segmenter that randomly inserts word boundaries.
 */
public class RandomSegmenter implements Segmenter {
	private double threshold = .5;
	private int segs = 0;
	
	/**
	 * Create a 
	 * @param threshold
	 */
	public RandomSegmenter(double threshold) {
		this.threshold = threshold;
	}
	
	/*
	 * Segment by marking each possible boundary as a boundary
	 */
	@Override
	public boolean[] segment(Utterance utterance, Lexicon lexicon, boolean trace) {
		// Return all segmentation points as true
		boolean[] boundaries = utterance.getBoundariesCopy();
		
		// Randomly insert boundaries
		for (int i=0; i<boundaries.length; i++) {
			if (Math.random() < threshold) {
				boundaries[i] = true;
				segs++;
			}
			else {
				boundaries[i] = false;				
			}
		}

		// Increment the words used in the utterance.
		lexicon.incUtteranceWords(utterance.getUnits(), utterance.getStresses(), 
				boundaries, null);
		
		return boundaries;
	}

	@Override
	public String getStats() {
		return "Segs: " + segs;
	}
}
