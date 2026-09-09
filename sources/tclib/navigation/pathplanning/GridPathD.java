/*	Focused D*
 * (c) 2002 Jose Antonio Marin
 */
 
package tclib.navigation.pathplanning;

import tclib.navigation.mapbuilding.*;

import devices.pos.*;
import wucore.utils.math.*;

public class GridPathD extends GridPath
{
	// Global variables
	public static final int			D_STAR_MI	= 0;				/* Use a Focused D* Min Init algorithm		*/
	public static final int			D_STAR_FI	= 1;				/* Use a Focused D* Full Init algorithm		*/
	
	protected double[][]				g;							/* Optimal cost from start to cell (bound) 	*/
	
	protected boolean[][]			closed;						/* Is the cell in the closed list?			*/
	protected boolean[][]			open;						/* Is the cell in the open list?			*/
	
	// D* algorithm
	protected	double				dcurr;
	protected PathNode				Rcurr;
	protected PathNode				R;
	protected PathNode[][]			r;
	protected PathNode[][]			backPointer;
	protected double[][]				fb;
	protected double[][]				k;
	protected double[][]				last_gcost;
	protected double[]				lgoal;
	protected boolean				first_path;
	protected PathNode[][]			list;
	protected int					min_x;
	protected int					min_y;
	protected PathNode				Source, Goal;
	protected boolean				time_limit;
	protected int					method;
	protected int 					cost_changed;
	protected int					expcnt;
	protected int					max_timeout = 0;
	
	// Constructors
	public GridPathD (Grid grid)
	{
		super (grid);
		
		closed 		= new boolean[size_x][size_y];
		open 		= new boolean[size_x][size_y];
		g 			= new double[size_x][size_y];
		
		// D* algorithm
		Rcurr 		= new PathNode();
		R			= new PathNode();
		r		 	= new PathNode[size_x][size_y];
		backPointer = new PathNode[size_x][size_y];
		fb 			= new double[size_x][size_y];
		k			= new double[size_x][size_y];
		last_gcost	= new double[size_x][size_y];
		lgoal 		= new double[2];
		list		= new PathNode[size_x][size_y];
		Source		= new PathNode();
		Goal		= new PathNode();
		first_path  = true;
		time_limit	= false;
		method		= D_STAR_MI;
		heuristic = FOLLOW_GN;
		cost_changed = 0;
		
		
		for (int i = 0; i < size_x; i++)
			for(int j = 0; j < size_y; j++){
			r[i][j] 			= new PathNode();
			list[i][j]			= new PathNode();
		}
		
				
		plan_reset ();
	}
		
	// Accessors
	public final void		method (int method)			{ this.method = method; }
	
	// Instance methods 

	/* -----------------------------------------------
	   Cost distribution function.
	   ------------------------------------------------ */
	
	
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

	protected void plan_reset ()
	{ 
		int	i, j;

		// Initialize the cost map
		for (i = 0; i < size_x; i++)
			for (j = 0; j < size_y; j++)
				{
					f[i][j]			= Km;
					g[i][j]			= Km;
					k[i][j]			= Km;
					fb[i][j]		= Km;
					closed[i][j]	= false;
					open[i][j]		= false;
					backPointer[i][j]	= new PathNode();
				}
				// Initialize open-list and path-list pointers
		min_x       = NULL;
		min_y		= NULL;
	}

	protected void plan_init (Position pos)
	{
		long		st;
		st	= System.currentTimeMillis ();
		
		expcnt	= 0;		
		locked		= true;
		finished	= false;
		lck_cnt		= 0;
		f1=f;
		f2=f;					
		location (pos);			
		lck_time = System.currentTimeMillis () - st;	
	}		

