/*
 * @(#)ItemRulesNamesList.java		1.0 2004/02/06
 * 
 * (c) 2004 Denis Remondini
 *
 */
package tc.shared.linda;

import java.io.*;
import java.util.ArrayList;

/**
 * This class implements a specific item that contains a rules names list
 * 
 * @version	1.0		06 Feb 2004
 * @author 	Denis Remondini
 */
public class ItemBehRules extends Item implements Serializable
{
	/* The list containing the rules names */
	private ArrayList rulesNamesList;
	
	public ItemBehRules() {
		rulesNamesList = null;
	}
	
	/**
	 * Sets the rules names
	 * @param rulesNamesList the list containing the rules names
	 */
	public void set(ArrayList rulesNamesList) {
		this.rulesNamesList = rulesNamesList;
	}
	
	/**
	 * Returns the rules names
	 * @return the rules names
	 */
	public ArrayList get() {
		return rulesNamesList;
	}
	
}

