/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tc.shared.world;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Properties;

import wucore.utils.geom.Line2;

/**
 * The icon library of a world: the {@link WMIcon} definitions referenced by
 * name from the objects.
 *
 * <pre>
 *   ICONS  = n
 *   ICON_i = label, nlines, x1, y1, x2, y2, ...
 * </pre>
 */
public class WMIcons
{
	protected WMIcon[]			icons;

	/* Constructors */

	public WMIcons ()
	{
		icons = new WMIcon[0];
	}

	public WMIcons (Properties props)
	{
		fromProperties (props);
	}

	/* Accessors */

	public final int			n ()			{ return icons.length; }
	public final WMIcon[]		icons ()		{ return icons; }

	public WMIcon at (int i)
	{
		if ((i < 0) || (i >= icons.length))		return null;
		return icons[i];
	}

	public WMIcon at (String label)
	{
		return at (index (label));
	}

	public int index (String label)
	{
		if ((label == null) || (icons == null))		return -1;
		for (int i = 0; i < icons.length; i++)
			if (label.equals (icons[i].label))		return i;
		return -1;
	}

	public int indexOf (WMIcon e)
	{
		for (int i = 0; i < icons.length; i++)
			if (icons[i] == e)						return i;
		return -1;
	}

	/** First icon with exactly the same segments, or null. */
	public WMIcon findGeometry (Line2[] lines)
	{
		WMIcon		probe = new WMIcon ("?", lines);
		for (int i = 0; i < icons.length; i++)
			if (icons[i].sameGeometry (probe))		return icons[i];
		return null;
	}

	/** A label not yet used: prefix, prefix_2, prefix_3 ... */
	public String uniqueLabel (String prefix)
	{
		if ((prefix == null) || (prefix.length () == 0))		prefix = "icon";
		if (index (prefix) < 0)			return prefix;
		for (int i = 2; ; i++)
			if (index (prefix + "_" + i) < 0)	return prefix + "_" + i;
	}

	/* Edition methods */

	public void add (WMIcon e)
	{
		WMIcon[]	tmp = new WMIcon[icons.length + 1];
		System.arraycopy (icons, 0, tmp, 0, icons.length);
		tmp[icons.length] = e;
		icons = tmp;
	}

	public WMIcon remove (int i)
	{
		if ((i < 0) || (i >= icons.length))		return null;
		WMIcon		old = icons[i];
		WMIcon[]	tmp = new WMIcon[icons.length - 1];
		System.arraycopy (icons, 0, tmp, 0, i);
		System.arraycopy (icons, i + 1, tmp, i, icons.length - i - 1);
		icons = tmp;
		return old;
	}

	/**
	 * Registers an icon for the given local segments, reusing an existing one
	 * with the same geometry when possible. Used when reading legacy files where
	 * every object carried its own inline icon.
	 */
	public WMIcon register (Line2[] lines, String preferredLabel)
	{
		WMIcon		icon = findGeometry (lines);
		if (icon != null)				return icon;
		icon = new WMIcon (uniqueLabel (preferredLabel), lines);
		add (icon);
		return icon;
	}

	/* Persistence */

	public void fromProperties (Properties props)
	{
		icons = new WMIcon[Integer.parseInt (props.getProperty ("ICONS", "0"))];
		for (int i = 0; i < icons.length; i++)
			icons[i] = new WMIcon (props.getProperty ("ICON_" + i));
	}

	/* JSON: [{label, lines}, ...] */

	public WMIcons (JsonElement e)				{ fromJson (e); }

	public void fromJson (JsonElement e)
	{
		JsonArray	arr = ((e != null) && e.isJsonArray ()) ? e.getAsJsonArray () : new JsonArray ();
		icons	= new WMIcon[arr.size ()];
		for (int i = 0; i < icons.length; i++)		icons[i] = new WMIcon (arr.get (i).getAsJsonObject ());
	}

	public JsonArray toJson ()
	{
		JsonArray	arr = new JsonArray ();
		for (WMIcon ic : icons)		arr.add (ic.toJson ());
		return arr;
	}
}
