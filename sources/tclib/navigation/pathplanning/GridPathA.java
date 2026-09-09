/*
 * (c) 2002 Humberto Martinez
 */
 
package tclib.navigation.pathplanning;

import tclib.navigation.mapbuilding.*;

import devices.pos.*;
import wucore.utils.math.*;

public class GridPathA extends GridPath
{
	protected double[][]				g;								/* Optimal cost from start to cell (bound) 	*/
	protected double[][]				h;								/* Optimal cost from cell to goal (bound) 	*/
			
	protected PathNode[]				open_list;						/* Keep track of nodes to be expanded		*/
	protected int					size_list;						/* The free nodes list length				*/
	protected boolean[][]			closed;							/* Is the cell in the closed list?			*/
	protected boolean[][]			open;							/* Is the cell in the open list?			*/
	protected int					free_head;						/* First free node. 						*/
	protected int					open_head;						/* First node in the open list. 			*/
	protected PathNode[][]			back_ptr;						/* Pointers to generate the path			*/

	// Constructors
	public GridPathA (Grid grid)
	{
		super (grid);
		
		int			i, j;
		
		size_list 	= size_x * size_y * 10;

		closed 		= new boolean[size_x][size_y];
		open 		= new boolean[size_x][size_y];
		g 			= new double[size_x][size_y];
		h	 		= new double[size_x][size_y];
		open_list 	= new PathNode[size_list];
		back_ptr	= new PathNode[size_x][size_y];
		
		for (i = 0; i < size_list; i++)
			open_list[i] = new PathNode ();
		
		for (i = 0; i < size_x; i++)
			for (j = 0; j < size_y; j++)
				back_ptr[i][j]	= new PathNode ();
				
		plan_reset ();
	}
		
	// Instance methods 

	/* -----------------------------------------------
	   Cost distribution function.
	   ------------------------------------------------ */

// ARGGHHH!!	This method MUST be checked and tuned
//				The cost calculation is quite crappy.
	protected double gcost (int x, int y)
	{ 
		double		cts;
		int			i, j;
		int			xi, yi, xf, yf;
		
		// Compute window boundaries
		xi	= x - DILATION;
		xf	= x + DILATION;
		yi	= y - DILATION;
		yf	= y + DILATION;
		
		if (xi < 1)				xi = 1;
		if (yi < 1)				yi = 1;
		if (xf > size_x - 2)	xf = size_x - 2;
		if (yf > size_y - 2)	yf = size_y - 2;

		// Perform the dilation step on a given place
		cts = Double.MIN_VALUE;
		for (i = xi; i <= xf; i++)
			for (j = yi; j <= yf; j++)
				if (free[i][j] > cts)		cts = free[i][j];
	
//		if (free[x][y] != cts)		cts = cts / 2.0;
		if (free[x][y] < THRES)		cts = 1.0;
		
		// Perform the exponentiation step with the previous value
		return Math.exp (Kc * cts);
	}

	protected double hcost (int x, int y)
	{
		double		dx, dy;
		
		dx = (robot_x - x);
		dy = (robot_y - y);
		return Math.sqrt (dx * dx + dy * dy);
	}

	/* -----------------------------------------------
	   Get a free node off the free list.
	   ------------------------------------------------ */
	protected int get_node ()
	{
		int 	i;

		if (free_head >= open_list.length)
        	System.out.println ("--[PathA*] get_node: out of free nodes.");
        		
		i = free_head;
		free_head ++;

		return i;
	}

	/* -----------------------------------------------
	   Pop the lowest-cost node node off the open list.
	   ------------------------------------------------ */
	protected int pop_node ()
	{
		int 		i;
		PathNode 	node;

		if (open_head == NULL)		return NULL;

		i = open_head;
		node = open_list[i];

		/*--- Reset the pointers ---*/
		open_head = open_list[i].next;

		// Mark node as closed		
		open[node.x][node.y] = false;
		closed[node.x][node.y] = true;
		return i;
	}

	/* -----------------------------------------------
	   Delete a node on the open list.
	   ------------------------------------------------ */
	protected void delete_node(int x, int y){
         int last_current = open_head;
         int current = open_head;
         
         open[x][y] = false;
         closed[x][y] = true;
         
         if (( open_list[current].x == x ) && (open_list[current].y == y )){ 
	 		/*--- Reset the pointers ---*/
			open_head = open_list[current].next;
			return;        
         }
         current = open_list[current].next;
         while ( (current != NULL) && (current < open_list.length) ){
                if (( open_list[current].x == x ) && (open_list[current].y == y )) {
                   	open_list[last_current].next = open_list[current].next;
                   	return;
                }
                last_current = current;
                current = open_list[current].next;
         }
	}

