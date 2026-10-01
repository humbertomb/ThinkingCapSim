/**
 * Created on 15-jun-2006
 *
 * @author Humberto Martinez Barbera (2006)
 * @author David Herrero Perez (2004)
 * @author Alessandro Saffiotti (2002)
 */

package tcrob.umu.soccer.gm.fmk;

import java.awt.*;

import tcrob.umu.soccer.gm.data.*;
import tcrob.umu.soccer.gm.*;
import wucore.utils.math.*;
import wucore.widgets.*;

public class GridFMarkov implements Localisation
{
	private String ID = new String("FMK"); 
	
	static public final double CoGThreshold				= 0.8;			// use all cells above this to compute CoG of grid
	public double BlurPosBias      		= 0.08;			// always blur position at least by this amount
	public double BlurAngleBias    		= RAD(1.0);		// always blur angle at least by this amount
	public double BlurAngleMax     		= RAD(10.0);	// blur angle at most by this amount

	static public final double MinDisplacement  		= 10.0;			// don't bother updating if motion less than this...
	static public final double MinRotation      		= RAD(1.0);		// ...or this
	public double MaxDisplacement  		= 400.0;		// max uncertainty at this displacement...
	public double MaxRotation			= RAD(60.0);	// ...and this rotation (2003)

	static public final double PI2						= (double) Angles.PI2;
	static public final double PIq						= 0.7853981633;
	static public final double INV_SQRT_2				= 1.0 / (double) Math.sqrt (2.0);
	public final double INV_MAX_DISP				= 1.0 / MaxDisplacement;
	public final double INV_MAX_ROT				= 1.0 / MaxRotation;

	//	 bias to account for mis-identification
	static public final double FUZZY_BIAS 				= 0.01;

	//	 Net uncertainty
	static public final double FUZZY_CORE_WIDTH_NET		= 0.1;
	static public final double FUZZY_SLOPE_WIDTH_NET	= 0.4;
	static public final double FUZZY_ANGLE_WIDTH_NET	= (double)(10.0 * Angles.DTOR);
	static public final double FUZZY_ANGLE_SLOPE_NET	= (double)(20.0 * Angles.DTOR);
	static public final double MIN_ANGLE_WIDTH_NET		= (double)(30.0 * Angles.DTOR);

	//	 Landmark uncertainty
	static public final double FUZZY_CORE_WIDTH_LM		= 0.05;
	static public final double FUZZY_SLOPE_WIDTH_LM		= 0.3;
	static public final double FUZZY_ANGLE_WIDTH_LM		= (double)(10.0 * Angles.DTOR);
	static public final double FUZZY_ANGLE_SLOPE_LM		= (double)(20.0 * Angles.DTOR);

	protected int gwidth, gheight, gtotal;		// World dimensions (grids)
	protected int gsize;						// Size of cell side (mm)
	
	// Fuzzy Markov GridMap
	protected GridCell[]				mMap;
	protected GridCell[]				mTempMap;
	protected GridConstraint[][][]	gridconst;
	protected GridConstraint[][][]	nfgridconst;
	protected int[]					mLastAnchored;
	protected boolean[]				mLastUpdated;
	private PerceptionModel 			perception;
	private GridCell					cell;
	
	protected Gs						gs;
	protected Odometry				motion;
	
	// structuring element for motion blurring
	private double[][]				mSE = new double[3][3];

	public GridFMarkov (int gsize, double rBlurPosBias, double rBlurAngleBias)
	{
		this.gsize	= gsize;		// Size per cell (mm)
		this.BlurPosBias = rBlurPosBias;
		this.BlurAngleBias = rBlurAngleBias;
		
		// Initialise position with uncertainly
		GsPosition	initPos;
		initPos			= new GsPosition ();
		initPos.x		= 0;
		initPos.y		= 0;
		initPos.dx		= 2000;
		initPos.dy		= 2000;	// 1000
		initPos.theta	= (double) (90.0 * Angles.DTOR);
		initPos.dtheta	= (double) (90.0 * Angles.DTOR);
		
		// Initialise odometry information
		motion	= new Odometry ();
		motion.reset ();

		gs = new Gs ();

		// World dimensions (grids)
		gwidth	= TOTAL_X_SIZE/gsize;
		gheight	= TOTAL_Y_SIZE/gsize;
		gtotal	= gwidth * gheight;
				
		mMap		= new GridCell[gtotal];
		mTempMap	= new GridCell[gtotal];
		for (int i = 0; i < gtotal; i++)
		{
			mMap[i]		= new GridCell ();
			mTempMap[i]	= new GridCell ();
		}
		mLastAnchored	= new int[LocLps.LPS_SIZE];
		mLastUpdated		= new boolean[LocLps.LPS_SIZE];
		perception		= new PerceptionModel ();
		cell				= new GridCell();
		
		initialiseContraints ();
		initialPosition (initPos);
		updatePosition ();
	}
	
	
	public GridCell[] getMap ()					{ return mMap; }
	public int getWorldCoordinateX (int index)		{ return ((index - (gwidth >> 1)) * gsize) + (gsize >> 1); }
	public int getWorldCoordinateY (int index)		{ return ((index - (gheight >> 1)) * gsize) + (gsize >> 1); }
	public Gs getGs ()							{ return gs; }
	public int getGridSizeX ()					{ return gwidth; }
	public int getGridSizeY ()					{ return gheight; }
	public int getGridSide ()						{ return gsize; }
	public boolean getLastUpdated (int index)		{ return mLastUpdated[index]; }

