"""Provide functions for reading the CALLHOME Spanish lexicon."""

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

import codecs

class EspLex(dict):
    """A class for representing a lexicon."""
    
    def __init__(self, path):
        """Load the lexicon at the given path."""       
        dict.__init__(self)        
         
        lex_file = codecs.open(path, 'Ur', 'ISO-8859-1')
        
        for line in lex_file:
            # Lexicon fields:
            # Field 1:    orthographic form (headword)
            # Field 2:    morphological analysis of the headword
            # Field 3:    pronunciation of the headword
            # Field 4:    primary stress information of the word
            # Field 5:    frequency of the word in the 80 Callhome Spanish training 
            #                 transcripts
            # Field 6:    frequency of the word in Corpus Oral radio transcripts
            # Field 7:    frequency of the word in AP newswire
            # Field 8:    frequency of the word in Reuters newswire
            # Field 9:    frequency of the word in El Norte newswire
        
            fields = line.strip().split()
            text = fields[0]
            analysis = fields[1]
            # Take the first pron and stress
            pron = fields[2].split('//')[0]
            stress = fields[3].split('//')[0]
            
            self[text] = (pron, stress, analysis)
            
        lex_file.close()
            
    def get_pron(self, word):
        """Return a word's (pronunciation, stress) in a tuple."""
        return self[word][0:2]
    
    def get_analysis(self, word):
        """Return the word's analysis."""
        return self[word][2]
