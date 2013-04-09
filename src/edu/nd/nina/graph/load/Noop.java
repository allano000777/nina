/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

package edu.nd.nina.graph.load;

import edu.nd.nina.types.Alphabet;
import edu.nd.nina.types.Instance;

/**
 * A pipe that does nothing to the instance fields but which has side effects on
 * the dictionary.
 * 
 * @author Andrew McCallum <a
 *         href="mailto:mccallum@cs.umass.edu">mccallum@cs.umass.edu</a>
 */

public class Noop extends Pipe {
	public Noop() {
	}

	/**
	 * Pass through input without change, but force the creation of Alphabet's,
	 * so it can be shared by future DictionariedPipe's.
	 * 
	 * You might want to use this before ParallelPipes where the previous pipes
	 * do not need dictionaries, but later steps in each parallel path do, and
	 * they all must share the same dictionary.
	 */

	public Noop(Alphabet dataDict, Alphabet targetDict) {
		super(dataDict, targetDict);
	}

	public Instance pipe(Instance carrier) {
		return carrier;
	}

}
