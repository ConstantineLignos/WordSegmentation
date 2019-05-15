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

package edu.upenn.ircs.lignos.cats;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import edu.upenn.ircs.lignos.cats.counters.SubSeqCounter;
import edu.upenn.ircs.lignos.cats.lexicon.Lexicon;
import edu.upenn.ircs.lignos.cats.lexicon.Word;
import edu.upenn.ircs.lignos.cats.metrics.Evaluation;
import edu.upenn.ircs.lignos.cats.metrics.Evaluation.EvalMethod;
import edu.upenn.ircs.lignos.cats.metrics.Result;
import edu.upenn.ircs.lignos.cats.segmenters.BeamSubtractiveSegmenter;
import edu.upenn.ircs.lignos.cats.segmenters.GambellYangSegmenter;
import edu.upenn.ircs.lignos.cats.segmenters.RandomSegmenter;
import edu.upenn.ircs.lignos.cats.segmenters.Segmenter;
import edu.upenn.ircs.lignos.cats.segmenters.SubtractiveSegmenter;
import edu.upenn.ircs.lignos.cats.segmenters.TPTroughSegmenter;
import edu.upenn.ircs.lignos.cats.segmenters.UnitSegmenter;
import edu.upenn.ircs.lignos.cats.segmenters.UtteranceSegmenter;

public class Segment {
	// Command line arguments
	public static final String NO_TEST_FILE = "none";

	// Parameter names used for reading from property files
	// Stress sensitive lookup is public because it affects lexicon creation
	public static final String STRESS_SENSITIVE_PROP = "Stress_sensitive_lookup";
	private static final String SEGMENTER_PROP = "Segmenter";
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
	private static final String SEGMENTER_TROUGH = "Trough";
	private static final String SEGMENTER_GY = "GambellYang";
	private static final String SEGMENTER_SUBTRACTIVE = "Subtractive";

	// Experimental controls
	// TODO: Consider just making this compile-time as it's pretty much useless
	public boolean STRESS_SENSITIVE_LOOKUP;
	public String SEGMENTER_NAME;
	public boolean DROP_STRESS;

	// Lexicon
	public boolean USE_SUBSEQ_DISCOUNT;
	public boolean NORMALIZATION;
	public boolean USE_PROB_MEM;
	public double PROB_AMOUNT;
	public double DECAY_AMOUNT;

	// Segmenter-specific
	// Beam subtractive
	public boolean USE_TRUST;
	public boolean LONGEST;
	public boolean USE_STRESS;
	public boolean RANDOMIZATION;
	public int BEAM_SIZE;
	// Random
	public double RANDOM_SEG_THRESHOLD;

	// Debugging info
	private boolean LEX_TRACE;
	private boolean SEG_TRACE;
	private boolean SEG_EVAL_TRACE;
	private boolean LEX_EVAL_TRACE;

	// Learner structures
	private String outputBase;
	private SubSeqCounter counter;
	public Lexicon lexicon;
	private Segmenter seg;

	public Segment(Properties props, String outputBase) {
		this.outputBase = outputBase;
		setParams(props);
	}

	/**
	 * Set parameters from a property file
	 * @param propertyFile Path to the property file
	 */
	public boolean setParams(Properties props) {
		// TODO Consider having it fall back to defaults for missing props

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
		outputBase += "_" + SEGMENTER_NAME;
		if (SEGMENTER_NAME.equals(SEGMENTER_BEAM_SUBTRACTIVE)) outputBase += "_" + BEAM_SIZE;
		if (USE_STRESS) outputBase += "_stress"; else outputBase += "_nostress";
		if (USE_PROB_MEM) outputBase += "_probmem"; else outputBase += "_perfectmem";
		if (USE_TRUST) outputBase += "_trust"; else outputBase += "_notrust";
		if (USE_STRESS && DROP_STRESS) outputBase += "reduced";

		return true;
	}

