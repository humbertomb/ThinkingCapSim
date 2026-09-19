/*
 * (c) 2000-2002 Humberto Martinez
 */

package tc.gui.visualization;

import java.awt.*;

import tc.shared.lps.*;
import tc.shared.lps.lpo.*;
import tc.vrobot.*;
import tclib.navigation.pathplanning.*;

import devices.pos.*;
import wucore.widgets.*;
import wucore.utils.geom.*;

public class LPS2D extends World2D
{	
	public static final double			SHORT_MARK		= 0.025;
	public static final double			LONG_MARK		= 0.075;
	public static final double			STEP_MARK		= 0.1;
	public static final double			RADIUS			= 0.05;					// metres (small circle)
	
	// LPS HUD's labels
	public static final int				H_TIME			= 0;
	
	public static final double			MARKS			= 80.0;					// as many marks as an axis carries, about

	// LPS objects drawing policies
	protected LPOView					view;									// Viewing properties
	protected double					reach			= 3.0;					// how far it reads on its own (m)
	
	/* Constructors */
	public LPS2D (Model2D model) 
	{
		this.initialise (model, null, null, null);
		this.initialise ();
	}
	
	/* Accessors */
	public void			orientation (double rotation)				{ view.rotation = rotation; }

	/** Told how far to draw, that is how far it reads: a wider view is asked for on top of it. */
	public void boundary (double bound)
	{
		super.boundary (bound);
		reach	= bound;
	}
	
	/* Instance methods */
	protected void initialise () 
	{
		// Setup viewing properties
		view			= new LPOView ();
		
		// Set up window behaviour
		boundary (3.0);
		clipping (true);
		autoscale (false);
	}
	