	protected void plan_step ()
	{
		long		st;
		st	= System.currentTimeMillis ();	
		
		Goal.x = goal_x;	Goal.y = goal_y;
		Source.x = robot_x;	Source.y = robot_y;

		if((goal_x!=lgoal[0])||(goal_y!=lgoal[1])){
			if(debug) System.out.println("  [GridPathD] Calculando nuevo Path ["+goal_x+","+goal_y+"] a ["+lgoal[0]+","+lgoal[1]+"]");
			first_path = true;
			time_limit = false;
		}
		lgoal[0] = goal_x; lgoal[1] = goal_y;
			
		if(move_robot(Source, Goal)){
			finished 	=	true;
			locked		=	false;
			time_limit	=	false;
			expanded = expcnt;
			generate_path ();
			add_time ((double) (st = (System.currentTimeMillis () - st + lck_time)));
			if(debug) System.out.println("  [GridPathD] Finished PathPlaning. time="+st+" expcnt="+expcnt+" cost_changed="+cost_changed);
			cost_changed = 0;
			return;
		}
		if(time_limit==true){
			lck_time += System.currentTimeMillis () - st;
				lck_cnt ++;
		}
		else{
			finished 	=	true;
			locked		=	false;			
			add_time ((double) (st = (System.currentTimeMillis () - st + lck_time)));
			//System.out.println(st);
		}	
	}

	protected double plan_path ()
	{
	    if(debug) System.out.println("  [GridPathD] Calculando Path. Heuristic="+heuristic);
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
				finished = false;
				first_path = true;	// Se genera el path de nuevo
				time_limit = false;
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
		
		if(debug) System.out.println("  [GridPathD] PathGn Calculado Correctamente");
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
		int count = 0;
		
		path.reset ();

		lx	= robot_x;
		ly	= robot_y;
		
		a	= Angles.radnorm_180 (Math.atan2 (ly - Goal.x, lx - Goal.y) + Math.PI);

		path.add (grid.gtoc_x (lx), grid.gtoc_y (ly), a);
		
		PathNode pn = new PathNode();
		pn.x = robot_x; pn.y = robot_y;
		while ((pn.x != Goal.x)||(pn.y != Goal.y)) 
		{
			pn  = backPointer[pn.x][pn.y]; 
			x	= pn.x;
			y	= pn.y;
			a	= Angles.radnorm_180 (Math.atan2 (ly - y, lx - x) + Math.PI);			
			path.add (grid.gtoc_x (x), grid.gtoc_y (y), a);
			lx	= x;
			ly	= y;
			if((count++) > MAX_PATH) {
				System.out.println("GridPathD.plan_path_fn(): Numero de iteraciones excedidas");			
				finished = false;
				first_path = true;	// Se genera el path de nuevo
				time_limit = false;
				return 0.0;
			}
		}
		path.add (goal);
		
		if(debug) System.out.println("  [GridPathD] PathFn Calculado Correctamente");
		return robot.alpha ();
	}