	static protected double RAD (double deg)			{ return (double) (deg * Angles.DTOR); }

	public void setBlurSettings(double rBlurPosBias, double rBlurAngleBias)
	{	
		this.BlurPosBias = rBlurPosBias;
		this.BlurAngleBias = rBlurAngleBias;
	}
	
	protected void initialiseContraints ()
	{
		int			i, j, k;
		int			num_marks;
		int			pos;
		int			gx, gy;
		
		num_marks = NUM_MARKS;
		gridconst = new GridConstraint[gwidth][gheight][num_marks];
		for (i = 0; i < gwidth; i++)
			for (j = 0; j < gheight; j++)
				for (k = 0; k < num_marks; k++)
					gridconst[i][j][k] = new GridConstraint ();
		
		for (gx = 0; gx < gwidth; gx++)
			for (gy = 0; gy < gheight; gy++)
			{
				for (pos = 0, i = 0; i < NUM_LANDMARKS; i++, pos++)
					gridconst[gx][gy][pos].setConstraint (gx, gy, getXGridIndex(LM_X[i]), getYGridIndex(LM_Y[i]), gsize);
				
				for (i = 0; i < NUM_NETS; i++, pos++)
					gridconst[gx][gy][pos].setConstraint (gx, gy, getXGridIndex(NET_X[i]), getYGridIndex(NET_Y[i]), gsize);
			}
	}
	
	public void initialPosition (GsPosition ipos)
	{
		int			xstart, xend, ystart, yend;
		int			gx, gy, cxpos, cypos;
		int			pos;
		double		dist, distx, disty;
		double		height;
		
		xstart	= ipos.x - (ipos.dx >> 1);
		xend		= ipos.x + (ipos.dx >> 1);
		ystart	= ipos.y - (ipos.dy >> 1);
		yend		= ipos.y + (ipos.dy >> 1);
		
		cell.set (1.0, (double) ipos.theta, (double) ipos.dtheta, (double) (ipos.dtheta + 40 * Angles.DTOR), GridCell.BIAS);
		for (pos = 0, gx = 0; gx < gwidth; gx++)
			for (gy = 0; gy < gheight; gy++, pos++)
			{
				// Current position in mm
				cxpos = (gx - (gwidth >> 1)) * gsize + (gsize >> 1);
				cypos = (gy - (gheight >> 1)) * gsize + (gsize >> 1);
				
				// Get distances to position "blob" for this cell
				if (cxpos < xstart)
					distx = (double)(xstart - cxpos);
				else if (cxpos > xend)
					distx = (double)(cxpos - xend);
				else
					distx = 0.0;
				
				if (cypos < ystart)
					disty = (double)(ystart - cypos);
				else if (cypos > yend)
					disty = (double)(cypos - yend);
				else
					disty = 0.0;
				
				dist = (double) Math.sqrt((double)(distx * distx + disty * disty));
				
				// Calculate height as a function of distance from position "blob"
				// Slope is such that height reaches bias one
				// half meter away... (hack!!!)
				height = 1.0 - dist * 0.002; 
				
				// And no lower than bias!
				if (height < GridCell.BIAS)
					height = GridCell.BIAS;
				
				cell.setHeight (height);
				
				mMap[pos].set (cell);
			}
	}

	//	___________________________________________________
	//
	//	 Grid indexing
	//	___________________________________________________
	public int getXGridIndex (int xworld)
	{
		int index_x;
		
/*		if (xworld > 0)
			xworld += (gsize >> 1);
		else
			xworld -= (gsize >> 1);
*/		
		index_x = (gwidth >> 1) + (xworld / gsize);
		
		if (index_x < 0)
			return 0;
			
		if (index_x >= gwidth)
			return (gwidth - 1);
		else
			return index_x;
	}

