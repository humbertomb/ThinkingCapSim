/** 
 * Gauge.java
 *
 * Description:		A simple gauge gadget. Useful for displaying speed, battery, etc.
 * @author			Humberto Martinez Barbera
 * @version			1.1
 * @since 			Dec 1999
 */

package wucore.widgets;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;

public class Gauge extends JComponent 
{
	protected double						max			= 100.0;		// Maximum value of the gauge
	protected double						min			= 0.0;			// Minimum value of the gauge
	protected double 						value		= 0.0;			// Current value of the gauge
	protected double						step		= 10.0;			// Step for drawing marks
	protected boolean						marks		= false;		// Draw gauge marks?
	protected boolean						box			= true;			// Draw gauge value-box?
	protected Color							gcol;						// Background color for gauge sphere

	// Constructors
	public Gauge ()
	{
		this.setPreferredSize (new Dimension(110, 110));
		this.setForeground (Color.black);
		this.setSphere (Color.white);
	}

	// Accessors
	public final void		setSphere (Color gcol)			{ this.gcol = gcol; 	repaint (); }
	public final Color		getSphere ()					{ return gcol; }
	public final void		setMarks (boolean marks)		{ this.marks = marks;	repaint (); }
	public final boolean	isMarks ()						{ return marks; }
	public final void		setBox (boolean box)			{ this.box = box; 		repaint (); }
	public final boolean	isBox ()						{ return box; }
	public final void	 	setStep (double step)			{ this.step = step; }
	public final double 	getStep ()						{ return step; }
	
	public final double 	getValue ()						{ return value; }
	public final double 	getMin ()						{ return min; }
	public final double 	getMax ()						{ return max; }
	
	// Instance methods
	public void setValue (double v)
	{
		if ((v >= min) && (v <= max))
		{
			value = v;
			repaint ();
		}
	}

	public void setMax (double m)
	{
		max = m;
		if (value > max)	setValue (max);
	}
	
	public void setMin (double m)
	{
		min = m;
		if (value < min)	setValue (min);
	}
	
	public void paint (Graphics g)
	{
		int			h = Math.min (getHeight(), getWidth ());
		int			w = Math.min (getHeight(), getWidth ());
		int			oh, ow;
		int			mh = h/2, qh = h/12;
		int			mw = w/2, tw = w/3;
		int			r = (h-10)/2;
		int			xlimit, ylimit;
		double		ii;
		int			xi, yi, xf, yf;
		double		mkr, heading;
		int			cr, cg, cb;

		// Precalculate translation factors
		if (getHeight() >= getWidth ())
		{
			oh	= (h - getWidth ()) / 2;
			ow	= 0;
		}
		else
		{
			oh	= 0;
			ow	= (w - getHeight ()) / 2;
		}
		
		// Draw gauge container
		g.setColor (this.getBackground ());
		g.fillRect (ow,oh,w,h);
		g.setColor (this.getSphere ());
		g.fillOval (ow+5,oh+5,w-10,h-10);
		g.setColor (this.getForeground ());
		g.drawOval (ow+5,oh+5,w-10,h-10);
		g.fillOval (ow+mw-2,oh+mh-2,5,5);
		
		// Draw gauge marks
		if (marks)
		{
			g.setColor (this.getForeground ());
			for (ii = min; ii < max; ii+= step)
			{
				mkr	= (2.0 * Math.PI * - ii) / (max - min);
				xi	= (int) Math.round (mw + r * Math.cos (mkr));
				yi	= (int) Math.round (mh + r * Math.sin (mkr));
				xf	= (int) Math.round (mw + (r - 5) * Math.cos (mkr));
				yf	= (int) Math.round (mh + (r - 5) * Math.sin (mkr));

				g.drawLine (ow+xi, oh+yi, ow+xf, oh+yf);
			}
		}

		// Draw gauge value-box
		if (box)
		{
			g.setColor (this.getSphere ().darker ());
			g.fillRect (ow+tw, oh+mh + qh, tw, 2*qh);
			g.setColor (this.getForeground ());
			g.drawRect (ow+tw, oh+mh + qh, tw, 2*qh);
			g.drawString (new Integer ((int) Math.round (value)).toString (), ow + tw + 5, oh + mh + 2*qh + 5);
		}
		
		// Draw gauge arrow/pointer
		heading = (2.0 * Math.PI * -value) / (max - min);
		xlimit = (int)(mw-(r*Math.sin (heading)));
		ylimit = (int)(mh-(r*Math.cos (heading)));

		cr		= Math.abs (Color.red.getRed () - 255);
		cg		= Math.abs (Color.red.getGreen () - 255);
		cb		= Math.abs (Color.red.getBlue () - 255);
		g.setColor (new Color (cr, cg, cb));
		
		g.drawLine (ow+mw-2,oh+mh-2,ow+xlimit,oh+ylimit);
		g.drawLine (ow+mw+2,oh+mh+2,ow+xlimit,oh+ylimit);
	}
}


/* @(#)Gauge.java */
