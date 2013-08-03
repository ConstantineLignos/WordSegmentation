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
	public final double precision;
	public final double recall;
	public final double fScore;
	public final double hitRate;
	public final double faRate;
	public final double aPrime;
	
	private Result(double precision, double recall, double fScore, double hitRate, double faRate, double aPrime) {
		this.precision = precision;
		this.recall = recall;
		this.fScore = fScore;
		this.hitRate = hitRate;
		this.faRate = faRate;
		this.aPrime = aPrime;
	}
	
	public static Result calcResult(int truePositives, int falsePositives,
			int falseNegatives, int trueNegatives) {
		double precision = truePositives / (float) (truePositives + falsePositives);
		double recall = truePositives / (float) (truePositives + falseNegatives);
		double fScore = 2 * ((precision * recall) / (precision + recall));
		double hitRate = recall; // HR and recall are the same measurement
		double faRate = falsePositives / (float) (trueNegatives + falsePositives);
		double aPrime = .5 + (((hitRate - faRate) * (1 + hitRate - faRate)) / (4 * hitRate * ( 1 - faRate)));

		
		// Correct for any NaNs by changing to zero
		precision = Double.isNaN(precision) ? 0.0 : precision;
		recall = Double.isNaN(recall) ? 0.0 : recall;
		fScore = Double.isNaN(fScore) ? 0.0 : fScore;
		aPrime = Double.isNaN(aPrime) || Double.isInfinite(aPrime) ? 0.0 : aPrime;
		
		Result r = new Result(precision, recall, fScore, hitRate, faRate, aPrime);
		return r;
	}
	
	public String toString() {
		return String.format("Precision: %4f, Recall: %4f, F-Score: %4f\nHit Rate: %.4f, FA Rate: %.4f, A': %.4f",
				precision, recall, fScore, hitRate, faRate, aPrime);
	}
	
	public String toStringPRF() {
		return String.format("Precision: %4f, Recall: %4f, F-Score: %4f",
				precision, recall, fScore, hitRate, faRate, aPrime);
	}
	
	public String toCSVString() {
		return String.format("%4f,%4f,%4f,%4f,%4f,%4f", precision, recall, fScore, hitRate, faRate,
				aPrime);
	}
}