	public int getYGridIndex (int yworld)
	{
		int index_y;
		
/*		if (yworld > 0)
			yworld += (gsize >> 1);
		else
			yworld -= (gsize >> 1);
*/		
		index_y = (gheight >> 1) + (yworld / gsize);
		
		if (index_y < 0)
			return 0;
			
		if (index_y >= gheight)
			return (gheight - 1);
		else
			return index_y;
	}

	public void updateMotionOnly (Odometry odo)
	{
		// Update current LOCAL position
		motion.translate (odo);

		// Update current GLOBAL position
		gs.getPosition().translate (odo);
	}
	
	public void updateMotionAndSensors (Odometry odo, LocLps lps)
	{
		// Update motion
		updateMotionOnly (odo);
		updateMotion (motion);
		motion.reset ();
		
		// Update sensors
		updateSensors (lps);
		
		// Localise
		updatePosition ();
	}

	//	___________________________________________________________________________
	//	 Motion update routine.
	//	 Use mask constructed from odometric info to translate grid
	//	 Do convolution in steps of max 'cellsize' mm
	//	 Also update angles using a 'patched average' (which sometimes creates troubles!) 
	//	___________________________________________________________________________
	protected void updateMotion (Odometry v) 
	{                                                    
		// First do the blurring
		blur (Math.sqrt(v.elin*v.elin + v.elat*v.elat), v.erot);
		
		// And then perform the convolution
		convolution (v.dlin, v.dlat, v.drot);
	}

	//	_______________________________________________________________________________________
	//
	//	 This does the pure blurring thing:
	//	 Add uncertainty in position and angle depending on current velocity
	//	_______________________________________________________________________________________
	private void blur (double rho, double theta)
	{
		// Set up an omnidirectional blurring element
		double blurD, blurA;
		double blur1, blur2;
		int maxX, maxY;
		
		blurD = Math.abs ((double) rho); //* 3; //*INV_MAX_DISP;		// displacement component
		blurA = Math.abs ((double) theta) * 3.0; //*INV_MAX_ROT;		// rotation component
		
		/* Dejo esto de momento, por si hay que volver a lo anterior: 
		blurD = fabs(rho)  *INV_MAX_DISP;		// displacement component
		blurA = fabs(theta)*INV_MAX_ROT;		// rotation component
		printf("blurD = %lf; blurA = %lf\n",blurD,blurA);
		*/
		
		blur1 = blurD + blurA - blurD*blurA;	// 4-neighbors
		if (blur1 < BlurPosBias)
			blur1 = BlurPosBias;
		
		blur2 = blur1 * INV_SQRT_2;	// diagonal neighbors
		
		mSE[0][0] = blur2;	mSE[0][1] = blur1;	mSE[0][2] = blur2;
		mSE[1][0] = blur1;	mSE[1][1] = 1.0;	mSE[1][2] = blur1;
		mSE[2][0] = blur2;	mSE[2][1] = blur1;	mSE[2][2] = blur2;
		
//	#ifdef TraceSE
//		printf("             | %5.2f %5.2f %5.2f |\n",   mSE[0][0], mSE[0][1], mSE[0][2]);
//		printf("Gm: BlurSE = | %5.2f %5.2f %5.2f |\n",   mSE[1][0], mSE[1][1], mSE[1][2]);
//		printf("             | %5.2f %5.2f %5.2f |\n\n", mSE[2][0], mSE[2][1], mSE[2][2]);
//	#endif
		
		// Do the dilation
		maxX = gwidth - 2; // max column to scan
		maxY = gheight - 2; // max row
				
		for(int i = 0; i < gtotal; i++)
			mTempMap[i].set (mMap[i]);
		
		int index_in, index_out;
		int index_cell;
		GridCell out;				// pointers to source and destination grids
		GridCell cell;				// pointer to grid cell for inner loop
		double seVal;					// cache its value
		double val;
		double height, bias;			// cumulative result of dilation
		
		// map row scan (left-to-right in GS)
		for (int x = 0; x < maxX; ++x)
		{
			index_in		= x * gheight;
			index_out	= ((x+1) * gheight) + 1;
			
			// map column scan (bottom-up in GS)
			for (int y = 0; y < maxY; ++y)
			{
				out = mMap[index_out];
				
				height = 0.0;
				bias = 0.0;
				
				// SE row scan
				for (int row = 0; row < 3; ++row)
				{
					index_cell = index_in + row * gheight;
					
					//cell = in + row * gheight;
					// SE column scan
					for (int col = 0; col < 3; ++col)
					{
						cell = mTempMap[index_cell];
						seVal = mSE[row][col];
						val = seVal * cell.getHeight();
						
						if (val > height)
							height = val;
						
						val = seVal * cell.getBias();
						
						if (val > bias)
							bias = val;
						
						++index_cell;
					}
				}
				out.setHeight(height);
				
				if (bias > height)
					out.setBias(height);
				else
					out.setBias(bias);
				
				// blur the angle as well (old angle still in out map)
				out.setCore(out.getCore() + (blurA * BlurAngleMax + BlurAngleBias));
				out.setSupport(out.getSupport() + (blurA * BlurAngleMax * 2.0 + BlurAngleBias));
				
				if (out.getCore () > PI2)
					out.setCore (PI2);
				
				if (out.getSupport () > PI2)
					out.setSupport (PI2);
				
				// next cell
				++index_in;
				++index_out;
			}
		}
	}

