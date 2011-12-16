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
import java.util.List;

import edu.upenn.ircs.lignos.cats.counters.SubSeqCounter;
import edu.upenn.ircs.lignos.cats.lexicon.Lexicon;
import edu.upenn.ircs.lignos.cats.lexicon.Word;

public class SegUtil {
	// Used to provide classes for toArrary results casting
	private static final Object[][] DUMMY_OBJECT_NESTED_ARRAY = new Object[0][0];
	private static final Boolean[] DUMMY_BOOLEAN_ARRAY = new Boolean[0];
	
	// TODO Make this configurable
	public static double PEAKINESS = 0.5;
	
	/**
	 * Return the slice created by the most recent boundary inserted. Will
	 * throw an exception if there are no boundaries in the boundary array given.
	 * @param sequence sequence to slice
	 * @param boundaries boundaries in the utterance
	 * @return the subsequence corresponding to the last boundary
	 */
	public static Object[] sliceFromLastBoundary(Object[] sequence, Boolean[] boundaries) {
		// Find the start and end of the word, which are the last and next
		// to last boundary indices. If there's only one boundary, the next
		// to last is effectively -1 (start of the text)
		int start = -1;
		int end = -1;
		for (int i = 0; i < boundaries.length; i++) {
			if (boundaries[i]) {
				start = end;
				end = i;
			}
		}
		
		// If there are no boundaries, throw an exception. This is to prevent
		// the caller (who is likely to call wordFromFinalBoundary later)
		// from getting the word twice
		if (start == end) throw new RuntimeException(
				"Cannot be called on a boundary array that contains no boundaries.");
		
		// Now get the appropriate slice from the text. Increase start and 
		// end by one since they are aligned one left of the text
		return Arrays.copyOfRange(sequence, start + 1, end + 1, sequence.getClass());
	}

	
	/**
	 * Return the slice between the final boundary and the end of the sequence.
	 * @param sequence sequence to slice
	 * @param boundaries boundaries in the utterance
	 * @return the subsequence of the text corresponding to the newest word
	 */
	public static Object[] sliceFromFinalBoundary(Object[] sequence, Boolean[] boundaries) {
		// Find the last boundary. If no boundary is found, -1 is correct since
		// when it is incremented it will be zero, the first index in the text
		int last = -1;
		for (int i = 0; i < boundaries.length; i++) {
			if (boundaries[i]) last = i;
		}
		
		// Now get the appropriate slice from last to the end. Increase last  
		// by one since it is aligned one left of the text
		return Arrays.copyOfRange(sequence, last + 1, sequence.length, sequence.getClass());
	}
	
	
	/**
	 * Return slices from all boundaries.
	 * @param sequence sequence to slice
	 * @param boundaries boundaries in the utterance
	 * @return the subsequence of the text corresponding to the newest word
	 */
	public static Object[][] slicesFromAllBoundaries(Object[] sequence, Boolean[] boundaries) {
		List<Object[]> slices = new LinkedList<Object[]>();
		
		int start = -1;
		int end = -1;
		// Go through all boundaries, slicing out each spot
		for (int i = 0; i < boundaries.length; i++) {
			if (boundaries[i]) {
				start = end;
				end = i;
				
				// Now get the appropriate slice. Increase start and 
				// end by one since they are aligned one left of the text
				 slices.add(Arrays.copyOfRange(sequence, start + 1, end + 1, sequence.getClass()));
			}
		}
		// Now get the final slice, similarly offset end by 1
		slices.add(Arrays.copyOfRange(sequence, end + 1, sequence.length, sequence.getClass()));
		
		// Do a crazy conversion to Object[][], thanks toArray!
		return slices.toArray(DUMMY_OBJECT_NESTED_ARRAY);
	}
	
	
	/**
	 * Choose the most frequent word in the list of words. 
	 * @param words the words to choose from
	 * @return the most frequent word
	 */
	public static Word chooseBestScoreWord(ArrayList<Word> words, Lexicon lex, 
			SubSeqCounter counter) {
		// Pick the most frequent
		Word bestWord = null;
		double bestScore = 0;
		for (Word word : words) {
			double score = lex.getScore(word, counter);
			if (score > bestScore) {
				bestWord = word;
				bestScore = score;
			}
		}
		return bestWord;
	}
	
