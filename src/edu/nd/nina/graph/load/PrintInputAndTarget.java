/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

package edu.nd.nina.graph.load;

import edu.nd.nina.types.Instance;

/**
 * Print the data and target fields of each instance.
 * 
 * @author Andrew McCallum <a
 *         href="mailto:mccallum@cs.umass.edu">mccallum@cs.umass.edu</a>
 */

public class PrintInputAndTarget extends Pipe {
	String prefix = null;

	public PrintInputAndTarget(String prefix) {
		this.prefix = prefix;
	}

	public PrintInputAndTarget() {
	}

	public Instance pipe(Instance carrier) {
		if (prefix != null)
			System.out.print(prefix);
		String targetString = "<null>";
		// Swapping order, since data often has a newline at the end -DM
		if (carrier.getTarget() != null)
			targetString = carrier.getTarget().toString();
		System.out.println("name: " + carrier.getName() + "\ntarget: "
				+ targetString + "\ninput: " + carrier.getData());
		return carrier;
	}

}
