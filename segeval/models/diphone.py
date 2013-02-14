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

from itertools import chain

from segeval.util import counter_freqs
from segeval.pyclassifiers.stump import Stump


class DiphoneSegmenter(object):
    """A supervised diphone segmenter."""

    def __init__(self):
        # Learned decision stump threshold
        self.threshold = None

    def train(self, corpus):
        """Train on segmented utterances."""
        # Get the boundaries and lables
        diphone_boundaries = chain.from_iterable(corpus.diphone_boundaries)
        diphones, diphone_labels = zip(*diphone_boundaries)

        # Convert counts to probabilities
        diphone_freq = counter_freqs(corpus.diphone_counts)

        # Pack the diphone features in the format for the classifier
        diphone_features = [[diphone_freq[diphone]] for diphone in diphones]

        # Train!
        stump = Stump(diphone_features, diphone_labels, False)
        print "Learned threshold:"
        print stump

        print "Training performance:"
        stump.evaluate(diphone_features, diphone_labels)
        print "Accuracy:", stump.accuracy()
        print "Precision:", stump.precision()
        print "Recall:", stump.recall()
        print "F1:", stump.F1()
