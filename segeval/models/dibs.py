"""
An implementation of the DiBS algorithm given in:
Daland, R., & Pierrehumbert, J. B. (2011). Learning Diphone-Based Segmentation.
Cognitive Science, 35(1), 119-155.

"""

# Copyright (C) 2013 Constantine Lignos
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

from segeval.models.diphone import DiphoneSegmenter


class DiBSSegmenter(DiphoneSegmenter):

    def classify_diphone(self, diphone):
        """Classify a diphone as a word boundary or not."""
        # If the diphone has never been seen, call it a word boundary
        if diphone not in self.diphone_freqs[diphone]:
            return True

        # Estimate P(x|inital) and P(y|final) for a diphone xy
        phone1, phone2 = diphone
        p_phone1_final = self.final_freqs[phone1] if phone1 in self.final_freqs else 0.0
        p_phone2_init = self.final_freqs[phone2] if phone2 in self.final_freqs else 0.0

        # Compute the DiBS score
        dibs_score = ((p_phone1_final * p_phone2_init * self.p_boundary) /
                      self.diphone_freqs[diphone])
        return dibs_score > 0.5
