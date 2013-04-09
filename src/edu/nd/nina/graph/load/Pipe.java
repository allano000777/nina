/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */

package edu.nd.nina.graph.load;

import java.rmi.dgc.VMID;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.nd.nina.types.Alphabet;
import edu.nd.nina.types.AlphabetCarrying;
import edu.nd.nina.types.Instance;

/**
 * The abstract superclass of all Pipes, which transform one data type to
 * another. Pipes are most often used for feature extraction.
 * <p>
 * Although Pipe does not have any "abstract methods", in order to use a Pipe
 * subclass you must override either the {@link pipe} method or the
 * {@link newIteratorFrom} method. The former is appropriate when the pipe's
 * processing of an Instance is strictly one-to-one. For every Instance coming
 * in, there is exactly one Instance coming out. The later is appropriate when
 * the pipe's processing may result in more or fewer Instances than arrive
 * through its source iterator.
 * <p>
 * A pipe operates on an {@link cc.mallet.types.Instance}, which is a carrier of
 * data. A pipe reads from and writes to fields in the Instance when it is
 * requested to process the instance. It is up to the pipe which fields in the
 * Instance it reads from and writes to, but usually a pipe will read its input
 * from and write its output to the "data" field of an instance.
 * <p>
 * A pipe doesn't have any direct notion of input or output - it merely modifies
 * instances that are handed to it. A set of helper classes, which implement the
 * interface {@link Iterator<Instance>}, iterate over commonly encountered input
 * data structures and feed the elements of these data structures to a pipe as
 * instances.
 * <p>
 * A pipe is frequently used in conjunction with an
 * {@link cc.mallet.types.InstanceList} As instances are added to the list, they
 * are processed by the pipe associated with the instance list and the processed
 * Instance is kept in the list.
 * <p>
 * In one common usage, a {@link cc.mallet.pipe.iterator.FileIterator} is given
 * a list of directories to operate over. The FileIterator walks through each
 * directory, creating an instance for each file and putting the data from the
 * file in the data field of the instance. The directory of the file is stored
 * in the target field of the instance. The FileIterator feeds instances to an
 * InstanceList, which processes the instances through its associated pipe and
 * keeps the results.
 * <p>
 * Pipes can be hierachically composed. In a typical usage, a SerialPipe is
 * created, which holds other pipes in an ordered list. Piping an instance
 * through a SerialPipe means piping the instance through each of the child
 * pipes in sequence.
 * <p>
 * A pipe holds two separate Alphabets: one for the symbols (feature names)
 * encountered in the data fields of the instances processed through the pipe,
 * and one for the symbols (e.g. class labels) encountered in the target fields.
 * <p>
 * 
 * @author Andrew McCallum <a
 *         href="mailto:mccallum@cs.umass.edu">mccallum@cs.umass.edu</a>
 */

public abstract class Pipe implements AlphabetCarrying {

	Alphabet dataAlphabet = null;
	Alphabet targetAlphabet = null;

	boolean dataAlphabetResolved = false;
	boolean targetAlphabetResolved = false;
	boolean targetProcessing = true;

	VMID instanceId = new VMID(); // used in readResolve to distinguish
									// persistent instances

	/**
	 * Construct a pipe with no data and target dictionaries
	 */
	public Pipe() {
		this(null, null);
	}

	/**
	 * Construct pipe with data and target dictionaries. Note that, since the
	 * default values of the dataDictClass and targetDictClass are null, that if
	 * you specify null for one of the arguments here, this pipe step will not
	 * ever create any corresponding dictionary for the argument.
	 * 
	 * @param dataDict
	 *            Alphabet that will be used as the data dictionary.
	 * @param targetDict
	 *            Alphabet that will be used as the target dictionary.
	 */
	public Pipe(Alphabet dataDict, Alphabet targetDict) {
		this.dataAlphabet = dataDict;
		this.targetAlphabet = targetDict;
	}

	// TODO Really this should be 'protected', but isn't for historical reasons.
	/** Really this should be 'protected', but isn't for historical reasons. */
	public Instance pipe(Instance inst) {
		throw new UnsupportedOperationException(
				"Pipes of class "
						+ this.getClass().getName()
						+ " do not guarantee one-to-one mapping of Instances.  Use 'newIteratorFrom' method instead.");
	}

