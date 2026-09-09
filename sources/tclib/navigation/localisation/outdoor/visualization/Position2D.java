/**
 */
/*
 * (c) 2001-2002 Humberto Martinez
 */

package tclib.navigation.localisation.outdoor.visualization;

import java.io.*;
import java.awt.*;

import devices.pos.*;
import wucore.widgets.*;

import tc.gui.visualization.*;

public class Position2D extends Object2D implements Component2DListener
{
	public static final int			MAX_POINTS		= 5000;				// Maximum number of samples per path
	public static final double		FOCUS			= 20.0;				// Focus circle size (m)
	public static final double		OFFSET			= 2.5;				// Offset for robot labelling (m)
	
	// Display parameters
	protected boolean				autocenter;							// Autocenter on a given point?
	protected double					boundary			= 250.0;		// Current displaying boundaries
	protected double					acx;							// Autocenter X point
	protected double					acy;							// Autocenter Y point
	
	// Objects
	protected Component2D				canvas;
	protected Path					path;
	
	// Constructors
	public Position2D (Component2D canvas)
	{
		this.canvas		= canvas;
		this.initialise (canvas.getModel ());
		this.initialise ();
	}
	
	// Accessors
	public final void		autocenter (boolean autocenter)		{ this.autocenter = autocenter; }
	public final void		boundary (double boundary)			{ this.boundary = boundary; }
	
	public final void		setCenter (double acx, double acy)	{ this.acx = acx; this.acy = acy; canvas.autoCenter (acx, acy); }
	
	// Instance methods
	protected void initialise ()
	{
		// Initialise custom variables
		autocenter		= false;
		path				= new Path (MAX_POINTS);
		
		// Initialise canvas options
		canvas.setDrawrefs (true);
		canvas.setDrawscale (true);
		canvas.setDrawbkgHUD (true);
		canvas.ref_label[0]	= "E";
		canvas.ref_label[1]	= "N";
		
		// Register listeners
		canvas.setListener (this);
	}
	
	// Find a suitable file name
	protected String log_name (String base, String suffix)
	{
		String			name		= null;
		boolean			notfound	= true;
		int				logorder	= 0;
		
		while (notfound)
			try
		{
				name	= base + "." + logorder + suffix;
				new FileReader (name);
				logorder ++;
		}
		catch (FileNotFoundException fnfe) { notfound = false; }
		
		return name;
	}
	
	public String getObjectText (String src)
	{
		return "N/A";
	}
	
//	public void update (GPSData gpsdata)
	public void update (UTMPos pos)
	{
		double			x, y, a;
		double			cx, cy;
		double			vminx, vminy, vmaxx, vmaxy;
		
		if (pos == null)			return;
		
		// Initialise drawing parameters
		model.clearView ();
		
		// Initialise dafault values
		cx			= acx;
		cy			= acy;
		
		// Set default viewing region
		vminx		= Double.MAX_VALUE;
		vminy		= Double.MAX_VALUE;
		vmaxx		= -Double.MAX_VALUE; 
		vmaxy		= -Double.MAX_VALUE;
		
		// Draw all the vehicles in the list
		a		= 0.0;
		x		= pos.getEast ();
		y		= pos.getNorth ();
		
		if ((x != 0.0) || (y != 0.0))
		{
			if (path.last () == null)
				path.add (x, y, a);
			else
			{
				Position		last;
				
				last		= path.last ();
				if ((last != null) && ((last.x () != x) || (last.y () != y)))
					path.add (x, y, a);
			}
		}
		
		// Draw focus circle
		model.addRawCircle (x, y, FOCUS, Color.RED);
		model.addRawLine (x - FOCUS, y, x - FOCUS / 2.0, y, Color.RED);
		model.addRawLine (x + FOCUS, y, x + FOCUS / 2.0, y, Color.RED);
		model.addRawLine (x, y - FOCUS, x, y - FOCUS / 2.0, Color.RED);
		model.addRawLine (x, y + FOCUS, x, y + FOCUS / 2.0, Color.RED);

		// Draw robot position
//		if (rdesc.image != null)
//			model.addRawIcon (x, y, robots[i], rdesc.image, Model2D.PLAIN, rcol);
//		else
//		{
//			// Draw focus circle
//			model.addRawCircle (x, y, FOCUS, rcol);
//			model.addRawLine (x - FOCUS, y, x - FOCUS / 2.0, y, rcol);
//			model.addRawLine (x + FOCUS, y, x + FOCUS / 2.0, y, rcol);
//			model.addRawLine (x, y - FOCUS, x, y - FOCUS / 2.0, rcol);
//			model.addRawLine (x, y + FOCUS, x, y + FOCUS / 2.0, rcol);
//			
//			// Draw line based icon
//			drawVehicle (rdesc, x, y, a, rcol);
//			model.addRawText (x + OFFSET, y + OFFSET, robots[i], rcol);			
//		}
		
		// Draw robot path
		drawPath (path, Color.GREEN.darker(), Model2D.THICK);
		
		// Adjust view point and visualization options
		model.setBB (Math.min (vminx, model.getMinX ()), Math.min (vminy, model.getMinY ()), Math.max (vmaxx, model.getMaxX ()), Math.max (vmaxy, model.getMaxY ()));		
		
		if (autoscale)
			canvas.autoScale (boundary);
		
		if (autocenter)
			canvas.autoCenter (cx, cy);
	}
}
