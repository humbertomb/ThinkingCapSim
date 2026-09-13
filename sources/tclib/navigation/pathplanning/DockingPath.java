/*
 * (c) 2004-2026 Humberto Martinez Barbera
 */

package tclib.navigation.pathplanning;

import devices.pos.Path;
import devices.pos.Position;
import wucore.utils.math.Angles;

/**
 * Docking manoeuvre of the iFork: the control points of the path that takes
 * the vehicle from its pose beside a dock (the waypoint, heading away from
 * the dock) to the dock pose, made of two circular arcs joined by a straight
 * segment and a final straight approach. The path planner (source
 * {@link GridPath#POINTS}) and the world editor (preview of the docking
 * trajectories of a waypoint) share this code.
 */
public final class DockingPath
{
	/** Extension beyond the goal used by the iFork navigation (GNodeFL.createMaps: setExtension (5.0)). */
	static public final double		NAV_EXTENSION	= 5.0;

	private DockingPath ()			{ }

	/**
	 * Labels of the docks a waypoint is linked to in the topological map of a
	 * world: the destinations of its arcs, in the graph of the zone that holds
	 * it, that are docks (empty when the world has no topology).
	 */
	static public java.util.List<String> linkedDocks (tc.shared.world.World world, String waypoint)
	{
		java.util.List<String>	docks = new java.util.ArrayList<String> ();
		tclib.planning.htopol.HTopolMap	topol = world.topology ();
		if (topol == null)		return docks;
		for (int i = 0; i < topol.numNodes (); i++)
		{
			tclib.utils.graphs.Graph	g = ((tclib.planning.htopol.GNodeFL) topol.getNode (i)).getGraph ();
			tclib.utils.graphs.GNode	n = (g != null) ? g.getNode (waypoint) : null;
			if (n == null)		continue;
			for (int j = 0; j < n.nList (); j++)
			{
				String	label = g.getNode (n.getList (j)).getLabel ();
				if ((world.dock (label) != null) && !docks.contains (label))		docks.add (label);
			}
		}
		return docks;
	}

