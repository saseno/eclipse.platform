package org.eclipse.debug.core.model;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
import org.eclipse.core.runtime.CoreException;

/**
 * A breakpoint that can be located at a specific line of source code.
 */
public interface ILineBreakpoint extends IBreakpoint {

/**
 * Returns the line number in the original source that corresponds
 * to the location of this breakpoint, or -1 if the attribute is not
 * present.
 *
 * @return this breakpoint's line number, or -1 if unknown
 * @exception CoreException if a <code>CoreException</code> is thrown
 * 	while accessing the underlying <code>IMarker.LINE_NUMBER</code> marker attribute
 */
public int getLineNumber() throws CoreException;
/**
 * Returns starting source index in the original source that corresponds
 * to the location of this breakpoint, or -1 if the attribute is not present.
 *
 * @return this breakpoint's char start value, or -1 if unknown
 * @exception CoreException if a <code>CoreException</code> is thrown
 * 	while accessing the underlying <code>IMarker.CHAR_START</code> marker attribute
 */
public int getCharStart() throws CoreException;
/**
 * Returns ending source index in the original source that corresponds
 * to the location of this breakpoint, or -1 if the attribute is not present.
 *
 * @return this breakpoint's char end value, or -1 if unknown
 * @exception CoreException if a <code>CoreException</code> is thrown
 * 	while accessing the underlying <code>IMarker.CHAR_END</code> marker attribute
 */
public int getCharEnd() throws CoreException;
}

