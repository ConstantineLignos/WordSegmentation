#!/usr/bin/env python
"""
Dump features for word segmentation

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
from itertools import chain
import csv

from segeval.corpus import Corpus
from segeval.util import counter_freqs


DIPHONES = "diphones"
FEATURES = (DIPHONES,)
BOUNDARY_HEADER = 'Boundary'


def convert_r_bool(value):
    """Convert a value to R's TRUE or FALSE."""
    return "TRUE" if value else "FALSE"


def diphone_features(corpus, out_csv):
    """Write diphone features of a corpus to a CSV."""
    diphone_boundaries = chain.from_iterable(corpus.diphone_boundaries)
    # Convert counts to probabilities
    diphone_freq = counter_freqs(corpus.diphone_counts)

    # Write header
    out_csv.writerow(('Diphone', 'Prob', BOUNDARY_HEADER))

    # Write data
    for diphone, label in diphone_boundaries:
        out_csv.writerow((''.join(diphone), diphone_freq[diphone], convert_r_bool(label)))


def extract(features, in_path, out_path):
    """Evaluate a word segmentation strategy."""
    print "Loading corpus..."
    corpus = Corpus(in_path)
    out_csv = csv.writer(open(out_path, 'wb'))

    print "Extracting features..."
    feature_handlers = {
     DIPHONES: diphone_features,
    }
    feature_handlers[features](corpus, out_csv)


def main():
    """Evaluate strategies for word segmentation."""
    parser = argparse.ArgumentParser(description=main.__doc__)
    parser.add_argument('features', choices=FEATURES, help='Features to extract')
    parser.add_argument('input', help='Input corpus')
    parser.add_argument('output', help='Output path')
    args = parser.parse_args()
    extract(args.features, args.input, args.output)


if __name__ == "__main__":
    main()
