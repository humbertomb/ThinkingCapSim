/*
 * (c) 2026 Humberto Martinez
 */

package tcrob.umu.soccer.lpo;

import java.io.*;

import tc.shared.lps.lpo.*;

import wucore.widgets.*;
import wucore.utils.color.*;

/**
 * A landmark of the field (a beacon of the Four-Legged League): a cylinder
 * with two bands of colour at the top, seen from above as a disc in the colour
 * of its top band ringed by the colour of the one below. The colour of the LPO
 * ({@link #color}) is the one of its top band, the one it is told apart by.
 */
public class LPOLandmark extends LPO implements Serializable
{
	// Object specific information
	protected double					radius;						// Radius of the cylinder (m)
	protected WColor					below;						// The colour of the band under the top one

	// Constructor
	public LPOLandmark (double radius, String label, LPOSource source)
	{
		super (0.0, 0.0, 0.0, label, source);

		this.radius		= radius;
	}

	/** The colour of the band under the top one (the top one is the colour of the LPO). */
	public void below (WColor color)		{ below = color; }
	public WColor below ()					{ return below; }

	// Instance methods
	public void draw (Model2D model, LPOView view)
	{
		double			xx, yy, aa;

		if (!active)	return;

		aa	= view.rotation + theta;
		xx 	= rho * Math.cos (aa);
		yy 	= rho * Math.sin (aa);

		if ((xx < view.min.x ()) || (xx > view.max.x ()) || (yy < view.min.y ()) || (yy > view.max.y ()))		return;

		if (label != null)
			model.addRawText (xx, yy, label, ColorTool.fromWColorToColor (color));

		// the band below as a ring round the top one
		if (below != null)
			model.addRawCircle (xx, yy, radius, Model2D.FILLED, ColorTool.fromWColorToColor (below));
		model.addRawCircle (xx, yy, (below != null) ? radius * 0.6 : radius, Model2D.FILLED, ColorTool.fromWColorToColor (color));
	}
}
