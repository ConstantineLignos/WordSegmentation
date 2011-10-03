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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import edu.upenn.ircs.lignos.cats.lexicon.Lexicon;
import edu.upenn.ircs.lignos.cats.lexicon.Word;
import edu.upenn.ircs.lignos.cats.metrics.Evaluation;
import edu.upenn.ircs.lignos.cats.metrics.Result;
import edu.upenn.ircs.lignos.cats.metrics.Evaluation.EvalMethod;
import edu.upenn.ircs.lignos.cats.segmenters.*;

public class Segment {
	private static final boolean STRESS_SENSITIVE_LOOKUP = false;
	
	// Basic constants that stay fixed
	private static final boolean USE_TRUST = true;
	private static final boolean LONGEST = false;
	private static final boolean DROP_STRESS = true;
	
	// Experimental controls
	private static final boolean USE_STRESS = true;
	private static final boolean USE_LRP = false;
	private static final boolean USE_PROB_MEM = true;
	private static final int BEAM_SIZE = 2;
	private static final double PROB_AMOUNT = 0.05;
	private static final double DECAY_AMOUNT = 0.0;
	
	// Debugging info
	private static final boolean LEX_TRACE = false;
	private static final boolean SEG_TRACE = false;
	private static final boolean SEG_EVAL_TRACE = true;
	private static final boolean LEX_EVAL_TRACE = true;
	
	// Learner structures
	private String inputPath;
	private String outputBase;
	private List<Utterance> goldUtterances;
	private List<Utterance> segUtterances;
	private Lexicon goldLexicon;
	private Lexicon segLexicon;
	
	@SuppressWarnings("unused")
	public Segment(String inputPath, String outputBase) {
		this.inputPath = inputPath;
		if (USE_STRESS) outputBase += "_stress"; else outputBase += "_nostress";
		if (USE_STRESS && DROP_STRESS) outputBase += "reduced";
		if (USE_PROB_MEM) outputBase += "_probmem"; else outputBase += "_perfectmem";
		if (USE_LRP) outputBase += "_lrp";
		if (BEAM_SIZE > 1) outputBase += "_beam_" + BEAM_SIZE;
		this.outputBase = outputBase;
	}
	
