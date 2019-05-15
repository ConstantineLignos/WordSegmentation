# Segmentation tools

These are incidental tools that were developed for a mix of projects
over many years. None of the tools are supported, and use of them is
discouraged.

However, if that didn't stop you, here's some usage examples below.

## convert_corpus_phonemic

This script can be used to prepare input to the CATS segmenter.

Examples:

```
# English, using CMUDict
python convert_corpus_phonemic.py ../data/cmudict.0.7a_ext_reduced eng < input.txt > output.txt

# Spanish, using the CALLHOME Spanish lexicon (which you must get on
your own.
python convert_corpus_phonemic.py sp_lex.v04 esp < input.txt > output.txt
```