	//	_______________________________________________________________________________________
	//
	//	 Motion update routine
	//	 Use mask constructed from odometric info to translate grid
	//	 Do convolution in steps of max 'cellsize' mm
	//	 Also update angles using a 'patched average' (which sometimes creates troubles!) 
	//	_______________________________________________________________________________________
	private void convolution (double dlin, double dlat, double drot)
	{
		// Linear and rotational displacement (Since robot system reference)
		double rho_robot;
		double phi_dispacement;
		
		rho_robot = Math.sqrt(dlin*dlin + dlat*dlat);
		
		if ((dlin == 0.0) && (dlat == 0.0))
			phi_dispacement = 0.0;
		else
			phi_dispacement = Math.atan2(dlat, dlin) + 0.5*drot;
		
		// Current displacement and rotation in robot coordinates
		double dx_robot, dy_robot;
		
		dx_robot = rho_robot * Math.cos(phi_dispacement);
		dy_robot = rho_robot * Math.sin(phi_dispacement);
		
		// For rotate to global coordinates
		double phi_global;
		double sinphi, cosphi;
		
		phi_global	= gs.getPosition().theta + 0.5*drot;
		sinphi		= Math.sin(phi_global);
		cosphi		= Math.cos(phi_global);
		
		double dx_global, dy_global;
		
		dx_global	= dx_robot * cosphi - dy_robot * sinphi;
		dy_global	= dx_robot * sinphi + dy_robot * cosphi;
		
		dx_global = -dx_global;	// we should reflect the SE
		dy_global = -dy_global;	// we reflect the displacement instead...
		
		// Divide global displacement in chunks no larger then cellsize
		int chunkno = 1;
		
		double dx_global_aux, dy_global_aux;
		
		dx_global_aux = dx_global;
		dy_global_aux = dy_global;
		
		double adx  = Math.abs(dx_global_aux);
		double ady  = Math.abs(dy_global_aux);
		double adth = Math.abs(drot);
		
		double maxd = (double)gsize;
		
		double dx, dy;
		double tmp;
		
		while ( (adx  > MinDisplacement) || (ady  > MinDisplacement) || (adth > MinRotation) )
		{
			// still some update to be done
			if (
				(adx > ady)	&&	// X is larger direction
				(adx > maxd))	// and must be split into chunks
			{
				tmp = maxd / adx;
				dx  = (dx_global_aux > 0.0) ? maxd : -maxd;
				dy  = dy_global_aux * tmp;
			} else if (
				(ady > adx)	&& // Y is larger direction
				(ady > maxd))		// and must be split into chunks
			{
				tmp = maxd / ady;
				dx  = dx_global_aux * tmp;
				dy  = (dy_global_aux > 0.0) ? maxd : -maxd;
			} else { // can be done in one step
				dx  = dx_global_aux;
				dy  = dy_global_aux;
			}
			
			dx_global_aux -= dx;			// how much is left to displace after this chunk
			dy_global_aux -= dy;
			adx  = Math.abs(dx_global_aux);
			ady  = Math.abs(dy_global_aux);
			
			// Compute translating structuring element
			// Note: since map is stored bot-up and left-right, the coords of SE are:
			//   +-----+-----+-----+-. Dy
			//   | 0:0 | 0:1 | 0:2 |
			//   +-----+-----+-----+
			//   | 1:0 | 1:1 | 1:2 |
			//   +-----+-----+-----+
			//   | 2:0 | 2:1 | 2:2 |
			//   +-----+-----+-----+
			//   |
			//   v
			//  Dx
			// Below, 'up' means to UP in Gs, ie, postive Y, etc.
			
			int upx, upy, rightx, righty, uprightx, uprighty; 
			double side  = (double)gsize;
			double side2 = side * side;
			
			mSE[0][0] = 0.0; mSE[0][1] = 0.0; mSE[0][2] = 0.0;
			mSE[1][0] = 0.0; mSE[1][1] = 0.0; mSE[1][2] = 0.0;
			mSE[2][0] = 0.0; mSE[2][1] = 0.0; mSE[2][2] = 0.0;
			
			if ((dx > 0.0) && (dy < 0.0))	// motion in 2nd quadrant
			{
//				up		= mSE[1][0];
//				right	= mSE[2][1];
//				upright	= mSE[2][0];
				upx		= 1; 	upy		= 0;
				rightx	= 2;		righty	= 1;
				uprightx	= 2;		uprighty	= 0;	
				dy = -dy;
			} 
			else if ((dx < 0.0) && (dy < 0.0))	// motion in 3rd quadrant
			{
//				up		= mSE[1][0];
//				right	= mSE[0][1];
//				upright	= mSE[0][0];
				upx		= 1; 	upy		= 0;
				rightx	= 0;		righty	= 1;
				uprightx	= 0;		uprighty	= 0;	
				dx = -dx;
				dy = -dy;
			} 
			else if ((dx < 0.0) && (dy > 0.0))	// motion in 4th quadrant
			{
//				up		= mSE[1][2];
//				right	= mSE[0][1];
//				upright	= mSE[0][2];
				upx		= 1; 	upy		= 2;
				rightx	= 0;		righty	= 1;
				uprightx	= 0;		uprighty	= 2;	
				dx = -dx;
			} 
			else
			{	// if motion in 1st quadrant
//				up		= mSE[1][2];
//				right	= mSE[2][1];
//				upright	= mSE[2][2];
				upx		= 1; 	upy		= 2;
				rightx	= 2;		righty	= 1;
				uprightx	= 2;		uprighty	= 2;	
			}
			
			mSE[1][1]				= (double) ((side - dx) * (side - dy) / side2);
			mSE[upx][upy]			= (double) ((side - dx) * dy / side2);
			mSE[rightx][righty]		= (double) ((side - dy) * dx / side2);
			mSE[uprightx][uprighty]	= (double) (dx * dy / side2);
			
			// Do the convolution
			int maxX = gwidth - 2; // max column to scan
			int maxY = gheight - 2; // max row
			
			// copy original map to tmp map
			for(int i = 0; i < gtotal; i++)
				mTempMap[i].set (mMap[i]);
			
			int index_in, index_out;
			int index_cell;
			
			GridCell out;			// pointers to source and destination grids
			GridCell cell;				// pointer to grid cell for inner loop
			double seVal;		// cache its value
			double height, bias;			// cumulative result of convolution
			double center, core, supp;	// for angle convolution
			double oldcenter;
			
			for (int x = 0; x < maxX; ++x)	// map row scan (left-to-right in GS)
			{
				index_in	= x * gheight;
				index_out	= ((x+1) * gheight) + 1;
				
				for (int y = 0; y < maxY; ++y)	// map column scan (bottom-up in GS)
				{
					out = mMap[index_out];
					
					height = 0.0;
					bias = 0.0;
					center = 0.0;
					core = 0.0;
					supp = 0.0;
					
					// old angle is still in out map
					oldcenter = out.getCenter();
					
					for (int row = 0; row < 3; ++row) // SE row scan
					{
						index_cell = index_in + row * gheight;
						
						for (int col = 0; col < 3; ++col) // SE column scan
						{
							cell = mTempMap[index_cell];
							seVal = mSE[row][col];
							if (seVal > 0.0)
							{
								height += seVal * cell.getHeight();
								bias   += seVal * cell.getBias();
								center += seVal * cell.getCenter();
								core   += seVal * cell.getCore();
								supp   += seVal * cell.getSupport();
							}
							
							++index_cell;
						}
					}
					
					// set result in grid
					if (height > 1.0)
						height = 1.0;
					
					if (height < GridCell.BIAS)
						height = FUZZY_BIAS;
					
					out.setHeight(height);
					
					if (bias > height)
						out.setBias(height);
					else
						out.setBias(bias);
					
					// cannot make average of angles, so just take max
					// out.Center = (in + iMaxSE*gheight + jMaxSE).Center;
					// no, just taking max gives systematic errors
					// better to risk an average, and make a sanity check
					while (center > PI2) center -= PI2;
					
					// sanity check
					if (Math.abs(center-oldcenter) > PIq) 
						center = oldcenter;
					
					if (chunkno == 1) // do the rotation
					{
						center += drot;
						adth = 0.0;
						
						if (center > PI2)
							center -= PI2;
							
						if (center < 0.0)
							center += PI2;
					}
					
					// we have our new angle
					out.setCenter(center);
					
					if (core > PI2)
						core = PI2;
					
					out.setCore(core);
					
					if (supp > PI2)
						supp = PI2;
					
					out.setSupport(supp);

					// next cell
					++index_in;
					++index_out;
				}
			}
			
			++chunkno;
			// done with this chunk
		};
	}

