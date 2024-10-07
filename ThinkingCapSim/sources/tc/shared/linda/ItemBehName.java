/*
 * @(#)ItemBehName.java		1.0 2004/01/22
 * 
 * (c) 2004 Denis Remondini
 *
 */
package tc.shared.linda;

import java.io.Serializable;

/**
 * This class implements a specific item that contains a behaviour name
 * 
 * @version	1.0		22 Jan 2004
 * @author 	Denis Remondini
 */
public class ItemBehName extends Item implements Serializable {

	/* The behaviour name */
	private String behName;
	
	public ItemBehName() {
		behName = null;
	}
	
	/**
	 * Sets the behaviour name
	 * @param name the behaviour name
	 */
	public void set(String name) {
		behName = name;
	}
	
	/**
	 * Returns the behaviour name
	 * @return the behaviour name
	 */
	public String get() {
		return behName;
	}
	
}