	// Move the robot from state S through the environment to G along an optimal traverse
	protected boolean move_robot(PathNode S, PathNode G){
		double[] val = {0.0, 0.0};
		long st = System.currentTimeMillis();
		if(debug) System.out.println("  [GridPathD] Move Robot S=["+S.x+","+S.y+"] G=["+G.x+","+G.y+"]");
		
		if(first_path == true){
			if(debug) System.out.println("  [GridPathD] Calculate First Path");
			if(time_limit == false){
			    if(debug) System.out.println("  [GridPathD] Calculando desde ["+S.x+","+S.y+"] hasta ["+G.x+","+G.y+"]");
				plan_reset();
			
				// Saved the last cost value
				for(int i=0; i<size_x; i++)
					for(int j=0; j<size_y;j++)
						last_gcost[i][j] = free[i][j];
				
					
				dcurr = 0;
				Rcurr.x = S.x; Rcurr.y = S.y;
				insert(G.x, G.y, 0.0);
			}
			//System.out.println("--------------- Inicilizando la primera ruta");
			while(((closed[S.x][S.y]==false) || (method == D_STAR_FI)) && (val != null)){
				val = process_state();
				if((System.currentTimeMillis()-st)>TIME_STEP){
					time_limit = true;
					if(debug) System.out.println("  [GridPathD] Time limit exceded");
					return false;
				}
			}
			time_limit = false;
			if( (closed[S.x][S.y]||open[S.x][S.y])==false ){
				System.out.println("  [GridPathD] Ruta no encontrada");
				return false;
			}
			first_path = false;
			R.x = S.x; R.y = S.y;
			if(debug) System.out.println("  [GridPathD] Calculado correctamente el primer Path. Expandidos="+expcnt);
			max_timeout = 0;
			return true;
		}
		
		//if(time_limit == false){
		    val = min_val();
		    if(debug) System.out.println("  [GridPathD] Iniciando Replanificacion");
			
			R.x = robot_x;
			R.y = robot_y;
		
			if((Rcurr.x != R.x)||(Rcurr.y != R.y)){
				dcurr += hcost(Rcurr.x, Rcurr.y) + Double.MIN_VALUE;
				Rcurr.x = R.x;
				Rcurr.y = R.y;
			}

			// Check if cost function is changed
			
			for(int i=0; i<size_x; i++)
				for(int j=0; j<size_y;j++){
					if(free[i][j]!=last_gcost[i][j]){
						if(debug) System.out.println("  [GridPathD] Modify Cost("+i+","+j+") "+last_gcost[i][j]+" to "+free[i][j]);
						val = change_cost(i,j);
						last_gcost[i][j] = free[i][j];
						cost_changed ++;
					}
					//if(gcost(i,j)!=last_gcost[i][j]){
					//val = modify_cost(i,j);
					//last_gcost[i][j] = gcost(i,j);
					//}
			}
		//}
		
		st = System.currentTimeMillis();
		if(debug && val != null && cost(R.x,R.y) != null)
			System.out.println("  [GridPathD] Replaning1 val=["+val[0]+","+val[1]+"] cost(R)=["+cost(R.x,R.y)[0]+","+cost(R.x,R.y)[1]+"]");

		while((val!=null) && less(val,cost(R.x,R.y))){
			val = process_state();
			if(debug && val != null && cost(R.x,R.y) != null)
				System.out.println("  [GridPathD] Replaning2 val=["+val[0]+","+val[1]+"] cost(R)=["+cost(R.x,R.y)[0]+","+cost(R.x,R.y)[1]+"]");

			if((System.currentTimeMillis()-st)>TIME_STEP){
				time_limit = true;
				if(debug){
					System.out.println("  [GridPathD] Excedido el tiempo en la Replanificacion. cost_changed="+cost_changed+" exp="+expcnt+" finished="+finished);
					System.out.println("R=["+R.x+","+R.y+"] robot=["+robot_x+","+robot_y+"] Rcurr=["+Rcurr.x+","+Rcurr.y+"] dcurr="+dcurr);
				}
				
				if(max_timeout++ > 10){
					System.out.println("  [GridPathD] Max TimeStep exceded. TIME_STEP="+TIME_STEP);
					finished = false;
					first_path = true;	// Se genera el path de nuevo
					time_limit = false;
					max_timeout = 0;
				}
				return false;
			}
		}

		if(debug){
			System.out.println("  [GridPathD] Replanificacion terminada. Expandidos="+expcnt+" dcurr="+dcurr);
			if(val!=null && cost(R.x,R.y)!=null) System.out.println(" val=["+val[0]+","+val[1]+"] cost(R)=["+cost(R.x,R.y)[0]+","+cost(R.x,R.y)[1]+"]");
		}
		max_timeout = 0;
		time_limit = false;
		return true;
		
		

	}

