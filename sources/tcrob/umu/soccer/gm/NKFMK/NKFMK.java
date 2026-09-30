/**
 * Created on 19-feb-2007
 *
 * @author Francisco Mart�n Rico
 */
package tcrob.umu.soccer.gm.NKFMK;


import wucore.utils.math.Angles;
import wucore.widgets.*;
import tcrob.umu.soccer.gm.data.*;
import tcrob.umu.soccer.gm.*;
import tcrob.umu.soccer.gm.fmk.GridFMarkov;
import tcrob.umu.soccer.gm.kalman.Kalman;

public class NKFMK implements Localisation
{
	private String ID = new String("NKFMK"); 

	static final int MAX_EKFS = 10;
	int actives = 0;
	
	protected Gs				gs;

	protected Kalman[]			ekf;
	protected GridFMarkov		fmk;
		
	LocLps lastLps;
	
	int _toler;
	double _odolinNoise;
	double _odorotNoise; 
	double _distNoise; 
	double _angleNoise;
	
	public NKFMK (int gsize, double rBlurPosBias, double rBlurAngleBias,
			int toler, double odolinNoise, double odorotNoise, double distNoise, double angleNoise,
			int numberEkfs, int minAge, double chithld, int posDetStrgy, int resetEKF, int resetFMK)
	{
		
		_toler 			= toler;
		_odolinNoise	= odolinNoise;
		_odorotNoise	= odorotNoise; 
		_distNoise		= distNoise; 
		_angleNoise		= angleNoise;
		
		gs	= new Gs ();
		
		ekf = new Kalman[MAX_EKFS];
	
		for(int i=0; i<MAX_EKFS;i++)
			ekf[i] = null;

		ekf[0] 	= new Kalman(toler, odolinNoise, odorotNoise, distNoise, angleNoise);
		fmk 	= new GridFMarkov(gsize, rBlurPosBias, rBlurAngleBias);
						
		GsPosition pos = new GsPosition();
			
		pos.x = 0;
		pos.y = 0;
		pos.theta = 0;
		pos.dx = 4000;
		pos.dy = 6000;
		pos.dtheta = (double) (180.0 * Angles.DTOR);
			
		gs.setPosition(pos);
		ekf[0].initialPosition(pos);
	
		actives++;
	}

	static protected double RAD (double deg)		{ return deg * Angles.DTOR; }
	
	public void updateMotionOnly (Odometry odo)
	{
		fmk.updateMotionOnly(odo);
		for(int i=0; i<MAX_EKFS;i++) 
			if(ekf[i] != null)
				ekf[i].updateMotionOnly(odo);
		
	}
	
