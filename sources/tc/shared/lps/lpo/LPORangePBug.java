/*
 * Created on 26-oct-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.lps.lpo;

import java.util.LinkedList;

import devices.pos.*;
import tc.shared.world.WMZone;
import tc.shared.world.World;
import devices.pos.Position;
import wucore.utils.geom.*;
import wucore.utils.math.Angles;
import wucore.utils.math.Matrix3D;
import wucore.widgets.Model2D;
import wucore.utils.color.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class LPORangePBug extends LPORangeBuffer
{	
	// LTG processing parameters
	protected double					SZONE	= 1.5;		// Security zone for edge points (radius of polarbug circles)

	// Parametros para los datos almacenados
	static protected double				PTDIST = 0.5;		// Distancia minima entre puntos para ser almacenados.
	static protected double				DIST_DEL = 3.0;		// Cuando un punto almacenado supera esta distancia del robot se elimina
	static protected double				DIST_ADD = 3.0;		// Distancia maxima del robot en la que se empieza a guardar un dato
	
	protected boolean 					debug = false;
	// Laser signal processing
	protected double					cone;			// Aperture of laser scan
	
	// LTG nodes connectivity (object boundaries)
	protected LPOLine				goal;			// Desired robot goal
	protected int[]					cindex;			// Index of colision points
	protected int					cnum;			// Number of object boundaries
	
	protected LinkedList			lastCol1;
	
	// Debugging tools
	LPORangePoint 					coldraw;	// guarda el punto que colisiona para dibujarlo
	LPORangePoint					ptcolis;
	Point2[][] 						robots	 = null;
	int 							buffindex = 0;
	World							world;
	boolean							onlyInZone = true;
	double							radius;

	// Constructor
	public LPORangePBug (int size, double cone, double range, double side, String label, int source)
	{			
		super (size, range, side, label, source);
		buffindex = 0;
		this.cone	= cone;	
		cindex	= new int[size];
		cnum		= 0;
		goal		= new LPOLine (0.0, 0.0, 0.0, "PBug_Goal", LPO.ARTIFACT);
		goal.color (WColor.MAGENTA.darker());
		lastCol1	= new LinkedList();
		color (WColor.ORANGE);
		world  = null;
	}
	
	// Constructor
	public LPORangePBug (int size, double cone, double range, double side, String label, int source, World world)
	{			
		super (size, range, side, label, source);
		buffindex = 0;
		this.cone	= cone;	
		cindex	= new int[size];
		cnum		= 0;
		goal		= new LPOLine (0.0, 0.0, 0.0, "PBug_Goal", LPO.ARTIFACT);
		goal.color (WColor.MAGENTA.darker());
		lastCol1	= new LinkedList();
		color (WColor.ORANGE);
		this.world = world;
	}
	
	// Accessors
	public int		boundaries ()				{ return cnum; }
	public void		setDebug(boolean debug)		{this.debug = debug;}
	public double	getSZone ()					{ return SZONE; }
	public void		setSZone (double szone)		{ this.SZONE = szone; }
	
	// Instance methods	
	public LPORangePoint collision (Point2 point)
	{
		if (point != null)
			return collision (point.x (), point.y ());
		
		return null;
	}
	
	
	/*
	 * POLARBUG. Funcionamiento:
	 * 
	 * - Se genera un circulo para cada medida del laser dentro del campo de actuacion (distancia lookahead), con radio
	 * 	en funcion de la distancia de seguridad.
	 * - Si la linea que une el robot al goal intersecta un circulo, hay una colision y se calcula la direccion para evitarlo.
	 * - Para evitarlo, se calculan todas los rayos que no intersectan con ningun circulo y se selecciona el mas adecuado: 
	 * 		el mas cercano al goal o el de menos giro
	 * - En el caso de no encontrar ninguno, calcula la linea que se queda mas alejado de los obstaculos.
	 * 		Si esta linea esta muy cerca, gira al maximo hay un lado (en funcion de los obstaculos de alrededor)
	 *
	 * 	- Se almacenan los valores del laser que estan a una distancia para poder evitar obstaculos que estan situados detras
	 * 	- Tambien se tiene en cuenta la posicion de otras carretillas  
	 */
	
	public LPORangePoint collision (double gx, double gy, Position pos)
	{
		LPORangePoint	pt = null;
		goal.active (true);
		double dist		= Math.min(Math.sqrt (gx * gx + gy * gy), 7.5);
		double robotdist = Math.max(dist, 4);
		double minang = Double.MAX_VALUE;
		double ang;
		cnum = 0;
		boolean colision = false;
		LPORangePoint col = null;
		double min;
		double dmin;
		double tmax = 0;
		double tmin = Double.MAX_VALUE;
		LPORangePoint cmax = null;
		double r;
		String szone = null;
		Line2[] zone = null;
		Position global = new Position();
		Point2 inters;
		boolean intersection = false;
		radius = 0;
		
		szone = world.zones().inZone(Transform2.toGlobal(gx,gy,0,pos.x(),pos.y(),pos.alpha()));
		if(szone!=null)
			zone = world.zones().at(szone).toLines();
		//System.out.println("zone = "+szone+" goal["+gx+","+gy+"] pos ="+pos+" transf="+Transform2.toGlobal(gx,gy,0,pos.x(),pos.y(),pos.alpha()));
		// Se considera siempre que el goal esta hacia delante. 
		// Si esta detras se considera que esta en los limites +/-90 grados
		
		//if(gx < 0) gx = 0;
		goal.locate (gx, gy);
		
		ptcolis = null;
		
		// Se actualiza el buffer, añadiendo nuevos puntos y eliminando antiguos
		updateBuffer();
		
		LPORangePoint[] oldbuffer = (LPORangePoint[]) lastCol1.toArray(new LPORangePoint[0]);

		r = calc_rad(gx,gy,dist);
		radius = r;
		
		if(debug) System.out.println("-------------- PolarBug (debug) -----------------");
		
		// Detecta una colision con los valores obtenidos del laser
		for(int i = 0; i<size; i++){
			if(onlyInZone && zone != null){	// Si un punto esta fuera de la zona actual, colisiona
				global.x(pos.x()+dist*Math.cos(buffer[i].phi() + pos.alpha()));
				global.y(pos.y()+dist*Math.sin(buffer[i].phi() + pos.alpha()));
				
				intersection = false;
				inters = null;
				// Calcula interseccion
				for(int j = 0; j<zone.length; j++){
					inters = zone[j].intersection(pos.x(),pos.y(),global.x(),global.y());
					if(inters!=null && inters.distance(pos.x(),pos.y()) < buffer[i].rho) break;
				}
				
				if(inters!=null && inters.distance(pos.x(),pos.y()) < buffer[i].rho){
					cindex[cnum++] = i;
					//colision = true;
					//System.out.println("Punto "+i+"["+global.x()+","+ global.y()+" ] colisiona con zona");
					buffer[i].locate_polar(inters.distance(pos.x(),pos.y()), buffer[i].phi());
					intersection = true;
					continue;
				}
			}
			if(buffer[i].len < (dist + 0.5) || intersection){
				cindex[cnum++] = i;
				if(!colision && mindistrad(buffer[i].x(),buffer[i].y(),r)<SZONE){ 
					//System.out.println("Colision ["+buffer[i].x()+","+buffer[i].y());
				    colision = true;
				    if(debug) System.out.println("Hay colision con valores del Laser o zona. Rayo_"+i+" ["+buffer[i].x()+","+buffer[i].y()+"]");
				}
			}
		}
			
		
		//	Detecta una colision con los valores almacenados
		if(colision!=true)
			for(int i = 0; i<oldbuffer.length; i++){
				if(oldbuffer[i].rho < dist){
					if(!colision && mindistrad(oldbuffer[i].x(),oldbuffer[i].y(),r)<SZONE){ 
					    colision = true;
					    if(debug) System.out.println("Colision valores antiguos ["+oldbuffer[i].x()+","+oldbuffer[i].y()+"]");
					    break;
					}
				}
			}
		
		//	Detecta una colision con otras carretillas
		if(colision != true && robots != null){
		    LPORangePoint goalpt = new LPORangePoint(0,0);
		    goalpt.locate(gx,gy);
		    for(int j = 0; j<robots.length;j++){
	    	    for(int n = 0; n<4; n++){
	    	        if(robots[j][n].distance(0,0)<robotdist){
	    	            dmin = mindistrad(robots[j][n].x(),robots[j][n].y(),r);
			    	    if(dmin<SZONE){
			    	    	if(debug) System.out.println("Colision otras carretillas");
			    	        colision = true;
			    	        break;
		    	        }

		    	    }
		    	}
		        
		    }
		}
		
		// No hay colision con el goal
		if(!colision){
	        coldraw = null;
	        return null;
		}
		
		// Se evaluan si los rayos intersectan los circulos	
		for(int i = 0; i<buffer_max; i++){
			r = calc_rad(buffer[i].x(),buffer[i].y(),dist);
			colision = false;
			tmin = Double.MAX_VALUE;	// distancia minima del rayo i (con todos los obstaculos (buffer y oldbuffer))
			min = Double.MAX_VALUE;
			// Calcula si el rayo i colisiona con los valores del Laser (con rho<dist)
			for(int j = 0; j<cnum;j++){
				if(cindex[j]!=i){
					dmin = mindistrad(buffer[cindex[j]].x(),buffer[cindex[j]].y(), r);
					tmin = Math.min(dmin,tmin);
					// Se comprueba que el rayo_i no cruza ningun circulo
					if(dmin<SZONE){	
						colision = true;
						//break;
					}
					else if(dmin<min){
					    min = dmin;
					    col = buffer[cindex[j]];	// Se guarda el punto que provoca la colision para añadirlo al buffer
					}
				}
				else{
					colision=true;
					tmin = Double.MAX_VALUE;
					min = Double.MAX_VALUE;
					break;
				}
			}
			
			// Calcula si algun rayo colisiona un valor almacenado
			if(colision == false)
				for(int j = 0; j<oldbuffer.length;j++){
				    //  Si el punto guardado esta situado al lado del robot.
				    if(Math.abs(oldbuffer[j].y())>2.0 || oldbuffer[j].x()>1.4 || oldbuffer[j].x()<0) continue;	
				    
				    dmin = mindistrad(oldbuffer[j].x(), oldbuffer[j].y(), r);
				    tmin = Math.min(dmin,tmin);
				    if(dmin<SZONE){	
						colision = true;
						//break;
					}
				}

			//	 Calcula si algun rayo colisiona algun robot
			if(colision == false && robots != null){
			    for(int j = 0; j<robots.length; j++)
			    {
  		            if(robots[j][0].distance(0,0)<robotdist || robots[j][1].distance(0,0)<robotdist || robots[j][2].distance(0,0)<robotdist || robots[j][3].distance(0,0)<robotdist)
			            for(int n = 0; n<4; n++)
			            {
			            	if(robots[j][n].x()<0) continue;
				            dmin = mindistrad(robots[j][n].x(),robots[j][n].y(),r);
				            if(dmin<SZONE)
				            {
				                colision = true;							
				                break;
				            }
				            
		    	    }		
			    }
			}
			
			//System.out.println("buffer_"+i+" tmin = "+tmin+" r = "+r+" SZone="+SZONE);
			
			if(tmin<SZONE && tmin>tmax){
			    tmax = tmin;
			    cmax = buffer[i];
			}
			

					
  			// Prioridad direccion mas cercana al goal (mas directo al goal, pero mas inseguro si para ir al goal tiene que girar demasiado)
  			// Si el rayo_i no toca nada se selecciona el que este mas cerca al goal (en funcion del angulo phi)
			if(colision == false && Math.abs(Angles.radnorm_180(goal.phi() - buffer[i].phi)) < minang){
				minang = Math.abs(Angles.radnorm_180(goal.phi() - buffer[i].phi));
				if(col!=null) ptcolis = col;
				pt = buffer[i];
				if(debug) System.out.println("MinAng_"+i+" pos["+buffer[i].x+","+buffer[i].y+"] mindist="+min+" rad="+r+" poscol ="+col);
				radius = r;
			}
			
			
/*			// Prioridad direccion con menos giro (menos optimo al no al punto mas cercano del goal, pero mas seguro al girar menos la direccion)
			// Si el rayo_i no toca nada se selecciona el que este mas cerca al goal (en funcion del angulo phi)
			if(colision == false && Math.abs(Angles.radnorm_180(0 - buffer[i].phi)) < minang){
				minang = Math.abs(Angles.radnorm_180(0 - buffer[i].phi));
				if(col!=null) ptcolis = col;
				pt = buffer[i];
			}*/
		}
		
		// Si no hay ningun valor valido intenta utilizar
		// una zona de seguridad mas pequeña
		if(pt==null && cmax !=null){
			double maxr = 0;
			for(int i = -5; i<=5; i++){
				if(i>0)
					r = ((dist / 2)-0.5)*(double)i/5 + 0.5;
				else
					r = ((dist / 2)-0.5)*(double)i/5 - 0.5;
				colision = false;
				tmin = Double.MAX_VALUE;	// distancia minima del rayo i (con todos los obstaculos (buffer y oldbuffer))
				min = Double.MAX_VALUE;
				for(int j = 0; j<cnum;j++){
					dmin = mindistrad(buffer[cindex[j]].x(),buffer[cindex[j]].y(), r);
					tmin = Math.min(dmin,tmin);
					// Se comprueba que el rayo_i no cruza ningun circulo
					if(dmin<SZONE){	
						colision = true;
						//break;
					}
					else if(dmin<min){
					    min = dmin;
					    col = buffer[cindex[j]];	// Se guarda el punto que provoca la colision para añadirlo al buffer
					}
				}
			
				if(colision == false)
					for(int j = 0; j<oldbuffer.length;j++){
					    //  Si el punto guardado esta situado al lado del robot.
					    if(Math.abs(oldbuffer[j].y())>2.0 || oldbuffer[j].x()>1.4 || oldbuffer[j].x()<0) continue;	
					    
					    dmin = mindistrad(oldbuffer[j].x(), oldbuffer[j].y(), r);
					    tmin = Math.min(dmin,tmin);
					    if(dmin<SZONE){	
							colision = true;
							//break;
						}
					}
				//			 Calcula si algun rayo colisiona algun robot
				if(colision == false && robots != null){
				    for(int j = 0; j<robots.length; j++)
				    {
	  		            if(robots[j][0].distance(0,0)<robotdist || robots[j][1].distance(0,0)<robotdist || robots[j][2].distance(0,0)<robotdist || robots[j][3].distance(0,0)<robotdist)
				            for(int n = 0; n<4; n++)
				            {
				            	if(robots[j][n].x()<0) continue;
					            dmin = mindistrad(robots[j][n].x(),robots[j][n].y(),r);
					            if(dmin<SZONE)
					            {
					                colision = true;							
					                break;
					            }
					            
			    	    }		
				    }
				}
				
				if(colision == false && Math.abs(r) > maxr){
					maxr = Math.abs(r);
					if(col!=null) ptcolis = col;
					pt = new LPORangePoint(0,0);
					pt.locate(0, Math.min(r-0.5,0.5)*2);
					if(debug) System.out.println("Radio de giro "+r+" pt["+pt.x()+","+pt.y()+"] ");
					radius = r;
				}
			
			}
			
			if(pt==null && cmax !=null){
				if(tmax>0.5 && tmax<SZONE){
				    pt = cmax;
				    if(debug) System.out.println("Haciendo tangente. tmax = "+tmax+" cmax["+cmax.x+","+cmax.y+"]");
				}
			}
		}
		
		// Si no se encuentra ningun rayo valido, gira a uno de los lados
		if(pt==null){
			// Se gira en la direccion donde hay mas espacio para girar
	    
		    pt = new LPORangePoint(0,0);
			double left = Double.MAX_VALUE;  // Distancia minima para girar a la izq
			double rigth = Double.MAX_VALUE; // Distancia minima para girar a la der
			int maxray = (int)Math.round((size / 4));
			for(int i = 0; i<maxray; i++){
				rigth = Math.min(buffer[i].rho, rigth);
				left  = Math.min(buffer[size-1-i].rho, left);
			}
			if(rigth>left){
				if(debug) System.out.println("giro a derecha. rigth="+rigth+" left="+left);
				pt = buffer[0]; 		// giro a derecha
				//dist = rigth;
			}
			else{
				if(debug) System.out.println("giro a izquierda. rigth="+rigth+" left="+left);
				pt = buffer[size-1];	// giro a izquierda
				//dist = left;
			}
			dist = 1;	// giro maximo
		}
		
		if(pt!=null){
			if(pt.rho() > dist){
				ang = pt.phi;
				pt = new LPORangePoint(0,0);
				pt.locate_polar(dist, ang);
				//addBuffer(ptcolis);
			}
		}	
		
		
		coldraw = pt;
		return pt;
	}
	
	public LPORangePoint collision (double gx, double gy)
	{
		LPORangePoint	pt = null;
		goal.active (true);
		double dist		= Math.min(Math.sqrt (gx * gx + gy * gy), 7.5);
		double robotdist = Math.max(dist, 4);
		double minang = Double.MAX_VALUE;
		double ang;
		cnum = 0;
		boolean colision = false;
		LPORangePoint col = null;
		double min;
		double dmin;
		double tmax = 0;
		double tmin = Double.MAX_VALUE;
		LPORangePoint cmax = null;
		double r;
		String szone = null;
		WMZone zone = null;
		
		szone = world.zones().inZone(gx,gy);
		if(szone!=null)
			zone = world.zones().at(szone);
		
		// Se considera siempre que el goal esta hacia delante. 
		// Si esta detras se considera que esta en los limites +/-90 grados
		
		//if(gx < 0) gx = 0;
		goal.locate (gx, gy);
		
		ptcolis = null;
		
		// Se actualiza el buffer, añadiendo nuevos puntos y eliminando antiguos
		updateBuffer();
		
		LPORangePoint[] oldbuffer = (LPORangePoint[]) lastCol1.toArray(new LPORangePoint[0]);

		r = calc_rad(gx,gy,dist);
		// Detecta una colision con los valores obtenidos del laser
		for(int i = 0; i<size; i++){
			if(onlyInZone && zone != null){	// Si un punto esta fuera de la zona actual, colisiona
				if(!zone.area.contains(buffer[i].x, buffer[i].y)){
					cindex[cnum++] = i;
					colision = true;
					continue;
				}
			}
			if(buffer[i].len < (dist + 0.5)){
				cindex[cnum++] = i;
				if(!colision && mindistrad(buffer[i].x(),buffer[i].y(),r)<SZONE){ 
				    colision = true;
				}
			}
		}
		
		//	Detecta una colision con los valores almacenados
		if(colision!=true)
			for(int i = 0; i<oldbuffer.length; i++){
				if(oldbuffer[i].rho < dist){
					if(!colision && mindistrad(oldbuffer[i].x(),oldbuffer[i].y(),r)<SZONE){ 
					    colision = true;
					    break;
					}
				}
			}
		
		//	Detecta una colision con otras carretillas
		if(colision != true && robots != null){
		    LPORangePoint goalpt = new LPORangePoint(0,0);
		    goalpt.locate(gx,gy);
		    for(int j = 0; j<robots.length;j++){
	    	    for(int n = 0; n<4; n++){
	    	        if(robots[j][n].distance(0,0)<robotdist){
	    	            dmin = mindistrad(robots[j][n].x(),robots[j][n].y(),r);
			    	    if(dmin<SZONE){
			    	        colision = true;
			    	        break;
		    	        }

		    	    }
		    	}
		        
		    }
		}
		
		// No hay colision con el goal
		if(!colision){
	        coldraw = null;
	        return null;
		}
		
		// Se evaluan si los rayos intersectan los circulos	
		for(int i = 0; i<buffer_max; i++){
			r = calc_rad(buffer[i].x(),buffer[i].y(),dist);
			colision = false;
			tmin = Double.MAX_VALUE;	// distancia minima del rayo i (con todos los obstaculos (buffer y oldbuffer))
			min = Double.MAX_VALUE;
			// Calcula si el rayo i colisiona con los valores del Laser (con rho<dist)
			for(int j = 0; j<cnum;j++){
				if(cindex[j]!=i){
					dmin = mindistrad(buffer[cindex[j]].x(),buffer[cindex[j]].y(), r);
					tmin = Math.min(dmin,tmin);
					// Se comprueba que el rayo_i no cruza ningun circulo
					if(dmin<SZONE){	
						colision = true;
						//break;
					}
					else if(dmin<min){
					    min = dmin;
					    col = buffer[cindex[j]];	// Se guarda el punto que provoca la colision para añadirlo al buffer
					}
				}
				else{
					colision=true;
					tmin = Double.MAX_VALUE;
					min = Double.MAX_VALUE;
					break;
				}
			}
			
			// Calcula si algun rayo colisiona un valor almacenado
			if(colision == false)
				for(int j = 0; j<oldbuffer.length;j++){
				    //  Si el punto guardado esta situado al lado del robot.
				    if(Math.abs(oldbuffer[j].y())>2.0 || oldbuffer[j].x()>1.4 || oldbuffer[j].x()<0) continue;	
				    
				    dmin = mindistrad(oldbuffer[j].x(), oldbuffer[j].y(), r);
				    tmin = Math.min(dmin,tmin);
				    if(dmin<SZONE){	
						colision = true;
						//break;
					}
				}

			//	 Calcula si algun rayo colisiona algun robot
			if(colision == false && robots != null){
			    for(int j = 0; j<robots.length; j++)
			    {
  		            if(robots[j][0].distance(0,0)<robotdist || robots[j][1].distance(0,0)<robotdist || robots[j][2].distance(0,0)<robotdist || robots[j][3].distance(0,0)<robotdist)
			            for(int n = 0; n<4; n++)
			            {
			            	if(robots[j][n].x()<0) continue;
				            dmin = mindistrad(robots[j][n].x(),robots[j][n].y(),r);
				            if(dmin<SZONE)
				            {
				                colision = true;							
				                break;
				            }
				            
		    	    }		
			    }
			}
			
			//System.out.println("buffer_"+i+" tmin = "+tmin+" r = "+r+" SZone="+SZONE);
			
			if(tmin<SZONE && tmin>tmax){
			    tmax = tmin;
			    cmax = buffer[i];
			}
			

					
  			// Prioridad direccion mas cercana al goal (mas directo al goal, pero mas inseguro si para ir al goal tiene que girar demasiado)
  			// Si el rayo_i no toca nada se selecciona el que este mas cerca al goal (en funcion del angulo phi)
			if(colision == false && Math.abs(Angles.radnorm_180(goal.phi() - buffer[i].phi)) < minang){
				minang = Math.abs(Angles.radnorm_180(goal.phi() - buffer[i].phi));
				if(col!=null) ptcolis = col;
				pt = buffer[i];
				if(debug) System.out.println("MinAng_"+i+" pos["+buffer[i].x+","+buffer[i].y+"] mindist="+min+" rad="+r+" poscol["+col.x+","+col.y+"]");
			}
			
			
/*			// Prioridad direccion con menos giro (menos optimo al no al punto mas cercano del goal, pero mas seguro al girar menos la direccion)
			// Si el rayo_i no toca nada se selecciona el que este mas cerca al goal (en funcion del angulo phi)
			if(colision == false && Math.abs(Angles.radnorm_180(0 - buffer[i].phi)) < minang){
				minang = Math.abs(Angles.radnorm_180(0 - buffer[i].phi));
				if(col!=null) ptcolis = col;
				pt = buffer[i];
			}*/
		}
		
		// Si no hay ningun valor valido intenta utilizar
		// una zona de seguridad mas pequeña
		if(pt==null && cmax !=null){
			if(tmax>0.5 && tmax<SZONE){
			    pt = cmax;
			    //System.out.println("Haciendo tangente. tmax = "+tmax+" cmax["+cmax.x+","+cmax.y+"]");
			}
		}
		
		// Si no se encuentra ningun rayo valido, gira a uno de los lados
		if(pt==null){
			// Se gira en la direccion donde hay mas espacio para girar
	    
		    pt = new LPORangePoint(0,0);
			double left = Double.MAX_VALUE;  // Distancia minima para girar a la izq
			double rigth = Double.MAX_VALUE; // Distancia minima para girar a la der
			int maxray = (int)Math.round((size / 4));
			for(int i = 0; i<maxray; i++){
				rigth = Math.min(buffer[i].len, rigth);
				left  = Math.min(buffer[size-1-i].len, left);
			}
			if(rigth>left){
				pt = buffer[0]; 		// giro a derecha
				//dist = rigth;
			}
			else{
				pt = buffer[size-1];	// giro a izquierda
				//dist = left;
			}
			dist = 1;	// giro maximo
		}
		
		if(pt!=null){
			ang = pt.phi;
			pt = new LPORangePoint(0,0);
			pt.locate_polar(dist, ang);
			//addBuffer(ptcolis);
		}	
		
		
		coldraw = pt;
		return pt;
	}
	
	
	// Añade un punto al buffer (si no supera la distancia maxima o esta cerca de otro almacenado)
	private boolean addBuffer(LPORangePoint data){
	    if(data == null || data.rho > DIST_ADD || !data.active) return false;
	    for(int i = 0; i<lastCol1.size();i++){
	        if( ((LPORangePoint)lastCol1.get(i)).distance(data) < PTDIST) return false;
	    }
	    LPORangePoint clone = new LPORangePoint(0,0);
	    clone.set(data.x,data.y,data.len,data.sensor,data.source);
	    clone.active(true);
	    lastCol1.add(clone);
	    //System.out.println("añadido p["+clone.x+", "+clone.y+"] total="+lastCol1.size());
	    return true;
	}
	
	
	// Actualiza el buffer, eliminando los puntos guardalos que se han alejado, 
	// eliminando los puntos almacenados que no concuerdan con las medidas del laser (objetos moviles)
	// y añadiendo nuevos puntos dentro del rango
	private void updateBuffer(){
		double dist;
		LPORangePoint pt;
		
		for(int i = 0; i<lastCol1.size();i++){
			pt = ((LPORangePoint)lastCol1.get(i));
			
			//	Borrando puntos lejanos
			if( pt.rho > DIST_DEL){
				//System.out.println("borrado: p["+pt.x+", "+pt.y+"] rho = "+ pt.rho +" total="+lastCol1.size());
	            lastCol1.remove(i);
	            i--;
	            continue;
	        }
			
			// Borrando puntos almacenados que no coinciden con las medidas del laser
			if(pt.x() < 1.314) continue; 	// Solo se eliminan puntos delante del laser (lrflen0 = 1.314)
			dist = Double.MAX_VALUE;
			for(int m = 0; m < buffer.length; m++){
				dist = pt.distance(buffer[m]);
				if(dist < PTDIST)
					break;
			}
			if(dist>PTDIST){
				lastCol1.remove(i);
				//System.out.println("borrado dato invalido:  p["+pt.x+", "+pt.y+"] total="+lastCol1.size()+" dist = "+dist);
	            i--;
			}
			
		}
				
		// Añadiendo puntos cercanos
		for(int m = 0; m<buffer.length; m++){
			 addBuffer(buffer[m]);
		}
	}
	
	// Calcula radio de curvatura para llegar al punto con rango dist y angulo (rx,ry)
	private double calc_rad(double rx, double ry, double dist) {
		if(ry == 0) return Double.POSITIVE_INFINITY;
		double ang = Math.atan2(ry,rx);
		return (dist/2)/Math.sin(ang);
	}

	// Minima distancia de un punto cx,cy con el radio de curvatura r
	private double mindistrad(double cx, double cy, double r) {
		if(Double.isInfinite(r)) return cy;
		return Math.abs(Math.sqrt((cy-r)*(cy-r)+(cx*cx))-Math.abs(r));
	}
	
