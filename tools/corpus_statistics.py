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

import re
import sys
import csv
from operator import itemgetter
from collections import defaultdict

PRI_STRESS_MARKER = "1"
WORD_SEP = " "
SYLL_SEP = "|"
PHON_SEP = "."


class CorpusStatistics:
    """Track basic statistics regarding a segmentation corpus."""

    def __init__(self, buffersize):
        # Simple counts
        self.word_counts = defaultdict(int)  # Count of words, including stress
        self.raw_syllable_counts = defaultdict(int)  # Count of syllables, including stress
        self.clean_syllable_counts = defaultdict(int)  # Count of syllables without stress
        self.num_tokens = 0
        self.num_sylls = 0
        self.num_utterances = 0
        self.pri_stresses = defaultdict(int)
        self.any_stresses = defaultdict(int)
        self.adjacent_stresses = 0
        self.adjacent_sylls = 0

        self.word_isolation_count = defaultdict(int)
        self.word_initial_count = defaultdict(int)
        self.word_final_count = defaultdict(int)
        self.monosyll_utt_count = 0

        self.syll_transitions = defaultdict(lambda: defaultdict(int))

        # Track lexicon turnover statistics
        self.utt_new_word_counts = []
        self.buffer = {}
        self.buffersize = buffersize

    def load_corpus(self, corpus_path):
        """Load a corpus and process it."""
        with open(corpus_path, "Ur") as corpus_file:
            for line in corpus_file:
                line = line.strip()
                self.num_utterances += 1

                all_sylls = []
                n_new_words = 0
                n_buffer_turnover = 0
                words = line.split(WORD_SEP)
                for idx, word in enumerate(words):
                    # Explicitly check whether a word is in so we know
                    # when to count new words
                    if word not in self.word_counts:
                        n_new_words += 1

                    self.word_counts[word] += 1
                    self.num_tokens += 1

                    # Update the rolling buffer, changing behavior based on whether it's full
                    self.buffer[word] = self.num_utterances
                    # Do any turnover required in the buffer
                    if len(self.buffer) > self.buffersize:
                        # Evict oldest
                        oldest, _ = sort_dict_vals(self.buffer)[0]
                        del self.buffer[oldest]
                        n_buffer_turnover += 1

                    # Mark first, last, and isolation words
                    if len(words) == 1:
                        self.word_isolation_count[word] += 1
                    elif idx == 0:
                        self.word_initial_count[word] += 1
                    elif idx == len(words) - 1:
                        self.word_final_count[word] += 1

                    # Count syllables
                    sylls = word.split(SYLL_SEP)
                    for syll in sylls:
                        # Count the syllable itself
                        self.raw_syllable_counts[syll] += 1
                        self.clean_syllable_counts[clean_syllable(syll)] += 1
                        self.num_sylls += 1
                        all_sylls.append(syll)

                # Loop over the syllables to compute transitional probabilities and stress
                # relationships
                last_stressed = None
                for idx, syll in enumerate(all_sylls):
                    # Count adjacent stresses and the total syllable adjacencies
                    stressed = has_pri_stress(syll)
                    # Only count adjacent syllables after last_stressed has been explicitly set
                    if last_stressed is not None:
                        if stressed and last_stressed:
                            self.adjacent_stresses += 1
                        self.adjacent_sylls += 1
                    last_stressed = stressed

                    # Skip first for TPs
                    if idx != 1:
                        syll1, syll2 = (clean_syllable(all_sylls[idx - 1]),
                                        clean_syllable(all_sylls[idx]))
                        self.syll_transitions[syll1][syll2] += 1

                # Check utterance syllable number
                if len(all_sylls) == 1:
                    self.monosyll_utt_count += 1

                # Count the new words
                self.utt_new_word_counts.append((self.num_utterances, n_new_words,
                                                 n_buffer_turnover, len(self.word_counts),
                                                 self.num_tokens))

        # Normalize the transitional probabilities
        for (context, outcomes) in self.syll_transitions.items():
            total_outcomes = sum(outcomes.values())
            for outcome, count in outcomes.items():
                self.syll_transitions[context][outcome] = float(count) / total_outcomes

        # Do a pass over types for stress information
        for word in self.word_counts:
            sylls = word.split(SYLL_SEP)

            # Skip single syllables
            if len(sylls) == 1:
                continue

            # Count stressed syllables and unstressed syllables
            syll_stresses = [has_pri_stress(syll) for syll in sylls]
            for idx, stressed in enumerate(syll_stresses):
                if stressed:
                    self.pri_stresses[idx] += 1
                self.any_stresses[idx] += 1

    def dump_corpus(self):
        """Dump statistics about the corpus."""
        print "Utterances:", self.num_utterances
        print "Words/utterance:",  self.num_tokens / float(self.num_utterances)
        print "Sylls/utterance:", self.num_sylls / float(self.num_utterances)
        print "Sylls/word:", self.num_sylls / float(self.num_tokens)
        print "Tokens, Types:", self.num_tokens, len(self.word_counts)
        print "Tokens/type", self.num_tokens / float(len(self.word_counts))
        print "Syllables:", self.num_sylls
        print "Unique syllables:", len(self.clean_syllable_counts)
        print "Sylls/unique syll:", self.num_sylls / float(len(self.clean_syllable_counts))
        print "Tokens, types of words in isolation:", \
            sum(self.word_isolation_count.values()), len(self.word_isolation_count)
        print "Percent of types seen in isolation:", \
            len(self.word_isolation_count) / float(len(self.word_counts))
        print "Percent monosyllabic utterances:", \
            self.monosyll_utt_count / float(self.num_utterances)
        print "Percent one-word utterances:", \
            sum(self.word_isolation_count.values()) / float(self.num_utterances)

        print "Adjacent stress rate:", self.adjacent_stresses / float(self.adjacent_sylls)
        print "Primary stress rate per syllable position (multisyllabic words):"
        stress_numerators = [count for dummy, count in sorted(self.pri_stresses.items())]
        stress_denomerators = [count for dummy, count in sorted(self.any_stresses.items())]
        stress_rate = [count1 / float(count2) for count1, count2 in
                       zip(stress_numerators, stress_denomerators)]
        print stress_rate

    def output_features(self, corpus_path, output_base):
        """Output features for each word boundary."""
        out_writer = csv.writer(open(output_base + "_boundaries.csv", 'wb'))
        out_writer.writerow(("TP", "StressBefore", "StressAfter", "LeftFreq", "RightFreq",
                             "Boundary"))
        delims = set((SYLL_SEP, WORD_SEP))

        # Loop over the input again
        with open(corpus_path, "Ur") as corpus_file:
            for line in corpus_file:
                line = line.strip()

                splits = re.split(r"([" + SYLL_SEP + WORD_SEP + "])", line)
                # If there are no splits, move on
                if not splits:
                    continue

                # Extract information
                boundaries = [WORD_SEP == delim for delim in splits if delim in delims]
                sylls = [syll for syll in splits if syll not in delims]
                clean_sylls = [clean_syllable(syll) for syll in sylls]
                stresses = [has_pri_stress(syll) for syll in sylls]

                for idx, boundary in enumerate(boundaries):
                    prev_syll = clean_sylls[idx]
                    next_syll = clean_sylls[idx + 1]
                    prev_stress = "TRUE" if stresses[idx] else "FALSE"
                    next_stress = "TRUE" if stresses[idx + 1] else "FALSE"
                    boundary = "TRUE" if boundary else "FALSE"
                    trans_prob = self.syll_transitions[prev_syll][next_syll]
                    prev_count = self.clean_syllable_counts[prev_syll]
                    next_count = self.clean_syllable_counts[next_syll]
                    out_writer.writerow((trans_prob, prev_stress, next_stress, prev_count,
                                         next_count, boundary))

    def output_lex_growth(self, output_base):
        """Output lexicon growth per utterance."""
        out_file = open(output_base + "_lexgrowth.csv", 'wb')
        out_writer = csv.writer(out_file)
        out_writer.writerow(("utt", "n.newwords", "n.buffturnover", "n.types", "n.tokens"))

        for row in self.utt_new_word_counts:
            out_writer.writerow(row)

        out_file.close()

    def output_lex(self, output_base):
        """Output lexicon."""
        out_file = open(output_base + "_lex.csv", 'wb')
        out_writer = csv.writer(out_file)
        out_writer.writerow(("rank", "word", "n.tokens", "n.isol", "n.first", "n.last"))

        # Sort lexicon by token count
        sorted_word_counts = sort_dict_vals_reverse(self.word_counts)

        rank = 0
        for word, count in sorted_word_counts:
            rank += 1
            out_writer.writerow((rank, word, count, self.word_isolation_count[word],
                                 self.word_initial_count[word], self.word_final_count[word]))

        out_file.close()


