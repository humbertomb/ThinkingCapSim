/**
 * Created on 26-jun-2006
 *
 * @author Humberto  Martinez Barbera
 */
package tcrob.umu.soccer.gm;

import tcrob.umu.soccer.gm.data.*;
import wucore.widgets.*;

/**
 * A localisation method of the robot on the field of the RoboCup Four-Legged
 * League (2007). The measures of the field (mm) are fixed: the ones of the
 * rules of 2007, the same the world of the simulator has. The origin is the
 * centre of the field, x across it and y along it, the yellow net (Net1) at +y.
 */
public interface Localisation
{
	// Field (mm)
	static public final int		FIELD_X_SIZE	= 3600;						// Width of the field (inner lines)
	static public final int		FIELD_Y_SIZE	= 5400;						// Length of the field (inner lines)
	static public final int		AREA_WIDTH		= 1300;						// Width of the penalty area
	static public final int		AREA_HEIGHT		= 650;						// Depth of the penalty area
	static public final int		CIRCLE_RADIUS	= 180;						// Radius of the centre circle
	static public final int		BORDER_X		= 400;						// Border outside the lines (x), where the robot can be
	static public final int		BORDER_Y		= 500;						// Border outside the lines (y), where the robot can be
	static public final int		TOTAL_X_SIZE	= FIELD_X_SIZE + 2 * BORDER_X;	// Width of the field and its borders
	static public final int		TOTAL_Y_SIZE	= FIELD_Y_SIZE + 2 * BORDER_Y;	// Length of the field and its borders

	// Nets (mm), Net1 and Net2 of the LPS
	static public final int		NUM_NETS		= 2;
	static public final int[]	NET_X			= { 0, 0 };
	static public final int[]	NET_Y			= { 2700, -2700 };
	static public final int		NET_WIDTH		= 800;
	static public final int		NET_DEPTH		= 300;

	// Landmarks (mm), Landmark1 and Landmark2 of the LPS
	static public final int		NUM_LANDMARKS	= 2;
	static public final int[]	LM_X			= { -1900, 1900 };
	static public final int[]	LM_Y			= { 0, 0 };
	static public final int		LM_RADIUS		= 70;

	// Marks the robot localises itself with: the landmarks and then the nets
	static public final int		NUM_MARKS		= NUM_LANDMARKS + NUM_NETS;

	// Robot (mm)
	static public final int		DOG_RADIUS		= 120;

	// Localisation interface
	public Gs getGs ();
	public boolean getLastUpdated (int index);

	public void updateMotionOnly (Odometry odo);
	public void updateMotionAndSensors (Odometry odo, LocLps lps);
	public void drawElements (Model2D model);
	
	public void setGT(GsPosition pos);
	
	public String getId();
	public void setId(String newid);

	// Field tools
	/** The position (mm) of the mark (landmark or net) of an object of the LPS, or null if it is not one. */
	static public int[] markAt (int index)
	{
		if ((index >= LocLps.INIT_LMS) && (index < LocLps.INIT_LMS + LocLps.NUM_LMS))
			return new int[] { LM_X[index - LocLps.INIT_LMS], LM_Y[index - LocLps.INIT_LMS] };
		if ((index >= LocLps.INIT_NETS) && (index < LocLps.INIT_NETS + LocLps.NUM_NETS))
			return new int[] { NET_X[index - LocLps.INIT_NETS], NET_Y[index - LocLps.INIT_NETS] };
		return null;
	}

	/** Whether a position (mm) is on the field, its borders or the nets. */
	static public boolean onField (double x, double y) 
	{
		double		abs_x, abs_y;
		
		abs_x = Math.abs (x);
		abs_y = Math.abs (y);
		
		// in middle of field
		if ((abs_x < TOTAL_X_SIZE * 0.5) && (abs_y < TOTAL_Y_SIZE * 0.5))
			return true;
		
		// in goal
		if ((abs_x < TOTAL_X_SIZE * 0.5) && (abs_y < TOTAL_Y_SIZE * 0.5 + NET_DEPTH))
			return true;
				
		return false;
	}

	/** Moves a position (mm) off the field back into it, offset from its limits. Whether it was moved. */
	static public boolean placeOnField (double[] buf, double offset) 
	{
		double			x, y;
		double			abs_x, abs_y;
		double			clipped_x, clipped_y;
		double			halfXSize, halfYSize, halfXGoal;
		
		x 	= buf[0];
		y	= buf[1];
		
		if (onField (x,y))			return false;
		
		// off field!
		halfXSize	= TOTAL_X_SIZE * 0.5;
		halfYSize	= TOTAL_Y_SIZE * 0.5;
		halfXGoal	= NET_WIDTH * 0.5;
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
			
			if (abs_y > halfYSize + NET_DEPTH) 
			{
				if (y > 0.0)
					clipped_y = halfYSize + NET_DEPTH - offset;
				else
					clipped_y = -halfYSize - NET_DEPTH + offset;
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
