/* Copyright (C) 2005 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

package edu.nd.nina.types;


/**
 * A FeatureSequence with a parallel record of bigrams, kept in a separate
 * dictionary
 * 
 * @author <a href="mailto:mccallum@cs.umass.edu">Andrew McCallum</a>
 */

public class FeatureSequenceWithBigrams extends FeatureSequence {
	public final static String deletionMark = "NextTokenDeleted";
	Alphabet biDictionary;
	int[] biFeatures;

	public FeatureSequenceWithBigrams(Alphabet dict, Alphabet bigramDictionary,
			TokenSequence ts) {
		super(dict, ts.size());
		int len = ts.size();
		this.biDictionary = bigramDictionary;
		this.biFeatures = new int[len];
		Token t, pt = null;
		for (int i = 0; i < len; i++) {
			t = ts.get(i);
			super.add(t.getText());
			if (pt != null && pt.getProperty(deletionMark) == null)
				biFeatures[i] = biDictionary == null ? 0 : biDictionary
						.lookupIndex(pt.getText() + "_" + t.getText(), true);
			else
				biFeatures[i] = -1;
			pt = t;
		}
	}

	public Alphabet getBiAlphabet() {
		return biDictionary;
	}

	public final int getBiIndexAtPosition(int pos) {
		return biFeatures[pos];
	}

	public Object getObjectAtPosition(int pos) {
		return biFeatures[pos] == -1 ? null : (biDictionary == null ? null
				: biDictionary.lookupObject(biFeatures[pos]));
	}
}
