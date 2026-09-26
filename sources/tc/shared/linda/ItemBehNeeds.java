/*
 * (c) 2026 Humberto Martinez
 */
 
package tc.shared.linda;

import java.io.*;
import java.util.*;

public class ItemBehNeeds extends Item implements Serializable
{
	static public class BehNeeds implements Serializable
	{
		private static final long	serialVersionUID = 1L;

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

	/**
	 * The object needed most: the one of the highest need, the first of them when
	 * several are needed as much, or null when nothing is needed.
	 */
	public BehNeeds mostNeeded ()
	{
		BehNeeds		best = null;

		for (BehNeeds n : needs)
			if ((n != null) && (n.object != null) && ((best == null) || (n.need > best.need)))		best = n;
		return best;
	}

	public String toString ()
	{
		StringBuilder	sb = new StringBuilder ("scan " + scanType);

		for (BehNeeds n : needs)		sb.append (", ").append (n.object).append ("=").append (n.need);
		return sb.toString ();
	}
}