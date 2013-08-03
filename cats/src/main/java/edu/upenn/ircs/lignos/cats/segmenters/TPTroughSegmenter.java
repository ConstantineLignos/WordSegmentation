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

package edu.upenn.ircs.lignos.cats.segmenters;

import edu.upenn.ircs.lignos.cats.Utterance;
import edu.upenn.ircs.lignos.cats.counters.FrequencyDistribution;
import edu.upenn.ircs.lignos.cats.lexicon.Lexicon;

/**
 * A segmenter that applies the USC left-to-right.
 */
public class TPTroughSegmenter implements Segmenter {
	private static final char SYLL_DELIM = Utterance.SYLL_BOUNDARY;
	private int segs = 0;
	private Lexicon lexicon;
	private FrequencyDistribution<String> sylls;
	private FrequencyDistribution<String> syllPairs;

	public TPTroughSegmenter(Lexicon lexicon) {
		this.lexicon = lexicon;
		sylls = new FrequencyDistribution<String>();
		syllPairs = new FrequencyDistribution<String>();
	}

	private void train(Utterance utterance) {
		String[] units = utterance.getUnits();
		int length = units.length;
		for (int i = 0; i < length - 1; i++) {
			String syll1 = units[i];
			String syll2 = units[i + 1];
			sylls.inc(syll1);
			syllPairs.inc(makeKey(syll1, syll2));
		}
	}

	public double transProb(String syll1, String syll2) {
		double freq1 = sylls.getFreq(syll1);
		// If first syllable is unseen, consider it zero probability.
		if (freq1 == 0.0) {
			return 0.0;
		}
		else {
			return syllPairs.getFreq(makeKey(syll1, syll2)) / freq1;
		}
	}

	/* 
	 * Segment using the Unique Stress Constraint to limit the amount of
	 * stress per word and place boundaries between adjacent primary stresses.
	 */
	@Override
	public Boolean[] segment(Utterance utterance, boolean training, boolean trace) {
		// Update probabilities if we're training
		if (training) {
			train(utterance);
		}
		
		// Get info about the utterance. Since the segmentation is a copy,
		// don't worry about modifying it
		Boolean[] segmentation = utterance.getBoundariesCopy();
		String[] units = utterance.getUnits();

		// Track probabilities in a window around the current position. This can be optimized by
		// saving the previous TP lookups rather than computing them again and again.
		for (int i = 1; i < units.length - 2; i++) {
			double prevProb = transProb(units[i - 1], units[i]);
			double currProb = transProb(units[i], units[i + 1]);
			double nextProb = transProb(units[i + 1], units[i + 2]);
			// Insert a boundary if we're in a "trough"; the preceding and following TPs are higher
			if (prevProb > currProb && nextProb > currProb) {
				segmentation[i] = true;
				segs++;
			}
		}
		
		// Increment the words used in the utterance.
		if (training) {
			lexicon.incUtteranceWords(utterance.getUnits(), utterance.getStresses(), segmentation,
					null);
		}
		
		return segmentation;
	}

	public String getStats() {
		return "Trough segs: " + segs;
	}

	private static String makeKey(String syll1, String syll2) {
		return syll1 + SYLL_DELIM + syll2;
	}
}