	//	_______________________________________________________________________________________
	//
	//	 Perceptual update
	//	_______________________________________________________________________________________
	
	//	 Fuse the information into the grid: For each cell, compute the measurement
	//	 trapezoid from sensor model and intersect it into the existing cell
	protected void updateSensors (LocLps lps)
	{
		LocLpo			lpo;
		
		for (int index = 0; index < LocLps.LPS_SIZE; index++)
			mLastUpdated[index]	= false;	

		// Landmarks
		for (int index = LocLps.INIT_LMS; index < (LocLps.INIT_LMS + LocLps.NUM_LMS); index++)
		{
			lpo = lps.getLpo(index);
						
			if (lpo.last_anchored > mLastAnchored[index])
			{
				mLastAnchored[index]	= lpo.last_anchored;				
				mLastUpdated[index]	= true;	
				
				updateLMGrid (index, lpo);
			}
		}
		
		// Nets
		for (int index = LocLps.INIT_NETS; index < (LocLps.INIT_NETS + LocLps.NUM_NETS); index++)
		{
			lpo = lps.getLpo(index);
			if (lpo.last_anchored > mLastAnchored[index])
			{
				mLastAnchored[index]	= lpo.last_anchored;				
				mLastUpdated[index]	= true;	
				
				updateNetGrid (index, lpo);
			}
		}
	}
	
