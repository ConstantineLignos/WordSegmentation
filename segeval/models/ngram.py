"""
A simple N-gram model class.

"""

# Copyright (C) 2012 Constantine Lignos
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

from operator import itemgetter

from nltk import ConditionalFreqDist


class NgramModel(object):
    """A simple N-gram model."""
    def __init__(self, n, training_data):
        """Create an n order model using training_data."""
        # Set n and train
        self._n = n
        train_ngrams = _make_ngram_tuples(training_data, self._n)
        self._cfd = ConditionalFreqDist((context, event) for (context, event) in train_ngrams)
        self._estimators = dict((context, self._cfd[context])
                                for context in self._cfd.conditions())

    def prob(self, event, context):
        """Return the probability for an event in the provided context"""
        context = tuple(context)
        try:
            return self._estimators[context].freq(event)
        except KeyError:
            return 0.0

    def seqprob(self, seq):
        """Return the probability of a sequence."""
        prob = 1.0
        for context, event in _make_ngram_tuples(seq, self._n):
            prob *= self.prob(event, context)
        return prob

    def allngrams(self):
        """Return all N-grams observed by the model and their probabilities."""
        ngram_probs = ((event, context, self.prob(event, context))
                        for context, dist in self._estimators.items()
                        for event in dist)
        return sorted(ngram_probs, key=itemgetter(1))


def _make_ngram_tuples(samples, n):
    """Creates context tuples of the given samples of the form (context, event)."""
    if n == 1:
        # In the unigram case, the context is always None
        return [(None, sample) for sample in samples]
    else:
        # Otherwise, create a rolling context window and build up a
        # list of the tuples
        # TODO: Rewrite as a list comp
        ngrams = []
        # Initialize context with the context of the first event
        context = samples[:n-1]
        # The first samples have no context so we slice them out
        for sample in samples[n-1:]:
            # Record the context and event
            ngrams.append((tuple(context), sample))
            # Shift the context window
            context.pop(0)
            context.append(sample)

        return ngrams
