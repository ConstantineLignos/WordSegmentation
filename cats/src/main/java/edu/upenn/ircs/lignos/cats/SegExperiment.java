package edu.upenn.ircs.lignos.cats;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.List;
import java.util.Properties;
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
		List<Utterance> goldUtterances;
		Lexicon goldLexicon;
		Properties props;
		String outPath;
		String name;

		public SegmenterParams(List<Utterance> goldUtterances, Lexicon goldLexicon,
				Properties props, String outPath, String name) {
			this.goldUtterances = goldUtterances;
			this.goldLexicon = goldLexicon;
			this.props = props;
			this.outPath = outPath;
			this.name = name;
		}
	}

	public void run() {
		System.out.println("Starting run for " + params.name + "...");
		Result[] segResults = Segment.runSegmenter(params.goldUtterances, params.goldLexicon,
				params.props, params.outPath);
		// Unpack results
		Result boundaryResult = segResults[0];
		Result wordResult = segResults[1];
		Result lexResult = segResults[2];
		// Format output
		results[resultsIndex] = (params.name + "," + 
				String.format("%1.4f,", boundaryResult.precision) + 
				String.format("%1.4f,", boundaryResult.recall) +
				String.format("%1.4f,", boundaryResult.fScore) +	
				String.format("%1.4f,", boundaryResult.hitRate) + 
				String.format("%1.4f,", boundaryResult.faRate) + 
				String.format("%1.4f,", boundaryResult.aPrime) + 
				String.format("%1.4f,", wordResult.precision) + 
				String.format("%1.4f,", wordResult.recall) + 
				String.format("%1.4f,", wordResult.fScore) + 
				String.format("%1.4f,", lexResult.precision) + 
				String.format("%1.4f,", lexResult.recall) + 
				String.format("%1.4f", lexResult.fScore));
		System.out.println("Completed run for " + params.name + ".");
	}

	/**
	 * @param args Command line arguments
	 */
	public static void main(String[] args) {
		// TODO: Read these in from a file
		String[] propsFiles = {"props/utterance.props", "props/random.props", 
				"props/random_oracle.props", "props/unit.props", "props/trough.props",
				"props/notrust.props", "props/nobeam.props",  "props/default.props",
				"props/reduced.props", "props/stress.props", "props/notrust_probmem.props",
				"props/nobeam_probmem.props", "props/default_probmem.props",
				"props/reduced_probmem.props", "props/stress_probmem.props"};

		if (args.length != 3) {
			System.err.println("Usage: SegExperiment input output_base csv_output");
			System.exit(2);
		}

		// Extract arguments
		String inputPath = args[0];
		String outBase = args[1];
		String outPath = args[2];

		// Read in the input
		List<Utterance> goldUtterances = Utterance.loadUtterances(inputPath);
		if (goldUtterances == null) {
			System.err.println("The input file " + inputPath + " could not be read.");
			System.exit(1);
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
		out.println("Condition,BP,BR,BF,BH,BFA,BAP,WP,WR,WF,LP,LR,LF");

		// Set up place to store the results of each line
		String[] outLines = new String[propsFiles.length];
		System.out.println("Number of experiments to run: " + propsFiles.length);

		// Get the number of cores and start a thread pool
		int cores = Runtime.getRuntime().availableProcessors();
		System.out.println("Number of threads: " + cores);
		ExecutorService pool = Executors.newFixedThreadPool(cores);
		long startTime = System.currentTimeMillis();

		// Index to store each result
		int idx = 0;
		for (String propsPath : propsFiles) {
			Properties props = Utils.loadProps(propsPath);

			// Because properties can vary in stress sensitive lookup, we have to make the lexicon
			// for each props
			boolean stress_sensitive_lookup = new Boolean(props.getProperty(Segment.STRESS_SENSITIVE_PROP));
			Lexicon goldLexicon = Lexicon.lexiconFromUtterances(goldUtterances,
					stress_sensitive_lookup);

			// Clean up name
			String name = new File(propsPath).getName();
			if (name.contains(".")) {
				name = name.substring(0, name.lastIndexOf('.'));
			}
			SegmenterParams params = new SegmenterParams(goldUtterances, goldLexicon, props,
					outBase, name);
			pool.submit(new SegExperiment(params, outLines, idx++));
		}

		// Shut down the pool and wait (forever) until everything is done
		pool.shutdown();
		try {
			pool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
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
