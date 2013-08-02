"""Define English-specific syllabification information."""

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

from syllabification import syllabify

_IMPORTED_SYLLABIFY = False

ENG_VOWELS = set(('AA', 'AE', 'AH', 'AO', 'AW', 'AY', 'EH',
    'ER', 'EY', 'IH', 'IY', 'OW', 'OY', 'UH', 'UW'))

ENG_CONSONANTS = set(('B', 'CH', 'D', 'DH', 'F', 'G', 'HH',
    'JH', 'K', 'L', 'M', 'N', 'NG', 'P', 'R', 'S', 'SH', 'T', 'TH', 'V', 'W',
    'Y', 'Z', 'ZH'))

ENG_ONSETS = set((('P',), ('T',), ('K',), ('B',), ('D',), ('G',), ('F',), ('V',),
    ('TH',), ('DH',), ('S',), ('Z',), ('SH',), ('CH',), ('JH',), ('M',), ('N',), ('R',),
    ('L',), ('HH',), ('W',), ('Y',), ('P', 'R'), ('T', 'R'), ('K', 'R'), ('B', 'R'),
    ('D', 'R'), ('G', 'R'), ('F', 'R'), ('TH', 'R'), ('SH', 'R'), ('P', 'L'),
    ('K', 'L'), ('B', 'L'), ('G', 'L'), ('F', 'L'), ('S', 'L'), ('T', 'W'),
    ('K', 'W'), ('D', 'W'), ('S', 'W'), ('S', 'P'), ('S', 'T'), ('S', 'K'),
    ('S', 'F'), ('S', 'M'), ('S', 'N'), ('G', 'W'), ('SH', 'W'),
    ('S', 'P', 'R'), ('S', 'P', 'L'), ('S', 'T', 'R'), ('S', 'K', 'R'),
    ('S', 'K', 'W'), ('S', 'K', 'L'), ('TH', 'W'), ('ZH'), ('P', 'Y'),
    ('K', 'Y'), ('B', 'Y'), ('F', 'Y'), ('HH', 'Y'), ('V', 'Y'), ('TH', 'Y'),
    ('M', 'Y'), ('S', 'P', 'Y'), ('S', 'K', 'Y'), ('G', 'Y')))


def eng_syllabify(phonemes):
    """Syllabify a sequence of phonemes using max onset and a gold set of onsets."""
    return syllabify(phonemes, ENG_ONSETS, ENG_VOWELS)


def kg_syllabify(phonemes):
    """Syllabify using Kyle Gorman's syllabify module."""
    # Dirty hack import so only this function, not the whole module,
    # depends on syllabify
    if not _IMPORTED_SYLLABIFY:
        from syllabify import syllabify as syllabify2

    return [onset + nucleus + coda for onset, nucleus, coda in
            syllabify2(phonemes)]
