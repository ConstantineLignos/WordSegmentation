package edu.upenn.ircs.lignos.cats.segmenters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

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

				// Segment the left side of the word if it's not the start of the utterance.
				if (baseIndex != 0) {
					segmentation[baseIndex - 1] = true;
				}
				// Segment the right side of the word if it's not the end of the utterance.
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
				// word and the previous word. Note that just hitting the end of the while loop will
				// not reward the final word; this is as specified by GY.
				else {
					// Two reasons not to reward the previous word:
					// 1. If the baseIndex is zero, there's no previous word to reward.
					// 2. If the base index is unchanged since the last segmentation, the previous
					//    word was already rewarded when it was segmented.
					if (baseIndex != 0 && baseIndex != lastSegBaseIndex) {
						if (training) {
							lexicon.rewardWord((String[]) SegUtil.sliceFromLastBoundary(units, segmentation),
									(Boolean[]) SegUtil.sliceFromLastBoundary(stresses, segmentation));
						}
					}
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
				// Reward this word
				if (training) {
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
