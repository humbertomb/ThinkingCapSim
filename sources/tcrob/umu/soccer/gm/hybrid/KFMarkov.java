/**
 * Created on 21-jun-2006
 *
 * @author Humberto Martinez Barbera
 */
package tcrob.umu.soccer.gm.hybrid;

import wucore.widgets.Model2D;
import tcrob.umu.soccer.gm.data.*;
import tcrob.umu.soccer.gm.*;
import tcrob.umu.soccer.gm.fmk.*;
import tcrob.umu.soccer.gm.kalman.*;

public class KFMarkov implements Localisation
{
	private String ID = new String("KFMK"); 

	static public final double		CHI_TEST		= 5.0;			// Matching value for the Chi-square test
	static public final double		CHI2_TEST	= CHI_TEST * CHI_TEST;

	protected GridFMarkov				fmk;
	protected Kalman					kalman;
	
	public KFMarkov (int gsize, double rBlurPosBias, double rBlurAngleBias,
			int toler, double odolinNoise, double odorotNoise, double distNoise, double angleNoise)
	{
		fmk	= new GridFMarkov(gsize, rBlurPosBias, rBlurAngleBias);
		kalman = new Kalman(toler, odolinNoise, odorotNoise, distNoise, angleNoise);
	}

	public Gs getGs ()							{ return kalman.getGs (); }
	public boolean getLastUpdated (int index)		{ return fmk.getLastUpdated (index); }
	public Kalman getKalman ()					{ return kalman; }
	public GridFMarkov getGridFMarkov ()			{ return fmk; }

	public void updateMotionOnly (Odometry odo)
	{
		fmk.updateMotionOnly (odo);
		kalman.updateMotionOnly (odo);
	}
	
	public void updateMotionAndSensors (Odometry odo, LocLps lps)
	{

		double auxvalue=0.0;

		fmk.updateMotionAndSensors (odo, lps);
		kalman.updateMotionAndSensors (odo, lps);
		
//		for(int y = 2700; y>=-2700;y=y-500) {
//			for(int x = 1800; x>=-1800;x=x-500) {
//				auxvalue = fmk.getValueXY(x, y);
//				System.out.print("  "+auxvalue);
//			}
//			System.out.println("");
//		}
		
		auxvalue = fmk.getValueXY(kalman.getGs().getX(), kalman.getGs().getY());

		System.out.println("auxvalue = "+auxvalue+"   dist = "+Math.sqrt(Math.pow(kalman.getGs().getX()-fmk.getGs().getX(),2.0)+
			      Math.pow(kalman.getGs().getY()-fmk.getGs().getY(),2.0)) + "  Q = "+fmk.getGs().getQuality()+" nis:"+kalman.getNISPosition());
		
		if(((auxvalue < 0.05) && 
		   (Math.sqrt(Math.pow(kalman.getGs().getX()-fmk.getGs().getX(),2.0)+
				      Math.pow(kalman.getGs().getY()-fmk.getGs().getY(),2.0))>1200.0) &&
				      (fmk.getGs().getQuality() > 0.9))) {
//			auxpos.x = (fmk.getGs().getX()+kalman.getGs().getX())/2;
//			auxpos.y = (fmk.getGs().getY()+kalman.getGs().getY())/2;
//			auxpos.theta = (fmk.getGs().getTheta()+kalman.getGs().getTheta())/2;
//			auxpos.dx = kalman.getGs().getDX() * 2;
//			auxpos.dy =  kalman.getGs().getDY() * 2;
//			auxpos.dtheta = kalman.getGs().getDTheta() * 2;
			
			System.out.println("RESET");
			kalman.initialPosition (fmk.getGs().getPosition());
		}
	}

	public void initialPosition (GsPosition pos)
	{
		fmk.initialPosition (pos);
		kalman.initialPosition (pos);
	}
	
	public void drawElements (Model2D model)
	{
		fmk.drawElements (model);
		kalman.drawElements (model);
	}

	public void setGT(GsPosition pos) {
		// TODO Auto-generated method stub
		
	}
	public String getId() {
		return ID;
	}

	public void setId(String newid) {
		ID = new String(newid);
	}
}
