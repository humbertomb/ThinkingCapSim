/**
 * Created on 17-jun-2006
 *
 * @author Humberto Martinez Barbera
 */
package tcrob.umu.soccer.gm.kalman;

import java.awt.*;

import wucore.widgets.*;
import wucore.utils.math.*;
import wucore.utils.math.jama.*;

import tcrob.umu.soccer.gm.data.*;
import tcrob.umu.soccer.gm.*;

public class Kalman implements Localisation
{
	private String ID = new String("KALMAN"); 

	
	// Kalman filter
	protected Matrix			S; 						// State (x, y, theta)
	public 	  Matrix			P;
	protected Matrix			A; 
	protected Matrix			W;
	protected Matrix			Q;
	protected Matrix			Z;
	protected Matrix			ZV;
	protected Matrix			H;
	private Matrix			V;
	private Matrix			R;
	private Matrix			I;

	// Observations
	protected Matrix			objaux;
	protected Matrix			robot1;
	protected Matrix			robot2;

	// NIS position calculations
	protected double			pnis;					// Position NIS
	protected Matrix			Vp;
	protected Matrix			HXp;
	protected Matrix			ZPp;
	protected Matrix			Sp;
	protected Matrix			Rp;

	// NIS heading calculations
	protected double			hnis;					// Heading NIS
	protected Matrix			Vh;
	protected Matrix			HXh;
	protected Matrix			ZPh;
	protected Matrix			Sh;
	protected Matrix			Rh;

	protected double			pred_odo_fl;
	protected double			pred_odo_fr;
	protected double			corr_anch_thre;
	protected double			corr_nois_dist; 
	protected double			corr_nois_ang;
	protected double			corr_delta_dist;
	protected double			corr_delta_ang;
	
	protected Gs				gs;

	protected int[]			mLastAnchored;
	protected boolean[]		mLastUpdated;
	protected int[]			mNewLandmarks;
	
	
	// Limit in the landmark adquisition tolerance. Medium values.
	private  final int  DEACTIVATED_TOLERANCE = 0;
	private  final int  LOW_TOLERANCE = 1;
	private  final int  MEDIUM_TOLERANCE = 2;
	private  final int  HIGH_TOLERANCE = 3;
	
	
	private int		tolerance	= MEDIUM_TOLERANCE;
	private double	limit		= 20.0;
	private double	max_filter	= 100.0;
	private double	min_filter	= 5.0;
	private double	filter_mult = 1.5;
	private double	filter_div 	= 2.0;
	
	private double	odolinNoise;
	private double	odorotNoise;
	private double	distNoise;
	private double	angleNoise;
	
	public Kalman (int toler, double odolinNoise, double odorotNoise, double distNoise, double angleNoise)
	{
		mLastAnchored	= new int[LocLps.LPS_SIZE];
		mLastUpdated		= new boolean[LocLps.LPS_SIZE];
		mNewLandmarks	= new int[LocLps.NUM_LMS + LocLps.NUM_NETS];

		setToleranceSettings(toler);
		
		// Initialise position with uncertainly
		GsPosition	initPos;
		initPos			= new GsPosition ();
		initPos.x		= 0;
		initPos.y		= -1500;
		initPos.dx		= 100;
		initPos.dy		= 100;
		initPos.theta	= (double) (90.0 * Angles.DTOR);
		initPos.dtheta	= (double) (90.0 * Angles.DTOR);
		
		gs	= new Gs ();
		
		S	= new Matrix (3, 1);
		P	= new Matrix (3, 3);
		A	= new Matrix (3, 3);
		W	= new Matrix (3, 3);
		Q	= new Matrix (3, 3);
		I	= new Matrix (3, 3);
		Z	= new Matrix (2, 1);
		ZV	= new Matrix (2, 1);
		H	= new Matrix (2, 3);
		R	= new Matrix (2, 2);
		V	= new Matrix (2, 2);

		objaux	= new Matrix (3,1);
		robot1	= new Matrix (3,1);
		robot2	= new Matrix (3,1);

		Vp	= new Matrix (2,1);
		HXp	= new Matrix (2,3);
		ZPp	= new Matrix (2,1);
		Sp	= new Matrix (2,2);
		Rp	= new Matrix (2,2);
		
		Vh	= new Matrix (1,1);
		HXh	= new Matrix (1,3);
		ZPh	= new Matrix (1,1);
		Sh	= new Matrix (1,1);
		Rh	= new Matrix (1,1);

		I.identity ();
		R.identity ();
		V.identity ();
		
		
		pred_odo_fl = odolinNoise;
		pred_odo_fr = odorotNoise;
		corr_anch_thre = 0.9;
		corr_nois_dist = distNoise; 
		corr_nois_ang = angleNoise;
		corr_delta_dist = 100.0;
		corr_delta_ang = RAD(10.0);
		
		initialPosition (initPos);
	}

