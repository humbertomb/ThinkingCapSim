package tcrob.umu.soccer.gm.data;

import java.util.Random;

import wucore.utils.math.Angles;
import wucore.utils.math.jama.Matrix;

public class EKFLpo {

	protected 	Matrix S;
	private 	Matrix P;
	private		Matrix R;
	
	
	double odolinNoise, odolatNoise, odorotNoise;
	double obsdisNoise, obsangNoise;
	
	public EKFLpo(Matrix S0, double olin, double olan, double oron, double odn, double oan) {
		
		S = new Matrix(2,1);
		P = new Matrix(2,2);
		R = new Matrix(2,2);
		
		S.setMatrix(S0);

		R.set(0,0, Math.pow(odn, 2.0));
		R.set(1,1, Math.pow(oan, 2.0));
	
		P.setMatrix(R);
		
//		System.out.println("Incertidumbre en rho = "+Math.sqrt(P.jacobian().get(0, 0))+"    theta = "+Angles.RTOD*Math.sqrt(P.jacobian().get(0, 1)));


		odolinNoise = olin;
		odolatNoise = olan; 
		odorotNoise = oron;
		obsdisNoise = odn; 
		obsangNoise = oan;

	}
	
	public void reset(Matrix S0) {
		S.setMatrix(S0);
		P.setMatrix(R);
	}
	
	public void predict(Odometry odom) {
		
		double compx, compy;
		double i, j;

		i = -S.get(0, 0)*Math.sin(S.get(1, 0));
		j = S.get(0, 0)*Math.cos(S.get(1, 0));
		
		compx = Math.pow(i+odom.dlat, 2.0);
		compy = Math.pow(j-odom.dlin, 2.0);
			
		S.set(0, 0, Math.sqrt(compx+compy));
		
		S.set(1, 0, Math.atan2(-(i+odom.dlat), j - odom.dlin)-odom.drot);
		
		P = A(odom).times(P).times(A(odom).transpose());

		P = P.plus(W(odom).times(Q(odom)).times(W(odom).transpose()));
	}
	
	public void correct(int rho, float theta) {
		Matrix z, zv, zzv;
		Matrix K;
		
//		Matrix SX;
		
		z 	= new Matrix(2, 1);
		zv	= new Matrix(2, 1);
		zzv	= new Matrix(2, 1);
		K 	= new Matrix(2, 2);
		
//		SX 	= new Matrix(1, 1);
		
		z.set(0, 0, (double) rho);
		z.set(1, 0, (double) theta);
		zv.setMatrix(S);
		
		K = P.times((P.plus(R)).inverse());

//		SX = (z.minus(zv)).transpose().times(P.plus(R)).times((z.minus(zv)));
//		System.out.println("X = "+Math.sqrt(SX.get(0,0)));
		
//		if(Math.sqrt(SX.get(0,0))<600000)
//		{
//			System.out.println("ACCEPTED");
			zzv.set(0,0, z.get(0, 0)-zv.get(0, 0));
			zzv.set(1,0, Angles.radnorm_180(z.get(1, 0)-zv.get(1, 0)));
//			System.out.println("K");
//			K.print(5, 5);
//			System.out.println("z ");
//			K.print(5, 5);
			
			S = S.plus(K.times(zzv));	
			P = Matrix.identity(K.getRowDimension(), K.getColumnDimension()).minus(K).times(P);

			//		}else
//			System.out.println("REJECTED");
	}
	
	protected Matrix A(Odometry odom) {
		Matrix Ar = new Matrix(2,2);
		double a1d, a2d, a2n, a21n, a22n;
		double i, j;
		double rho, theta;
		
		rho 	= S.get(0, 0);
		theta	= S.get(1, 0);
		
		i = -rho * Math.sin(theta);
		j = rho * Math.cos(theta);
				
		a1d = 2.0 * Math.sqrt( Math.pow(i + odom.dlat,2.0) + 
				Math.pow(j - odom.dlin,2.0) );
		a2d = 1.0 + Math.pow( (i+odom.dlat) / 
							  (j-odom.dlin), 2.0);
		a2n = Math.pow(j+odom.dlin, 2.0);
		a21n = (-Math.sin(theta)*(j-odom.dlin)) -
			   (Math.cos(theta)*(i+odom.dlat));
		a22n = (-j*(j-odom.dlin)) +
		   (-i*(i+odom.dlat));
		
		Ar.set(0, 0, ( (-2.0*(i+odom.dlat)*Math.sin(theta)) +
					   (2.0*(j-odom.dlin)*Math.cos(theta)) ) / a1d);
		Ar.set(0, 1, ( (2.0*j*odom.dlat) - (2.0*i*odom.dlin)) / a1d);
					
		Ar.set(1, 0, (a21n/a2n)/a2d);
		Ar.set(1, 1, (a22n/a2n)/a2d);
					   
		return Ar;
	}

