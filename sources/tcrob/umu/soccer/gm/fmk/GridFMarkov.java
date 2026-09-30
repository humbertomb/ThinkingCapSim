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
	
	static public final float CoGThreshold				= 0.8f;			// use all cells above this to compute CoG of grid
	public float BlurPosBias      		= 0.08f;			// always blur position at least by this amount
	public float BlurAngleBias    		= RAD(1.0f);		// always blur angle at least by this amount
	public float BlurAngleMax     		= RAD(10.0f);	// blur angle at most by this amount

	static public final float MinDisplacement  		= 10.0f;			// don't bother updating if motion less than this...
	static public final float MinRotation      		= RAD(1.0f);		// ...or this
	public float MaxDisplacement  		= 400.0f;		// max uncertainty at this displacement...
	public float MaxRotation			= RAD(60.0f);	// ...and this rotation (2003)

	static public final float PI2						= (float) Angles.PI2;
	static public final float PIq						= 0.7853981633f;
	static public final float INV_SQRT_2				= 1.0f / (float) Math.sqrt (2.0);
	public final float INV_MAX_DISP				= 1.0f / MaxDisplacement;
	public final float INV_MAX_ROT				= 1.0f / MaxRotation;

	//	 bias to account for mis-identification
	static public final float FUZZY_BIAS 				= 0.01f;

	//	 Net uncertainty
	static public final float FUZZY_CORE_WIDTH_NET		= 0.1f;
	static public final float FUZZY_SLOPE_WIDTH_NET	= 0.4f;
	static public final float FUZZY_ANGLE_WIDTH_NET	= (float)(10.0 * Angles.DTOR);
	static public final float FUZZY_ANGLE_SLOPE_NET	= (float)(20.0 * Angles.DTOR);
	static public final float MIN_ANGLE_WIDTH_NET		= (float)(30.0 * Angles.DTOR);

	//	 Landmark uncertainty
	static public final float FUZZY_CORE_WIDTH_LM		= 0.05f;
	static public final float FUZZY_SLOPE_WIDTH_LM		= 0.3f;
	static public final float FUZZY_ANGLE_WIDTH_LM		= (float)(10.0 * Angles.DTOR);
	static public final float FUZZY_ANGLE_SLOPE_LM		= (float)(20.0 * Angles.DTOR);

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
	protected WorldModel				wm;
	protected Odometry				motion;
	
	// structuring element for motion blurring
	private float[][]				mSE = new float[3][3];

	public GridFMarkov (String name, int gsize, float rBlurPosBias, float rBlurAngleBias)
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
		initPos.theta	= (float) (90.0 * Angles.DTOR);
		initPos.dtheta	= (float) (90.0 * Angles.DTOR);
		
		// Initialise odometry information
		motion	= new Odometry ();
		motion.reset ();

