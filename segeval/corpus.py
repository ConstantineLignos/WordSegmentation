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

    @property
    def seg_utterances(self):
        """Segmented utterances, each of form [word1, word2]."""
        return self._utterances

    @property
    def unseg_utterances(self):
        """Unsegmented utterances, each a string of units."""
        return (list(chain(*utt)) for utt in self._utterances)

    @staticmethod
    def _parse_line(line):
        """Return a list of words in the line, each word a list of units.

        >>> Corpus._parse_line('yu want tu si D6 bUk\\n')
        [['y', 'u'], ['w', 'a', 'n', 't'], ['t', 'u'], ['s', 'i'], ['D', '6'], ['b', 'U', 'k']]
        >>> Corpus._parse_line('lUk\\n')
        [['l', 'U', 'k']]

        """
        return [[char for char in word] for word in line.rstrip().split(" ")]

    @staticmethod
    def _parse_boundaries(utt):
        """Return which transitions are boundaries.

        >>> Corpus._parse_boundaries([['h', '&', 'v'], ['6'], ['d', 'r', 'I', 'N', 'k']])
        [False, False, True, True, False, False, False, False]
        >>> Corpus._parse_boundaries([['y', 'E', 's']])
        [False, False]
        >>> Corpus._parse_boundaries([['6'], ['6'], ['6']])
        [True, True]
        >>> Corpus._parse_boundaries([['6']])
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


if __name__ == "__main__":
    import doctest
    doctest.testmod()
