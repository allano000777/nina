package edu.nd.nina.math;

public class ReferenceCount {
	private int Refs;

	public ReferenceCount() {
		Refs = 0;
	}

	private ReferenceCount set(final ReferenceCount TCRef) {
		return TCRef;
	}

	public void MkRef() {
		Refs++;
	}

	public void UnRef() {
		assert (Refs > 0);
		Refs--;
	}

	public boolean NoRef() {
		return Refs == 0;
	}

	public int GetRefs() {
		return Refs;
	}
}
