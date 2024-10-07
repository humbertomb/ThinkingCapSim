/*
 * Created on 04-oct-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tcrob.ingenia.ifork.linda;

import java.io.*;

import tc.shared.linda.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ItemIForkZone extends Item implements Serializable
{
	public String			zone;
	
	// Constructors
	public ItemIForkZone ()
	{
		
	}
	
	// Instance methods
	public void set (String zone)
	{
		this.zone	= zone;
	}
}
