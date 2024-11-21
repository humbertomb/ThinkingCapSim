/*
 * (c) 2001 Humberto Martinez
 *			Miguel Zamora
 *			Jose Ant. Marin
 *
 *		Kalman-based localisation with beacons (with LSB latency)
 */

package tcrob.ingenia.ifork;

import tclib.navigation.localisation.*;
import tc.vrobot.models.*;

import java.io.*;

import devices.pos.*;
import wucore.utils.math.jama.*;
import wucore.utils.math.*;

public class IForkKLoc extends Object
{	
	// Initial variances
	static public final boolean			REINIT		= false;
	public static double				CHI_TEST	= 10;		// Matching value for the Chi-square test

	public static final double			SD_VM		= 1;	// Standard Desviation % (displacement of front wheel)
	public static final double			SD_DELTA	= 0.5;	// Standard Desviation of delta in degree (angle of front wheel)

	public double						SD_POSY		= 0.1;		// Standard Desviation of position initial
	public double						SD_POSX		= 0.1;		// Standard Desviation of position initial
	public double						SD_ANG		= 0.1;		// Standard Desviation of orientation initial
	
	protected double					POSX_INIT	= 0.0;		// Position X initial
	protected double					POSY_INIT	= 0.0;		// Posicion Y initial
	protected double					ANG_INIT	= 0.0;		// Orientation initial
	
	public static double				DTOR		= Math.PI/180.0;
	public static double				RTOD		= 180.0/Math.PI;
	
		
	// Global matrices
	protected Matrix					P;							// State prediction covariance
	protected Matrix					X;							// Estimated state
	
	// Position prediction variables
	protected Position					posk;						// Current position, time k
	protected Position					pest;						// Position at time k-1
	protected long						tk;							// Time at k
	protected long						tk1;						// Time at k-1	

	protected double					velAng;
	protected double					velLin;
	protected double					maxvelAng;
	protected double					maxvelLin;
	
	// Position prediction matrices
	protected Matrix					Fx;							// Jacobian with respect to the state of the previous state 
	protected Matrix					Fu;							// Jacobian with respect to the input of the previous state 
	protected Matrix					Qx;							// Variance of the input projected into the state space 
	protected Matrix					Qmodel;	
	protected Matrix					Qu;							// Variance of the input 
	
	// Measure matching
	protected Matrix					Aux;						// Matrix auxiliary
	protected Matrix					du;							// Jacobian of u with respect to u_tr 
	protected Matrix					hx;							// Jacobian with respect to the state of the measure prediction 
	protected Matrix					s;							// Innovation covariance 
	protected Matrix					r;							// Variance of the measure 
	protected KMatch					match;						// Store for matched segments
	protected double					NIS;						// Normalized Innovation Squared
	
	// Position updating matrices
	protected Matrix					Hx;							// Jacobian with respect to the state of the measure prediction (only matched measures)
	protected Matrix					S;							// Innovance covariance (only matched measures)
	protected Matrix					R;							// Variance of the measure  (only matched measures)
	protected Matrix					W;							// Kalman gain
	protected Matrix					V;							// Innovation
	protected Matrix					Pe;							// State prediction covariance (filtered)
	protected Matrix					Xe;							// Estimated state (filtered)
	
	
	// Other local stuff

	protected TricycleDrive				model;
	protected boolean					debug;						// Standard console debug
	protected boolean					initialized;
	protected boolean					firstime;


	protected int 						latency;					//  Ciclos de la latencia
	protected int						latencyTime;				//  Tiempo de la latencia (ms)
	protected Matrix[] 					recX;						//  Registro de los estado del sistema
	protected Matrix[] 					recP;						//  Registro de las Matrizes de Covarianza del sistema
	protected double[] 					recDt;						//	Registro del dt
	protected double[] 					recSt;					
	protected double[]					recVr;
	protected double[]					recWr;
	
	protected int						index;
	protected boolean 					isLatency;					//  Indica si esta recalculando el estado debido a la latencia
	protected double 					lastVr;
	protected double 					lastWr;
		
	protected							PrintWriter file;		  
	protected boolean					debugLog;
	protected boolean					no_filter;
	protected	long					st;

	
	// Constructors
	
