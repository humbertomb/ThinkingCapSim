/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcrob.umu.quaky2.lpo;

import java.io.*;

import tc.shared.lps.lpo.*;

import wucore.widgets.*;
import wucore.utils.color.*;

/**
 * The point to align the ball with the net from: a circle, as a point, and an
 * arrow from it the way the ball has to go (towards the ball and the net, its
 * heading phi).
 */
public class Align extends LPOPoint implements Serializable
{
	public static final double		ARROW		= 0.3;		// Length of the arrow (m)

	// Constructor
	public Align (String label, LPOSource source)
	{
		super (0.0, 0.0, 0.0, label, source);
	}

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
			model.addRawText (xx, yy, label, ColorTool.fromWColorToColor(color));

		model.addRawCircle (xx, yy, RADIUS, ColorTool.fromWColorToColor(color));
		model.addRawArrow (xx, yy, ARROW, view.rotation + phi, ColorTool.fromWColorToColor(color));
	}
}