	public Gs getGs ()							{ return gs; }
	public double getNISHeading ()				{ return hnis; }
	public double getNISPosition ()				{ return pnis; }
	public boolean getLastUpdated (int index)		{ return mLastUpdated[index]; }

	static protected double RAD (double deg)		{ return deg * Angles.DTOR; }

	static protected double distance (Matrix a, Matrix b)
	{
		double disx, disy;
		
		if(a.get(0,0) > b.get(0,0))
			disx = Math.pow(a.get(0,0) - b.get(0,0), 2);
		else
			disx = Math.pow(b.get(0,0) - a.get(0,0), 2);
			
		if(a.get(1,0) > b.get(1,0))
			disy = Math.pow(a.get(1,0) - b.get(1,0), 2);
		else
			disy = Math.pow(b.get(1,0) - a.get(1,0), 2);
			
		return Math.sqrt(disx + disy);
	}

	static protected double angle (Matrix a, Matrix b)
	{
		double xr, yr, tr, xb, yb;
		double t1;
		
		xr = a.get(0,0);
		yr = a.get(1,0);
		tr = a.get(2,0);
		xb = b.get(0,0);
		yb = b.get(1,0);

		t1 = Math.atan2(yb-yr, xb-xr);

		return Angles.radnorm_180 (tr-t1); 
	}
	
	public void initialPosition (GsPosition pos)
	{
		S.zero ();
		S.set (0,0, (double) pos.x);
		S.set (1,0, (double) pos.y);
		S.set (2,0, (double) pos.theta);
		
		P.zero ();
		P.set (0,0, Math.pow((double) pos.dx/2, 2.0));
		P.set (1,1, Math.pow((double) pos.dy/2, 2.0));
		P.set (2,2, Math.pow((double) pos.dtheta/2, 2.0));
		
		
		updatePosition ();
	}
	
	public void updateMotionOnly (Odometry odo)
	{
		//System.out.print("MO ");
		// Update motion
		updateMotion (odo);
		
		// Localise
		updatePosition ();
	}
	
	public void updateMotionAndSensors (Odometry odo, LocLps lps)
	{
		//System.out.print("MS ");
		// Update motion
		updateMotion (odo);
//		
//		// Update sensors
		updateSensors (lps);
//		
//		// Localise
 		updatePosition ();
		
	}
	
