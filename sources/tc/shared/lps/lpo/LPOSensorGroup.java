/*
 * (c) 2003 Humberto Martinez, Alessandro Saffiotti
 */
 
package tc.shared.lps.lpo;

import java.io.*;

import tc.vrobot.*;

import wucore.widgets.*;
import wucore.utils.color.*;

public class LPOSensorGroup extends LPO implements Serializable
{
	public static final double			RADIUS			= 0.05;

	protected int						size;					// Number of range data points
	protected SensorPos[]				spos;
	public double[]						range;
	public boolean[]					valid;

	// Constructor
	public LPOSensorGroup (SensorPos[] spos, String label, LPOSource source)
	{			
		super (0.0, 0.0, 0.0, label, source);
			
		this.spos	= spos;

		size		= spos.length;
		range		= new double[size];
		valid		= new boolean[size];
		
		color (WColor.MAGENTA);
		active (true);
	}

	// Instance methods
	public void update (int i, double range, boolean valid)
	{
		if ((i < 0) || (i >= size))		return;
		
		this.range[i]	= range;
		this.valid[i]	= valid;
	}
	
	public void update (double[] range, boolean[] valid)
	{
		int				i;
		
		for (i = 0; i < size; i++)
		{
			this.range[i]	= range[i];
			this.valid[i]	= valid[i];
		}
	}
	
	public void draw (Model2D model, LPOView view)
	{
		int				i;
		double			xx, yy, aa;
		
		if (!active)	return;

		for (i = 0; i < size; i++)
		{
			if (!valid[i])				continue;
			if ((spos[i] instanceof FeaturePos) && !(range[i] < ((FeaturePos) spos[i]).range () - 1E-3))		continue;	// at its range it sees nothing
			
			aa	= view.rotation + spos[i].theta ();
			xx 	= spos[i].rho () * Math.cos (aa) + range[i] * Math.cos (view.rotation + spos[i].orientation ());
			yy 	= spos[i].rho () * Math.sin (aa) + range[i] * Math.sin (view.rotation + spos[i].orientation ());
			
			model.addRawCircle (xx, yy, RADIUS, ColorTool.fromWColorToColor(color));
			if (spos[i] instanceof FeaturePos)
				sector (model, view, (FeaturePos) spos[i], range[i]);
		}
	}

	/** The sector of a sensor of an area out to what it reads, in dashed lines: its two sides and its arc. */
	protected void sector (Model2D model, LPOView view, FeaturePos f, double d)
	{
		java.awt.Color	c = ColorTool.fromWColorToColor (color);
		double			aa = view.rotation + f.theta ();
		double			xs = f.rho () * Math.cos (aa), ys = f.rho () * Math.sin (aa);
		double			cone = f.cone ();
		double			a0 = view.rotation + f.orientation () - cone * 0.5;
		int				steps = Math.max (2, (int) Math.ceil (Math.toDegrees (cone) / 10.0));
		double			px = xs + d * Math.cos (a0), py = ys + d * Math.sin (a0);

		if (!(cone > 0.0))		return;
		model.addRawLine (xs, ys, px, py, Model2D.DASHED, c);
		for (int k = 1; k <= steps; k++)
		{
			double	b = a0 + cone * k / steps;
			double	qx = xs + d * Math.cos (b), qy = ys + d * Math.sin (b);

			model.addRawLine (px, py, qx, qy, Model2D.DASHED, c);
			px	= qx;
			py	= qy;
		}
		model.addRawLine (px, py, xs, ys, Model2D.DASHED, c);
	}
}