	private void updateLMGrid (int index, LocLpo objlpo)
	{
		perception.index		= index;
		perception.rho		= objlpo.rho;
		perception.theta		= objlpo.theta;
		
		if (perception.rho < 1500)
		{
			perception.model		= PerceptionModel.DISTANCE_BEARING;			
			perception.dcore		= 0.05 * perception.rho;
			perception.dslope	= 0.6 * perception.rho;			
			perception.acore		= RAD(10.0);
			perception.aslope	= RAD(30.0);
		}
		else if (perception.rho < 2500) 
		{
			perception.model		= PerceptionModel.DISTANCE_BEARING;		
			perception.dcore		= 0.2 * perception.rho;
			perception.dslope	= 0.75 * perception.rho;		
			perception.acore		= RAD(20.0);
			perception.aslope	= RAD(35.0);
		} 
		else 
		{
			perception.model		= PerceptionModel.DISTANCE_BEARING;			
			perception.dcore		= 0.3 * perception.rho;
			perception.dslope	= 0.9 * perception.rho;			
			perception.acore		= RAD(20.0);
			perception.aslope	= RAD(40.0);
		}
		
		updateGrid (perception);
		
//		} else {
//			perception.model	= BEARING_WITH_THRESHOLD;
//			
//			perception.mrho		= 2500;
//			
//			perception.dslope	= 1000;
//			
//			perception.acore	= RAD(10.0);
//			perception.aslope	= RAD(30.0);
//			
//			// Update with bearing model
//			updateGrid(&perception);
//		}
		
//		} else {
//			perception.model	= BEARING_ONLY;
//			
//			//perception.acore  = DEFAULT_ANGLE_WIDTH;
//			
//			perception.acore	= RAD(5.0);
//			perception.aslope	= RAD(30.0);
//			
//			// Update with bearing model
//			updateGrid(&perception);
//		}
		
	}

	private void updateNetGrid (int index, LocLpo objlpo)
	{
		perception.model 	= PerceptionModel.DISTANCE_BEARING;
		perception.index 	= index;
		perception.rho		= objlpo.rho;
		perception.theta		= objlpo.theta;

		if(perception.rho < 3000)
		{
			perception.dcore  = perception.rho * FUZZY_CORE_WIDTH_NET;
			perception.dslope = perception.rho * FUZZY_SLOPE_WIDTH_NET;
		} 
		else 
		{
			perception.dcore  = 500;
			perception.dslope = 800;
		}
		
		// if net is close, we are more uncertain about its angle
		if (perception.rho < DOG_RADIUS)
			perception.acore = (double) (Math.PI * 2.0);
		else
			perception.acore = ((double)NET_WIDTH / (double)perception.rho);
		
		if (perception.acore < MIN_ANGLE_WIDTH_NET)
			perception.acore = MIN_ANGLE_WIDTH_NET;	
		perception.aslope = FUZZY_ANGLE_SLOPE_NET;
		
		updateGrid (perception);
	}

