/*
 * (c) 1999-2002 Humberto Martinez 
 */
 
package tclib.navigation.mapbuilding;

import java.util.*;

import tclib.utils.fusion.*;
import tc.vrobot.*;
import tc.shared.lps.lpo.*;
import tc.shared.world.*;

import devices.pos.*;
import wucore.utils.geom.*;

public abstract class Grid extends Object implements MapBuilding
{
	// Global variables
	public static final double		EMPTY		= 0.0;				/* Indicates an empty cell 					*/
	public static final double		FULL		= 1.0;				/* Indicates a full cell 					*/
										
	// Grid map update time 
	protected double				time;							// Current control cycle time (ms)
	protected double				tsum;							// Accumulated time updates (ms)
	protected int					tcount;							// Number of time updates
	
	// Instance variables
	protected int					off_x;							/* Minimum value for X axis					*/
	protected int					off_y;							/* Minimum value for Y axis					*/
	protected int					size_x;							/* The grid X axis size						*/
	protected int					size_y;							/* The grid Y axis size						*/

	private double[][]			    free;							/* Combination of empty/occupied beliefs	*/
	protected double[][]			empty;							/* Belief that the cell is empty			*/
	protected double[][]			occupied;						/* Belief that the cell is occupied			*/
	private boolean[][]             obstacles;
	
	protected double				h				= 1.0;			/* Cell side lenght in application units.	*/
	protected double				MAX_RANGE_SON	= 1.5;			/* Do not use soanr readings futher (m)		*/
	protected double				MAX_RANGE_LRF	= 15.0;			/* Do not use laser readings futher (m)		*/

	protected RobotDesc				rdesc;
	protected FusionDesc			fdesc;
	protected int					mode;							/* Cell-update mode							*/

	// Algorithm dependent debuging support
	protected Point2[]				data;							// Raw buffer for algorithm operations
	protected int					data_n;							// Number of raw points
	protected Line2[]				lines;							// Lines obtained by the algorithm
	protected int					lines_n;						// Number of extracted lines

	protected Hashtable<String,LinkedList<double[]>>	tempChanges;	
	
	// Constructors
	public Grid (FusionDesc fdesc, RobotDesc rdesc, int nx, int ny, double h)
	{
		this.rdesc	= rdesc;
		this.fdesc	= fdesc;
		
		size_x 		= nx + 2; 
		size_y 		= ny + 2;	
		off_x		= size_x / 2;
		off_y		= size_y / 2;
		
		this.h		= h;

		free 		= new double[size_x][size_y];
		empty		= new double[size_x][size_y];
		occupied 	= new double[size_x][size_y];
		obstacles	= new boolean[size_x][size_y];
		
		tempChanges = new Hashtable<String,LinkedList<double[]>>();
		// Subclasses may redefine this for its own purposes
		data		= null;
		data_n		= 0;
		lines		= null;
		lines_n		= 0;
		
		reset ();
	}
	
	// Accessors
	public final double[][] 	navigation ()						{ return free; }
	public final double[][] 	empty ()							{ return empty; }
	public final double[][] 	occupied ()							{ return occupied; }

    public boolean              isObstacle(int i, int j)                { return obstacles[i][j];}
    public void                 setObstacle(int i, int j, boolean bol)  { obstacles[i][j] = bol;}
    
    public double               getFree(int i, int j)               { return free[i][j]; }
    
    //  Change free cells. If this cells is obstacle set FULL
    public void                 setFree(int i, int j, double val)   { 
        if(isObstacle(i,j))     free[i][j] = FULL;
        else                    free[i][j] = val; 
    }
    
	public final int 			min_x ()							{ return -off_x; }
	public final int 			min_y ()							{ return -off_y; }
	public final int 			size_x ()							{ return size_x; }
	public final int 			size_y ()							{ return size_y; }
	
	public final double			getRangeSON ()						{ return MAX_RANGE_SON; }
	public final double			getRangeLRF ()						{ return MAX_RANGE_LRF; }
	public final void			setRangeSON (double r)				{ MAX_RANGE_SON = r; }
	public final void			setRangeLRF (double r)				{ MAX_RANGE_LRF = r; }
	public final void			setRange (double r1, double r2)		{ MAX_RANGE_SON = r1; MAX_RANGE_LRF = r2; }
	public final int	 		getMode ()							{ return mode; }
	public final void 			setMode (int m)						{ mode = m; }
	
