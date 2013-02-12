#!/usr/bin/env python
"""Download segmentation data."""

# Copyright (C) 2013 Constantine Lignos
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

import os

from datamanager import download, unzip


ROOT_DIR = os.path.dirname(os.path.abspath(__file__))
BRENT_URL = "http://childes.psy.cmu.edu/derived/brent_ratner.zip"
FILENAME = os.path.join(ROOT_DIR, "brent_ratner.zip")


download(BRENT_URL, FILENAME)
unzip(FILENAME, ROOT_DIR)
