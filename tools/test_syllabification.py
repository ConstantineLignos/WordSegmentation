"""Test syllabification."""

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

# pylint: disable-msg=W0401,W0614
from syllabification import *
from eng_syll import *
from esp_syll import *
from esp_dict import *

import unittest

class TestSyllabification(unittest.TestCase):
    """Test syllabification module"""
    
    def test_phonemes_str(self):
        """Phonemes convert to strings with periods between phonemes"""
        self.assertEqual(phonemes_str(('B')), 'B')
        self.assertEqual(phonemes_str(('B', 'IH', 'G')), 'B.IH.G')
        
    def test_str_phonemes(self):
        """Strings are split on periods to make phoneme sequences"""
        self.assertEqual(str_phonemes('B'), ('B',))
        self.assertEqual(str_phonemes('B.IH.G'), ('B', 'IH', 'G'))
        
    def test_max_onset_split_badonset(self):
        """Impossible onsets result in an index of the length"""
        self.assertEqual(max_onset_split(str_phonemes('X'), ENG_ONSETS), 1)
        self.assertEqual(max_onset_split(str_phonemes('X.X'), ENG_ONSETS), 2)
        
    def test_max_onset_split_allonset(self):
        """Complete onset results in split index of zero"""
        self.assertEqual(max_onset_split(str_phonemes('B'), ENG_ONSETS), 0)
        self.assertEqual(max_onset_split(str_phonemes('B.R'), ENG_ONSETS), 0)
        
    def test_max_onset_split_someonset(self):
        """Coda/onset results in split index in middle"""
        self.assertEqual(max_onset_split(str_phonemes('Z.B'), ENG_ONSETS), 1)
        self.assertEqual(max_onset_split(str_phonemes('Z.B.R'), ENG_ONSETS), 1)
        self.assertEqual(max_onset_split(str_phonemes('T.S.B.R'), ENG_ONSETS), 2)

    def test_eng_syllabification_onesyll(self):
        """One syllable words are one syllable"""
        self.assertEqual(eng_syllabify(str_phonemes('IH1.L')), 
                         (str_phonemes('IH1.L'),))
        
        self.assertEqual(eng_syllabify(str_phonemes('B.IH1.G')), 
                         (str_phonemes('B.IH1.G'),))
        
    def test_eng_syllabification_twosyll(self):
        """Two syllable words are two syllables"""
        self.assertEqual(eng_syllabify(str_phonemes('B.IH1.G.ER0')), 
                         (str_phonemes('B.IH1'), str_phonemes('G.ER0')))
        
        self.assertEqual(eng_syllabify(str_phonemes('CH.EH1.K.ER0')), 
                         (str_phonemes('CH.EH1'), str_phonemes('K.ER0')))
        
        self.assertEqual(eng_syllabify(str_phonemes('CH.EH1.K.ER0.Z')), 
                         (str_phonemes('CH.EH1'), str_phonemes('K.ER0.Z')))
        
    def test_eng_syllabification_vowel(self):
        """Two vowels in a row implies a syllable break"""
        self.assertEqual(eng_syllabify(str_phonemes('HH.UW1.IH1.Z')), 
                         (str_phonemes('HH.UW1'), str_phonemes('IH1.Z')))
        
    def test_esp_syllabification_onesyll(self):
        """One syllable words are one syllable"""
        self.assertEqual(esp_syllabify(str_phonemes('l.o')), 
                         (str_phonemes('l.o'),))
        
        self.assertEqual(esp_syllabify(str_phonemes('l.o.s')), 
                         (str_phonemes('l.o.s'),))
        
        self.assertEqual(esp_syllabify(str_phonemes('y.o')), 
                         (str_phonemes('y.o'),))
        
        # Test for glides
        self.assertEqual(esp_syllabify(str_phonemes('b.y.e.n')), 
                         (str_phonemes('b.y.e.n'),))
        
    def test_esp_syllabification_twosyll(self):
        """Two syllable words are two syllables"""
        self.assertEqual(esp_syllabify(str_phonemes('a.m.a.s')), 
                         (str_phonemes('a'), str_phonemes('m.a.s')))
        
        self.assertEqual(esp_syllabify(str_phonemes('a.n.t.r.o.s')), 
                         (str_phonemes('a.n'), str_phonemes('t.r.o.s')))
        
        self.assertEqual(esp_syllabify(str_phonemes('p.o.r.t.l.a.n.d')), 
                         (str_phonemes('p.o.r'), str_phonemes('t.l.a.n.d')))
        
        self.assertEqual(esp_syllabify(str_phonemes('p.o.r.t.l.a.n.d')), 
                         (str_phonemes('p.o.r'), str_phonemes('t.l.a.n.d')))
        
    def test_esp_syllabification_vowel(self):
        """Test the handling of two vowel sequences"""
        self.assertEqual(eng_syllabify(str_phonemes('HH.UW1.IH1.Z')), 
                         (str_phonemes('HH.UW1'), str_phonemes('IH1.Z')))
        
    def test_esp_onsets(self):
        """Test the valid onset checking"""
        # Any one consonant should work
        self.assertTrue(is_esp_onset('m'))
        self.assertTrue(is_esp_onset('b'))
        self.assertTrue(is_esp_onset('l'))
        self.assertTrue(is_esp_onset('r'))
        self.assertTrue(is_esp_onset('t'))
        self.assertTrue(is_esp_onset('s'))
        
        # Any vowel should fail
        self.assertFalse(is_esp_onset('a'))
        self.assertFalse(is_esp_onset('e'))
        self.assertFalse(is_esp_onset('i'))
        self.assertFalse(is_esp_onset('o'))
        self.assertFalse(is_esp_onset('u'))
        
        # Consonant plus a liquid is okay
        self.assertTrue(is_esp_onset('tl'))
        self.assertTrue(is_esp_onset('br'))
        
        # Consonant plus non liquid is bad
        self.assertFalse(is_esp_onset('lt'))
        self.assertFalse(is_esp_onset('rb'))
        self.assertFalse(is_esp_onset('bb'))
        
        # More than one consonant plus liquid is bad
        self.assertFalse(is_esp_onset('stl'))
        self.assertFalse(is_esp_onset('lbr'))
        
    def test_esp_syll_count(self):
        """Test by checking the number of syllables against a lexicon."""
        lex = EspLex("../data/sp_lex_CEL.v04")
        
        for word in lex:
            pron, stress = lex.get_pron(word)
            sylls = esp_syllabify(pron, stress)
            
            if len(sylls) != len(stress):
                self.assertEqual(len(stress), len(sylls))    


if __name__ == '__main__':
    unittest.main()