	protected void updateMotion (Odometry odom)
	{
		A.identity ();
		W.identity ();
		Q.identity ();
		
		Q.set(0, 0, Math.pow(odom.dlin* pred_odo_fl +(odom.dlat * pred_odo_fl * 0.1), 2.0) );
		Q.set(1, 1, Math.pow(odom.dlat* pred_odo_fl +(odom.dlin * pred_odo_fl * 0.1), 2.0) );
		Q.set(2, 2, Math.pow(odom.drot* pred_odo_fr +(odom.dlat * pred_odo_fl * 0.001) +(odom.dlin * pred_odo_fl * 0.001), 2.0)); 
		
		A.set(0, 0, 1.0);
		A.set(0, 2, -odom.dlat * Math.cos(S.get(2,0)) - odom.dlin * Math.sin(S.get(2,0)));
		A.set(1, 1, 1.0);
		A.set(1, 2, odom.dlin * Math.cos(S.get(2,0)) - odom.dlat * Math.sin(S.get(2,0)));
		A.set(2, 2, 1.0);
		
		W.set(0, 0, Math.cos(S.get(2,0)));
		W.set(0, 1, -Math.sin(S.get(2,0)));
		W.set(1, 0, Math.sin(S.get(2,0)));
		W.set(1, 1, Math.cos(S.get(2,0)));
		W.set(2, 2, 1.0);
		
		S.set (0, 0, S.get(0,0) + ((double)odom.dlin * Math.cos(S.get(2,0))) - ((double)odom.dlat * Math.sin(S.get(2,0))));
		S.set (1, 0, S.get(1,0) + ((double)odom.dlin * Math.sin(S.get(2,0))) + ((double)odom.dlat * Math.cos(S.get(2,0))));
		S.set (2, 0, Angles.radnorm_180 (S.get(2,0) + (double)odom.drot));
		
		P	= A.times (P).times (A.transpose()).plus (W.times (Q).times (W.transpose()));
	}
	
	protected void updateSensors (LocLps lps)
	{
		int m;
		LocLpo				objlpo;
		Matrix			K;
		Matrix			S1, S2;
		
		for (int index = 0; index < LocLps.LPS_SIZE; index++)
			mLastUpdated[index]	= false;	

		m = newLandmarks (lps, mNewLandmarks);

		if (m <= 0)			return;
		
		Z = new Matrix(2*m, 1);
		ZV = new Matrix(2*m, 1);
		
		H = new Matrix(2*m, 3);
		K = new Matrix(3, 2*m);	
		V = new Matrix(2*m, 2*m);
		R = new Matrix(2*m, 2*m);
		
		V.identity();
		R.identity();
		H.identity();
		ZV.identity();
		Z.identity();
		K.identity();
		
		for (int i = 0; i < m; i++)
		{
			int[]			omaux;
			
			objlpo = lps.getLpo(mNewLandmarks[i]);
			Z.set(2*i, 0, objlpo.rho);
			Z.set(2*i+1, 0, objlpo.theta);
			
			omaux = Localisation.markAt (mNewLandmarks[i]);
			if (omaux == null)			continue;
			
			objaux.set(0, 0, (double)omaux[0]);
			objaux.set(1, 0, (double)omaux[1]);
			objaux.set(2, 0, 0.0);
			
			ZV.set(2*i, 0, distance (S, objaux));
			ZV.set(2*i+1, 0, -angle (S, objaux));

			
	 		double rx = S.get(0, 0);
	  		double ry = S.get(1, 0);
	  		double lx = (double)omaux[0];
	  		double ly = (double)omaux[1];
	   	 	
	   	 	double den = Math.pow(lx-rx,2.0)+Math.pow(ly-ry,2.0);
	    		
	   		H.set(2*i,0,-(lx-rx)/Math.sqrt(den)); 
	   		H.set(2*i,1,-(ly-ry)/Math.sqrt(den)); 
	   		H.set(2*i,2, 0.0); 
	   		H.set(2*i+1,0, (ly-ry)/den); 
	   		H.set(2*i+1,1, -(lx-rx)/den); 
	   		H.set(2*i+1,2, -1.0);

//			R.set (i*2, i*2, objlpo.getP().get(0, 0));
//			R.set (i*2+1, i*2+1, objlpo.getP().get(1, 1));

			R.set (i*2, i*2, corr_nois_dist*corr_nois_dist);
			R.set (i*2+1, i*2+1, corr_nois_ang*corr_nois_ang);
		}
		
		if(tolerance != DEACTIVATED_TOLERANCE)
			if (!tolerate(Z, ZV, H, R))
				return;
		
		S1	= H.times (P).times (H.transpose ());
		S2	= V.times (R).times (V.transpose ());

		K	= P.times (H.transpose ()).times (S1.plus (S2).inverse());
		P	= I.minus (K.times (H)).times (P);
		S	= S.plus (K.times (Z.minus (ZV)));
		
		S.set (2, 0, Angles.radnorm_180 (S.get(2, 0)));
		
//		System.out.print("["+getNISPosition()+"] ");
		
//		}
//		for (int i = 0; i < m; i++)
//		{
//			processLandmark (lps, Z, H, ZV, mNewLandmarks[i]);
//						
//			if (H.getRowDimension () == 0)			continue;
//		
//			Matrix			K;
//			Matrix			S1, S2;
//			
//			objlpo = lps.getLpo(mNewLandmarks[i]);
//			
//			//R.setMatrix(objlpo.getP());
//			R.set (0, 0, corr_nois_dist*corr_nois_dist);
//			R.set (1, 1, corr_nois_ang*corr_nois_ang);
//			
//			//System.out.print("["+tolerance+"] ");
//			if(tolerance != DEACTIVATED_TOLERANCE)
//				if (!tolerate(Z, ZV, H, R))
//					continue;
//				
//				
//				S1	= H.times (P).times (H.transpose ());
//				S2	= V.times (R).times (V.transpose ());
//			
//				K	= P.times (H.transpose ()).times (S1.plus (S2).inverse());
//				P	= I.minus (K.times (H)).times (P);
//				S	= S.plus (K.times (Z.minus (ZV)));
//			
//			S.set (2, 0, Angles.radnorm_180 (S.get(2, 0)));
//		}
	}

