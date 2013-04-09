/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

/** 
 @author Andrew McCallum <a href="mailto:mccallum@cs.umass.edu">mccallum@cs.umass.edu</a>
 */

package edu.nd.nina.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CharSequenceLexer implements Lexer {
	// Some predefined lexing rules
	public static final Pattern LEX_ALPHA = Pattern.compile("\\p{Alpha}+");
	public static final Pattern LEX_WORDS = Pattern.compile("\\w+");
	public static final Pattern LEX_NONWHITESPACE_TOGETHER = Pattern
			.compile("\\S+");
	public static final Pattern LEX_WORD_CLASSES = Pattern
			.compile("\\p{Alpha}+|\\p{Digit}+");
	public static final Pattern LEX_NONWHITESPACE_CLASSES = Pattern
			.compile("\\p{Alpha}+|\\p{Digit}+|\\p{Punct}");

	// Lowercase letters and uppercase letters
	public static final Pattern UNICODE_LETTERS = Pattern
			.compile("[\\p{Ll}&&\\p{Lu}]+");

	Pattern regex;
	Matcher matcher = null;
	CharSequence input;
	String matchText;
	boolean matchTextFresh;

	public CharSequenceLexer() {
		this(LEX_ALPHA);
	}

	public CharSequenceLexer(Pattern regex) {
		this.regex = regex;
		setCharSequence(null);
	}

	public CharSequenceLexer(String regex) {
		this(Pattern.compile(regex));
	}

	public CharSequenceLexer(CharSequence input, Pattern regex) {
		this(regex);
		setCharSequence(input);
	}

	public CharSequenceLexer(CharSequence input, String regex) {
		this(input, Pattern.compile(regex));
	}

	public void setCharSequence(CharSequence input) {
		this.input = input;
		this.matchText = null;
		this.matchTextFresh = false;
		if (input != null)
			this.matcher = regex.matcher(input);
	}

	public CharSequence getCharSequence() {
		return input;
	}

	public String getPattern() {
		return regex.pattern();
	}

	public void setPattern(String reg)// added by Fuchun
	{
		if (!regex.equals(getPattern())) {
			this.regex = Pattern.compile(reg);
			// this.matcher = regex.matcher(input);
		}
	}

	public int getStartOffset() {
		if (matchText == null)
			return -1;
		return matcher.start();
	}

	public int getEndOffset() {
		if (matchText == null)
			return -1;
		return matcher.end();
	}

	public String getTokenString() {
		return matchText;
	}

	// Iterator interface methods

	private void updateMatchText() {
		if (matcher != null && matcher.find()) {
			matchText = matcher.group();
			if (matchText.length() == 0) {
				// xxx Why would this happen?
				// It is happening to me when I use the regex ".*" in an attempt
				// to make
				// Token's out of entire lines of text. -akm.
				updateMatchText();
				// System.err.println ("Match text is empty!");
			}
			// matchText = input.subSequence (matcher.start(),
			// matcher.end()).toString ();
		} else
			matchText = null;
		matchTextFresh = true;
	}

	public boolean hasNext() {
		if (!matchTextFresh)
			updateMatchText();
		return (matchText != null);
	}

	public String next() {
		if (!matchTextFresh)
			updateMatchText();
		matchTextFresh = false;
		return matchText;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

}
