/*
 * @(#) ElementNotFound.java		1.0 2003/12/07
 * 
 * (c) 2003 Denis Remondini
 *
 */
package tclib.behaviours.fhb.exceptions;

/**
 * This is a simple implementation of the element not found exception.
 * 
 * @version	1.0		07 Dic 2003
 * @author 	Denis Remondini
 */
public class ElementNotFound extends Exception {

	private String elementName;
	
	public ElementNotFound(String element) {
		elementName = element;
	}
	
	public String getElementName() {
		return elementName;
	}
}
