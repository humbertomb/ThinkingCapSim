	/*
 * (c) 2003 Humberto Martinez, Alessandro Saffiotti
 */
 
package tc.shared.lps.lpo;

import java.io.*;

import tc.vrobot.*;

import wucore.widgets.*;
import wucore.utils.color.*;

public class LPOSensorScanner extends LPO implements Serializable
{
	static public final int				CONTOUR	= 0;
	static public final int				RAYS		= 1;
	
	protected int						size;		// Number of scan points
	protected double						cone;		// Sensor aperture (rad)
	protected SensorPos					spos;
	protected int						dmode;		// Drawing mode
	public double[]						range;

	// Constructor
	public LPOSensorScanner (SensorPos spos, int size, double cone, String label, int source)
	{			
		super (0.0, 0.0, 0.0, label, source);
			
		this.spos	= spos;	
		this.size	= size;
		this.cone	= cone;
		
		range		= new double[size];
		dmode		= CONTOUR;
		
		color (WColor.BLUE);
	}

	// Accessors
	public final SensorPos	senpos ()			{ return spos; }
	public final double		cone ()				{ return cone; }
	public final int			size ()				{ return size; }
	
	public final void		setMode (int dmode)	{ this.dmode = dmode; }
	
	// Instance methods
	public void update (double[] range, boolean valid)
	{
		int				i;
		
		active (valid);
		
		if (!active)				return;
		
		for (i = 0; i < size; i++)
			this.range[i]	= range[i];
	}
	
	public void draw (Model2D model, LPOView view)
	{
		int				i;
		double			xx, yy;
		double			xi, yi, xf, yf;
		double			k, delta, aa;
		
		if (!active)				return;

		delta	= cone / ((double) size - 1.0);

		aa	= view.rotation + spos.phi ();
   		xx	= spos.rho () * Math.cos (aa);
       	yy 	= spos.rho () * Math.sin (aa);
       	xi	= xx;
       	yi	= yy;
		for (i = 0, k = -cone * 0.5; i < size; i++, k += delta)
		{
			xf 	= spos.rho () * Math.cos (aa) + range[i] * Math.cos (view.rotation + spos.alpha () + k);
			yf 	= spos.rho () * Math.sin (aa) + range[i] * Math.sin (view.rotation + spos.alpha () + k);

			switch (dmode)
			{
			case RAYS:
				if (i % 5 == 0)
					model.addRawLine (xx, yy, xf, yf, ColorTool.fromWColorToColor(WColor.CYAN));
				model.addRawLine (xi, yi, xf, yf, ColorTool.fromWColorToColor(color));
				break;
				
			case CONTOUR:
			default:
				model.addRawLine (xi, yi, xf, yf, ColorTool.fromWColorToColor(color));
			}
			
			xi	= xf;
			yi	= yf;
		}

   		xf	= spos.rho () * Math.cos (aa);
       	yf 	= spos.rho () * Math.sin (aa);
		model.addRawLine (xi, yi, xf, yf, ColorTool.fromWColorToColor(color));
	}
}


