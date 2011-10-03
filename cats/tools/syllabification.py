"""Functions for learning syllabification through phonotactics"""

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

def segment_phonemes(word):
    """Segment a word into its phonemes"""
    # Before the phonemes in the input were segmented, this function did real work
    return word.split('.')


def get_onset(phonemes, consonants):
    """Return the onset of a sequence of phonemes as a string"""
    onset = []
    
    # Build up the onset until we hit a vowel
    for phoneme in phonemes:
        if phoneme in consonants:
            onset.append(phoneme)
        else:
            break
    
    return tuple(onset)


def get_coda(phonemes, consonants):
    """Return the coda of a sequence of phonemes as a string"""
    coda = []
    
    # Build up the coda from the end of the string until we hit a vowel
    for phoneme in phonemes[::-1]:
        if phoneme in consonants:
            coda.insert(0, phoneme)
        else:
            break
    
    return '.'.join(coda)


def format_sylls(sylls):
    """Format a sequence of syllables as a word."""
    return "|".join('.'.join(syll) for syll in sylls)


def phonemes_str(phonemes):
    """Convert phonemes to a period-joined string."""
    return '.'.join(phonemes)


def str_phonemes(phonemes):
    """Convert a period-joined string to a tuple of phonemes."""
    return tuple(phonemes.split('.'))


STRESS_RE = re.compile("\d")

def syllabify(phonemes, onsets, vowels, allow_dipthongs=False):
    """Syllabify a sequence of phonemes using max onset and a set of onsets."""
    sylls = [] # Final syllables
    curr_syll = [] # Working syllable contents
    consonants = [] # Working buffer for coda/onset to be segmented
    in_nucleus = False # Flag for current state
    
    for phoneme in phonemes:
        # Strip stress information. Note that we only use this in the check
        # against vowels, the stress information remains in the output
        clean_phoneme = STRESS_RE.sub("", phoneme)
        
        if in_nucleus:        
            # Another vowel means a syllable break unless we allow dipthongs
            if clean_phoneme in vowels:
                if allow_dipthongs:
                    curr_syll.append(phoneme)
                else:
                    # Add the current syllable (which must be non-empty) to the word
                    sylls.append(tuple(curr_syll))
                    curr_syll = [phoneme]
            # If we've hit a consonant, we're no longer in the nucleus.
            # Note the consonant and keep going
            else:
                in_nucleus = False
                consonants.append(phoneme)
        # Otherwise, work through the coda/onset and segment if needed
        else:
            # If we've hit another nucleus, we're in the nucleus.
            # Apply MAX ONSET and insert a syllable break
            if clean_phoneme in vowels:
                in_nucleus = True
                
                # If there are any consonants, split them and finish the current
                # syllable
                if consonants:
                    # The split is the index of the start of the onset
                    split_idx = max_onset_split(consonants, onsets)
                    curr_syll.extend(consonants[:split_idx])
                    
                    # If there is actually anything in the current syllable,
                    # add it to the syllable list and start a new one
                    if curr_syll:
                        sylls.append(tuple(curr_syll))
                    
                    curr_syll = consonants[split_idx:]
                    
                    # Reset consonants
                    consonants = []
                    
                # Always add the vowel to the current syllable
                curr_syll.append(phoneme)                
                    
            # Otherwise, keep adding to the consonants
            else:
                consonants.append(phoneme)
        
    # Empty out the remnants
    curr_syll.extend(consonants)
    sylls.append(tuple(curr_syll))
    
    return tuple(sylls)
                
def max_onset_split(phonemes, valid_onsets):
    """Find the split in a sequence of phonemes that maxmimizes the onset."""
    # The default is no onset at all
    split = 0
    
    # Increasingly slice bigger onsets, we add one in order to include
    # the full string as a possiblity
    for poss_split in range(-1, -(len(phonemes) + 1), -1):
        # Check the current possible onset, if it's valid, update split,
        # otherwise break, the last good value will be returned 
        onset = tuple(phonemes[poss_split:])
        if onset in valid_onsets:
            split = poss_split
        else:
            break
    
    # For convenience, convert split to positive index
    return len(phonemes) + split
        
    