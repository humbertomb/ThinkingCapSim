/*
 * Created on 28-ene-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package devices.gui;

import java.awt.*;
import javax.swing.*;

import devices.data.*;

import wucore.utils.math.*;

/**
 * @author Juan Pedro Canovas Qui–onero
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class Satellites extends JComponent
{
	// Visualization components
	protected Color							gcol;						// Background color for gauge sphere

	// Data components
	protected SatelliteData[]				sats;
	
	// Constructors
	public Satellites ()
	{
		this.setPreferredSize (new Dimension(190, 190));
		this.setForeground (Color.black);
		this.setSphere (Color.white);
	}

	// Accessors
	public final void		setSphere (Color gcol)			{ this.gcol = gcol; 	repaint (); }
	public final Color		getSphere ()					{ return gcol; }
	
	// Instance methods
	public void update (SatelliteData[] sats)
	{
		this.sats	= sats;
		repaint ();
	}
	
	public void paint (Graphics g)
	{
		int				i;
		int				h = Math.min (getHeight(), getWidth ());
		int				w = Math.min (getHeight(), getWidth ());
		int				oh, ow;
		int				mh = h/2;
		int				mw = w/2;
		int				xi, yi;
		SatelliteData	sat;

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
				
		// Draw satellite canvas			TODO make everything re-scalable
		g.setColor (this.getForeground ());
		g.drawOval (mw-90,mh-90,180,180);
		g.drawOval (mw-60,mh-60,120,120);
		g.drawOval (mw-30,mh-30,60,60);
		g.drawLine (mw-95,mh,mw+95,mh);
		g.drawLine (mw,mh-95,mw,mh+95);
		g.drawLine (mw-80,mh-80,mw+80,mh+80);
		g.drawLine (mw+80,mh-80,mw-80,mh+80);
		
		g.setColor(Color.blue);
		g.drawString ("N",mw-5,mh-95);
		g.drawString ("S",mw-5,mh+95);
		g.drawString ("E",mw+95,mh);
		g.drawString ("W",mw-95,mh);
		
		g.setFont (new Font((g.getFont()).getFontName(), (g.getFont()).getStyle(), (g.getFont()).getSize()-4)); 
		g.drawString("90",mw-5,mh-5);
		g.drawString("60",mw-5,mh-25);
		g.drawString("30",mw-5,mh-55);
		g.drawString("0",mw-5,mh-85);

		 // Draw
		if (sats != null)
			for (i=0; i < sats.length; i++)
			{
				sat = sats[i];
        
				if ((sat == null) || !sat.valid)			continue;
        
				xi	= (int)((90.0-sat.elevation)*Math.sin(sat.azimuth*Angles.DTOR));
				yi	= (int)((90.0-sat.elevation)*Math.cos(sat.azimuth*Angles.DTOR));

				g.setColor(Color.blue);
				g.fillOval (mw-xi,mh-yi,10,10);

				g.setColor(Color.red);
				g.drawString(""+sat.prn,mw-xi,mh-yi);
			}
	}
}