	protected Matrix W(Odometry odom) {
		Matrix Wr = new Matrix(2,3);
		double k1, k2d, k2n;
		double i, j;
		double rho, theta;
		double wlin, wlat;
		
		rho 	= S.get(0, 0);
		theta	= S.get(1, 0);

		i = -rho * Math.sin(theta);
		j = rho * Math.cos(theta);
		
		wlin = odom.dlin * odolinNoise;
		wlat = odom.dlat * odolatNoise;
//		wrot = odom.drot * odorotNoise;
		
		k1 = 2.0 * Math.sqrt(Math.pow(i + odom.dlat + wlat, 2.0) + 
				             Math.pow(j - odom.dlin - wlin, 2.0));
		k2d = 1.0 + Math.pow( (i - odom.dlin - wlin), 2.0);
		k2n = Math.pow( (i - odom.dlin - wlin), 2.0);
		
		Wr.set(0, 0, (-2 * (j - odom.dlin - wlin))/k1);
		Wr.set(0, 1, (-2 * (i + odom.dlat + wlat))/k1);
		Wr.set(0, 2, 0);
		Wr.set(1, 0, ((i + odom.dlat + wlat)/k2n)/k2d);
		Wr.set(1, 1, ((1/(i - odom.dlin - wlin) ) /k2d));
		Wr.set(1, 2, -1);
		
		return Wr;
	}

	protected Matrix Q(Odometry odom) {
		Matrix Qr = new Matrix(3,3);
		
		Qr.set(0, 0, Math.pow((odom.dlin*odolinNoise) + 0.1, 2.0));
		Qr.set(1, 1, Math.pow((odom.dlat*odolatNoise) + 0.1, 2.0));
		Qr.set(2, 2, Math.pow((odom.drot*odorotNoise) + 0.1, 2.0));
		
		return Qr;
	}
	
	public double getQuality() {
		double comppos, compor;
		
		comppos = (1000.0 - Math.sqrt(P.jacobian().get(0, 0)))/1000.0;
		if(comppos < 0) comppos = 0;
		compor = (100.0 - (Angles.RTOD * Math.sqrt(P.jacobian().get(0, 1))))/100.0;
		if(compor < 0) compor = 0;
		
		return Math.sqrt((comppos + compor)/2.0);
	}
	
	public void print() {
		
		
		System.out.println("Rho = "+S.get(0, 0)+"  Theta = "+ Angles.degnorm_180(Angles.RTOD*S.get(1, 0))+"");
		
		System.out.print("P = ");
		P.print(5, 5);
		System.out.println("Incertidumbre en rho = "+Math.sqrt(P.jacobian().get(0, 0))+"    theta = "+Angles.RTOD*Math.sqrt(P.jacobian().get(0, 1)));

	}
	
	public int getRho() {
		return (int) S.get(0,0);
	}
	
	public float getTheta() {
		return (float) Angles.radnorm_180(S.get(1,0));
	}
	
	public static int calculateRho(int x, int y) {
		int i, j;
		
		i = 1950;
		j = 0;
		
		return (int) Math.sqrt(Math.pow(i-x, 2.0)+Math.pow(j-y, 2.0));
	}
	
	public static float calculateTheta(int x, int y) {
		return calculateTheta(x, y, (float) (90.0*Angles.DTOR));
	}
	
	public static float calculateTheta(int x, int y, float theta) {
		int i, j;
		
		i = 1950;
		j = 0;
		
		return (float) Angles.radnorm_180(Math.atan2(j-y, i-x)-theta);
	}
	
