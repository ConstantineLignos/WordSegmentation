package edu.upenn.ircs.lignos.cats;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import edu.upenn.ircs.lignos.cats.metrics.Result;

public class SegExperiment implements Runnable  {

    private final String[] args;
	private final String[] results;
	private final int resultsIndex;

	/**
	 * @param args arguments to be given to callSegmenter
	 * @param results array to store results in
	 * @param resultsIndex index in results to use to store results
	 */
	SegExperiment(String[] args, String[] results, int resultsIndex) {
    	this.args = args;
    	this.results = results;
    	this.resultsIndex = resultsIndex;
    }
	
    public void run() {
    	String propsFile = args[2];
		System.out.println("Starting run for " + propsFile + "...");
    	Result[] segResults = Segment.callSegmenter(args);
    	// Unpack results
    	Result boundaryResult = segResults[0];
    	Result wordResult = segResults[1];
    	Result lexResult = segResults[2];
    	// Format output
    	results[resultsIndex] = (args[2] + "," + 
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
    	System.out.println("Completed run for " + propsFile + ".");
    }
	
	/**
	 * @param args Command line arguments
	 */
	public static void main(String[] args) {
		// TODO: Read these in from a file
		String[] propsFiles = {"utterance.props", "unit.props", "default.props", "reduced.props",
				"stress.props"};
		
		if (args.length != 3) {
			System.err.println("Usage: SegExperiment input output_base csv_output");
			System.exit(2);
		}
		
		String inPath = args[0];
		String outBase = args[1];
		String outPath = args[2];
		
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
		
		// Get the number of cores and start a thread pool
		int cores = Runtime.getRuntime().availableProcessors();
		System.out.println("Number of concurrent threads: " + cores);
		ExecutorService pool = Executors.newFixedThreadPool(cores);
		
		// Index to store each result
		int idx = 0;
		for (String propsFile : propsFiles) {
			String[] segArgs = {inPath, outBase, propsFile};
			pool.submit(new SegExperiment(segArgs, outLines, idx++));
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
	}

}