	/**
	 * Load the input file, initializing goldUtterances and segUtterances
	 */
	private boolean load() {
		try {
			Scanner input = new Scanner(new File(inputPath));
			goldUtterances = new LinkedList<Utterance>();
			segUtterances = new LinkedList<Utterance>();
			// Parse each line as an utterance
			System.out.println("Loading utterances from " + inputPath + " ...");
			String line;
			while (input.hasNextLine()) {
				// Strip trailing whitespace
				line = input.nextLine().replaceAll("\\s+$", "");
				// Create gold and non-gold utterances
				goldUtterances.add(new Utterance(line, true, DROP_STRESS));
				segUtterances.add(new Utterance(line, false, DROP_STRESS));
			}
		} catch (FileNotFoundException e) {
			System.err.println("Could not read from input file: " + inputPath);
			return false;
		}
		
		// Build gold lexicon from gold utterances
		goldLexicon = Lexicon.lexiconFromUtterances(goldUtterances, STRESS_SENSITIVE_LOOKUP);
		
		// Create empty segmentation lexicon
		segLexicon = new Lexicon(STRESS_SENSITIVE_LOOKUP, LEX_TRACE, USE_TRUST, USE_LRP,
				USE_PROB_MEM, PROB_AMOUNT, DECAY_AMOUNT);
		
		System.out.println("Done loading utterances.");
		
		// Return true if load succeeded
		return true;
	}
	
	
	/**
	 * Run the segmenter on each utterance.
	 */
	private void segment() {
		System.out.println("Segmenting...");
		Segmenter seg = new BeamSubtractiveSegmenter(LONGEST, USE_STRESS, BEAM_SIZE);
		for (Utterance utterance : segUtterances) {
			utterance.setBoundaries(seg.segment(utterance, segLexicon, SEG_TRACE));
			if (SEG_TRACE) {
				System.out.println("Segmentation:" + utterance.getSegText());
			}
			
			// Tick the lexicon
			segLexicon.tick();
		}
		System.out.println(seg.getStats());
		System.out.println("Done segmenting.");
	}
	
	
	/**
	 * Evaluate the segmentation against gold
	 */
	private void eval() {
		System.out.println("Evaluating...");
		// TODO Refactor logging
		PrintStream segLog;
		PrintStream perfLog;
		PrintStream wordLog;
		try {
			segLog = SEG_EVAL_TRACE ? 
					new PrintStream(outputBase + "_segeval.csv") : null;
		} catch (FileNotFoundException e) {
			System.err.println("Couldn't open evaluation log file");
			segLog = null;
		}
		try {
			perfLog = SEG_EVAL_TRACE ? 
					new PrintStream(outputBase + "_perflog.csv") : null;
		} catch (FileNotFoundException e) {
			System.err.println("Couldn't open perf log file");
			perfLog = null;
		}
		try {
			wordLog = SEG_EVAL_TRACE ? 
					new PrintStream(outputBase + "_word.csv") : null;
		} catch (FileNotFoundException e) {
			System.err.println("Couldn't open word log file");
			wordLog = null;
		}
		Result boundaryResult = Evaluation.evalUtterances(goldUtterances, segUtterances, 
				segLog, perfLog, EvalMethod.BOUNDARIES);
		System.out.println("Boundaries:");
		System.out.println(boundaryResult);
		
		Result wordResult = Evaluation.evalUtterances(goldUtterances, segUtterances, 
				segLog, wordLog, EvalMethod.WORDS);
		System.out.println("Words:");
		System.out.println(wordResult);
		
		PrintStream lexLog;
		try {
			lexLog = LEX_EVAL_TRACE ? 
					new PrintStream(outputBase + "_lexeval.txt") : null;
		} catch (FileNotFoundException e) {
			System.err.println("Couldn't open evaluation log file");
			lexLog = null;
		}
		System.out.println("Lexicon:");
		Result lexResult = Evaluation.evalLexicons(goldLexicon, segLexicon, lexLog);
		
		// TODO Close logs properly
		
		System.out.println(lexResult);
		System.out.println("Done evaluating.");
	}
	

	/**
	 * Write output to the pre-set path.
	 */
	private void writeOutput() {
		try {
			// Write segmentation
			PrintStream out = new PrintStream(outputBase + "_seg.txt");
			for (Utterance utt : segUtterances) {
				out.println(utt.getSegText());
			}
			out.close();
			
			// Write lexicon, sorted by score
			ArrayList<Word> words = new ArrayList<Word>(segLexicon.getWords());
			Collections.sort(words, Collections.reverseOrder(segLexicon.new WordScoreComparator()));
			out = new PrintStream(outputBase + "_lex.txt");
			for (Word w : words) {
				out.println(segLexicon.dumpWord(w));
			}
			out.close();
			
		} catch (FileNotFoundException e) {
			System.err.println("Couldn't open output files");
		}
	}

	public static void main(String[] argv) {
		// Start up a segmenter
		if (argv.length == 2) {
			long startTime = System.currentTimeMillis();
			Segment seg = new Segment(argv[0], argv[1]);
			if (!seg.load()) {
				System.err.println("The input file " + argv[0] + " could not be read.");
				System.exit(1);
			}
			long segTime = System.currentTimeMillis();
			seg.segment();
			segTime = System.currentTimeMillis() - segTime;
			System.out.println("Segmentation took " + segTime / 1000F + " seconds.");
			seg.eval();
			seg.writeOutput();
			long endTime = System.currentTimeMillis() - startTime;
			System.out.println("Run took " + endTime / 1000F + " seconds.");
		}
		else {
			System.err.println("Usage: Segment input output_base");
			System.exit(2);
		}
	}
}
