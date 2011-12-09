"""Convert a text corpus to a syllabified phonemic representation using CMUdict"""

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
import re

from syllabification import format_sylls
from eng_syll import eng_syllabify
from esp_syll import esp_syllabify
from esp_dict import EspLex

ALT_RE = re.compile(".+\(\d+\)$")

def load_pron_dict(dict_path):
    """Load a dictionary in CMUdict format to a word->pronunciation dictionary.
    
    Only the first alternation pronunciation is kept. Keys are lowercased.
    Values contain the pronunciation as a list of phonemes."""
    prons = {}
    
    # Read in the dictionary
    dict_file = open(dict_path, 'rU')
    for line in dict_file:
        # Skip comments
        if line.startswith(";;;"):
            continue
        
        # Split the line on double space
        (word, pron) = line.rstrip().split("  ")
        
        # If the word is an alternate pron, skip it
        if ALT_RE.match(word):
            continue
        
        # Reformat
        word = word.lower()
        pron = pron.split()
        
        # Store the word
        prons[word] = pron
        
    dict_file.close()
    return prons


def load_eng_syll_dict(dict_path):
    """Return a syllabified pron dict from the unsyllabified dict given."""
    # The raw pronunciation for each word
    prons = load_pron_dict(dict_path)
    
    # The syllabified pronunciation of each word
    return dict((word, eng_syllabify(pron)) for word, pron in prons.items())


def load_esp_syll_dict(dict_path):
    """Return a syllabified pron dict from the unsyllabified dict given."""
    # Load the lexicon
    lex = EspLex(dict_path)
        
    # Make a syllabified dictionary
    return dict((word, esp_syllabify(*lex.get_pron(word))) for word in lex)


def convert(syll_prons):
    """Convert data from stdin into its syllabified pron."""
    # Now replace every word in the corpus with its syllabified phonemic equivalent
    hits = 0
    misses = 0
    for line in sys.stdin:
        words = line.split()
        word_prons = [format_sylls(syll_prons[word]) for word in line.split() 
                      if word in syll_prons]
        
        # Print misses to stderr
        for word in line.split():
            if word not in syll_prons:
                try:
                    print >> sys.stderr, word.encode('ascii')
                except UnicodeDecodeError:
                    pass
        
        
        # Print if all words we transcribed
        if len(word_prons) == len(words):
            print " ".join(word_prons)
            hits += 1
        else:
            misses += 1

    print >> sys.stderr, "Utterances excluded: %d/%d" % (misses, hits + misses)


def main():
    """Parse command line arguments and call the converter"""
    try:
        dict_path = sys.argv[1]
        lang = sys.argv[2]
    except IndexError:
        print "Usage: convert_phonemic dictionary lang"
        sys.exit(2)
        
    if lang == "eng":
        syll_dict = load_eng_syll_dict(dict_path)
    elif lang == "esp":
        syll_dict = load_esp_syll_dict(dict_path)
        
    convert(syll_dict)


if __name__ == "__main__":
    main()