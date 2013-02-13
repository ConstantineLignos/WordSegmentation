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
# Given a binomial variable Y (represented as a string) and a continuous 
# variable X (represented as number), find a split-point which minimizes 
# classification error, and report classification statistics

from math import sqrt

from binaryclassifier import BinaryClassifier, Threshold

class Stump(BinaryClassifier):
    """
    Compute a classifier which makes a single "cut" in a continuous
    predictor vector X that splits the outcomes into "hit" and "miss"
    so as to maximize the number of correct classifications

    >>> from csv import DictReader
    >>> X = []
    >>> Y = []
    >>> for row in DictReader(open('iris.csv', 'r')):
    ...     X.append([float(row['Petal.Width'])])
    ...     Y.append(row['Species'])
    >>> s = Stump(X, Y, 'versicolor')
    >>> s.leave_one_out(X, Y)
    >>> round(s.accuracy(), 2)
    0.88
    >>> round(s.AUC(X, Y), 2)
    0.99
    """

    def __repr__(self):
        lower = self.miss
        upper = self.hit
        if not self.thresh.hit_upper: # swap
            (lower, upper) = (upper, lower)
        return 'Stump({} < {: 02.3f} < {})'.format(lower,
                                            self.thresh.split, upper)

    def train(self, X, Y):
        """
        Find the optimal split point
        """
        # train
        self.thresh = Threshold([x[0] for x in X],
                                [y == self.hit for y in Y])

    def score(self, x):
        return x[0]

    def classify(self, x):
        return self.hit if self.thresh.is_hit(self.score(x)) else self.miss


if __name__ == '__main__':
    import doctest
    doctest.testmod()
