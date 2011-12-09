"""Implements Spanish syllabification."""

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

from memoization import memoized
from syllabification import syllabify

# Hardcoded prefixes given by Cuayahuitl
PREFIXES = set(('circun', 'cuadri', 'cuadru', 'cuatri', 'quinqu', 'archi', 'arqui', 
            'citer', 'cuasi', 'infra', 'inter', 'intra', 'multi', 'radio', 
            'retro', 'satis', 'sobre', 'super', 'supra', 'trans', 'ulter', 
            'ultra', 'yuxta', 'ante', 'anti', 'cata', 'deci', 'ecto', 'endo', 
            'hemi', 'hipo', 'meta', 'omni', 'pali', 'para', 'peri', 'post', 
            'radi', 'tras', 'vice', 'cons', 'abs', 'ana', 'apo', 'arz', 'bis', 
            'biz', 'cis', 'com', 'con', 'des', 'dia', 'dis', 'dis', 'epi', 
            'exo', 'met', 'pen', 'pos', 'pre', 'pro', 'pro', 'tri', 'uni', 
            'viz', 'ins', 'nos'))

ESP_VOWELS = set(('a', 'e', 'i', 'o', 'u'))
ESP_STRONG_VOWELS = set(('a', 'e', 'o'))
ESP_WEAK_VOWELS = set(('i', 'u'))
ESP_LIQUIDS = set(('l', 'r'))
ESP_GLIDES = set(('y', 'w'))

class EspOnsets:
    """Define a class that provides membership checking by a method."""
    def __init__(self):
        pass
    
    def __contains__(self, item):
        return is_esp_onset(item)


@memoized
def is_esp_onset(chars):
    """Test whether the provided characters form a valid onset."""
    # Exclude a final glide if there are other chars
    if len(chars) > 1 and chars[-1] in ESP_GLIDES:
        chars = chars[:-1]
    
    length = len(chars)
    if length == 1:
        return chars not in ESP_VOWELS
    elif length == 2:
        # Last char must be a liquid
        if chars[-1] not in ESP_LIQUIDS:
            return False
        
        # First char must be a consonant
        return chars[0] not in ESP_VOWELS
    # 3 or more consonants never allowed
    else:
        return False


def esp_split_vowels(sylls, stress):
    """Clean up a syllabification by separating adjacent vowels where needed."""
    split_sylls = []
    
    for syll in sylls:
        curr_syll = []
        last_vowel = None
        
        # Find adjacent vowels and split them if needed
        for char in syll:
            if char in ESP_VOWELS:
                if last_vowel:
                    if ((char in ESP_STRONG_VOWELS and last_vowel in ESP_STRONG_VOWELS) or
                        (char == last_vowel)):
                        # If both vowels are strong or they are the same
                        # vowel always split and reset
                        split_sylls.append(curr_syll)
                        curr_syll = []
                    elif stress and len(sylls) < len(stress):
                        # If one is strong, consult the stress, and add
                        # a break if we need one
                        # Note- we don't update our syllable count based on 
                        # splits we added to make sure we can explicitly spot
                        # any cases where we add too many splits
                        # Split and reset if we need to make another syll
                        split_sylls.append(curr_syll)
                        curr_syll = []
                        
                    # The original algorithm involved a "weak vowel" case, 
                    # but they are all replaced by glides in pronunciation
                    # so it is unnecessary 
                            
                # Always update the last vowel
                last_vowel = char
            
            # Always add the newest char to curr_syll
            curr_syll.append(char)
        
        split_sylls.append(tuple(curr_syll))
    
    return tuple(split_sylls)


def esp_syllabify(word, stress=None):
    """Syllabify the given word using the Cuayahuitl (2004) algorithm."""
    
    # Make an onset instance
    onsets = EspOnsets()
    
    # Call the syllabifier
    first_sylls = syllabify(word, onsets, ESP_VOWELS, True)

    # Now do a second pass for vowels that need to be broken up
    second_sylls = esp_split_vowels(first_sylls, stress)

    return second_sylls

    