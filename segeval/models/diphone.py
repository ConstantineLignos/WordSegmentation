"""
A simple supervised diphone segmenter.

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

from segeval.pyclassifiers.stump import Stump


class DiphoneSegmenter(object):
    """A supervised diphone segmenter."""

    def __init__(self):
        # Learned decision stump threshold
        self.threshold = None

    def train(self, corpus):
        """Train on segmented utterances."""
        # TODO: Implement

    @staticmethod
    def _corpus_diphone_labels(corpus):
        """Return the diphones from a corpus as features and outcome."""
        # TODO: Implement
        pass

    @staticmethod
    def _utterance_diphone_labels(utt, boundaries):
        """Return the diphones contained in an utterance."""
        # TODO: Implement
        pass