	public final double 		side ()								{ return h; }
	public final double			time ()								{ return time; }
	public final double			avg ()								{ if (tcount > 0) return (tsum / (double) tcount); return 0.0; }
		
	public final Point2[]		data ()								{ return data; }
	public final int			data_n ()							{ return data_n; }
	public final Line2[]		lines ()							{ return lines; }
	public final int			lines_n ()							{ return lines_n; }
		
	// Instance methods 
	public void reset ()
	{ 
		int	i, j;

		for (i = 0; i < size_x; i++)
			for (j = 0; j < size_y; j++)
				if ((i == 0) || (i == (size_x - 1)) || (j == 0) || (j == (size_y - 1)))
					set_occupied (i, j);
				else
					set_unknown (i, j);
	}

	public void add_time (double time)
	{
		this.time	= time;
		tsum		+= time;
		tcount ++;
	}
	
	public void	setOffsets (double x, double y)	
	{ 
		off_x	= (int) Math.round (-x / h);	
		off_y	= (int) Math.round (-y / h);	
	}
	
	public int ctog_x (double x)
	{
		return (int) Math.round (x / h) + off_x;
	}
	
	public int ctog_y (double y)
	{
		return (int) Math.round (y / h) + off_y;
	}
	
	public double gtoc_x (int x)
	{
		return (double) (x - off_x) * h;
	}
	
	public double gtoc_y (int y)
	{
		return (double) (y - off_y) * h;
	}
	
	protected void fromFAreas (Polygon2[] polygons)
	{
		int	i, j, k;
		
		for (i = 0; i < size_x; i++)
			for (j = 0; j < size_y; j++)
				for (k = 0; k < polygons.length; k++)
					if(polygons[k].contains(gtoc_x (i), gtoc_y (j)))
					{
						set_occupied(i, j);
						setObstacle(i, j, true);
					}
	}
	
	protected void fromEdges (Line2[] edges)
	{
		int		ci, cj;
		double	xi, yi;
		double	x1, y1, x2, y2;
		double	As, Bs, alpha;
		
		for (int i = 0; i< edges.length; i++)
		{
			x1	=	edges[i].orig().x();
			y1	=	edges[i].orig().y();
			x2	=	edges[i].dest().x();
			y2	=	edges[i].dest().y();

			x2	= 	Math.max(x1, x2);
		
			if (x2 == edges[i].orig().x()){
				y2	=	edges[i].orig().y();
				x1	=	edges[i].dest().x();
				y1	=	edges[i].dest().y();
			} else{
				y2	=	edges[i].dest().y();
				x1	=	edges[i].orig().x();
				y1	=	edges[i].orig().y();		
			}
			
			As	=	(y2-y1)/(x2-x1);

			if (Math.abs(As) <= 1)
			{	
				if((x2 != 0.0) || (y2 != 0.0))
					Bs	=	y2 - As * x2;
				else
					Bs	=	y1 - As * x1;	
				
				alpha	=	Math.atan(As);
					
				for (xi = x1 ; xi <= x2 ; xi += h * Math.abs(Math.cos(alpha))) 
				{
					yi = As * xi + Bs;
					ci 	= ctog_x (xi);
					cj 	= ctog_y (yi);
					
					if ((ci < 1) || (cj< 1) || (ci > size_x - 2) || (cj > size_y - 2))
						continue;	
					
                    set_occupied(ci,cj);
					setObstacle(ci, cj, true);
				}	
				
			} 
			else 
			{
				y2	= 	Math.max(y1,y2);
		
				if (y2 == edges[i].orig().y()){
					x2	=	edges[i].orig().x();
					x1	=	edges[i].dest().x();
					y1	=	edges[i].dest().y();
				} 
				else
				{
					x2	=	edges[i].dest().x();
					x1	=	edges[i].orig().x();
					y1	=	edges[i].orig().y();		
				}

				As	= (x2-x1)/(y2-y1);	
			
				if((x2 != 0.0) || (y2 != 0.0))
					Bs	=	x2 - As * y2;
				else
					Bs	=	x1 - As * y1;	

				alpha	=	Math.atan(As);
					
				for (yi = y1 ; yi <= y2 ; yi += h * Math.abs(Math.cos(alpha))) 
				{
					xi = As * yi + Bs;
					ci 	= ctog_x (xi);
					cj 	= ctog_y (yi);
					
					if ((ci < 1) || (cj< 1) || (ci > size_x - 2) || (cj > size_y - 2))
						continue;	
								
                    set_occupied(ci,cj);        
                    setObstacle(ci, cj, true);  	
				}
			}
		}
	}
	
