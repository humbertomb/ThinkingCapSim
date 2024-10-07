/*
 * (c) 2003 Humberto Martinez
 */
 
package tc.shared.lps.lpo;

import java.io.*;

import tc.vrobot.*;

import wucore.widgets.*;
import wucore.utils.math.*;
import wucore.utils.color.*;

public class LPOArcBuffer extends LPO implements Serializable
{
	static public final double			ARC_STEP	= 1.0;		// Circular arc step (rad)
	static public final double[]		buffer_wgt	= { 1.0, 1.0, 1.1, 1.2, 1.4, 1.7, 2.2, 3.0, 5.0, 10.0 };

	// Arc buffer structure
	protected LPOArcPoint[]				buffer;					// Circular buffer to store range data
	
	// Arc buffer parameters
	protected SensorPos					pos;					// Sensor position
	protected int						size;					// Number of range data points
	protected double					mark;					// Scale mark length (m)
	protected double					side;					// Quantization cell size (m)
	protected double					cone;					// Circular arc width (rad)
	protected double					range;					// Current maximum range (m)
	
	// Arc buffer management
	protected int 						buffer_head;
	protected int 						buffer_tail;
	protected int 						buffer_max;	

	// Constructor
	public LPOArcBuffer (SensorPos pos, int size, double mark, double range, double side, double cone, String label, int source)
	{			
		super (0.0, 0.0, 0.0, label, source);
		
		setRange (range);
		setSide (side);
		setSize (size);
		
		this.pos	= pos;
		this.mark	= mark;
		this.cone	= cone;
		
		resetBuffer ();

		// The buffer is active by default
		active (true);
	}

	// Accessors
	public final void				setRange (double range)		{ this.range = range; }
	public final double				getRange ()					{ return range; }
	public final void				setSide (double side)		{ this.side = side; }
	public final double				getSide ()					{ return side; }
	public final void				setSize (int size)			{ this.size = size; }
	public final int				getSize ()					{ return size; }
	
	public final LPOArcPoint[]		buffer ()					{ return buffer; }
	
	// Instance methods
	public void resetBuffer ()
	{
		int			i;
		
		buffer		= new LPOArcPoint[size];

		// Initialize range sensors buffer
		for (i = 0; i < size; i++)
			buffer[i]	= new LPOArcPoint (side, PERCEPT);			
			
		buffer_head	= 0;
		buffer_tail	= 0;
		buffer_max	= size - 1;
	}

	public void add_range (SensorPos feat, int i, TrackerData tracker)
	{
		int				j;
		double			x, y;
		double			dst;
		LPOArcPoint		tail;
	
		for (j = 0; j < tracker.trks.length; j++)
		{	
			dst		= tracker.trks[j][TrackerData.RANGE];
			tail	= buffer[buffer_tail];
			tail.active = ((dst <= range) && (dst > 0.0) && (tracker.valid[j]));
		  
			if (tail.active)
			{
				// Compute sensor detection point
				x 	= feat.x () + dst * Math.cos (tracker.trks[j][TrackerData.ALPHA]);
				y 	= feat.y () + dst * Math.sin (tracker.trks[j][TrackerData.ALPHA]);

				tail.set (x, y, tracker.trks[j][TrackerData.SPEED], i);
		    }
		
			// Circulate the buffer	
			buffer_tail ++;
			if (buffer_tail > buffer_max)			/* wraparound */
				buffer_tail = 0;
		
			if (buffer_tail == buffer_head)			/* full */
			{
		 		buffer_head++;						/* pop oldest entry */
				if (buffer_head > buffer_max)
					buffer_head = 0;
			}
		}
	}
	
	public double occupied_arc (FeaturePos f, double cone, double range, boolean dowgt)
	{
		int				i, j;
		double			x1, y1;
		double			min;
		double			a1, a2;
		double			phi, rho;
		LPOArcPoint		s;

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

	public void clamp (Matrix3D rm)
	{
		int				i;
		
		for (i = 0; i < size; i++)
			buffer[i].clamp (rm);
	}
	
	public void draw (Model2D model, LPOView view)
	{
		int				i;
		double			xx, yy, aa;
		double			dr0, dr1, kk;
		double			xi, yi, xf, yf;
		
		if (!active)	return;

		aa		= view.rotation + pos.alpha ();
		dr0		= cone * 0.5;
		
		xx = pos.x () + range * Math.cos (aa);
		yy = pos.y () + range * Math.sin (aa);				
		model.addRawLine (pos.x (), pos.y (), xx, yy, ColorTool.fromWColorToColor(WColor.YELLOW));

		xx = pos.x () + range * Math.cos (aa - dr0);
		yy = pos.y () + range * Math.sin (aa - dr0);				
		model.addRawLine (pos.x (), pos.y (), xx, yy, ColorTool.fromWColorToColor(WColor.YELLOW));

		xx = pos.x () + range * Math.cos (aa + dr0);
		yy = pos.y () + range * Math.sin (aa + dr0);				
		model.addRawLine (pos.x (), pos.y (), xx, yy, ColorTool.fromWColorToColor(WColor.YELLOW));
		
		for (kk = mark; kk <= range; kk += mark)
		{
			xi = pos.x () + kk * Math.cos (aa - dr0);
			yi = pos.y () + kk * Math.sin (aa - dr0);		
					
			for (dr1 = -dr0 + ARC_STEP; dr1 <= dr0; dr1 += ARC_STEP)
			{
				xf = pos.x () + kk * Math.cos (aa + dr1);
				yf = pos.y () + kk * Math.sin (aa + dr1);				
				model.addRawLine (xi, yi, xf, yf, ColorTool.fromWColorToColor(WColor.YELLOW));
			
				xi = xf;
				yi = yf;
			}
			
			xx = pos.x () + kk * Math.cos (pos.alpha ());
			yy = pos.y () + kk * Math.sin (pos.alpha ());									
			model.addRawText (xx, yy, new Integer ((int) Math.round (kk)).toString (), ColorTool.fromWColorToColor(WColor.CYAN));
		}	

		for (i = 0; i < size; i++)
			if (buffer[i].active)
				buffer[i].draw (model, view);
	}
}


