"""
Load data from corpora.

Constantine Lignos, February 2013

"""

# Copyright (C) 2013 Constantine Lignos
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

from itertools import chain
from collections import Counter


class Corpus(object):
    """Corpus of utterances."""

    def __init__(self, path):
        """Load a corpus from the given path.

        Assumes that words are separated by spaces and each line is an
        utterance.

        """
        with open(path, 'rU') as infile:
            self._utterances = [self._parse_line(line) for line in infile]
        self._boundaries = [self._parse_boundaries(utt) for utt in self._utterances]
        self._word_counts = None
        self._phoneme_counts = None
        self._diphone_counts = None
        self._diphone_boundaries = None

    @property
    def seg_utterances(self):
        """Segmented utterances, each of form [word1, word2]."""
        return self._utterances

    @property
    def unseg_utterances(self):
        """Unsegmented utterances, each a string of units."""
        return (list(chain(*utt)) for utt in self._utterances)

    @property
    def word_counts(self):
        """Counter of all word types in the corpus.

        >>> c = Corpus('data/test-corpus.txt')
        >>> sorted(c.word_counts.items()) # doctest: +NORMALIZE_WHITESPACE
        [(('&', 'n', 'd'), 1), (('6',), 2), (('W', 'A', 't'), 1),
         (('d', 'O', 'g', 'i'), 2), (('n', '9', 's'), 1)]

        """
        if not self._word_counts:
            self._word_counts = Counter(word for utt in self._utterances for word in utt)
        return self._word_counts

    @property
    def diphone_counts(self):
        """Counter of all diphones in the corpus.

        >>> c = Corpus('data/test-corpus.txt')
        >>> sorted(c.diphone_counts.items()) # doctest: +NORMALIZE_WHITESPACE
        [(('&', 'n'), 1), (('6', 'd'), 1), (('6', 'n'), 1), (('9', 's'), 1),
         (('A', 't'), 1), (('O', 'g'), 2), (('W', 'A'), 1), (('d', '6'), 1),
         (('d', 'O'), 2), (('g', 'i'), 2), (('n', '9'), 1), (('n', 'd'), 1),
         (('s', 'd'), 1), (('t', '6'), 1)]

        """
        if not self._diphone_counts:
            self._diphone_counts = \
                Counter(chain.from_iterable(self._extract_diphones(utt)
                                            for utt in self._utterances))
        return self._diphone_counts

    @property
    def phoneme_counts(self):
        """Counter of all phonemes in the corpus.

        >>> c = Corpus('data/test-corpus.txt')
        >>> sorted(c.phoneme_counts.items()) # doctest: +NORMALIZE_WHITESPACE
        [('&', 1), ('6', 2), ('9', 1), ('A', 1), ('O', 2), ('W', 1), ('d', 3),
        ('g', 2), ('i', 2), ('n', 2), ('s', 1), ('t', 1)]

        """
        if not self._phoneme_counts:
            self._phoneme_counts = \
                Counter(chain.from_iterable(chain(*utt) for utt in self._utterances))

        return self._phoneme_counts

    @property
    def diphone_boundaries(self):
        """Tuples of each diphone token and whether it is a boundary."""
        if not self._diphone_boundaries:
            self._diphone_boundaries = \
                [self._extract_diphone_boundaries(utt)
                 for utt in self._utterances]
        return self._diphone_boundaries

    @property
    def outside_phoneme_counts(self):
        """Two counters (initial, final) for phonemes at utterance edges."""
        initial = Counter()
        final = Counter()
        for utt in self._utterances:
            first, last = self._extract_outside_phonemes(utt)
            initial[first] += 1
            final[last] += 1
        
        return (initial, final)

    @staticmethod
    def _parse_line(line):
        """Return a list of words in the line, each word a list of units.

        >>> Corpus._parse_line('yu want tu si D6 bUk\\n')
        [('y', 'u'), ('w', 'a', 'n', 't'), ('t', 'u'), ('s', 'i'), ('D', '6'), ('b', 'U', 'k')]
        >>> Corpus._parse_line('lUk\\n')
        [('l', 'U', 'k')]

        """
        return [tuple(word) for word in line.rstrip().split(" ")]

    @staticmethod
    def _parse_boundaries(utt):
        """Return which transitions are boundaries.

        >>> Corpus._parse_boundaries([('h', '&', 'v'), ('6',), ('d', 'r', 'I', 'N', 'k')])
        [False, False, True, True, False, False, False, False]
        >>> Corpus._parse_boundaries([('y', 'E', 's')])
        [False, False]
        >>> Corpus._parse_boundaries([('6',), ('6',), ('6',)])
        [True, True]
        >>> Corpus._parse_boundaries([('6',)])
        []

        """
        boundaries = []
        first = True
        for word in utt:
            # Transition between words gets a boundary, but start of utterance does not
            if not first:
                boundaries.append(True)
            first = False

            # Add a false for each intra-word transition
            boundaries.extend([False] * (len(word) - 1))

        return boundaries

    @staticmethod
    def _extract_diphones(utt):
        """Return the diphones in a utterance, treating it as unsegmented.

        >>> Corpus._extract_diphones([('h', '&', 'v'), ('6',),
        ... ('d', 'r', 'I', 'N', 'k')]) # doctest: +NORMALIZE_WHITESPACE
        [('h', '&'), ('&', 'v'), ('v', '6'), ('6', 'd'), ('d', 'r'),
         ('r', 'I'), ('I', 'N'), ('N', 'k')]
        >>> Corpus._extract_diphones([('y', 'E', 's')])
        [('y', 'E'), ('E', 's')]
        >>> Corpus._extract_diphones([('6',), ('6',), ('6',)])
        [('6', '6'), ('6', '6')]
        >>> Corpus._extract_diphones([('6',)])
        []

        """
        phones = list(chain(*utt))
        return zip(phones[:-1], phones[1:])

    @staticmethod
    def _extract_diphone_boundaries(utt):
        """Return the diphones in an utterance and whether they were boundaries.

        >>> Corpus._extract_diphone_boundaries([('h', '&', 'v'),
        ... ('6',), ('d', 'r', 'I', 'N', 'k')]) # doctest: +NORMALIZE_WHITESPACE
        [(('h', '&'), False), (('&', 'v'), False), (('v', '6'), True), (('6', 'd'), True),
         (('d', 'r'), False), (('r', 'I'), False), (('I', 'N'), False), (('N', 'k'), False)]

        >>> Corpus._extract_diphone_boundaries([('y', 'E', 's')])
        [(('y', 'E'), False), (('E', 's'), False)]
        >>> Corpus._extract_diphone_boundaries([('6',), ('6',), ('6',)])
        [(('6', '6'), True), (('6', '6'), True)]
        >>> Corpus._extract_diphone_boundaries([('6',)])
        []

        """
        return zip(Corpus._extract_diphones(utt), Corpus._parse_boundaries(utt))

    @staticmethod
    def _extract_outside_phonemes(utt):
        """Return the first and last phonemes in an utterance.

        >>> Corpus._extract_outside_phonemes([('h', '&', 'v'),
        ... ('6',), ('d', 'r', 'I', 'N', 'k')]) # doctest: +NORMALIZE_WHITESPACE
        ('h', 'k')
        >>> Corpus._extract_outside_phonemes([('y', 'E', 's')])
        ('y', 's')
        >>> Corpus._extract_outside_phonemes([('6',), ('6',), ('6',)])
        ('6', '6')
        >>> Corpus._extract_outside_phonemes([('6',)])
        ('6', '6')

        """
        return (utt[0][0], utt[-1][-1])


if __name__ == "__main__":
    import doctest
    doctest.testmod()