	public void fromWorld (World world)
	{
		fromEdges (world.getLines());
		fromFAreas (world.getFAreas());
	}
	
	public void fromWorld (World world, String zone)
	{
		Line2[]				edges;
		LinkedList<Line2>	zoneedges;
		
		edges = world.getLines();
		zoneedges = new LinkedList<Line2> ();
		for (int i=0; i < edges.length; i++)
		{
			if (zone.equals (world.zones ().inZone(edges[i].orig().x(), edges[i].orig().y())) 
				|| zone.equals (world.zones ().inZone(edges[i].dest().x(), edges[i].dest().y())))
				zoneedges.add (edges[i]);
		}
				
		fromEdges ((Line2[])zoneedges.toArray());
	}	
	
	/* 
	 * Set new robot position, while erasing all cells occuppied by the
	 * robot. Then we get clearer maps.
	 * NOTE: the robot orientation and actual shape is not taken into account.
	 */
	public void location (Position pos)
	{
		int				i, j;
		double			xi, yi;
		double			minx, miny;
		double			maxx, maxy;
		
		minx	= pos.x () - rdesc.RADIUS * 0.6;		
		miny	= pos.y () - rdesc.RADIUS * 0.6;		
		maxx	= pos.x () + rdesc.RADIUS * 0.6;		
		maxy	= pos.y () + rdesc.RADIUS * 0.6;		
		
		for (yi = miny; yi <= maxy; yi += h)
		{
			for (xi = minx; xi <= maxx; xi += h)
			{
				i 	= ctog_x (xi);
				j 	= ctog_y (yi);
				if ((i < 1) || (j < 1) || (i > size_x - 2) || (j > size_y - 2))
					continue;
					
				set_empty (i, j);	
			}					
		}		
	}

	public void obstacle (Position pos, double radius)
	{
		int				i, j;
		double			xi, yi;
		double			minx, miny;
		double			maxx, maxy;
		
		minx	= pos.x () - radius * 0.6;		
		miny	= pos.y () - radius * 0.6;		
		maxx	= pos.x () + radius * 0.6;		
		maxy	= pos.y () + radius * 0.6;		
		
		for (yi = miny; yi <= maxy; yi += h)
		{
			for (xi = minx; xi <= maxx; xi += h)
			{
				i 	= ctog_x (xi);
				j 	= ctog_y (yi);
				if ((i < 1) || (j < 1) || (i > size_x - 2) || (j > size_y - 2))
					continue;
					
				set_occupied (i, j);	
			}					
		}		
	}

	// Calcula las celdas ocupadas por el robot, con posicion pos, y con dimensiones
	// maximas y minimas en cada eje (respecto al cero del robot)
	// Las modificaciones hechas son temporales.
	public void robotOccupied (Position pos, Point2 max, Point2 min, String robot)
	{
		int i, j, m;
		double x, y;
		int maxx, maxy, minx, miny;
		Position[] pt = {
				Transform2.toGlobal(new Position(max.x(), max.y(), 0), pos),
				Transform2.toGlobal(new Position(max.x(), min.y(), 0), pos),
				Transform2.toGlobal(new Position(min.x(), min.y(), 0), pos),
				Transform2.toGlobal(new Position(min.x(), max.y(), 0), pos) };
		Line2[] lines = {
				new Line2(pt[0].x(), pt[0].y(), pt[1].x(), pt[1].y()),
				new Line2(pt[1].x(), pt[1].y(), pt[2].x(), pt[2].y()),
				new Line2(pt[2].x(), pt[2].y(), pt[3].x(), pt[3].y()),
				new Line2(pt[3].x(), pt[3].y(), pt[0].x(), pt[0].y()) };
		
		LinkedList<double[]> changes = new LinkedList<double[]>();
				
		minx = ctog_x( Math.min(
		        Math.min(pt[0].x(), pt[1].x()), 
		        Math.min(pt[2].x(), pt[3].x())  ));
		miny = ctog_y( Math.min(
                Math.min(pt[0].y(), pt[1].y()), 
                Math.min(pt[2].y(), pt[3].y())  ));
		
		maxx = ctog_x( Math.max(
                Math.max(pt[0].x(), pt[1].x()), 
                Math.max(pt[2].x(), pt[3].x())  ));
		maxy = ctog_y( Math.max(
                Math.max(pt[0].y(), pt[1].y()), 
                Math.max(pt[2].y(), pt[3].y())  ));
		
		restartChanges(robot);
		for (i = minx; i <= maxx; i++) {
		    for (j = miny; j <= maxy; j++) {
		        if ( ((i < 1) || (j < 1) || (i > size_x - 2) || (j > size_y - 2)) )
		            continue;
		        
		        for(m = 0; m<lines.length; m++){
		            x = gtoc_x(i);
		            y = gtoc_y(j);
		            if(lines[m].distance(x,y)<h){
		                
		                // Save changes
		                double[] change = {i, j, empty[i][j], occupied[i][j], free[i][j]};
		                changes.add(change);
		                
		                set_occupied(i, j);
		                break;
		            }     
		        }	        
		    }
		}
		
		tempChanges.put(robot,changes);	    
	}
		
