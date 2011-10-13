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

package edu.upenn.ircs.lignos.cats.metrics;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

import edu.upenn.ircs.lignos.cats.Utils;
import edu.upenn.ircs.lignos.cats.Utterance;
import edu.upenn.ircs.lignos.cats.lexicon.Lexicon;
import edu.upenn.ircs.lignos.cats.lexicon.Word;

public class Evaluation { 
	private final static int INTERVAL_SIZE = 500;
	
	public enum EvalMethod {
		BOUNDARIES, WORDS
	}
	
	public enum ErrorType {
		UNDERSEG, OVERSEG
	}
	
	/**
	 * Compute precision, recall, and f-score over ambiguous boundaries in
	 * utterances.
	 * @param goldUtterances the gold standard segmented utterances
	 * @param segUtterances the segmented utterances being tested
	 * @param method Evaluation method to use
	 * @return the precision, recall, and f-score for the tested segmentation
	 */
	public static Result evalUtterances(List<Utterance> goldUtterances, 
			List<Utterance> segUtterances, PrintStream evalLog, PrintStream log, 
			EvalMethod method) {
		// Corpus counters
		int truePositives = 0;
		int falsePositives = 0;
		int falseNegatives = 0;

		// Interval counters
		int intTruePositives = 0;
		int intFalsePositives = 0;
		int intFalseNegatives = 0;

		// Write interval header
		if (log != null) {
			switch (method) {
			case BOUNDARIES: log.println("Interval,Precision,Recall,Fscore"); break;
			case WORDS: log.println("Utt,Error,Type"); break;
			}
		}

		// Write log header
		if (evalLog != null) {
			evalLog.println("Gold,Seg,TP,FP,FN,Prec,Recall,Fscore");
		}

		// Set up iteration over utterances
		Iterator<Utterance> goldIter = goldUtterances.iterator();
		Iterator<Utterance> segIter = segUtterances.iterator();
		int nIntervalUtts = 0;
		int nUtts = 0;
		while (goldIter.hasNext()) {
			// Get utterances and boundaries
			Utterance gold = goldIter.next();
			Utterance seg = segIter.next();		
			boolean[] goldBound = gold.getBoundariesCopy();
			boolean[] segBound = seg.getBoundariesCopy();

			// Per-utterance counters
			int uttTruePositives = 0;
			int uttFalsePositives = 0;
			int uttFalseNegatives = 0;

			// Skip everything if this is a totally unambiguous utterance
			if (goldBound.length == 0)
				continue;

			// Increment counters now so they don't count if the utterance is skipped
			nIntervalUtts++;
			nUtts++;

			// Tracking variables for error analysis
			String errorWord;
			ErrorType errorType;

			switch (method) {
			case BOUNDARIES:
				// Compare boundaries directly between gold and segmented
				for (int i = 0; i < goldBound.length; i++) {
					if (goldBound[i] && segBound[i]) {
						truePositives++;
						uttTruePositives++;
						intTruePositives++;
					}
					else if (goldBound[i]) {
						falseNegatives++;
						uttFalseNegatives++;
						intFalseNegatives++;
					}
					else if (segBound[i]) {
						falsePositives++;
						uttFalsePositives++;
						intFalsePositives++;
					}
				}
				break;
			case WORDS:
				// Compare predicted words at each location. The words always
				// have the same length (although varying numbers of nulls),
				// so iteration over either is fine
				String[] goldWords = wordsPredicted(gold);
				String[] segWords = wordsPredicted(seg);
				for (int i = 0; i < goldWords.length; i++) {
					if (goldWords[i] != null && goldWords[i].equals(segWords[i])) {
						truePositives++;
						uttTruePositives++;
						intTruePositives++;
					}
					else {
						if (goldWords[i] != null && segWords[i] == null) {
							falseNegatives++;
							uttFalseNegatives++;
							intFalseNegatives++;
						}
						else if (segWords[i] != null) {
							falsePositives++;
							uttFalsePositives++;
							intFalsePositives++;
						}
						
						// Track predicted word errors and log them
						if (log != null && segWords[i] != null && goldWords[i] != null) {
							errorWord = segWords[i];
							errorType = errorWord.length() < goldWords[i].length() ? 
									ErrorType.OVERSEG : ErrorType.UNDERSEG;
							log.println(nUtts + "," + errorWord + "," + errorType);
						}
					}					
				}

				break;
			}

			// Log the evaluation if needed
			if (evalLog != null) {
				Result r = Result.calcResult(uttTruePositives, uttFalsePositives, 
						uttFalseNegatives);

				evalLog.println(gold.getSegText() + "," + seg.getSegText() + "," +
						uttTruePositives + "," + uttFalsePositives + "," +
						uttFalseNegatives + "," + r.toCSVString());
			}

			// Print interval results and reset interval, if needed
			if (log != null) {
				if (method == EvalMethod.BOUNDARIES && nIntervalUtts % INTERVAL_SIZE == 0) {
					Result r = Result.calcResult(intTruePositives, intFalsePositives, 
							intFalseNegatives);
					log.println(nUtts + "," + r.toCSVString());
					intTruePositives = intFalseNegatives = intFalsePositives = 0;
					nIntervalUtts = 0;
				}
			}
		}

		Result finalResult = Result.calcResult(truePositives, falsePositives, 
				falseNegatives);
		if(log != null) {
			switch (method) {
			case BOUNDARIES: log.println("Final," + finalResult.toCSVString()); break;
			}
		}

		return finalResult;
	}
	
	
	/**
	 * Compute precision, recall, and f-score over lexicon entries.
	 * @param goldLex the lexicon built from the gold standard utterances
	 * @param segLex the lexicon being tested
	 * @return the precision, recall, and f-score for the tested segmentation
	 */
	public static Result evalLexicons(Lexicon goldLex, Lexicon segLex, PrintStream log) {
		int truePositives = 0;
		int falsePositives = 0;
		int falseNegatives = 0;
		
		int numStressWords = 0;
		int trochaicWords = 0;
		
		// First check precision. The iterator returns all items in the lexicon,
		// some of which are not considered real words
		Iterator<Word> segIter = segLex.getWords().iterator();
		while (segIter.hasNext()) {
			Word seg = segIter.next();
			
			// Skip words if they are not really "in" the lexicon
			if (!segLex.isEvalWord(seg)) {
				continue;
			}
			
			// Check if this word appears in the gold lexicon
			if (goldLex.getWord(seg.units, seg.stresses) != null) {
				truePositives++;
			}
			else {
				if (log != null) log.println("FP: " + Utils.formatUnits(seg.units, seg.stresses));
				falsePositives++;
			}
			
			// Note the stress of the word if it's more than one syllable
			if (seg.length > 1) {
				if (seg.isTrochaic()) {
					trochaicWords++;
				}
				numStressWords++;
			}
		}
		
		// Print the stress information and reset it
		System.out.println("Learner's lexicon trochaic stress rate: " + 
				(trochaicWords / (float) numStressWords));
		numStressWords = 0;
		trochaicWords = 0;
		
 		// Then check recall
		Iterator<Word> goldIter = goldLex.getWords().iterator();

		while (goldIter.hasNext()) {
			Word gold = goldIter.next();
			
			// Check if this word was missed in the seg lexicon
			if (!segLex.isEvalWord(gold.units, gold.stresses)) {
				if (log != null) log.println("Miss: " + Utils.formatUnits(gold.units, gold.stresses));
				falseNegatives++;
			}
			
			// Note the stress of the word if it's more than one syllable
			if (gold.length > 1) {
				if (gold.isTrochaic()) {
					trochaicWords++;
				}
				numStressWords++;
			}
		}
		
		// Print the stress information and reset it
		System.out.println("Gold lexicon trochaic stress rate: " + 
				(trochaicWords / (float) numStressWords));
		
		return Result.calcResult(truePositives, falsePositives, falseNegatives);	
	}
	

	/**
	 * Return strings predicted to start in each boundary
	 * @param utt The segmented utterance
	 * @return Words at each boundary
	 */
	public static String[] wordsPredicted(Utterance utt) {
		String[] units = utt.getUnits();
		boolean[] boundaries = utt.getBoundariesCopy();
		// The output of words is one longer than that boundaries as the first syllable doesn't
		// get a leading boundary
		String[] words = new String[boundaries.length + 1];
		
		// Seed words with the first unit
		int currWord = 0;
		words[currWord] = units[0];
		
		// Now build up the remaining words. Note that since units are one ahead
		// of boundaries, indices are offset by one
		for (int i = 0; i < boundaries.length; i++) {
			if (boundaries[i]) {
				// Move the current word to one ahead of the boundaries and 
				// start a new word
				currWord = i + 1;
				words[currWord] = units[i + 1];
			}
			else {
				// Add the current unit to the current word. Since units are one ahead
				// of boundaries, offset by one
				words[currWord] += "|" + units[i + 1];
			}
		}

		return words;
	}
}
