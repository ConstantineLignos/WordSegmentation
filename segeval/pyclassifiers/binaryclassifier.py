#!/usr/bin/env python
# 
# Copyright (c) 2013 Kyle Gorman <gormanky@ohsu.edu>
# 
# Permission is hereby granted, free of charge, to any person obtaining a 
# copy of this software and associated documentation files (the 
# "Software"), to deal in the Software without restriction, including 
# without limitation the rights to use, copy, modify, merge, publish, 
# distribute, sublicense, and/or sell copies of the Software, and to 
# permit persons to whom the Software is furnished to do so, subject to 
# the following conditions:
# 
# The above copyright notice and this permission notice shall be included 
# in all copies or substantial portions of the Software.
# 
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
# OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
# IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY 
# CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
# TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
# SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
# 
# contains a binary classification abstract class, and threshold finder that
# maximizes binary classification accuracy

from math import sqrt
from operator import xor
from itertools import combinations

class BinaryClassifier(object):
    """
    Dummy class representing a binary classifier
    """
     
    #FIXME implement me!
    def __repr__(self):
        raise NotImplementedError

    def __init__(self, X, Y, hit, **kwargs):
        self.hit = hit
        self.miss = None
        for y in Y: # figure out what the "miss" is called
            if y != hit:
                self.miss = y
                break
        if self.miss is None:
            raise ValueError('Outcomes are invariant')
        self.train(X, Y, **kwargs)

    #FIXME implement me!
    def train(self, X, Y):
        raise NotImplementedError

    def classify_all(self, X):
        """
        Classify a vector of continuous values
        """
        for x in X:
            yield self.classify(x)

    #FIXME implement me!
    def classify(self, x):
        raise NotImplementedError

    def evaluate(self, X, Y):
        """
        Compute scores measuring the classification Y ~ X
        """
        assert all(len(Y) == len(feat) for feat in zip(*X))
        self.tp = 0
        self.tn = 0
        self.fp = 0
        self.fn = 0
        for (guess, answer) in zip(self.classify_all(X), Y):
            self.update_confusion_matrix(guess, answer)

    def update_confusion_matrix(self, guess, answer):
        if guess == answer:
            if guess == self.hit:
                self.tp += 1
            else:
                self.tn += 1
        elif guess == self.hit: # but is wrong
            self.fp += 1
        else:   
            self.fn += 1

    def leave_one_out(self, X, Y):
        """
        Score using leave-one-out cross-validation
        """
        assert all(len(Y) == len(feat) for feat in zip(*X))
        self.tp = 0
        self.tn = 0
        self.fp = 0
        self.fn = 0
        for i in xrange(1, len(Y)):
            self.train(X[:i] + X[i + 1:], Y[:i] + Y[i + 1:])
            self.update_confusion_matrix(self.classify(X[i]), Y[i])

    #FIXME implement me!
    def score(self, x):
        raise NotImplementedError

    def AUC(self, X, Y):
        """
        Compute accuracy as measured by area under the ROC curve (AUC) 
        using all-pairs analysis
        """
        assert all(len(Y) == len(feat) for feat in zip(*X))
        hit_gt_miss = 0
        denominator = 0
        for (i, j) in combinations(xrange(len(Y)), 2):
            if Y[i] == Y[j]: # tie
                continue
            # train on held-out
            self.train(X[:i] + X[i + 1:j] + X[j + 1:],
                       Y[:i] + Y[i + 1:j] + Y[j + 1:])
            # score the happenings
            i_score = self.score(X[i])
            j_score = self.score(X[j])
            if i_score == j_score: # tie
                continue
            hit_gt_miss += xor(Y[i] == self.hit, i_score < j_score)
            denominator += 1
        # compute area
        if denominator == 0: # worthless (avoids a zero division exception)
            return .5
        AUC = hit_gt_miss / float(denominator)
        if AUC < .5: # swap hits and misses
            AUC = 1. - AUC
        return AUC

    def precision(self):
        denom = self.tp + self.fp
        if not denom:   
            return float('nan')
        return self.tp / float(denom)
    
    def recall(self):
        denom = self.tp + self.fn
        if not denom:
            return float('nan')
        return self.tp / float(denom)
    
    # alias for recall
    def sensitivity(self):
        return self.recall()

    def specificity(self):
        denom = self.tn + self.fp
        if not denom:
            return float('nan')
        return self.tn / float(denom)

    def accuracy(self):
        numer = self.tp + self.fn
        if not numer:
            return 0.
        return numer/ float(numer + self.fp + self.fn)

    def F1(self):
        p = self.precision()
        r = self.recall()
        return 2. * p * r / (p + r)

    def MCC(self):
        N = float(self.tp + self.tn + self.fp + self.fn)
        S = (self.tp + self.fn) / N
        P = (self.tp + self.fp) / N
        PS = P * S
        denom = sqrt(PS * (1. - S) * (1. - P))
        if denom == 0:
            return float('nan')
        return ((self.tp / N) - PS) / denom

# a class representing a numerical threshold; stump.Stump is little
# more than a wrapper around this, but it is also used by LDA

class Threshold(object):
    """
    Class representing a single split in a continuous vector of data
    """

    # static methods 

    @staticmethod
    def updo(lower, upper):
        return upper[True] - upper[False] - lower[True] + lower[False]

    # instance methods

    def __repr__(self):
        if self.hit_upper:
            return 'Threshold(miss < {:.6} < hit)'.format(self.split)
        else:
            return 'Threshold(hit < {:.6} < miss)'.format(self.split)

    def __init__(self, scores, Y):
        # make sorted copy
        (my_s, my_Y) = zip(*sorted(zip(scores, Y)))
        # hits and misses on either side of the threshold
        lower = {True: 0, False: 0}
        upper = {True: 0, False: 0}
        # start with most likely candidate, as the upper side
        for y in my_Y:
            upper[y] +=1
        upl = Threshold.updo(lower, upper)
        fit = abs(upl)            # score so far
        self.split = None         # negative infinity...
        self.hit_upper = upl >= 0 # boolean
        # run through the split points
        prev_s = None # negative infinity...
        for (s, y) in zip(my_s, my_Y):
            if s != prev_s:
                # score the previous threshold; otherwise don't bother
                upl   = Threshold.updo(lower, upper)
                a_upl = abs(upl)
                if a_upl > fit:
                    fit = a_upl
                    self.split = s
                    self.hit_upper = upl >= 0
            # move an observation from upper to lower
            upper[y] -= 1
            lower[y] += 1
            # store the score for comparison at next iteration
            prev_s = s

    def is_hit(self, score):
        return xor(score < self.split, self.hit_upper)


if __name__ == '__main__':
    import doctest
    doctest.testmod()
