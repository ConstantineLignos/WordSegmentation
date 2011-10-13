"""Compute statistics relevant for word segmenation corpora."""

# Copyright (C) 2010, 2011 Constantine Lignos
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

from collections import defaultdict
import re
import sys

PRI_STRESS_MARKER = "1"
WORD_SEP = " "
SYLL_SEP = "|"
PHON_SEP = "."


class CorpusStatistics:
    """Track basic statistics regarding a segmentation corpus."""
    
    def __init__(self):
        # Simple counts
        self.word_counts = defaultdict(int) # Count of words, including stress
        self.raw_syllable_counts = defaultdict(int) # Count of syllables, including stress
        self.clean_syllable_counts = defaultdict(int) # Count of syllables without stress
        self.num_tokens = 0
        self.num_sylls = 0
        self.num_utterances = 0
        self.pri_stresses = defaultdict(int)
        self.any_stresses = defaultdict(int)
        
        self.word_isolation_count = defaultdict(int)
        self.word_initial_count = defaultdict(int)
        self.word_final_count =  defaultdict(int)
        
    def load_corpus(self, corpus_path):
        """Load a corpus and process it."""
        with open(corpus_path, "Ur") as corpus_file:
            for line in corpus_file:
                line = line.strip()
                self.num_utterances += 1
                
                words = line.split(WORD_SEP)
                for idx, word in enumerate(words):
                    self.word_counts[word] += 1
                    self.num_tokens += 1
                    
                    # Mark first, last, and isolation words
                    if idx == 0:
                        self.word_initial_count[word] += 1
                    if idx == len(words) - 1:
                        self.word_final_count[word] += 1
                    if len(words) == 1:
                        self.word_isolation_count[word] += 1
                    
                    for syll in word.split(SYLL_SEP):
                        self.raw_syllable_counts[syll] += 1
                        self.clean_syllable_counts[clean_syllable(syll)] += 1
                        self.num_sylls += 1
                        
        # Do a pass over types for stress information
        for word in self.word_counts:
            sylls = word.split(SYLL_SEP)
            for idx, syll in enumerate(sylls):
                # Add to the stress vector if it's multi-syllabic
                if len(sylls) > 1:
                    if pri_stress(syll):
                        self.pri_stresses[idx] += 1
                    self.any_stresses[idx] += 1
                
    def dump_corpus(self):
        """Dump statistics about the corpus."""
        print "Utterances:", self.num_utterances
        print "Words/utterance:",  self.num_tokens / float(self.num_utterances)
        print "Sylls/utterance:", self.num_sylls / float(self.num_utterances)
        print "Sylls/word:", self.num_sylls / float(self.num_tokens)
        print "Tokens:", self.num_tokens
        print "Types:", len(self.word_counts)
        print "Tokens/type", self.num_tokens / float(len(self.word_counts))
        print "Syllables:", self.num_sylls
        print "Unique syllables:", len(self.clean_syllable_counts)
        print "Sylls/unique syll:", self.num_sylls / float(len(self.clean_syllable_counts))
        
        print "Syllable patterns:"
        stress_numerators = [count for unused, count in sorted(self.pri_stresses.items())]
        stress_denomerators = [count for unused, count in sorted(self.any_stresses.items())]
        stress_rate = [count1 / float(count2) for count1, count2 in 
                       zip(stress_numerators, stress_denomerators)]
        print stress_rate
                

def sort_dict(adict):
    """Sort a dictionary by descending values."""
    return sorted(adict.items(), key=lambda x: x[1], reverse=True)


def print_tuples(tups, name):
    """Print tuples neatly."""
    print "%s:" % name
    for key, val in tups:
        print key, val


def clean_syllable(syll):
    """Remove stress markings from a syllable."""
    return re.sub(r"\d", "", syll)


def pri_stress(syll):
    """Return whether a syllable has primary stress."""
    return PRI_STRESS_MARKER in syll


def main():
    """Process a file and dump its statistics."""
    try:
        corpus_path = sys.argv[1]
    except IndexError:
        print >> sys.stderr, "Usage: corpus_statistics file"
        sys.exit(2)
        
    corpstats = CorpusStatistics()
    corpstats.load_corpus(corpus_path)
    corpstats.dump_corpus()


if __name__ == "__main__":
    main()


    