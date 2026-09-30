package tcrob.umu.soccer.gm.data;

import java.util.Random;

import wucore.utils.math.jama.Matrix;
import wucore.utils.math.Angles;

public class EKFLpos {
	private EKFLpo[] 	lpo;
	private boolean[] 	lpovalid;
	static final int	MAXLPO = 5;	
	
	public EKFLpos(Matrix S0, double olin, double olan, double oron, double odn, double oan) {
		
		lpo 		= new EKFLpo[MAXLPO];
		lpovalid 	= new boolean[MAXLPO];
		
		for(int i=0; i<MAXLPO;i++){
			lpo[i] =  new EKFLpo(S0, olin, olan, oron, odn, oan);
			lpovalid[i] = false;
		}
		lpovalid[0] = true;		
	}
	
	public void predict(Odometry odom) {
		for(int i=0; i<MAXLPO;i++) {
			if(lpovalid[i] && (lpo[i].getQuality()<0.5))
				lpovalid[i] = false;
			if(lpovalid[i])
				lpo[i].predict(odom);
		}
	}
	
	public void correct(int rho, double theta) {
		boolean found = false;
		Matrix S0 = new Matrix(2,1);
		int worst = -1;
		double qworst = 1.0;
		
		for(int i=0; i<MAXLPO;i++){
//			System.out.println("* "+Math.abs(lpo[i].getRho() - rho)+" y "+Angles.RTOD*Math.abs(Angles.radnorm_180(lpo[i].getTheta() - theta)));

			if( lpovalid[i] && 
				(Math.abs(lpo[i].getRho() - rho) < 800) &&
				(Math.abs(Angles.radnorm_180(lpo[i].getTheta() - theta)) < (Angles.DTOR * 30))
				) {
					lpo[i].correct(rho, theta);
					found = true;
				}
		}
		
		if(!found)
		{
//			System.out.println("*********** Buscando alternativa");
			
			for(int i=0; i<MAXLPO;i++)
				if( !lpovalid[i])
					worst = i;
			if(worst == -1)
				for(int i=0; i<MAXLPO;i++)
					if( lpovalid[i] && (lpo[i].getQuality() < qworst)) {
						qworst = lpo[i].getQuality();
						worst = i;
					}
			
			S0.set(0, 0, rho);
			S0.set(1, 0, theta);
			
			lpo[worst].reset(S0);
			lpovalid[worst] = true;
			
		}
		
	}
	
	public int getRho() {
		int best = 0;
		double qbest = 0.0;
		
		for(int i=0; i<MAXLPO;i++)
			if( lpovalid[i] && (lpo[i].getQuality() > qbest) ) {
				qbest = lpo[i].getQuality();
				best = i;
			}
		
		return lpo[best].getRho();
	}

	public Matrix getP() {
		int best = 0;
		double qbest = 0.0;
		
		for(int i=0; i<MAXLPO;i++)
			if( lpovalid[i] && (lpo[i].getQuality() > qbest) ) {
				qbest = lpo[i].getQuality();
				best = i;
			}
		
		return lpo[best].getP();
	}
	
	public double getTheta() {
		int best = 0;
		double qbest = 0.0;
		
		for(int i=0; i<MAXLPO;i++)
			if( lpovalid[i] && (lpo[i].getQuality() > qbest) ) {
				qbest = lpo[i].getQuality();
				best = i;
			}
		
		return lpo[best].getTheta();
	}
	public double getQuality() {
//		int best = 0;
		double qbest = 0.0;
		
		for(int i=0; i<MAXLPO;i++)
			if( lpovalid[i] && (lpo[i].getQuality() > qbest) ) {
				qbest = lpo[i].getQuality();
//				best = i;
			}
		
		return qbest;
	}
	
	public void print() {
//		for(int i=0; i<MAXLPO;i++) {
//			System.out.print("["+i+"]");
//			if(!lpovalid[i])
//				System.out.println("No valid");
//			else
//				System.out.println("Rho = "+lpo[i].getRho()+"    theta = "+ Angles.RTOD*lpo[i].getTheta()+
//						"    Q = "+ lpo[i].getQuality());
//		}
		System.out.println("==> "+getRho()+"    theta = "+ Angles.RTOD*getTheta()+
						"    Q = "+ getQuality());
	}
	
