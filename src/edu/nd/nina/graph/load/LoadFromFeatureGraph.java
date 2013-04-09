package edu.nd.nina.graph.load;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import edu.nd.nina.util.CharSequenceLexer;

public class LoadFromFeatureGraph {
	// ---------------------------------------------

		// If true, do not force all strings to lowercase.
		private static final Boolean preserveCase = false;
		// If true, remove a default list of common English \"stop words\" from
		// the text.
		private static final Boolean removeStopWords = false;

		// Instead of the default list, read stop words from a file, one per
		// line. Implies --remove-stopwords
		private static final File stoplistFile = null;

		// Read whitespace-separated words from this file, and add them to
		// either the default English stoplist or the list specified by
		// --stoplist-file.
		private static final File extraStopwordsFile = null;

		// If true, remove text occurring inside <...>, as in HTML or SGML.
		private static final Boolean skipHtml = false;

		// If true, features will be binary.
		private static final Boolean binaryFeatures = false;

		// Include among the features all n-grams of sizes specified. For
		// example, to get all unigrams and bigrams, use --gram-sizes 1,2. This
		// option occurs after the removal of stop words, if removed.
		private static final int[] gramSizes = { 1 };

		// If true, final data will be a FeatureSequence rather than a
		// FeatureVector.
		private static final Boolean keepSequence = false;

		// If true, final data will be a FeatureSequenceWithBigrams rather than
		// a FeatureVector.
		private static final Boolean keepSequenceBigrams = true;

		// Character encoding for input file
		private static final String encoding = Charset.defaultCharset()
				.displayName();

		// "Regular expression used for tokenization. Example:
		// \"[\\p{L}\\p{N}_]+|[\\p{P}]+\" (unicode letters, numbers and
		// underscore OR all punctuation)
		private static final String tokenRegex = CharSequenceLexer.LEX_ALPHA
				.toString();

		// If true, print a representation of the processed data to standard
		// output. This option is intended for debugging.
		private static final Boolean printOutput = false;
		
		public static Pipe createPipe() {
			// Build a new pipe

			// Create a list of pipes that will be added to a SerialPipes object
			// later
			ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

			// Convert the "target" object into a numeric index
			// into a LabelAlphabet.
			pipeList.add(new Target2Label());

			// The "data" field is currently a filename. Save it as "source".
			// pipeList.add(new SaveDataInSource());

			// Set "data" to the file's contents. "data" is now a String.
			pipeList.add(new Input2CharSequence(encoding));

			// Remove HTML tags. Suitable for SGML and XML.
			if (skipHtml) {
				pipeList.add(new CharSequenceRemoveHTML());
			}

			//
			// Tokenize the input: first compile the tokenization pattern
			//

			Pattern tokenPattern = null;

			if (keepSequenceBigrams) {
				// We do not want to record bigrams across punctuation,
				// so we need to keep non-word tokens.
				tokenPattern = CharSequenceLexer.LEX_NONWHITESPACE_CLASSES;
			} else {
				// Otherwise, try to compile the regular expression pattern.

				try {
					tokenPattern = Pattern.compile(tokenRegex);
				} catch (PatternSyntaxException pse) {
					throw new IllegalArgumentException(
							"The token regular expression (" + tokenRegex
									+ ") was invalid: " + pse.getMessage());
				}
			}

			// Add the tokenizer
			pipeList.add(new CharSequence2TokenSequence(tokenPattern));

			
			// The first token is the ID - add it to the source
			pipeList.add(new IDToSource());
			
			if (!preserveCase) {
				pipeList.add(new TokenSequenceLowercase());
			}

			

			if (keepSequenceBigrams) {
				// Remove non-word tokens, but record the fact that they
				// were there.
				pipeList.add(new TokenSequenceRemoveNonAlpha(true));
			}

			// Stopword removal.

			if (stoplistFile != null) {

				// The user specified a new list

				TokenSequenceRemoveStopwords stopwordFilter = new TokenSequenceRemoveStopwords(
						stoplistFile, encoding, false, // don't include default list
						false, keepSequenceBigrams);

				if (extraStopwordsFile != null) {
					stopwordFilter.addStopWords(extraStopwordsFile);
				}

				pipeList.add(stopwordFilter);
			} else if (removeStopWords) {

				// The user did not specify a new list, so use the default
				// built-in English list, possibly adding extra words.

				TokenSequenceRemoveStopwords stopwordFilter = new TokenSequenceRemoveStopwords(
						false, keepSequenceBigrams);

				if (extraStopwordsFile != null) {
					stopwordFilter.addStopWords(extraStopwordsFile);
				}

				pipeList.add(stopwordFilter);

			}

			// gramSizes is an integer array, with default value [1].
			// Check if we have a non-default value.
			if (!(gramSizes.length == 1 && gramSizes[0] == 1)) {
				pipeList.add(new TokenSequenceNGrams(gramSizes));
			}

			// So far we have a sequence of Token objects that contain
			// String values. Look these up in an alphabet and store integer IDs
			// ("features") instead of Strings.
			if (keepSequenceBigrams) {
				pipeList.add(new TokenSequence2FeatureSequenceWithBigrams());
			} else {
				pipeList.add(new TokenSequence2FeatureSequence());
			}

			// For many applications, we do not need to preserve the sequence of
			// features,
			// only the number of times times a feature occurs.
			if (!(keepSequence || keepSequenceBigrams)) {
				pipeList.add(new FeatureSequence2AugmentableFeatureVector(
						binaryFeatures));
			}

			if (printOutput) {
				pipeList.add(new PrintInputAndTarget());
			}

			return new SerialPipes(pipeList);
		}
}
