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

from __future__ import division
import argparse
from itertools import chain
import csv
from operator import itemgetter
from collections import defaultdict, Counter

from segeval.corpus import Corpus
from segeval.util import counter_freqs


PHONEMES = "phonemes"
DIPHONES = "diphones"
DIBS = "dibs"
FEATURES = (DIPHONES, PHONEMES, DIBS)
BOUNDARY_HEADER = 'boundary'


def convert_r_bool(value):
    """Convert a value to R's TRUE or FALSE."""
    return "TRUE" if value else "FALSE"


def diphone_features(corpus, out_csv):
    """Write diphone features of a corpus to a CSV."""
    diphone_boundaries = chain.from_iterable(corpus.diphone_boundaries)
    # Convert counts to probabilities
    diphone_freq = counter_freqs(corpus.diphone_counts)

    # Write header
    out_csv.writerow(('diphone', 'prob', BOUNDARY_HEADER))

    # Write data
    for diphone, label in diphone_boundaries:
        out_csv.writerow((''.join(diphone), diphone_freq[diphone], convert_r_bool(label)))


def phoneme_features(corpus, out_csv):
    """Write phoneme counts to a CSV."""
    # Convert counts to probabilities
    phoneme_freq = counter_freqs(corpus.phoneme_counts)

    # Write header
    out_csv.writerow(('phoneme', 'prob', 'rank'))

    # Write data
    for idx, (phoneme, count) in enumerate(sorted(phoneme_freq.items(),
                                                  key=itemgetter(1), reverse=True)):
        out_csv.writerow((phoneme, count, idx + 1))


def dibs_features(corpus, out_csv):
    """Write information for the DiBs segmentation model to a CSV."""
    # Get phoneme and boundary information
    phoneme_counts = corpus.phoneme_counts
    phoneme_freq = counter_freqs(phoneme_counts)

    # Get diphone information
    diphone_freq = counter_freqs(corpus.diphone_counts)

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
    boundary_diphone_probs = \
        {label: counter_freqs(counts) for label, counts in boundary_diphone_counts.items()}
    assert all(0 <= prob <= 1.0 for diphone_probs in boundary_diphone_probs.values()
               for prob in diphone_probs.values())
    # If this assertion fails but the value is very close to 1.0, this just means
    # there is a lot of accumulated rounding error.
    assert all(.99 < sum(diphone_probs.values()) < 1.01
               for diphone_probs in boundary_diphone_probs.values())

    # P(#|xy)
    diphone_boundary_probs = \
        {diphone: (sum(outcomes) / len(outcomes))
         for diphone, outcomes in diphone_outcomes.iteritems()}
    assert all(0 <= prob <= 1.0 for prob in diphone_boundary_probs.values())

    # P(#)
    p_boundary = total_boundaries / total_diphones
    assert 0 <= p_boundary <= 1.0

    ## Get phrase initial/final counts
    initial_counts, final_counts = corpus.outside_phoneme_counts
    # P(x|initial)
    initial_freq = counter_freqs(initial_counts)
    # P(x|final)
    final_freq = counter_freqs(final_counts)

    # Output information for each boundary
    out_csv.writerow(('diphone', 'prob.true', 'prob.dibs', 'prob.est1', 'prob.est2', 'score',
                      BOUNDARY_HEADER))
    for diphone, label in chain.from_iterable(corpus.diphone_boundaries):
        # Estimate P(x|inital) and P(y|final) for a diphone xy
        phone1, phone2 = diphone
        p_phone1_final = final_freq[phone1] if phone1 in final_freq else 0.0
        p_phone2_init = initial_freq[phone2] if phone2 in initial_freq else 0.0

        # Compute the DiBS score
        assert 1.0 >= diphone_freq[diphone] >= 0.0
        dibs_score = (2.0 * p_phone1_final * p_phone2_init) / diphone_freq[diphone]

        # True P(#|xy)
        # If you want to do it by Bayes' rule, it would be:
        # P(xy|#) * P(#) / P(xy)
        # (boundary_diphone_probs[True][diphone] * p_boundary) / diphone_freq[diphone]
        true_prob = diphone_boundary_probs[diphone]

        # Compute Daland's estimated P(#|xy) assuming P(#) = .28, the mean
        # value explored in their study
        dibs_prob = (p_phone1_final * p_phone2_init * 0.28) / diphone_freq[diphone]
        # Probability that it's not a boundary
        p_phone1_final = final_freq[phone1] if phone1 in final_freq else 0.0
        p_phone2_init = initial_freq[phone2] if phone2 in initial_freq else 0.0

        # A way to estimate with a more normal normalization in the denominator
        est1_prob = ((p_phone1_final * p_phone2_init * 0.28) /
                     (phoneme_freq[phone1] * phoneme_freq[phone2]))

        # Another way of estimating:
        # P(x) * P(#|x) * P(y|#) / P(xy)
        # P(x) * P(final|x) * P(y|initial) / P(xy)
        try:
            p_phone1_final = final_counts[phone1] / phoneme_counts[phone1]
        except KeyError:
            p_phone1_final = 0.0
        est2_prob = ((phoneme_freq[phone1] * p_phone1_final * p_phone2_init) /
                     (diphone_freq[diphone]))

        out_csv.writerow((''.join(diphone), true_prob, dibs_prob, est1_prob,
                          est2_prob, dibs_score, convert_r_bool(label)))


def extract(features, in_path, out_path):
    """Evaluate a word segmentation strategy."""
    corpus = Corpus(in_path)
    out_csv = csv.writer(open(out_path, 'wb'))

    feature_handlers = {
        DIPHONES: diphone_features,
        PHONEMES: phoneme_features,
        DIBS: dibs_features,
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
