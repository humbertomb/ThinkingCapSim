/*
 * (c) 1999-2000 Humberto Martinez
 * (c) 2003 Humberto Martinez
 */

package tclib.navigation.mapbuilding.visualization;

import java.awt.*;

import tc.vrobot.*;
import tc.gui.visualization.*;

import tclib.navigation.mapbuilding.*;
import tclib.navigation.pathplanning.*;

import devices.pos.*;
import wucore.widgets.*;
import wucore.utils.geom.*;

public class Grid2D extends World2D
{
	public static final int				NAVIGATION	= 1;
	public static final int				EMPTY		= 2;
	public static final int				OCCUPIED	= 3;
	public static final int				COST		= 4;
	
	public static final int				COLORS		= 21;
	protected static final double		MAX_COLOR	= (double) (COLORS - 1);
	
	// LPS HUD's labels
	public static final int				H_MTIME		= 0;
	public static final int				H_PTIME		= 1;
	public static final int				H_PEXP		= 2;
	
	protected Component2D 				compo;

	public boolean						draw_hud	= false;

	protected Color[]					colors;
	
	/* Constructors */
	public Grid2D (Model2D model, Component2D compo, RobotDesc rdesc) 
	{
    	this.compo = compo;

		this.initialise (model, rdesc, null, null);
		this.initialise ();
	}
	
	public Grid2D (Model2D model, Component2D compo, RobotDesc rdesc, String mapname) 
	{
    	this.compo = compo;

		this.initialise (model, rdesc, null, mapname);
		this.initialise ();
	}
	
	/* Instance methods */
	public Color getMiddleColor ()
	{
		return colors[(int) Math.round (0.5 * MAX_COLOR)]; 
	}
	
	protected void initialise () 
	{
		int			gray;
		double		step;
		
		colors	= new Color[COLORS];
		step	= 255.0 / MAX_COLOR;
		for (int i = 0; i < COLORS; i++) 
		{
			gray = 255 - (int) Math.round ((double) i * step);
			colors[i] = new Color (gray, gray, gray);
		}
		
		// Add support for HUD objects
		if (draw_hud)
		{
			compo.hud_n				= 3;
			compo.hud_x[H_MTIME]	= 45;
			compo.hud_y[H_MTIME]	= 45;
			compo.hud_x[H_PTIME]	= 45;
			compo.hud_y[H_PTIME]	= 60;
			compo.hud_x[H_PEXP]		= 250;
			compo.hud_y[H_PEXP]		= 60;
		}
	}
	
