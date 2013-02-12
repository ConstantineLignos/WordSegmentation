"""
Tools for performing evaluations.
Constantine Lignos, July 2012

"""

# Copyright (C) 2012 Constantine Lignos
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

from __future__ import division
from math import sqrt
from collections import defaultdict, Counter


class Accuracy(object):
    """Evaluates accuracy."""

    def __init__(self):
        self.hits = 0
        self.misses = 0
        self.confusion = defaultdict(lambda: defaultdict(list))
        self.classes = set()
        self.gold_counts = Counter()

    def score(self, pred, gold, data=None):
        """Score any non-None outcomes."""
        # We validate because of the risk of None/False kind of confusions
        assert pred is not None
        assert gold is not None
        # Score
        if pred == gold:
            self.hits += 1
        else:
            self.misses += 1
        # Track all classes seen and update confusion matrix
        self.gold_counts[gold] += 1
        self.classes.add(pred)
        self.classes.add(gold)
        self.confusion[gold][pred].append(data)

    @property
    def accuracy(self):
        """Accuracy"""
        try:
            return self.hits / (self.hits + self.misses)
        except ZeroDivisionError:
            return float('nan')

    @property
    def baseline_accuracy(self):
        """Baseline accuracy"""
        try:
            return self.gold_counts.most_common(1)[0][1] / sum(self.gold_counts.values())
        except (IndexError, ZeroDivisionError):
            return float('nan')

    @property
    def all_stats(self):
        """All relevant statistics."""
        return (self.accuracy,)

    def __str__(self):
        return "\n".join([
                "Gold standard of {0} items".format(len(self)),
                "Accuracy: {0:.3f}".format(self.accuracy),
                "Baseline accuracy: {0:.3f}".format(self.baseline_accuracy)])

    def __repr__(self):
        return "<Accuracy with %s observations>" % len(self)

    def __len__(self):
        return self.hits + self.misses

    def confusion_matrix(self):
        """Return a string representation of a confusion matrix."""
        classes = sorted(self.classes)
        rows = (["\t-Predicted labels-", "-Gold-\t" + "\t".join(classes)] +
                ["\t".join([rowname] + [str(len(self.confusion[rowname][colname])) for colname in classes])
                 for rowname in classes])
        return "\n".join(rows)


class SDMetrics(object):
    """Evaluates information retrieval/signal dection metrics."""
    def __init__(self):
        self._falsepos = []
        self._truepos = []
        self._falseneg = []
        self._trueneg = []

    def score(self, pred, gold, data=None):
        """Score a binary prediction."""
        assert pred in (True, False)
        assert gold in (True, False)
        if pred:
            if gold:
                self._truepos.append((pred, gold, data))
            else:
                self._falsepos.append((pred, gold, data))
        else:
            if gold:
                self._falseneg.append((pred, gold, data))
            else:
                self._trueneg.append((pred, gold, data))

    @property
    def fp(self):
        """False positives"""
        return len(self._falsepos)

    @property
    def tp(self):
        """True positives"""
        return len(self._truepos)

    @property
    def fn(self):
        """False negatives"""
        return len(self._falseneg)

    @property
    def tn(self):
        """True negatives"""
        return len(self._trueneg)

    @property
    def precision(self):
        """Precision"""
        try:
            return self.tp / (self.tp + self.fp)
        except ZeroDivisionError:
            return float('nan')

    @property
    def recall(self):
        """Recall"""
        try:
            return self.tp / (self.tp + self.fn)
        except ZeroDivisionError:
            return float('nan')

    @property
    def fscore(self):
        """F-score (F1)"""
        try:
            return 2 * (self.precision * self.recall) / (self.precision + self.recall)
        except ZeroDivisionError:
            return float('nan')

    @property
    def baseline_fscore(self):
        """Baseline fscore assuming always say positive"""
        try:
            return 2 * ((self.tp + self.fn) / len(self)) / ((self.tp + self.fn) / len(self) + 1.0)
        except (IndexError, ZeroDivisionError):
            return float('nan')

    @property
    def mcc(self):
        """
        Matthews Correlation Coefficient

        Provides a more robust measure than F-score when classes are unbalanced. See:
        Matthews, B.W., Comparison of the predicted and observed secondary structure of T4 phage
        lysozyme. Biochim. Biophys. Acta 1975, 405, 442-451
        """
        try:
            return (((self.tp * self.tn) - (self.fp * self.fn)) /
                    sqrt((self.tp + self.fp) * (self.tp + self.fn) *
                         (self.tn + self.fp) * (self.tn + self.fn)))
        except ZeroDivisionError:
            return float('nan')

    @property
    def all_stats(self):
        """All relevant statistics."""
        return (self.precision, self.recall, self.fscore, self.mcc)

    def __str__(self):
        return "\n".join([
                "Gold standard of {0} items, {1:2.1f}% positive".format(len(self),
                                                                  100 * (self.tp + self.fn) / len(self)),
                "Precision: {0:.3f} ({1} items)".format(self.precision, self.tp + self.fp),
                "Recall: {0:.3f} ({1} items)".format(self.recall, self.tp + self.fn),
                "F-score: {0:.3f}".format(self.fscore),
                "Baseline F-score: {0:.3f}".format(self.baseline_fscore),
                "MCC: {0:.3f}".format(self.mcc),
                ])

    def __repr__(self):
        return "<SDMetrics with %s observations>" % len(self)

    def __len__(self):
        return self.tp + self.fp + self.tn + self.fn

    def confusion_matrix(self):
        """Return a string representation of a confusion matrix."""
        return "\n".join(["\t-Predicted labels-", "-Gold-\tTrue\tFalse",
                          "True\t{0}\t{1}".format(self.tp, self.fn),
                          "False\t{0}\t{1}".format(self.fp, self.tn)])
