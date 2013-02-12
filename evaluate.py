"""
Tools for evaluating word segmenters.

Constantine Lignos, February 2013

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


from corpus import Corpus
from segeval.models.diphone import DiphoneSegmenter


def main():
    """Evaluate a segmentation."""
    # TODO: Right now this is just plugged in to the diphone segmenter

if __name__ == "__main__":
    main()
