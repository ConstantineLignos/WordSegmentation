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

import edu.upenn.ircs.lignos.cats.Utils;
import edu.upenn.ircs.lignos.cats.Utterance;
import edu.upenn.ircs.lignos.cats.counters.SubSeqCounter;
import edu.upenn.ircs.lignos.cats.segmenters.SegUtil;
import gnu.trove.THashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Random;

public class Lexicon {
	private static final String KEY_DELIM = "|";
	
	// TODO: Make these configurable
	// Amount to penalize
	private static final double PENALTY = 1.0; 
	private static final double INIT_SCORE = 1.0; 
	private static final double UNKNOWN_WORD_SCORE = .5; 
	private static final double SMOOTHING_MIN = 1.0; 
	
	private final boolean stressSensitive;
	private final boolean trace;
	private final boolean useTrust;
	private final boolean useProbMem;
	private final boolean NORMALIZATION;
	// Constant used for probabilistic memory
	private final double probAmount;
	// The initial score given to a word
	private final double initScore;
	// The score used as a dummy for unknown words
	private final double unknownWordScore;
	// The minimum score a word can have in scoring a hypothesis
	private final double smoothingMin;
	// The number of times tick has been called since the creation of the lexicon
	private long time;
	// The number of tokens represented by the lexicon
	// TODO: It's impossible to efficiently sync token count and decay. As every term
	// gets the same denominator, this should not matter for normalization.
	private long numTokens;
	// Our random number generator
	private Random rand;
	// The subsequence counter
	private SubSeqCounter counter;
	
