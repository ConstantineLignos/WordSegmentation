# How to run an experiment

1. *Set up your data.* The format for the data is one utterance per
   line, with words separated by spaces. Each word is separated into
   syllables, delimited by `|`, and phonemes, delimited by the `.`
   character. If `1` appears in the syllable, the syllable treated as
   stressed. For example, the utterance "Play checkers" might be
   written as follows if transcribed using ARPABET:
   `P.L.EY1 CH.EH1|K.ER0.Z`
2. *Compile the code.* First, install Java and maven (`mvn`) on your
   system. From the `cats` directory, run: `mvn package`.
3. *Run the segmenter.* Use the `segment.sh` script to run the
   compiled code. For example, to use the sample data for training and
   testing, writing various diagnostic outputs using the prefix
   `output`, and using the default configuration, run:
   `./segment.sh ../data/sample_syll_train.txt ../data/sample_syll_test.txt output props/default.props`
4. Your output should look like the sample output below. I recommend
   paying most attention to the "Boundaries" metrics, specifically
   precision/recall/F1 and A' (which measures how well boundaries are
   discriminated from non-boundaries). The lexicon metric should never
   be compared against other models.


Sample output:
```
$ ./segment.sh ../data/sample_syll_train.txt ../data/sample_syll_test.txt output props/default.props
Loading utterances from ../data/sample_syll_train.txt ...
Loading training data took 0.523 seconds.
Loading utterances from ../data/sample_syll_test.txt ...
Loading testing data took 0.075 seconds.
Running segmenter BeamSubtractive
Initializing...
Segmenting...
USC Segs: 0
Sub. segs: 240146
Average highest beam: 1.0662167
Training took 0.438 seconds.
Initializing...
Segmenting...
USC Segs: 0
Sub. segs: 337673
Average highest beam: 1.0682398
Testing took 0.123 seconds.
Evaluating...
Learner's lexicon stress-initial rate: 0.6855727 (1245/1816 total)
Gold lexicon stress-initial rate: 0.8098976 (2373/2930 total)
Boundaries:
Precision: 0.8836, Recall: 0.9911, F-Score: 0.9343
Hit Rate: 0.9911, FA Rate: 0.5383, A': 0.8594, B'': -0.9846
Word tokens:
Precision: 0.8152, Recall: 0.8935, F-Score: 0.8525
Hit Rate: 0.8935, FA Rate: 0.6962
Word types:
Precision: 0.7339, Recall: 0.6505, F-Score: 0.6897
Lexicon:
Precision: 0.6478, Recall: 0.4515, F-Score: 0.5321
Run took 2.585 seconds.
```


# FAQ

* Can I run on a single file or do I need separate train and test?
  You can test on the training file (which is standard for many
  unsupervised word segmentation studies) by specifying `none` for the
  test file. For example:
  `./segment.sh ../data/sample_syll_train.txt none output props/default.props`
