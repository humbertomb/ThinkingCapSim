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
public class WMDocks
{
	protected WMDock[] docks;
	

	
	// Accessors	
	public final int	 		n () 				{ return docks.length; }
	public final WMDock[]	waypoints ()		{ return docks; }

	// Instance methods
	public WMDock at (int i)
	{
		if ((i < 0) || (i >= docks.length)) return null;
		return docks[i];
	}
	
	public WMDock at (String label)
	{
		return at(index(label));
	}
	
	public int index (String label)
	{
		int  		i;
		
		if ((label == null) || (docks == null))			return -1;

		for (i = 0; i < docks.length; i++)
			if (label.equals (docks[i].label))			return i;

		return -1;
	}
	
	/* Edition methods (world editor) */
	public void add (WMDock e)
	{
		WMDock[]	tmp = new WMDock[docks.length + 1];
		System.arraycopy (docks, 0, tmp, 0, docks.length);
		tmp[docks.length] = e;
		docks = tmp;
	}

	public WMDock remove (int i)
	{
		if ((i < 0) || (i >= docks.length))		return null;
		WMDock		old = docks[i];
		WMDock[]	tmp = new WMDock[docks.length - 1];
		System.arraycopy (docks, 0, tmp, 0, i);
		System.arraycopy (docks, i + 1, tmp, i, docks.length - i - 1);
		docks = tmp;
		return old;
	}

	public int indexOf (WMDock e)
	{
		for (int i = 0; i < docks.length; i++)
			if (docks[i] == e)				return i;
		return -1;
	}

	/* JSON: [{label, x, y, z, orientation, flow}, ...] */

	public WMDocks ()							{ docks = new WMDock[0]; }
	public WMDocks (JsonElement e)				{ fromJson (e); }

	public void fromJson (JsonElement e)
	{
		JsonArray	arr = ((e != null) && e.isJsonArray ()) ? e.getAsJsonArray () : new JsonArray ();
		docks	= new WMDock[arr.size ()];
		for (int i = 0; i < docks.length; i++)		docks[i] = new WMDock (arr.get (i).getAsJsonObject ());
	}

	public JsonArray toJson ()
	{
		JsonArray	arr = new JsonArray ();
		for (WMDock x : docks)		arr.add (x.toJson ());
		return arr;
	}
}
