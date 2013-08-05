package edu.upenn.ircs.lignos.cats.segmenters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import edu.upenn.ircs.lignos.cats.Utterance;
import edu.upenn.ircs.lignos.cats.lexicon.Lexicon;
import edu.upenn.ircs.lignos.cats.lexicon.Word;

public class GambellYangSegmenter implements Segmenter {
	private Lexicon lexicon;
	private boolean useStress;
	private int subtractiveSegs = 0;
	private int uscSegs = 0;

	public GambellYangSegmenter(Lexicon lexicon, boolean useStress) {
		this.lexicon = lexicon;
		this.useStress = useStress;
	}

	@Override
	public Boolean[] segment(Utterance utterance, boolean training, boolean trace) {
		String[] units = utterance.getUnits();
		Boolean[] segmentation = utterance.getBoundariesCopy();
		Boolean[] stresses = utterance.getStresses();
		int baseIndex = 0;
		int lastSegBaseIndex = 0;
		while (baseIndex < utterance.length) {
			ArrayList<Word> prefixes = lexicon.getPrefixWords(utterance, baseIndex);
			// Subtract a word from the start if there is one
			if (!prefixes.isEmpty()) {
				// Get the highest scoring word
				Word word = SegUtil.chooseBestScoreWord(prefixes, lexicon, null);
				subtractiveSegs++;

				// Segment the left side of the word if it's not the start of the utterance. Also
				// reward the previous word if it's valid.
				if (baseIndex != 0) {
					segmentation[baseIndex - 1] = true;

					// When training, we can reward the previous word under two conditions:
					// 1. The base index has changed since the last segmentation. If it hasn't,
					//    the previous word was already rewarded when it was segmented.
					// 3. The previous word has 1 or fewer primary stresses, if we're using stress.
					int nPrevWordStresses = Collections.frequency(Arrays.asList(
							(Boolean[]) SegUtil.sliceFromLastBoundary(stresses, segmentation)), true);
					if (training && baseIndex != lastSegBaseIndex &&
							(!useStress || nPrevWordStresses <= 1)) {
						lexicon.rewardWord((String[]) SegUtil.sliceFromLastBoundary(units, segmentation),
								(Boolean[]) SegUtil.sliceFromLastBoundary(stresses, segmentation));
					}
				}

				// Segment the right side of the word if it's not the end of the utterance. Reward
				// the resulting word.
				int finalBound = baseIndex + word.length - 1;
				if (finalBound != segmentation.length) {
					segmentation[finalBound] = true;
					// Reward this word
					if (training) {
						lexicon.rewardWord((String[]) SegUtil.sliceFromLastBoundary(units, segmentation),
								(Boolean[]) SegUtil.sliceFromLastBoundary(stresses, segmentation));
					}
				}
				// If we did segment to the end of the utterance, make sure to reward the final
				// word. Note that just hitting the end of the while loop will not reward the final
				// word; this is as specified by GY.
				else {
					if (training) {
						lexicon.rewardWord((String[]) SegUtil.sliceFromFinalBoundary(units, segmentation),
								(Boolean[]) SegUtil.sliceFromFinalBoundary(stresses, segmentation));
					}
				}

				// Move baseIndex by word length
				baseIndex += word.length;
				lastSegBaseIndex = baseIndex;
			}
			// Insert a USC segmentation if there are adjacent stresses
			else if (useStress && baseIndex < stresses.length - 1 && stresses[baseIndex] &&
					stresses[baseIndex + 1]) {
				segmentation[baseIndex] = true;
				// Reward this word if it has one or fewer primary stresses
				int nWordStresses = Collections.frequency(Arrays.asList(
						(Boolean[]) SegUtil.sliceFromLastBoundary(stresses, segmentation)), true);
				if (training && nWordStresses <= 1) {
					lexicon.rewardWord((String[]) SegUtil.sliceFromLastBoundary(units, segmentation),
							(Boolean[]) SegUtil.sliceFromLastBoundary(stresses, segmentation));
				}
				uscSegs++;
				baseIndex++;
				lastSegBaseIndex = baseIndex;
			}
			else {
				baseIndex++;
			}
		}

		// If we made no segmentations at all, add the while utterance to the lexicon if there is
		// one or fewer primary stresses in it or if we're not using stress.
		if (lastSegBaseIndex == 0) {
			if (training && (!useStress || Collections.frequency(Arrays.asList(stresses), true) <= 1)) {
				lexicon.rewardWord(units, stresses);
			}
		}

		return segmentation;
	}

	@Override
	public String getStats() {
		return "Subtractive segs: " + subtractiveSegs + "\n" + "USC segs: " + uscSegs;
	}
}