	/*
	 * Set new robot position, while erasing all cells occuppied by the robot.
	 * Then we get clearer maps. NOTE: the robot orientation and actual shape is
	 * not taken into account.
	 */
	public void robotOccupied (double x, double y, int rad)
	{
		int				i, j;
		int				xx, yy;
		
		xx	= ctog_x (x);
		yy	= ctog_y (y);
		for (i = xx-rad; i < xx+rad; i++)
			for (j = yy-rad; j < yy+rad; j++)
			{
				if ((i < 1) || (j < 1) || (i > size_x - 2) || ( j > size_y - 2))
					continue;
				
				set_occupied (i, j);
			}
	}

	public void robotEmpty (double x, double y, int rad)
	{
		int				i, j;
		int				xx, yy;
		
		xx	= ctog_x (x);
		yy	= ctog_y (y);
		for (i = xx-rad; i < xx+rad; i++)
			for (j = yy-rad; j < yy+rad; j++)
			{
				if ((i < 1) || (j < 1) || (i > size_x - 2) || ( j > size_y - 2))
					continue;
				
				set_empty (i, j);
			}
	}
	
	// Restaura los valores del grid almacenados para cada robot
	public void restartChanges(String robot){
	    double[] change;
	    LinkedList changes;
	    int i, j;
	    
	    changes = (LinkedList) tempChanges.get(robot);
	    if(changes == null) return;
	    
	    while(!changes.isEmpty()){
	        change = (double[]) changes.removeLast();
	        i = (int)change[0];
	        j = (int)change[1];
	        empty[i][j] = change[2];
	        occupied[i][j] = change[3];
	        free[i][j] = change[4];
	    }
	}
	
	public void restartChangesPrint(String robot){
	    double[] change;
	    LinkedList changes;
	    int i, j;
	    
	    changes = (LinkedList) tempChanges.get(robot);
	    if(changes == null) return;
	    
	    while(!changes.isEmpty()){
	        change = (double[]) changes.removeLast();
	        i = (int)change[0];
	        j = (int)change[1];
	        System.out.println("Restaurando "+i+","+j+" empty->"+empty[i][j]+" a "+change[2]+" occupied->"+occupied[i][j]+" a "+change[3]+" free->"+free[i][j]+" a "+change[4]);
	        empty[i][j] = change[2];
	        occupied[i][j] = change[3];
	        free[i][j] = change[4];
	    }
	}
	
	// Restaura los valores del grid almacenados para cada robot
	private void restartChanges1(String robot){
	    double[] change;
	    LinkedList changes;
	    int i, j;
	    
	    changes = (LinkedList) tempChanges.get(robot);
	    if(changes == null) return;
	    	    
	    for(int k = 0; k < changes.size(); k++){
	        change = (double[]) changes.get(k);
	        i = (int)change[0];
	        j = (int)change[1];
	        empty[i][j] = change[2];
	        occupied[i][j] = change[3];
	        free[i][j] = change[4];
	    }
	    tempChanges.remove(robot);
	}
	
	// Subclasses MUST implement these methods
	public abstract void set_occupied (int i, int j);								// Set up a cell as occupied
	public abstract void set_empty (int i, int j);									// Set up a cell as empty
	public abstract void set_unknown (int i, int j);								// Set up a cell as unknown

	public abstract void update (Position pos, LPOSensorScanner scan);				// Virtual scanner sensor
	public abstract void update (Position pos, int s, double dist);					// ToF sensor (sonar, infrared, virtual)
}
