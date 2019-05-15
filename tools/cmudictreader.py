"""
Tools for reading from the CMU Pronouncing Dictionary.
"""

# Copyright (C) 2011-2013 Constantine Lignos
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

import re
import sys


class CMUDict(dict):

    """A representation of the CMU Pronouncing Dictionary."""

    ALT_RE = re.compile(r".+\(\d+\)$")

    def __init__(self, dict_path):
        dict.__init__(self)
        self._load_pron_dict(dict_path)

    def _load_pron_dict(self, dict_path):
        """Load a dictionary in CMUdict format to a word->pronunciation dictionary.

        Only the first pronunciation is kept for each word. Keys are lowercased.
        Values contain the pronunciation as a list of phonemes. The data and information
        about the phoneme set used are at:
        https://cmusphinx.svn.sourceforge.net/svnroot/cmusphinx/trunk/cmudict/

        Sample usage, assuming a current CMUDict file is available in the working dir,
        which you can ensure by running download():
        >>> CMUDict(DEFAULT_PATH)["constantine"]
        ['K', 'AA1', 'N', 'S', 'T', 'AH0', 'N', 'T', 'IY2', 'N']
        """

        # Read in the dictionary
        try:
            dict_file = open(dict_path, 'rU')
        except IOError:
            raise IOError("The CMUDict file %s could not be found. You can run "
                          "cmudictreader.download() to download a copy of the dictionary." %
                          dict_path)

        for line in dict_file:
            # Skip comments
            if line.startswith(";;;"):
                continue

            # Split the line on double space
            try:
                (word, pron) = line.rstrip().split("  ")
            except ValueError:
                print >> sys.stderr, "Unreadable line in dictionary:", repr(line.rstrip())
                continue

            # If the word is an alternate pron, skip it
            if CMUDict.ALT_RE.match(word):
                continue

            # Reformat
            word = word.lower()
            pron = pron.split()

            # Store the word
            self[word] = pron

        dict_file.close()


if __name__ == "__main__":
    import doctest
    doctest.testmod()
