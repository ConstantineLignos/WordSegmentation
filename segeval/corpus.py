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


class Corpus(object):
    """Corpus of utterances."""

    def __init__(self, path):
        """Load a corpus from the given path.

        Assumes that words are separated by spaces and each line is an
        utterance.

        """
        with open(path, 'rU') as infile:
            self._utterances = [_parse_line(line) for line in infile]

    @property
    def seg_utterances(self):
        """Segmented utterances, each of form [word1, word2]."""
        return self._utterances

    @property
    def unseg_utterances(self):
        """Unsegmented utterances, each a string of units."""
        return (''.join(words) for words in self._utterances)


def _parse_line(line):
    """Parse a line into words."""
    return " ".split(line.rstrip())
