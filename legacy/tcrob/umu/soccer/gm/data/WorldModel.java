/**
 * Created on 15-jun-2006
 *
 * @author Humberto Martinez Barbera (2006)
 * @author David Herrero Perez (2004)
 * @author Alessandro Saffiotti (2002)
 */

package tcrob.umu.soccer.gm.data;

import java.io.*;
import java.util.*;

public class WorldModel
{
	// Field Dimensions (mm)
	protected int 			fieldXSize, fieldYSize;
	protected int 			areaWidth, areaHeight;
	protected int 			penalty_dist;
	protected int 			circle_radius;

	private double			halfXSize, halfYSize;
	private double			halfXGoal,  goalDepth;
	
	// Physical Dimensions (mm)
	protected int 			lm_radius;
	protected int 			ball_radius;
	protected int 			dogWidth, dogHeight, dogRadius;
	
	// Border (mm) - This border is included on fuzzy grid map
	protected int 			rborderX, rborderY;
	
	// Nets
	protected int 			n_nets;
	protected ObjectModel[]	nets;
	
	// Landmarks
	protected int			n_lms;
	protected ObjectModel[]	lms;
	
	public WorldModel (String name)
	{
		Properties		props = new Properties ();
		try
		{
			File f = new File (name);
			FileInputStream fd = new FileInputStream (f);
			props.load (fd);
			fd.close ();
		} catch (Exception e) { }

		int				i;
		
		fieldXSize		= new Integer (props.getProperty("FIELDXSIZE")).intValue ();
		fieldYSize		= new Integer (props.getProperty("FIELDYSIZE")).intValue ();
		rborderX			= new Integer (props.getProperty("BORDERX")).intValue ();
		rborderY			= new Integer (props.getProperty("BORDERY")).intValue ();
		
		circle_radius	= new Integer (props.getProperty("CIRCLE_RADIUS")).intValue ();
		penalty_dist		= new Integer (props.getProperty("PENALTY_DIST")).intValue ();
		areaWidth		= new Integer (props.getProperty("AREA_WIDTH")).intValue ();
		areaHeight		= new Integer (props.getProperty("AREA_HEIGHT")).intValue ();

		n_nets			= new Integer (props.getProperty("NUM_NETS")).intValue ();
		nets				= new ObjectModel[n_nets];
		for (i = 0; i < n_nets; i++)
		{
			int			posx, posy;
			int			nwidth, nheight;

			nwidth	= new Integer (props.getProperty("NET_WIDTH")).intValue ();
			nheight	= new Integer (props.getProperty("NET_DEPTH")).intValue ();
			posx		= new Integer (props.getProperty("NETX_"+i)).intValue ();
			posy		= new Integer (props.getProperty("NETY_"+i)).intValue ();

			nets[i]	= new ObjectModel (ObjectModel.NET);
			nets[i].setPosition (posx, posy);
			nets[i].setDimensions (nwidth, nheight);
		}

		n_lms			= new Integer (props.getProperty("NUM_LANDMARKS")).intValue ();
		lm_radius		= new Integer (props.getProperty("LM_RADIUS")).intValue ();
		lms				= new ObjectModel[n_lms];
		for (i = 0; i < n_lms; i++)
		{
			//System.out.println("n1 lm: "+n_lms );
			
			int			posx, posy;
			posx		= new Integer (props.getProperty("LMX_"+i)).intValue ();
			posy		= new Integer (props.getProperty("LMY_"+i)).intValue ();

			lms[i]	= new ObjectModel (ObjectModel.LANDMARK);
			lms[i].setPosition (posx, posy);
		}

		dogRadius		= new Integer (props.getProperty("DOG_RADIUS")).intValue ();
		dogWidth			= new Integer (props.getProperty("DOG_WIDTH")).intValue ();
		dogHeight		= new Integer (props.getProperty("DOG_HEIGHT")).intValue ();
		ball_radius		= new Integer (props.getProperty("BALL_RADIUS")).intValue ();
		
		halfXSize		= fieldXSize * 0.5 + rborderX;
		halfYSize		= fieldYSize * 0.5 + rborderY;
		halfXGoal		= nets[0].getWidth () * 0.5;
		goalDepth		= nets[0].getHeight ();
	}
	
	public int getFieldXSize()	{ return fieldXSize; }
	public int getFieldYSize ()	{ return fieldYSize; }
	
	public int getAreaWidth ()	{ return areaWidth; }
	public int getAreaHeight ()	{ return areaHeight; }
	public int getPenaltyDist ()	{ return penalty_dist; }
	public int getCircleRadius ()	{ return circle_radius; }
	
	public int getBorderX ()		{ return rborderX; }
	public int getBorderY ()		{ return rborderY; }
	
	public int getTotalXSize ()	{ return fieldXSize + 2*rborderX; }
	public int getTotalYSize ()	{ return fieldYSize + 2*rborderY; }
	
	public int getNetNumber ()	{ return n_nets; }
	public int getLMNumber ()		{ return n_lms; }
	
	public int getLMRadius ()		{ return lm_radius; }
	public int getBallRadius ()	{ return ball_radius; }
	public int getDogWidth ()		{ return dogWidth; }
	public int getDogHeight ()	{ return dogHeight; }
	public int getDogRadius ()	{ return dogRadius; }
	
	public int getMarks ()		{ return n_nets + n_lms; }
	
	public ObjectModel getNet (int netid)
	{
		if(netid > (n_nets-1))
			return null;
		
		return nets[netid];
	}

	public ObjectModel getLM (int lmid)
	{
		if(lmid > (n_lms-1))
			return null;
		
		return lms[lmid];
	}
	
	public boolean onField (double x, double y) 
	{
		double		abs_x,abs_y;
		
		abs_x = Math.abs (x);
		abs_y = Math.abs (y);
		
		// in middle of field
		if ((abs_x < halfXSize) && (abs_y < halfYSize))
			return true;
		
		// in goal
		if ((abs_x < halfXSize) && (abs_y < halfYSize + nets[0].height))
			return true;
				
		return false;
	}

	public boolean placeOnField (double[] buf, double offset) 
	{
		double			x, y;
		double			abs_x, abs_y;
		double			clipped_x, clipped_y;
		
		x 	= buf[0];
		y	= buf[1];
		
		if (onField (x,y))			return false;
		
		// off field!
		abs_x 		= Math.abs (x);
		abs_y 		= Math.abs (y);	
		clipped_x	= x;
		clipped_y	= y;
		
		if (abs_y - halfYSize < abs_x - halfXGoal) 
		{
			// in goal			
			if (abs_x > halfXGoal) 
			{
				if (x > 0.0)
					clipped_x = halfXGoal - offset;
				else
					clipped_x = -halfXGoal + offset;
			}
			
			if (abs_y > halfYSize + goalDepth) 
			{
				if (y > 0.0)
					clipped_y = halfYSize + goalDepth - offset;
				else
					clipped_y = -halfYSize - goalDepth + offset;
			}
		}
		else 
		{
			// off ends
			if (abs_x > halfXSize) 
			{
				if (x > 0.0)
					clipped_x = halfXSize - offset;
				else
					clipped_x = -halfXSize + offset;
			}
			
			// off sides
			if (abs_y > halfYSize) 
			{
				if (y > 0.0)
					clipped_y = halfYSize - offset;
				else
					clipped_y = -halfYSize + offset;
			}	
		}
		
		buf[0]	= clipped_x;
		buf[1]	= clipped_y;
		
		return true;
	}
}
