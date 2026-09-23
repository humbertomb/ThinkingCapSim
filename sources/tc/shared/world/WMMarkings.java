/*
 * (c) 2026 Humberto Martinez Barbera
 */
package tc.shared.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import wucore.utils.color.ColorTool;
import wucore.utils.color.WColor;

/**
 * The markings of the world: lines drawn on the floor, each one with its colour
 * and width, which no robot bumps into but every camera sees.
 */
public class WMMarkings extends Object
{
	protected WMMarking[]			lines;

	private WColor					defColor	= WColor.WHITE;
	private double					defWidth	= 0.05;

	// Constructors
	public WMMarkings (int n)					{ lines = new WMMarking[n]; }
	public WMMarkings ()						{ this (0); }
	public WMMarkings (JsonElement e)			{ fromJson (e); }

	// Accessors
	public final int				n ()				{ return lines.length; }
	public final WMMarking[]		items ()			{ return lines; }

	public final WColor				defaultColor ()		{ return defColor; }
	public final double				defaultWidth ()		{ return defWidth; }

	// Instance methods
	public WMMarking at (int i)
	{
		if ((i < 0) || (i >= lines.length))		return null;
		return lines[i];
	}

	/* Edition methods (world editor) */
	public void add (WMMarking e)
	{
		WMMarking[]	tmp = new WMMarking[lines.length + 1];
		System.arraycopy (lines, 0, tmp, 0, lines.length);
		tmp[lines.length] = e;
		lines = tmp;
	}

	public WMMarking remove (int i)
	{
		if ((i < 0) || (i >= lines.length))		return null;
		WMMarking	old = lines[i];
		WMMarking[]	tmp = new WMMarking[lines.length - 1];
		System.arraycopy (lines, 0, tmp, 0, i);
		System.arraycopy (lines, i + 1, tmp, i, lines.length - i - 1);
		lines = tmp;
		return old;
	}

	public int indexOf (WMMarking e)
	{
		for (int i = 0; i < lines.length; i++)
			if (lines[i] == e)					return i;
		return -1;
	}

	public void setDefaults (WColor color, double width)
	{
		defColor	= color;
		defWidth	= width;
	}

	/* JSON: {defaults: {color, width}, items: [...]} */

	public void fromJson (JsonElement e)
	{
		JsonObject	o = ((e != null) && e.isJsonObject ()) ? e.getAsJsonObject () : new JsonObject ();
		JsonObject	def = World.getObject (o, "defaults");
		JsonArray	arr = World.getArray (o, "items");
		String		cname = World.getString (def, "color", null);

		if (cname != null)		defColor = ColorTool.getColorFromName (cname);
		defWidth	= World.getDouble (def, "width", defWidth);

		lines	= new WMMarking[arr.size ()];
		for (int i = 0; i < lines.length; i++)		lines[i] = new WMMarking (arr.get (i).getAsJsonObject (), defColor, defWidth);
	}

	public JsonObject toJson ()
	{
		JsonObject	o = new JsonObject ();
		JsonObject	def = new JsonObject ();
		JsonArray	arr = new JsonArray ();

		def.addProperty ("color", ColorTool.getNameFromColor (defColor));
		def.addProperty ("width", World.num (defWidth));
		for (WMMarking m : lines)		arr.add (m.toJson (defColor, defWidth));
		o.add ("defaults", def);
		o.add ("items", arr);
		return o;
	}
}
