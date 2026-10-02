/*
 * (c) 2026 Humberto Martinez Barbera
 */
 
package tcrob.umu.soccer.lpo;

import java.io.*;

import tc.shared.lps.lpo.*;

import wucore.widgets.*;
import wucore.utils.color.*;

/**
 * A robot the vision sees, of one team or the other: where the nearest of its
 * feet is, drawn as a ring of the size of a robot in the colour of its uniform,
 * with its name.
 */
public class LPORobot extends LPO implements Serializable
{
	// Object specific information
	protected double					radius;						// How big it is drawn (m)

	// Constructor
	public LPORobot (double radius, String label, LPOSource source)
	{			
		super (0.0, 0.0, 0.0, label, source);
		
		this.radius		= radius;
	}

	// Instance methods
	public void draw (Model2D model, LPOView view)
	{
		double			xx, yy, aa;
		java.awt.Color	c;
		
		if (!active)	return;

		aa	= view.rotation + theta;
		xx 	= rho * Math.cos (aa);
		yy 	= rho * Math.sin (aa);
		
		if ((xx < view.min.x ()) || (xx > view.max.x ()) || (yy < view.min.y ()) || (yy > view.max.y ()))		return;

		c	= ColorTool.fromWColorToColor (color);
		if (label != null)
			model.addRawText (xx, yy, label, c);

		// the ring, behind the foot that is seen (the robot is there, not in front of it)
		model.addRawCircle (xx + radius * Math.cos (aa), yy + radius * Math.sin (aa), radius, c);
	}
}