	// The cost changes are propagated and new path are computed
	protected double[] process_state(){
		int x,y,i,j;
		double gn;
		expcnt++;
		
		// The state of lowest fb value is removed from the open list
		PathNode X = min_state();
		if (X == null) return null;
		x = X.x; y = X.y;
		if(debug) System.out.println("  [GridPAthD] Process State ["+x+","+y+"] f="+f[x][y]+" k="+ k[x][y]+" g="+g[x][y]);
		double[] val = {f[x][y],k[x][y]};
		double kval = k[x][y];
		delete(x,y);
		
		if(kval<g[x][y]){
			for(i = (x-1) ; i <= (x+1) ; i++)
				for(j = (y-1) ; j <= (y+1) ; j++){
					if( (x == i) && (y == j) ) continue;
					if((i<=0)||(j<=0)||(i>size_x-1)||(j>size_y-1)) continue;
					if( (x == i) || (y == j) ) 	gn = g[i][j] + gcost(x,y);
					else						gn = g[i][j] + Math.sqrt(2)*gcost(x,y);
					if( (closed[i][j] || open[i][j]) && lesseq(cost(i,j),val) && (g[x][y] > gn) ){
						backPointer[x][y].x = i; backPointer[x][y].y = j;
						g[x][y] = gn;
					}
				}
		}
		
		if(kval==g[x][y]){
			for(i = (x-1) ; i <= (x+1) ; i++)
				for(j = (y-1) ; j <= (y+1) ; j++){
					if( (x == i) && (y == j) ) continue;
					if((i<=0)||(j<=0)||(i>size_x-1)||(j>size_y-1)) continue;
					if( (x == i) || (y == j) ) 	gn = g[x][y] + gcost(i,j);
					else						gn = g[x][y] + Math.sqrt(2)*gcost(i,j);
					if( ((closed[i][j] || open[i][j])==false) || ((backPointer[i][j].x==x)&&(backPointer[i][j].y==y)&&(g[i][j]!=gn)) || (((backPointer[i][j].x!=x)||(backPointer[i][j].y!=y))&&(g[i][j]>gn)) ){
						backPointer[i][j].x = x; backPointer[i][j].y = y;
						insert(i,j,gn); 
					}
				}
		}
		
		else{
			for(i = (x-1) ; i <= (x+1) ; i++)
				for(j = (y-1) ; j <= (y+1) ; j++){
					if( (x == i) && (y == j) ) continue;
					if((i<=0)||(j<=0)||(i>size_x-1)||(j>size_y-1)) continue;
					if( (x == i) || (y == j) ) 	gn = g[x][y] + gcost(i,j);
					else						gn = g[x][y] + Math.sqrt(2)*gcost(i,j);
					if( ((closed[i][j] || open[i][j])==false) || ((backPointer[i][j].x==x)&&(backPointer[i][j].y==y)&&(g[i][j]!=gn)) ){
						backPointer[i][j].x = x; backPointer[i][j].y = y;
						insert(i,j,gn);
 
					}
					else{
						if( ((backPointer[i][j].x!=x)||(backPointer[i][j].y!=y)) && (g[i][j]>gn) && (closed[x][y]==true)){
							insert(x,y,g[x][y]);

						}
						else{
							if( (x == i) || (y == j) ) 	gn = g[i][j] + gcost(x,y);
							else						gn = g[i][j] + Math.sqrt(2)*gcost(x,y);
							if( ((backPointer[i][j].x!=x)||(backPointer[i][j].y!=y)) && (g[x][y]>gn) && (closed[i][j]==true) && less(val,cost(i,j)) ){
								insert(i,j,g[i][j]);
							}
						}
					}
					
					
					
					
				}		
		
		}
		
		return min_val();	
	}