	public IForkKLoc (TricycleDrive model)
	{
		// Set up global parameters
		this.model	= model;
		match		= new KMatch ();
		if(model.l < 0.1){
			System.out.println("IForkKLoc: Parametros del Modelo Cinematico incorrectos:");
			System.out.println("IForkKLoc:     Longitud del vehiculo (L) igual a 0");
			System.out.println("IForkKLoc:     Corregirlo, usando por defecto L = 1.003m");
			model.l = 1.003;
		}
		// Create prediction data structures
		posk			= new Position ();
		
		X			= new Matrix (3, 1);
		P			= new Matrix (3, 3);

		Fx			= new Matrix (3, 3);
		Fu			= new Matrix (3, 2);
		Qx			= new Matrix (3, 3);
		Qmodel		= new Matrix (3, 3);
		Qu			= new Matrix (2, 2);
		NIS			= 0;
		
		// Create matching data structures
		Aux			= new Matrix (2, 2);
		du			= new Matrix (2 ,2);
		hx			= new Matrix (2, 3);
		r			= new Matrix (2, 2);
		
		no_filter = false;

		// Save debug data
		debugLog = false;
		if(debugLog == true)
		{
			st = System.currentTimeMillis();
			try
			{
				file 	= new PrintWriter(new BufferedWriter(new FileWriter("filter.log")));
			}
			catch(IOException e){
				System.out.println("Error create archive ");
				e.printStackTrace ();
				System.exit(0);
			}
			catch(Exception e){
				System.out.println("Error create archive ");			
				e.printStackTrace ();
				System.exit(0);			
			}

		}


		latencyTime = 0;
		latency		= 0;	// No latency
		index		= 0;
		recX 		= new Matrix[50];
		recP 		= new Matrix[50];
						
		for(int i=0; i<50;i++){
			recX[i]		= new Matrix(3,1);
			recP[i]		= new Matrix(3,3);
		}
		
		recVr		= new double[50];
		recWr		= new double[50];		
		recDt		= new double[50];
		recSt		= new double[50];
		
		isLatency 	= false;
		debug		= false;	
		initialized	= false;
		firstime	= true;
	

		X.set(0,0,POSX_INIT);
		X.set(1,0,POSY_INIT); 
		X.set(2,0,ANG_INIT);
		
		P.identity();
		P.set(0,0,Math.pow(SD_POSX,2));
		P.set(1,1,Math.pow(SD_POSY,2));
		P.set(2,2,Math.pow(SD_ANG*Angles.DTOR,2));
	
		posk.set (X.get(0,0), X.get(1,0), X.get(2,0));	
	}
	
	// Accessors
	public Position					current ()				{ return posk; }
	public int						matched ()				{ return match.n; }
	public double					NIS()					{ return NIS;	}
	public String					printState()			{ return("X = ["+X.get(0,0)+", "+X.get(1,0)+", "+X.get(2,0)+"]");}
	public final void				debug (boolean debug)	{ this.debug = debug; }
	
	
	// Cambia la posicion del filtro
	public void						posInit(Position pos)	{ 
	POSX_INIT=pos.x();
	POSY_INIT=pos.y();
	ANG_INIT=pos.alpha();
	X.set (0, 0, POSX_INIT);
	X.set (1, 0, POSY_INIT);
	X.set (2, 0, ANG_INIT);
	//initialized = true;
	}
	
	// 	Realiza la prediccion utilizando la posicion de la odometria.
	//
	//	posOdom es la posicion estimada por la odometria
	//

	public void prediction(Position posOdom){
		double		dt;
		
		// Update time
		tk1	= tk;
		tk	= System.currentTimeMillis();
		dt	= (double) (tk - tk1) / 1000.0;
		
		prediction(posOdom, dt);	
	}
	
	
	
	public void prediction(Position posOdom, double dt){
	
		double w,v;
		
			// Calculo de [v,w] a partir de las ultimas posiciones del vehiculo (cinematica inversa)
		/*
		w	= Angles.radnorm_180(posOdom.alpha() - posk.alpha())/dt;
		if ( Math.abs(Math.sin(posk.alpha ()+dt*w/2)) > 0.5)
			v	= (posOdom.y () - posk.y ()) / (dt * Math.sin (posk.alpha ()+dt*w/2));
		else
			v	= (posOdom.x () - posk.x ()) / (dt * Math.cos (posk.alpha ()+dt*w/2));		
		*/
		
		w	= Angles.radnorm_180(posOdom.alpha() - posk.alpha())/dt;
		if ( Math.abs(Math.sin(posOdom.alpha())) > 0.5)
			v	= (posOdom.y () - posk.y ()) / (dt * Math.sin (posOdom.alpha()));
		else
			v	= (posOdom.x () - posk.x ()) / (dt * Math.cos (posOdom.alpha()));	

		if(debugLog == true){
			try{
				if(System.in.available()>0){
					file.close();
					System.out.println("Archivos del debug del Filtro cerrados");
					debugLog = false;
				}
				else
					file.print("\n"+(System.currentTimeMillis()-st)+"\t"+posOdom.x()+"\t"+posOdom.y()+"\t"+posOdom.alpha()*Angles.RTOD+"\t");
			}catch(Exception e){}
		}
	
		prediction(v,w,dt);
	}
	
	
	
