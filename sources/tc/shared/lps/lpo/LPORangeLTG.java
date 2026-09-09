/*
 * Created on 26-oct-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.lps.lpo;

import wucore.widgets.*;
import wucore.utils.math.*;
import wucore.utils.geom.*;
import wucore.utils.logs.*;
import wucore.utils.color.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class LPORangeLTG extends LPORangeBuffer
{	
	static protected final String[]	labels	= { "Raw", "Filter", "1st Deriv" };
	
	// LTG processing parameters
	protected int					FWINDOW	= 3;		// Window size for signal filter
	protected double					ZERO		= 0.1;	// Zero interval for 1st derivative
	protected double					SZONE	= 2.0;	// Security zone for edge points
	protected double					MPOINTS	= 2.0;	// Minimum point density (points/m)
	
	// Laser signal processing
	protected double					cone;			// Aperture of laser scan
	protected double					step;			// Distance between scan rays (rad)
	protected double[]				filter;			// Filter for signal
	protected double[]				deriv1;			// First derivative of signal
	
	// LTG nodes connectivity (object boundaries)
	protected LPOLine				goal;			// Desired robot goal
	protected int[]					cfrom;			// Initial node of boundary
	protected int[]					cto;				// Final node of boundary
	protected boolean[]				cdanger;			// Boundary intersects with desired trajectory
	protected int					cnum;			// Number of object boundaries
	
	// Debugging tools
	protected LogPlot				plot;
	protected double[][]				pbuffer;
	protected boolean				debug;
	
	// Constructor
	public LPORangeLTG (int size, double cone, double range, double side, String label, int source)
	{			
		super (size, range, side, label, source);
		
		this.cone	= cone;
		this.step	= (cone * Angles.RTOD) / (double) size;
		
		filter	= new double[size];
		deriv1	= new double[size];
	
		cfrom	= new int[size];
		cto		= new int[size];
		cdanger	= new boolean[size];
		cnum		= 0;
		goal		= new LPOLine (0.0, 0.0, 0.0, "LTG_Goal", LPO.ARTIFACT);
		goal.color (WColor.MAGENTA.darker());
		
		color (WColor.ORANGE);
		setDebug (false);
	}
	
	// Accessors
	public int		boundaries ()				{ return cnum; }

	public int		getFWindow ()				{ return FWINDOW; }
	public double	getZDeriv ()					{ return ZERO; }
	public double	getSZone ()					{ return SZONE; }
	public double	getMinPoints ()				{ return MPOINTS; }
	public void		setFWindow (int fwindow)		{ this.FWINDOW = fwindow; }
	public void		setZDeriv (double zero)		{ this.ZERO = zero; }
	public void		setSZone (double szone)		{ this.SZONE = szone; }
	public void		setMinPoints (double mpoints)	{ this.MPOINTS = mpoints; }

	public int[]		getInitBoundaries()			{ return cfrom; }
	public int[]		getEndBoundaries()			{ return cto; }
	public int		getNumBoundaries()			{ return cnum; }

	// Instance methods
	public void setDebug (boolean debug)
	{
		this.debug = debug;
		
		if (debug)
		{
			pbuffer	= new double[size][labels.length+1];
			plot		= new LogPlot ("Laser Scan", "deg", "m");
			plot.setYRange (0.0, range_lrf);
			plot.open (labels);
		}
		else if (plot != null)
		{
			plot.close ();
			plot		= null;
		}
	}
	
	public void build_graph ()
	{
		int				i, j, k;

		goal.active (false);

		// Filter signal noise (average filter + discretisation)
		int				cnt;
		double			avg;

		for (i = 0; i < FWINDOW; i++)
		{
			filter[i]		= buffer[i].len;
			filter[size-i-1]	= buffer[size-i-1].len;
		}
		for (i = FWINDOW; i < size-FWINDOW; i++)
		{
			if (buffer[i].active)
			{
				avg	= 0.0;
				cnt	= 0;
				for (j = i-FWINDOW; j < i+FWINDOW; j++)
					if (buffer[j].len < range_lrf)
					{
						avg += buffer[j].len;
						cnt ++;
					}
				filter[i]	= Math.round (avg / (double) cnt * 10.0) / 10.0;		
//				filter[i]	= Math.log (1.0 + filter[i]) - 1.0;
			}
			else
				filter[i]	= buffer[i].len;
		}
				
		// Compute first derivative, with band pass filter
		deriv1[0]	= 0.0;
		for (i = size-2; i >= 0; i--) 
		{
			deriv1[i]	= (filter[i+1] - filter[i]) / step;
			if (Math.abs (deriv1[i]) < ZERO)
				deriv1[i]	= 0.0;		
		}

		// Show signal plots
		if (debug)
		{		
			// Reverse curve order, from [-cone/2 .. +cone/2]
			for (j = 0, i = size-1; i >= 0; i--, j++)
			{
				pbuffer[j][0]	= (double) j * step - cone * Angles.RTOD * 0.5;
				pbuffer[j][1]	= buffer[i].len;
				pbuffer[j][2]	= filter[i];
				pbuffer[j][3]	= deriv1[i] + range_lrf * 0.5;
			}
			plot.draw (pbuffer, size, labels.length+1);	
		}

		// Find singular points in 1st derivative
		for (i = 1; i < size-1; i++) 
		{
			if (((deriv1[i] == 0.0) && (deriv1[i] > deriv1[i+1]))
				|| ((deriv1[i] == 0.0) && (deriv1[i] < deriv1[i+1])))
			{
				buffer[i].active = (buffer[i].len < range_lrf);
				continue;
			}

			if (((deriv1[i] == 0.0) && (deriv1[i] > deriv1[i-1]))
				|| ((deriv1[i] == 0.0) && (deriv1[i] < deriv1[i-1])))
			{
				buffer[i].active = (buffer[i].len < range_lrf);
				continue;
			}
			
			buffer[i].active = false;
		}

/*
		// Skip initial out of range readings
		for (i = 0; i < size; i++)
			if (filter[i] < range_lrf)
				break;
			
		// If there are valid measures, find local minima in filtered data
		boolean		up;

		if (i < size-1)
		{
			up	= (filter[i] < filter[i+1]);
			i	+= 2;
			k	= -1;
			for (; i < size; i++) 
			{
				if ((filter[i] < filter[i-1]) && !up)
				{
					k = -1;
					continue;
				}
				if ((filter[i] > filter[i-1]) && up)
				{
					k = -1;
					continue;
				}
				if (filter[i] == filter[i-1])
				{
					if (k == -1)		k = i;
					continue;
				}
				
				if (k != -1)
				{
					boolean		range = true;
					
					for (j = k; j <=i; j++)
						if (buffer[j].len >= range_lrf)
							range = false;						
					buffer[k+(i-k)/2].active = range;			
				}
				else
					buffer[i].active = (buffer[i].len < range_lrf);			
				
				up	= !up;
				k	= -1;
			}		
		}
*/		
		// Add extreme singular points (in case they have not been detected)
		for (i = 0; i < size; i++)
			if (buffer[i].len < range_lrf)
			{
				buffer[i].active = true;
				break;
			}
		for (i = size-1; i >= 0; i--)
			if (buffer[i].len < range_lrf)
			{
				buffer[i].active = true;
				break;
			}
			
		// Move all graph nodes closer to the robot
		if (SZONE > 0.0)
			for (i = 0; i < size; i++)
				if (buffer[i].active)
					buffer[i].locate_polar (Math.max (buffer[i].rho - SZONE, 0.0), buffer[i].phi);
			
		// Build connectivity graph (object boundaries)
		boolean		connected;
		double		ratio;
		
		k		= -1;
		cnum		= 0;
		for (i = 0; i < size; i++)
			if (buffer[i].active)
			{
				k = i;
				break;
			}
		for (i = k+1; i < size; i++)
			if (buffer[i].active)
			{				
				connected = true;
				for (j = k; j < i; j++)
					if (buffer[j].len >= range_lrf)
						connected = false;
				
				ratio	= (double) (i - k) / buffer[k].distance (buffer[i]);
				if (connected && (ratio > MPOINTS))
				{
					cfrom[cnum]		= k;
					cto[cnum]		= i;
					cdanger[cnum]	= false;
					cnum ++;
				}		
				k	= i;
			}
	}
	
	public LPORangePoint collision (Point2 point)
	{
		if (point != null)
			return collision (point.x (), point.y ());
		
		return null;
	}
		
	public LPORangePoint collision (double gx, double gy)
	{
		int				i;
		double			dist;
		Line2			path;
		Point2			cross;
		LPORangePoint	pt1, pt2;
		LPORangePoint	pt3;

		goal.locate (gx, gy);
		goal.active (true);
		pt3		= null;
		dist		= Math.sqrt (gx * gx + gy * gy);
		path		= new Line2 (0.2, 0.0, gx, gy);
		for (i = 0; i < cnum; i++)
		{
			cdanger[i]	= false;
			pt1			= buffer[cfrom[i]];
			pt2			= buffer[cto[i]];			
			if ((pt1.rho > dist) && (pt2.rho > dist))
				continue;

			cross	= path.intersection (pt1.x, pt1.y, pt2.x, pt2.y);
			if (cross != null)
			{
				cdanger[i]	= true;
				pt3			= pt2;
				// break;		// TODO for optimisation reasons, only one should be considered
			}
		}		

		return pt3;
	}