	// Deletes state X from the open list an sets closed(X) = true
	protected void delete(int x, int y){
		int i,j,lx,ly;
		i 	= min_x;	j 	= min_y;	
		lx 	= min_x;	ly	= min_y;

		closed[x][y] = true;
		open[x][y] = false;
		
		if((x==min_x)&&(y==min_y)) {
			min_x = list[x][y].x;
			min_y = list[x][y].y;
			if(debug) System.out.println("  [GridPathD] Delete Min Node ["+x+","+y+"] f="+f[x][y]+" k="+ k[x][y]+" g="+g[x][y]);
			return;
		}
		
		while((i != NULL)&&(j != NULL)){	// Para hasta encontrar el sitio donde meterlo en la lista
			i = list[lx][ly].x; j = list[lx][ly].y; 
			if((i==x)&&(j==y)){
				list[lx][ly].x = list[x][y].x;	// Pasa la informacion del borrado al anterior
				list[lx][ly].y = list[x][y].y;			
				if(debug) System.out.println("  [GridPathD] Delete Node ["+x+","+y+"] f="+f[x][y]+" k="+ k[x][y]+" g="+g[x][y]);
				return;
			}
			lx = i; ly = j;
		}		
		
		if(debug) System.out.println("  [GridPathD] Error al borrar en la lista");


	}
	
	
	// Set open(X) = true and insert X on the open list acocording th the vector <fb, f, k>
	protected void put_state(int x, int y){
		int i,j,lx,ly;
		i 	= min_x;	j 	= min_y;	
		lx 	= min_x;	ly	= min_y;
		open[x][y] = true;
		closed[x][y] =false;

		if((i == NULL)||(j == NULL)){		// Si la lista esta vacia pone el primer elemento
			list[x][y].x = NULL;
			list[x][y].y = NULL;
			min_x = x;	min_y = y;
			if(debug) System.out.println("  [GridPathD] Put initial Node ["+x+","+y+"] f="+f[x][y]+" k="+ k[x][y]+" g="+g[x][y]);
			return;
		}
		
		while((i != NULL)&&(j != NULL)){	// Para hasta encontrar el sitio donde meterlo en la lista
			if( fb[x][y] < fb[i][j] )	break;
			if( (fb[x][y] == fb[i][j]) && (f[x][y] < f[i][j]) ) break;
			if( (fb[x][y] == fb[i][j]) && (f[x][y] == f[i][j]) && (k[x][y] < k[i][j]) ) break;
			lx = i; ly = j;
			i = list[lx][ly].x;
			j = list[lx][ly].y; 
		}
		
				
		if((i==min_x)&&(j==min_y)){				// Si el nuevo elemento es el primero
			list[x][y].x = min_x;
			list[x][y].y = min_y;
 			min_x = x; min_y = y;
			if(debug) System.out.println("  [GridPathD] Put first Node ["+x+","+y+"] f="+f[x][y]+" k="+ k[x][y]+" g="+g[x][y]);
			return;
		}
		
		list[x][y].x = list[lx][ly].x;	// Mete nodo en medio o al final
		list[x][y].y = list[lx][ly].y;
		if(debug) System.out.println("  [GridPathD] Put Node ["+x+","+y+"] f="+f[x][y]+" k="+ k[x][y]+" g="+g[x][y]);

		list[lx][ly].x = x;
		list[lx][ly].y = y; 
		
	}
	
	
	
	// Return the state on the open list with minimum vector value (null if the list is empty)
	protected PathNode get_state(){
		PathNode pn = new PathNode();
		if( (min_x==NULL)&&(min_y==NULL) ) return null;
		pn.x = min_x;
		pn.y = min_y;
		return pn;	
	}

	
	// Changes the value of g(X) a hnew and inserts or repositions X on the open list	
	protected void insert(int x, int y, double gnew){
		if ((closed[x][y]||open[x][y]) == false) k[x][y] = gnew;	// if t(X) = NEW then k(X) = hnew
		else{														// else
			if(open[x][y] == true){									// 		if t(X) = OPEN then
				k[x][y] = Math.min(k[x][y],gnew	);					// 			k[X] = MIN(k(X),hnew)
				delete(x,y);										//			DELETE(X)		
			}
			else													// 		else
				k[x][y] = Math.min(g[x][y],gnew);					//			k[X] = MIN(h(X),hnew);
		}
		g[x][y] = gnew;												// h(X) = hnew
		r[x][y].x = Rcurr.x; r[x][y].y = Rcurr.y;					// r(X) = Rcurr
		f[x][y] = k[x][y]+hcost(x,y);								// f(X) = k(X) + GVAL(X,Rcurr)
		fb[x][y] = f[x][y]+dcurr;									// fb(X) = f(X)+dcurr 
		put_state(x,y);												// PUT-STATE(X);		
	}
	
	
	