/*	// Calcula radio de curvatura para llegar al punto con rango dist y angulo (rx,ry)
	private double calc_rad(double rx, double ry, double dist) {
		if(rx == 0) return Double.POSITIVE_INFINITY;
		double ang = Math.atan2(ry,rx);
		return (dist/2)/Math.cos(ang);
	}

	private double mindistrad(double cx, double cy, double r) {
		if(Double.isInfinite(r)) return cx;
		return Math.abs(Math.sqrt((cx-r)*(cx-r)+(cy*cy))-Math.abs(r));
	}*/
	
	
	// minima distancia de un rayo (line) a un punto (point)
	private double mindist(LPORangePoint line, LPORangePoint point) {
		double ang = Angles.radnorm_180(line.phi-point.phi);
		if(Math.abs(ang)>Math.PI/2) return line.rho(); 
		return Math.abs(line.rho() * Math.sin(ang));
	}

	//	 minima distancia del punto que colisiona (line) a un punto (px,py)
	private double mindist(LPORangePoint line, double px, double py) {
		double ang = Angles.radnorm_180(Math.abs(line.phi-Math.atan2(py,px)));
		if(ang>Math.PI/2) return line.rho(); 
		return Math.abs(line.rho() * Math.sin(ang));
	}
	
	//	 Minima distancia de un punto (cx,cy) al rayo que forma desde el origen al punto (rx,ry)
	private double mindist(double cx, double cy, double rx, double ry) {
		double ang = Angles.radnorm_180(Math.abs(Math.atan2(cy,cx)-Math.atan2(ry,rx)));
		double rho = Math.sqrt(cx*cx+cy*cy);
		if(ang>Math.PI/2) return rho; 
		return Math.abs(rho * Math.sin(ang));
	}
	
	//	 Minima distancia de una linea al rayo que forma des de el origen al punto (cx,cy)
	private double mindist(Line2 line, double rx, double ry) {
	    double phi = Math.atan2(ry,rx);
	    double ang1 = phi-Math.atan2(line.orig().y(),line.orig().x());
		double ang2 = phi-Math.atan2(line.dest().y(),line.dest().x());
		if( Math.max(Math.abs(Angles.radnorm_180(phi-ang1)) ,Math.abs(Angles.radnorm_180(phi-ang2)) ) < Math.abs(Angles.radnorm_180(ang2-ang1)) )
		    return 0; // Intersecta

		return Math.min(
		        mindist(line.orig().x(),line.orig().y(),rx, ry),
		        mindist(line.dest().x(),line.dest().y(), rx, ry)
		);
	}
	
	
	public void draw (Model2D model, LPOView view)
	{	
		int			i;
		int			ci;
		double		x1, y1, a1;
		
		if (!active)		return;

		//super.draw (model, view);		
		goal.draw (model, view);
		
		// Draw connecting segments
		for (i = 0; i < cnum; i++)		
		{
			ci	= cindex[i];
			
			a1	= view.rotation + buffer[ci].phi;
			x1 	= buffer[ci].rho * Math.cos (a1);
			y1 	= buffer[ci].rho * Math.sin (a1);

			// Pinta algunos circulos de las esquinas de los obstaculos 
			if(i == 0 || i >= (cnum-1) || (cindex[i]-cindex[i-1]>1) || (cindex[i+1]-cindex[i]>1))
			    model.addRawCircle(x1,y1,SZONE,ColorTool.fromWColorToColor(WColor.RED));
			else
				model.addRawPoint(x1,y1,0,ColorTool.fromWColorToColor(WColor.RED));
		}
		// Pinta el punto donde debe dirigirse para evitar la colision
		if(coldraw!=null){
			a1	= view.rotation + coldraw.phi;
			x1 	= coldraw.rho * Math.cos (a1);
			y1 	= coldraw.rho * Math.sin (a1);
			model.addRawLine(0,0,x1,y1,Model2D.THICK, ColorTool.fromWColorToColor(WColor.BLACK));
		}
		//model.addRawCircle(0.8,0,SZONE,Color.RED);
		// Pinta el circulo tangente
		if(ptcolis!=null){
		    a1	= view.rotation + ptcolis.phi;
			x1 	= ptcolis.rho * Math.cos (a1);
			y1 	= ptcolis.rho * Math.sin (a1);
		    model.addRawCircle(x1,y1,SZONE,ColorTool.fromWColorToColor(WColor.BLACK));
		}
		
		if(radius!=0 && Math.abs(radius)<10){
		    a1	= view.rotation + Math.PI/2;
			x1 	= radius * Math.cos (a1);
			y1 	= radius * Math.sin (a1);
		    model.addRawCircle(x1,y1,radius,ColorTool.fromWColorToColor(WColor.BLACK));
		}
		
		//if(lastCol!=null)
		for(i = 0; i<lastCol1.size(); i++){
		    LPORangePoint p = (LPORangePoint) lastCol1.get(i);
		    a1	= view.rotation + p.phi();
			x1 	= p.rho() * Math.cos (a1);
			y1 	= p.rho() * Math.sin (a1);
			model.addRawCircle(x1,y1,SZONE, ColorTool.fromWColorToColor(WColor.GREEN.darker()));
		}
			
		if(robots != null)
		    for(int j = 0; j<robots.length; j++){
		    	    model.addRawCircle(Transform2.rot(robots[j][0],view.rotation),SZONE,ColorTool.fromWColorToColor(WColor.RED));
		    	    model.addRawCircle(Transform2.rot(robots[j][1],view.rotation),SZONE,ColorTool.fromWColorToColor(WColor.RED));
		    	    model.addRawCircle(Transform2.rot(robots[j][2],view.rotation),SZONE,ColorTool.fromWColorToColor(WColor.RED));
		    	    model.addRawCircle(Transform2.rot(robots[j][3],view.rotation),SZONE,ColorTool.fromWColorToColor(WColor.RED));
		    }
	}


    public void setRobots(LPOMate[] l_mates) {
        int cont = 0;
        if(l_mates != null){
            for(int i = 0; i < l_mates.length; i++){
            	if(l_mates[i] != null && l_mates[i].label != null) cont++;
            }
            robots = new Point2[cont][4];
            cont = 0;
            for(int j = 0; j < l_mates.length; j++){
            	if(l_mates[j] != null && l_mates[j].label != null){
			        robots[cont][0] = Transform2.rotTrans(new Point2(l_mates[j].xmin, l_mates[j].ymin), l_mates[j].x, l_mates[j].y, l_mates[j].alpha);
			        robots[cont][1] = Transform2.rotTrans(new Point2(l_mates[j].xmin, l_mates[j].ymax), l_mates[j].x, l_mates[j].y, l_mates[j].alpha);
			        robots[cont][2] = Transform2.rotTrans(new Point2(l_mates[j].xmax, l_mates[j].ymin), l_mates[j].x, l_mates[j].y, l_mates[j].alpha);
			        robots[cont][3] = Transform2.rotTrans(new Point2(l_mates[j].xmax, l_mates[j].ymax), l_mates[j].x, l_mates[j].y, l_mates[j].alpha);
			        cont++;
            	}
            }
        }
        else{
            robots = null;
        }

    }
    
	public void clamp (Matrix3D rm)
	{
		super.clamp(rm);
		LPORangePoint data;
		for (int i = 0; i < lastCol1.size(); i++){
		    data = ((LPORangePoint)lastCol1.get(i));
		    data.clamp(rm);
		}
	}
    
}

