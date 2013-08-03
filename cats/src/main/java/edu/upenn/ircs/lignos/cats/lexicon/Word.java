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

package edu.upenn.ircs.lignos.cats.lexicon;

import java.util.Arrays;

import edu.upenn.ircs.lignos.cats.Utils;

/**
 * Representation of a word
 */
public class Word {
	// Set by the lexicon to determine the decay computed
	private static boolean decay;
	private static double decayAmt;

	public final String[] units;
	public final Boolean[] stresses;
	private String prettyString;
	private double score;
	public final int length;
	public long timestamp;
	private int[] observedStresses;
	private int observedStressCount;


	/**
	 * Create a new word with score zero with the given units and stresses.
	 * @param units the units that make up the word
	 * @param stresses the stress on each unit
	 */
	public Word(String[] units, Boolean[] stresses) {
		this.units = units;
		this.stresses = stresses;
		this.score = 0.0;
		this.length = units.length;
		this.timestamp = 0;
		this.observedStresses = new int[stresses.length];
		Arrays.fill(this.observedStresses, 0);
		this.observedStressCount = 0;
	}


	/**
	 * Create a new word with the given initial score and units and stresses.
	 * @param units the units that make up the word
	 * @param stresses the stress on each unit
	 * @param initialScore the score to start with
	 */
	public Word(String[] units, Boolean[] stresses, double initialScore, long
			timestamp) {
		this.units = units;
		this.stresses = stresses;
		this.score = initialScore;
		this.length = units.length;
		this.timestamp = timestamp;
		this.observedStresses = new int[stresses.length];
		Arrays.fill(this.observedStresses, 0);
		this.observedStressCount = 0;
	}


	/**
	 * Set whether or not decay is being used and the constant to use when
	 * calculating decay.
	 * @param decay
	 * @param decayAmt
	 */
	public static void setDecay(boolean decay, double decayAmt) {
		Word.decay = decay;
		Word.decayAmt = decayAmt;
	}


	/**
	 * Increment the score of the word. This is protected to prevent
	 * segmenters from calling it directly; they should call the appropriate
	 * lexicon methods instead.
	 */
	protected void increment(long timestamp) {
		score++;
		this.timestamp = timestamp;
	}


	/**
	 * Increment the score of the word by the given amount. This is protected to prevent
	 * segmenters from calling it directly; they should call the appropriate
	 * lexicon methods instead.
	 * @param amount amount to increment the count
	 */
	protected void increment(double amount, long timestamp) {
		score += amount;
		this.timestamp = timestamp;
	}


	/**
	 * Decrement the score of the word by the given amount. This is protected to prevent
	 * segmenters from calling it directly; they should call the appropriate
	 * lexicon methods instead.
	 * @param amount amount to decrement the count
	 */
	protected void decrement(double amount) {score -= amount;}


	/**
	 * Return the score of the word. If decay is in use, this is relative to the
	 * current time.
	 * @param timestamp the current time
	 * @return the count given the current time
	 */
	protected double getScore(long timestamp) {
		return decay ? decayScore(timestamp - this.timestamp, score) : score;
	}


	/**
	 * Compute the decay function for the given elapsed time and raw score.
	 * @param elapsedTime the time elapsed
	 * @param rawScore the raw score
	 * @return the decayed score
	 */
	private static double decayScore(long elapsedTime, double rawScore) {
		return rawScore * Math.exp(-elapsedTime * decayAmt);
	}


	/**
	 * @return the Word's raw score, not adjusted for the current time
	 */
	protected double getRawScore() {return score;}


	/**
	 * @return the timestamp
	 */
	public long getTimestamp() {return timestamp;}


	/**
	 * Formats the word for a lexicon dump.
	 * @param timestamp the current time
	 * @return a string representation of the word
	 */
	public String toOutputString(long timestamp){
		return getScore(timestamp) + " " + toString();
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString(){
		if (prettyString == null) {
			prettyString = Utils.formatUnits(units, stresses);
		}
		return prettyString;
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof Word))
			return false;
		else {
			Word otherWord = (Word) other;
			return Arrays.equals(stresses, otherWord.stresses) &&
				Arrays.equals(units, otherWord.units);
		}
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return toString().hashCode();
	}


	/**
	 * Note the stress that a word appeared with.
	 * @param stresses stress the word appeared with
	 */
	public void countStress(Boolean[] stresses) {
		// Add to observed stresses
		for(int i = 0; i < stresses.length; i++) {
			observedStresses[i] += stresses[i] ? 1 : 0;
		}
		observedStressCount++;
	}



	/**
	 * Return whether a word appears to be stress-initial
	 * @return whether the word appears to be stress-initial
	 */
	public boolean isStressInitial() {
		// Get the normalized stress rate for the word
		double[] normStresses = new double[stresses.length];
		for(int i = 0; i < stresses.length; i++) {
			normStresses[i] = observedStresses[i] / (float) observedStressCount;
		}
		// Check whether the initial is above .5
		return normStresses[0] > .5;
	}




}
