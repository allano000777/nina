/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

package edu.nd.nina.graph.load;

import edu.nd.nina.types.Instance;
import edu.nd.nina.types.TokenSequence;

/**
 * Set the source field of each instance to its data field.
 * 
 * @author Andrew McCallum <a
 *         href="mailto:mccallum@cs.umass.edu">mccallum@cs.umass.edu</a>
 */

public class NameToName extends Pipe {
	public NameToName() {
	}

	public Instance pipe(Instance carrier) {
		String s = ((String)carrier.getData());
		int f = ((String)carrier.getData()).indexOf(" "); 
		String ts = s.substring(f, s.indexOf(" ", f+1)).trim();
		// the first token is the ID;
		carrier.setName(ts);
		return carrier;
	}

}
