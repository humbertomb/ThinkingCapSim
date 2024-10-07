/*
 * @(#)ItemBehInformation.java		1.0 2004/01/22
 * 
 * (c) 2004 Denis Remondini
 *
 */
package tc.shared.linda;

import java.io.*;

import tclib.behaviours.fhb.*;

/**
 * This class implements a specific item that contains the informations about
 * a behaviour
 * 
 * @version	1.0		22 Jan 2004
 * @author 	Denis Remondini
 */
public class ItemBehInfo extends Item implements Serializable
{
	/* The behaviour informations */
	private BehaviourInfo behInfo;
	
	public ItemBehInfo() {
		behInfo = null;
	}
	
	/**
	 * Sets the behaviour informations
	 * @param behInfo the behaviour informations
	 */
	public void set(BehaviourInfo behInfo) {
		this.behInfo = behInfo;
	}
	
	/**
	 * Returns the behaviour informations
	 * @return the behaviour informations
	 */
	public BehaviourInfo get() {
		return behInfo;
	}
	
}

