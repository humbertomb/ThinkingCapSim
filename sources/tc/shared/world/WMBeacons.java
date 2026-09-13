/*
 * Created on 08-feb-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.ArrayList;

import wucore.utils.geom.Line2;
import wucore.utils.geom.Point2;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class WMBeacons
{
	
	protected WMBeacon[]			beacons;

	
	// Accessors
	public final int	 		n () 				{ return beacons.length; }
	public final WMBeacon[]	edges ()			{ return beacons; }
	
	// Instance methods
	public WMBeacon at (int i)
	{
		if ((i < 0) || (i >= beacons.length)) return null;
		return beacons[i];
	}
	
	public int index (String label)
	{
		int  		i;
		
		if ((label == null) || (beacons == null))			return -1;

		for (i = 0; i < beacons.length; i++)
			if (label.equals (beacons[i].label))			return i;

		return -1;
	}
	
    public int crossline(Line2 line){
    	return crossline(line.orig ().x (), line.orig ().y (), line.dest ().x (), line.dest ().y ());
    }
    
    public int crossline (double x1, double y1, double x2, double y2){

		int				i, index;
		Point2			pt;
		double			d1;
		double			d;
		
		index=-1;	// No intersecta
		d 	= Double.MAX_VALUE;
		for (i = 0; i < n(); i++)
		{
			pt = beacons[i].getLine().intersection (x1, y1, x2, y2);
			if (pt != null)
			{
				d1 = pt.distance (x1, y1);
				if (d1 < d)
				{
					d 	= d1;
					index=i;
				}
			}
		}
		return index;
    }
	
	/* Edition methods (world editor) */
	public void add (WMBeacon e)
	{
		WMBeacon[]	tmp = new WMBeacon[beacons.length + 1];
		System.arraycopy (beacons, 0, tmp, 0, beacons.length);
		tmp[beacons.length] = e;
		beacons = tmp;
	}

	public WMBeacon remove (int i)
	{
		if ((i < 0) || (i >= beacons.length))		return null;
		WMBeacon		old = beacons[i];
		WMBeacon[]	tmp = new WMBeacon[beacons.length - 1];
		System.arraycopy (beacons, 0, tmp, 0, i);
		System.arraycopy (beacons, i + 1, tmp, i, beacons.length - i - 1);
		beacons = tmp;
		return old;
	}

	public int indexOf (WMBeacon e)
	{
		for (int i = 0; i < beacons.length; i++)
			if (beacons[i] == e)				return i;
		return -1;
	}

	/* JSON: [{label, x, y, z, orientation, width, height}, ...] */

	public WMBeacons ()							{ beacons = new WMBeacon[0]; }
	public WMBeacons (JsonElement e)				{ fromJson (e); }

	public void fromJson (JsonElement e)
	{
		JsonArray	arr = ((e != null) && e.isJsonArray ()) ? e.getAsJsonArray () : new JsonArray ();
		beacons	= new WMBeacon[arr.size ()];
		for (int i = 0; i < beacons.length; i++)		beacons[i] = new WMBeacon (arr.get (i).getAsJsonObject ());
	}

	public JsonArray toJson ()
	{
		JsonArray	arr = new JsonArray ();
		for (WMBeacon x : beacons)		arr.add (x.toJson ());
		return arr;
	}
}
