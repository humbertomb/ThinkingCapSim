/*
 * (c) 2003 Humberto Martinez, Alessandro Saffiotti
 */
 
package tc.shared.lps.lpo;

import java.io.*;

import tc.vrobot.*;

import wucore.widgets.*;
import wucore.utils.color.*;

public class LPOSensorRange extends LPO implements Serializable
{
	protected int						size;					// Number of range data points
	protected SensorPos[]				spos;
	public double[]						range;
	public boolean[]					valid;

	// Constructor
	public LPOSensorRange (SensorPos[] spos, String label, int source)
	{			
		super (0.0, 0.0, 0.0, label, source);
		
		this.spos	= spos;
		
		size		= spos.length;
		range		= new double[size];
		valid		= new boolean[size];
		
		color (WColor.CYAN.brighter());
		active (true);
	}

	// Instance methods
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
			xx 	= spos[i].rho () * Math.cos (aa);
			yy 	= spos[i].rho () * Math.sin (aa);
			
			model.addRawArrow (xx, yy, range[i], view.rotation + spos[i].alpha (), ColorTool.fromWColorToColor(color));
		}
	}
}


