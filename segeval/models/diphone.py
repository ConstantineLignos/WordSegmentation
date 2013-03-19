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
from collections import defaultdict, Counter

from segeval.util import counter_freqs


class DiphoneSegmenter(object):
    """A supervised diphone segmenter."""

    def __init__(self):
        # Unigram phoneme and diphone
        self.phoneme_counts = None
        self.phoneme_freqs = None
        self.diphone_freqs = None
        self.p_boundary = None

        # P(xy|#)
        self.boundary_diphone_probs = None
        # P(#|xy)
        self.diphone_boundary_probs = None

        # Phrase-end distributions
        self.initial_freqs = None
        self.final_freqs = None

    def train(self, corpus):
        """Train on segmented utterances."""
        # Get phoneme and boundary information
        self.phoneme_counts = corpus.phoneme_counts
        self.phoneme_freqs = counter_freqs(self.phoneme_counts)

        # Get diphone information
        self.diphone_freqs = counter_freqs(corpus.diphone_counts)

        ## Compute P(xy|#) and P(#|xy)
        # For P(#|xy), make a list of 1 for True, 0 for False to make
        # summing easier
        diphone_outcomes = defaultdict(list)
        # For P(xy|#), track all diphones with each label
        boundary_diphone_counts = {True: Counter(), False: Counter()}
        # Count it all up
        total_boundaries = 0
        total_diphones = 0
        for diphone, label in chain.from_iterable(corpus.diphone_boundaries):
            boundary_diphone_counts[label][diphone] += 1
            label = int(label)
            diphone_outcomes[diphone].append(label)
            total_diphones += 1
            total_boundaries += label

        # P(xy|#)
        self.boundary_diphone_probs = \
            {label: counter_freqs(counts) for label, counts in boundary_diphone_counts.items()}
        assert all(0 <= prob <= 1.0 for diphone_probs in self.boundary_diphone_probs.values()
                   for prob in diphone_probs.values())
        # If this assertion fails but the value is very close to 1.0, this just means
        # there is a lot of accumulated rounding error.
        assert all(.999 < sum(diphone_probs.values()) < 1.001
                   for diphone_probs in self.boundary_diphone_probs.values())

        # P(#|xy)
        self.diphone_boundary_probs = \
            {diphone: (sum(outcomes) / len(outcomes))
             for diphone, outcomes in diphone_outcomes.iteritems()}
        assert all(0 <= prob <= 1.0 for prob in self.diphone_boundary_probs.values())

        # P(#)
        self.p_boundary = total_boundaries / total_diphones
        assert 0 <= self.p_boundary <= 1.0

        ## Get phrase initial/final counts
        initial_counts, final_counts = corpus.outside_phoneme_counts
        # P(x|initial)
        self.initial_freqs = counter_freqs(initial_counts)
        # P(x|final)
        self.final_freqs = counter_freqs(final_counts)

    def classify_diphone(self, diphone):
        """Classify a diphone as a word boundary or not."""
        raise NotImplementedError("classify_diphone must be implemented by a derived class")
