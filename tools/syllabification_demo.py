"""A syllabification learning demo that learns by utterance onsets."""

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

import re

import nltk

VOWELS = set(('AA', 'AE', 'AH', 'AO', 'AW', 'AY', 'EH', 'ER', 'EY', 'IH', 'IY',
    'OW', 'OY', 'UH', 'UW'))

CONSONANTS = set(('B', 'CH', 'D', 'DH', 'F', 'G', 'HH', 'JH', 'K', 'L', 'M',
    'N', 'NG', 'P', 'R', 'S', 'SH', 'T', 'TH', 'V', 'W', 'Y', 'Z', 'ZH'))

ONSETS = set(('P', 'T', 'K', 'B', 'D', 'G', 'F', 'V', 'TH', 'DH', 'S', 'Z', 
    'SH', 'CH', 'JH', 'M', 'N', 'R', 'L', 'HH', 'W', 'Y', 'P.R', 'T.R', 'K.R', 
    'B.R', 'D.R', 'G.R', 'F.R', 'TH.R', 'SH.R', 'P.L', 'K.L', 'B.L', 'G.L', 
    'F.L', 'S.L', 'T.W', 'K.W', 'D.W', 'S.W', 'S.P', 'S.T', 'S.K', 'S.F', 'S.M',
    'S.N', 'G.W', 'SH.W', 'S.P.R', 'S.P.L', 'S.T.R', 'S.K.R', 'S.K.W', 'S.K.L',
    'TH.W', 'ZH', 'P.Y', 'K.Y', 'B.Y', 'F.Y', 'HH.Y', 'V.Y', 'TH.Y', 'M.Y',
    'S.P.Y', 'S.K.Y', 'G.Y'))

def main():
    """Try to syllabify!"""
    in_file = open('../gambellyang/seg/l_formatted.txt', 'rU')
    lines = [parse_line(line) for line in in_file]
    
    # Count the phonemes and onsets
    phonemes_fd = nltk.FreqDist()
    onsets_fd = nltk.FreqDist()
    codas_fd = nltk.FreqDist()
    
    for line in lines:
        for idx, word in enumerate(line):
            phonemes = segment_phonemes(word)

            # Always count the phonemes
            for phoneme in phonemes:
                phonemes_fd.inc(phoneme)

            # If it's the first word, count the onset
            if idx == 0:
                onsets_fd.inc(get_onset(phonemes))
            
            # If it's the last word, count the coda
            if idx == len(line) - 1:
                # Just reverse the phonemes and get the onset and reverse it
                codas_fd.inc(get_coda(phonemes))
                
    # Create sets for onset comparision
    onsets = set(onsets_fd.keys())
    onsets.remove('') # Remove null onset
    good_onsets = onsets & ONSETS
    bad_onsets = onsets - ONSETS
    missed_onsets = ONSETS - onsets
    
    onset_type_coverage = float(len(good_onsets))/len(ONSETS)
    
    print 'Onset Coverage:'
    print 'Types:', onset_type_coverage
    print 'Bad onsets:', len(bad_onsets)
    print_contents(bad_onsets)
    print
    
    print 'Missed onsets:', len(missed_onsets), '/', len(ONSETS)
    print_contents(missed_onsets)
    print
    
    print 'Phonemes:'
    top_phonemes = ["%s\t%d" % item for item in phonemes_fd.items()]
    print_contents(top_phonemes)
    print
    
    print 'Onsets:'
    top_onsets = ["%s\t%d" % item for item in onsets_fd.items()]
    print_contents(top_onsets)
    print
    
    print 'Codas:'
    top_codas = ["%s\t%d" % item for item in codas_fd.items()]
    print_contents(top_codas)
    print            


def parse_line(line):
    """Parse a line from the input, returning a list of cleaned words"""
    return clean_markings(line.rstrip()).split()


CLEAN_RE = re.compile(r'\d|\|')
def clean_markings(text):
    """Sanitize text"""
    return CLEAN_RE.sub('', text)

def segment_phonemes(word):
    """Segment a word into its phonemes"""
    phonemes = []
    current = ""
    last = word[0].upper()
    
    # Scan with a one-char lookahead
    for char in word[1:]:
        current = last + char.upper()
        
        # If last contains a phoneme but current does it, flush
        if ((last in VOWELS or last in CONSONANTS) and 
            not (current in VOWELS or current in CONSONANTS)):
            phonemes.append(last)
            last = char.upper()
        # Otherwise, get more phonemes
        else:
            last = current
            
    # Clean up left over segments
    if last in VOWELS or last in CONSONANTS:
        phonemes.append(last)
    else:
        assert("Segments left over in phoneme segmentation: " + last)
    
    return phonemes

def get_onset(phonemes):
    """Return the onset of a sequence of phonemes as a string"""
    onset = []
    
    # Build up the onset until we hit a vowel
    for phoneme in phonemes:
        if phoneme in CONSONANTS:
            onset.append(phoneme)
        else:
            break
    
    return '.'.join(onset)

def get_coda(phonemes):
    """Return the coda of a sequence of phonemes as a string"""
    coda = []
    
    # Build up the coda from the end of the string until we hit a vowel
    for phoneme in phonemes[::-1]:
        if phoneme in CONSONANTS:
            coda.insert(0, phoneme)
        else:
            break
    
    return '.'.join(coda)

def print_contents(items):
    """Print the contents of an iterable structure."""
    for item in items:
        print item

if __name__ == "__main__":
    main()