	private boolean tolerate(Matrix z2, Matrix zv2, Matrix h2, Matrix r2) 
	{
        
		Matrix X = null;
		
        Matrix zzv = z2.minus(zv2);
        
		
		
		X = zzv.transpose().times(
				(h2.times(P).times(h2.transpose()).plus(r2)).inverse()).times(zzv);
						
		
		if(X.get(0, 0) < limit)
		{
//			System.out.println("Accepted "+X.get(0, 0)+" < "+limit);
			
			limit = limit / filter_div;
			if(limit < min_filter)
				limit = min_filter;
			return true;
		}else
		{
//			System.out.println("Rejected "+X.get(0, 0)+" >= "+limit);
			
			limit = limit * filter_mult;
			if(limit > max_filter)
				limit = max_filter;
			return false;
		}
			
	}

	
	protected void updatePosition ()
	{
		GsPosition	pos;
		double		quality;
	  	Matrix		eigenvalues;
		
		pos			= gs.getPosition ();

		// Check field limits
		double		fieldMaxX = TOTAL_X_SIZE * 0.5;
	    double		fieldMaxY = TOTAL_Y_SIZE * 0.5 + NET_DEPTH;
	  
		if (S.get (0, 0) > fieldMaxX)			S.set (0, 0, fieldMaxX);
	  	if (S.get (1, 0) > fieldMaxY)			S.set (1, 0, fieldMaxY);
	  	if (S.get (0, 0) < -fieldMaxX)		S.set (0, 0, -fieldMaxX);
	  	if (S.get (1, 0) < -fieldMaxY)		S.set (1, 0, -fieldMaxY);

		// Update current uncertainty
	  	eigenvalues	= P.jacobian ();
//	  	pos.dx		= (int) Math.sqrt(eigenvalues.get (0, 0)); 
//	  	pos.dy		= (int) Math.sqrt(eigenvalues.get (0, 1)); 
//	  	pos.dtheta	= (double) Math.sqrt(eigenvalues.get (0, 2)); 
	  	pos.dx		= (int) Math.sqrt(eigenvalues.get (0, 0)); 
	  	pos.dy		= (int) Math.sqrt(eigenvalues.get (0, 1)); 
	  	pos.dtheta	= (double) Math.sqrt(eigenvalues.get (0, 2)); 
		
//	  	System.out.println("P=");
//	  	P.print(5, 5);
	  	
	  	// Use previous position pos and current position S(t) and uncertainty P(t)
		pnis			= matchingPosition (pos);
		hnis			= matchingHeading (pos);

	  	// Update current position
		pos.x		= (int) S.get (0, 0);
		pos.y		= (int) S.get (1, 0);
		pos.theta	= (double) S.get (2, 0);
		
	  	// Compute overall quality of the position
		quality		= ((1.0 -(pos.dx/1000.0)) * (1.0 -(pos.dy/1000.0)) * (1.0 -(pos.dtheta/RAD(180.0))));
		if (quality < 0.0)		quality = 0.0;
		if (quality > 1.0)		quality = 1.0;
		gs.setQuality ((double) quality);
		
	}
	
	
	