	public void update (Grid grid, GridPath gpath, Position pos, int mode)
	{
		int			i, j;
		double		ii, jj, li, lj;
		int			cndx;
		double		rx, ry, gx, gy, cx, cy;
		double		bx0, by0, bx1, by1;
		double		side;
		double[][]	cells;
		Path		path = null;
		Line2[]		icon;
		
		model.clearView ();
		
		if (grid == null) return;
		
		// Initialize component's model
		side		= grid.side ();
		
		// Initialize auxiliary constants
		icon = rdesc.icon;
		if (pos != null)
		{
			rx 		= pos.x ();
			ry	 	= pos.y ();
		}
		else
		{
			rx 		= -Double.MAX_VALUE;
			ry	 	= -Double.MAX_VALUE;
		}
		
		// Set HUD data values
		if (draw_hud)
			compo.hud_label[H_MTIME] = "Map: " + (int) grid.time () + " (" + (Math.round (grid.avg () * 10.0) / 10.0) + ") ms";

		if (gpath != null)
		{
			path		= gpath.path ();
			
			if (draw_hud)
			{
				compo.hud_label[H_PTIME] = "Path: " + (int) gpath.time () + " (" + (Math.round (gpath.avg () * 10.0) / 10.0) + ") ms";
				compo.hud_label[H_PEXP] = "Nodes: " + (int) gpath.expanded () + " expanded";
			}
			
			gx 		= grid.gtoc_x (gpath.goal_x ());
			gy	 	= grid.gtoc_y (gpath.goal_y ());
		}
		else
		{
			if (draw_hud)
			{
				compo.hud_label[H_PTIME] = null;
				compo.hud_label[H_PEXP] = null;
			}
			
			gx		= -Double.MAX_VALUE;
			gy		= -Double.MAX_VALUE;
		}
		
		if (mode != COST)						// Draw the occupancy grid
		{
			int			ggx, ggy, grx, gry;
			Color		color;
			
			// Convert real world to grid coordinates
			ggx = grid.ctog_x (gx);
			ggy = grid.ctog_y (gy);
			grx = grid.ctog_x (rx);
			gry = grid.ctog_y (ry);

			// Select the map to be drawn
			switch (mode)
			{
			case EMPTY:
				cells = grid.empty ();
				break;
			case OCCUPIED:
				cells = grid.occupied ();
				break;
			case NAVIGATION:
			default:
				cells = grid.navigation ();
			}
			
			// Draw the grid map
			for (i = 0; i < grid.size_x (); i++)
				for (j = 0; j < grid.size_y (); j++)
				{
//					if ((i == 0) || (j == 0) || (i == grid.size_x () - 1) || (j == grid.size_y () - 1))
//						continue;
//					
					cx = grid.gtoc_x (i) - side*0.5;
					cy = grid.gtoc_y (j) - side*0.5;
					
					if ((grx == i) && (gry == j))
						color = Color.WHITE;
					else if ((ggx == i) && (ggy == j))
						color = Color.BLUE;
					else
						color = colors[(int) Math.round (cells[i][j] * MAX_COLOR)];
					
					model.addRawBox (cx, cy, cx+side, cy+side, Model2D.FILLED, color);					
				}
		}
		else									// Draw the costs grid
		{
			double		cmin, cmax;
			double		cavg;
			int			cavgn;
			Color		color;
			int			ggx, ggy, grx, gry;
			
			// Convert real world to grid coordinates
			ggx = grid.ctog_x (gx);
			ggy = grid.ctog_y (gy);
			grx = grid.ctog_x (rx);
			gry = grid.ctog_y (ry);
			
			cmin	= Double.MAX_VALUE;
			cmax	= Double.MIN_VALUE;
			cavg	= 0.0;
			cavgn	= 0;
			cells	= gpath.cost ();
			
			// Compute the minimum and maximum values of the cells
			for (i = 0; i < grid.size_x (); i++)
				for (j = 0; j < grid.size_y (); j++)
				{
					if (cells[i][j] < Double.MAX_VALUE)
					{
						cavg	+= cells[i][j];
						cavgn++;
					}
					
					if (cells[i][j] < cmin)											cmin	= cells[i][j];
					if ((cells[i][j] > cmax) && (cells[i][j] < Double.MAX_VALUE))	cmax	= cells[i][j];
				}
			cavg	= (cavg / (double) cavgn) - cmin;			// The median could give much better results
			
			// Draw the color-corrected values of the cells
			for (i = 0; i < grid.size_x (); i++)
				for (j = 0; j < grid.size_y (); j++)
				{
					if (cells[i][j] < gpath.kmax ())
					{
						cndx = (int) Math.round (MAX_COLOR * 0.5 * (cells[i][j] - cmin) / cavg);
						
						if (cndx >= COLORS)		cndx = COLORS - 1;
					}
					else
						cndx = COLORS-1;
					
//					if ((i == 0) || (j == 0) || (i == grid.size_x () - 1) || (j == grid.size_y () - 1))
//						continue;
					
					cx = grid.gtoc_x (i) - side*0.5;
					cy = grid.gtoc_y (j) - side*0.5;
					
					if ((grx == i) && (gry == j))
						color = Color.WHITE;
					else if ((ggx == i) && (ggy == j))
						color = Color.BLUE;
					else
						color = colors[COLORS - cndx - 1];
						
					model.addRawBox (cx, cy, cx+side, cy+side, Model2D.FILLED, color);
				}
		}
		
		// Draw algorithm dependent artifacts
		if (drawartifacts)
		{
			Point2[]	data;
			Line2[]		lines;
			
			data	= grid.data ();
			lines	= grid.lines ();
			
			// Draw raw data points
			if (data != null)
				for (i = 0; i < grid.data_n (); i++)
					model.addRawCircle (data[i].x (), data[i].y (), 0.05, Color.ORANGE);
			
			// Draw generated line segments
			if (lines != null)
				for (i = 0; i < grid.lines_n (); i++)
				{				
					model.addRawCircle (lines[i].orig ().x (), lines[i].orig ().y (), 0.15, Color.RED);
					model.addRawLine (lines[i].orig ().x (), lines[i].orig ().y (), lines[i].dest ().x (), lines[i].dest ().y (), Color.BLUE);
				}			
		}
		// Draw grid boundary
		bx0 = grid.gtoc_x (0) - side*0.5;
		by0 = grid.gtoc_y (0) - side*0.5;
		bx1 = grid.gtoc_x (grid.size_x ()-1) + side*0.5;
		by1 = grid.gtoc_y (grid.size_y ()-1) + side*0.5;

		model.addRawLine (bx0, by0, bx0, by1, Model2D.THICK, Color.BLACK);
		model.addRawLine (bx0, by1, bx1, by1, Model2D.THICK, Color.BLACK);
		model.addRawLine (bx1, by1, bx1, by0, Model2D.THICK, Color.BLACK);
		model.addRawLine (bx1, by0, bx0, by0, Model2D.THICK, Color.BLACK);

		if (path != null)
		{
			Position			ppos;
			
			// Draw the planned robot global path
			ppos = path.first();
			li = ppos.x ();
			lj = ppos.y ();
			for (ppos = path.next (); ppos != null; ppos = path.next ())
			{
				ii = ppos.x ();
				jj = ppos.y ();
				
				model.addRawLine (li, lj, ii, jj, Color.GREEN);
				
				li = ii;
				lj = jj;
			}
		}
		
		if ((rx > 0) && (ry > 0) && (rx < grid.size_x ()) && (ry < grid.size_y ()))
		{			
			// Draw the real robot
			if (icon == null)
			{
				model.addRawCircle (rx, ry, rdesc.RADIUS, Color.RED);
				model.addRawArrow (rx, ry, rdesc.RADIUS, pos.alpha (), Color.RED);	
			}
			else
			{
				model.addRawArrow (rx, ry, rdesc.RADIUS, pos.alpha (), Color.ORANGE);	
				for (i = 0; i < icon.length; i++)
					model.addRawTransRotLine (icon[i], rx, ry, pos.alpha (), Color.RED);
			}
		}
		
		// Set clipping region to the boundaries of the grid map
		double		minx, miny, maxx, maxy;
		
		minx	= grid.gtoc_x (0) - 0.5;
		miny	= grid.gtoc_y (0) - 0.5;
		maxx	= grid.gtoc_x (grid.size_x ()) + 0.5;
		maxy	= grid.gtoc_y (grid.size_y ()) + 0.5;

		model.setBB (minx, miny, maxx, maxy);
	}
}


