package edu.upenn.ircs.lignos.cats;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import edu.upenn.ircs.lignos.cats.lexicon.Lexicon;
import edu.upenn.ircs.lignos.cats.metrics.Result;

public class SegExperiment implements Runnable  {

	private final SegmenterParams params;
	private final String[] results;
	private final int resultsIndex;

	/**
	 * @param args arguments to be given to callSegmenter
	 * @param results array to store results in
	 * @param resultsIndex index in results to use to store results
	 */
	SegExperiment(SegmenterParams params, String[] results, int resultsIndex) {
		this.params = params;
		this.results = results;
		this.resultsIndex = resultsIndex;
	}

	public static class SegmenterParams {
		public final List<Utterance> goldTrainUtterances;
		public final Lexicon goldTrainLexicon;
		public final List<Utterance> goldTestUtterances;
		public final Lexicon goldTestLexicon;
		public final Properties props;
		public final String outPath;
		public final String name;

		public SegmenterParams(List<Utterance> goldUtterances, Lexicon goldLexicon,
				List<Utterance> goldTestUtterances,	Lexicon goldTestLexicon,
				Properties props, String outPath, String name) {
			this.goldTrainUtterances = goldUtterances;
			this.goldTrainLexicon = goldLexicon;
			this.goldTestUtterances = goldTestUtterances;
			this.goldTestLexicon = goldTestLexicon;
			this.props = props;
			this.outPath = outPath;
			this.name = name;
		}
	}

	public void run() {
		System.out.println("Started " + params.name);
		Result[] segResults = Segment.runSegmenter(params.goldTrainUtterances,
				params.goldTrainLexicon, params.goldTestUtterances, params.goldTestLexicon,
				params.props, params.outPath, false);
		// Unpack results
		Result boundaryResult = segResults[0];
		Result wordTokenResult = segResults[1];
		Result wordTypeResult = segResults[2];
		Result lexResult = segResults[3];
		// Format output
		results[resultsIndex] = (params.name + "," +
				String.format("%1.4f,", boundaryResult.precision) +
				String.format("%1.4f,", boundaryResult.recall) +
				String.format("%1.4f,", boundaryResult.fScore) +
				String.format("%1.4f,", boundaryResult.hitRate) +
				String.format("%1.4f,", boundaryResult.faRate) +
				String.format("%1.4f,", boundaryResult.aPrime) +
				String.format("%1.4f,", boundaryResult.bDoublePrime) +
				String.format("%1.4f,", wordTokenResult.precision) +
				String.format("%1.4f,", wordTokenResult.recall) +
				String.format("%1.4f,", wordTokenResult.fScore) +
				String.format("%1.4f,", wordTypeResult.precision) +
				String.format("%1.4f,", wordTypeResult.recall) +
				String.format("%1.4f,", wordTypeResult.fScore) +
				String.format("%1.4f,", lexResult.precision) +
				String.format("%1.4f,", lexResult.recall) +
				String.format("%1.4f", lexResult.fScore));
		System.out.println("Finished " + params.name);
	}

	/**
	 * @param args Command line arguments
	 */
	public static void main(String[] args) {
		if (args.length != 5) {
			System.err.println("Usage: SegExperiment train_file test_file|none output_base propslist csv_output");
			System.exit(64);
		}

		// Extract arguments
		String trainPath = args[0];
		String testPath = args[1];
		String outBase = args[2];
		String propsListPath = args[3];
		String outPath = args[4];
		boolean useTestData = testPath.toLowerCase() != Segment.NO_TEST_FILE;

		// Read in the names of the props files
		List<String> propsFiles = new LinkedList<String>();
		Scanner propsScanner = null;
		try {
			propsScanner = new Scanner(new File(propsListPath));
		} catch (FileNotFoundException e1) {
			System.err.println("Could not read list of props files at " + propsListPath);
			System.exit(1);
		}
		while (propsScanner.hasNext()) {
			propsFiles.add(propsScanner.next());
		}
		propsScanner.close();

		// Read in the input
		List<Utterance> goldTrainUtterances = Utterance.loadUtterances(trainPath);
		if (goldTrainUtterances == null) {
			System.err.println("Could not read training file " + trainPath);
			System.exit(1);
		}

		List<Utterance> goldTestUtterances = null;
		if (useTestData) {
			goldTestUtterances = Utterance.loadUtterances(testPath);
			if (goldTrainUtterances == null) {
				System.err.println("Could not reading testing file " + testPath);
				System.exit(1);
			}
		}

		// Open the output
		PrintStream out = null;
		try {
			out = new PrintStream(outPath);
		}
		catch (FileNotFoundException e) {
			System.err.println("Couldn't open output file " + outPath);
			System.exit(1);
		}

		// Set up place to store the results of each line
		String[] outLines = new String[propsFiles.size()];
		System.out.println("Number of experiments to run: " + propsFiles.size());

		// Get the number of cores and start a thread pool
		int cores = Runtime.getRuntime().availableProcessors();
		System.out.println("Number of parallel workers: " + cores);
		ExecutorService pool = Executors.newFixedThreadPool(cores);
		long startTime = System.currentTimeMillis();

		// Index to store each result
		int idx = 0;
		for (String propsPath : propsFiles) {
			Properties props = Utils.loadProps(propsPath);

			// Because properties can vary in stress sensitive lookup, we have to make the lexicon
			// for each props
			boolean stress_sensitive_lookup = new Boolean(props.getProperty(Segment.STRESS_SENSITIVE_PROP));
			Lexicon goldLexicon = Lexicon.lexiconFromUtterances(goldTrainUtterances,
					stress_sensitive_lookup);

			// Clean up name
			String name = new File(propsPath).getName();
			if (name.contains(".")) {
				name = name.substring(0, name.lastIndexOf('.'));
			}
			SegmenterParams params = new SegmenterParams(goldTrainUtterances, goldLexicon,
					goldTestUtterances, goldLexicon, props,	outBase, name);
			pool.submit(new SegExperiment(params, outLines, idx++));
		}

		// Shut down the pool and wait (forever) until everything is done
		pool.shutdown();
		try {
			pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
			out.println("Condition,BP,BR,BF,BH,BFA,BAP,BBDP,ToP,ToR,ToF,TyP,TyR,TyF,LP,LR,LF");
			for (String line : outLines) {
				out.println(line);
			}
		}
		catch (InterruptedException e) {
			System.err.println("Execution interrupted!");
		}
		finally {
			out.close();
		}
		long endTime = System.currentTimeMillis() - startTime;
		System.out.println("Experiments took " + endTime / 1000F + " seconds.");
	}

}
