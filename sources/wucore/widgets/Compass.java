/** 
 * Compass.java
 *
 * Description:		A widget to show heading on a compass.
 * @author			Humberto Martinez Barbera
 * @version			1.2
 * @since 			Dec 1999
 */

package wucore.widgets;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import wucore.utils.math.Angles;

public class Compass extends Gauge 
{
	// Constructors
	public Compass ()
	{
		this.setPreferredSize (new Dimension(110, 110));
		this.setForeground (Color.black);
		this.setSphere (Color.white);
		
		this.setMax (Math.PI);
		this.setMin (-Math.PI);
		this.setStep (15.0 * Angles.DTOR);
	}

	// Instance methods
	public void setValue (double v)
	{
		super.setValue (Angles.radnorm_180 (v));
	}

	public void paint (Graphics g)
	{
		int			h = Math.min (getHeight(), getWidth ());
		int			w = Math.min (getHeight(), getWidth ());
		int			oh, ow;
		int			mh = h/2;
		int			mw = w/2;
		int			r = (h-10)/2;
		int			xlimit, ylimit;
		int			xi, yi, xf, yf;
		double		k;
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
		
		// Draw compass container
		g.setColor (this.getBackground ());
		g.fillRect (ow,oh,w,h);
		g.setColor (this.getSphere ());
		g.fillOval (ow+5,oh+5,w-10,h-10);
		g.setColor (this.getForeground ());
		g.drawOval (ow+5,oh+5,w-10,h-10);
		g.fillOval (ow+mw-2,oh+mh-2,5,5);
		
		// Draw compass marks
		if (marks)
		{
			g.setColor (this.getForeground ());
			for (k = min; k < max; k+= step)
			{
				mkr	= (2.0 * Math.PI * -k) / (max - min);
				xi	= (int) Math.round (mw + r * Math.cos (mkr));
				yi	= (int) Math.round (mh + r * Math.sin (mkr));
				xf	= (int) Math.round (mw + (r - 5) * Math.cos (mkr));
				yf	= (int) Math.round (mh + (r - 5) * Math.sin (mkr));

				g.drawLine (ow+xi, oh+yi, ow+xf, oh+yf);
			}
		}

		// Draw compass labels
		g.setColor (this.getForeground ());
		g.drawString ("N", ow+mw-2,oh+15);
		g.drawString ("E", ow+w-15,oh+mh);
		g.drawString ("S", ow+mw-2,oh+h-8);
		g.drawString ("W", ow+8,oh+mh);
	
		// Draw compass arrow
		heading = Angles.radnorm_180 (value - Angles.PI05);
		
		xlimit = (int)(mw-(r*Math.sin(heading)));
		ylimit = (int)(mh-(r*Math.cos(heading)));

		cr		= Math.abs (Color.red.getRed () - 255);
		cg		= Math.abs (Color.red.getGreen () - 255);
		cb		= Math.abs (Color.red.getBlue () - 255);
		g.setColor (new Color (cr, cg, cb));
		
		g.drawLine (ow+mw-2,oh+mh-2,ow+xlimit,oh+ylimit);
		g.drawLine (ow+mw+2,oh+mh+2,ow+xlimit,oh+ylimit);
	}
}