	public void prediction(double vm, double delta){	
		double dt;
		double		x,y,fi;	// positions and angle auxiliary
		double 		w,v;
		if (firstime==true){ 						
			firstime 	= false;
			return;
		}

		// Update time
		tk1	= tk;
		tk	= System.currentTimeMillis();
		dt	= (double) (tk - tk1) / 1000.0;
		if(dt==0) dt = 0.001;
		//dt = 0.2;
		// Compute direct kynematics
		w		= Math.sin (delta) * vm / (model.l);
		v		= vm * Math.cos (delta);			

		velAng 	= Math.abs(w/dt);
		velLin  = Math.abs(v/dt);
		if(velAng>maxvelAng) maxvelAng =velAng;
		if(velLin>maxvelLin) maxvelLin =velLin;
		
					
		// Variables auxiliares del estado
		x=X.get(0,0);
		y=X.get(1,0);
		fi=X.get(2,0);

		if (debug)
		{
			System.out.println ("-------------------------- KBeac ----------------------------");
			System.out.println ("Position (k-1) = ["+x+", "+y+", "+fi*Angles.RTOD+"]");
		}
			
		try
		{
			// Fx = df(x,u)/dx												[3x3]
			Fx.identity ();
			Fx.set (0, 2, - Math.sin (fi + w/2) * v);
			Fx.set (1, 2, Math.cos (fi + w/2) * v);
			
			// Qu				
			// du= du/du_tr   [2x2]
			du.set (0, 0, Math.cos(delta));
			du.set (0, 1, -vm*Math.sin(delta));
			du.set (1, 0, Math.sin(delta)/model.l);
			du.set (1, 1, vm*Math.cos(delta)/model.l);	
			
			// Incertidumbre en la medida vm,delta
			Aux.identity();
			Aux.set(0, 0, Math.pow( Math.abs(vm * SD_VM/100), 2.0 ));
			Aux.set(1, 1, Math.pow( Math.toRadians(SD_DELTA) , 2.0 ));

			Qu= du.times(Aux).times(du.transpose());
				
			// Fu = df(x,u)/du												[3x2]
			Fu.set (0, 0, Math.cos (fi + w/2));
			Fu.set (0, 1, -Math.sin (fi + w/2) * v / 2);
			Fu.set (1, 0, Math.sin (fi + w/2));
			Fu.set (1, 1, Math.cos (fi +w/2) * v / 2);
			Fu.set (2, 0, 0.0);
			Fu.set (2, 1, 1);
			
			Qmodel.times(0);
			Qmodel.identity();
			Qmodel.set(0,0,Math.pow(0.003,2));
			Qmodel.set(1,1,Math.pow(0.003,2));
			Qmodel.set(2,2,Math.toRadians(Math.pow(0.03,2)));
			
			// Qx=FuQuFu'+ Error Modelo										[3x3]
			Qx = (Fu.times(Qu).times(Fu.transpose())).plus(Qmodel);
			
			
			
			// P=FxPFx'+Qx									[3x3]
			P	= (Fx.times (P).times (Fx.transpose ())).plus (Qx);

															
			// f(x,u) 										[3x1]
			X.set (0, 0, x + Math.cos(fi + w / 2) * v );
			X.set (1, 0, y + Math.sin(fi + w / 2) * v);
			X.set (2, 0, Angles.radnorm_180(fi + w));
				
		} catch (Exception e) { e.printStackTrace (); }
		
		posk.set (X.get(0,0), X.get(1,0), X.get(2,0));
		
		// Se memorizan las variables para recalcular el estado cuando hay latencia
		if((latencyTime>0) && (isLatency==false)){
			recP[index].setMatrix(P);
			recX[index].setMatrix(X);
			recVr[index] = v;
			recWr[index] = w;
			recSt[index] =System.currentTimeMillis();
			recDt[index++]=1;
			if(index==recP.length) index = recP.length-1;
		}
		//System.out.println ("Pred ["+posk.x()+", "+posk.y()+", "+Math.toDegrees(posk.alpha())+"]");
		//System.out.println("Err["+Math.sqrt(P.get(0,0))+","+Math.sqrt(P.get(1,1))+","+Math.toDegrees(Math.sqrt(P.get(2,2)))+"]");
		
		if (debug)
		{
			System.out.println ("Position (k) = ["+posk.x()+", "+posk.y()+", "+posk.alpha()*Angles.RTOD+"]");
			System.out.println ("Matrix P (prediction)");
			P.print (12, 5);
		}
	
	}
	