	private Map<String, Word> lexicon;
	
	
	/**
	 * Create a new, empty lexicon.
	 * @param stressSensitive whether the lexicon should take stress into account
	 * @param trace whether to output tracing information
	 */
	public Lexicon(boolean stressSensitive, boolean trace, boolean useTrust, boolean useProbMem,
			boolean useNorm, double probAmount, double decayAmount, SubSeqCounter counter) {
		this.stressSensitive = stressSensitive;
		this.trace = trace;
		this.useTrust = useTrust;
		this.NORMALIZATION = useNorm;
		this.counter = counter;
		
		// Set up probabilistic memory
		this.useProbMem = useProbMem;
		this.probAmount = useProbMem ? probAmount : 0.0;
		
		// Other constants
		this.initScore = INIT_SCORE;
		this.smoothingMin = SMOOTHING_MIN;
		this.unknownWordScore = UNKNOWN_WORD_SCORE;
		
		// Set up decay on words
		if (decayAmount != 0.0) {
			Word.setDecay(true, decayAmount);
		}
		
		lexicon = new THashMap<String, Word>();
		time = 1;
		numTokens = 0;
		rand = new Random(0);
	}
	
	
	/**
	 * Make a lexicon key for the given text and stress information, taking 
	 * stress sensitivity into account.
	 * @param text the text to generate a key from
	 * @param stress the matching stress information for the text
	 * @return a key made from the text
	 */
	private String makeKey(String[] text, Boolean[] stresses) {
		StringBuilder key = new StringBuilder();
		key.append(Utils.join(text, KEY_DELIM));
		
		// If we're stress sensitive, tack the stresses on the end, note
		// we just use 1s and 0s to keep the string shorter than true/false would
		if (stressSensitive) {
			for (Boolean stress : stresses) 
				key.append(stress ? '1' : '0');
		}
		return key.toString();
	}
	
	
	/**
	 * Returns the Word for the given text and stress information, returning
	 * null if it is not in the lexicon.
	 * @param units the text to check
	 * @param stresses the matching stress information for the text
	 * @return a Word if the word is found, null otherwise
	 */
	public Word getWord(String[] units, Boolean[] stresses){
		return lexicon.get(makeKey(units, stresses));
	}
	
	
	/**
	 * Returns whether there is a Word with "in" the lexicon with the provided
	 * info. This should only be used in evaluation.
	 * @param units the text to check
	 * @param stresses the matching stress information for the text
	 * @return true if a matching Word with acceptable score is found, false otherwise
	 */
	public boolean isEvalWord(String[] units, Boolean[] stresses){
		return isEvalWord(lexicon.get(makeKey(units, stresses)));
	}
	
	
	/**
	 * Returns whether there is a Word with "in" the lexicon with the provided
	 * info. This should only be used in evaluation.
	 * @param word word to check
	 * @return true if a matching Word with score > 0 is found, false otherwise
	 */
	public boolean isEvalWord(Word word){
		// Return false if the word wasn't found, and check the score otherwise
		return recallWord(word);
	}
	
	
	/**
	 * Returns whether we succeed in recalling a word.
	 * @param word word to check
	 * @return true if the word was recalled successfully, false otherwise
	 */
	public boolean recallWord(Word w){
		if (w == null) {
			return false;
		}
		else if (useProbMem) {
			// Probabilistically look up the word
			return probMemRecallRate(w.getScore(time)) > rand.nextDouble();
		}
		else {
			// Return just whether the word has a positive score
			return w.getScore(time) > 0;
		}
	}
	
	
	/**
	 * Increment a word in the lexicon
	 * @param units units of the word
	 * @param stresses the word's stress
	 * @return
	 */
	public void rewardWord(String[] units, Boolean[] stresses) {
		// Look up the key, check if the word is there, create it if needed
		String key = makeKey(units, stresses);
		Word w = lexicon.get(key);
		if (w == null) {
			w = new Word(units, stresses, this.initScore, time);
			lexicon.put(key, w);
			if (trace) System.out.println("Added " + w + " " + w.getScore(time) + 
					(counter != null ? " " + counter.get(w.units) : ""));
			// All new words start with an initial score, so they don't need
			// to be incremented like existing words
		}
		else {
			// Increment the word's score
			incWord(w);
		}
		// Count the token
		numTokens++;
		
		// Note the stress information
		w.countStress(stresses);
	}
	
	
	/**
	 * Increment a word known to be in the lexicon. This indirection is provided
	 * to allow for any bookkeeping on increment
	 * @param w
	 */
	private void incWord(Word w) {
		w.increment(time);
		if (trace) System.out.println("Incremented " + w + " " + w.getScore(time) + 
				(counter != null ? " " + counter.get(w.units) : ""));
	}
	
	
	/**
	 * Penalize the given word.
	 * @param w word to penalize
	 */
	public void penalizeWord(Word w) {
		w.decrement(PENALTY);
		// Uncount the token. This is needed to keep normalization in sync
		numTokens -= PENALTY;
		if (trace) System.out.println("Penalized " + w + " " + w.getScore(time));
	}
	
	
	/**
	 * @return a Collection of the words in the lexicon
	 */
	public Collection<Word> getWords(){return lexicon.values();}

	
	/**
	 * Increment all words contained in a segmented utterance.
	 * @param units the units of the utterance
	 * @param stresses the stresses of the utterance
	 * @param boundaries the boundaries of the utterance
	 */
	public void incUtteranceWords(String[] units, Boolean[] stresses, 
			Boolean[] boundaries, boolean[] trusts) {
		// If we're not using trusts, ignore the trusts passed
		Boolean[] wordsTrusts;
		if (!useTrust || trusts == null) {
			wordsTrusts = null;
		}
		else {
			wordsTrusts = SegUtil.wordsTrusts(trusts, boundaries);
		}
		
		// Get slices of the stresses and units in this utterance
		Object[][] wordsUnits = 
			SegUtil.slicesFromAllBoundaries(units, boundaries);
		Object[][] wordsStresses = 
			SegUtil.slicesFromAllBoundaries(stresses, boundaries);
		
		// Each element of the outer array represents units/stresses for a single word
		for (int i = 0; i < wordsUnits.length; i++) {
			// Reward if no trust info was provided or if trusted  
			if (wordsTrusts == null || wordsTrusts[i]) {
				rewardWord(((String[]) wordsUnits[i]), ((Boolean[]) wordsStresses[i]));
			}
		}
	}
	
	
	/**
	 * Return the scores of words in an utterance, counting unknown words as
	 * the default initial score.
	 * @param units the units of the utterance
	 * @param stresses the stresses of the utterance
	 * @param boundaries the boundaries of the utterance
	 * @param counter the subsequence counter to discount scores by, null if not needed
	 * @return the frequency of the words in the utterance
	 */
	public double[] utteranceWordsScores(String[] units, Boolean[] stresses, Boolean[] boundaries, 
			SubSeqCounter counter) {
		// Get slices of the stresses and units in this utterance
		Object[][] wordsUnits = 
			SegUtil.slicesFromAllBoundaries(units, boundaries);
		Object[][] wordsStresses = 
			SegUtil.slicesFromAllBoundaries(stresses, boundaries);
		
		// Each element of the outer array represents units/stresses for a single word
		double[] wordsScores = new double[wordsUnits.length];
		for (int i = 0; i < wordsUnits.length; i++) {
			Word w = getWord((String[]) wordsUnits[i], ((Boolean[]) wordsStresses[i]));
			// If the word is missing, give the initial score
			if (w == null) {
				wordsScores[i] = getNewWordScore();
			}
			else {
				// If it's there, smooth up to the minimum if needed
				wordsScores[i] = getScore(w, counter);
			}
		}
		return wordsScores;
	}
	
	/**
	 * Gives the minimum smoothed word score taking normalization into account.
	 * @return minimum smoothed word score
	 */
	private double getSmoothingMin(){
		return NORMALIZATION ? smoothingMin / numTokens : smoothingMin;
	}
	
