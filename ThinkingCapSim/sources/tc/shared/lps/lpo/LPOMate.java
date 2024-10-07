/*
 * Created on 24-oct-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.lps.lpo;

import java.io.*;
import java.awt.*;

import tc.vrobot.*;

import devices.pos.*;
import wucore.widgets.*;
import wucore.utils.geom.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class LPOMate extends LPOPoint implements Serializable
{
	protected double				xmin;
	protected double				ymin;
	protected double				xmax;
	protected double				ymax;
	
	// Constructor
	public LPOMate (int source)
	{			
		super (0.0, 0.0, 0.0, null, source);
	}
	
	// Instance methods
	public void set (RobotDesc rdesc)
	{
		int			i;
		Line2[]		icon;
		
		icon		= rdesc.icon;
		if (icon == null)
		{
			xmin = ymin = -rdesc.RADIUS;
			xmax = ymax = rdesc.RADIUS;
		}
		else
		{
			xmin = ymin = Double.MAX_VALUE;
			xmax = ymax = Double.MIN_VALUE;
			for (i = 0; i < icon.length; i++)
			{
				if (icon[i].orig ().x () < xmin)		xmin	 = icon[i].orig ().x ();
				if (icon[i].orig ().x () > xmax)		xmax	 = icon[i].orig ().x ();
				if (icon[i].orig ().y () < ymin)		ymin	 = icon[i].orig ().y ();
				if (icon[i].orig ().y () > ymax)		ymax	 = icon[i].orig ().y ();

				if (icon[i].dest ().x () < xmin)		xmin	 = icon[i].dest ().x ();
				if (icon[i].dest ().x () > xmax)		xmax	 = icon[i].dest ().x ();
				if (icon[i].dest ().y () < ymin)		ymin	 = icon[i].dest ().y ();
				if (icon[i].dest ().y () > ymax)		ymax	 = icon[i].dest ().y ();
			}
		}		
	}
	
	public void update (Position cur, Position point)
	{
		super.update (cur, point);
		
		alpha	= point.alpha () - cur.alpha ();
	}
	
	public void draw (Model2D model, LPOView view)
	{
		double			xx, yy, aa;
		double			gap;
		
		if (!active)			return;

		aa	= view.rotation + phi;
		xx 	= rho * Math.cos (aa);
		yy 	= rho * Math.sin (aa);
		gap	= Math.max (xmax-xmin, ymax-ymin) * 2.0;
		
		if ((xx < view.min.x ()) || (xx > view.max.x ()) || (yy < view.min.y ()) || (yy > view.max.y ()))		return;
			
		if (label != null)
			model.addRawText (xx+gap, yy+gap, label, Color.RED);

		model.addRawCircle (xx, yy, gap, Model2D.DASHED, Color.RED);

		//model.addRawRotBox (xx+xmin, yy+ymin, xx+xmax, yy+ymax, alpha, Model2D.PLAIN, Color.RED);
		model.addRawRotTransBox (xmin, ymin, xmax, ymax, xx, yy, view.rotation + alpha, Model2D.PLAIN, Color.RED);
		model.addRawTransRotLine (0.0, 0.0, xmax*1.5, 0.0, xx, yy, view.rotation + alpha, Color.RED.darker());
	}
}
