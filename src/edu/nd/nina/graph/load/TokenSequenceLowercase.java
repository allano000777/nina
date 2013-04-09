/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

package edu.nd.nina.graph.load;

import edu.nd.nina.types.Instance;
import edu.nd.nina.types.Token;
import edu.nd.nina.types.TokenSequence;

/**
 * Convert the text in each token in the token sequence in the data field to
 * lower case.
 * 
 * @author Andrew McCallum <a
 *         href="mailto:mccallum@cs.umass.edu">mccallum@cs.umass.edu</a>
 */

public class TokenSequenceLowercase extends Pipe {

	public Instance pipe(Instance carrier) {
		TokenSequence ts = (TokenSequence) carrier.getData();
		for (int i = 0; i < ts.size(); i++) {
			Token t = ts.get(i);
			t.setText(t.getText().toLowerCase());
		}
		return carrier;
	}

}
