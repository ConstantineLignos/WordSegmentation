"""Analyze segmentation errors."""

# Copyright (C) 2010, 2011 Constantine Lignos
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

import sys
from collections import defaultdict

INTERVAL_SIZE = 3000

class ErrorCounter:
    def __init__(self):
        self.undersegs = 0
        self.oversegs = 0
        self.error_counter = defaultdict(int)

def sum_errors(errors, intervals):
    """Sum the word errors across the given intervals."""
    word_error_sums = defaultdict(int)
    for interval in intervals:
        interval_error_counts = errors[interval].error_counter
        for error, count in interval_error_counts.items():
            word_error_sums[error] += count
            
    return word_error_sums


def main():
    """Process the under/oversegmentation rate of each interval."""
    input_path = sys.argv[1]
    interval_segs_path = sys.argv[2]
    interval_errors_path = sys.argv[3]

    first = True
    interval = 0
    # Store errors as a dictionary, seeding the first interval
    errors = defaultdict(ErrorCounter)
    
    for line in open(input_path, "U"):
        # Skip the header
        if first:
            first = False
            continue
        
        # Parse the line
        n_utt, error, error_type = line.strip().split(",")
        n_utt = int(n_utt)
                
        # Compute interval number for the current utterance, flooring and adding one so
        # the range starts at 1
        interval = n_utt / INTERVAL_SIZE + 1
        
        # Count the score in this interval
        if error_type == "UNDERSEG":
            errors[interval].undersegs += 1
        elif error_type == "OVERSEG":
            errors[interval].oversegs += 1
        else:
            raise ValueError("Unkown error type: " + error_type)
    
        # Record the error
        errors[interval].error_counter[error] += 1


    # Now print each interval and the under/overseg ratio. We exclude the last interval
    # because it will be incomplete
    interval_segs_out = open(interval_segs_path, "w")
    print >> interval_segs_out, "Interval,Ratio,Sum"
    for interval in sorted(errors.keys())[:-1]:
        undersegs = errors[interval].undersegs
        oversegs = errors[interval].oversegs
        
        ratio = float(oversegs) / undersegs if undersegs else "INF"
        sum = oversegs - undersegs
        
        print >> interval_segs_out, ",".join([str(item) for item in (interval, ratio, sum)])
        
    # Bin together errors in the first and second 5k of segmentation
    intervals = sorted(errors.keys())
    first_intervals = [interval for interval in intervals if (interval * INTERVAL_SIZE) < 10000]
    second_intervals = [interval for interval in intervals if 40000 < (interval * INTERVAL_SIZE) < 50000]
    
    first_errors = sum_errors(errors, first_intervals)
    second_errors = sum_errors(errors, second_intervals)
    
    interval_words_out = open(interval_errors_path, "w")
    print >> interval_words_out, "First errors:"
    for error, count in sorted(first_errors.items(), key=lambda x: x[1], reverse=True)[:50]:
        print >> interval_words_out, ",".join((error, str(count)))
    
    print >> interval_words_out
        
    print >> interval_words_out, "Second errors:"
    for error, count in sorted(second_errors.items(), key=lambda x: x[1], reverse=True)[:50]:
        print >> interval_words_out, ",".join((error, str(count)))
        
    

  
if __name__ == "__main__":
    main()
