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

package edu.upenn.ircs.lignos.cats.test;

import java.util.Arrays;

import edu.upenn.ircs.lignos.cats.Utterance;
import edu.upenn.ircs.lignos.cats.metrics.Evaluation;
import junit.framework.TestCase;

public class EvaluationTest extends TestCase{
	String[] doggieSent1 = {"the", "do", "ggie", "ran"};
	Boolean[] doggieBound1 = {true, false, true};
	
	String[] doggieSent2 = {"do", "ggies", "run"};
	Boolean[] doggieBound2 = {false, true};
	
	/*
	 * Test basic word extraction
	 */
	public void testwordsPredicted() {
		String[] doggie1Words = {"the", "do|ggie", null, "ran"};
		Utterance doggie1Utt = new Utterance(doggieSent1, null, doggieBound1);
		assertTrue(Arrays.equals(Evaluation.wordsPredicted(doggie1Utt), doggie1Words));
		
		String[] doggie2Words = {"do|ggies", null, "run"};
		Utterance doggie2Utt = new Utterance(doggieSent2, null, doggieBound2);
		assertTrue(Arrays.equals(Evaluation.wordsPredicted(doggie2Utt), doggie2Words));
	}
}
