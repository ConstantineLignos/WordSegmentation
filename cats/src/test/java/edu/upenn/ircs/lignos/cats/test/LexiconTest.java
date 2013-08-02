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

import java.util.LinkedList;
import java.util.List;

import edu.upenn.ircs.lignos.cats.Utterance;
import edu.upenn.ircs.lignos.cats.lexicon.Lexicon;
import edu.upenn.ircs.lignos.cats.lexicon.Word;

import junit.framework.TestCase;

public class LexiconTest extends TestCase{
	Utterance pieUtt = new Utterance("I0 like1 pie1", false, false);
	Utterance iUtt = new Utterance("I0", false, false);
	Utterance junk = new Utterance("aldkjasldkja0", false, false);
	String[] i = {"I"};
	Boolean[] iStress = {false};
	String[] like = {"like"};
	Boolean[] likeStress = {true};
	String[] pie = {"pie"};
	Boolean[] pieStress = {true};
	String[] likePie = {"like", "pie"};
	Boolean[] likePieStress = {true, true};
	String[] iLikePie = {"I", "like", "pie"};
	Boolean[] iLikePieStress = {false, true, true};

	
	/**
	 * Test basic functionality
	 */
	public void testgetPrefixWordsBasic() {
		// Build up a lexicon with "I" and "Ilikepie." as words
		Lexicon lex = new Lexicon(true, false, false, false, false, 0.0, 0.0, null);
		lex.rewardWord(i, iStress);
		lex.rewardWord(iLikePie, iLikePieStress);
		
		// Build up the result list
		List<Word> words = new LinkedList<Word>();
		words.add(lex.getWord(i, iStress));
		words.add(lex.getWord(iLikePie, iLikePieStress));
		
		// Check that with the index at the start we get both
		assertEquals(words, lex.getPrefixWords(pieUtt, 0));
		
		// Check that with the index at 1 we get nothing
		words = new LinkedList<Word>();
		assertEquals(words, lex.getPrefixWords(pieUtt, 1));
	}
	
	/**
	 * Test a valid range of indices
	 */
	public void testgetPrefixWordsIndices() {
		// Build up a lexicon with "like", "likepie", and "pie" as words
		Lexicon lex = new Lexicon(true, false, false, false, false, 0.0, 0.0, null);
		lex.rewardWord(like, likeStress);
		lex.rewardWord(likePie, likePieStress);
		lex.rewardWord(pie, pieStress);
		
		// Check that with the index at 0 we get nothing
		List<Word> words = new LinkedList<Word>();
		assertEquals(words, lex.getPrefixWords(pieUtt, 0));
		
		// Check that with the index at 1 we get "like" and "likepie"
		words = new LinkedList<Word>();
		words.add(lex.getWord(like, likeStress));
		words.add(lex.getWord(likePie, likePieStress));
		assertEquals(words, lex.getPrefixWords(pieUtt, 1));
		
		// Check that with the index at 2 we get "pie"
		words = new LinkedList<Word>();
		words.add(lex.getWord(pie, pieStress));
		assertEquals(words, lex.getPrefixWords(pieUtt, 2));
	}
	
	
	/**
	 * Test a one unit utterance
	 */
	public void testgetPrefixWordsOneUnitUtt() {
		// Build up a lexicon with "I" and "Ilikepie." as words
		Lexicon lex = new Lexicon(true, false, false, false, false, 0.0, 0.0, null);
		lex.rewardWord(i, iStress);
		lex.rewardWord(iLikePie, iLikePieStress);
		
		// Build up the result list
		List<Word> words = new LinkedList<Word>();
		words.add(lex.getWord(i, iStress));
		
		// Check that with the index at the start we get "I"
		assertEquals(words, lex.getPrefixWords(iUtt, 0));
		
		// Check that on a garbage utterance we get nothing
		words = new LinkedList<Word>();
		assertEquals(words, lex.getPrefixWords(junk, 0));
	}
	
	
	/**
	 * Test passing a bad index
	 */
	public void testgetPrefixWordsBadIndices() {
		Lexicon lex = new Lexicon(true, false, false, false, false, 0.0, 0.0, null);
		try {
			lex.getPrefixWords(iUtt, -1); 
			fail("Should not allow negative indices");
		}
		catch (RuntimeException e) {}
		try {
			lex.getPrefixWords(iUtt, 1); 
			fail("Should not allow too high indices.");
		}
		catch (RuntimeException e) {}
	}


	/**
	 * Test basic decay functionality
	 */
	public void testDecayBasic() {
		// Make a lexicon with decay
		Lexicon lex = new Lexicon(true, false, false, false, false, 0.0, 0.1, null);
		lex.incUtteranceWords(i, iStress, new Boolean[] {}, null);
		
		// Check the initial score
		double score1 = lex.getScore(lex.getWord(i, iStress), null);
		
		// Get the score after a tick
		lex.tick();
		
		// Score2 should be lower
		double score2 = lex.getScore(lex.getWord(i, iStress), null);
		
		assert(score2 < score1);
	}
	
}
