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

package edu.upenn.ircs.lignos.cats.metrics;

public class Result {
	double precision;
	double recall;
	double fScore;
	
	private Result(double precision, double recall, double fScore) {
		this.precision = precision;
		this.recall = recall;
		this.fScore = fScore;
	}
	
	public static Result calcResult(int truePositives, int falsePositives,
			int falseNegatives) {
		double precision = ((float) truePositives)/(truePositives + falsePositives);
		double recall = ((float) truePositives)/(truePositives +falseNegatives);
		double fScore = 2 * (precision * recall) / (precision + recall);
		
		// Correct for any NaNs by changing to zero
		precision = Double.isNaN(precision) ? 0.0 : precision;
		recall = Double.isNaN(recall) ? 0.0 : recall;
		fScore = Double.isNaN(fScore) ? 0.0 : fScore;
		
		Result r = new Result(precision, recall, fScore);
		return r;
	}
	
	public String toString() {
		return String.format("Precision: %4f, Recall: %4f, F-Score: %4f",
				precision, recall, fScore);
	}
	
	public String toCSVString() {
		return String.format("%4f,%4f,%4f", precision, recall, fScore);
	}
}