	/* -----------------------------------------------
	   Put a node on the open list. In sorted order by cost.
	   ------------------------------------------------ */
	protected void insert_node (int x, int y)		
	{
		int     	i;
		int     	current;
		int     	last;
		double		fn;
		
				
		// If node is open and the cost is lower, remove it
		if (open[x][y])
			delete_node (x, y);
			
		/*--- Skip if already open ---*/
		if (!open[x][y])
		{
			fn	= f[x][y];

			i = get_node ();
			open_list[i].x		= x;
			open_list[i].y		= y;
			open_list[i].fn		= fn;

			// Update the open-nodes list
	        if (open_head == NULL)
			{
				open_list[i].next = NULL;
				open_head = i;
			}
			else
			{
                last = NULL;
                current = open_head;
                while ((current != NULL) && (current < open_list.length) && (open_list[current].fn < fn ))
                {
                	last = current;
                    current = open_list[current].next;
                }
                if (current == open_head)
                {
                    open_head = i;
                    open_list[i].next = current;
                }
                else
                {
                    open_list[last].next = i;
                    open_list[i].next = current;
                }
            }         
            
            // Mark node as open
        	open[x][y] = true;
        	closed[x][y] = false;
        }
	}

	/* -----------------------------------------------
	   Calculate the cost of travelling from a cell to the goal. 
	   Returns 'false' if no change, 'true' otherwise.
	   ------------------------------------------------ */
	protected boolean cell_cost (int x, int y)
	{
		int			i, j;
		double		temp_gn, low_gn;
		double		gn, dgn, hn;
		boolean		changed;
		int			bestx, besty;
		
		// Check if out of bounds.
		if ((x < 0) || (y < 0) || (x >= size_x) || (y >= size_y)) 
			return false;
			
		// Check if it is the start position
		if ((x == goal_x) && (y == goal_y))
		{
			g[x][y]		= 0.0;
			h[x][y]		= hcost (x, y);
			f[x][y]		= h[x][y];
			return false;
		}
		if ((x < 0) || (y < 0) || (x >= size_x) || (y >= size_y)) return false;
		
		gn		= gcost (x, y);
		hn		= hcost (x, y);
		dgn		= Math.sqrt (gn * gn + gn * gn);
		low_gn 	= Km;
		temp_gn	= Km;
		bestx	= -1; 
		besty	= -1;
		
		// Look at neighbors to find lowest potential cost
		for (i = (x - 1); i <= (x + 1); i++)
		{
			for (j = (y - 1); j <= (y + 1); j++)
			{
				if ((i == x) && (j == y))	continue;
			
				if ((i < 0) || (j < 0) || (i >= size_x) || (j >= size_y)) continue;		// Check if out of bounds.

				if ((i == x) || (j == y))
					temp_gn = g[i][j] + gn;
				else
					temp_gn = g[i][j] + dgn;
				
				if (temp_gn < low_gn)		
				{
					low_gn	= temp_gn;				
					bestx	= i; 
					besty	= j;
				}
			}
		}

		changed		= (g[x][y] > low_gn);
		if (changed)
		{
			g[x][y]				= low_gn;
			h[x][y]				= hn;
			f[x][y]				= low_gn + hn;
			back_ptr[x][y].x	= bestx;
			back_ptr[x][y].y	= besty;
			
		}

		return changed;
	}

	/* -----------------------------------------------
	   Expand the current node by computing the costs
	   of its successors, and putting them in the open
	   list if apropiate.
	   ------------------------------------------------ */
	protected void expand (int x, int y)					
	{
		int			i, j;

		// Calculate the cost for each sucessor of [x,y]
		for (i = (x - 1); i <= (x + 1); i++)
			for (j = (y - 1); j <= (y + 1); j++)
			{
				if ((i == x) && (j == y))	continue;
				
				if (cell_cost (i, j))
					insert_node (i, j);
			}
	}	

	protected void plan_reset ()
	{ 
		int	i, j;

		// Initialize the cost map
		for (i = 0; i < size_x; i++)
			for (j = 0; j < size_y; j++)
			{
				f[i][j]			= Km;
				g[i][j]			= Km;
				h[i][j]			= 0.0;
				closed[i][j]	= false;
				open[i][j]		= false;
			}
		
		// Initialize open-list and path-list pointers
		free_head	= 0;
		open_head	= NULL;
	}

	protected void plan_init (Position pos)
	{
		long		st;
		
		st	= System.currentTimeMillis ();
		
		expanded	= 0;		
		locked		= true;
		finished	= false;
		lck_cnt		= 0;
				
		// Select which buffer will be used
		lck_buffer	= !lck_buffer;
		if (lck_buffer)
			f	= f1;
		else
			f	= f2;
			
		plan_reset ();
		location (pos);
		
		cell_cost (goal_x, goal_y);
		insert_node (goal_x, goal_y);
		
		lck_time = System.currentTimeMillis () - st;
	}
	
