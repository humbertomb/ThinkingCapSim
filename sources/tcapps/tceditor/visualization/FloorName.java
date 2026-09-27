/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor.visualization;

import javax.media.j3d.*;
import javax.vecmath.*;

import com.sun.j3d.utils.geometry.Text2D;

import tc.shared.world.WMObject;
import wucore.utils.geom.Line2;

/**
 * The name of a robot or an object, written flat on the floor under it: centred
 * on it, just over the floor so that it is not lost in it, and in the one size
 * of letter of the scene, which the robots set: the size at which the name of a
 * robot is no wider than {@link #SHARE} times the robot seen from above
 * ({@link #fit}), the same for every name so that they read alike -- a ball is
 * not named in letters bigger than the robots'. It is placed again with
 * {@link #place} as the thing moves, and it does not turn with it, so that it
 * can always be read.
 */
public class FloorName extends BranchGroup
{
	/** How far over the floor the name lies (m). */
	static public final double		Z		= 0.03;
	/** How wide the name may be, as a share of the largest dimension of the thing seen from above. */
	static public final double		SHARE	= 2.0;

	protected TransformGroup		tg;
	protected Transform3D			t		= new Transform3D ();
	protected double				w, h;					// the text as written (m)
	protected double				scale	= 1.0;			// the size of letter: how much the text as written is shrunk
	protected double				x, y, z;				// where it lies

	/** @param name  what is written */
	public FloorName (String name)
	{
		Text2D		text = new Text2D (name, new Color3f (0.1f, 0.1f, 0.6f), "Application", 140, java.awt.Font.BOLD);
		double[]	size = size (text);

		setCapability (BranchGroup.ALLOW_DETACH);
		w	= size[0];
		h	= size[1];
		tg	= new TransformGroup ();
		tg.setCapability (TransformGroup.ALLOW_TRANSFORM_WRITE);
		tg.addChild (text);
		addChild (tg);
		place (0.0, 0.0, 0.0);
	}

	/** A name in the size of letter something else set (see {@link #fit}). */
	public FloorName (String name, double scale)
	{
		this (name);
		setScale (scale);
	}

	/**
	 * The size of letter at which this name is no wider than {@link #SHARE} times
	 * a thing this big seen from above (m): 1 when it fits as written, less when
	 * it has to be shrunk. What a robot gives the scene.
	 */
	public double fit (double across)
	{
		return ((across > 0.0) && (w > 0.0)) ? Math.min (1.0, SHARE * across / w) : 1.0;
	}

	/** The size of letter: how much the text as written is shrunk (1: as written). */
	public void setScale (double scale)
	{
		if (!(scale > 0.0) || (scale == this.scale))		return;
		this.scale	= scale;
		place (x, y, z);
	}

	public double getScale ()						{ return scale; }

	/** Centred under (x, y), on the floor at height z. */
	public void place (double x, double y, double z)
	{
		this.x	= x;	this.y	= y;	this.z	= z;
		t.setIdentity ();
		t.setScale (scale);
		t.setTranslation (new Vector3d (x - scale * w / 2.0, y - scale * h / 2.0, z + Z));
		tg.setTransform (t);
	}

	/** How wide and how tall a flat text is drawn (m), from its bounds; zeros when it cannot be told. */
	static public double[] size (Node text)
	{
		try
		{
			BoundingBox	box = new BoundingBox (text.getBounds ());
			Point3d		lo = new Point3d (), up = new Point3d ();

			box.getLower (lo);
			box.getUpper (up);
			if ((up.x > lo.x) && (up.y > lo.y))		return new double[] { up.x - lo.x, up.y - lo.y };
		}
		catch (Exception e)		{ }
		return new double[] { 0.0, 0.0 };
	}

	/**
	 * The largest dimension of some parts seen from above (m): the longer side
	 * of the box round them, in the frame they are in when asked (so before they
	 * are placed anywhere). Zero when it cannot be told.
	 */
	static public double footprint (Node... parts)
	{
		double	d = 0.0;

		for (Node n : parts)
		{
			if (n == null)		continue;
			try
			{
				BoundingBox	box = new BoundingBox (n.getBounds ());
				Point3d		lo = new Point3d (), up = new Point3d ();

				box.getLower (lo);
				box.getUpper (up);
				if ((up.x > lo.x) && (up.y > lo.y))		d = Math.max (d, Math.max (up.x - lo.x, up.y - lo.y));
			}
			catch (Exception e)		{ }
		}
		return d;
	}

	/** The largest dimension of an object drawn by its icon (m): the longer side of the box round its segments. */
	static public double footprint (WMObject o)
	{
		double	x0 = Double.MAX_VALUE, y0 = Double.MAX_VALUE, x1 = -Double.MAX_VALUE, y1 = -Double.MAX_VALUE;
		Line2[]	icon = (o != null) ? o.getLocalIcon () : null;

		if ((icon == null) || (icon.length == 0))		return 0.0;
		for (Line2 l : icon)
		{
			x0 = Math.min (x0, Math.min (l.orig ().x (), l.dest ().x ()));		x1 = Math.max (x1, Math.max (l.orig ().x (), l.dest ().x ()));
			y0 = Math.min (y0, Math.min (l.orig ().y (), l.dest ().y ()));		y1 = Math.max (y1, Math.max (l.orig ().y (), l.dest ().y ()));
		}
		return Math.max (0.0, Math.max (x1 - x0, y1 - y0));
	}
}