	/**
	 * Adds to <code>path</code> the control points from <code>robot</code> to
	 * <code>goal</code> (the goal included). Returns the number of points of
	 * the final straight approach, which the caller uses to anchor the
	 * extension beyond the goal.
	 */
	static public int controlPoints (Path path, Position robot, Position goal)
	{
		int			plast = 0;
		double		alf, dst;
		
//			double distx;
		double disty, del;			
		double x, y, alpha, inicio, dif;
		double rx, ry, fin, radio;		
		int puntos;
		//	Radio Minimo = Length / tg(delta_max) = 1.008/tg(60)= 0.58
		//  Con radio 0.7=>55°, 0.8=>51.5, 0.9=>48.2°

		//  CALCULO PRIMER SEMICIRCULO RMIN	//

		alf     = Math.atan2(robot.y()-goal.y(),robot.x()-goal.x()) - goal.alpha();
		dst		= robot.distance (goal);
//			distx   = dst * Math.cos (alf);
		disty   = dst * Math.sin (alf);
		del = Angles.radnorm_180(goal.alpha() - robot.alpha()) * Angles.RTOD;

		// CASO GIRO 270°-360°
		if( ((del > -90.0)&&(del < 0.0)&&(disty>0)) || ((del > 0.0)&&(del < 90.0)&&(disty<0)) ){		
			/*path.reset();
			path.add(robot.x(),robot.y(),0.0);
			path.add(robot.x()+0.1 * Math.cos(robot.alpha()+Math.PI),robot.y()+0.1*Math.sin(robot.alpha()+Math.PI),0.0);
			path.add(robot.x()+0.4 * Math.cos(robot.alpha()+Math.PI),robot.y()+0.4*Math.sin(robot.alpha()+Math.PI),0.0);

			dst		= distx;
			x = goal.x() + dst * Math.cos(goal.alpha());
			y = goal.y() + dst * Math.sin(goal.alpha());
			path.add(x, y, goal.alpha());

			dst		= 0.75 * dst;
			plast	= (int)(dst/0.2); //puntos cada 0.2m
			for(int i = 1;i < plast; i++){
				x = goal.x() + (1-(double)i/(double)plast) * dst * Math.cos(goal.alpha());
				y = goal.y() + (1-(double)i/(double)plast) * dst * Math.sin(goal.alpha());
				path.add(x, y, goal.alpha());	
			}*/
						    
		    double ang;
		    
		    path.reset();
		    ////			 Primer Semicirculo ////			
			radio = 0.7;	// Radio primer semicirculo
			puntos = 10;  	// Numero de puntos del semicirculo
			if(Math.abs(disty) < 1.0)
				radio = 0.6;
			
			// Linea
			if(disty < 0)
			    ang = goal.alpha() + Math.PI / 2;
			else
			    ang = goal.alpha() - Math.PI / 2;
			dst = Math.abs(disty) - radio;
			x = robot.x() + dst * Math.cos(ang);
			y = robot.y() + dst * Math.sin(ang);
			
			Position pi = robot;
//				Position pf = new Position(x,y,0);	
			puntos = 5; //puntos cada 0.2m
			
			for(int i = 1;i < puntos; i++){
				x = pi.x() + ((double)i/(double)puntos) * dst * Math.cos(ang);
				y = pi.y() + ((double)i/(double)puntos) * dst * Math.sin(ang);
				path.add(x, y, ang);
			}
			
			x = robot.x() + dst * Math.cos(ang);
			y = robot.y() + dst * Math.sin(ang);
			//	Calculo del radio del segundo semicirculo		
			rx = x + radio * Math.cos(goal.alpha()+Math.PI);
			ry = y + radio * Math.sin(goal.alpha()+Math.PI);
			
			// Genera segundo semicirculo
			puntos = 10;
			alpha = goal.alpha();
			dif = Math.PI /2;
			for(int i = 0; i < puntos; i++){
				path.add(rx+radio*Math.cos(alpha),ry+radio*Math.sin(alpha),0.0);
				if(disty<0) alpha = alpha + Math.abs(dif)/(puntos-1);
				else		alpha = alpha - Math.abs(dif)/(puntos-1);
			}
			
			//// Recta Final ////
			dst		= path.last(-1).distance (goal);
			plast	= (int)(dst/0.2); //puntos cada 0.2m
			for(int i = 1;i < plast; i++){
				x = goal.x() + (1-(double)i/(double)plast) * dst * Math.cos(goal.alpha());
				y = goal.y() + (1-(double)i/(double)plast) * dst * Math.sin(goal.alpha());
				path.add(x, y, goal.alpha());	
			}
		}		
		// // CASO dy MENOR A 0.5 METROS
		else if((Math.abs(disty)<0.5))
		{	
			dst		= 0.90 * dst;
			plast	= (int)(dst/0.2); //puntos cada 0.2m
			path.reset();
			path.add(robot.x(),robot.y(),0.0);
			for(int i = 1;i < plast; i++){
				x = goal.x() + (1-(double)i/(double)plast) * dst * Math.cos(goal.alpha());
				y = goal.y() + (1-(double)i/(double)plast) * dst * Math.sin(goal.alpha());
				path.add(x, y, goal.alpha());	
			}
		}		
		else	// CASO GIRO NORMAL
		{	 						
			//// Primer Semicirculo ////			
			radio = 0.7;	// Radio primer semicirculo
			puntos = 10;  	// Numero de puntos del semicirculo
			if(Math.abs(disty) < 1.0)
				radio = 0.6;
		
			// Calculo del radio							
			if(disty<0){
				rx = robot.x() + radio * Math.cos(robot.alpha()-Math.PI/2);
				ry = robot.y() + radio * Math.sin(robot.alpha()-Math.PI/2);
				inicio = Math.atan2(robot.y()-ry,robot.x()-rx);
				fin = goal.alpha();
				dif = fin-inicio; 
			}
			else{
				rx = robot.x() + radio * Math.cos(robot.alpha()+Math.PI/2);
				ry = robot.y() + radio * Math.sin(robot.alpha()+Math.PI/2);
				inicio = Math.atan2(robot.y()-ry,robot.x()-rx);
				fin = goal.alpha();
				dif = -(fin-inicio);			
			}
			if (dif<0) dif+= Math.PI *2;
			if (dif>Math.PI*2) dif-= Math.PI *2;

			// Generando primer semicirculo
			path.reset();
			alpha = inicio;
			for(int i = 0; i < puntos; i++){
				path.add(rx+radio*Math.cos(alpha),ry+radio*Math.sin(alpha),0.0);
				if(disty<0) alpha = alpha + Math.abs(dif)/(puntos-1);
				else		alpha = alpha - Math.abs(dif)/(puntos-1);
			}

			//// Segundo Semicirculo ////
			alf     = Math.atan2(path.at(puntos-1).y()-goal.y(),path.at(puntos-1).x()-goal.x()) - goal.alpha();
			dst		= path.at(puntos-1).distance (goal);
//				distx   = dst * Math.cos (alf);
			disty   = dst * Math.sin (alf);

			radio = 0.8;	// Radio maximo segundo semicirculo
			if(Math.abs(disty) < radio) radio = Math.abs(disty);
			dst = Math.abs(disty) - radio;
			if(dst<0) dst = 0;

			// Calculo del radio del segundo semicirculo		
			fin = 0;
			if(disty<0){
				x = path.at(puntos-1).x() + dst * Math.cos(goal.alpha()+Math.PI/2);
				y = path.at(puntos-1).y() + dst * Math.sin(goal.alpha()+Math.PI/2);
				rx = x + radio * Math.cos(goal.alpha()+Math.PI);
				ry = y + radio * Math.sin(goal.alpha()+Math.PI);
				inicio = Math.atan2(y-ry,x-rx);
				fin = goal.alpha()+Math.PI/2;
				dif = fin-inicio;
			}
			else{
				x = path.at(puntos-1).x() + dst * Math.cos(goal.alpha()-Math.PI/2);
				y = path.at(puntos-1).y() + dst * Math.sin(goal.alpha()-Math.PI/2);
				rx = x + radio * Math.cos(goal.alpha()-Math.PI);
				ry = y + radio * Math.sin(goal.alpha()-Math.PI);
				inicio = Math.atan2(y-ry,x-rx);
				fin = goal.alpha()-Math.PI/2;
				dif = -(fin-inicio);
			}
			if (dif<0) dif+= Math.PI *2;
			if (dif>Math.PI*2) dif-= Math.PI *2;

			//// Recta entre 1 y 2 semicirculo ////
			Position pi = path.last(-1);
			Position pf = new Position(rx+radio*Math.cos(inicio),ry+radio*Math.sin(inicio),0.0);	
			puntos = (int)(pi.distance(pf)/0.2); //puntos cada 0.2m
			double ang = Math.atan2(pf.y()-pi.y(),pf.x()-pi.x());
			for(int i = 1;i < puntos; i++){
				x = pi.x() + ((double)i/(double)puntos) * pi.distance (pf) * Math.cos(ang);
				y = pi.y() + ((double)i/(double)puntos) * pi.distance (pf) * Math.sin(ang);
				path.add(x, y, goal.alpha());	
			}

			// Genera segundo semicirculo
			puntos = 10;
			alpha = inicio;
			for(int i = 0; i < puntos; i++){
				path.add(rx+radio*Math.cos(alpha),ry+radio*Math.sin(alpha),0.0);
				if(disty<0) alpha = alpha + Math.abs(dif)/(puntos-1);
				else		alpha = alpha - Math.abs(dif)/(puntos-1);
			}
	
			//// Recta Final ////
			dst		= path.last(-1).distance (goal);
			plast	= (int)(dst/0.2); //puntos cada 0.2m
			for(int i = 1;i < plast; i++){
				x = goal.x() + (1-(double)i/(double)plast) * dst * Math.cos(goal.alpha());
				y = goal.y() + (1-(double)i/(double)plast) * dst * Math.sin(goal.alpha());
				path.add(x, y, goal.alpha());	
			}
		}
		path.add (goal);
		return plast;
	}

	/**
	 * The docking trajectory (B-spline through the control points, as the
	 * navigation module builds it) from the robot pose to the goal pose.
	 * @param extension  metres the path is extended beyond the goal (5 m in the iFork navigation)
	 */
	static public Path generate (Position robot, Position goal, double extension)
	{
		Path	path = new Path (1000);
		int		plast = controlPoints (path, robot, goal);
		path.add (GridPath.generate_extension (path.last (-plast/2-1), goal, extension));
		return new BSpline (path, robot.alpha () + Math.PI);
	}
}
