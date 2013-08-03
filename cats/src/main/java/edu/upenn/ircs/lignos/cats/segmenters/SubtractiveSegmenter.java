package edu.upenn.ircs.lignos.cats.segmenters;

import java.util.ArrayList;

import edu.upenn.ircs.lignos.cats.Utterance;
import edu.upenn.ircs.lignos.cats.lexicon.Lexicon;
import edu.upenn.ircs.lignos.cats.lexicon.Word;

public class SubtractiveSegmenter implements Segmenter {
	private Lexicon lexicon;
	private int nSubtractions = 0;

	public SubtractiveSegmenter(Lexicon lexicon) {
		this.lexicon = lexicon;
	}

	@Override
	public Boolean[] segment(Utterance utterance, boolean training, boolean trace) {
		Boolean[] segmentation = utterance.getBoundariesCopy();
		int baseIndex = 0;
		while (baseIndex < utterance.length) {
			ArrayList<Word> prefixes = lexicon.getPrefixWords(utterance, baseIndex);
			if (!prefixes.isEmpty()) {
				// Get the highest scoring word
				Word word = SegUtil.chooseBestScoreWord(prefixes, lexicon, null);
				nSubtractions++;

				// Segment the left side of the word if it's not the start of the utterance.
				if (baseIndex != 0) {
					segmentation[baseIndex - 1] = true;
				}
				// Segment the right side of the word if it's not the end of the utterance.
				int finalBound = baseIndex + word.length - 1;
				if (finalBound != segmentation.length) {
					segmentation[finalBound] = true;
				}

				// Move baseIndex by word length
				baseIndex += word.length;
			}
			else {
				baseIndex++;
			}
		}
		// Increment the words used in the utterance.
		if (training) {
			lexicon.incUtteranceWords(utterance.getUnits(), utterance.getStresses(), segmentation, 
					null);
		}

		return segmentation;
	}

	@Override
	public String getStats() {
		return "Subtractive segs: " + nSubtractions;
	}
}
