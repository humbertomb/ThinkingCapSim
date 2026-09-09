/*
 * (c) 2003 Humberto Martinez, Alessandro Saffiotti
 */
 
package tc.shared.lps.lpo;

import java.io.*;

import tc.vrobot.*;

import wucore.widgets.*;
import wucore.utils.math.*;
import wucore.utils.color.*;

public class LPORangeBuffer extends LPO implements Serializable
{
	static public final double[]		buffer_wgt		= { 1.0, 1.0, 1.1, 1.2, 1.4, 1.7, 2.2, 3.0, 5.0, 10.0 };

	// Range buffer structures
	protected LPORangePoint[]			buffer;			// Circular buffer to store range data
	protected int[]					ndx;				// Phi-ordered measures
	
	// Range buffer parameters
	protected int					size;			// Number of range data points
	protected double					side;			// Quantization cell size (m)
	protected double					range_son;		// Current sonar-like maximum range (m)
	protected double					range_lrf;		// Current laser-like maximum range (m)
	
	// Range buffer management
	protected int 					buffer_head;
	protected int 					buffer_tail;
	protected int 					buffer_max;	
	protected int					ndx_n;			// Number of ordered measures

	// Constructor
	public LPORangeBuffer (int size, double range, double side, String label, int source)
	{			
		super (0.0, 0.0, 0.0, label, source);
		
		setRange (range, range);
		setSide (side);
		setSize (size);
		
		resetBuffer ();

		// The buffer is active by default
		active (true);
	}

	// Accessors
	public final void			setRange (double rson, double rlrf)	{ this.range_son = rson; this.range_lrf = rlrf; }
	public final void			setRangeSON (double range)			{ this.range_son = range; }
	public final void			setRangeLRF (double range)			{ this.range_lrf = range; }
	public final double			getRangeSON ()						{ return range_son; }
	public final double			getRangeLRF ()						{ return range_lrf; }
	public final void			setSide (double side)					{ this.side = side; }
	public final double			getSide ()							{ return side; }
	public final void			setSize (int size)					{ this.size = size; resetBuffer (); }
	public final int				getSize ()							{ return size; }
	
	public final LPORangePoint	buffer (int i)						{ return buffer[ndx[i]]; }
	
	public final LPORangePoint[]	buffer ()							{ return buffer; }
	public final int[]			ndx ()								{ return ndx; }
	public final int				ndx_n ()								{ return ndx_n; }
	
	// Instance methods
	public void color (WColor color)
	{
		int			i;
		
		super.color (color);
		
		for (i = 0; i < size; i++)
			buffer[i].color (color);
	}
	
	public void resetBuffer ()
	{
		int			i;
		
		buffer		= new LPORangePoint[size];
		ndx			= new int[size];

		// Initialize range sensors buffer
		for (i = 0; i < size; i++)
		{
			buffer[i]	= new LPORangePoint (side, PERCEPT);			
			ndx[i]		= 0;
		}
			
		buffer_head	= 0;
		buffer_tail	= 0;
		buffer_max	= size - 1;
		ndx_n		= 0;
	}

	/* 
	 * Put reading R from sonar I in circular buffer
	 * Note: also insert null readings, so we have fixed
	 * latency time (cycles) in buffer
	 */	
	public void add_range (SensorPos feat, int i, double rho)
	{
		double			x, y;
		double			len;
		LPORangePoint	tail;
	
		tail = buffer[buffer_tail];
		tail.active = ((rho <= range_son) && (rho > 0.0));
	  
		// Compute sensor detection point
		len	= Math.max (Math.min (rho, range_son), 0.0);
		x 	= feat.x () + len * Math.cos (feat.alpha ());
		y 	= feat.y () + len * Math.sin (feat.alpha ());

		tail.set (x, y, len, LPORangePoint.SONAR, i);
	
		// Circulate the buffer	
		buffer_tail ++;
		if (buffer_tail > buffer_max)				/* wraparound */
			buffer_tail = 0;
	
		if (buffer_tail == buffer_head)			/* full */
		{
	 		buffer_head++;						/* pop oldest entry */
			if (buffer_head > buffer_max)
				buffer_head = 0;
		}
	}
	
