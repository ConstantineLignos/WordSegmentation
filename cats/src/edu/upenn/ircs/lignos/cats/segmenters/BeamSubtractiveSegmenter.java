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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

import edu.upenn.ircs.lignos.cats.Utterance;
import edu.upenn.ircs.lignos.cats.lexicon.Lexicon;
import edu.upenn.ircs.lignos.cats.lexicon.Word;

/**
 * A segmenter that uses a beam search to apply the USC left-to-right and 
 * subtracts known words from the utterance.
 */
public class BeamSubtractiveSegmenter implements Segmenter {
	private final static boolean WIDESEARCH = false;
	
	private boolean longest;
	private boolean useUSC;
	private int beamSize;
	private ArrayList<SegResult> beam;
	private ArrayList<SegResult> candidates;
	
	private int nUtts = 0;
	private int totalHighestBeamSize = 0;
	
	private int uscSegs = 0;
	private int subtractionSegs = 0;
	
	public BeamSubtractiveSegmenter(boolean longest, boolean useUSC, int beamSize) {
		this.beamSize = beamSize;
		this.useUSC = useUSC;
		// Create the beams. These are reused each time for efficiency
		beam = new ArrayList<SegResult>(beamSize);
		candidates = new ArrayList<SegResult>(beamSize * 2);
	}
	
	
	/* 
	 * Segment by subtracting known words from the utterance and using the 
	 * Unique Stress Constraint to limit the amount of stress per word 
	 * (if specified). 
	 */
	@Override
	public boolean[] segment(Utterance utterance, Lexicon lexicon, boolean trace) {
		// Get the initial segmentation for the utterance
		boolean[] segmentation = utterance.getBoundariesCopy();
		// Clear beam and candidates, seed the beam
		beam.clear();
		candidates.clear();
		// Create an empty seg result with false trusts for each boundary
		beam.add(new SegResult(segmentation, 0, false, new boolean[utterance.length - 1]));
		
		// Track the maximum beam size
		int highestBeamSize = 0;
		
		// Keep segmenting until we're done
		while (true) {
			// Loop over the beam, keeping complete segmentations and moving others
			// Keep track of whether everything in the beam is done
			boolean allDone = true;
			for (SegResult currResult : beam) {
				// Track highest beam size
				if (beam.size() > highestBeamSize) {
					highestBeamSize = beam.size();
				}
				// Always let complete candidates propagate. 
				if (currResult.index == utterance.length) {
					candidates.add(currResult);
				}
				else {
					// Otherwise, note that we are not all done and advance
					allDone = false;
					
					// Set the beam lock if the beam is full
					boolean beamLock = beam.size() == beamSize;
					SegResult[] results = extendSeg(utterance, lexicon, 
							currResult, trace, beamLock);
					// Depending on the full beam behavior, either keep the top
					// result or add all to candidates 
					for (SegResult result : results)
						candidates.add(result);
				}
			}
			// Clear the beam and copy the candidates to it 
			beam.clear();
			if (candidates.size() <= beamSize) {
				beam.addAll(candidates);
			}
			else {
				pruneBeam(beam, candidates);
			}
			candidates.clear();
			
			// If we're all done, pick the best one
			if (allDone) {
				SegResult bestSeg = pickBestSeg(utterance, beam, lexicon, trace);
				// Increment the words used
				lexicon.incUtteranceWords(utterance.getUnits(), utterance.getStresses(), 
						bestSeg.segmentation, bestSeg.trusts);
				
				// Note beam size statistics before we return
				nUtts++;
				totalHighestBeamSize += highestBeamSize;
				
				return bestSeg.segmentation;
			}
		}
	}
	
	
	private void pruneBeam(ArrayList<SegResult> beam,
			ArrayList<SegResult> candidates) {
		// TODO Implement pruning properly
		for (SegResult candidate : candidates) {
			if (beam.size() < beamSize) {
				beam.add(candidate);
			}
		}
	}
	
	
	private SegResult pickBestSeg(Utterance utt, ArrayList<SegResult> beam, 
			Lexicon lex, boolean trace) {
		// If the beam size is one, just return the only item
		if (beam.size() == 1) {
			return beam.get(0);
		}
		
		if (trace) System.out.println("Choosing from beam of size " + beam.size());

		// Pick the one with the highest score
		double maxScore = Double.NEGATIVE_INFINITY;
		int maxScoreIdx = -1;
		for (int i = 0; i < beam.size(); i++) {
			double scores[] = lex.utteranceWordsScores(utt.getUnits(), 
					utt.getStresses(), beam.get(i).segmentation);
			// TODO: Make logProb scoring an option
			double segScore =  SegUtil.geometricMean(scores);
			//double segScore = SegUtil.logProb(scores);
			if (trace) System.out.println(Utterance.makeSegText(utt.getUnits(), 
					utt.getStresses(), beam.get(i).segmentation) +  " score: " + 
					segScore);
			
			if (segScore > maxScore) {
				maxScore = segScore;
				maxScoreIdx = i;
			}
		}
		
		if (trace) System.out.println("Chose " + maxScoreIdx);
		
		// If the beam size is two and wide search is off, blame the losing word
		if (beam.size() == 2  && !WIDESEARCH) {
			Word w = lex.getSplitWord(utt, beam.get(maxScoreIdx).segmentation, 
					beam.get((maxScoreIdx + 1) % 2).segmentation);
			if (w != null) {
				lex.penalizeWord(w);
			}
		}
			
			
		return beam.get(maxScoreIdx);
	}


