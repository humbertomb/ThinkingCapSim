/** 
 * Gauge.java
 *
 * Description:		Artificial horizont for aircraft control.
 * @author			Humberto Martinez Barbera
 * @version			1.1
 * @since 			Dec 1999
 */

package wucore.widgets;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import wucore.utils.math.Angles;

public class Horizont extends Gauge 
{
	static public final int					MAXPTS		= 100;			// Number of point per polyline
	
	protected double 						pitch		= 0.0;			// Current pitch value

	// Additional support variables
	private int[]							xps;
	private int[]							yps;
	
	// Constructors
	public Horizont ()
	{
		xps		= new int[MAXPTS];
		yps		= new int[MAXPTS];
		
		this.setPreferredSize (new Dimension(110, 110));
		this.setForeground (Color.black);
		this.setSphere (Color.white);
		
		this.setMax (Angles.PI05);
		this.setMin (-Angles.PI05);
		this.setStep (15.0 * Angles.DTOR);
	}
	
	// Accesors
	public final double 	getRoll ()						{ return value; }
	public final double 	getPitch ()						{ return pitch; }
	
	// Instance methods
	public void setValue (double value)
	{
		super.setValue (Angles.radnorm_90 (value));
	}

	public void setValues (double pitch, double roll)
	{
		this.pitch		= Angles.radnorm_90 (pitch);
		setValue (roll);
	}

	public void paint (Graphics g)
	{
		int			h = Math.min (getHeight(), getWidth ());
		int			w = Math.min (getHeight(), getWidth ());
		int			oh, ow;
		int			mh = h/2, qh = h/12;
		int			mw = w/2, tw = w/3, qw = w/12;
		int			r = (h-10)/2;
		int			i;
		double		k;
		double		rr, dd, aa, ss;
		double		b1, b2;
		int			xi, yi, xf, yf;
		int			xc, yc, xl, yl;
		double		mkr;

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
		
		// Draw horizont container
		g.setColor (this.getBackground ());
		g.fillRect (ow,oh,w,h);
		g.setColor (this.getSphere ());
		g.fillOval (ow+5,oh+5,w-10,h-10);
		
		// Draw artificial horizont sky
		dd	= (pitch / Angles.PI05 * r);	
		rr	= Math.sqrt (r * r - dd * dd);
		xl	= (int) Math.round (rr * Math.cos (-value));
		yl	= (int) Math.round (rr * Math.sin (-value));
		xc	= (int) Math.round (dd * Math.cos (Angles.PI05 - value));
		yc	= (int) Math.round (dd * Math.sin (Angles.PI05 - value));

		g.setColor (Color.blue);
		b1	= Angles.radnorm_180 (Math.atan2 ((yc-yl), (xc-xl)));		
		b2	= Angles.radnorm_180 (Math.atan2 ((yc+yl), (xc+xl)));	
		if (b2 > b1)
			ss	= (b2-b1) / (double) (MAXPTS - 1);
		else
			ss	= (Angles.PI2 - (b1-b2)) / (double) (MAXPTS - 1);
		for (i = 0, aa = b1; i < MAXPTS; i++, aa+=ss)
		{
			xps[i]	= ow+mw + (int) Math.round (r * Math.cos (aa));
			yps[i]	= oh+mh + (int) Math.round (r * Math.sin (aa));
		}
		g.fillPolygon (xps, yps, MAXPTS);
		
		g.setColor (Color.blue.darker ());		
		g.drawLine (ow+mw+xc-xl, oh+mh+yc-yl, ow+mw+xc+xl, oh+mh+yc+yl);

		// Draw artificial horizont airplane and marks
		g.setColor (Color.white);		
		xl	= (int) Math.round (r * Math.cos (-value));
		yl	= (int) Math.round (r * Math.sin (-value));
		g.drawLine (ow+mw-xl, oh+mh-yl, ow+mw+xl, oh+mh+yl);

		xl	= (int) Math.round (qw * 2.0 * Math.cos (-value));
		yl	= (int) Math.round (qw * 2.0 * Math.sin (-value));
		for (k = -4.0; k <= 4.0; k+= 1.0)
		{
			xc	= (int) Math.round (qh * k * Math.cos (Angles.PI05 - value));
			yc	= (int) Math.round (qh * k * Math.sin (Angles.PI05 - value));
			g.drawLine (ow+mw+xc-xl, oh+mh+yc-yl, ow+mw+xc+xl, oh+mh+yc+yl);
		}

		// Draw widget outmost circle and marks
		g.setColor (this.getForeground ());
		g.drawOval (ow+5,oh+5,ow+w-10,oh+h-10);
			
		if (marks)
		{
			for (k = min; k < max; k+= step)
			{
				mkr	= (Angles.PI2 * - k) / (max - min);
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
			g.setColor (this.getForeground ());
			g.drawString (new Integer ((int) Math.round (pitch * Angles.RTOD)).toString (), ow+tw, oh + mh + 2*qh);
		}		
	}
}

/* @(#)Horizont.java */