//		System.out.println(name + "\n");
		
		wm = new WorldModel (name);
		gs = new Gs ();

		// World dimensions (grids)
		gwidth	= wm.getTotalXSize()/gsize;
		gheight	= wm.getTotalYSize()/gsize;
		gtotal	= gwidth * gheight;
				
		mMap		= new GridCell[gtotal];
		mTempMap	= new GridCell[gtotal];
		for (int i = 0; i < gtotal; i++)
		{
			mMap[i]		= new GridCell ();
			mTempMap[i]	= new GridCell ();
		}
		mLastAnchored	= new int[Lps.LPS_SIZE];
		mLastUpdated		= new boolean[Lps.LPS_SIZE];
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
	public WorldModel getWorldModel ()			{ return wm; }
	public int getGridSizeX ()					{ return gwidth; }
	public int getGridSizeY ()					{ return gheight; }
	public int getGridSide ()						{ return gsize; }
	public boolean getLastUpdated (int index)		{ return mLastUpdated[index]; }

	static protected float RAD (float deg)			{ return (float) (deg * Angles.DTOR); }

	public void setBlurSettings(float rBlurPosBias, float rBlurAngleBias)
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
		ObjectModel	objm;
		
		num_marks = wm.getMarks ();
		gridconst = new GridConstraint[gwidth][gheight][num_marks];
		for (i = 0; i < gwidth; i++)
			for (j = 0; j < gheight; j++)
				for (k = 0; k < num_marks; k++)
					gridconst[i][j][k] = new GridConstraint ();
		
		for (gx = 0; gx < gwidth; gx++)
			for (gy = 0; gy < gheight; gy++)
			{
				for (pos = 0, i = 0; i < wm.getLMNumber(); i++, pos++)
				{
					objm = wm.getLM (i);
					gridconst[gx][gy][pos].setConstraint (gx, gy, getXGridIndex(objm.getPosX()), getYGridIndex(objm.getPosY()), gsize);
				}
				
				for (i = 0; i < wm.getNetNumber(); i++, pos++)
				{
					objm = wm.getNet (i);
					gridconst[gx][gy][pos].setConstraint (gx, gy, getXGridIndex(objm.getPosX()), getYGridIndex(objm.getPosY()), gsize);
				}
			}
	}
	
	public void initialPosition (GsPosition ipos)
	{
		int			xstart, xend, ystart, yend;
		int			gx, gy, cxpos, cypos;
		int			pos;
		float		dist, distx, disty;
		float		height;
		
		xstart	= ipos.x - (ipos.dx >> 1);
		xend		= ipos.x + (ipos.dx >> 1);
		ystart	= ipos.y - (ipos.dy >> 1);
		yend		= ipos.y + (ipos.dy >> 1);
		
		cell.set (1.0f, (float) ipos.theta, (float) ipos.dtheta, (float) (ipos.dtheta + 40 * Angles.DTOR), GridCell.BIAS);
		for (pos = 0, gx = 0; gx < gwidth; gx++)
			for (gy = 0; gy < gheight; gy++, pos++)
			{
				// Current position in mm
				cxpos = (gx - (gwidth >> 1)) * gsize + (gsize >> 1);
				cypos = (gy - (gheight >> 1)) * gsize + (gsize >> 1);
				
				// Get distances to position "blob" for this cell
				if (cxpos < xstart)
					distx = (float)(xstart - cxpos);
				else if (cxpos > xend)
					distx = (float)(cxpos - xend);
				else
					distx = 0.0f;
				
				if (cypos < ystart)
					disty = (float)(ystart - cypos);
				else if (cypos > yend)
					disty = (float)(cypos - yend);
				else
					disty = 0.0f;
				
				dist = (float) Math.sqrt((double)(distx * distx + disty * disty));
				
				// Calculate height as a function of distance from position "blob"
				// Slope is such that height reaches bias one
				// half meter away... (hack!!!)
				height = 1.0f - dist * 0.002f; 
				
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
	
	public void updateMotionAndSensors (Odometry odo, Lps lps)
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
		float blurD, blurA;
		float blur1, blur2;
		int maxX, maxY;
		
		blurD = Math.abs ((float) rho); //* 3; //*INV_MAX_DISP;		// displacement component
		blurA = Math.abs ((float) theta) * 3.0f; //*INV_MAX_ROT;		// rotation component
		
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
		mSE[1][0] = blur1;	mSE[1][1] = 1.0f;	mSE[1][2] = blur1;
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
		float seVal;					// cache its value
		float val;
		float height, bias;			// cumulative result of dilation
		
		// map row scan (left-to-right in GS)
		for (int x = 0; x < maxX; ++x)
		{
			index_in		= x * gheight;
			index_out	= ((x+1) * gheight) + 1;
			
			// map column scan (bottom-up in GS)
			for (int y = 0; y < maxY; ++y)
			{
				out = mMap[index_out];
				
				height = 0.0f;
				bias = 0.0f;
				
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
				out.setSupport(out.getSupport() + (blurA * BlurAngleMax * 2.0f + BlurAngleBias));
				
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
			
			mSE[0][0] = 0.0f; mSE[0][1] = 0.0f; mSE[0][2] = 0.0f;
			mSE[1][0] = 0.0f; mSE[1][1] = 0.0f; mSE[1][2] = 0.0f;
			mSE[2][0] = 0.0f; mSE[2][1] = 0.0f; mSE[2][2] = 0.0f;
			
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
			
			mSE[1][1]				= (float) ((side - dx) * (side - dy) / side2);
			mSE[upx][upy]			= (float) ((side - dx) * dy / side2);
			mSE[rightx][righty]		= (float) ((side - dy) * dx / side2);
			mSE[uprightx][uprighty]	= (float) (dx * dy / side2);
			
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
			float seVal;		// cache its value
			float height, bias;			// cumulative result of convolution
			float center, core, supp;	// for angle convolution
			float oldcenter;
			
			for (int x = 0; x < maxX; ++x)	// map row scan (left-to-right in GS)
			{
				index_in	= x * gheight;
				index_out	= ((x+1) * gheight) + 1;
				
				for (int y = 0; y < maxY; ++y)	// map column scan (bottom-up in GS)
				{
					out = mMap[index_out];
					
					height = 0.0f;
					bias = 0.0f;
					center = 0.0f;
					core = 0.0f;
					supp = 0.0f;
					
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
						height = 1.0f;
					
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
	protected void updateSensors (Lps lps)
	{
		Lpo			lpo;
		
		for (int index = 0; index < Lps.LPS_SIZE; index++)
			mLastUpdated[index]	= false;	

		// Landmarks
		for (int index = Lps.INIT_LMS; index < (Lps.INIT_LMS + Lps.NUM_LMS); index++)
		{
			lpo = lps.getLpo(index);
						
			if (lpo.getLastAnchored() > mLastAnchored[index])
			{
				mLastAnchored[index]	= lpo.getLastAnchored();				
				mLastUpdated[index]	= true;	
				
				updateLMGrid (index, lpo);
			}
		}
		
		// Nets
		for (int index = Lps.INIT_NETS; index < (Lps.INIT_NETS + Lps.NUM_NETS); index++)
		{
			lpo = lps.getLpo(index);
			if (lpo.getLastAnchored() > mLastAnchored[index])
			{
				mLastAnchored[index]	= lpo.getLastAnchored();				
				mLastUpdated[index]	= true;	
				
				updateNetGrid (index, lpo);
			}
		}
	}
	
	private void updateLMGrid (int index, Lpo objlpo)
	{
		perception.index		= index;
		perception.rho		= (float)objlpo.getRho();
		perception.theta		= objlpo.getTheta();
		
		if (perception.rho < 1500)
		{
			perception.model		= PerceptionModel.DISTANCE_BEARING;			
			perception.dcore		= 0.05f * perception.rho;
			perception.dslope	= 0.6f * perception.rho;			
			perception.acore		= RAD(10.0f);
			perception.aslope	= RAD(30.0f);
		}
		else if (perception.rho < 2500) 
		{
			perception.model		= PerceptionModel.DISTANCE_BEARING;		
			perception.dcore		= 0.2f * perception.rho;
			perception.dslope	= 0.75f * perception.rho;		
			perception.acore		= RAD(20.0f);
			perception.aslope	= RAD(35.0f);
		} 
		else 
		{
			perception.model		= PerceptionModel.DISTANCE_BEARING;			
			perception.dcore		= 0.3f * perception.rho;
			perception.dslope	= 0.9f * perception.rho;			
			perception.acore		= RAD(20.0f);
			perception.aslope	= RAD(40.0f);
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

	private void updateNetGrid (int index, Lpo objlpo)
	{
		perception.model 	= PerceptionModel.DISTANCE_BEARING;
		perception.index 	= index;
		perception.rho		= (float)objlpo.getRho();
		perception.theta		= objlpo.getTheta();

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
		if (perception.rho < wm.getDogRadius())
			perception.acore = (float) (Math.PI * 2.0);
		else
			perception.acore = ((float)wm.getNet(0).getWidth() / (float)perception.rho);
		
		if (perception.acore < MIN_ANGLE_WIDTH_NET)
			perception.acore = MIN_ANGLE_WIDTH_NET;	
		perception.aslope = FUZZY_ANGLE_SLOPE_NET;
		
		updateGrid (perception);
	}

	private void updateGrid (PerceptionModel perception)
	{
		int pos;
		int gx, gy;
		float angle, delta;
		
		for (pos = 0, gx = 0; gx < gwidth; gx++)
			for (gy = 0; gy < gheight; gy++, pos++)
			{
				// Get angle
				
				//System.out.println("("+gx+","+gy+") es "+Angles.RTOD*gridconst[gx][gy][perception.index - 1].getAngle()+"-"+Angles.RTOD*perception.theta);
				
				angle = (float) Angles.radnorm_180 (gridconst[gx][gy][perception.index - 1].getAngle() - perception.theta);
				
				switch (perception.model)
				{
				case PerceptionModel.DISTANCE_BEARING:
					// Get distance
					delta = gridconst[gx][gy][perception.index - 1].getDistance() - perception.rho;
					delta = Math.abs(delta);
					
					// Set value depending on delta value
					if (delta < perception.dcore) // Core. Highest value
						cell.setHeight(1.0f);
					else if (delta < (perception.dslope + perception.dcore)) // Slope
						cell.setHeight(1.0f - ((delta - perception.dcore) * 0.9f / perception.dslope));
					else
						cell.setHeight(GridCell.BIAS); // Bias value. Outside fuzzy area
					break;
					
				case PerceptionModel.BEARING_ONLY:
					cell.setHeight(1.0f);
					break;
					
				case PerceptionModel.BEARING_WITH_THRESHOLD:
					// Get distance
					delta = gridconst[gx][gy][perception.index - 1].getDistance();
					delta = Math.abs(delta);
					
					if (delta < (perception.mrho - perception.dslope))
						cell.setHeight(GridCell.BIAS);
					else if (delta < perception.mrho)
						cell.setHeight(1.0f - (((1 - GridCell.BIAS)*(perception.mrho - delta))/perception.dslope));
					else
						cell.setHeight(1.0f);
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
		float		highest, bias, current;
		
		// Get the center of gravity for the highest values in the grid.
		// We start with a normalize, by finding highest value
		
		// First pass. Find highest value
		highest = 0.0001f;
		bias = 0.0f;
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
			gs.setReliability (0.0f);
		
		if (highest <= 0.000101f)
		{
			// We have a problem here. All values are (almost) zero.
			// So we will reset all values...
			for(i = 0; i < gtotal; i++)
				mMap[i].clear();
			
			highest = 1.0f;
		}
		
		// Second pass. Normalize so that highest value is 1.0
		for (i = 0; i < gtotal; i++)
			mMap[i].normalize (highest);
		
		// compute CoG for the area above a given threshold
		// together with the bounding box of this area
		
		float sumMu    = 0.0f;		// possibility degree
		float sumX     = 0.0f;		// X index
		float sumY     = 0.0f;		// Y index
		
		float minX = (float)gwidth;
		float maxX = 0.0f;
		float minY = (float)gheight;
		float maxY = 0.0f;
		
		float boundX = (float)gwidth;
		float boundY = (float)gheight;
		
		float		x, y;
		
		for (x = 0.0f, i = 0; x < boundX; ++x) {
			for (y = 0.0f; y < boundY; ++y, i++)
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
		float w1, w2;
		
		sumX /= sumMu;
		idx1  = (int)sumX;
		idx2  = (idx1 < (gwidth-1)) ? (idx1 + 1) : idx1;
		w2    = sumX - (float)idx1;
		w1    = 1.0f - w2;
		
		int XVal, YVal;
		float angle, angleVariance;
		
		// and make weighted average of their coordinates
		XVal = (int)((((float)getWorldCoordinateX(idx1)) * w1) + (((float)getWorldCoordinateX(idx2)) * w2));
		
		// do the same for Y
		sumY /= sumMu;
		idx1  = (int)sumY;
		idx2  = (idx1 < (gheight-1)) ? (idx1 + 1) : idx1;
		w2    = sumY - (float)idx1;
		w1    = 1.0f - w2;
		
		// and make weighted average of their coordinates
		YVal = (int)((((float)getWorldCoordinateY(idx1)) * w1) + (((float)getWorldCoordinateY(idx2)) * w2));
				
		// for the orientation, just take the one of the closest cell
		idx1 = ((int)(sumX + 0.5) * gheight) + (int)(sumY + 0.5);
		
		angle  = (float) Angles.radnorm_180 (mMap[idx1].getCenter());
		
		// for variance, take width of the alpha-cut at CoGThreshold
		angleVariance = mMap[idx1].getCore() * CoGThreshold + mMap[idx1].getSupport() * (1.0f - CoGThreshold);
		
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
		int fieldMaxX = (wm.getTotalXSize() >> 1);
		int fieldMaxY = (wm.getTotalYSize() >> 1) + wm.getNet(0).getHeight();
		
		if (myposition.x >  fieldMaxX) myposition.x =  fieldMaxX;
		if (myposition.y >  fieldMaxY) myposition.y =  fieldMaxY;
		if (myposition.x < -fieldMaxX) myposition.x = -fieldMaxX;
		if (myposition.y < -fieldMaxY) myposition.y = -fieldMaxY;
		
		gs.setFocus(1.0f - ((float)(myposition.dx * myposition.dy) / (float)(wm.getTotalXSize() * wm.getTotalYSize())));
		if (gs.getFocus() < 0.0) gs.setFocus(0.0f);
		
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
		
		gs.setReliability (1.0f); 
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
				model.addRawArrow (x1+hside, y1+hside, hside, head, Color.GREEN);
			}
	}

	public void setGT(GsPosition pos) {
		// TODO Auto-generated method stub
		
	}

	
	public float getValueXY(int x, int y) {
		int i,j;
		int idx;
		
		i = (int) (((float)x/(float)gsize) + ((float)wm.getTotalXSize()/(2.0*(float)gsize)));
		j = (int) (((float)y/(float)gsize) + ((float)wm.getTotalYSize()/(2.0*(float)gsize)));
		
//		i = (2 * x - wm.getTotalXSize())/(2*gsize);
//		j = (2 * y - wm.getTotalYSize())/(2*gsize);
		
		//System.out.println("x = "+x+"  i = ("+x+"/"+gsize+") +"+wm.getTotalXSize()+" / 2*"+gsize+"="+i);
		//System.out.println("y = "+y+"  j = ("+y+"/"+gsize+") +"+wm.getTotalYSize()+" / 2*"+gsize+"="+j);
		
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