	/*
	 * Update the LPS window, by putting both LPOs
	 * and range data.
	 */
	public void update (LPS lps, Path path)
	{
		int				i, k;
		double			xx, yy, aa;
		double			xi, yi, xf, yf;
		double			rho, phi;
		Line2[]			icon;
		RobotDesc		rdesc;
		double			cmax;
		LPORangeBuffer	rbuffer;
		
		if (lps == null) return;
		
		// Initialise component's model
		model.clearView ();
		
		// Compute maximum LPS window bounding box
		rbuffer	= (LPORangeBuffer) lps.find ("RBuffer");		// This should be "smarter"
		if (rbuffer != null)
			// Check for the boundary size (WARNING: This is a hack, and MUST be modified!!!!)
			reach	= Math.max (Math.max (rbuffer.getRangeSON (), rbuffer.getRangeLRF ()), reach);
		// as far as it reads, and as much wider as the view was zoomed out: the
		// boundary goes out with the view, so that what lies beyond the reach of the
		// sensors is kept instead of being cut away into empty margins
		cmax		= reach * widening ();
		MAXX_BNDRY	= cmax;			MAXY_BNDRY = cmax;
		MINX_BNDRY	= -cmax;		MINY_BNDRY = -cmax;

		view.min.set (-cmax, -cmax, -cmax);
		view.max.set (cmax, cmax, cmax);
		
		// Initialise auxiliary constants
		rdesc	= lps.rdesc ();
		fdesc	= lps.fdesc ();
		icon	= rdesc.icon;
				
		if (!autoscale)
		{
			// Draw bounding box
			model.addRawLine (MINX_BNDRY, MINY_BNDRY, MINX_BNDRY, MAXY_BNDRY, Color.BLACK);
			model.addRawLine (MINX_BNDRY, MAXY_BNDRY, MAXX_BNDRY, MAXY_BNDRY, Color.BLACK);
			model.addRawLine (MAXX_BNDRY, MAXY_BNDRY, MAXX_BNDRY, MINY_BNDRY, Color.BLACK);
			model.addRawLine (MAXX_BNDRY, MINY_BNDRY, MINX_BNDRY, MINY_BNDRY, Color.BLACK);
			
			// Draw central cross
			model.addRawLine (0.0, MINY_BNDRY, 0.0, MAXY_BNDRY, Color.BLACK);
			model.addRawLine (MINX_BNDRY, 0.0, MAXX_BNDRY, 0.0, Color.BLACK);
			// the marks are spaced by a tenth of a metre while that leaves an axis
			// readable, and by ten times as much each time it would not: a view taken
			// far out is ruled in metres, and then in tens of them
			double	step = STEP_MARK;
			double	shrt, lng;
			while ((2.0 * cmax / step) > MARKS)		step *= 10.0;
			// and they are as long on the screen as they always were, which is as much
			// longer in metres as the view was widened
			shrt	= SHORT_MARK * widening ();
			lng		= LONG_MARK * widening ();
			for (xx = MINX_BNDRY; xx <= MAXX_BNDRY; xx += step)
			{
				yy = shrt;
				if (Math.abs (Math.IEEEremainder (xx, 10.0 * step)) < (step / 10.0))	yy = lng;

				model.addRawLine (xx, -yy, xx, yy, Color.BLACK);
			}
			for (yy = MINY_BNDRY; yy <= MAXY_BNDRY; yy += step)
			{
				xx = shrt;
				if (Math.abs (Math.IEEEremainder (yy, 10.0 * step)) < (step / 10.0))	xx = lng;

				model.addRawLine (-xx, yy, xx, yy, Color.BLACK);
			}
		}
		
		// Draw desired path
		if ((path != null) && drawpath)
			switch (path.type ())
			{
			case GridPath.BSPLINE:
				xi	= path.first ().x () - lps.cur.x ();
				yi	= path.first ().y () - lps.cur.y ();
				
				for (Position pos = path.next (); pos != null; pos = path.next ())
				{
					xf	= pos.x () - lps.cur.x ();
					yf	= pos.y () - lps.cur.y ();
					
					if (!autoscale && clipping && ((xi < -cmax) || (xi > cmax) || (yi < -cmax) || (yi > cmax)))		continue;
					
					model.addRawTransRotLine (xi, yi, xf, yf, 0.0, 0.0, view.rotation - lps.cur.alpha (), Color.ORANGE);
					
					xi	= xf;
					yi	= yf;
				}
				break;
				
			case GridPath.POLYLINE:
			default:
				k = 0;
				for (Position pos = path.first (); pos != null; pos = path.next (), k++)
				{
					if (k % 2 != 0)			continue;
					
					xx	= pos.x () - lps.cur.x ();
					yy	= pos.y () - lps.cur.y ();
					aa	= pos.alpha () - lps.cur.alpha ();
					
					rho	= Math.sqrt (xx * xx + yy * yy);
					phi	= Math.atan2 (yy, xx);
					xx	= rho * Math.cos (view.rotation + phi - lps.cur.alpha ());
					yy	= rho * Math.sin (view.rotation + phi - lps.cur.alpha ());				
					
					if (!autoscale && clipping && ((xx < -cmax) || (xx > cmax) || (yy < -cmax) || (yy > cmax)))		continue;
					
					model.addRawArrow (xx, yy, path.step () * 0.75, view.rotation + aa, Color.ORANGE);
				}
			}
		
		// Draw active LPOs, clipping into the LPS viewing area
		view.verbose	= drawartifacts;
		lps.draw (model, view);
		
		// Draw the real robot
		if (icon == null)
		{
			model.addRawCircle (0.0, 0.0, rdesc.RADIUS, Color.RED);
			model.addRawArrow (0.0, 0.0, rdesc.RADIUS, view.rotation, Color.RED);
		}
		else
			for (i = 0; i < icon.length; i++)
				model.addRawRotLine (icon[i], view.rotation, Color.RED);
		
		// Draw movement commands (speed and turn)
		/*
		 af = Math.min (MAXX_BNDRY - MINX_BNDRY, MAXY_BNDRY - MINY_BNDRY) * 0.4;
		 xx = lps.speed () / rdesc.Vmax * af;
		 model.addRawArrow (0.0, 0.0, xx, view.rotation, Color.ORANGE_DARK);
		 xx = lps.turn () / rdesc.Rmax * af;
		 model.addRawArrow (0.0, 0.0, xx, view.rotation - Math.PI / 2.0, Color.ORANGE_DARK);
		 */
		// Set clipping region to the boundaries of the LPS
		if (!autoscale)
			model.setBB (MINX_BNDRY, MINY_BNDRY, MAXX_BNDRY, MAXY_BNDRY);
	}
}