	public void prediction(double v, double w, double dt){	

		double		vm, delta;
		double		x,y,fi;	// positions and angle auxiliary
		
		if (firstime==true){ 						
			velAng 		= 	0;	
			firstime 	= false;
			return;
		}

		// cinematica inversa
		model.kynematics_inverse(v,w);
		delta 	= model.del;
		vm 		= model.vm;
		velAng 	= w;
		velLin  = v;	
					
		if(dt < 0.001){
			System.out.println("Periodo de tiempo demasiado peque�o: dt = "+dt);
			return;
		}
					
			// Variables auxiliares del estado
		x=X.get(0,0);
		y=X.get(1,0);
		fi=X.get(2,0);

		if (debug)
		{
			System.out.println ("-------------------------- KBeac ----------------------------");
			System.out.println ("Position (k-1) = ["+x+", "+y+", "+fi*Angles.RTOD+"]");
		}
			
		try
		{
			
			// Fx = df(x,u)/dx												[3x3]
			Fx.identity ();
			Fx.set (0, 2, - Math.sin (fi + w/2) * v);
			Fx.set (1, 2, Math.cos (fi + w/2) * v);
			
			// Qu				
			// du= du/du_tr   [2x2]
			du.set (0, 0, Math.cos(delta));
			du.set (0, 1, -vm*Math.sin(delta));
			du.set (1, 0, Math.sin(delta)/model.l);
			du.set (1, 1, vm*Math.cos(delta)/model.l);	
			
			// Incertidumbre en la medida vm,delta
			Aux.identity();
			Aux.set(0, 0, Math.pow( Math.abs(vm * SD_VM/100), 2.0 ));
			Aux.set(1, 1, Math.pow( Math.toRadians(SD_DELTA) , 2.0 ));

			Qu= du.times(Aux).times(du.transpose());
				
			// Fu = df(x,u)/du												[3x2]
			Fu.set (0, 0, Math.cos (fi + w/2));
			Fu.set (0, 1, -Math.sin (fi + w/2) * v / 2);
			Fu.set (1, 0, Math.sin (fi + w/2));
			Fu.set (1, 1, Math.cos (fi +w/2) * v / 2);
			Fu.set (2, 0, 0.0);
			Fu.set (2, 1, 1);
			
			Qmodel.times(0);
			Qmodel.identity();
			Qmodel.set(0,0,Math.pow(0.003,2));
			Qmodel.set(1,1,Math.pow(0.003,2));
			Qmodel.set(2,2,Math.toRadians(Math.pow(0.03,2)));
			
			// Qx=FuQuFu'+ Error Modelo										[3x3]
			Qx = (Fu.times(Qu).times(Fu.transpose())).plus(Qmodel);
			
			
			
			// P=FxPFx'+Qx									[3x3]
			P	= (Fx.times (P).times (Fx.transpose ())).plus (Qx);

			// X												[3x1]
			X.set (0, 0, x + dt * Math.cos(fi + dt * w / 2) * v );
			X.set (1, 0, y + dt * Math.sin(fi + dt * w / 2) * v);
			X.set (2, 0, Angles.radnorm_180(fi + dt * w));
				
		} catch (Exception e) { e.printStackTrace (); }
		
		posk.set (X.get(0,0), X.get(1,0), X.get(2,0));
		
		// Se memorizan las variables para recalcular el estado cuando hay latencia
		if((latencyTime>0) && (isLatency==false)){
			recP[index].setMatrix(P);
			recX[index].setMatrix(X);
			recVr[index] = v;
			recWr[index] = w;
			recSt[index] =System.currentTimeMillis();
			recDt[index++]=dt;
			if(index==recP.length) index = recP.length-1;
		}
	
		if (debug)
		{
			System.out.println ("Position (k) = ["+posk.x()+", "+posk.y()+", "+posk.alpha()*Angles.RTOD+"]");
			System.out.println ("Matrix P (prediction)");
			P.print (12, 5);
		}
	
	}

	
	public void updateLaser (Position Pos, int quality){
		double rAng, rx, ry;
		int lindex;
		double time = System.currentTimeMillis();
		
		if(quality > 70){
			CHI_TEST = 8;
			rx = 0.01;
			ry = 0.01;
			rAng = Math.toRadians(0.5)+ maxvelAng/10;	
		}else if(quality > 40){
			CHI_TEST = 6;
			rx = 0.02;
			ry = 0.02;
			rAng = Math.toRadians(1.0)+ maxvelAng/10;
		}
		else if(quality > 10){
			CHI_TEST = 3;
			rx = 0.04;
			ry = 0.04;
			rAng = Math.toRadians(3.0) + maxvelAng/10;
		}
		else{
			System.out.println("----------NO UPDATED -----------");
			return;
		}
		
		if(maxvelAng>10 || maxvelLin>0.75){
		//	CHI_TEST -=2 ;
			if(maxvelAng<5){
				rx += 0.1;
				ry += 0.1;
			}
			else{
				rx += 0.2;
				ry += 0.2;
			}
			
		}
		
		if(quality > 50 && (maxvelAng < Math.toRadians(10) && maxvelLin < 0.3)){
			CHI_TEST = Double.MAX_VALUE;
			rx = 0.005;
			ry = 0.005;
			rAng = Math.toRadians(0.5);
		}

		if(initialized == false)			// Si todavia no ha inicializado, se inicializa con la posicion del laser
		{
			// Initialise state prediction covariance
			P.identity();

			P.set (0, 0, Math.pow(0.1,2.0));
			P.set (1, 1, Math.pow(0.1,2.0));
			P.set (2, 2, Math.pow(0.1*DTOR,2.0));

			// Initialise estimated state
			X.set (0, 0, Pos.x());
			X.set (1, 0, Pos.y());
			X.set (2, 0, Pos.alpha());
			posk.set (X.get(0,0), X.get(1,0), X.get(2,0));

			initialized = true;
			System.out.println("  [iFrkKLoc] Initialized filter position ("+posk.x()+","+posk.y()+","+posk.alpha()*Angles.RTOD+")");
		
			if(debugLog == true){
				file.print((System.currentTimeMillis()-st)+"\t"+Pos.x()+"\t"+Pos.y()+"\t"+Pos.alpha()*Angles.RTOD+"\t");
				file.print((System.currentTimeMillis()-st)+"\t"+posk.x()+"\t"+posk.y()+"\t"+posk.alpha()*Angles.RTOD);
			}			
			index = 0;
			return;
		}
		
		if (no_filter)
		{
			// Initialise state prediction covariance
			P.identity();

			P.set (0, 0, Math.pow(0.1,2.0));
			P.set (1, 1, Math.pow(0.1,2.0));
			P.set (2, 2, Math.pow(0.1*DTOR,2.0));
		
			// Initialise estimated state
			X.set (0, 0, Pos.x());
			X.set (1, 0, Pos.y());
			X.set (2, 0, Pos.alpha());
			posk.set (X.get(0,0), X.get(1,0), X.get(2,0));
			index = 0;

			return;
		}
		

		if (latencyTime>0.0){
			isLatency = true;			// Para realizar la prediccion sin guardar datos de odometria
			if( index == 0 || (time - recSt[index-1]) >= latencyTime)
			{
				latency  = 0;
			}
			else
			{
				// Calcula los ciclos de latencia (latency) segun el tiempo de latencia del laser (latencyTime)
				latency = 1;
				for(int i=index-1; i>=0; i--){
					if( (time - recSt[i]) >= latencyTime ){
						if( Math.abs(time - recSt[i] - latencyTime) <= Math.abs(time - recSt[i+1] - latencyTime) )
							latency = index-1-i;
						else
							latency = index-2-i;					
					break;
					}
				}
				
			
				lindex = index-1-latency;
				if(lindex < 0) lindex = 0;
				X.setMatrix(recX[lindex]);	// Se vuelve al estado k-latency
				P.setMatrix(recP[lindex]);
				posk.set (X.get(0,0), X.get(1,0), X.get(2,0));

				match.n		= 0;
				if(matching(Pos,rx,ry,rAng))	// Actualizando en instante k-latency
					update();
				else{
					System.out.println("  [iFrkKLoc] Reinitialising Kalman Filter");
					// Initialise state prediction covariance
					P.identity();

					P.set (0, 0, Math.pow(0.1,2.0));
					P.set (1, 1, Math.pow(0.1,2.0));
					P.set (2, 2, Math.pow(0.1*DTOR,2.0));
		
					// Initialise estimated state
					X.set (0, 0, Pos.x());
					X.set (1, 0, Pos.y());
					X.set (2, 0, Pos.alpha());
					posk.set (X.get(0,0), X.get(1,0), X.get(2,0));
					index = 0;
				}
				for(int i=lindex+1;i<index;i++){
					prediction(recVr[i],recWr[i],recDt[i]);		// Cuando se realiza la prediccion con la odometria	
				}

			}
			index = 0;
			lindex = 0;
			isLatency = false;						// Para realizar la prediccion sin guardar datos de odometria
		}
		else{
			match.n		= 0;
			if(matching(Pos,rx,ry,rAng)){	// Actualizando en instante k-latency
				//System.out.println("ErrAnt["+Math.sqrt(P.get(0,0))+","+Math.sqrt(P.get(1,1))+", "+Math.toDegrees(Math.sqrt(P.get(2,2)))+"]");
				update();
				//System.out.println("Upd: ["+Pos.x()+","+Pos.y()+", "+Math.toDegrees(Pos.alpha())+"]"+"Upd: ["+posk.x()+","+posk.y()+", "+Math.toDegrees(posk.alpha())+"] Q="+quality);
				//System.out.println("Err["+Math.sqrt(P.get(0,0))+","+Math.sqrt(P.get(1,1))+", "+Math.toDegrees(Math.sqrt(P.get(2,2)))+"]");
				//System.out.println("vr = "+maxvelLin+" wr = "+Math.toDegrees(maxvelAng) +" rx="+rx+" ry="+ry+" rAng = "+Math.toDegrees(rAng));
			}
			else{
				//System.out.println("Upd FAIL: ["+Pos.x()+","+Pos.y()+"] Q="+quality);
				if(!REINIT) return;
				
				System.out.println("  [iFrkKLoc] Reinitialising Kalman Filter");
				// Initialise state prediction covariance
				P.identity();

				P.set (0, 0, Math.pow(0.1,2.0));
				P.set (1, 1, Math.pow(0.1,2.0));
				P.set (2, 2, Math.pow(0.1*DTOR,2.0));
		
				// Initialise estimated state
				X.set (0, 0, Pos.x());
				X.set (1, 0, Pos.y());
				X.set (2, 0, Pos.alpha());
				posk.set (X.get(0,0), X.get(1,0), X.get(2,0));
				index = 0;
			}			
		}
		isLatency = false;
		
				
		if(debugLog == true){
			file.print((System.currentTimeMillis()-st)+"\t"+Pos.x()+"\t"+Pos.y()+"\t"+Pos.alpha()*Angles.RTOD+"\t");
			file.print((System.currentTimeMillis()-st)+"\t"+posk.x()+"\t"+posk.y()+"\t"+posk.alpha()*Angles.RTOD);
		}
		
		maxvelAng = 0;
		maxvelLin = 0;
	}
	
	
	// Actualiza con las medidas de posicion del Laser
	//
	// Entradas: posicion (x,y) y orientacion estimada por el Laser
	