	private int newLandmarks (LocLps lps, int[] element)
	{
		LocLpo		lpo;	
		int		numelem = 0;
		
		for (int index = LocLps.INIT_LMS; index < (LocLps.INIT_LMS + LocLps.NUM_LMS + LocLps.NUM_NETS); index++)
		{
			lpo = lps.getLpo(index);		
			
			if ((lpo.last_anchored > mLastAnchored[index]) && (lpo.anchored > 0.95))
			{
				mLastAnchored[index]	= lpo.last_anchored;
				mLastUpdated[index]	= true;	
				element[numelem++]	= index;
			}
		}
		
		if(numelem > 1)
		{	
			int i, j, tmp;
			for (i=0; i<numelem-1; i++)
			{
				for (j=0; j<numelem-1-i; j++)
					if (lps.getLpo(element[j+1]).anchored > lps.getLpo(element[j]).anchored)
					{ 
						tmp = element[j];   
						element[j] = element[j+1];
						element[j+1] = tmp;
					}
			}
		}
		return numelem;
	}
	
	private void processLandmark (LocLps lps, Matrix Z, Matrix H, Matrix ZV, int numlps)
	{
		int				i;
		int[]			omaux;
		LocLpo				objlpo;
		double			delta;

		Z.zero ();
		ZV.zero ();
		H.zero ();
		objaux.zero ();
		robot1.zero ();
		robot2.zero ();
		
		objlpo = lps.getLpo(numlps);
		
		Z.set(0, 0, objlpo.rho);
		Z.set(1, 0, objlpo.theta);
		
		omaux = Localisation.markAt (numlps);

		if (omaux == null)			return;
				
		for (i = 0; i < 3; i++)
		{
			if(i<2)
				delta = corr_delta_dist;
			else
				delta = corr_delta_ang;
			
			robot1.set(0, 0, S.get(0,0));
			robot1.set(1, 0, S.get(1,0));
			robot1.set(2, 0, S.get(2,0));
			
			robot2.set(0, 0, S.get(0,0));
			robot2.set(1, 0, S.get(1,0));
			robot2.set(2, 0, S.get(2,0));
			
			robot1.set(i, 0, robot1.get(i,0) + delta);
			robot2.set(i, 0, robot2.get(i,0) - delta);
			

			
			objaux.set(0, 0, (double)omaux[0]);
			objaux.set(1, 0, (double)omaux[1]);
			objaux.set(2, 0, 0.0);
			
			ZV.set(0, 0, distance (S, objaux));
			ZV.set(1, 0, -angle (S, objaux));
			
			H.set(0, i, (distance (robot1, objaux) - distance (robot2, objaux)) / (2 * delta));
			H.set(1, i, ((-angle(robot1, objaux)) - (-angle(robot2, objaux))) / (2 * delta));
		}
	}
	