	static public void main (String[] params)
	{
		Matrix s0 = new Matrix(2,1);
		EKFLpos miekf;
		
		Odometry odoaux = new Odometry();
		int lastx=0, lasty=-1500;
		double lastt;
		Random random = new Random();
		int rho;
		double theta;
		int newx, newy;
		double newt;
		
		lastx = (int) (random.nextGaussian() * 1000);
		lasty = (int) (random.nextGaussian() * 1000);
		lastt = (double) (90.0 * Angles.DTOR) ;//(double) (random.nextGaussian() * Angles.DTOR * 30.0);
		
		s0.set(0, 0, EKFLpo.calculateRho(lastx, lasty));
		s0.set(1, 0, EKFLpo.calculateTheta(lastx, lasty, lastt));
				
		miekf = new EKFLpos(s0, 0.3, 0.3, 0.3, 1000.0, Angles.DTOR * 7.0);
		miekf.print();
		
		for(int i=0; i<30;i++) {
			
			odoaux.dlin = (double) (random.nextGaussian() * 300);
			odoaux.dlat = (double) (random.nextGaussian() * 300);		
			odoaux.drot = (double) (random.nextGaussian() * Angles.DTOR * 30.0);
			odoaux.elin = (double) (odoaux.dlin * 0.3);
			odoaux.elat = (double) (odoaux.dlat * 0.3);
			odoaux.erot = (double) (odoaux.drot * 0.3);
			
			newx = (int) (lastx + (odoaux.dlin * Math.cos(lastt)) -  (odoaux.dlat * Math.sin(lastt)));		
			newy = (int) (lasty + (odoaux.dlin * Math.sin(lastt)) +  (odoaux.dlat * Math.cos(lastt)));		
			newt = (double) Angles.radnorm_180(lastt + odoaux.drot);

			odoaux.dlin = (double) (odoaux.dlin + (random.nextGaussian() * 0.3 * odoaux.dlin));
			odoaux.dlat = (double) (odoaux.dlat + (random.nextGaussian() * 0.3 * odoaux.dlat));
			odoaux.drot = (double) (odoaux.drot + (random.nextGaussian() * 0.3 * odoaux.drot));
			
				
			//System.out.println("Robot en "+lastx+","+lasty+","+Angles.RTOD*lastt);
			System.out.println("PREDICT ("+odoaux.dlin+", "+odoaux.dlat+", "+Angles.RTOD*odoaux.drot+")");
			//System.out.println("Robot en "+newx+","+newy+","+Angles.RTOD*newt);
			miekf.predict(odoaux);
			miekf.print();

//			newx = (int) (newx + random.nextGaussian() * 50.0);
//			newy = (int) (newy + random.nextGaussian() * 50.0);
//			newt = (double) (newt + random.nextGaussian() * 2 * Angles.DTOR);

			System.out.println("CORRECT ("+EKFLpo.calculateRho(newx, newy)+", "+ Angles.RTOD*EKFLpo.calculateTheta(newx, newy, newt)+")");

			rho 	= EKFLpo.calculateRho(newx, newy);
			theta 	= EKFLpo.calculateTheta(newx, newy, newt);
			
			System.out.println("(sin ruido) rho = "+rho+"   theta = "+theta*Angles.RTOD);
			
			if(random.nextInt(100)>50) {
				System.out.print("FP: ");
				rho 	= (int) (rho + (random.nextGaussian() * 4000));
				theta 	= (double) Angles.radnorm_180(theta + (random.nextGaussian() * 360 * Angles.DTOR));
				miekf.correct(rho, theta);
			}
			else
			{
				System.out.print("VP: ");

				rho 	= (int) (rho + (random.nextGaussian() * 500));
				theta 	= (double) (theta + (random.nextGaussian() * 5 * Angles.DTOR));
				miekf.correct(rho, theta);
			}
			System.out.println("(con ruido) rho = "+rho+"   theta = "+theta*Angles.RTOD);

			
			miekf.print();
			System.out.println("----------------------------------"+(i+1));
			
			lastx = newx;
			lasty = newy;
			lastt = newt;
		}
			
		
		
		
	}

}
