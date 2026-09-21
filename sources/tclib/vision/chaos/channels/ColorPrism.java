/**
 * Created on 16-nov-2005
 *
 * @author Humberto Martinez Barbera
 */
package tclib.vision.chaos.channels;

import java.util.*;

public class ColorPrism extends Cluster
{
	public int min0;
	public int max0;
	public int min1;
	public int max1;
	public int min2;
	public int max2;

	public final int getMin0 ()				{ return min0; }
	public final int getMax0 ()				{ return max0; }
	public final int getMin1 ()				{ return min1; }
	public final int getMax1 ()				{ return max1; }
	public final int getMin2 ()				{ return min2; }
	public final int getMax2 ()				{ return max2; }

	public String paramCookedData ()
	{
		if (seeds.isEmpty ())
			return "";
		
		return "["+format(min0)+", "+format(max0)+"] ["+format(min1)+", "+format(max1)+"] ["+format(min2)+", "+format(max2)+"]";
	}

	public String paramRawData ()
	{
		return max0+","+min0+","+max1+","+min1+","+max2+","+min2;
	}

	public void initialise (String params, String slist)
	{
		// the limits given are only a start: the prism is made again from the seeds (recomputeShape)
		if ((params != null) && (params.trim ().length () > 0))
		{
			StringTokenizer st = new StringTokenizer (params, ", ");
			
			max0 = Integer.parseInt(st.nextToken());
			min0 = Integer.parseInt(st.nextToken());
			max1 = Integer.parseInt(st.nextToken());
			min1 = Integer.parseInt(st.nextToken());
			max2 = Integer.parseInt(st.nextToken());
			min2 = Integer.parseInt(st.nextToken());
		}

		parseSeeds (slist);
		recomputeShape ();
	}
		
	public void include (Pixel pix)
	{
		seeds.add (pix);		
		recomputeShape ();
	}
	
	public void exclude (Pixel pix)
	{
		seeds.remove (pix);
		recomputeShape ();
	}
	
	public boolean inside (Pixel pix)
	{
		return (pix.co1 >= min0) && (pix.co1 <= max0) && (pix.co2 >= min1) && (pix.co2 <= max1) && (pix.co3 >= min2) && (pix.co3 <= max2);
	}
	
	public boolean intersection (Cluster other)
	{
		// Check for trivial cases
		if (seeds.isEmpty () || other.seeds.isEmpty ())	return false;
		
		// Check for defined Cluster types
		if (other instanceof ColorPrism)
		{
			ColorPrism		prism;
			boolean			collision0, collision1, collision2;
			
			prism = (ColorPrism) other;
			collision0 = collision1 = collision2 = false;
			
			if (min0 < prism.getMin0())
			{
				if (max0 >= prism.getMin0())
					collision0 = true;
			}
			else
				if (min0 < prism.getMax0())
					collision0 = true;
						
			if (min1 < prism.getMin1())
			{		
				if (max1 >= prism.getMin1())
					collision1 = true;
			}
			else
				if (min1 < prism.getMax1())
					collision1 = true;
						
			if (min2 < prism.getMin2())
			{
				if (max2 >= prism.getMin2())
					collision2 = true;
			}
			else
				if (min2 < prism.getMax2())
					collision2 = true;
									
			return (collision0 && collision1 && collision2);
		}
			
		return false;
	}

	protected void recomputeShape ()
	{
		int			bmin0, bmax0, bmin1, bmax1, bmin2, bmax2;
			
		bmin0	= min0;
		bmax0	= max0;
		bmin1	= min1;
		bmax1	= max1;
		bmin2	= min2;
		bmax2	= max2;

		min0		= 255;
		max0		= 0;
		min1		= 255;
		max1		= 0;
		min2		= 255;
		max2		= 0;
		
		for (Pixel pix : seeds)
		{
			if (pix.co1 < min0)		min0 = pix.co1;
			if (pix.co1 > max0)		max0 = pix.co1;
			
			if (pix.co2 < min1)		min1 = pix.co2;
			if (pix.co2 > max1)		max1 = pix.co2;
			
			if (pix.co3 < min2)		min2 = pix.co3;
			if (pix.co3 > max2)		max2 = pix.co3;
		}
		
		modified = (bmin0 != min0) || (bmax0 != max0) || (bmin1 != min1) || (bmax1 != max1) || (bmin2 != min2) || (bmax2 != max2);
	}
}
