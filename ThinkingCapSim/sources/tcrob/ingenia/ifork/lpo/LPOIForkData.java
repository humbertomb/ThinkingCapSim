/*
 * Created on 10-feb-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tcrob.ingenia.ifork.lpo;

import java.io.*;
import java.awt.*;

import tc.shared.lps.lpo.*;

import wucore.widgets.*;
import wucore.utils.math.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class LPOIForkData extends LPO implements Serializable
{
	// Object specific information
	protected double				vm;			// Traction motor displacement (m)
	protected double				del;		// Steering motor position (rad)
	protected double				fork;		// Lifting motor height (m)
	protected int					pal_switch=-1;	// Lifting motor height (m)
	
	// Constructor
	public LPOIForkData (String label, int source)
	{			
		super (0.0, 0.0, 0.0, label, source);
	}
	
	// Accessors
	public final double			vm ()			{ return vm; }
	public final double			del ()			{ return del; }
	public final double			fork ()			{ return fork; }
	public final int			pal_switch ()	{ return pal_switch; }
	
	// Instance methods
	public void set (double vm, double del, double fork)
	{
		this.vm		= vm;
		this.del		= del;
		this.fork	= fork;
	}
	
	public void set (double vm, double del, double fork, int pal_switch)
	{
		this.vm			= vm;
		this.del		= del;
		this.fork		= fork;
		this.pal_switch 	= pal_switch;
	}
	
	protected String format (double val)
	{
		return Double.toString (Math.round (val * 1000.0) / 1000.0);
	}
	
	public void draw (Model2D model, LPOView view)
	{
		if (!active)			return;
					
		model.addRawArrow (0.0, 0.0, vm * 10.0, view.rotation + del, Color.ORANGE.darker());
		model.addRawText (2.0, -3.0, "vm = "+format (vm)+" m/s", Color.ORANGE.darker());
		model.addRawText (2.0, -4.5, "del = "+format (del*Angles.RTOD)+" deg", Color.ORANGE.darker());
		model.addRawText (2.0, -6.0, "palet = "+pal_switch, Color.ORANGE.darker());
	}
}