	// Return the state on the open list with minimun fb value.
	protected PathNode min_state(){
		int x,y;
		PathNode X;
		double gnew;
		while( (X = get_state()) != null ){							// while X = GET-STATE() != NULL
			x = X.x;	y = X.y; 								
			
			if( (r[x][y].x!=Rcurr.x)||(r[x][y].y!=Rcurr.y) ){		// 		if r(X) != Rcurr then
				gnew = g[x][y];										//			hnew = h(X)
				g[x][y] = k[x][y];									//			h(X) = k(X)
				delete(x,y);										//			DELETE(X)
				insert(x,y,gnew);									// 			INSERT(X,hnew)
			}
			else{													//		else
				return X;											//			return X
			}

		}
		return null;												//	return NULL
	}


	
	// Return the f(X) and k(X) values of the state on the open list with minimum fb value.
	protected double[] min_val(){
		PathNode X = min_state();									// X = MIN-STATE()
		if (X == null)	return null;								// if X = NULL then return NO-VAL
																    // else return <f(X),k(X)>
		double[] val = {f[X.x][X.y], k[X.x][X.y]};
		return val;
			
	}	


	// Computes f(X,Rcurr) = g(X)+hcost(X) y returns the vector of values <f(X,Rcurr), g(X)>
	protected double[] cost(int x, int y){
		double[] val = {g[x][y] + hcost(x,y) , g[x][y]};
		return val;
	}

	// Take a vector of values <a1, a2> for a and a vector <b1, b2> for b and return TRUE if a1<b1 or (a1=b1 and a2<b2)
	protected boolean less(double[] a, double[] b){
		if(a[0] < b[0]) return true;
		if( (a[0] == b[0]) && (a[1] < b[1])) return true;
		return false;
	}

	// Takes two vector a and b and return TRUE if a1<b1 or (a1=b1 and a2<=b2) 
	protected boolean lesseq(double[] a, double[] b){
		if((a[0]==b[0])&&(a[1]==b[1])) return true;
		return(less(a,b));
	}




	// Compute the nodes changed
	protected double[] change_cost(int x, int y){
		int			i, j;
		int			xi, yi, xf, yf;
		
		// Compute window boundaries
		xi	= x - DILATION+1;
		xf	= x + DILATION+1;
		yi	= y - DILATION+1;
		yf	= y + DILATION+1;
		
		if (xi < 1)				xi = 1;
		if (yi < 1)				yi = 1;
		if (xf > size_x - 2)	xf = size_x - 2;
		if (yf > size_y - 2)	yf = size_y - 2;

		for (i = xi; i <= xf; i++)
			for (j = yi; j <= yf; j++)
				if(closed[i][j] == true)
					insert(i,j,g[i][j]);
		return min_val();
	}

/*
	// Compute the nodes changed
	protected double[] change_cost(int x, int y){
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

		for (i = xi; i <= xf; i++)
			for (j = yi; j <= yf; j++)
				modify_cost(i,j);
		return min_val();
	}
	
	
	// The cost funtion is updated with the changed value.
	protected double[] modify_cost(int x, int y){
		for(int i=(x-1); i<=(x+1) ; i++)
			for(int j=(y-1); j<=(y+1) ; j++){
				if((x==i)&&(y==j)) continue;
				if( (i<0) || (j<0) || (i>=size_x) || (j>=size_y) ) continue; 
				if(closed[i][j] == true)
					insert(i,j,g[i][j]);

			}
		return min_val();	
	}
*/




	}
	
	