	/**
	 * Set whether input is taken from target field of instance during
	 * processing. If argument is false, don't expect to find input material for
	 * the target. By default, this is true.
	 */
	public void setTargetProcessing(boolean lookForAndProcessTarget) {
		targetProcessing = lookForAndProcessTarget;
	}

	/**
	 * Return true iff this pipe expects and processes information in the
	 * <tt>target</tt> slot.
	 */
	public boolean isTargetProcessing() {
		return targetProcessing;
	}
	
	/**
	 * Given an InstanceIterator, return a new InstanceIterator whose instances
	 * have also been processed by this pipe. If you override this method, be
	 * sure to check and obey this pipe's {@link skipIfFalse(Instance)} method.
	 */
	public Iterator<Instance> newIteratorFrom(Iterator<Instance> source) {
		return new SimplePipeInstanceIterator(source);
	}

	// If this Pipe produces objects that use a Alphabet, this
	// method returns that dictionary. Even if this particular Pipe
	// doesn't use a Alphabet it may return non-null if
	// objects passing through it use a dictionary.

	// This method should not be called until the dictionary is really
	// needed, because it may set off a chain of events that "resolve"
	// the dictionaries of an entire pipeline, and generally this
	// resolution should not take place until the pipeline is completely
	// in place, and pipe() is being called.
	// xxx Perhaps desire to wait until pipe() is being called is unrealistic
	// and unnecessary.

	public Alphabet getDataAlphabet() {
		return dataAlphabet;
	}

	public Alphabet getTargetAlphabet() {
		return targetAlphabet;
	}

	public Alphabet getAlphabet() {
		return getDataAlphabet();
	}

	public List<Alphabet> getAlphabets() {
		List<Alphabet> x = new ArrayList<Alphabet>();
		x.add(getDataAlphabet());
		x.add(getTargetAlphabet());
		return x;
	}

	public boolean alphabetsMatch(AlphabetCarrying object) {
		List<Alphabet> oas = object.getAlphabets();
		return oas.size() == 2 && oas.get(0).equals(getDataAlphabet())
				&& oas.get(1).equals(getTargetAlphabet());
	}

	public void setDataAlphabet(Alphabet dDict) {
		if (dataAlphabet != null && dataAlphabet.size() > 0)
			throw new IllegalStateException(
					"Can't set this Pipe's Data  Alphabet; it already has one.");
		dataAlphabet = dDict;
	}

	public boolean isDataAlphabetSet() {
		if (dataAlphabet != null && dataAlphabet.size() > 0)
			return true;
		return false;
	}

	public void setOrCheckDataAlphabet(Alphabet a) {
		if (dataAlphabet == null)
			dataAlphabet = a;
		else if (!dataAlphabet.equals(a))
			throw new IllegalStateException("Data alphabets do not match");
	}

	public void setTargetAlphabet(Alphabet tDict) {
		if (targetAlphabet != null)
			throw new IllegalStateException(
					"Can't set this Pipe's Target Alphabet; it already has one.");
		targetAlphabet = tDict;
	}

	public void setOrCheckTargetAlphabet(Alphabet a) {
		if (targetAlphabet == null)
			targetAlphabet = a;
		else if (!targetAlphabet.equals(a))
			throw new IllegalStateException("Target alphabets do not match");
	}

	protected void preceedingPipeDataAlphabetNotification(Alphabet a) {
		if (dataAlphabet == null)
			dataAlphabet = a;
	}

	protected void preceedingPipeTargetAlphabetNotification(Alphabet a) {
		if (targetAlphabet == null)
			targetAlphabet = a;
	}

	public VMID getInstanceId() {
		return instanceId;
	} // for debugging

	// The InstanceIterator used to implement the one-to-one pipe() method
	// behavior.
	private class SimplePipeInstanceIterator implements Iterator<Instance> {
		Iterator<Instance> source;

		public SimplePipeInstanceIterator(Iterator<Instance> source) {
			this.source = source;
		}

		public boolean hasNext() {
			return source.hasNext();
		}

		public Instance next() {
			Instance input = source.next();

			return pipe(input);
		}

		/**
		 * Return the @link{Pipe} that processes @link{Instance}s going through
		 * this iterator.
		 */
		public Pipe getPipe() {
			return null;
		}

		public Iterator<Instance> getSourceIterator() {
			return source;
		}

		public void remove() {
			throw new IllegalStateException("Not supported.");
		}
	}

}
