/*
 * @(#)LexicalError.java		1.0 2003/12/15
 * 
 * (c) 2003 Denis Remondini
 *
 */
package tclib.behaviours.fhb.exceptions;

/**
 * This is a simple implementation of the lexical error exception.
 * 
 * @version	1.0		15 Dic 2003
 * @author 	Denis Remondini
 */
public class LexicalError extends Exception {
	
	public LexicalError( String s ) {
		super(s);
	}
	
}