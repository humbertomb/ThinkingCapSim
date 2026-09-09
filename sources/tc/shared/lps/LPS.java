/***************************************************************
 ***
 ***  LPS.java
 ***
 ***  Maintain the information in the local perceptual state
 ***
 ***  Three tasks:
 ***  1. put data from robot into the LPS structures
 ***  2. maintain and clamp a set of LPS objects (LPO structures)
 ***  3. maintain and clamp a circular buffer of range readings
 ***
 ***  Later on, may include calls to perceptual interpretation
 ***  and localization routines
 ***
 ***  (c) 1999 Alessandro Saffiotti
 ***  (c) 2000-2001 Humberto Martinez
 ***************************************************************/

package tc.shared.lps;

import java.io.*;

import tc.vrobot.*;
import tclib.utils.fusion.*;
import tc.shared.lps.lpo.*;
import tc.shared.world.*;

import devices.pos.*;
import wucore.widgets.*;
import wucore.utils.math.*;
import wucore.utils.color.*;

import devices.data.*;

public class LPS extends Object
{
	static public final int				LPO_BUFFER		= 200;				
	static public final int				PPR_BUFFER		= 20;		// Point per range data

	// ----------------------
	// CONVENIENCE DATA
	// ----------------------
	
	// Things that could be stored elsewhere (Is this a hack? Meanwhile leave it here)

	// Robot global position
	public Position						cur;						// Current corrected coordinates (m, m, rad)
	public Position						odom;						// Odometry-based coordinates (m, m, rad)
	public Position						real;						// Real coordinates for ground truth (m, m, rad)
	public Pose							pose;						// INS-based coordinates (rad, rad, rad)
	public double						qlty			= -1.0;		// Quality of current position		
	
	// Signal-level and feature-level sensor fusion
	public boolean[] 					dsignals;					// Digital inputs values
	public boolean[]					dsignals_flg;				// Digital inputs update flags
	public int							pal_switch		= -1;
	
	// -----------------------
	// LPS AND LPO MANAGEMENT
	// -----------------------
	
	// LPS elements
	protected LPO[]						lpos;						// LPS objects
	protected int						lpos_n;						// Number of LPO objects available
	protected RobotDesc					rdesc;						// Robot description
	protected FusionDesc				fdesc;						// Fusion method description

	// LPS update time 
	protected double					time;						// Current control cycle time (ms)
	protected double					tsum;						// Accumulated time updates (ms)
	protected int						tcount;						// Number of time updates
			
	private Matrix3D					rotm;						// Rotation matrix for clamping
	private double						last_x;						// Previous robot coordinates
	private double						last_y;
	private double						last_a;	
	private boolean						firstime;
	
	// Debug
	private PrintWriter					stream			= null;

	// Constructor
	public LPS (RobotDesc rdesc, FusionDesc fdesc)
	{			
		this.rdesc	= rdesc;
		this.fdesc	= fdesc;
		
		if (fdesc != null)
		{
			dsignals		= new boolean [fdesc.MAXDSIG];
			dsignals_flg	= new boolean [fdesc.MAXDSIG];
		}		

		cur				= new Position ();
		odom			= new Position ();
		real			= new Position ();
		pose			= new Pose ();

		rotm		= new Matrix3D ();
		lpos		= new LPO[LPO_BUFFER];
		
		firstime	= true;
		
		// Initialise time calculations
		time		= 0.0;
		tsum		= 0.0;
		tcount		= 0;
	}

	// Accessors
	public final int			lpos_n ()					{ return lpos_n; }
	public final LPO[]			lpos ()						{ return lpos; }
	
	public final RobotDesc		rdesc ()					{ return rdesc; }
	public final FusionDesc		fdesc ()					{ return fdesc; }
	public final double			time ()						{ return time; }
	public final double			avg ()						{ if (tcount > 0) return (tsum / (double) tcount); else return 0.0; }
		
	// Instance methods
	
	/* ---------- Non-standard elements management ---------- */

	public void update (RobotData data, Fusion fusion, Position odom, Position cur, Pose pose)
	{
		int			i;
		
		if ((fusion != null) && (fdesc != null))
		{
			for (i = 0; i < fdesc.MAXDSIG; i++)
			{
				dsignals[i]		= fusion.dsignals[i];
				dsignals_flg[i]	= fusion.dsignals_flg[i];
			}
		}
		
		if (odom != null)			this.odom.set (odom);
		if (cur != null)			this.cur.set (cur);			
		if (data != null)			this.real.set (data.real_x, data.real_y, data.real_a);
		if (pose != null)			this.pose.set (pose);
	}

	/* ---------- Time management ---------- */
	
	public void add_time (double time)
	{
		this.time	= time;
		tsum		+= time;
		tcount ++;
	}
	
	/* -------------- Drawing --------------- */
	
