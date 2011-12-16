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
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Properties;

import edu.upenn.ircs.lignos.cats.counters.SubSeqCounter;
import edu.upenn.ircs.lignos.cats.lexicon.Lexicon;
import edu.upenn.ircs.lignos.cats.lexicon.Word;
import edu.upenn.ircs.lignos.cats.metrics.Evaluation;
import edu.upenn.ircs.lignos.cats.metrics.Result;
import edu.upenn.ircs.lignos.cats.metrics.Evaluation.EvalMethod;
import edu.upenn.ircs.lignos.cats.segmenters.*;

public class Segment {
	// Parameter names used for reading from property files
	private static final String SEGMENTER_PROP = "Segmenter";
	private static final String STRESS_SENSITIVE_PROP = "Stress_sensitive_lookup";
	private static final String TRUST_PROP = "Use_trust";
	private static final String DROP_STRESS_PROP = "Drop_stress";
	private static final String USE_STRESS_PROP = "Use_stress";
	private static final String PROB_MEM_PROP = "Use_prob_mem";
	private static final String NORMALIZATION_PROP = "Lex_normalization";
	private static final String RANDOMIZATION_PROP = "Use_randomization";
	private static final String SUBSEQDISCOUNT_PROP = "Use_subseqdiscount";
	private static final String PROB_MEM_AMOUNT_PROP = "Prob_mem_amount";
	private static final String DECAY_AMT_PROP = "Decay_amount";
	private static final String LONGEST_PROP = "Longest";
	private static final String RANDOM_SEG_THRESHOLD_PROP = "Random_Seg_Rate";
	private static final String BEAM_SIZE_PROP = "Beam_size";
	private static final String LEX_TRACE_PROP = "Lex_trace"; 
	private static final String SEG_TRACE_PROP = "Seg_trace";
	private static final String SEG_EVAL_LOG_PROP = "Seg_logging";
	private static final String LEX_EVAL_LOG_PROP = "Lex_logging";
	
	// Known segmenters
	private static final String SEGMENTER_BEAM_SUBTRACTIVE = "BeamSubtractive";
	private static final String SEGMENTER_UNIT = "Unit";
	private static final String SEGMENTER_UTTERANCE = "Utterance";
	private static final String SEGMENTER_RANDOM = "Random";

	// Experimental controls
	private String SEGMENTER_NAME;
	private boolean STRESS_SENSITIVE_LOOKUP;
	private boolean USE_TRUST;
	private boolean LONGEST;
	private double RANDOM_SEG_THRESHOLD;
	private boolean DROP_STRESS;
	private boolean USE_STRESS;
	private boolean USE_PROB_MEM;
	private boolean USE_SUBSEQ_DISCOUNT;
	private boolean NORMALIZATION;
	private boolean RANDOMIZATION;
	private int BEAM_SIZE;
	private double PROB_AMOUNT;
	private double DECAY_AMOUNT;
	
	// Debugging info
	private boolean LEX_TRACE;
	private boolean SEG_TRACE;
	private boolean SEG_EVAL_TRACE;
	private boolean LEX_EVAL_TRACE;
	
	// Learner structures
	private String inputPath;
	private String outputBase;
	private List<Utterance> goldUtterances;
	private List<Utterance> segUtterances;
	private Lexicon goldLexicon;
	private Lexicon segLexicon;
	private SubSeqCounter counter;
	
	
	public Segment(String inputPath, String outputBase) {
		this.inputPath = inputPath;
		this.outputBase = outputBase;
	}
	
