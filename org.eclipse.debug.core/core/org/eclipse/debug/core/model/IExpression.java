package org.eclipse.debug.core.model;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
 
/**
 * An expression is a snippet of code that can be evaluated
 * to produce a value. When and how an experssion is evaluated
 * is implementation specific. The context/binding required to
 * evaluate an expression varies by debug model, and by
 * user intent. Furthermore, an expression may need to be evaluated
 * at a specific location in a program (for example, at a
 * breakpoint/line where certain variables referenced in the
 * expression are visible/allocated). A user may want to
 * evaluate an expression once to produce a value than can
 * be inspected iteratively, or they may wish to evaluate an
 * expression iteratively producing new values each time
 * (i.e. as in a watch list). 
 * <p>
 * Clients are intended to implement this interface.
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @since 2.0
 */
public interface IExpression extends IDebugElement {
	
	/**
	 * Returns this expression's snippet of code.
	 * 
	 * @return the expression
	 */
	public abstract String getExpressionText();

	/**
	 * Returns the current value of this expression or
	 * <code>code</code> null if this expression does not
	 * currently have a value.
	 * 
	 * @return value or <code>null</code>
	 */
	public abstract IValue getValue();
	
	/**
	 * Returns the debug target this expression is associated
	 * with, or <code>null</code> if this expression is not
	 * associated with a debug target.
	 * 
	 * @return debug target or <code>null</code>
	 * @see IDebugElement#getDebugTarget()
	 */
	public abstract IDebugTarget getDebugTarget();
	
	/**
	 * Notifies this expression that it has been removed
	 * from the expression manager. Any required clean up
	 * is be performed such that this expression can be
	 * garbage collected.
	 */
	public abstract void dispose();
}
