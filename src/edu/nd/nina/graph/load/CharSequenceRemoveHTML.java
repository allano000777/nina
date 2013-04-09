/* Copyright (C) 2006 Univ. of Massachusetts Amherst, Computer Science Dept.
 This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
 http://www.cs.umass.edu/~mccallum/mallet 
 This software is provided under the terms of the Common Public License,
 version 1.0, as published by http://www.opensource.org.  For further
 information, see the file `LICENSE' included with this distribution. */

package edu.nd.nina.graph.load;

import org.jsoup.Jsoup;

import edu.nd.nina.types.Instance;

/**
 * This pipe removes HTML from a CharSequence. The HTML is actually parsed here,
 * so we should have less HTML slipping through... but it is almost certainly
 * much slower than a regular expression, and could fail on broken HTML.
 * 
 * @author Greg Druck <a
 *         href="mailto:gdruck@cs.umass.edu">gdruck@cs.umass.edu</a>
 */

public class CharSequenceRemoveHTML extends Pipe {

	public Instance pipe(Instance carrier) {
		String text = ((CharSequence) carrier.getData()).toString();

		String result = Jsoup.parse(text).text();
		result = result.toLowerCase();

		carrier.setData((CharSequence) result);
		return carrier;
	}

}