	/**
	 * Run the segmenter on each utterance.
	 * @param verbose TODO
	 */
	public void segment(List<Utterance> segUtterances, boolean training, boolean verbose) {
		// We need to either be training or already have a lexicon
		assert(training || lexicon != null);

		if (verbose) System.out.println("Initializing...");
		// Create empty counter
		counter = training && USE_SUBSEQ_DISCOUNT ? new SubSeqCounter() : null;

		// Create empty segmentation lexicon
		if (training) {
			lexicon = new Lexicon(STRESS_SENSITIVE_LOOKUP, LEX_TRACE, USE_TRUST,
					USE_PROB_MEM, NORMALIZATION, PROB_AMOUNT, DECAY_AMOUNT, counter);
		}

		if (verbose) System.out.println("Segmenting...");
		long segTime = System.currentTimeMillis();

		// Create the segmenter if we're training
		if (training) {
			if (SEGMENTER_NAME.equals(SEGMENTER_BEAM_SUBTRACTIVE)) {
				seg = new BeamSubtractiveSegmenter(LONGEST, USE_STRESS, BEAM_SIZE, lexicon, counter,
						RANDOMIZATION);
			}
			else if (SEGMENTER_NAME.equals(SEGMENTER_UNIT)) {
				seg = new UnitSegmenter(lexicon);
			}
			else if (SEGMENTER_NAME.equals(SEGMENTER_UTTERANCE)) {
				seg = new UtteranceSegmenter(lexicon);
			}
			else if (SEGMENTER_NAME.equals(SEGMENTER_SUBTRACTIVE)) {
				seg = new SubtractiveSegmenter(lexicon);
			}
			else if (SEGMENTER_NAME.equals(SEGMENTER_GY)) {
				seg = new GambellYangSegmenter(lexicon, USE_STRESS);
			}
			else if (SEGMENTER_NAME.equals(SEGMENTER_RANDOM)) {
				seg = new RandomSegmenter(RANDOM_SEG_THRESHOLD, lexicon);
			}
			else if (SEGMENTER_NAME.equals(SEGMENTER_TROUGH)) {
				seg = new TPTroughSegmenter(lexicon);
			}
			else {
				throw new RuntimeException("Unknown segmenter specified: " + SEGMENTER_NAME);
			}
		}

		// Segment
		for (Utterance utterance : segUtterances) {
			utterance.setBoundaries(seg.segment(utterance, training, SEG_TRACE));
			if (SEG_TRACE) {
				System.out.println("Segmentation:" + utterance.getSegText());
			}

			// Tick the lexicon in training mode
			if (training) {
				lexicon.tick();
			}
		}

		segTime = System.currentTimeMillis() - segTime;
		// Output stats
		if (verbose) {
			System.out.println(seg.getStats());
			System.out.println((training ? "Training" : "Testing") + " took " + segTime / 1000F +
					" seconds.");
		}
	}


	/**
	 * Evaluate the segmentation against gold
	 */
	public Result[] eval(List<Utterance> goldTrainUtterances, List<Utterance> segTrainUtterances,
			List<Utterance> goldTestUtterances, List<Utterance> segTestUtterances,
			Lexicon goldTrainLexicon, Lexicon segTrainLexicon, boolean verbose) {
		boolean useTestData = goldTestUtterances != null;
		List<Utterance> goldEvalUtterances = useTestData ? goldTestUtterances : goldTrainUtterances;
		List<Utterance> segEvalUtterances = useTestData ? segTestUtterances : segTrainUtterances;

		if (goldEvalUtterances.size() != segEvalUtterances.size()) {
			throw new RuntimeException("Different length gold and test utterances.");
		}

		if (verbose) System.out.println("Evaluating...");

		// Set up logs
		PrintStream segLog = null;
		PrintStream perfLog = null;
		PrintStream wordLog = null;
		PrintStream lexLog = null;
		try {
			segLog = SEG_EVAL_TRACE ?
					new PrintStream(outputBase + "_segeval.csv") : null;
		} catch (FileNotFoundException e) {
			System.err.println("Couldn't open evaluation log file");
		}
		try {
			perfLog = SEG_EVAL_TRACE ?
					new PrintStream(outputBase + "_perflog.csv") : null;
		} catch (FileNotFoundException e) {
			System.err.println("Couldn't open perf log file");
		}
		try {
			wordLog = SEG_EVAL_TRACE ?
					new PrintStream(outputBase + "_word.csv") : null;
		} catch (FileNotFoundException e) {
			System.err.println("Couldn't open word log file");
		}
		try {
			lexLog = LEX_EVAL_TRACE ?
					new PrintStream(outputBase + "_lexeval.txt") : null;
		} catch (FileNotFoundException e) {
			System.err.println("Couldn't open evaluation log file");
		}

		// If we're eval-ing on the test data, pass null for the perflog, and then do a second
		// pass on the training data solely for the purpose of performance logging.
		Result boundaryResult = Evaluation.evalUtterances(goldEvalUtterances, segEvalUtterances,
				segLog, useTestData ? null : perfLog, EvalMethod.BOUNDARIES);
		if (perfLog != null && useTestData) {
			// Pass null for the segLog since this it should only show the testing evaluation
			Evaluation.evalUtterances(goldTrainUtterances, segTrainUtterances,
					null, perfLog, EvalMethod.BOUNDARIES);
		}

		// Word tokens
		Result wordTokensResults = Evaluation.evalUtterances(goldEvalUtterances, segEvalUtterances,
				segLog, wordLog, EvalMethod.WORDS);

		// Word types
		Lexicon goldEvalLexicon = Lexicon.lexiconFromUtterances(goldEvalUtterances,
				goldTrainLexicon.stressSensitive);
		Lexicon segEvalLexicon = Lexicon.lexiconFromUtterances(segEvalUtterances,
				goldTrainLexicon.stressSensitive);
		Result wordTypesResult = Evaluation.evalLexicons(goldEvalLexicon, segEvalLexicon, null, false);
		Result lexResult = Evaluation.evalLexicons(goldTrainLexicon, segTrainLexicon, lexLog, verbose);

		if (verbose) {
			System.out.println("Boundaries:");
			System.out.println(boundaryResult);
			System.out.println("Word tokens:");
			System.out.println(wordTokensResults.toStringNoAPrimeBDoublePrime());
			System.out.println("Word types:");
			System.out.println(wordTypesResult.toStringPRF());
			System.out.println("Lexicon:");
			System.out.println(lexResult.toStringPRF());
		}

		// Close any open logs
		if (segLog != null) segLog.close();
		if (perfLog != null) perfLog.close();
		if (wordLog != null) wordLog.close();
		if (lexLog != null) lexLog.close();

		return new Result[] {boundaryResult, wordTokensResults, wordTypesResult, lexResult};
	}


