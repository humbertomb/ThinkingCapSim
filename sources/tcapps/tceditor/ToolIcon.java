/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;

/**
 * Small vector icons for the editor toolbar, drawn on the fly so that no image
 * resources are needed.
 */
public class ToolIcon implements Icon
{
	static public final int		SELECT		= 0;
	static public final int		PAN			= 1;
	static public final int		START		= 2;
	static public final int		WALL		= 3;
	static public final int		OBJECT		= 4;
	static public final int		FAREA		= 5;
	static public final int		ZONE		= 6;
	static public final int		DOOR		= 7;
	static public final int		WAYPOINT	= 8;
	static public final int		DOCK		= 9;
	static public final int		BEACON		= 10;
	static public final int		CBEACON		= 11;
	static public final int		PATH		= 12;
	static public final int		DELETE		= 20;
	static public final int		ZOOM_FIT	= 21;
	static public final int		ZOOM_IN		= 22;
	static public final int		ZOOM_OUT	= 23;
	static public final int		GRID		= 24;
	static public final int		UNDO		= 25;
	static public final int		REDO		= 26;
	static public final int		VIEW3D		= 27;

	protected int				type;
	protected int				size;

	public ToolIcon (int type)			{ this (type, 22); }
	public ToolIcon (int type, int size)	{ this.type = type; this.size = size; }

	public int getIconWidth ()			{ return size; }
	public int getIconHeight ()			{ return size; }