	public void draw (Model2D model, LPOView view)
	{
		int			i;
		
		for (i = 0; i < lpos_n; i++)
			if (lpos[i] != null)
				lpos[i].draw (model, view);
	}

	/* ---------- LPS objects management ---------- */
	public void add (LPO lpo)
	{	
		if (lpos_n >= LPO_BUFFER)				return;
		
		lpos[lpos_n]	= lpo;
		lpos_n ++;
	}

	public LPO find (String label)
	{
		int				i;
		String			str;
		
		for (i = 0; i < lpos_n; i++)
		{
			str	= lpos[i].label ();
			if ((str != null) && (str.equals (label)))
				return lpos[i];
		}
		
		return null;
	}
	
	public void dump ()
	{
		int				i;
		
		System.out.println ("Dumping LPS content ("+ lpos_n + ")");
		for (i = 0; i < lpos_n; i++)
			System.out.println ("\t<" + lpos[i].label () + ">");
	}
	
	public void add_lpos (World map)
	{
		int			i;
		double		x1, y1, ra;
		double		ll, aa;
		double		x, y;
		
		// Draw docks
		for (i = 0; i < map.docks().n(); i++)
		{
			x1 = map.docks().at(i).pos.x ();
			y1 = map.docks().at(i).pos.y ();
			ra = map.docks().at(i).getAng() - Math.PI*0.5;
			
			x	= x1 - cur.x ();
			y	= y1 - cur.y ();
			ll	= Math.sqrt (x * x + y * y);
			aa	= Math.atan2 (y, x);
			x	= ll * Math.cos (aa - cur.alpha ());
			y	= ll * Math.sin (aa - cur.alpha ());

			lpos[i]	= new LPODock (x, y, ra, map.D_LENGHT, map.docks().at(i).label, LPO.MAP);
			lpos[i].active (true);
		}
		lpos_n += map.docks().n();
	}
						
	public void set_lpo (VisionData data)
	{
		int			i;
		double		x, y;
		double		ll, aa;
		Position	pos;

		pos		= new Position ();
		for (i = 0; i < lpos_n; i++)
			if (data.id.equals (lpos[i].label ()))
			{
				// Compute sensor absolute position (where the objects were captured)
				pos.set (cur.x (), cur.y (), cur.alpha ());
				pos.untranslate (data.cpos);
				
				// Compute object absolute positions (where captured)
				x	= pos.x () + data.rho * Math.cos (pos.alpha () + data.phi);
				y	= pos.y () + data.rho * Math.sin (pos.alpha () + data.phi);

				// Compute object relative positions (current robot frame)
				x	= x - cur.x ();
				y	= y - cur.y ();
				ll	= Math.sqrt (x * x + y * y);
				aa	= Math.atan2 (y, x);
				x	= ll * Math.cos (aa - cur.alpha ());
				y	= ll * Math.sin (aa - cur.alpha ());
				
				// Update LPS data
				lpos[i].locate (x, y, 0.0);
				lpos[i].color (ColorTool.fromColorToWColor(data.color));
				//lpos[i].color (data.color);
				
				lpos[i].active (true);
				lpos[i].anchor (1.0);
				lpos[i].ageing (0);
			}
	}
		
	public void update_anchors ()
	{
		int			i;
		
		for (i = 0; i < lpos_n; i++)
		{
			lpos[i].ageing (lpos[i].ageing () + 1);
			if (lpos[i].ageing () > 3)
				lpos[i].anchor (lpos[i].anchor () - 0.025);
		}
	}
	
	/*
	 * Update all objects in the LPS to account for robot's
	 * displacement in the the last control cycle.
	 * Use a unique rotation matrix for this.
	 */
	protected void set_clamp ()
	{
		double		dth, dx, dy;
		double		dxx, dyy;
	
		if (!firstime)
		{
			// Compute increment in absolute position
			dx	= cur.x () - last_x;
			dy	= cur.y () - last_y;
			dth	= Angles.radnorm_180 (cur.alpha () - last_a);
		
		  	// Rotate to local increment
			rotm.toFrame (0.0, 0.0, last_a);
			
			dxx = (rotm.mat[0][0] * dx) + (rotm.mat[0][1] * dy);
			dyy = (rotm.mat[1][0] * dx) + (rotm.mat[1][1] * dy);
		
			rotm.toFrame (dxx, dyy, dth);
		}
		else
		{
			firstime = false;
			rotm.toFrame (0.0, 0.0, 0.0);
		}
		
		last_x	= cur.x ();
		last_y	= cur.y ();
		last_a	= cur.alpha ();
	}
	
	public void clamp (Position pos)
	{
		int			i;
		
		// Set current robot position and motion commands
		cur.set (pos);
		
		// Set current clamp matrix
		set_clamp	();
	  
	  	// Clamp LPO objects
		for (i = 0; i < lpos_n; i++)
			lpos[i].clamp (rotm);		
	}
}

