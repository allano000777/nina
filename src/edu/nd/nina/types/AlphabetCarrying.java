package edu.nd.nina.types;

import java.util.List;

/**
 * An interface for objects that contain one or more Alphabets.
 * <p>
 * The primary kind of type checking among MALLET objects such as Instances,
 * InstanceLists, Classifiers, etc is by checking that their Alphabets match.
 */
public interface AlphabetCarrying {
	Alphabet getAlphabet();

	List<Alphabet> getAlphabets();
}
