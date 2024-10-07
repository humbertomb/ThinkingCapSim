/*
 * @(#)SyntaxError.java		1.0 2003/12/15
 * 
 * (c) 2003 Denis Remondini
 *
 */
package tclib.behaviours.fhb.exceptions;

/**
 * This is a simple implementation of the syntax error exception.
 * 
 * @version	1.0		15 Dic 2003
 * @author 	Denis Remondini
 */
public class SyntaxError extends Exception {
	
	public SyntaxError( String s ) {
		super(s);
	}
	
}
