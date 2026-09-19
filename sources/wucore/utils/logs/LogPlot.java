/*
 * (c) 2003 Humberto Martinez
 */
 
package wucore.utils.logs;

import wucore.gui.*;

public class LogPlot implements ChildWindowListener
{
	protected PlotWindow			plot;						// Log output window
	protected String				tittle;
	protected String				xlabel;
	protected String				ylabel;
	protected double				ymin;
	protected double				ymax;
	protected boolean				impulses;
	protected String				values;						// title of the panel of values, when one is asked for

	// Constructors
	protected LogPlot ()
	{
	}
	
	public LogPlot (String tittle, String xlabel, String ylabel)
	{
		this.tittle	= tittle;
		this.xlabel	= xlabel;
		this.ylabel	= ylabel;
		
		plot			= null;
		ymin			= -1.0;
		ymax			= 1.0;
		impulses		= false;
		values			= null;
		
	}
	
	// Accessors
	public final void		setImpulses (boolean impulses)			{ this.impulses = impulses; }
	public final void		setYRange (double ymin, double ymax)	{ this.ymin = ymin; this.ymax = ymax; }
	/** Asks for what every line is worth to be written beside the plot, under this title (null: none). */
	public final void		setValues (String title)				{ this.values = title; }
	
	// Instance methods

	/**
	 * Opens the plot window. Swing components must be created on the event
	 * thread: doing it from a module thread deadlocks against the AWT tree
	 * lock while another window is laying out (the GUI freezes, no exception).
	 */
	public void open (final String[] labels)
	{
		onEventThread (new Runnable ()
		{
			public void run ()
			{
				if (plot == null)				plot	= new PlotWindow (LogPlot.this, tittle);
				
				plot.setValues (values);						// before the legend: the rows are named by it
				plot.setLegend (labels);
				plot.setLabels (xlabel, ylabel);
				plot.setYRange (ymin, ymax);
				plot.setImpulses (impulses);
				plot.open ();
			}
		});
	}
	
	public void close ()
	{
		if (plot == null)				return;
		
		final PlotWindow	p = plot;
		onEventThread (new Runnable ()
		{
			public void run ()				{ p.close (); }
		});
	}

	static private void onEventThread (Runnable r)
	{
		if (javax.swing.SwingUtilities.isEventDispatchThread ())
			r.run ();
		else
			try { javax.swing.SwingUtilities.invokeAndWait (r); } catch (Exception e) { e.printStackTrace (); }
	}
	
	public void draw (double[] buffer)
	{
		if (plot == null)				return;
			
		plot.updateData (buffer);	
	}
	
	public void draw (double[][] buffer, int n, int m)
	{
		if (plot == null)				return;
			
		plot.updateData (buffer, n, m);	
	}
	
	public void childClosed (Object window)
	{
		if (window instanceof PlotWindow)		plot = null;
	}
}