	public Matrix getP() {
		return P;
	}
	
	static public void main (String[] params)
	{
		Matrix s0 = new Matrix(2,1);
		EKFLpo miekf;
//		int[][] pnts = {{0,-1000}, {0, -500}, {0, 0},{0, 500},{0, 1000}};
		Odometry odoaux = new Odometry();
		int lastx=0, lasty=-1500;
		float lastt;
		Random random = new Random();;
		int rho;
		float theta;
		int newx, newy;
		float newt;
		
		lastx = (int) (random.nextGaussian() * 1000);
		lasty = (int) (random.nextGaussian() * 1000);
		lastt = (float) (90.0 * Angles.DTOR) ;//(float) (random.nextGaussian() * Angles.DTOR * 30.0);
		
		s0.set(0, 0, calculateRho(lastx, lasty));
		s0.set(1, 0, calculateTheta(lastx, lasty, lastt));
				
		miekf = new EKFLpo(s0, 0.3, 0.3, 0.3, 1000.0, Angles.DTOR * 7.0);
		miekf.print();
		
		for(int i=0; i<30;i++) {
			
			odoaux.dlin = (float) (random.nextGaussian() * 300);
			odoaux.dlat = (float) (random.nextGaussian() * 300);		
			odoaux.drot = (float) (random.nextGaussian() * Angles.DTOR * 30.0);
			odoaux.elin = (float) (odoaux.dlin * 0.3);
			odoaux.elat = (float) (odoaux.dlat * 0.3);
			odoaux.erot = (float) (odoaux.drot * 0.3);
			
			newx = (int) (lastx + (odoaux.dlin * Math.cos(lastt)) -  (odoaux.dlat * Math.sin(lastt)));		
			newy = (int) (lasty + (odoaux.dlin * Math.sin(lastt)) +  (odoaux.dlat * Math.cos(lastt)));		
			newt = (float) Angles.radnorm_180(lastt + odoaux.drot);

			odoaux.dlin = (float) (odoaux.dlin + (random.nextGaussian() * 0.3 * odoaux.dlin));
			odoaux.dlat = (float) (odoaux.dlat + (random.nextGaussian() * 0.3 * odoaux.dlat));
			odoaux.drot = (float) (odoaux.drot + (random.nextGaussian() * 0.3 * odoaux.drot));
			
				
			//System.out.println("Robot en "+lastx+","+lasty+","+Angles.RTOD*lastt);
			System.out.println("PREDICT ("+odoaux.dlin+", "+odoaux.dlat+", "+Angles.RTOD*odoaux.drot+")");
			//System.out.println("Robot en "+newx+","+newy+","+Angles.RTOD*newt);
			miekf.predict(odoaux);
			miekf.print();

//			newx = (int) (newx + random.nextGaussian() * 50.0);
//			newy = (int) (newy + random.nextGaussian() * 50.0);
//			newt = (float) (newt + random.nextGaussian() * 2 * Angles.DTOR);

			System.out.println("CORRECT ("+calculateRho(newx, newy)+", "+ Angles.RTOD*calculateTheta(newx, newy, newt)+")");

			rho 	= calculateRho(newx, newy);
			theta 	= calculateTheta(newx, newy, newt);
			
			//System.out.println("(sin ruido) rho = "+rho+"   theta = "+theta*Angles.RTOD);
			
			if(random.nextInt(100)>50) {
				System.out.print("FP: ");
				rho 	= (int) (rho + (random.nextGaussian() * 4000));
				theta 	= (float) Angles.radnorm_180(theta + (random.nextGaussian() * 360 * Angles.DTOR));
			}
			else
			{
				System.out.print("VP: ");

				rho 	= (int) (rho + (random.nextGaussian() * 500));
				theta 	= (float) (theta + (random.nextGaussian() * 5 * Angles.DTOR));
				miekf.correct(rho, theta);
			}
			//System.out.println("(con ruido) rho = "+rho+"   theta = "+theta*Angles.RTOD);

			
			miekf.print();
			//System.out.println("----------------------------------"+(i+1));
			
			lastx = newx;
			lasty = newy;
			lastt = newt;
		}
			
		
		
		
	}
}