	/**
	 * Gives the new word score taking normalization into account.
	 * @return new word score
	 */
	private double getNewWordScore(){
		return NORMALIZATION ? unknownWordScore / numTokens : unknownWordScore;
	}
	
	/**
	 * Return an ArrayList of Words that are prefixes of the utterance at the given
	 * index. Throws a RuntimeException if the index is not a valid index
	 * for the length of the utterance.
	 * @param utt the utterance to search
	 * @param index the index of the first element to examine
	 * @return an ArrayList of Words that are prefixes, in order of increasing length
	 */
	public ArrayList<Word> getPrefixWords(Utterance utt, int index) {
		String[] units = utt.getUnits(); 
		Boolean[] stresses = utt.getStresses();
		ArrayList<Word> prefixWords = new ArrayList<Word>();
		
		// Reject index if it's too high
		if (index >= units.length || index < 0)
			throw new RuntimeException("Starting index out of range.");
		
		// Take progressively larger slices of the utterance. Since the
		// slice end index is exclusive, i can go up to units.length
		for (int i = index + 1; i <= units.length; i++) {
			String[] prefixUnits = Arrays.copyOfRange(units, index, i);
			Boolean[] prefixStresses = Arrays.copyOfRange(stresses, index, i);
			
			// Add to the list if the current prefix is a word with a positive
			// score
			Word w = getWord(prefixUnits, prefixStresses);
			if (w != null && recallWord(w)) prefixWords.add(w);
		}

		return prefixWords;
	}
	
	
	/**
	 * Create a lexicon from segmented utterances.
	 * @param utterances the segmented utterances to build a lexicon from
	 * @return a lexicon matching the utterances
	 */
	public static Lexicon lexiconFromUtterances(Collection<Utterance> utterances, 
			boolean stressSensitive) {
		// Make a new lexicon with trace off
		Lexicon lex = new Lexicon(stressSensitive, false, false, false, false, 0.0, 0.0, null);
		// Increment the words in each utterance
		for (Utterance utt : utterances) {
			lex.incUtteranceWords(utt.getUnits(), utt.getStresses(), 
					utt.getBoundariesCopy(), null);
		}
		return lex;
	}


	/**
	 * Return the word in the bad segmentation that is the first difference
	 * between the two segmentations. null will be returned if the word
	 * is not in the lexion.
	 * @param utt
	 * @param goodSeg
	 * @param badSeg
	 * @return
	 */
	public Word getSplitWord(Utterance utt, Boolean[] goodSeg, Boolean[] badSeg) {
		// Find the word that they split on
		// Get slices of the stresses and units in each segmentation
		Object[][] goodWordsUnits = 
			SegUtil.slicesFromAllBoundaries(utt.getUnits(), goodSeg);
		Object[][] badWordsUnits = 
			SegUtil.slicesFromAllBoundaries(utt.getUnits(), badSeg);
		Object[][] badWordsStresses = 
			SegUtil.slicesFromAllBoundaries(utt.getStresses(), badSeg);
		
		// Find the first difference
		String [] blameWordUnits = null;
		Boolean [] blameWordStresses = null;
		for (int i = 0; i < Math.min(goodWordsUnits.length, badWordsUnits.length); i++) {
			if (!Arrays.equals((String[]) goodWordsUnits[i], (String[]) badWordsUnits[i])) {
				blameWordUnits = (String[]) badWordsUnits[i];
				blameWordStresses = (Boolean[]) badWordsStresses[i];
				break;
			}
		}
		
		if(blameWordUnits == null || blameWordStresses == null)
			throw new RuntimeException("Failed to find difference between segmentations.");
		
		return getWord(blameWordUnits, blameWordStresses);
	}
	
	
	/**
	 * Perform any maintenance operations after each utterance.
	 */
	public void tick() {
		time++;
	}
	
	
	/**
	 * Return the score of a word. Wraps the word function and provides the time
	 * to it.
	 * @param w the word to get the score of
	 * @return the word's score at the current time
	 */
	public double getScore(Word w, SubSeqCounter counter) {
		// Smooth sub-minimal scores
		double score = Math.max(w.getScore(time), getSmoothingMin());
		
		// Account for normalization, and then sequence frequency
		score = NORMALIZATION ? score / numTokens : score;
		score = counter != null ? score / counter.get(w.units) : score;
		return score;
	}

	
	private double probMemRecallRate(double rawScore) {
		return 1.0 - Math.exp(-probAmount * rawScore);
	}
	
	
	public class WordScoreComparator implements Comparator<Word> {
		/* (non-Javadoc)
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(Word o1, Word o2) {
			return (int) Math.signum(o1.getScore(time) - o2.getScore(time));
		}
	}

	
	/**
	 * Return a formatted version of a word for dumps.
	 * @param w the word
	 * @return a formatted string representation
	 */
	public String dumpWord(Word w) {
		return w.toOutputString(time);
	}
}
