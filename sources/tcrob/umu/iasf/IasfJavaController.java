/*
 * (c) 2006 Humberto Martinez
 */
 
package tcrob.umu.iasf;

import tc.runtime.thread.ModuleConfig;

import tc.modules.*;
import tc.shared.lps.lpo.*;
import tc.shared.linda.*;

import wucore.utils.logs.*;

public class IasfJavaController extends Controller
{
	static public final String		PREFFIX			= "CNTL_";
		
	// Controller debug
	protected LogPlot				c_plot;
	protected double[]				c_buffer;
	protected String[]				c_labels;
		
	// Constructors
	public IasfJavaController (ModuleConfig cfg, Linda linda) 
	{
		super (cfg, linda);
	}
	
	// Instance methods
	protected void initialise (ModuleConfig cfg)
	{		
		super.initialise (cfg);
				
		// Initialize debug modules
		c_buffer	= new double[3];
		c_labels	= new String[3];			// the three velocities of the control action
		c_labels[0]	= "vlin";
		c_labels[1]	= "vlat";
		c_labels[2]	= "vrot";
		c_plot		= new LogPlot ("Controller Output", "step", "m/s");	
		if (localgfx)
			openMotionPlot (c_plot, c_labels);	// the window, with its scales as far as the platform goes
	}
			
	/**
	 * The program starts afresh: the interpreter forgets what it had worked out and
	 * there is no goal, no plan and no path any more (RESET). Whether it runs from
	 * the first cycle again is what AUTO says, as when the module was set up.
	 */
	protected void reset ()
	{
		super.reset ();
	}

	protected void controller () 
	{
		LPOSensorGroup		group;
		double				group0, group1, group2, group3, group4;
		double				vlin, vlat, vrot;

		/* ----------- */
		/* CONTROLLER  */
		/* ----------- */
				
		// Read sensor values
		group = (LPOSensorGroup) lps.find ("Group");
		group0 = group.range[0];
		group1 = group.range[1];
		group2 = group.range[2];
		group3 = group.range[3];
		group4 = group.range[4];
		
		// Set action values
		vlin	= 0.30;
		vlat	= 0.0;
		vrot	= 0.0;

		if (group2 < 0.5)
		{
			vlin = 0.0;
			vrot = (group1 > group2 ? 45.0 : -45.0);
		}
		else if (group1 < 0.5)
		{
			vlin = 0.1;
			vrot = -30.0;			
		}
		else if (group0 < 0.5)
		{
			vlin = 0.2;
			vrot = -15.0;			
		}
		else if (group3 < 0.5)
		{
			vlin = 0.1;
			vrot = 30.0;			
		}
		else if (group4 < 0.5)
		{
			vlin = 0.2;
			vrot = 15.0;			
		}
		
		// Set action
		setMotion (vlin, vlat, vrot);

		// Plot current control commands
		if (localgfx)
		{
			motionValues (c_buffer, vlin, vlat, vrot);
			c_plot.draw (c_buffer);	
		}
	}
		
	protected void close_gfx ()
	{
		if (c_plot != null)		c_plot.close ();
	}

	public void step (long ctime) 
	{
		if (state != RUN)						return;
		
		if (!auto || (lps == null))				return;
				
		// Run controller
		controller ();
	}
}

