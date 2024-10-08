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
	public static final int				OCCUPIED		= 3;
	public static final int				COST			= 4;
	
	public static final int				COLORS		= 21;
	protected static final double			MAX_COLOR	= (double) (COLORS - 1);
	
	// LPS HUD's labels
	public static final int				H_MTIME		= 0;
	public static final int				H_PTIME		= 1;
	public static final int				H_PEXP		= 2;
	
	protected Color[]				colors;
	
	/* Constructors */
	public Grid2D (Model2D model, RobotDesc rdesc) 
	{
		this.initialise (model, rdesc, null, null);
		this.initialise ();
	}
	
	public Grid2D (Model2D model, RobotDesc rdesc, String mapname) 
	{
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
		int			i, gray;
		double		step;
		
		colors	= new Color[COLORS];
		step		= 255.0 / MAX_COLOR;
		for (i = 0; i < COLORS; i++) 
		{
			gray	 = 255 - (int) Math.round ((double) i * step);
			colors[i] = new Color (gray, gray, gray);
		}
		
//		// Add support for HUD objects
//		model.hud_n				= 3;
//		model.hud_x[H_MTIME]		= 45;
//		model.hud_y[H_MTIME]		= 45;
//		model.hud_x[H_PTIME]		= 45;
//		model.hud_y[H_PTIME]		= 60;
//		model.hud_x[H_PEXP]		= 250;
//		model.hud_y[H_PEXP]		= 60;
	}
	
	public void update (Grid grid, GridPath gpath, Position pos, int mode)
	{
		int			i, j;
		double		ii, jj, li, lj;
		int			cndx;
		int			rx, ry, gx, gy;
		int			xi, yi;
		double		side;
		double[][]	cells;
		Path			path = null;
		Line2[]		icon;
		
		model.clearView ();
		
		if (grid == null) return;
		
		// Initialise component's model
		side		= grid.side ();
		model.clearView ();
		
		// Initialise auxiliary constants
		icon		= rdesc.icon;
		if (pos != null)
		{
			rx 		= grid.ctog_x (pos.x ());
			ry	 	= grid.ctog_y (pos.y ());
		}
		else
		{
			rx 		= -1;
			ry	 	= -1;
		}
		
		// Set HUD data values
//		model.hud_label[H_MTIME] = "Map: " + (int) grid.time () + " (" + (Math.round (grid.avg () * 10.0) / 10.0) + ") ms";
		if (gpath != null)
		{
			path		= gpath.path ();
//			model.hud_label[H_PTIME] = "Path: " + (int) gpath.time () + " (" + (Math.round (gpath.avg () * 10.0) / 10.0) + ") ms";
//			model.hud_label[H_PEXP] = "Nodes: " + (int) gpath.expanded () + " expanded";

			gx 		= gpath.goal_x ();
			gy	 	= gpath.goal_y ();
		}
		else
		{
//			model.hud_label[H_PTIME] = null;
//			model.hud_label[H_PEXP] = null;
			
			gx		= -1;
			gy		= -1;
		}
		
		if (mode != COST)						// Draw the occupancy grid
		{
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
					cndx = (int) Math.round (cells[i][j] * MAX_COLOR); 
					
					if ((i == 0) || (j == 0) || (i == grid.size_x () - 1) || (j == grid.size_y () - 1))
						continue;
					else if ((rx == i) && (ry == j))
						model.addRawBox (i, j, side, Model2D.FILLED, Color.BLUE);
					else if ((gx == i) && (gy == j))
						model.addRawBox (i, j, side, Model2D.FILLED, Color.RED);
					else /* if (!colors[cndx].equals (model..getBackground ())) */
						model.addRawBox (i, j, side, Model2D.FILLED, colors[cndx]);
				}
		}
		else									// Draw the costs grid
		{
			double		cmin, cmax;
			double		cavg;
			int			cavgn;
			
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
					if ((cells[i][j] > cmax) && (cells[i][j] < Double.MAX_VALUE))		cmax	= cells[i][j];
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
						cndx = COLORS;
					
					if ((i == 0) || (j == 0) || (i == grid.size_x () - 1) || (j == grid.size_y () - 1))
						continue;
					else if ((rx == i) && (ry == j))
						model.addRawBox (i, j, side, Model2D.FILLED, Color.BLUE);
					else if ((gx == i) && (gy == j))
						model.addRawBox (i, j, side, Model2D.FILLED, Color.RED);
					else if (cndx < COLORS)
						model.addRawBox (i, j, side, Model2D.FILLED, colors[COLORS - cndx - 1]);
				}
		}
		
		// Draw algorithm dependent artifacts
		if (drawartifacts)
		{
			Point2[]		data;
			Line2[]		lines;
			
			data		= grid.data ();
			lines	= grid.lines ();
			
			// Draw raw data points
			if (data != null)
				for (i = 0; i < grid.data_n (); i++)
				{				
					xi = grid.ctog_x (data[i].x ());
					yi = grid.ctog_y (data[i].y ());
					
					model.addRawCircle (xi, yi, 0.15, Color.ORANGE);
				}
			
			// Draw generated line segments
			if (lines != null)
				for (i = 0; i < grid.lines_n (); i++)
				{				
					model.addRawCircle (lines[i].orig ().x (), lines[i].orig ().y (), 0.15, Color.RED);
					model.addRawLine (lines[i].orig ().x (), lines[i].orig ().y (), lines[i].dest ().x (), lines[i].dest ().y (), Color.BLUE);
				}			
		}
		
		if (path != null)
		{
			Position			ppos;
			
			// Draw the planned robot gpath
			ppos	= path.first();
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
				model.addRawCircle (rx*side, ry*side, rdesc.RADIUS, Color.RED);
				model.addRawArrow (rx*side, ry*side, rdesc.RADIUS, pos.alpha (), Color.RED);	
			}
			else
			{
				model.addRawCircle (rx*side, ry*side, rdesc.RADIUS, Color.ORANGE);
				model.addRawArrow (rx*side, ry*side, rdesc.RADIUS, pos.alpha (), Color.ORANGE);	
				for (i = 0; i < icon.length; i++)
					model.addRawTransRotLine (icon[i], rx * side, ry * side, pos.alpha (), Color.RED);
			}
		}
		
		// Set clipping region to the boundaries of the grid map
		/*		double		minx, miny, maxx, maxy;
		 
		 minx	= grid.gtoc_x (0);
		 miny	= grid.gtoc_y (0);
		 maxx	= grid.gtoc_x (grid.size_x ());
		 maxy	= grid.gtoc_y (grid.size_y ());
		 
		 model.setBB (minx, miny, maxx, maxy);
		 
		 System.out.println ("minx="+minx+", miny="+miny+", maxx="+maxx+", maxy="+maxy); */
	}
}


