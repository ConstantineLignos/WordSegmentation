package edu.upenn.ircs.lignos.cats;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import edu.upenn.ircs.lignos.cats.metrics.Result;

public class SegExperiment {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String[] propsFiles = {"random.props", "random_oracle.props", "unit.props", "utterance.props",
				"nostress_perfectmem_base.props", "nostress_perfectmem_trust.props", 
				"nostress_perfectmem_trust_beam2.props", "nostress_probmem_base.props", "nostress_probmem_trust.props", 
				"nostress_probmem_trust_beam2.props",};
		
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
		
		out.println("Condition,Precision,Recall,F-score,Hit Rate,FA Rate,A'");

		for (String propsFile : propsFiles) {
			System.out.println("****************************************");
			System.out.println(propsFile);
			String[] segArgs = {inPath, outBase, propsFile};
			Result result = Segment.callSegmenter(segArgs);
			out.println(propsFile + "," + result.precision + "," + result.recall + "," + result.fScore + "," +
				result.hitRate + "," + result.faRate + "," + result.aPrime);
			System.out.println();
		}
		
		out.close();

	}

}