def sort_dict_vals_reverse(adict):
    """Sort a dictionary by descending values."""
    return sorted(adict.items(), key=itemgetter(1), reverse=True)


def sort_dict_vals(adict):
    """Sort a dictionary by ascending values."""
    return sorted(adict.items(), key=itemgetter(1))


def print_tuples(tups, name):
    """Print tuples neatly."""
    print "%s:" % name
    for key, val in tups:
        print key, val


def clean_syllable(syll):
    """Remove stress markings from a syllable."""
    return re.sub(r"\(*\d\)*", "", syll)


def has_pri_stress(syll):
    """Return whether a syllable has primary stress."""
    return PRI_STRESS_MARKER in syll


def main():
    """Process a file and dump its statistics."""
    try:
        corpus_path = sys.argv[1]
        if len(sys.argv) > 2:
            output_base = sys.argv[2]
            buffer_size = int(sys.argv[3])
        else:
            output_base = None
            buffer_size = sys.maxint
    except (IndexError, ValueError):
        print >> sys.stderr, "Usage: corpus_statistics file [output_base buffer_size]"
        sys.exit(2)

    corpstats = CorpusStatistics(buffer_size)
    corpstats.load_corpus(corpus_path)
    corpstats.dump_corpus()
    if output_base:
        corpstats.output_lex_growth(output_base)
        corpstats.output_lex(output_base)


if __name__ == "__main__":
    main()