	public void updateLaser (Position Pos){
		double rPos, rAng, rx, ry;
		rPos = 0.01;
		int lindex;
		double time = System.currentTimeMillis();
		if(velLin < 0.1){
			rx = rPos ;
			ry = rPos ;
			rAng = Math.toRadians(1);		
		}
		else if(velLin < 0.5 && velAng < Math.toRadians(5)){
			rx = rPos + Math.abs(0.02 * Math.cos(posk.alpha()));
			ry = rPos + Math.abs(0.02 * Math.sin(posk.alpha()));
			rAng = Math.toRadians(5);
		}
		else if(velAng < Math.toRadians(5)){
			rx = rPos + Math.abs(0.05 * Math.cos(posk.alpha()));
			ry = rPos + Math.abs(0.05 * Math.sin(posk.alpha()));
			rAng = Math.toRadians(5);
		}
		else{
			rx = rPos + Math.abs(0.1 * Math.cos(posk.alpha()));
			ry = rPos + Math.abs(0.1 * Math.sin(posk.alpha()));
			rAng = Math.toRadians(20);
		}
		
		if(initialized == false)			// Si todavia no ha inicializado, se inicializa con la posicion del laser
		{
			// Initialise state prediction covariance
			P.identity();

			P.set (0, 0, Math.pow(0.1,2.0));
			P.set (1, 1, Math.pow(0.1,2.0));
			P.set (2, 2, Math.pow(0.1*DTOR,2.0));

			// Initialise estimated state
			X.set (0, 0, Pos.x());
			X.set (1, 0, Pos.y());
			X.set (2, 0, Pos.alpha());
			posk.set (X.get(0,0), X.get(1,0), X.get(2,0));

			initialized = true;
			System.out.println("  [iFrkKLoc] Initialized filter position ("+posk.x()+","+posk.y()+","+posk.alpha()*Angles.RTOD+")");
		
			if(debugLog == true){
				file.print((System.currentTimeMillis()-st)+"\t"+Pos.x()+"\t"+Pos.y()+"\t"+Pos.alpha()*Angles.RTOD+"\t");
				file.print((System.currentTimeMillis()-st)+"\t"+posk.x()+"\t"+posk.y()+"\t"+posk.alpha()*Angles.RTOD);
			}			
			index = 0;
			return;
		}
		
		if (no_filter)
		{
			// Initialise state prediction covariance
			P.identity();

			P.set (0, 0, Math.pow(0.1,2.0));
			P.set (1, 1, Math.pow(0.1,2.0));
			P.set (2, 2, Math.pow(0.1*DTOR,2.0));
		
			// Initialise estimated state
			X.set (0, 0, Pos.x());
			X.set (1, 0, Pos.y());
			X.set (2, 0, Pos.alpha());
			posk.set (X.get(0,0), X.get(1,0), X.get(2,0));
			index = 0;

			return;
		}
		

		if (latencyTime>0.0){
			isLatency = true;			// Para realizar la prediccion sin guardar datos de odometria
			if( index == 0 || (time - recSt[index-1]) >= latencyTime)
			{
				latency  = 0;
			}
			else
			{
				// Calcula los ciclos de latencia (latency) segun el tiempo de latencia del laser (latencyTime)
				latency = 1;
				for(int i=index-1; i>=0; i--){
					if( (time - recSt[i]) >= latencyTime ){
						if( Math.abs(time - recSt[i] - latencyTime) <= Math.abs(time - recSt[i+1] - latencyTime) )
							latency = index-1-i;
						else
							latency = index-2-i;					
					break;
					}
				}
				
			
				lindex = index-1-latency;
				if(lindex < 0) lindex = 0;
				X.setMatrix(recX[lindex]);	// Se vuelve al estado k-latency
				P.setMatrix(recP[lindex]);
				posk.set (X.get(0,0), X.get(1,0), X.get(2,0));

				match.n		= 0;
				if(matching(Pos,rx,ry,rAng))	// Actualizando en instante k-latency
					update();
				else{
					System.out.println("  [iFrkKLoc] Reinitialising Kalman Filter");
					// Initialise state prediction covariance
					P.identity();

					P.set (0, 0, Math.pow(0.1,2.0));
					P.set (1, 1, Math.pow(0.1,2.0));
					P.set (2, 2, Math.pow(0.1*DTOR,2.0));
		
					// Initialise estimated state
					X.set (0, 0, Pos.x());
					X.set (1, 0, Pos.y());
					X.set (2, 0, Pos.alpha());
					posk.set (X.get(0,0), X.get(1,0), X.get(2,0));
					index = 0;
				}
				for(int i=lindex+1;i<index;i++){
					prediction(recVr[i],recWr[i],recDt[i]);		// Cuando se realiza la prediccion con la odometria	
				}

			}
			index = 0;
			lindex = 0;
			isLatency = false;						// Para realizar la prediccion sin guardar datos de odometria
		}
		else{
			match.n		= 0;
			if(matching(Pos,rx,ry,rAng)){	// Actualizando en instante k-latency
				update();
				//System.out.println("Upd: ["+Pos.x()+","+Pos.y()+"]"+"Upd: ["+posk.x()+","+posk.y()+"]");
				//System.out.println("Err["+Math.sqrt(P.get(0,0))+","+Math.sqrt(P.get(1,1))+"]");
				//System.out.println("vr = "+velLin+" wr = "+Math.toDegrees(velAng) +" rPos = "+rPos+" rAng = "+Math.toDegrees(rAng));
			}
			else{
				//System.out.println("Upd FAIL: ["+Pos.x()+","+Pos.y()+"]");
				if(!REINIT) return;
				
				System.out.println("  [iFrkKLoc] Reinitialising Kalman Filter");
				// Initialise state prediction covariance
				P.identity();

				P.set (0, 0, Math.pow(0.1,2.0));
				P.set (1, 1, Math.pow(0.1,2.0));
				P.set (2, 2, Math.pow(0.1*DTOR,2.0));
		
				// Initialise estimated state
				X.set (0, 0, Pos.x());
				X.set (1, 0, Pos.y());
				X.set (2, 0, Pos.alpha());
				posk.set (X.get(0,0), X.get(1,0), X.get(2,0));
				index = 0;
			}			
		}
		isLatency = false;
		
				
		if(debugLog == true){
			file.print((System.currentTimeMillis()-st)+"\t"+Pos.x()+"\t"+Pos.y()+"\t"+Pos.alpha()*Angles.RTOD+"\t");
			file.print((System.currentTimeMillis()-st)+"\t"+posk.x()+"\t"+posk.y()+"\t"+posk.alpha()*Angles.RTOD);
		}		
	}

// Valida las medidas de posicion y orientacion, dando la incertidumbre en la Posicion y Orientacion.
// Forma las matrices para la actualizacion (updated)

