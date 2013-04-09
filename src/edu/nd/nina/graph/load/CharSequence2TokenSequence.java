/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

/** 
 @author Andrew McCallum <a href="mailto:mccallum@cs.umass.edu">mccallum@cs.umass.edu</a>
 */

package edu.nd.nina.graph.load;

import java.util.regex.Pattern;

import edu.nd.nina.extract.StringSpan;
import edu.nd.nina.extract.StringTokenization;
import edu.nd.nina.types.Instance;
import edu.nd.nina.types.TokenSequence;
import edu.nd.nina.util.CharSequenceLexer;

/**
 * Pipe that tokenizes a character sequence. Expects a CharSequence in the
 * Instance data, and converts the sequence into a token sequence using the
 * given regex or CharSequenceLexer. (The regex / lexer should specify what
 * counts as a token.)
 */
public class CharSequence2TokenSequence extends Pipe {
	CharSequenceLexer lexer;

	public CharSequence2TokenSequence(CharSequenceLexer lexer) {
		this.lexer = lexer;
	}

	public CharSequence2TokenSequence(String regex) {
		this.lexer = new CharSequenceLexer(regex);
	}

	public CharSequence2TokenSequence(Pattern regex) {
		this.lexer = new CharSequenceLexer(regex);
	}

	public CharSequence2TokenSequence() {
		this(new CharSequenceLexer());
	}

	public Instance pipe(Instance carrier) {
		CharSequence string = (CharSequence) carrier.getData();
		lexer.setCharSequence(string);
		TokenSequence ts = new StringTokenization(string);
		while (lexer.hasNext()) {
			lexer.next();
			ts.add(new StringSpan(string, lexer.getStartOffset(), lexer
					.getEndOffset()));
		}
		carrier.setData(ts);
		return carrier;
	}

}