	/**
	 * Set parameters from a property file
	 * @param propertyFile Path to the property file
	 */
	public boolean setParams(String propertyFile) {
		// TODO Consider having it fall back to defaults for missing props
		
		Properties props = new Properties();
		try {
			props.load(new FileReader(propertyFile));
		} catch (FileNotFoundException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
		// Look up each property
		SEGMENTER_NAME = props.getProperty(SEGMENTER_PROP);
		STRESS_SENSITIVE_LOOKUP = new Boolean(props.getProperty(STRESS_SENSITIVE_PROP));
		USE_TRUST = new Boolean(props.getProperty(TRUST_PROP));
		LONGEST = new Boolean(props.getProperty(LONGEST_PROP));
		DROP_STRESS = new Boolean(props.getProperty(DROP_STRESS_PROP));
		USE_STRESS = new Boolean(props.getProperty(USE_STRESS_PROP));
		USE_PROB_MEM = new Boolean(props.getProperty(PROB_MEM_PROP));
		BEAM_SIZE = new Integer(props.getProperty(BEAM_SIZE_PROP));
		PROB_AMOUNT = new Double(props.getProperty(PROB_MEM_AMOUNT_PROP));
		DECAY_AMOUNT = new Double(props.getProperty(DECAY_AMT_PROP));
		LEX_TRACE = new Boolean(props.getProperty(LEX_TRACE_PROP));
		SEG_TRACE = new Boolean(props.getProperty(SEG_TRACE_PROP));
		SEG_EVAL_TRACE = new Boolean(props.getProperty(SEG_EVAL_LOG_PROP));
		LEX_EVAL_TRACE = new Boolean(props.getProperty(LEX_EVAL_LOG_PROP));
		RANDOM_SEG_THRESHOLD = new Double(props.getProperty(RANDOM_SEG_THRESHOLD_PROP));
		NORMALIZATION = new Boolean(props.getProperty(NORMALIZATION_PROP));
		RANDOMIZATION = new Boolean(props.getProperty(RANDOMIZATION_PROP));
		USE_SUBSEQ_DISCOUNT = new Boolean(props.getProperty(SUBSEQDISCOUNT_PROP));
		
		// Set up the output path
		if (USE_STRESS) outputBase += "_stress"; else outputBase += "_nostress";
		if (USE_STRESS && DROP_STRESS) outputBase += "reduced";
		if (USE_PROB_MEM) outputBase += "_probmem"; else outputBase += "_perfectmem";
		if (BEAM_SIZE > 1) outputBase += "_beam_" + BEAM_SIZE;
		
		return true;
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
		segLexicon = new Lexicon(STRESS_SENSITIVE_LOOKUP, LEX_TRACE, USE_TRUST,
				USE_PROB_MEM, NORMALIZATION, PROB_AMOUNT, DECAY_AMOUNT);
		
		// Create empty counter
		counter = USE_SUBSEQ_DISCOUNT ? new SubSeqCounter() : null;
		
		System.out.println("Done loading utterances.");
		
		// Return true if load succeeded
		return true;
	}
	
	
	/**
	 * Run the segmenter on each utterance.
	 */
	private void segment() {
		System.out.println("Segmenting...");
		
		// Create the segmenter
		Segmenter seg; 
		if (SEGMENTER_NAME.equals(SEGMENTER_BEAM_SUBTRACTIVE)) {
			seg = new BeamSubtractiveSegmenter(LONGEST, USE_STRESS, BEAM_SIZE, segLexicon, counter, 
					RANDOMIZATION);
		}
		else if (SEGMENTER_NAME.equals(SEGMENTER_UNIT)) {
			seg = new UnitSegmenter(segLexicon);
		}
		else if (SEGMENTER_NAME.equals(SEGMENTER_UTTERANCE)) {
			seg = new UtteranceSegmenter(segLexicon);
		}
		else if (SEGMENTER_NAME.equals(SEGMENTER_RANDOM)) {
			seg = new RandomSegmenter(RANDOM_SEG_THRESHOLD, segLexicon);
		}
		else {
			throw new RuntimeException("Unknown segmenter specified: " + SEGMENTER_NAME);
		}

		// Segment
		for (Utterance utterance : segUtterances) {
			utterance.setBoundaries(seg.segment(utterance, SEG_TRACE));
			if (SEG_TRACE) {
				System.out.println("Segmentation:" + utterance.getSegText());
			}
			
			// Tick the lexicon
			segLexicon.tick();
		}
		
		// Output stats
		System.out.println(seg.getStats());
		System.out.println("Done segmenting.");
	}
	
	
	/**
	 * Evaluate the segmentation against gold
	 */
	private Result eval() {
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
		return boundaryResult;
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
	
	private static class CommentedProperties {
		private Properties properties;
		private String comments;
	
		public CommentedProperties(Properties properties, String comments) {
			this.properties = properties;
			this.comments = comments;
		}

		public Properties getProperties() {return properties;}

		public String getComments() {return comments;}
	}
		
	private static CommentedProperties defaultProperties() {
		Properties props = new Properties();
		StringBuilder comments = new StringBuilder();

		// High level experimental parameters
		comments.append("You must leave all parameters defined when editing this file. " +
				"Leaving any parameters undefined may result in undefined behavior.\n");
		comments.append(SEGMENTER_PROP + ": Which segmenter to use.\n");
		props.setProperty(SEGMENTER_PROP, "BeamSubtractive");
		comments.append(STRESS_SENSITIVE_PROP + ": Whether lexicon entries are stress-sensitive. " +
				"Should be false unless you are changing the lookup functionality.\n");
		props.setProperty(STRESS_SENSITIVE_PROP, "false");
		comments.append(TRUST_PROP + ": Whether the segmenter is allowed to specify which " +
				"boundaries are to be trusted when adding words to the lexicon.\n");
		props.setProperty(TRUST_PROP, "true");
		comments.append(DROP_STRESS_PROP + ": Whether adjacent stresses in the input should be " +
				"reduced to better reflect natural speech patterns.\n");
		props.setProperty(DROP_STRESS_PROP, "true");
		comments.append(USE_STRESS_PROP + ": Whether the segmenter is given stress information.\n");
		props.setProperty(USE_STRESS_PROP, "true");
		
		// Lexicon behavior parameters
		comments.append(PROB_MEM_PROP + ": Whether recall of words from the lexicon is proabilistic.\n");
		props.setProperty(PROB_MEM_PROP, "true");
		comments.append(PROB_MEM_AMOUNT_PROP + ": Parameter for exponential function for probabilitic " + 
				"lexicon recall.\n");
		props.setProperty(PROB_MEM_AMOUNT_PROP, "0.05");
		comments.append(DECAY_AMT_PROP + ": Amount lexical entries decay after each utterance. " +
				"Experimental feature. Set to 0.0 to disable decay.\n");
		props.setProperty(DECAY_AMT_PROP, "0.0");
		comments.append(NORMALIZATION_PROP + ": Whether to normalize scores in the lexicon.\n");
		props.setProperty(NORMALIZATION_PROP, "false");
		
		// Segmenter behavior parameters
		comments.append(LONGEST_PROP + ": Whether a subtractive segmenter is forced to use the longest words " +
				"possible in its segmentation.\n");
		props.setProperty(LONGEST_PROP, "false");
		comments.append(BEAM_SIZE_PROP + ": Size of the beam for the beam segmenter. Set to 1 to use " +
				"greedy search.\n");
		props.setProperty(BEAM_SIZE_PROP, "2");
		comments.append(RANDOM_SEG_THRESHOLD_PROP + ": Rate at which the random segmenter places a boundary.\n");
		props.setProperty(RANDOM_SEG_THRESHOLD_PROP, "0.5");
		comments.append(RANDOMIZATION_PROP + ": Whether to randomize word subtraction and hypothesis selection.\n");
		props.setProperty(RANDOMIZATION_PROP, "false");
		comments.append(SUBSEQDISCOUNT_PROP + ": Whether to divide words scores by subsequence frequency.\n");
		props.setProperty(SUBSEQDISCOUNT_PROP, "false");
		
		// Logging parameters
		comments.append(LEX_TRACE_PROP + ": Whether to print debugging information for lexicon " +
				"stores and lookups\n");
		props.setProperty(LEX_TRACE_PROP, "false");
		comments.append(SEG_TRACE_PROP + ": Whether to print debugging information for " +
				"segmentation.\n");
		props.setProperty(SEG_TRACE_PROP, "false");
		comments.append(SEG_EVAL_LOG_PROP + ": Whether to write out information about the " +
				"evaluation of segmentation to a file.\n");
		props.setProperty(SEG_EVAL_LOG_PROP, "true");
		comments.append(LEX_EVAL_LOG_PROP + ": Whether to write out information about the " +
				"evaluation of the lexicon to a file.\n");
		props.setProperty(LEX_EVAL_LOG_PROP, "true");		

		return new CommentedProperties(props, comments.toString());
	}

	public static void main(String[] argv) {
		callSegmenter(argv);
	}
	
	public static Result callSegmenter(String[] argv){
		if (argv.length == 1) {
			if (argv[0].equals("--dump-defaults")) {
				CommentedProperties comProps = defaultProperties();
				try {
					comProps.getProperties().store(System.out, comProps.getComments());
				} catch (IOException e) {
					System.err.println("Couldn't write defaults to standard out.");
				}
				System.exit(1);
			}
		}
		else if (argv.length == 3) {
			// Start up a segmenter
			long startTime = System.currentTimeMillis();
			Segment seg = new Segment(argv[0], argv[1]);
			if (!seg.setParams(argv[2])) {
				System.err.println("The properties file " + argv[2] + " could not be read.");
				System.exit(1);
			}
			long loadTime = System.currentTimeMillis();
			if (!seg.load()) {
				System.err.println("The input file " + argv[0] + " could not be read.");
				System.exit(1);
			}
			loadTime = System.currentTimeMillis() - loadTime;
			System.out.println("Loading took " + loadTime / 1000F + " seconds.");
			long segTime = System.currentTimeMillis();
			seg.segment();
			segTime = System.currentTimeMillis() - segTime;
			System.out.println("Segmentation took " + segTime / 1000F + " seconds.");
			Result evalResult = seg.eval();
			seg.writeOutput();
			long endTime = System.currentTimeMillis() - startTime;
			System.out.println("Run took " + endTime / 1000F + " seconds.");
			return evalResult;
		}
		
		// If we fell through, print usage
		System.err.println("Usage: Segment input output_base properties_file");
		System.err.println("To generate a properties file with defaults, run:");
		System.err.println("Segment --dump-defaults");
		System.exit(2);
		
		// Satisfy compiler with guaranteed return
		return null;
	}
}
