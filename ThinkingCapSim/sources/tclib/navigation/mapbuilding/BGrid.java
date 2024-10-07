/*
 * (c) 2002 David Herrero, Humberto Martinez 
 */
 
package tclib.navigation.mapbuilding;

import tc.vrobot.*;
import tclib.utils.fusion.*;
import tc.shared.lps.lpo.*;

import devices.pos.*;
		
public class BGrid extends Grid
{
	// Constructors
	public BGrid (FusionDesc fdesc, RobotDesc rdesc, int nx, int ny, double h)
	{
		super (fdesc, rdesc, nx, ny, h);
	}
	
	// Instance methods 
	public void set_occupied (int i, int j)
	{
		empty[i][j] 	= FULL;
		occupied[i][j]	= FULL;				
        setFree(i,j, FULL);						
	}
	
	public void set_empty (int i, int j)
	{
		empty[i][j] 	= EMPTY;
		occupied[i][j]	= EMPTY;					
        setFree(i,j, EMPTY);						
	}

	public void set_unknown (int i, int j)
	{
		empty[i][j] 	= EMPTY;
		occupied[i][j]	= EMPTY;					
        setFree(i,j, EMPTY);						
	}

	public void update (Position pos, int s, double dist)
	{
		// This should be implemented 
	}

	public void update (Position pos, LPOSensorScanner scan)
	{
		int				ci, cj;
		double			a, da;
		double			xs, ys, xm, ym;
		SensorPos		s;
		
		a = (fdesc.CONESCAN / 2.0);
		da = fdesc.CONESCAN/(fdesc.RAYSCAN-1);
		
		s	= fdesc.scanfeat;
			
		xs 	= pos.x () + s.rho () * Math.cos (pos.alpha () + s.rho());			
		ys 	= pos.y () + s.rho () * Math.sin (pos.alpha () + s.rho());
			
		for (int j = 0; j < fdesc.RAYSCAN; j++){
					
			if ((scan.range[j] <= 0.0) || (scan.range[j] > MAX_RANGE_LRF))		continue;
				
			if(j < ((fdesc.RAYSCAN-1)/2)){
																																	
				xm	= xs + scan.range[j] * Math.cos ((s.alpha() - a + j*da )  + pos.alpha ());
				ym	= ys + scan.range[j] * Math.sin ((s.alpha() - a + j*da )  + pos.alpha ());
										
			}else if (j==((fdesc.RAYSCAN-1)/2)){
				
				xm	= xs + scan.range[j] * Math.cos (s.alpha ()  + pos.alpha ());
				ym	= ys + scan.range[j] * Math.sin (s.alpha ()  + pos.alpha ());	
								
			}else{
																										
				xm	= xs + scan.range[j] * Math.cos ((s.alpha() + (j-((fdesc.RAYSCAN-1)/2))*da )  + pos.alpha ());
				ym	= ys + scan.range[j] * Math.sin ((s.alpha() + (j-((fdesc.RAYSCAN-1)/2))*da )  + pos.alpha ());			
				
			}	
				
			ci 	= ctog_x (xm);
			cj 	= ctog_y (ym);
			if ((ci < 1) || (cj< 1) || (ci > size_x - 2) || (cj > size_y - 2))
					continue;	
							
			set_occupied (ci, cj);
		}			
	}
}
