/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

import javax.swing.Icon;

/**
 * The icon of a flat projection: a box drawn in isometry with the face that
 * projection looks at filled in, so that the three of them are told apart at a
 * glance.
 */
public class ViewIcon implements Icon
{
	static public final int			SIZE	= 22;

	static private final Color		C_LINE	= new Color (90, 95, 105);
	static private final Color		C_FACE	= new Color (215, 218, 224);
	static private final Color		C_ON	= new Color (255, 170, 60);

	protected final int				view;
	protected final int				size;

	public ViewIcon (int view)					{ this (view, SIZE); }
	public ViewIcon (int view, int size)		{ this.view = view; this.size = size; }

	public int getIconWidth ()					{ return size; }
	public int getIconHeight ()					{ return size; }

	public void paintIcon (Component c, Graphics g0, int x, int y)
	{
		Graphics2D	g = (Graphics2D) g0.create ();
		double		cx = x + size / 2.0, cy = y + size / 2.0;
		double		w = size * 0.40, h = size * 0.22, v = size * 0.30;		// half width, half depth, half height

		g.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setStroke (new java.awt.BasicStroke (1f));

		face (g, RobotCanvas.V_TOP, new double[] { cx, cy - v - h,  cx + w, cy - v,  cx, cy - v + h,  cx - w, cy - v });
		face (g, RobotCanvas.V_SIDE, new double[] { cx - w, cy - v,  cx, cy - v + h,  cx, cy + v + h,  cx - w, cy + v });
		face (g, RobotCanvas.V_FRONT, new double[] { cx + w, cy - v,  cx, cy - v + h,  cx, cy + v + h,  cx + w, cy + v });

		g.dispose ();
	}

	/** One face of the box: filled in when it is the one this icon stands for. */
	private void face (Graphics2D g, int which, double[] pts)
	{
		Path2D.Double		p = new Path2D.Double ();

		p.moveTo (pts[0], pts[1]);
		for (int i = 2; i < pts.length; i += 2)		p.lineTo (pts[i], pts[i + 1]);
		p.closePath ();

		g.setColor ((which == view) ? C_ON : C_FACE);
		g.fill (p);
		g.setColor (C_LINE);
		g.draw (p);
	}
}
