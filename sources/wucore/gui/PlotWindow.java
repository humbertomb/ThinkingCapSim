/*
 * (c) 2000-2002 Humberto Martinez
 * (c) 2026 Humberto Martinez Barbera
 */

package wucore.gui;

import java.awt.*;
import java.io.*;
import javax.swing.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * A window that draws a few values as they go by: whoever has them hands them
 * over a cycle at a time ({@link #updateData}) and the last {@link #POINTS} of
 * each are what is shown.
 *
 * The lines can be drawn against two scales of their own: the ones of the left
 * axis and, from whichever line {@link #setRightAxis} says, the ones of the
 * right one, each with its own unit and its own range. That is how velocities in
 * metres a second and a turn rate in degrees a second are read off the same
 * picture, each in its own unit and none of them normalised.
 *
 * Beside the plot there can be a panel of values ({@link #setValues}), which is
 * what every line is worth right now, for what cannot be read off the picture.
 */
public class PlotWindow extends JFrame
{
	private static final long		serialVersionUID = 1L;

	/** How many points of every line are kept. */
	static public int				POINTS		= 50;
	static public final int			OFFSET		= 50;
	static public final int			WIDTH		= 584;						// the plot on its own
	static public final int			HEIGHT		= 300;
	static public final int			VALUES_W	= 175;						// and what a panel of values adds to it (a third)
	static public final Color		C_VALUE		= new Color (226, 226, 226);	// the ground a value is read off

	/** The colours of the lines, in the order they are given. */
	static public final Color[]		COLOURS		= { new Color (204, 0, 0), new Color (0, 0, 204), new Color (0, 153, 153),
													new Color (204, 102, 0), new Color (102, 0, 153), new Color (0, 128, 0),
													new Color (153, 102, 51), new Color (204, 0, 153) };

	static public String			PSUFFIX		= ".plot";
	static private int				counter		= 0;

	protected JFreeChart			chart;
	protected XYPlot				plot;
	protected ChartPanel			panel;
	protected XYSeriesCollection	left		= new XYSeriesCollection ();		// the lines of the left axis
	protected XYSeriesCollection	right		= new XYSeriesCollection ();		// ... and of the right one
	// the very same lines again, which is what the curve over a plot of impulses is
	// drawn from: a dataset of its own, because a plot draws one dataset once
	protected XYSeriesCollection	ljoin		= new XYSeriesCollection ();
	protected XYSeriesCollection	rjoin		= new XYSeriesCollection ();
	protected String[]				legends		= new String[0];
	protected int					onright		= -1;						// the first line of the right axis, -1 for none
	protected int[]					rlines		= new int[0];				// or the very lines of it, when they are not the last ones
	protected int[]					place		= new int[0];				// where every line sits in the dataset it went to
	protected int[]					lmap		= new int[0];				// and which line every place of each dataset is
	protected int[]					rmap		= new int[0];
	// the colour a line is asked to be drawn in, when it is not the one of its place
	protected java.util.HashMap<Integer, Color>	tints = new java.util.HashMap<Integer, Color> ();
	protected boolean				impulses;
	protected ChildWindowListener	parent		= null;

	protected int					curx		= 0;
	protected PrintWriter			stream		= null;

	// the values of the lines, written beside the plot when they are asked for
	protected String				vtitle		= null;
	protected JPanel				vpanel		= null;
	protected JTextField[]			vfields		= null;

	protected PlotWindow ()
	{
		chart	= ChartFactory.createXYLineChart (null, null, null, left, PlotOrientation.VERTICAL, true, true, false);
		plot	= chart.getXYPlot ();
		curx	= 0;

		chart.setBackgroundPaint (Color.WHITE);
		chart.getLegend ().setPosition (RectangleEdge.RIGHT);
		legendOrder ();
		plot.setBackgroundPaint (Color.WHITE);
		plot.setDomainGridlinePaint (new Color (200, 200, 200));
		plot.setRangeGridlinePaint (new Color (200, 200, 200));
		plot.setDataset (0, left);
		plot.setRenderer (0, lines ());
		plot.getDomainAxis ().setAutoRange (true);
		((NumberAxis) plot.getDomainAxis ()).setAutoRangeIncludesZero (false);
		((NumberAxis) plot.getRangeAxis ()).setAutoRangeIncludesZero (true);
	}

	public PlotWindow (String title)
	{
		this ();

		init_interface (title);
		this.parent = null;

		counter ++;
	}

	public PlotWindow (ChildWindowListener parent, String title)
	{
		this ();

		init_interface (title);
		this.parent = parent;

		counter ++;
	}

	private void init_interface (String title)
	{
		setVisible (false);
		setTitle (title);
		setLocation (250 + counter * OFFSET, counter * OFFSET);

		panel	= new ChartPanel (chart);
		panel.setPreferredSize (new Dimension (WIDTH, HEIGHT));
		panel.setMouseWheelEnabled (true);
		getContentPane ().add ("Center", panel);
		pack ();
		setSize (WIDTH, HEIGHT);

		SymWindow aSymWindow = new SymWindow();
		this.addWindowListener(aSymWindow);
	}

	/**
	 * Names the lines in the legend in the order they were given, and not in the
	 * order of the scales they are read against: a plot draws the lines of the left
	 * scale and then the ones of the right one, and with the two units coming one
	 * after the other in the legend that would name them out of order.
	 */
	private void legendOrder ()
	{
		if (chart.getLegend () == null)			return;

		chart.getLegend ().setSources (new org.jfree.chart.LegendItemSource[]
		{
			new org.jfree.chart.LegendItemSource ()
			{
				public org.jfree.chart.LegendItemCollection getLegendItems ()
				{
					org.jfree.chart.LegendItemCollection	all = new org.jfree.chart.LegendItemCollection ();

					for (int i = 0; i < legends.length; i++)
					{
						int					ds = isRight (i) ? 1 : 0;
						XYItemRenderer		r = plot.getRenderer (ds);

						if ((r == null) || (i >= place.length))		continue;

						org.jfree.chart.LegendItem	item = r.getLegendItem (ds, place[i]);

						if (item != null)		all.add (item);
					}
					return all;
				}
			}
		});
	}

	/** How the lines are drawn: as lines, or as stems from zero when they are impulses. */
	protected XYItemRenderer lines ()
	{
		if (impulses)
		{
			XYBarRenderer	r = new XYBarRenderer (0.0);

			r.setShadowVisible (false);
			r.setBarPainter (new org.jfree.chart.renderer.xy.StandardXYBarPainter ());
			r.setMargin (0.85);									// a stem, and not a bar as wide as the step
			r.setDrawBarOutline (false);
			return r;
		}

		XYLineAndShapeRenderer	r = new XYLineAndShapeRenderer (true, false);

		r.setDefaultStroke (new BasicStroke (1.4f));
		r.setAutoPopulateSeriesStroke (false);
		return r;
	}

	/**
	 * How the curve over a plot of impulses is drawn: a thinner line, of the colour
	 * of the impulses it joins, and not named again in the legend, which already
	 * names them once.
	 */
	protected XYItemRenderer join ()
	{
		XYLineAndShapeRenderer	r = new XYLineAndShapeRenderer (true, false);

		r.setDefaultStroke (new BasicStroke (1.0f));
		r.setAutoPopulateSeriesStroke (false);
		r.setDefaultSeriesVisibleInLegend (false);
		return r;
	}

	/**
	 * The curve that joins the tops of the impulses: the very same lines, drawn a
	 * second time as a line, and over the impulses rather than under them. An
	 * impulse says what a value is worth at that cycle, and the curve says where it
	 * is going, which one impulse beside another does not tell.
	 *
	 * With no impulses there is nothing to join, and the lines are the curve.
	 */
	protected void joins ()
	{
		plot.setRenderer (2, null);		plot.setDataset (2, null);
		plot.setRenderer (3, null);		plot.setDataset (3, null);
		if (!impulses)
		{
			plot.setDatasetRenderingOrder (org.jfree.chart.plot.DatasetRenderingOrder.REVERSE);
			return;
		}

		plot.setRenderer (2, join ());
		plot.setDataset (2, ljoin);
		plot.mapDatasetToRangeAxis (2, 0);
		if (plot.getRangeAxis (1) != null)
		{
			plot.setRenderer (3, join ());
			plot.setDataset (3, rjoin);
			plot.mapDatasetToRangeAxis (3, 1);
		}
		// the impulses first and the curve over them, and not the other way about
		plot.setDatasetRenderingOrder (org.jfree.chart.plot.DatasetRenderingOrder.FORWARD);
	}

	/** Gives every line of a dataset its colour, which is the colour of the line and not of its place in the dataset. */
	protected void colours (XYItemRenderer r, int[] map)
	{
		if ((r == null) || (map == null))		return;
		for (int i = 0; i < map.length; i++)
			r.setSeriesPaint (i, colour (map[i]));
	}

	public void open ()
	{
		open (null);
	}

	public void open (String name)
	{
		setVisible (true);

		dump_stop ();
		if (name != null)	dump_start (name + PSUFFIX, "Data Plotting");
	}

	public synchronized void close ()
	{
		dump_stop ();

		if (parent != null)
			parent.childClosed (this);

		setVisible (false);
		dispose ();
	}

	/**
	 * Asks for a panel of values beside the plot, under the title given (null
	 * asks for none, and it is built once): what every line of the plot is worth
	 * right now, as a pair of its label and its value, which a plot of impulses
	 * does not let anybody read off the picture.
	 *
	 * The window is made a third wider for it, so that the plot keeps the room it
	 * had. The values themselves are not to be typed in.
	 */
	public void setValues (String title)
	{
		JScrollPane		sp;

		if ((title == null) || (vtitle != null))		return;
		vtitle	= title;
		vpanel	= new JPanel (new GridBagLayout ());
		vpanel.setBorder (BorderFactory.createTitledBorder (title));
		sp		= new JScrollPane (vpanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
									ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		sp.setBorder (BorderFactory.createEmptyBorder ());
		sp.setPreferredSize (new Dimension (VALUES_W, HEIGHT));
		getContentPane ().add ("East", sp);
		setSize (getWidth () + VALUES_W, getHeight ());
		validate ();
		if (legends.length > 0)		buildValues (legends);
	}

	/** What the lines are called, in the order they are given to {@link #updateData}. */
	public void setLegend (String[] labels)
	{
		legends	= (labels != null) ? labels : new String[0];
		series ();
		if (vpanel != null)			buildValues (legends);
	}

	/**
	 * Draws the lines from the one given on against a second scale on the right,
	 * of its own unit and its own range: -1 for none, which is where it starts.
	 *
	 * @param first		the first line of the right axis
	 * @param label		the unit of that axis
	 */
	public void setRightAxis (int first, String label)
	{
		rlines	= new int[0];
		onright	= first;
		rightAxis (label);
	}

	/**
	 * The same, for lines that are not the last ones: which lines of the legend are
	 * read against the right scale, whichever they are. That is what a plot of two
	 * units needs when they come one after the other -- a velocity and a distance
	 * against the left scale, a turn rate and an angle against the right one -- and
	 * the legend is still read in the order it was given.
	 *
	 * @param which		the lines of the right axis, by their place in the legend (none: no right axis)
	 * @param label		the unit of that axis
	 */
	public void setRightAxis (int[] which, String label)
	{
		rlines	= (which != null) ? which.clone () : new int[0];
		onright	= -1;
		rightAxis (label);
	}

	/** Builds the second scale and the dataset that is drawn against it, or takes them away. */
	private void rightAxis (String label)
	{
		if (!anyRight () || (label == null))
		{
			plot.setRangeAxis (1, null);
			plot.setDataset (1, null);
			joins ();
			series ();
			return;
		}

		NumberAxis		axis = new NumberAxis (label);

		axis.setAutoRangeIncludesZero (true);
		plot.setRangeAxis (1, axis);
		plot.setRangeAxisLocation (1, org.jfree.chart.axis.AxisLocation.BOTTOM_OR_RIGHT);
		plot.setDataset (1, right);
		plot.setRenderer (1, lines ());
		plot.mapDatasetToRangeAxis (1, 1);
		joins ();
		series ();
	}

	/** Whether there is a right scale at all. */
	protected boolean anyRight ()
	{
		return (rlines.length > 0) || (onright >= 0);
	}

	/** Whether this line of the legend is read against the right scale. */
	protected boolean isRight (int i)
	{
		if (rlines.length > 0)
		{
			for (int k = 0; k < rlines.length; k++)		if (rlines[k] == i)		return true;
			return false;
		}
		return (onright >= 0) && (i >= onright);
	}

	/**
	 * Asks for a line to be drawn in a colour of its own, whatever the colour of its
	 * place would be (null: the one of its place again).
	 *
	 * @param line		the line, by its place in the legend
	 * @param tint		the colour to draw it in
	 */
	public void setColour (int line, Color tint)
	{
		if (tint != null)		tints.put (Integer.valueOf (line), tint);
		else					tints.remove (Integer.valueOf (line));
		paints ();
	}

	/** The colour a line is drawn in: the one it was asked for, or the one of its place. */
	protected Color colour (int line)
	{
		Color		tint = tints.get (Integer.valueOf (line));

		return (tint != null) ? tint : COLOURS[((line >= 0) ? line : 0) % COLOURS.length];
	}

	/** Builds a line of the right dataset for every label, in the order they were given. */
	protected void series ()
	{
		int			nl = 0, nr = 0;

		left.removeAllSeries ();
		right.removeAllSeries ();
		ljoin.removeAllSeries ();
		rjoin.removeAllSeries ();

		place	= new int[legends.length];
		lmap	= new int[legends.length];
		rmap	= new int[legends.length];
		for (int i = 0; i < legends.length; i++)
		{
			XYSeries	s = new XYSeries ((legends[i] != null) ? legends[i] : ("y" + i), false, true);

			s.setMaximumItemCount (POINTS);
			// the same line in both datasets: whoever draws writes it once
			if (isRight (i))
			{
				right.addSeries (s);	rjoin.addSeries (s);
				place[i] = nr;			rmap[nr] = i;			nr++;
			}
			else
			{
				left.addSeries (s);		ljoin.addSeries (s);
				place[i] = nl;			lmap[nl] = i;			nl++;
			}
		}
		lmap	= java.util.Arrays.copyOf (lmap, nl);
		rmap	= java.util.Arrays.copyOf (rmap, nr);
		paints ();
	}

	/** Gives every line its colour, whichever dataset it went to. */
	protected void paints ()
	{
		// the curve over the impulses is of the colour of the impulses it joins
		colours (plot.getRenderer (0), lmap);
		colours (plot.getRenderer (1), rmap);
		colours (plot.getRenderer (2), lmap);
		colours (plot.getRenderer (3), rmap);
	}

	/** One row per line of the plot: what it is called, and a field for its value. */
	protected void buildValues (String[] labels)
	{
		GridBagConstraints	gc = new GridBagConstraints ();

		vpanel.removeAll ();
		vfields		= new JTextField[labels.length];
		gc.insets	= new Insets (2, 4, 2, 4);
		gc.anchor	= GridBagConstraints.WEST;
		for (int i = 0; i < labels.length; i++)
		{
			JLabel		l = new JLabel (labels[i]);

			l.setFont (l.getFont ().deriveFont (Font.PLAIN, 11f));
			gc.gridx = 0;		gc.gridy = i;		gc.weightx = 0.0;
			gc.fill = GridBagConstraints.NONE;
			vpanel.add (l, gc);

			vfields[i]	= new JTextField (5);
			vfields[i].setEditable (false);							// read, not typed in
			vfields[i].setFocusable (false);
			vfields[i].setBackground (C_VALUE);
			vfields[i].setHorizontalAlignment (JTextField.RIGHT);
			vfields[i].setFont (vfields[i].getFont ().deriveFont (Font.PLAIN, 11f));
			gc.gridx = 1;		gc.weightx = 1.0;
			gc.fill = GridBagConstraints.HORIZONTAL;
			vpanel.add (vfields[i], gc);
		}
		// and a filler, so that the rows stay at the top of the panel
		gc.gridx = 0;		gc.gridy = labels.length;		gc.gridwidth = 2;
		gc.weighty = 1.0;	gc.fill = GridBagConstraints.BOTH;
		vpanel.add (new JLabel (), gc);
		vpanel.revalidate ();
		vpanel.repaint ();
	}

	/**
	 * What every line is worth, written beside the plot. Whoever draws does it
	 * from its own thread, so the fields are written on the event thread.
	 */
	protected void showValues (final double[] data)
	{
		final int		n;
		Runnable		r;

		if ((vfields == null) || (data == null))		return;
		n	= Math.min (vfields.length, data.length);
		r	= new Runnable ()
		{
			public void run ()
			{
				for (int i = 0; i < n; i++)		vfields[i].setText (String.format (java.util.Locale.US, "%.3f", data[i]));
			}
		};
		if (SwingUtilities.isEventDispatchThread ())		r.run ();
		else												SwingUtilities.invokeLater (r);
	}

	/** What the axes are called: the one along the bottom and the one on the left. */
	public void setLabels (String horiz, String vert)
	{
		plot.getDomainAxis ().setLabel (horiz);
		plot.getRangeAxis ().setLabel (vert);
	}

	public void setImpulses (boolean stems)
	{
		impulses	= stems;
		plot.setRenderer (0, lines ());
		if (plot.getRangeAxis (1) != null)		plot.setRenderer (1, lines ());
		joins ();
		series ();
	}

	/** What the left axis covers; a range of nothing at all is left to the values themselves. */
	public void setYRange (double ymin, double ymax)
	{
		range (plot.getRangeAxis (), ymin, ymax);
	}

	/** The same, for the second scale on the right. */
	public void setRightRange (double ymin, double ymax)
	{
		range (plot.getRangeAxis (1), ymin, ymax);
	}

	static private void range (org.jfree.chart.axis.ValueAxis axis, double ymin, double ymax)
	{
		if (axis == null)						return;
		if (ymax > ymin)						axis.setRange (ymin, ymax);
		else									axis.setAutoRange (true);
	}


	/** One cycle of every line, in the order the legend named them. */
	public void updateData (double[] data)
	{
		if (data == null)			return;

		dump_data (data);

		curx ++;
		for (int i = 0; i < data.length; i++)
		{
			XYSeries	s = line (i);

			if (s != null)			s.add ((double) curx, data[i], true);
		}
		showValues (data);
	}

	/** A whole plot at once: a row per point, its first value being where it goes along the bottom. */
	public void updateData (double[][] data, int n, int m)
	{
		if ((data == null) || (n <= 0))		return;

		// a whole plot is as long as it is: it is not the last few points of a line
		// that goes by, which is what POINTS is for
		for (int i = 0; i < left.getSeriesCount (); i++)
		{
			left.getSeries (i).setMaximumItemCount (Math.max (n, POINTS));
			left.getSeries (i).clear ();
		}
		for (int i = 0; i < right.getSeriesCount (); i++)
		{
			right.getSeries (i).setMaximumItemCount (Math.max (n, POINTS));
			right.getSeries (i).clear ();
		}

		for (int i = 0; i < n; i++)
		{
			dump_data (data[i]);
			for (int j = 1; j < m; j++)
			{
				XYSeries	s = line (j - 1);

				if (s != null)		s.add (data[i][0], data[i][j], false);
			}
		}
		for (int i = 0; i < left.getSeriesCount (); i++)		left.getSeries (i).fireSeriesChanged ();
		for (int i = 0; i < right.getSeriesCount (); i++)	right.getSeries (i).fireSeriesChanged ();
	}

	/**
	 * The line a value goes to, of whichever axis it is drawn against, or null when
	 * there is no line of that number (nobody named it).
	 */
	protected XYSeries line (int i)
	{
		XYSeriesCollection	c;
		int					p;

		if ((i < 0) || (i >= place.length))			return null;
		c	= isRight (i) ? right : left;
		p	= place[i];
		return (p < c.getSeriesCount ()) ? c.getSeries (p) : null;
	}

	/** How many points of every line are kept, from now on. */
	public void setPoints (int points)
	{
		if (points <= 0)							return;
		POINTS	= points;
		for (int i = 0; i < left.getSeriesCount (); i++)		left.getSeries (i).setMaximumItemCount (points);
		for (int i = 0; i < right.getSeriesCount (); i++)		right.getSeries (i).setMaximumItemCount (points);
	}

	/** The chart itself, for whoever wants more of it than this window gives. */
	public final JFreeChart			getChart ()			{ return chart; }
	public final XYPlot				getPlot ()			{ return plot; }

	protected void dump_start (String name, String title)
	{
		File				file;

		if (name == null)			return;

		file = new File (name);
		try
		{
			stream = new PrintWriter (new BufferedOutputStream (new FileOutputStream (file)));
			if (title != null)
				stream.println ("# " + title);
			stream.flush ();
		} catch (Exception e) { stream = null; }
	}

	protected void dump_data (double[] current)
	{
		int			i;

		if ((stream == null) || (current == null)) 	return;

		for (i = 0; i < current.length; i++)
			stream.print (current[i] +  "\t");

		stream.println (" ");
		stream.flush ();
	}

	protected void dump_data (String[] labels)
	{
		int			i;

		if ((stream == null) || (labels == null)) 	return;

		for (i = 0; i < labels.length; i++)
			stream.print (labels[i] +  "\t");

		stream.println (" ");
		stream.flush ();
	}

	protected void dump_stop ()
	{
		if (stream == null) return;

		stream.flush ();
		stream.close ();
		stream = null;
	}

	class SymWindow extends java.awt.event.WindowAdapter
	{
		public void windowClosing(java.awt.event.WindowEvent event)
		{
			Object object = event.getSource();
			if (object == PlotWindow.this)
				PlotWindow_WindowClosing(event);
		}
	}

	void PlotWindow_WindowClosing(java.awt.event.WindowEvent event)
	{
		close ();
	}
}
