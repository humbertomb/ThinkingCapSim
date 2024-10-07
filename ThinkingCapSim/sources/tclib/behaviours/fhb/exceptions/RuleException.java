/*
 * @(#)RuleException.java		1.0 2003/12/16
 * 
 * (c) 2003 Denis Remondini
 *
 */
package tclib.behaviours.fhb.exceptions;

/**
 * This is a simple implementation of the rule exception.
 * 
 * @version	1.0		16 Dic 2003
 * @author 	Denis Remondini
 */
public class RuleException extends Exception {
	
	public RuleException(String msg) {
		super(msg);
	}
	
}