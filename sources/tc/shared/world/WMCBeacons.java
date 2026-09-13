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


/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class WMCBeacons
{
	
	protected WMCBeacon[]			beacons;

	
	// Accessors
	public final int	 		n () 				{ return beacons.length; }
	public final WMCBeacon[]	edges ()			{ return beacons; }
	
	// Instance methods
	public WMCBeacon at (int i)
	{
		if ((i < 0) || (i >= beacons.length)) return null;
		return beacons[i];
	}
	
	/* Edition methods (world editor) */
	public void add (WMCBeacon e)
	{
		WMCBeacon[]	tmp = new WMCBeacon[beacons.length + 1];
		System.arraycopy (beacons, 0, tmp, 0, beacons.length);
		tmp[beacons.length] = e;
		beacons = tmp;
	}

	public WMCBeacon remove (int i)
	{
		if ((i < 0) || (i >= beacons.length))		return null;
		WMCBeacon		old = beacons[i];
		WMCBeacon[]	tmp = new WMCBeacon[beacons.length - 1];
		System.arraycopy (beacons, 0, tmp, 0, i);
		System.arraycopy (beacons, i + 1, tmp, i, beacons.length - i - 1);
		beacons = tmp;
		return old;
	}

	public int indexOf (WMCBeacon e)
	{
		for (int i = 0; i < beacons.length; i++)
			if (beacons[i] == e)				return i;
		return -1;
	}

	/* JSON: [{label, x, y, z, diameter, height}, ...] */

	public WMCBeacons ()							{ beacons = new WMCBeacon[0]; }
	public WMCBeacons (JsonElement e)				{ fromJson (e); }

	public void fromJson (JsonElement e)
	{
		JsonArray	arr = ((e != null) && e.isJsonArray ()) ? e.getAsJsonArray () : new JsonArray ();
		beacons	= new WMCBeacon[arr.size ()];
		for (int i = 0; i < beacons.length; i++)		beacons[i] = new WMCBeacon (arr.get (i).getAsJsonObject ());
	}

	public JsonArray toJson ()
	{
		JsonArray	arr = new JsonArray ();
		for (WMCBeacon x : beacons)		arr.add (x.toJson ());
		return arr;
	}
}