	/**
	 * Randomly choose the best word, preferring words of higher score.
	 * @param words the words to choose from
	 * @return the selected  word
	 */
	public static Word chooseSampledBestScoreWord(ArrayList<Word> words, Lexicon lex, 
			SubSeqCounter counter) {
		// First get the normalization denominator
		double scoreSum = 0;
		for (Word w : words) {
			scoreSum += lex.getScore(w, counter);
		}
		
		// Then normalize
		double[] normScores = new double[words.size()];
		for (int i = 0; i < normScores.length; i++) {
			normScores[i] = lex.getScore(words.get(i), counter) / scoreSum;
		}
		
		// Bias the scores, normalize again
		scoreSum = 0;
		for (int i = 0; i < normScores.length; i++) {
			normScores[i] = biasProb(normScores[i]);
			scoreSum += normScores[i];
		}
		for (int i = 0; i < normScores.length; i++) {
			normScores[i] = normScores[i] / scoreSum;
		}
		
		// Draw a random number and find the winner using Shannon/Miller/Selfridge
		double draw = Math.random();
		double sum = 0;
		int winningIdx;
		for (winningIdx = 0; winningIdx < normScores.length; winningIdx++) {
			sum += normScores[winningIdx];
			if (sum > draw) {
				break;
			}
		}
		
		return words.get(winningIdx);
	}
	
	
	private static double biasProb(double score) {
		return Math.pow(Math.E, PEAKINESS * score);
	}

	
	/**
	 * Return which words should be trusted from all boundaries.
	 * @param trusts the matching trusts for the boundaries
	 * @param boundaries the boundaries
	 * @return trust level of each word in the boundaries
	 */
	public static Boolean[] wordsTrusts(boolean[] trusts, Boolean[] boundaries) {
		if (boundaries.length == 0) {
			// Trust anything with no boundaries
			return new Boolean[] {true};
		}
		else {
			List<Boolean> wordsTrusts = new LinkedList<Boolean>();
			for (int i = 0; i < boundaries.length; i++) {
				// Every time we see a boundary, note the trust of the word
				// to its left
				if (boundaries[i]) {
					wordsTrusts.add(trusts[i]);
				}
			}
			// Always add the last word as trusted (its boundary is the end
			// of the utterance
			wordsTrusts.add(true);
			
			return wordsTrusts.toArray(DUMMY_BOOLEAN_ARRAY);
		}
	}
	
	
	/**
	 * Return the geometric mean of an array of numbers.
	 * @param nums the numbers to average
	 * @return their geometric mean
	 */
	public static double geometricMean(double[] nums) { 
		double prod = 1.0;
		for (double num : nums) {
			prod *= num;
		}
		return Math.pow(prod, 1.0/(double) nums.length);
	}
	
	
	/**
	 * Return the negation of the number of ones in an array. 
	 * @param nums the numbers to scan
	 * @return the negation of the number of ones
	 */
	public static double countOnes(double[] nums) { 
		int count = 0;
		for (double num : nums) {
			if (num == 1.0) {
				count += 1;
			}
		}
		
		// Return the negation of the count so we can still choose a hypothesis
		// by the max
		return -count;
	}
	
	
	/**
	 * Return the variance of the numbers in an array.
	 * @param nums the numbers to scan
	 * @return the variance
	 */
	public static double calcVariance(double[] nums) { 
		double sum = 0;
		double sumSqrs = 0;
		
		for (double num : nums) {
			sum += num;
			sumSqrs += Math.pow(num, 2);
		}
			
		double mean = sum/nums.length;
		return (sumSqrs - sum * mean)/nums.length;
	}
	
	
	/**
	 * Return the sum of differences between frequencies between words and the
	 * the word that precedes them
	 * @param nums the frequencies of words
	 * @return sum of incremental differences
	 */
	public static double calcAlternation(double[] nums) { 
		double sum = 0;
		
		for (int i = 1; i < nums.length; i++) {
			sum += Math.abs(nums[i] - nums[i - 1]);
		}
			
		return sum;
	}

	
	/**
	 * Return the sum of the logs of the numbers given.
	 * @param nums the numbers
	 * @return sum of logs of numbers
	 */
	public static double logProb(double[] nums) {
		double sum = 0;
		for (int i = 0; i < nums.length; i++) {
			sum += Math.log(nums[i]);
		}
		return sum;
	}
	
	
	/**
	 * Return the mean entropy of the logs of the numbers given.
	 * @param nums the numbers
	 * @return sum of logs of numbers
	 */
	public static double meanEnt(double[] nums) {
		return logProb(nums) / nums.length;
	}


	/**
	 * Return the winning beam index by sampling among scores based on their relative size.
	 * @param beamScores scores of each hypothesis in the beam
	 * @return index of the winning hypothesis
	 */
	public static int sampleScores(double[] beamScores) {
		// First get the normalization denominator
		double sum = 0;
		for (int i = 0; i < beamScores.length; i++) {
			sum += beamScores[i];
		}
		// Then normalize
		double[] normScores = new double[beamScores.length];
		for (int i = 0; i < beamScores.length; i++) {
			normScores[i] = beamScores[i] / sum;
		}
		
		// Bias the scores, normalize again
		sum = 0;
		for (int i = 0; i < normScores.length; i++) {
			normScores[i] = biasProb(normScores[i]);
			sum += normScores[i];
		}
		for (int i = 0; i < normScores.length; i++) {
			normScores[i] = normScores[i] / sum;
		}
		
		// Draw a random number and find the winner using Shannon/Miller/Selfridge
		double draw = Math.random();
		sum = 0;
		int winningIdx;
		for (winningIdx = 0; winningIdx < beamScores.length; winningIdx++) {
			sum += normScores[winningIdx];
			if (sum > draw) {
				break;
			}
		}
		
		return winningIdx;
	}
}