	private boolean matching (Position pos, double rPosx,double rPosy, double rAng)	{
	
		Matrix		v, hx, zp, s, r;
		double		NIS;
		boolean 	matched = true;
		
		if (initialized == false)			return false;
			
		//match.n		= 0;

		v =  new Matrix(2,1);
		hx = new Matrix(2,3);
		zp = new Matrix (2,1);
		s = new Matrix (2,2);
		r = new Matrix (2,2);

				//  MATCHING SOLO POSICION  //


		// Prediccion de la medida  (solo posicion x,y)
		zp.set(0,0 ,X.get(0,0)); 			
		zp.set(1,0 ,X.get(1,0));				
				
		// Funcion de observacion
		hx.identity();		// dh(k+1)/dx	
	
		// Varianza de la observacion
		r.set (0, 0, Math.pow(rPosx,2.0));
		r.set (1, 1, Math.pow(rPosy,2.0));

		v.set(0,0,pos.x()-zp.get(0,0));
		v.set(1,0,pos.y()-zp.get(1,0));

		s		= hx.times (P).times (hx.transpose ()).plus (r);
				
		NIS=((v.transpose()).times(s.inverse()).times(v)).get(0,0); 						
		if (NIS < CHI_TEST * CHI_TEST){ 
				
			match.hx[match.n][0]	= hx.get (0, 0);
			match.hx[match.n][1]	= hx.get (0, 1);
			match.hx[match.n][2]	= hx.get (0, 2);
						
			match.r[match.n]		= r.get (0,0);
			match.v[match.n]		= v.get(0,0);
					
			match.n ++;
					
			match.hx[match.n][0]	= hx.get (1, 0);
			match.hx[match.n][1]	= hx.get (1, 1);
			match.hx[match.n][2]	= hx.get (1, 2);
						
			match.r[match.n]		= r.get (1, 1);
			match.v[match.n]		= v.get(1,0);
			match.n ++;	
		}
		else {
			matched = false;
			System.out.println("--[iFrkKLoc] Non validated position NIS = "+Math.sqrt(NIS)+ " V = ["+v.get(0,0)+","+v.get(1,0)+"] MAX = "+CHI_TEST);	
		}
		
		//  MATCHING SOLO ORIENTACION  //				


		v =  new Matrix(1,1);
		hx = new Matrix(1,3);
		zp = new Matrix (1,1);
		s = new Matrix (1,1);
		r = new Matrix (1,1);	
			
		// Prediccion de la medida  (zp = posicion Laser)		
		zp.set(0,0 ,X.get(2,0));
				
				
		// Funcion de observacion
		hx.set(0,2,1.0);

		// Varianza de la observacion
		r.set (0, 0, Math.pow(rAng,2.0));
				
		v.set(0,0,Angles.radnorm_180(pos.alpha()-zp.get(0,0)));

		s		= hx.times (P).times (hx.transpose ()).plus (r);
				
		NIS=((v.transpose()).times(s.inverse()).times(v)).get(0,0); 						
		if (NIS < CHI_TEST * CHI_TEST){ 
				
					match.hx[match.n][0]	= hx.get (0, 0);
					match.hx[match.n][1]	= hx.get (0, 1);
					match.hx[match.n][2]	= hx.get (0, 2);
						
					match.r[match.n]		= r.get (0, 0);
					match.v[match.n]		= v.get(0,0);
					
					match.n ++;
		}
		else{
			 matched = false;
			 System.out.println("--[iFrkKLoc] Non validated heading NIS = "+Math.sqrt(NIS)+ " v = "+v.get(0,0)*RTOD+" MAX = "+CHI_TEST);					
		}
		return matched;
	}
	






// Actualiza segun las medidas validadas con el matching
	private void update ()	{
		int			i;
		
		if (initialized==false){
			return;
		}

		if (match.n == 0)		return;

		try
		{
			// Hx													[nx3]
			Hx		= new Matrix (match.n, 3);
			for (i = 0; i < match.n; i++)
			{
				Hx.set (i, 0, match.hx[i][0]);
				Hx.set (i, 1, match.hx[i][1]);
				Hx.set (i, 2, match.hx[i][2]);
			}

			// R													[nxn]
			R		= new Matrix (match.n, match.n);
			for (i = 0; i < match.n; i++)
				R.set (i, i, match.r[i]);

			// V													[nx1]
			V		= new Matrix (match.n, 1);
			for (i = 0; i < match.n; i++)
				V.set (i, 0, match.v[i]);

			// S=HxPHx'+R											[nxn]
			S		= Hx.times (P).times (Hx.transpose ()).plus (R);
		
			// W=PHx'/S												[3xn]
			W		= P.times (Hx.transpose ()).times (S.inverse ());
			
			
			// Xe=X+WV												[3x1]
			Xe		= X.plus (W.times (V));
			
			// Pe=P-WSW'											[3x3]
			Pe		= P.minus (W.times (S).times (W.transpose ()));
			

			
			// Update current state and covariances
			X.setMatrix (Xe); 
			P.setMatrix (Pe); 		
			posk.set (X.get(0,0), X.get(1,0), X.get(2,0));
			
			// Calcula el NIS
			NIS = (V.transpose().times(S.inverse()).times(V)).get(0,0);
			
		} catch (Exception e) { e.printStackTrace (); }

		if (debug)
		{
			System.out.println ("Kalman update with " + match.n + " measures");
			System.out.println ("Position (corrected) = ["+X.get(0,0)+", "+X.get(1,0)+", "+X.get(2,0)*Angles.RTOD+"]");
			System.out.println ("Innovation ");
			V.print(12,5);
			System.out.println ("R: ");
			R.print(12,5);
			System.out.println ("H ");
			Hx.print(12,5);
			System.out.println ("Matrix P (update)");
			P.print (12, 5);
		}

	}
	
}



