#!/usr/bin/python
"""Cleans up the output of the word segmenter to be read by other tools."""

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

import sys
import re

from lexinfo.arpabet import convert_twochar_phoneseq


STRESS_RE = re.compile(r'\(\d\)')
SYLL_SEP = '|'
PHONE_SEP = '.'


def _clean_word(word):
    """Remove extra markers from a word."""
    return STRESS_RE.sub("", word.replace(SYLL_SEP, PHONE_SEP))


def main():
    """Read a segmentation from stdin and write the clean version to stdout."""
    for line in sys.stdin:
        # Split on white space
        words = line.split()

        # Convert each word: clean stress, split phones, convert phones,
        # and join phones
        clean_words = \
            ["".join(convert_twochar_phoneseq(_clean_word(word).split('.')))
             for word in words]

        # Join the sentence together
        print " ".join(clean_words)


if __name__ == "__main__":
    main()
