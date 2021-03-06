/*
 Copyright (C) 2010, 2011 Constantine Lignos

 This file is a part of CATS.

 CATS is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 CATS is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with CATS.  If not, see <http://www.gnu.org/licenses/>.
*/

package edu.upenn.ircs.lignos.cats.segmenters;

import edu.upenn.ircs.lignos.cats.Utterance;
class SegResult {
	public Boolean[] segmentation;
	public int index;
	public boolean seenStress;
	public boolean[] trusts;


	public SegResult (Boolean[] segmentation, int index,
			boolean seenStress, boolean[] trusts) {
		this.segmentation = segmentation;
		this.index = index;
		this.seenStress = seenStress;
		this.trusts = trusts;
	}


	public String toString() {
		return " i: " + index + " stress: " + seenStress;
	}


	public String toPrettyString(Utterance utt) {
		return Utterance.makeSegText(utt.getUnits(), utt.getStresses(), segmentation) +
		" i: " + index + " stress: " + seenStress;
	}


	public static void recycleSegResult(Boolean[] segmentation, int index,
			boolean seenStress, boolean[] trusts, SegResult result) {
		result.segmentation = segmentation;
		result.index = index;
		result.seenStress = seenStress;
		result.trusts = trusts;
	}
}