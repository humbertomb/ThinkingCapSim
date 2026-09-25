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
	protected double				yspan;						// the least a scale left to its values spans (0: no least)
	protected double				rspan;
	protected int					rfirst;						// the first line of the right scale, -1 for none
	protected String				rlabel;						// ... what it is measured in
	protected double				rmin;
	protected double				rmax;

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
		yspan			= 0.0;
		rspan			= 0.0;
		rfirst			= -1;
		rlabel			= null;
		rmin			= -1.0;
		rmax			= 1.0;
		
	}
	
	// Accessors
	public final void		setImpulses (boolean impulses)			{ this.impulses = impulses; }
	public final void		setYRange (double ymin, double ymax)	{ this.ymin = ymin; this.ymax = ymax; }
	/** Asks for what every line is worth to be written beside the plot, under this title (null: none). */
	public final void		setValues (String title)				{ this.values = title; }
	/** Draws the lines from this one on against a second scale on the right, of its own unit (-1: none). */
	public final void		setRightAxis (int first, String label)	{ this.rfirst = first; this.rlabel = label; }
	/** The least a scale left to its own values spans, so that values that are all nothing are still read against something. */
	public void setSpans (double left, double right)
	{
		this.yspan	= left;		this.rspan	= right;
		if (plot == null)				return;

		final PlotWindow	p = plot;
		final double		l = left, r = right;
		onEventThread (new Runnable ()
		{
			public void run ()			{ p.setSpans (l, r); }
		});
	}
	/** What that second scale covers. */
	public final void		setRightRange (double ymin, double ymax){ this.rmin = ymin; this.rmax = ymax; }
	
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
				plot.setRightAxis (rfirst, rlabel);				// after the legend: it splits the lines
				plot.setRightRange (rmin, rmax);
				plot.setImpulses (impulses);
				plot.setSpans (yspan, rspan);
				plot.open ();
			}
		});
	}
	
	/**
	 * What the scales cover, said again once it is known: a plot may be opened
	 * before whoever draws on it knows how far its values go (a controller opens
	 * its windows before the description of the platform has arrived), and this is
	 * how the scales are put right without the window being built again. A range of
	 * nothing at all is left to the values themselves.
	 */
	public void rescale (double ymin, double ymax, double rmin, double rmax)
	{
		this.ymin	= ymin;		this.ymax	= ymax;
		this.rmin	= rmin;		this.rmax	= rmax;

		if (plot == null)				return;

		final PlotWindow	p = plot;
		final double		y0 = ymin, y1 = ymax, r0 = rmin, r1 = rmax;
		onEventThread (new Runnable ()
		{
			public void run ()			{ p.setYRange (y0, y1);		p.setRightRange (r0, r1); }
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

