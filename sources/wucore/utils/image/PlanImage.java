/*
 * (c) 2026 Humberto Martinez Barbera
 */

package wucore.utils.image;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import wucore.utils.geom.Line2;

/**
 * The bitmap an element of a simulation may carry (a robot, an object of the
 * world) drawn on a plan view: placed where the drawing of the element is,
 * scaled to it and turned with it.
 *
 * Images are read once and kept, and a file that cannot be read is remembered
 * as such, so a view that repaints continuously does not try again and again.
 */
public class PlanImage
{
	static private final Map<String, Image>		CACHE = new HashMap<String, Image> ();

	/** The image at a path, or null when there is none or it cannot be read. */
	static public synchronized Image get (String path)
	{
		Image		img;

		if ((path == null) || (path.trim ().length () == 0))		return null;
		path	= path.trim ();
		if (CACHE.containsKey (path))		return CACHE.get (path);

		img		= null;
		try
		{
			File	f = new File (path);
			if (f.isFile ())		img = ImageIO.read (f);
		} catch (Exception e) { }
		if (img == null)		System.out.println ("--[PlanImage] Cannot read the image <" + path + ">");
		CACHE.put (path, img);
		return img;
	}

	/** Forgets what was read (an editor changing the image of an element, for instance). */
	static public synchronized void flush ()					{ CACHE.clear (); }
	static public synchronized void flush (String path)			{ if (path != null)		CACHE.remove (path.trim ()); }

	/**
	 * Box a drawing occupies.
	 *
	 * @return {minx, miny, maxx, maxy}, or null when there is nothing to measure
	 */
	static public double[] bounds (Line2[] lines)
	{
		double[]	b = { Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE };
		boolean		any = false;

		if (lines == null)			return null;
		for (Line2 l : lines)
		{
			if (l == null)			continue;
			b[0] = Math.min (b[0], Math.min (l.orig ().x (), l.dest ().x ()));
			b[1] = Math.min (b[1], Math.min (l.orig ().y (), l.dest ().y ()));
			b[2] = Math.max (b[2], Math.max (l.orig ().x (), l.dest ().x ()));
			b[3] = Math.max (b[3], Math.max (l.orig ().y (), l.dest ().y ()));
			any	= true;
		}
		return any ? b : null;
	}

	/**
	 * Draws an image on a plan view.
	 *
	 * @param g			where to draw
	 * @param img		the image ({@link #get(String)})
	 * @param px, py	centre of the image, in pixels
	 * @param wpx, hpx	size the image takes, in pixels
	 * @param angle		heading of the element (rad); its X axis is the width of the image
	 */
	static public void draw (Graphics2D g, Image img, double px, double py, double wpx, double hpx, double angle)
	{
		AffineTransform		old;

		if ((img == null) || (wpx <= 0.0) || (hpx <= 0.0))		return;

		old		= g.getTransform ();
		g.translate (px, py);
		g.rotate (-angle);												// the Y axis of the view points down
		g.drawImage (img, (int) Math.round (-wpx / 2), (int) Math.round (-hpx / 2),
						  (int) Math.round (wpx), (int) Math.round (hpx), null);
		g.setTransform (old);
	}
}