	/* 
	 * Put reading R from target I in circular buffer
	 * Note: also insert null readings, so we have fixed
	 * latency time (cycles) in buffer
	 */	
	public void add_range (SensorPos feat, int i, double rho, double alpha)
	{
		double			x, y;
		double			len;
		LPORangePoint	tail;
	
		tail = buffer[buffer_tail];
		tail.active = ((rho <= range_lrf) && (rho > 0.0));
	  
		// Compute sensor detection point
		len	= Math.max (Math.min (rho, range_lrf), 0.0);
		x 	= feat.x () + len * Math.cos (feat.alpha () + alpha);
		y 	= feat.y () + len * Math.sin (feat.alpha () + alpha);

		tail.set (x, y, len, LPORangePoint.LRF, i);
	
		// Circulate the buffer	
		buffer_tail ++;
		if (buffer_tail > buffer_max)				/* wraparound */
			buffer_tail = 0;
	
		if (buffer_tail == buffer_head)			/* full */
		{
	 		buffer_head++;						/* pop oldest entry */
			if (buffer_head > buffer_max)
				buffer_head = 0;
		}
	}
	
	/* 
	 * Check occupancy
	 *
	 * Find the closest reading (m) in a slice from a1 to a2 rad
	 *
	 */
	public double occupied_arc (FeaturePos f, double cone, double range, boolean dowgt)
	{
		int				i, j;
		double			x1, y1;
		double			min;
		double			a1, a2;
		double			phi, rho;
		LPORangePoint	s;

		a1	= Angles.radnorm_180 (f.alpha () - cone);
		a2	= Angles.radnorm_180 (f.alpha () + cone);

		// Compute sensor detection cone
		x1	= f.rho () * Math.cos (f.phi ());
		y1	= f.rho () * Math.sin (f.phi ());
				
		// Find closest reading within bounding region
		min = range;	
		for (i = 0; i < size; i++) 
		{
			s = buffer[i];
			if (s.active)
			{
				// Compute translated point
				rho	= Math.sqrt ((s.x - x1) * (s.x - x1) + (s.y - y1) * (s.y - y1));
				phi	= Math.atan2 (s.y - y1, s.x - x1);
				
				// Check if the weighting scheme applies
				if (dowgt)
					j	= Math.min (Math.max (s.ageing, 0), buffer_wgt.length-1);
				else
					j	= 0;
					
				// Check if point is in sensors cone
				if ((a1 <= phi) && (phi <= a2) && (buffer_wgt[j] * rho < min))
					min = rho;
			}
		}
		
		return min;
	}
		
	/* 
	 * Check occupancy
	 *
	 * Find the closest reading (m) in a rectangle of size base x range
	 *
	 */
	public double occupied_rect (FeaturePos f, double base, double range, boolean dowgt)
	{
		int				i, j;
		double			x, y, xs, ys;
		double			min;
		double			phi, rho;
		LPORangePoint	s;

		// Compute sensor detection rectangle
		x	= f.rho () * Math.cos (f.phi ());
		y	= f.rho () * Math.sin (f.phi ());
		
		// Find closest reading within bounding region
		min = range;	
		for (i = 0; i < size; i++) 
		{
			s = buffer[i];
			if (s.active)
			{
				// Compute translated and rotated point
				rho	= Math.sqrt ((s.x - x) * (s.x - x) + (s.y - y) * (s.y - y));
				phi	= Math.atan2 (s.y - y, s.x - x) - f.alpha ();
				
				xs	= rho * Math.cos (phi);
				ys	= rho * Math.sin (phi);
				
				// Check if the weighting scheme applies
				if (dowgt)
					j	= Math.min (Math.max (s.ageing, 0), buffer_wgt.length-1);
				else
					j	= 0;
					
				// Check if point is in sensors area
				if ((ys > -base) && (ys < base) && (xs > 0.0)  && (xs < range) && (buffer_wgt[j] * rho < min))
					min = rho;
			}
		}
		
		return min;
	}
		
	public void sort_buffer ()
	{
		int				i;
		double			a1, a2, step;
		LPORangePoint	s;
		
		step	= 2.0 * Math.PI / (double) (size - 1);
		ndx_n	= 0;
		for (a1 = -Math.PI; a1 <= Math.PI; a1 += step)
			for (i = 0; i < size; i++) 
			{
				a2 = a1 + step;
				s = buffer[i];
				if (s.active)
					if ((a1 <= s.phi) && (s.phi < a2))
					{
						ndx[ndx_n] = i;
						ndx_n ++;
					}	
			}
	}
	
	public void clamp (Matrix3D rm)
	{
		int				i;
		
		for (i = 0; i < size; i++)
			buffer[i].clamp (rm);
	}
	
	public void draw (Model2D model, LPOView view)
	{
		int				i;
		
		if (!active)		return;

		// Draw new measures
		for (i = 0; i < size; i++)
			if (buffer[i].active)
				buffer[i].draw (model, view);
	}
}