	public void updateMotionAndSensors (Odometry odo, LocLps lps)
	{
		double auxvalue;
		double valuechosen = 1.0;
		int chosen = -1;
		
		fmk.updateMotionAndSensors(odo, lps);
		for(int i=0; i<MAX_EKFS;i++) 
			if(ekf[i] != null) {
				ekf[i].updateMotionAndSensors(odo, lps);
		
				System.out.println("->"+i+" x = "+ekf[i].getGs().getX()+ "  y = "+ekf[i].getGs().getY()+" ("+ekf[i].getGs().getQuality()+") ["+fmk.getValueXY(ekf[i].getGs().getX(), ekf[i].getGs().getY())+"]");
				
				auxvalue = fmk.getValueXY(ekf[i].getGs().getX(), ekf[i].getGs().getY());
				
				if((auxvalue < 0.05) && (ekf[i].getGs().getQuality() < 0.5) && (actives > 1)) {
					actives--;
					System.out.println("Eliminado "+i+" ("+auxvalue+", "+ekf[i].getGs().getQuality());
					ekf[i] = null;

				}
			}
		
		for(int i=0; i<MAX_EKFS;i++) 
			for(int j=0; j<MAX_EKFS;j++) 
				if((ekf[i]!=null) && (ekf[j]!=null) && (i!=j) &&
				   (Math.sqrt(Math.pow(ekf[i].getGs().getX()-ekf[j].getGs().getX(),2.0)+
				      	      Math.pow(ekf[i].getGs().getY()-ekf[j].getGs().getY(),2.0)) < 200 ))
					if(ekf[i].getGs().getQuality() > ekf[j].getGs().getQuality()) {
						actives--;
						System.out.println("Eliminado "+j+" (dist="+Math.sqrt(Math.pow(ekf[i].getGs().getX()-ekf[j].getGs().getX(),2.0)+
					      	      Math.pow(ekf[i].getGs().getY()-ekf[j].getGs().getY(),2.0))+", "+ekf[j].getGs().getQuality());

						ekf[j] = null;
					}
					else {
						actives--;
						System.out.println("Eliminado "+i+" (dist="+Math.sqrt(Math.pow(ekf[i].getGs().getX()-ekf[j].getGs().getX(),2.0)+
					      	      Math.pow(ekf[i].getGs().getY()-ekf[j].getGs().getY(),2.0))+", "+ekf[i].getGs().getQuality());
						ekf[i] = null;
					}
						
					
		
		System.out.println("Activos = "+ actives);
		
		if(fmk.getGs().getQuality()<0.9) {
			calculateNewPos();
			
			resetting();
			return;
		}
			
		
		if(actives == 0)
			ekf[0].initialPosition (fmk.getGs().getPosition());
		else if (actives < (MAX_EKFS-1)) {
				boolean nocubre = false;
				
				for(int i=0; i<MAX_EKFS;i++) { 
					if(ekf[i] == null)
						continue;
					
//					System.out.println("Distancia en "+i+" = "+Math.sqrt(Math.pow(ekf[i].getGs().getX()-fmk.getGs().getX(),2.0)+
//				      	      Math.pow(ekf[i].getGs().getY()-fmk.getGs().getY(),2.0)));
					
					if(Math.sqrt(Math.pow(ekf[i].getGs().getX()-fmk.getGs().getX(),2.0)+
					      	      Math.pow(ekf[i].getGs().getY()-fmk.getGs().getY(),2.0))< 1200)
						nocubre = false;
					else {
						System.out.println("*");
						nocubre = true;
						break;
					}
				}
				
				if(!nocubre) {
//					System.out.println("idx = "+idx);
//					System.out.println("No reiniciado al ser dist < 1200 ");//+Math.sqrt(Math.pow(ekf[idx].getGs().getX()-fmk.getGs().getX(),2.0)+
					      	      //Math.pow(ekf[idx].getGs().getY()-fmk.getGs().getY(),2.0)));
				}
				else
					for(int i=0; i<MAX_EKFS;i++)  
						if(ekf[i] == null) { 
							actives++;
							ekf[i] 	= new Kalman(_toler, _odolinNoise, _odorotNoise, _distNoise, _angleNoise);

							GsPosition pos = new GsPosition();

							pos.x = 0;
							pos.y = 0;
							pos.theta = 0;
							pos.dx = 4000;
							pos.dy = 6000;
							pos.dtheta = (double) (180.0 * Angles.DTOR);

							ekf[i].initialPosition (fmk.getGs().getPosition());
							System.out.println("Iniciado en "+i);
							break;
						}
				}
		else {
			for(int i=0; i<MAX_EKFS;i++) 
				if(ekf[i] != null) {
					auxvalue = fmk.getValueXY(ekf[i].getGs().getX(), ekf[i].getGs().getY());
					
					if(auxvalue <=valuechosen) {
						valuechosen = auxvalue;
						chosen = i;
					}
				}
			System.out.println("Reiniciado en "+chosen);
			ekf[chosen].initialPosition (fmk.getGs().getPosition());
		}
		
		calculateNewPos();
		
		resetting();		
	}

	
	
	private void calculateNewPos() {
		
		int idx = 0;
		double qmayor=0.0;
		
		for(int i=0; i<MAX_EKFS;i++) 
			if(ekf[i] != null)
				if(ekf[i].getGs().getQuality() > qmayor) {
					qmayor = ekf[i].getGs().getQuality();
					idx = i;
				}

		gs.setPosition(ekf[idx].getGs().getPosition());
			
	}

	public void drawElements (Model2D model)
	{
		fmk.drawElements(model);
		for(int i=0; i<MAX_EKFS;i++) 
			if(ekf[i] != null)
				ekf[i].drawElements(model);
	}
	
	public boolean getLastUpdated (int index)	{ return true; }

	public void setGT(GsPosition pos) 
	{
		GsPosition minpos = new GsPosition();
		
		minpos.x = pos.x - 10;
		minpos.y = pos.y - 10;
		
		minpos.theta = pos.theta;
		minpos.dx = 10;
		minpos.dy = 10;
		
		minpos.dtheta = (double) RAD(10.0);
		
		gs.setPosition(minpos);
		
	}
	
	
	private void resetting() {

		
	}
	
	public Gs getGs () { 
		return gs; 
	}
	
	public String getId() {
		return ID;
	}

	public void setId(String newid) {
		ID = new String(newid);
	}
}