/*
 		int				i;
		Point2			pt;
		Line2			cln;
		double			d1;
		double			d;
		
		d 	= Double.MAX_VALUE;
				d1 = pt.distance (x1, y1);
				if (d1 < d)
				{
					d 	= d1;
					cln	= edges[i].edge;
				}
 */	
	public void draw (Model2D model, LPOView view)
	{	
		int			i;
		int			ci, cf;
		double		x1, y1, a1;
		double		x2, y2, a2;
		
		if (!active)		return;

		super.draw (model, view);		
		goal.draw (model, view);
		
		// Draw connecting segments
		for (i = 0; i < cnum; i++)		
		{
			ci	= cfrom[i];
			cf	= cto[i];
			
			a1	= view.rotation + buffer[ci].phi;
			x1 	= buffer[ci].rho * Math.cos (a1);
			y1 	= buffer[ci].rho * Math.sin (a1);

			a2	= view.rotation + buffer[cf].phi;
			x2 	= buffer[cf].rho * Math.cos (a2);
			y2 	= buffer[cf].rho * Math.sin (a2);

			if (cdanger[i])
				model.addRawLine (x1, y1, x2, y2, Model2D.THICK, ColorTool.fromWColorToColor(WColor.RED));
			else
				model.addRawLine (x1, y1, x2, y2, Model2D.THICK, ColorTool.fromWColorToColor(color));
		}		
	}
}