	public void paintIcon (Component c, Graphics g0, int x, int y)
	{
		Graphics2D	g = (Graphics2D) g0.create ();
		g.setRenderingHint (RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.translate (x, y);
		double		s = size / 22.0;
		g.scale (s, s);
		g.setStroke (new BasicStroke (1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		Color		fg = ((c == null) || c.isEnabled ()) ? new Color (50, 50, 50) : new Color (160, 160, 160);	// c is null with the macOS screen menu bar
		g.setColor (fg);

		switch (type)
		{
		case SELECT:		// arrow cursor
			g.fillPolygon (new int[] { 5, 5, 9, 12, 14, 11, 16 }, new int[] { 3, 17, 13, 19, 18, 12, 12 }, 7);
			break;
		case PAN:			// hand-like cross with arrows
			g.drawLine (11, 3, 11, 19);	g.drawLine (3, 11, 19, 11);
			g.drawLine (11, 3, 8, 6);	g.drawLine (11, 3, 14, 6);
			g.drawLine (11, 19, 8, 16);	g.drawLine (11, 19, 14, 16);
			g.drawLine (3, 11, 6, 8);	g.drawLine (3, 11, 6, 14);
			g.drawLine (19, 11, 16, 8);	g.drawLine (19, 11, 16, 14);
			break;
		case START:
			g.setColor (new Color (220, 30, 30));
			g.drawOval (4, 4, 14, 14);
			g.drawLine (11, 11, 18, 5);
			g.fillOval (9, 9, 4, 4);
			break;
		case WALL:
			g.setStroke (new BasicStroke (3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.drawLine (4, 17, 18, 5);
			break;
		case OBJECT:
			g.setColor (new Color (90, 90, 90));
			g.drawRect (4, 6, 14, 11);
			g.drawLine (4, 6, 8, 3);	g.drawLine (18, 6, 21, 3);	g.drawLine (8, 3, 21, 3);
			break;
		case FAREA:
			g.setColor (new Color (255, 90, 90, 90));
			g.fillPolygon (new int[] { 3, 12, 19, 15, 6 }, new int[] { 8, 3, 9, 19, 17 }, 5);
			g.setColor (new Color (200, 40, 40));
			g.drawPolygon (new int[] { 3, 12, 19, 15, 6 }, new int[] { 8, 3, 9, 19, 17 }, 5);
			break;
		case ZONE:
			g.setColor (new Color (255, 235, 130, 120));
			g.fillRect (3, 5, 16, 12);
			g.setColor (new Color (200, 160, 0));
			g.setStroke (new BasicStroke (1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[] { 3f, 2f }, 0f));
			g.drawRect (3, 5, 16, 12);
			break;
		case DOOR:
			g.setColor (new Color (150, 90, 30));
			g.setStroke (new BasicStroke (3f));
			g.drawLine (5, 16, 17, 16);
			g.setColor (new Color (40, 170, 40));
			g.setStroke (new BasicStroke (1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[] { 3f, 2f }, 0f));
			g.drawLine (11, 4, 11, 16);
			break;
		case WAYPOINT:
			g.setColor (new Color (30, 80, 220));
			g.drawOval (5, 5, 12, 12);
			g.drawLine (11, 11, 19, 6);
			break;
		case DOCK:
			g.setColor (new Color (0, 140, 60));
			g.drawRect (5, 5, 12, 12);
			g.drawLine (11, 11, 19, 6);
			break;
		case BEACON:
			g.setColor (new Color (200, 0, 200));
			g.setStroke (new BasicStroke (3f));
			g.drawLine (4, 14, 18, 8);
			g.setStroke (new BasicStroke (1.5f));
			g.drawLine (11, 11, 13, 16);
			break;
		case CBEACON:
			g.setColor (new Color (200, 0, 200, 90));
			g.fillOval (5, 5, 12, 12);
			g.setColor (new Color (200, 0, 200));
			g.drawOval (5, 5, 12, 12);
			break;
		case PATH:
			g.setColor (new Color (0, 170, 200));
			g.drawLine (3, 17, 8, 8);	g.drawLine (8, 8, 14, 13);	g.drawLine (14, 13, 19, 4);
			g.fillOval (1, 15, 4, 4);	g.fillOval (6, 6, 4, 4);	g.fillOval (12, 11, 4, 4);	g.fillOval (17, 2, 4, 4);
			break;
		case DELETE:
			g.setColor (new Color (200, 40, 40));
			g.drawLine (5, 5, 17, 17);	g.drawLine (17, 5, 5, 17);
			break;
		case ZOOM_FIT:
			g.drawRect (3, 3, 16, 16);
			g.drawLine (3, 3, 8, 8);	g.drawLine (19, 3, 14, 8);	g.drawLine (3, 19, 8, 14);	g.drawLine (19, 19, 14, 14);
			break;
		case ZOOM_IN:
			g.drawOval (3, 3, 12, 12);	g.drawLine (13, 13, 19, 19);	g.drawLine (6, 9, 12, 9);	g.drawLine (9, 6, 9, 12);
			break;
		case ZOOM_OUT:
			g.drawOval (3, 3, 12, 12);	g.drawLine (13, 13, 19, 19);	g.drawLine (6, 9, 12, 9);
			break;
		case GRID:
			g.setStroke (new BasicStroke (1f));
			for (int i = 3; i <= 19; i += 4) { g.drawLine (i, 3, i, 19); g.drawLine (3, i, 19, i); }
			break;
		case UNDO:
			g.drawArc (5, 6, 12, 12, 0, 270);
			g.drawLine (5, 12, 5, 6);	g.drawLine (5, 6, 11, 6);
			break;
		case REDO:
			g.drawArc (5, 6, 12, 12, 270, 270);
			g.drawLine (17, 12, 17, 6);	g.drawLine (17, 6, 11, 6);
			break;
		case VIEW3D:		// isometric cube
			g.setColor (new Color (120, 160, 220, 110));
			g.fillPolygon (new int[] { 11, 19, 19, 11 }, new int[] { 9, 5, 14, 18 }, 4);
			g.setColor (new Color (70, 110, 180, 110));
			g.fillPolygon (new int[] { 3, 11, 11, 3 }, new int[] { 5, 9, 18, 14 }, 4);
			g.setColor (fg);
			g.drawPolygon (new int[] { 3, 11, 19, 11 }, new int[] { 5, 1, 5, 9 }, 4);
			g.drawPolygon (new int[] { 3, 11, 11, 3 }, new int[] { 5, 9, 18, 14 }, 4);
			g.drawPolygon (new int[] { 11, 19, 19, 11 }, new int[] { 9, 5, 14, 18 }, 4);
			break;
		}
		g.dispose ();
	}
}
