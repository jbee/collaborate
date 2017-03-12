package vizio.model;

@UseCode
public enum Status {

	/**
	 * The problem is not concluded yet.
	 */
	unsolved, 
	
	/*
	 * concluded
	 */
	
	/**
	 * Problem isn't worth looking into (ignored).
	 */
	absolved, 
	/**
	 * Problem tracked down and "fixed".
	 */
	resolved, 
	/**
	 * System redesigned to not have the (kind of) problem any more.
	 */
	dissolved;
}
