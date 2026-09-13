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
	static public final int		CONNECTOR		= 7;
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
	static public final int		ICON		= 28;
	static public final int		NEW_ICON	= 29;
	static public final int		WORLD		= 30;
	static public final int		FOLDER		= 31;
	static public final int		EXECUTE		= 32;
	static public final int		RUN			= 33;
	static public final int		STEP		= 34;
	static public final int		STOP		= 35;
	static public final int		TASKS		= 36;
	static public final int		ARCHITECTURE	= 37;	// block diagram (edit the architecture)
	static public final int		LINDA		= 38;		// database-like cylinder
	static public final int		ROUTER		= 39;		// box with two vertical bars
	static public final int		MODULE		= 40;		// plain box
	static public final int		ROBOT		= 41;		// rounded box with wheels
	static public final int		TOPOLOGY	= 42;		// three connected nodes (topological map)
	static public final int		NODE		= 43;		// a single node
	static public final int		ARC			= 44;		// arrow between two nodes
	static public final int		SUBGRAPH	= 45;		// node with a small graph below (open its level)
	static public final int		EDIT_WORLD	= 46;		// map with a pencil (edit the world)

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
		Color		fg = new Color (50, 50, 50);
		// disabled buttons: paint the whole glyph faded (colours included) so the state is obvious
		if ((c != null) && !c.isEnabled ())										// c is null with the macOS screen menu bar
			g.setComposite (java.awt.AlphaComposite.getInstance (java.awt.AlphaComposite.SRC_OVER, 0.28f));
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
		case CONNECTOR:
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
		case ICON:			// polyline with vertex handles
			g.setColor (new Color (255, 140, 0));
			g.drawLine (4, 16, 9, 6);	g.drawLine (9, 6, 14, 14);	g.drawLine (14, 14, 19, 4);
			g.setColor (Color.WHITE);
			g.fillRect (2, 14, 5, 5);	g.fillRect (7, 4, 5, 5);	g.fillRect (12, 12, 5, 5);	g.fillRect (17, 2, 5, 5);
			g.setColor (new Color (255, 140, 0));
			g.setStroke (new BasicStroke (1f));
			g.drawRect (2, 14, 5, 5);	g.drawRect (7, 4, 5, 5);	g.drawRect (12, 12, 5, 5);	g.drawRect (17, 2, 5, 5);
			break;
		case NEW_ICON:		// small polyline with a plus sign
			g.setColor (new Color (255, 140, 0));
			g.drawLine (3, 18, 7, 10);	g.drawLine (7, 10, 11, 15);
			g.fillRect (1, 16, 4, 4);	g.fillRect (5, 8, 4, 4);	g.fillRect (9, 13, 4, 4);
			g.setColor (new Color (30, 140, 40));
			g.setStroke (new BasicStroke (2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.drawLine (16, 3, 16, 11);	g.drawLine (12, 7, 20, 7);
			break;
		case EXECUTE:		// gear-like wheel with a play sign (launch the architecture)
			g.setColor (new Color (60, 60, 60));
			g.setStroke (new BasicStroke (2.2f));
			g.drawOval (4, 4, 14, 14);
			for (int i = 0; i < 8; i++)
			{
				double	an = i * Math.PI / 4;
				g.drawLine ((int) Math.round (11 + 7 * Math.cos (an)), (int) Math.round (11 + 7 * Math.sin (an)),
							(int) Math.round (11 + 10 * Math.cos (an)), (int) Math.round (11 + 10 * Math.sin (an)));
			}
			g.setColor (new Color (30, 140, 40));
			g.fillPolygon (new int[] { 9, 9, 15 }, new int[] { 7, 15, 11 }, 3);
			break;
		case RUN:			// play
			g.setColor (new Color (30, 140, 40));
			g.fillPolygon (new int[] { 5, 5, 19 }, new int[] { 3, 19, 11 }, 3);
			break;
		case STEP:			// play + bar
			g.setColor (new Color (30, 110, 200));
			g.fillPolygon (new int[] { 4, 4, 15 }, new int[] { 3, 19, 11 }, 3);
			g.fillRect (16, 3, 3, 16);
			break;
		case STOP:			// square
			g.setColor (new Color (200, 40, 40));
			g.fillRoundRect (5, 5, 12, 12, 2, 2);
			break;
		case TASKS:			// check list
			g.setColor (new Color (250, 250, 250));
			g.fillRoundRect (3, 2, 16, 18, 3, 3);
			g.setColor (fg);
			g.setStroke (new BasicStroke (1.2f));
			g.drawRoundRect (3, 2, 16, 18, 3, 3);
			g.setStroke (new BasicStroke (1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor (new Color (30, 140, 40));
			for (int yy = 6; yy <= 16; yy += 5)		{ g.drawLine (5, yy + 1, 7, yy + 3); g.drawLine (7, yy + 3, 10, yy - 1); }
			g.setColor (fg);
			for (int yy = 6; yy <= 16; yy += 5)		g.drawLine (12, yy + 1, 17, yy + 1);
			break;
		case FOLDER:		// classic folder
			g.setColor (new Color (255, 210, 110));
			g.fillRoundRect (2, 6, 18, 13, 3, 3);
			g.setColor (new Color (255, 228, 150));
			g.fillPolygon (new int[] { 2, 9, 11, 20, 20, 2 }, new int[] { 6, 6, 4, 4, 9, 9 }, 6);
			g.setColor (new Color (170, 120, 30));
			g.setStroke (new BasicStroke (1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.drawRoundRect (2, 6, 18, 13, 3, 3);
			g.drawLine (2, 9, 20, 9);
			break;
		case WORLD:			// folder (load) with a small floor plan at its bottom right corner
			g.setColor (new Color (255, 210, 110));
			g.fillRoundRect (1, 5, 16, 12, 3, 3);
			g.setColor (new Color (255, 228, 150));
			g.fillPolygon (new int[] { 1, 7, 9, 17, 17, 1 }, new int[] { 5, 5, 3, 3, 8, 8 }, 6);
			g.setColor (new Color (170, 120, 30));
			g.setStroke (new BasicStroke (1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.drawRoundRect (1, 5, 16, 12, 3, 3);
			g.drawLine (1, 8, 17, 8);
			plan (g, fg, 11, 11, 10);
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
		case ARCHITECTURE:	// three blocks connected by lines
			g.setStroke (new BasicStroke (1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor (new Color (200, 225, 250));
			g.fillRect (7, 2, 8, 6);	g.fillRect (2, 14, 8, 6);	g.fillRect (12, 14, 8, 6);
			g.setColor (fg);
			g.drawRect (7, 2, 8, 6);	g.drawRect (2, 14, 8, 6);	g.drawRect (12, 14, 8, 6);
			g.drawLine (11, 8, 11, 11);	g.drawLine (6, 11, 16, 11);
			g.drawLine (6, 11, 6, 14);	g.drawLine (16, 11, 16, 14);
			break;
		case LINDA:			// database cylinder
			g.setStroke (new BasicStroke (1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor (new Color (200, 225, 250));
			g.fillRect (4, 6, 14, 10);
			g.fillOval (4, 12, 14, 7);
			g.fillOval (4, 3, 14, 7);
			g.setColor (fg);
			g.drawOval (4, 3, 14, 7);
			g.drawLine (4, 6, 4, 16);	g.drawLine (18, 6, 18, 16);
			g.drawArc (4, 12, 14, 7, 180, 180);
			g.drawArc (4, 8, 14, 7, 180, 180);
			break;
		case ROUTER:		// box with two vertical bars
			g.setStroke (new BasicStroke (1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor (new Color (255, 240, 200));
			g.fillRect (3, 5, 16, 12);
			g.setColor (fg);
			g.drawRect (3, 5, 16, 12);
			g.drawLine (7, 5, 7, 17);	g.drawLine (15, 5, 15, 17);
			break;
		case MODULE:		// plain box
			g.setStroke (new BasicStroke (1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor (new Color (225, 245, 225));
			g.fillRect (3, 5, 16, 12);
			g.setColor (fg);
			g.drawRect (3, 5, 16, 12);
			break;
		case ROBOT:			// rounded body with two wheels and a heading mark
			g.setStroke (new BasicStroke (1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor (new Color (250, 220, 220));
			g.fillRoundRect (4, 4, 14, 12, 6, 6);
			g.setColor (fg);
			g.drawRoundRect (4, 4, 14, 12, 6, 6);
			g.fillRoundRect (5, 16, 5, 3, 2, 2);	g.fillRoundRect (12, 16, 5, 3, 2, 2);
			g.drawLine (11, 7, 11, 4);	g.fillOval (9, 8, 4, 4);
			break;
		case TOPOLOGY:		// triangle of nodes joined by arcs
			g.setStroke (new BasicStroke (1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.drawLine (6, 16, 11, 5);	g.drawLine (11, 5, 17, 16);	g.drawLine (6, 16, 17, 16);
			g.setColor (new Color (215, 230, 250));
			g.fillOval (8, 2, 6, 6);	g.fillOval (3, 13, 6, 6);	g.fillOval (14, 13, 6, 6);
			g.setColor (fg);
			g.drawOval (8, 2, 6, 6);	g.drawOval (3, 13, 6, 6);	g.drawOval (14, 13, 6, 6);
			break;
		case NODE:			// one node
			g.setStroke (new BasicStroke (1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor (new Color (215, 230, 250));
			g.fillOval (6, 6, 10, 10);
			g.setColor (fg);
			g.drawOval (6, 6, 10, 10);
			break;
		case ARC:			// arrow from one node to another
			g.setStroke (new BasicStroke (1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor (new Color (215, 230, 250));
			g.fillOval (2, 13, 6, 6);	g.fillOval (14, 3, 6, 6);
			g.setColor (fg);
			g.drawOval (2, 13, 6, 6);	g.drawOval (14, 3, 6, 6);
			g.drawLine (7, 13, 14, 8);
			g.drawLine (14, 8, 10, 9);	g.drawLine (14, 8, 13, 12);
			break;
		case EDIT_WORLD:	// simplified floor plan of a world (walls, a door and a zone)
			plan (g, fg, 2, 3, 18);
			break;
		case SUBGRAPH:		// node with a small graph hanging below it
			g.setStroke (new BasicStroke (1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.setColor (new Color (215, 230, 250));
			g.fillOval (7, 2, 8, 8);
			g.setColor (fg);
			g.drawOval (7, 2, 8, 8);
			g.drawLine (11, 10, 11, 13);	g.drawLine (5, 13, 17, 13);
			g.drawLine (5, 13, 5, 16);		g.drawLine (17, 13, 17, 16);
			g.fillOval (3, 16, 4, 4);		g.fillOval (15, 16, 4, 4);		g.fillOval (9, 16, 4, 4);
			g.drawLine (11, 13, 11, 16);
			break;
		}
		g.dispose ();
	}

	/**
	 * A simplified floor plan (outer walls, an inner wall with a door gap and a
	 * shaded zone) in a square of side <code>size</code> at (x, y).
	 */
	static private void plan (Graphics2D g, Color fg, int x, int y, int size)
	{
		double	s = size / 18.0;
		Graphics2D	p = (Graphics2D) g.create ();
		p.translate (x, y);
		p.scale (s, s);
		p.setColor (new Color (255, 250, 230));
		p.fillRect (0, 0, 18, 18);
		p.setColor (new Color (215, 230, 250));							// a zone
		p.fillRect (1, 1, 8, 9);
		p.setColor (fg);
		p.setStroke (new BasicStroke (1.6f / (float) s, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
		p.drawRect (0, 0, 18, 18);												// outer walls
		p.drawLine (9, 0, 9, 6);	p.drawLine (9, 11, 9, 18);						// inner wall with a door gap
		p.drawLine (0, 10, 6, 10);
		p.setColor (new Color (200, 120, 40));
		p.setStroke (new BasicStroke (1.2f / (float) s));
		p.drawLine (9, 6, 9, 11);												// the door
		p.dispose ();
	}
}