	private void updateGrid (PerceptionModel perception)
	{
		int pos;
		int gx, gy;
		double angle, delta;
		
		for (pos = 0, gx = 0; gx < gwidth; gx++)
			for (gy = 0; gy < gheight; gy++, pos++)
			{
				// Get angle
				
				//System.out.println("("+gx+","+gy+") es "+Angles.RTOD*gridconst[gx][gy][perception.index - 1].getAngle()+"-"+Angles.RTOD*perception.theta);
				
				angle = Angles.radnorm_180 (gridconst[gx][gy][perception.index - 1].getAngle() - perception.theta);
				
				switch (perception.model)
				{
				case PerceptionModel.DISTANCE_BEARING:
					// Get distance
					delta = gridconst[gx][gy][perception.index - 1].getDistance() - perception.rho;
					delta = Math.abs(delta);
					
					// Set value depending on delta value
					if (delta < perception.dcore) // Core. Highest value
						cell.setHeight(1.0);
					else if (delta < (perception.dslope + perception.dcore)) // Slope
						cell.setHeight(1.0 - ((delta - perception.dcore) * 0.9 / perception.dslope));
					else
						cell.setHeight(GridCell.BIAS); // Bias value. Outside fuzzy area
					break;
					
				case PerceptionModel.BEARING_ONLY:
					cell.setHeight(1.0);
					break;
					
				case PerceptionModel.BEARING_WITH_THRESHOLD:
					// Get distance
					delta = gridconst[gx][gy][perception.index - 1].getDistance();
					delta = Math.abs(delta);
					
					if (delta < (perception.mrho - perception.dslope))
						cell.setHeight(GridCell.BIAS);
					else if (delta < perception.mrho)
						cell.setHeight(1.0 - (((1 - GridCell.BIAS)*(perception.mrho - delta))/perception.dslope));
					else
						cell.setHeight(1.0);
					break;						
				}
				
				cell.setBias(GridCell.BIAS);
				cell.setCore(perception.acore);
				cell.setSupport(perception.acore + perception.aslope);
				cell.setCenter(angle);
				
				mMap[pos].intersectionEnveloped(cell);
			}
	}

	//	_______________________________________________________________________________________
	//
	//	 Center of gravity self-location
	//	_______________________________________________________________________________________
	protected void updatePosition ()
	{
		int			i;
		double		highest, bias, current;
		
		// Get the center of gravity for the highest values in the grid.
		// We start with a normalize, by finding highest value
		
		// First pass. Find highest value
		highest = 0.0001;
		bias = 0.0;
		for (i = 0; i < gtotal; i++)
		{
			current = mMap[i].getHeight();
			if (current > highest)
				highest = current;
				
			current = mMap[i].getBias();		
			if (current > bias)
				bias = current;
		}
		
		if (highest > bias)
			gs.setReliability (highest-bias);
		else
			gs.setReliability (0.0);
		
		if (highest <= 0.000101)
		{
			// We have a problem here. All values are (almost) zero.
			// So we will reset all values...
			for(i = 0; i < gtotal; i++)
				mMap[i].clear();
			
			highest = 1.0;
		}
		
		// Second pass. Normalize so that highest value is 1.0
		for (i = 0; i < gtotal; i++)
			mMap[i].normalize (highest);
		
		// compute CoG for the area above a given threshold
		// together with the bounding box of this area
		
		double sumMu    = 0.0;		// possibility degree
		double sumX     = 0.0;		// X index
		double sumY     = 0.0;		// Y index
		
		double minX = (double)gwidth;
		double maxX = 0.0;
		double minY = (double)gheight;
		double maxY = 0.0;
		
		double boundX = (double)gwidth;
		double boundY = (double)gheight;
		
		double		x, y;
		
		for (x = 0.0, i = 0; x < boundX; ++x) {
			for (y = 0.0; y < boundY; ++y, i++)
			{
				GridCell 	cell;

				cell = mMap[i];
				if (cell.getHeight() > CoGThreshold)
				{
					sumMu    += cell.getHeight();
					sumX     += cell.getHeight() * x;
					sumY     += cell.getHeight() * y;
					
					if (x < minX) minX = x;
					if (x > maxX) maxX = x;
					if (y < minY) minY = y;
					if (y > maxY) maxY = y;
				}
				//System.out.print("("+x+","+y+")"+cell.getHeight());
			}
			//System.out.println("");
		}
		
		// this should never happen...
		if (sumMu == 0.0)
			return;
		
		// find indexes of the two cells nearest to the CoG
		int idx1, idx2;
		double w1, w2;
		
		sumX /= sumMu;
		idx1  = (int)sumX;
		idx2  = (idx1 < (gwidth-1)) ? (idx1 + 1) : idx1;
		w2    = sumX - (double)idx1;
		w1    = 1.0 - w2;
		
		int XVal, YVal;
		double angle, angleVariance;
		
		// and make weighted average of their coordinates
		XVal = (int)((((double)getWorldCoordinateX(idx1)) * w1) + (((double)getWorldCoordinateX(idx2)) * w2));
		
		// do the same for Y
		sumY /= sumMu;
		idx1  = (int)sumY;
		idx2  = (idx1 < (gheight-1)) ? (idx1 + 1) : idx1;
		w2    = sumY - (double)idx1;
		w1    = 1.0 - w2;
		
		// and make weighted average of their coordinates
		YVal = (int)((((double)getWorldCoordinateY(idx1)) * w1) + (((double)getWorldCoordinateY(idx2)) * w2));
				
		// for the orientation, just take the one of the closest cell
		idx1 = ((int)(sumX + 0.5) * gheight) + (int)(sumY + 0.5);
		
		angle  = (double) Angles.radnorm_180 (mMap[idx1].getCenter());
		
		// for variance, take width of the alpha-cut at CoGThreshold
		angleVariance = mMap[idx1].getCore() * CoGThreshold + mMap[idx1].getSupport() * (1.0 - CoGThreshold);
		
		// Put values in global space regarding own position
		GsPosition myposition;

		myposition = gs.getPosition();
		
		myposition.x = XVal;
		myposition.y = YVal;
		myposition.theta = angle;
		
		myposition.dx = (int)(maxX - minX + 1.0) * gsize;
		myposition.dy = (int)(maxY - minY + 1.0) * gsize;
		myposition.dtheta = angleVariance;
		
		// make sure we do not set our position outside the field
		// (including nets)
		int fieldMaxX = (TOTAL_X_SIZE >> 1);
		int fieldMaxY = (TOTAL_Y_SIZE >> 1) + NET_DEPTH;
		
		if (myposition.x >  fieldMaxX) myposition.x =  fieldMaxX;
		if (myposition.y >  fieldMaxY) myposition.y =  fieldMaxY;
		if (myposition.x < -fieldMaxX) myposition.x = -fieldMaxX;
		if (myposition.y < -fieldMaxY) myposition.y = -fieldMaxY;
		
		gs.setFocus(1.0 - ((double)(myposition.dx * myposition.dy) / (double)(TOTAL_X_SIZE * TOTAL_Y_SIZE)));
		if (gs.getFocus() < 0.0) gs.setFocus(0.0);
		
		gs.updateQuality ();
	}

