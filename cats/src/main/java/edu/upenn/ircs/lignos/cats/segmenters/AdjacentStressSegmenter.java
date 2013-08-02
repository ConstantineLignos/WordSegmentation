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
 * A segmenter that segments by placing boundaries between adjacent strongly 
 * stressed units.
 */
public class AdjacentStressSegmenter implements Segmenter {
	private int uscSegs = 0;
	private Lexicon lexicon;
	
	public AdjacentStressSegmenter(Lexicon lexicon) {
		this.lexicon = lexicon;
	}

	/* 
	 * Segment by placing boundaries between adjacent strong stresses.
	 */
	@Override
	public Boolean[] segment(Utterance utterance, boolean trace) {
		// Get info about the utterance. Since the segmentation is a copy,
		// don't worry about modifying it
		String[] units = utterance.getUnits();
		Boolean[] segmentation = utterance.getBoundariesCopy();
		Boolean[] stresses = utterance.getStresses();
		
		// Go through the n-1 words of the utterance, placing a boundary
		// where there are two sequential stresses
		for (int i = 0; i < units.length - 1; i++) {
			if (stresses[i] && stresses[i + 1]) {
				segmentation[i] = true;
				uscSegs++;
				// Each time you place a boundary, increment the word in the lexicon
				lexicon.rewardWord((String[]) SegUtil.sliceFromLastBoundary(units, segmentation),
						(Boolean[]) SegUtil.sliceFromLastBoundary(stresses, segmentation));
			}
		}
		
		// Increment the final word in the lexicon
		lexicon.rewardWord((String[]) SegUtil.sliceFromFinalBoundary(units, segmentation),
				(Boolean[]) SegUtil.sliceFromFinalBoundary(stresses, segmentation));
		
		return segmentation;
	}
	
	public String getStats() {
		return "USC segs: " + uscSegs;
	}
}