	/**
	 * Write output to the pre-set path.
	 */
	public void writeOutput(List<Utterance> segUtterances, Lexicon segLexicon) {
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
		props.setProperty(USE_STRESS_PROP, "false");

		// Lexicon behavior parameters
		comments.append(PROB_MEM_PROP + ": Whether recall of words from the lexicon is proabilistic.\n");
		props.setProperty(PROB_MEM_PROP, "false");
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
		else if (argv.length == 4) {
			String trainPath = argv[0];
			String testPath = argv[1];
			String outPath = argv[2];
			String propsPath = argv[3];
			callSegmenter(trainPath, testPath, outPath, propsPath);
		}
		else {
			// If we fell through, print usage
			System.err.println("Usage: Segment train_file test_file|none output_base properties_file");
			System.err.println("To generate a properties file with defaults, run:");
			System.err.println("Segment --dump-defaults");
			System.exit(64);
		}
	}

	public static Result[] callSegmenter(String trainPath, String testPath, String outPath,
			String propsPath){
		long startTime = System.currentTimeMillis();

		// Decide whether we're going to separate test and training data
		boolean useTestData = !NO_TEST_FILE.equals(testPath.toLowerCase());

		// Load gold utterances and lexicon
		long loadTime = System.currentTimeMillis();
		List<Utterance> goldTrainUtterances = Utterance.loadUtterances(trainPath);
		if (goldTrainUtterances == null) {
			System.err.println("Could not reading training file " + trainPath);
			System.exit(1);
		}
		loadTime = System.currentTimeMillis() - loadTime;
		System.out.println("Loading training data took " + loadTime / 1000F + " seconds.");

		List<Utterance> goldTestUtterances = null;
		if (useTestData) {
			loadTime = System.currentTimeMillis();
			goldTestUtterances = Utterance.loadUtterances(testPath);
			if (goldTestUtterances == null) {
				System.err.println("Could not reading testing file " + testPath);
				System.exit(1);
			}
			loadTime = System.currentTimeMillis() - loadTime;
			System.out.println("Loading testing data took " + loadTime / 1000F + " seconds.");
		}

		// Load props
		Properties props = Utils.loadProps(propsPath);
		boolean stress_sensitive_lookup = new Boolean(props.getProperty(STRESS_SENSITIVE_PROP));
		String segmenterName = props.getProperty(SEGMENTER_PROP);
		System.out.println("Running segmenter " + segmenterName);

		// Create the gold lexicons
		Lexicon goldTrainLexicon = Lexicon.lexiconFromUtterances(goldTrainUtterances,
				stress_sensitive_lookup);
		Lexicon goldTestLexicon = null;
		if (useTestData) {
			goldTestLexicon = Lexicon.lexiconFromUtterances(goldTrainUtterances,
					stress_sensitive_lookup);
		}

		// Segment
		Result[] evalResults = runSegmenter(goldTrainUtterances, goldTrainLexicon,
				goldTestUtterances, goldTestLexicon, props, outPath, true);

		long endTime = System.currentTimeMillis() - startTime;
		System.out.println("Run took " + endTime / 1000F + " seconds.");
		return evalResults;
	}

	public static Result[] runSegmenter(List<Utterance> goldTrainUtterances,
			Lexicon goldTrainLexicon, List<Utterance> goldTestUtterances, Lexicon goldTestLexicon,
			Properties props, String outPath, boolean verbose) {
		boolean useTestData = goldTestUtterances != null;
		Segment seg = new Segment(props, outPath);

		// Copy utterances into segUtterances
		boolean dropStress = seg.DROP_STRESS;
		List<Utterance> segTrainUtterances = Utterance.segUtterances(goldTrainUtterances,
				dropStress);
		List<Utterance> segTestUtterances = useTestData ?
				Utterance.segUtterances(goldTestUtterances, dropStress) : null;

		// Train and test
		seg.segment(segTrainUtterances, true, verbose);
		if (useTestData) {
			seg.segment(segTestUtterances, false, verbose);
		}

		// Output eval. It always gets the goldTrainLexicon because the lexicon is only learned
		// during training.
		Result[] evalResults = seg.eval(goldTrainUtterances, segTrainUtterances,
				goldTestUtterances, segTestUtterances, goldTrainLexicon, seg.lexicon, verbose);
		seg.writeOutput(segTrainUtterances, seg.lexicon);

		return evalResults;
	}
}
