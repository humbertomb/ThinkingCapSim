/*
 * (c) 2026 Humberto Martinez
 */
 
package tc.shared.linda;

import java.io.*;
import java.util.*;

public class ItemBehNeeds extends Item implements Serializable
{
	public class BehNeeds
	{
		public String 				object;
		public double				need;
		
		public BehNeeds (String object, double need)
		{
			this.object	= object;
			this.need	= need;
		}
	}

	public enum ScanTypes			{ SCAN_NONE, SCAN_LOW, SCAN_MID, SCAN_HIGH, SCAN_FULL }

	public ScanTypes				scanType = ScanTypes.SCAN_NONE;
	public ArrayList<BehNeeds>		needs = new ArrayList<BehNeeds> ();
	
	//Constructors
	public ItemBehNeeds ()
	{
		this.set (0);
	}
	
	// Instance methods
	public void clearNeeds ()					{ needs.clear(); }
	public void changeScan (ScanTypes scan)		{ this.scanType = scan; }
	
	public void addNeed (String object, double need, long tstamp)
	{
		set (tstamp);
		
		needs.add (new BehNeeds (object, need));
	}
}