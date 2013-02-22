#!/usr/bin/env python
"""
Tools for evaluating word segmenters.

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

import argparse

from segeval.corpus import Corpus
from segeval.models.diphone import DiphoneSegmenter


SUPERVISED_DIPHONE = "supdiphone"
METHODS = (SUPERVISED_DIPHONE,)


def evaluate(method, train_path, test_path):
    """Evaluate a word segmentation strategy."""
    train_corpus = Corpus(train_path)
    if test_path:
        test_corpus = Corpus(test_path)

    print "Training..."
    if method == SUPERVISED_DIPHONE:
        seg = DiphoneSegmenter()
        seg.train(train_corpus)

    print "Evaluating..."
    # TODO: Evaluate

def main():
    """Evaluate strategies for word segmentation."""
    parser = argparse.ArgumentParser(description=main.__doc__)
    parser.add_argument('method', choices=METHODS, help='Method to evaluate')
    parser.add_argument('train', help='Training corpus')
    parser.add_argument('--test', help='Testing corpus', default=None)
    args = parser.parse_args()
    evaluate(args.method, args.train, args.test)


if __name__ == "__main__":
    main()
