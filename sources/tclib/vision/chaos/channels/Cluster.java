/**
 * Created on 17-nov-2005
 *
 * @author Humberto Martinez Barbera
 */
package tclib.vision.chaos.channels;

import java.util.*;

public abstract class Cluster implements Cloneable
{
	static protected final String		NOSEEDS	= "EMPTY";
	
	protected HashSet<Pixel>			seeds	= new HashSet<Pixel> ();
	protected boolean				modified	= false;

	public abstract String paramRawData ();
	public abstract String paramCookedData ();
	
	public abstract void initialise (String params, String seeds);
	public abstract void include (Pixel pix);
	public abstract void exclude (Pixel pix);
	public abstract boolean inside (Pixel pix);
	public abstract boolean intersection (Cluster other);
	protected abstract void recomputeShape ();
	
	static protected String format (int value)
	{
		String		out;
		
		out	= Integer.valueOf (value).toString ();
		while (out.length() < 3)
			out = "0" + out;
		
		return out;
	}
	
	public HashSet<Pixel>	getSeeds () 						{ return seeds; }
	public boolean	isModified () 					{ return modified; }
	
	public void setSeeds (HashSet<Pixel> seeds)
	{
		this.seeds = seeds;
		recomputeShape ();
	}
	
	public HashSet<Pixel> cloneSeeds ()
	{
		@SuppressWarnings("unchecked")
		HashSet<Pixel> set = (HashSet<Pixel>) seeds.clone (); 
		return set;
	}
	
	public String seedsRawData ()
	{
		String		out;
			
		if (seeds.isEmpty ())		return NOSEEDS;
		out	= "";
		for (Pixel pix : seeds)
			out += " "+pix.co1+","+pix.co2+","+pix.co3;
		return out;
	}

	protected void parseSeeds (String slist)
	{
		StringTokenizer	st1;
		StringTokenizer	st2;
		String			str;
		Pixel			pix;
		int				co1, co2, co3;
		
		seeds	= new HashSet<Pixel> ();
		if (slist == null)			return;
		
		st1		= new StringTokenizer (slist, " ");
		while (st1.hasMoreTokens ())
		{
			str = st1.nextToken ();
			if (!str.equals (NOSEEDS))
			{
				st2 = new StringTokenizer (str, ",");
				co1 = Integer.parseInt(st2.nextToken());
				co2 = Integer.parseInt(st2.nextToken());
				co3 = Integer.parseInt(st2.nextToken());
				pix	= new Pixel (co1, co2, co3);
				seeds.add (pix);
			}
		}
	}
	
	public void dumpSeeds ()
	{
		for (Pixel pix : seeds)
			System.out.print (pix + " ");
		System.out.println("\n\tTotal seeds count = "+seeds.size ());
	}
}