	protected void plan_step ()
	{
		long		st;
		PathNode	node;
		int			p;

		st	= System.currentTimeMillis ();

		while (((p = pop_node ()) != NULL) && (System.currentTimeMillis () - st < TIME_STEP))
		{
			// Process the node
			node = open_list[p];
			if (closed[robot_x][robot_y]==true)
			{
				finished	= true;
				locked		= false;

				generate_path ();

				add_time ((double) (st = (System.currentTimeMillis () - st + lck_time)));
				return;
			}
						
			expanded ++;
			expand (node.x, node.y);
		}
		
		lck_time += System.currentTimeMillis () - st;
		lck_cnt ++;
	}
	
	protected double plan_path ()
	{
		switch (heuristic)
		{
		case FOLLOW_GN:
			return plan_path_gn ();
			
		case FOLLOW_FN:
		default:
			return plan_path_fn ();
		}
	}
	
	/* Nuevo metodo. Genera el path siguiendo el minimo valor de gn.
	 * Las trayectorias son menos rectas hacia el punto goal, pero son mas rectas
	 * en general, porque no suelen anternar movimientos diagonales y transversales
	 * por lo que son mas faciles de seguir (tienen prioridad las diagonales). 
	 */
	protected double plan_path_gn ()
	{				
		int			x, y;
		double		px, py, pa;
		int count = 0;
		path.reset ();

		double min;
		int minx = 0;
		int miny = 0;

		x = robot_x;
		y = robot_y;
		
		path.add (robot);

		while(!(x==goal_x && y==goal_y)){
		    min = Double.MAX_VALUE;
		    for(int i = (x-1) ; i <= (x+1) ; i++)
		        for(int j = (y-1) ; j <= (y+1) ; j++){
		            if( (x == i) && (y == j) ) continue;
		            if((i<0)||(j<0)||(i>size_x-1)||(j>size_y-1)) continue;
		            if(g[i][j] < min){
		                min = g[i][j];
		                minx = i;
		                miny = j;
		            }
		        }
		    x = minx;
		    y = miny;
		    //System.out.println("min=["+x+","+y+"]");
		    if (!(x==goal_x && y==goal_y)){
		        path.add (grid.gtoc_x (x), grid.gtoc_y (y), 0);
		    }
			if((count++) > MAX_PATH) {
				System.out.println("GridPathD.plan_path_gn(): Numero de iteraciones excedidas");			
				return 0.0;
			}
		}
		path.add (goal);
		
		px = path.at(0).x();
		py = path.at(0).y();
		pa = Angles.radnorm_180 (Math.atan2 (path.at(1).y()-path.at(0).y(),path.at(1).x()-path.at(0).x()));			
		path.at(0,px,py,pa);
		for(int i = 1; i<(path.num()-1);i++){
			px = path.at(i).x();
			py = path.at(i).y();
			pa = Angles.radnorm_180 (Math.atan2 (path.at(i+1).y()-path.at(i-1).y(),path.at(i+1).x()-path.at(i-1).x()));			
			path.at(i,px,py,pa);
		}
		px = path.last(-1).x();
		py = path.last(-1).y();
		pa = Angles.radnorm_180 (Math.atan2 (path.last(-1).y()-path.last(-2).y(),path.last(-1).x()-path.last(-2).x()));			
		path.at(path.num()-1,px,py,pa);
		
		return path.at(0).alpha();
	}

	/* Genera el path siguiendo el minimo valor de fn. Es el metodo normal,
	 *  y las trayectorias son mas rectas hacia el goal (por la heuristica),
	 *  pero mas dificil de seguir ya que alterna movimientos diagonales y 
	 *  transversales con mas frecuencia.
	 */
	protected double plan_path_fn ()
	{		
		int			x, y;
		int			lx, ly;
		double		a;
		int			count = 0;
		PathNode	pn = new PathNode();
		
		path.reset ();

		lx	= robot_x;
		ly	= robot_y;
		
		pn.x = robot_x; pn.y = robot_y;
		
		while ((pn.x != goal_x)||(pn.y != goal_y))
		{
			x	= pn.x;
			y	= pn.y;
			a	= Angles.radnorm_180 (Math.atan2 (ly - y, lx - x) + Math.PI);
			path.add (grid.gtoc_x (x), grid.gtoc_y (y), a);

			lx	= x;
			ly	= y;
			pn 	= back_ptr[pn.x][pn.y];						
			if ((count++) > MAX_PATH)
			{
				System.out.println ("--[PathA*] Error generating path");
				break;
			} 
		}
		
		path.add (goal);		
		return robot.alpha ();
	}
}