	private double matchingPosition (GsPosition pos)
	{	
		double		nis;
		
		Vp.zero ();
		HXp.zero ();
		ZPp.zero ();
		Sp.zero ();
		Rp.zero ();

		// Prediccion de la medida  (solo posicion x,y)
		ZPp.set(0,0 ,S.get(0,0)); 			
		ZPp.set(1,0 ,S.get(1,0));				
				
		// Funcion de observacion
		HXp.identity();		// dh(k+1)/dx	
	
		// Varianza de la observacion
		Rp.set (0, 0, Math.pow(pos.dx,2.0));
		Rp.set (1, 1, Math.pow(pos.dy,2.0));

		Vp.set(0,0,pos.x-ZPp.get(0,0));
		Vp.set(1,0,pos.y-ZPp.get(1,0));

//		System.out.println("HXp = ");
//		HXp.print(5, 5);
//		System.out.println("P = ");
//		P.print(5, 5);
//		System.out.println("Rp = ");
//		Rp.print(5, 5);
	
		
		Sp	= HXp.times (P).times (HXp.transpose ()).plus (Rp);	
		
//		System.out.println("Sp = ");
//		Sp.print(5, 5);
		
		nis	= ((Vp.transpose()).times(Sp.inverse()).times(Vp)).get(0,0);
				
		return nis;
	}
	
	private double matchingHeading (GsPosition pos)
	{	
		double		nis;
				
		Vh.zero ();
		HXh.zero ();
		ZPh.zero ();
		Sh.zero ();
		Rh.zero ();
			
		// Prediccion de la medida  (zp = posicion Laser)		
		ZPh.set(0,0 ,S.get(2,0));
								
		// Funcion de observacion
		HXh.set(0,2,1.0);

		// Varianza de la observacion
		Rh.set (0, 0, Math.pow(pos.dtheta,2.0));
				
		Vh.set(0,0,Angles.radnorm_180(pos.theta-ZPh.get(0,0)));

		Sh	= HXh.times (P).times (HXh.transpose ()).plus (Rh);	
		nis	= ((Vh.transpose()).times(Sh.inverse()).times(Vh)).get(0,0); 	
				
		return nis;
	}
	
	public void drawElements (Model2D model)
	{
		int size = 75;
		double dx, dy;
		Matrix		eigenvalues;

		// Update current uncertainty
	  	eigenvalues	= P.jacobian ();
	  	
	  	dx		= Math.sqrt(eigenvalues.get (0, 0)); 
	  	dy		= Math.sqrt(eigenvalues.get (0, 1)); 
			  	
//System.out.println("dx = "+dx+"  dy = "+dy);
		
		model.addRawCircle(S.get(0,0), S.get(1,0), size, Model2D.FILLED, Color.RED);
		//model.addRawBox(x1, y2, x2, y2, Model2D.FILLED, Color.RED);
		//addRawEllipse(x1, x1, 500, 1000, Color.RED);
		model.addRawArrow (S.get(0,0) - dx/2, S.get(1,0)-dy/2, size*3, S.get(2,0), Color.RED);

	}

	public void setGT(GsPosition pos) {
		// TODO Auto-generated method stub
//		System.out.print("GT    :    x:" + pos.x + "    y:" + pos.y + "    t:"+pos.theta+"\n");

	}
	
	public String getId() {
		return ID;
	}

	public void setId(String newid) {
		ID = new String(newid);
	}

	public void setToleranceSettings(int selectedIndex) {
		
		tolerance = selectedIndex;
		
		switch (selectedIndex)
		{
		case LOW_TOLERANCE:
			limit		= 25.0;
			max_filter	= 300.0;
			min_filter	= 20.0;
			filter_mult = 2.0;
			filter_div 	= 1.5;
			break;
		case MEDIUM_TOLERANCE:
			limit		= 35.0;
			max_filter	= 500.0;
			min_filter	= 30.0;
			filter_mult = 2.5;
			filter_div 	= 1.3;
			break;
		case HIGH_TOLERANCE:
			limit		= 50.0;
			max_filter	= 800.0;
			min_filter	= 40.0;
			filter_mult = 4.0;
			filter_div 	= 1.1;
			break;
		}	
		
	}
}