	//	_______________________________________________________________________________________
	//
	//	 Reset the map to total ignorance
	//	_______________________________________________________________________________________
	public void clearMap ()
	{
		for(int i = 0; i < gtotal; i++)
			mMap[i].clear ();
		
		gs.setReliability (1.0); 
	}
	
	public void drawElements (Model2D model)
	{
		int			i, j;
		int			color;
		int			offsetx, offsety;
		double		hside;
		double		head;
		
		hside	= gsize * 0.5;
		offsetx	= gwidth / 2;
		offsety	= gheight / 2;
	
		for (i = 0; i < gwidth; i++)
			for (j = 0; j < gheight; j++)
			{
				double		x1, y1, x2, y2;
				
				x1	= (i - offsetx) * gsize;
				y1 	= (j - offsety) * gsize;
				x2	= (i + 1 - offsetx) * gsize;
				y2	= (j + 1 - offsety) * gsize;
				
				head		= mMap[j+i*gheight].getCenter();
				color	= (int) Math.round (255.0 - mMap[j+i*gheight].getHeight() * 255.0);
				color	= Math.min (Math.max (color, 0), 255);
				model.addRawBox (x1, y1, x2, y2, Model2D.FILLED, new Color (color, color, color));
				model.addRawArrow (x1+hside, y1+hside, hside, head, Color.MAGENTA);
			}
	}

	public void setGT(GsPosition pos) {
		// TODO Auto-generated method stub
		
	}

	
	public double getValueXY(int x, int y) {
		int i,j;
		int idx;
		
		i = (int) (((double)x/(double)gsize) + ((double)TOTAL_X_SIZE/(2.0*(double)gsize)));
		j = (int) (((double)y/(double)gsize) + ((double)TOTAL_Y_SIZE/(2.0*(double)gsize)));
		
//		i = (2 * x - TOTAL_X_SIZE)/(2*gsize);
//		j = (2 * y - TOTAL_Y_SIZE)/(2*gsize);
		
		//System.out.println("x = "+x+"  i = ("+x+"/"+gsize+") +"+TOTAL_X_SIZE+" / 2*"+gsize+"="+i);
		//System.out.println("y = "+y+"  j = ("+y+"/"+gsize+") +"+TOTAL_Y_SIZE+" / 2*"+gsize+"="+j);
		
		if(i>(gwidth-1))
			i = gwidth-1;
		if(j>(gheight-1))
			j = gheight-1;
		
		idx = (i * gheight) + j;

		return mMap[idx].getHeight();

	}
	
	
	public String getId() {
		return ID;
	}

	public void setId(String newid) {
		ID = new String(newid);
	}
}
