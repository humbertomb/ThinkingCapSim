/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tclib.behaviours.hfsm.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;

/**
 * The icons of the toolbar of the editor, drawn in code so that the editor
 * needs no image beside it, in the way of the icons of the world editor.
 */
public class HFSMIcon implements Icon
{
	static public final int			SELECT		= 0;
	static public final int			PAN			= 1;
	static public final int			STATE		= 2;
	static public final int			METASTATE	= 3;
	static public final int			TRANSITION	= 4;
	static public final int			LINK		= 5;		// the arrow that joins two blocks
	static public final int			INITIAL		= 6;
	static public final int			UP			= 7;		// out of a meta state
	static public final int			DELETE		= 8;
	static public final int			ZOOM_FIT	= 9;
	static public final int			ZOOM_IN		= 10;
	static public final int			ZOOM_OUT	= 11;
	static public final int			NEW_FILE	= 12;
	static public final int			OPEN		= 13;
	static public final int			SAVE		= 14;

	/* The colours the diagram is drawn with, so that the icons match it */
	static public final Color		C_STATE		= new Color (216, 216, 216);
	static public final Color		C_META		= new Color (168, 168, 168);
	static public final Color		C_TRANS		= new Color (180, 240, 240);
	static public final Color		C_EDGE		= new Color (90, 90, 90);
	static public final Color		C_SEL		= new Color (255, 140, 0);

	protected int					type;
	protected int					size;

	public HFSMIcon (int type)				{ this (type, 22); }
	public HFSMIcon (int type, int size)	{ this.type = type; this.size = size; }

	public int getIconWidth ()				{ return size; }
	public int getIconHeight ()				{ return size; }

	public void paintIcon (Component c, Graphics g0, int x, int y)
	{
		Graphics2D	g = (Graphics2D) g0.create ();

		g.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.translate (x, y);
		g.scale (size / 22.0, size / 22.0);
		g.setStroke (new BasicStroke (1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		Color		fg = new Color (50, 50, 50);

		if ((c != null) && !c.isEnabled ())
			g.setComposite (java.awt.AlphaComposite.getInstance (java.awt.AlphaComposite.SRC_OVER, 0.28f));
		g.setColor (fg);

		switch (type)
		{
		case SELECT:															// arrow cursor
			g.fillPolygon (new int[] { 5, 5, 9, 12, 14, 11, 16 }, new int[] { 3, 17, 13, 19, 18, 12, 12 }, 7);
			break;

		case PAN:																// cross with arrows
			g.drawLine (11, 3, 11, 19);		g.drawLine (3, 11, 19, 11);
			g.drawLine (11, 3, 8, 6);		g.drawLine (11, 3, 14, 6);
			g.drawLine (11, 19, 8, 16);		g.drawLine (11, 19, 14, 16);
			g.drawLine (3, 11, 6, 8);		g.drawLine (3, 11, 6, 14);
			g.drawLine (19, 11, 16, 8);		g.drawLine (19, 11, 16, 14);
			break;

		case STATE:																// a circle
			g.setColor (C_STATE);
			g.fillOval (3, 3, 16, 16);
			g.setColor (fg);
			g.drawOval (3, 3, 16, 16);
			break;

		case METASTATE:															// a circle with a circle inside
			g.setColor (C_META);
			g.fillOval (2, 2, 18, 18);
			g.setColor (fg);
			g.drawOval (2, 2, 18, 18);
			g.setColor (C_STATE);
			g.fillOval (7, 7, 8, 8);
			g.setColor (fg);
			g.drawOval (7, 7, 8, 8);
			break;

		case TRANSITION:														// a box
			g.setColor (C_TRANS);
			g.fillRoundRect (2, 7, 18, 9, 4, 4);
			g.setColor (fg);
			g.drawRoundRect (2, 7, 18, 9, 4, 4);
			break;

		case LINK:																// two blocks and an arrow between them
			g.setColor (C_STATE);
			g.fillOval (1, 6, 9, 9);
			g.setColor (fg);
			g.drawOval (1, 6, 9, 9);
			g.setColor (C_TRANS);
			g.fillRoundRect (13, 7, 8, 7, 3, 3);
			g.setColor (fg);
			g.drawRoundRect (13, 7, 8, 7, 3, 3);
			arrow (g, 10, 11, 13, 11);
			break;

		case INITIAL:															// a circle with the mark of the start
			g.setColor (C_STATE);
			g.fillOval (6, 4, 14, 14);
			g.setColor (fg);
			g.drawOval (6, 4, 14, 14);
			g.drawOval (8, 6, 10, 10);
			arrow (g, 1, 11, 6, 11);
			break;

		case UP:																// arrow up to a bar
			g.drawLine (11, 19, 11, 7);
			arrow (g, 11, 12, 11, 5);
			g.drawLine (4, 3, 18, 3);
			break;

		case DELETE:															// a cross
			g.setColor (new Color (190, 40, 40));
			g.setStroke (new BasicStroke (2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.drawLine (5, 5, 17, 17);		g.drawLine (17, 5, 5, 17);
			break;

		case ZOOM_FIT:
			g.drawRect (3, 5, 16, 12);
			g.drawLine (7, 9, 15, 9);		g.drawLine (7, 13, 15, 13);
			break;

		case ZOOM_IN:
		case ZOOM_OUT:
			g.drawOval (4, 4, 11, 11);
			g.drawLine (14, 14, 19, 19);
			g.drawLine (7, 9, 12, 9);
			if (type == ZOOM_IN)			g.drawLine (9, 7, 9, 12);
			break;

		case NEW_FILE:															// a page with a folded corner
			g.drawPolygon (new int[] { 5, 13, 17, 17, 5 }, new int[] { 3, 3, 7, 19, 19 }, 5);
			g.drawLine (13, 3, 13, 7);		g.drawLine (13, 7, 17, 7);
			break;

		case OPEN:																// a folder
			g.drawPolygon (new int[] { 3, 9, 11, 19, 19, 3 }, new int[] { 5, 5, 7, 7, 17, 17 }, 6);
			break;

		case SAVE:																// a floppy disk
			g.drawRect (4, 4, 14, 14);
			g.drawRect (8, 4, 6, 5);
			g.drawRect (7, 12, 8, 6);
			break;
		}
		g.dispose ();
	}

	/** A line with a head at its end. */
	static protected void arrow (Graphics2D g, int x1, int y1, int x2, int y2)
	{
		double		a = Math.atan2 (y2 - y1, x2 - x1);
		int			hx1 = (int) Math.round (x2 - 4.0 * Math.cos (a - 0.5));
		int			hy1 = (int) Math.round (y2 - 4.0 * Math.sin (a - 0.5));
		int			hx2 = (int) Math.round (x2 - 4.0 * Math.cos (a + 0.5));
		int			hy2 = (int) Math.round (y2 - 4.0 * Math.sin (a + 0.5));

		g.drawLine (x1, y1, x2, y2);
		g.fillPolygon (new int[] { x2, hx1, hx2 }, new int[] { y2, hy1, hy2 }, 3);
	}
}
