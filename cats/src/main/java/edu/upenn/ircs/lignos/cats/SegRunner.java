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

import java.util.LinkedList;
import java.util.List;

import edu.upenn.ircs.lignos.cats.lexicon.Lexicon;
import edu.upenn.ircs.lignos.cats.metrics.Result;

public class SegRunner {
	private List<Utterance> goldUtterances;
	private String propsPath;	

	public SegRunner(List<Utterance> goldUtterances, String propsPath) {
		this.goldUtterances = goldUtterances;
		this.propsPath = propsPath;
	}

	public Result[] runSegmenter(String outPath) {
		Segment seg = new Segment(outPath);
		boolean dropStress = seg.DROP_STRESS;
		if (!seg.setParams(propsPath)) {
			System.err.println("The properties file " + propsPath + " could not be read.");
			System.exit(1);
		}

		// TODO: This should happen in the caller
		// Create the gold lexicon
		Lexicon goldLexicon = Lexicon.lexiconFromUtterances(goldUtterances,
				seg.STRESS_SENSITIVE_LOOKUP);

		// Copy utterances into segUtterances
		List<Utterance> segUtterances = new LinkedList<Utterance>();
		for (Utterance utt : goldUtterances) {
			Utterance segUtt = new Utterance(utt, false);
			if (dropStress) {
				segUtt.reduceStresses();
			}
			segUtterances.add(segUtt);
		}

		Lexicon segLexicon = seg.segment(segUtterances);
		// Output eval
		Result[] evalResults = seg.eval(goldUtterances, segUtterances, goldLexicon, segLexicon);
		seg.writeOutput(segUtterances, segLexicon);

		return evalResults;
	}

}