	public SegResult[] extendSeg(Utterance utterance, Lexicon lexicon, 
			SegResult baseResult, boolean trace, boolean beamLock) {
		// We keep a main segmentation for USC or no seg where the
		// beam does not split and we recycle the original segResult.
		boolean[] baseSegmentation = baseResult.segmentation;
		boolean[] baseTrusts = baseResult.trusts;
		int baseIndex = baseResult.index;
		// Additional pairs of segmentations and indices go here
		LinkedList<SegResult> otherResults = null;
		
		// A default segmentation to be used for a wider search or if we 
		// don't segment
		SegResult defaultSeg = null;
		
		Boolean[] stresses = utterance.getStresses();
		boolean seenStress = baseResult.seenStress;		
		
		// Set the flag if this unit is stressed
		if (stresses[baseIndex]) seenStress = true;
		
		// If WIDESEARCH is on, always make the default segmentation
		if (WIDESEARCH) {
			defaultSeg = new SegResult(baseSegmentation, 
					baseIndex + 1, seenStress, baseTrusts);
		}
		
		// Try stress-based segmentation first if we have room to look ahead
		// and it's enabled, then try other techniques
		if (baseIndex < utterance.length - 1 && useUSC && seenStress && 
				stresses[baseIndex + 1]) {
			// Add a segmentation point after the current unit. Since
			// boundaries are offset from units by one, this means the
			// same index
			baseSegmentation[baseIndex] = true;
			// Trust USC segs
			baseTrusts[baseIndex] = true;
			uscSegs += 1;
			seenStress = false;
			baseIndex++; // Move forward just one unit
			
			// Update the segmentation
			SegResult.recycleSegResult(baseSegmentation, baseIndex, seenStress, 
					baseTrusts, baseResult);
		}
		else {			
			// Try to subtract a word starting at this position
			ArrayList<Word> prefixes = lexicon.getPrefixWords(utterance, baseIndex);
			if (!prefixes.isEmpty()) {
				// Add all possible prefixes, splitting the beam if necessary
				boolean first = true; // Track the first so we can put it in main
				for (Word w : prefixes) {
					// If beamlock is on, we're going to sneakily change w
					// to the best one and break out at the end
					if (beamLock && first) {
						w = longest ? prefixes.get(prefixes.size() - 1):
							SegUtil.chooseBestScoreWord(prefixes, lexicon);
					}
					
					if (first && !beamLock && trace && prefixes.size() > 1) 
						System.out.println("Beam split of size " + prefixes.size());
					// if (trace) System.out.println("Subtracting " + w + " " + w.getCount());
					
					// Only count the subtraction once
					if (first) subtractionSegs += 1;
					
					// Copy the base index and modification so we have a fresh 
					// copy each time
					int index = baseIndex;
					boolean[] segmentation = 
						Arrays.copyOf(baseSegmentation, baseSegmentation.length);
					boolean[] trusts = 
						Arrays.copyOf(baseTrusts, baseTrusts.length);

					// Place the initial boundary of the word if that boundary
					// is not the start of the utterance
					if (baseIndex > 0) {
						// Since boundary i corresponds to a boundary just after 
						// unit i, subtract by 1 to go before it
						segmentation[baseIndex - 1] = true;
						// Don't update trusts, since we don't trust this word
					}
					
					// Get the final boundary for the word, again adjusting by 1
					int finalBound = baseIndex + w.length - 1;
					// If the final boundary is at too high an index, either
					// we've reached the end of the utterance or there's an error
					if (finalBound == segmentation.length) {
						// There's no boundary to insert, do nothing here and fall
						// through to adjusting i
					}
					else if (finalBound > segmentation.length) 
						throw new RuntimeException("Tried to subtract a word longer " +
								"than the remaining utterance.");
					else {
						// Otherwise, insert the new boundary, reset the stress flag
						// since we segmented, and move i and continue
						segmentation[finalBound] = true;
						trusts[finalBound] = true;
						seenStress = false;
						// Fall through to adjust i
					}
					
					// Adjust index to the length of the word used
					index += w.length;
					
					// Now, store the result appropriately based on whether
					// we are first or not
					if (first) {
						// Recycle baseResult
						SegResult.recycleSegResult(segmentation, index, 
								seenStress, trusts, baseResult);
						// If the beam is locked, break here
						if (beamLock) {
							break;
						}
						// Turn off first
						first = false;
					}
					else {
						// Allocate result list if needed					
						if (otherResults == null) {
							otherResults = new LinkedList<SegResult>();
						}
						otherResults.add(new SegResult(segmentation, 
								index, seenStress, trusts));
					}
				}
			}
			else {
				// If we get here, there was no segmentation to be done.
				// Recycle the base result with the index moved ahead,
				// and null out the default result so we don't have two
				SegResult.recycleSegResult(baseSegmentation, baseIndex + 1, 
						seenStress, baseTrusts, baseResult);
				defaultSeg = null;
			}
		}
		
		// If there's just one result, we will have never created otherResults
		if (otherResults == null) {
			// Just retain the main result, which will always have been updated,
			// and the defaultseg if it exists
			if (defaultSeg != null) {
				return new SegResult[] {baseResult, defaultSeg};
			}
			else {
				return new SegResult[] {baseResult};
			}
		}
		else {
			// Otherwise, build up the results
			// Keep track of whether to add in the default result
			int offset = defaultSeg == null ? 1 : 2;
			
			SegResult[] results = new SegResult[otherResults.size() + offset];
			// First result is always the recycled one
			results[0] = baseResult;
			
			// Add the default if there is one in the second spot
			if (defaultSeg != null) {
				results[1] = defaultSeg;
			}
			
			for (int i = offset; i < results.length; i++) {
				results[i] = otherResults.pop();
			}
			
			return results;
		}
	}
	
	
	@Override
	public String getStats() {
		float averageBeam = totalHighestBeamSize / (float) nUtts;
		return "USC Segs: " + uscSegs + "\nSub. segs: " + subtractionSegs +
		"\nAverage highest beam: " + averageBeam;
	}
}
