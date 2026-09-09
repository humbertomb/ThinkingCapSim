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
	public LPOSensorGroup (SensorPos[] spos, String label, int source)
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
			
			aa	= view.rotation + spos[i].phi ();
			xx 	= spos[i].rho () * Math.cos (aa) + range[i] * Math.cos (view.rotation + spos[i].alpha ());
			yy 	= spos[i].rho () * Math.sin (aa) + range[i] * Math.sin (view.rotation + spos[i].alpha ());
			
			model.addRawCircle (xx, yy, RADIUS, ColorTool.fromWColorToColor(color));
		}
	}
}


