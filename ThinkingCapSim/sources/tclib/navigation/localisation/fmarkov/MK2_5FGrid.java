/*
 * (c) 2002 Alessandro Saffiotti
 * (c) 2004 David Herrero Perez
 *
 *	Fuzzy Markov 2.5 Grid 
 *
 */

package tclib.navigation.localisation.fmarkov;

import tc.shared.world.*;
import wucore.utils.geom.*;
import tclib.navigation.localisation.fmarkov.gui.*;
import tclib.navigation.mapbuilding.lpo.*;

/**
 * @author David Herrero-Perez
 * @version 1.0
 * 
 */
public class MK2_5FGrid {
	// Kinematic Constraints - limit values for consider the movoment
	public static final double MaxDisplacement = 2.0; // max uncertainty at this displacement...
	public static final double MinDisplacement = 0.01; // don't bother updating if motion less than this...
	public static final double MaxRotation = Math.toRadians(60.0); // ...and this rotation
	public static final double MinRotation = Math.toRadians(1.0); // ...or this
	
	// Blur Constraints
	public static final double BlurPosBias = 0.08; // always blur position at least by this amount
	public static final double BlurAngleBias = Math.toRadians(1.0); // always blur angle at least by this amount
	public static final double BlurAngleMax = Math.toRadians(10.0); // blur angle at most by this amount
	
	// Pre-calculate operations - For performing reasons
	public static final double PI2 = 2*Math.PI;
	public static final double PI = Math.PI;
	public static final double one_over_sqrt_2 = 1.0 / Math.sqrt(2);
	public static final double one_over_maxdis = 1.0 / MaxDisplacement;
	public static final double one_over_maxrot = 1.0 / MaxRotation;
	
	// Threshold for compute the grid gravitity centre
	public static final double CoGThreshold = 0.8; // use all cells above this to compute CoG of grid
	
	// World dimesions (m)
	double rwidth, rheight; // World dimensions width-height (m)
	double rborder; // Border dimension (m)
	double rcwidth, rcheight; // Centered world (m)
	
	double maxx, minx, maxy, miny; // Limits of the real world
	double xcenter, ycenter; // Center of real world limits (It is the origin of global system reference)
	
	// World dimensions (grids)
	int gwidth, gheight;
	double gsize; // Pixel size per cell (m)
	
	// A priori known world
	World world;
	
	GSPosition gspos;
	//MKPos myposition;
	
	FMKMap fmkMap; // Belief that the cell is occupied
	GridConst[][][] gridcons;
	
	// Constructors
	public MK2_5FGrid(
		World world, // A priori known world
		double rborder, // Border component size (m)
		double gsize // Desired pixelation (m)
	) {
		this.world = world;
		this.rborder = rborder;
		this.gsize = gsize;
		
		initKnownWorld(world);
		
		gwidth = (int) ((rwidth + rborder) / gsize);
		gheight = (int) ((rheight + rborder) / gsize);
		
		gridcons = new GridConst[gwidth][gheight][world.walls().edges().length];
		initGridConst();
		
		initilizeMKGrid();
	}
	
	// Initialise methods
	private void initKnownWorld(World world)
	{
		WMWalls wmwalls = world.walls();
//		WMWall[] wmwall = wmwalls.edges();
		
		maxx = wmwalls.maxx();
		minx = wmwalls.minx();
		maxy = wmwalls.maxy();
		miny = wmwalls.miny();
		
		xcenter = minx + (maxx - minx) * 0.5;
		ycenter = miny + (maxy - miny) * 0.5;
		
		rwidth = maxx - minx;
		rheight = maxy - miny;
		
		rcwidth = minx + rwidth * 0.5;
		rcheight = miny + rheight * 0.5;
	}
	
	public void initilizeMKGrid()
	{
		gspos = new GSPosition();
		//myposition = new MKPos(); // Set initial start position and uncertainty
		
		fmkMap = new FMKMap(gwidth, gheight);
	}
	
	private void initGridConst()
	{
		double rho_edge, theta_edge;
		double rx, ry;
		double rho, theta;
		double rho_aux;
		
		Line2 line_edge;
		WMWall[] wmwall;
		
		wmwall = world.walls().edges();
		
		for(int i = 0; i < wmwall.length; i++)
		{
			line_edge = wmwall[i].edge;
			
			rho_edge = line_edge.rho();
			theta_edge = line_edge.theta();
			
			//System.out.println("Line " + i + "   Xo: " + line2.orig().x() + " Yo: " + line2.orig().y() + " Xd: " + line2.dest().x() + " Yd: " + line2.dest().y());
			//System.out.println(" Rho: " + line2.rho() + " m Theta: " + Math.toDegrees(line2.theta()) + " degrees");
			
			for (int gx = 0; gx < gwidth; gx++)
				for (int gy = 0; gy < gheight; gy++)
				{
					rx = getGridRealPosX(gx);
					ry = getGridRealPosY(gy);
					///*
					if(!checkConst(i, wmwall, rx, ry))
					{
						gridcons[gx][gy][i] = new GridConst();
						continue;
					}
					//*/
					rho_aux = (rx * Math.cos(theta_edge) + ry*Math.sin(theta_edge)) - rho_edge;
					
					rho = Math.abs(rho_aux);
					
					if(rho_aux <= 0.0)
						theta = theta_edge;
					else
						theta = theta_edge + Math.PI;
					
					gridcons[gx][gy][i] = new GridConst(rho, theta, true);
				}
		}
	}
	
	// Accessor methods
	public F2_5Cell getCell(int gx, int gy)
	{
		return fmkMap.getCell(gx, gy);
	}
	
	public MKPos getMKPos()
	{
		return gspos.getPosition();
	}
	
	public void setInfoPanel(InforPanel infopanel)
	{
		gspos.setInfoPanel(infopanel);
	}
	
	public int getGridWidth()
	{
		return gwidth;
	}
	
	public int getGridHeight()
	{
		return gheight;
	}
	
	public double getCellSize()
	{
		return gsize;
	}
	
	public World getWorld()
	{
		return world;
	}
	
	public double getWidth()
	{
		return rwidth;
	}
	
	public double getHeight()
	{
		return rheight;
	}
	
	public double getBorder()
	{
		return rborder;
	}
	
	public double getCenterWidth()
	{
		return rcwidth;
	}
	
	public double getCenterHeight()
	{
		return rcheight;
	}
	
	public double getXCenterWorld()
	{
		return xcenter;
	}
	
	public double getYCenterWorld()
	{
		return ycenter;
	}
	
	// Transformations
	// Return the real world coordinate
	// input: grid index
	protected double getGridRealPosX(int i)
	{
		return getGridCenterPosX(i) + rcwidth;
	}
	protected double getGridRealPosY(int j)
	{
		return getGridCenterPosY(j) + rcheight;
	}
	// Return the real world coordinate respect to a 
	// system reference in the center of the grid
	//	input: grid index
	protected double getGridCenterPosX(int i)
	{
		return (((double) (i - (gwidth >> 1))) + 0.5) * gsize;
	}
	protected double getGridCenterPosY(int j)
	{
		return (((double) (j - (gheight >> 1))) + 0.5) * gsize;
	}
	
	// Others
	public void clearGrid()
	{
		fmkMap.clearGrid(); // .resetGrid();
		gspos.clear();
	}
	
	private void checkInsidePosition()
	{
		MKPos mypos = gspos.getPosition();
		
		if(mypos.getX() > maxx)	mypos.setX(maxx);
		if(mypos.getX() < minx)	mypos.setX(minx);
		if(mypos.getY() > maxy)	mypos.setY(maxy);
		if(mypos.getY() < miny)	mypos.setY(miny);
	}
	
	private boolean checkConst(int index, WMWall[] wmwall, double rx, double ry)
	{
		Line2 line_edge;
//		Line2 line_ray;
		
		double x1, y1, x2, y2;
		double dx, dy;
		double cell_size;
		
		cell_size = gsize;
		line_edge = wmwall[index].edge;
		
		x1 = line_edge.orig().x();
		y1 = line_edge.orig().y();
		x2 = line_edge.dest().x();
		y2 = line_edge.dest().y();
		
		dx = x2 - x1;
		dy = y2 - y1;
		
		if(checkConst(index, wmwall, new Line2(rx, ry, x1, y1)))
			return true;
		
		if (Math.abs(dx) > Math.abs(dy)) // slope < 1
		{
			double m = dy / dx;	// compute slope
			double b = y1 - m*x1;
			
			dx = (dx < 0) ? -cell_size : cell_size;
			
			if (dx < 0)
			{
				while (x1 >= x2)
				{
					x1 += dx;
					y1 = (double)Math.round(m*x1 + b);
				
					if(checkConst(index, wmwall, new Line2(rx, ry, x1, y1)))
						return true;
				}	
			} else {
				while (x1 <= x2)
				{
					x1 += dx;
					y1 = (double)Math.round(m*x1 + b);
				
					if(checkConst(index, wmwall, new Line2(rx, ry, x1, y1)))
						return true;
				}	
			}
		} else if (dy != 0) { // slope >= 1
			double m = dx / dy;	// compute slope
			double b = x1 - m*y1;
			dy = (dy < 0) ? -cell_size : cell_size;
			
			if (dy < 0)
			{
				while (y1 >= y2)
				{
					y1 += dy;
					x1 = (double)Math.round(m*y1 + b);
				
					if(checkConst(index, wmwall, new Line2(rx, ry, x1, y1)))
						return true;
				}	
			} else {
				while (y1 <= y2)
				{
					y1 += dy;
					x1 = (double)Math.round(m*y1 + b);
				
					if(checkConst(index, wmwall, new Line2(rx, ry, x1, y1)))
						return true;
				}					
			}
		}
		
		return false;
	}
	
	private boolean checkConst(int index, WMWall[] wmwall, Line2 line_ray)
	{
		Line2 line_other;
		
//		lenght = line_ray.lenght();
		//System.out.println(line_ray.toString());
		
		for(int j = 0; j < wmwall.length; j++)
		{
			if(j == index) continue;
			
			line_other = wmwall[j].edge;
			
			Point2 p = line_ray.intersection(line_other);
			
			if(p != null) 
				return false;
		}
		
		return true;
	}
	
	public void introducePerceptions(LPOFSegments segslrf)
	{
		for (int i = 0; i < segslrf.numseg(); i++)
			introduceFSegmentPerceptions(segslrf.segment(i));
		
		//introduceFSegmentPerceptions(segslrf.segment(0));
		
		/*
		System.out.println(" Number of Fuzzy Segments: " + segslrf.numseg());
		
		for (int i = 0; i < segslrf.numseg(); i++)
		{
			LPOFSegment seglrf = segslrf.segment(i);
			System.out.println(seglrf.toString());
		}
		System.out.println("");
		*/
	}
	
	public void introduceFSegmentPerceptions(LPOFSegment seglrf)
	{
		double rho_percept;
		double theta_percept;
		
		double rho_core;
		double rho_slope;
		
		double theta_core;
		double theta_slope;
		
		F2_5Cell f_global_cell; // Global Fuzzy Set
		F2_5Cell f_local_cell; // Local Percepted Fuzzy Set
		
		rho_percept	= seglrf.rho();
		theta_percept = seglrf.phi();
		
		rho_core = seglrf.v1_rho() + gsize;
		rho_slope = seglrf.v0_rho() + 2*gsize;
		
		theta_core		= F2_5Cell.DEFAULT_ANGLE_WIDTH;
		theta_slope		= F2_5Cell.DEFAULT_ANGLE_SLOPE;
		
		f_local_cell = new F2_5Cell();
		
		boolean first_time, isinited;
		
		for (int gy = 0; gy < gheight; gy++)
			for (int gx = 0; gx < gwidth; gx++)
			{
				double rho_delta;
				double cell_height;
				//double cell_height_aux;
				
				double angle_center;
				
				first_time = true;
				isinited = false;
				
				f_global_cell = getCell(gx, gy);
				
				WMWall[] wmwall;
				
				wmwall = world.walls().edges();
				
				//cell_height = 0.0;
				
				//int wall = 1;
				// We introduce the certainly possibility to the observed grid map
				//for(int wall = 0; wall < 2; wall++)
				for(int wall = 0; wall < world.walls().edges().length; wall++)
				{
					if(wmwall[wall].edge.lenght() < 2.0)
						continue;
					
					if(gridcons[gx][gy][wall].IsPossible())
					{
						rho_delta = Math.abs(gridcons[gx][gy][wall].Dist() - rho_percept);
						
						if(rho_delta < rho_core)
							cell_height = F2_5Cell.FULL;
						else if (rho_delta < rho_slope)
							cell_height = 1.0 - (rho_delta - rho_core) * 0.9 / rho_slope;
						else
							cell_height = F2_5Cell.BIAS;
						
						isinited = true;
						
						//if(cell_height_aux > cell_height)
						//	cell_height = cell_height_aux;
						
						angle_center = gridcons[gx][gy][wall].Angle() - theta_percept;
						
						// Normalize to -180 - 180 degrees
						while (angle_center > (Math.PI)) angle_center -= (2*Math.PI);
						while (angle_center <= (-Math.PI)) angle_center += (2*Math.PI);
						
						// Normalize to 0 - 360 degrees
						//while (angle_center < 0.0) angle_center += PI2;
						//while (angle_center >= PI2)	angle_center -= PI2;
						
						if(first_time)
						{
							f_local_cell.set(cell_height, angle_center, theta_core, theta_core + theta_slope, F2_5Cell.BIAS);
							//f_local_cell.setHeight(cell_height);
							//f_local_cell.setBias(F2_5Cell.BIAS);
							//f_local_cell.setCore(theta_core);
							//f_local_cell.setSupport(theta_core + theta_slope);
							//f_local_cell.setCenter(angle_center);
							
							first_time = false;
						} else {
							//if (cell_height > F2_5Cell.BIAS)
							f_local_cell.union(new F2_5Cell(cell_height, angle_center, theta_core, theta_core + theta_slope, F2_5Cell.BIAS));
						}
					}
				}
				
				// I have to execute the intersection here, I'm displaying the percepted information in this moment
				if(isinited)
					f_global_cell.intersectionEnveloped(f_local_cell);
					//f_global_cell.set(f_local_cell);
			}
	}
	
	public void introducePerceptions(double rho_percept, double theta_percept)
	{
		double rho_core;
		double rho_slope;
		
		double theta_core;
		double theta_slope;
		
		F2_5Cell f_global_cell; // Global Fuzzy Set
		F2_5Cell f_local_cell; // Local Percepted Fuzzy Set
		
		rho_core	= gsize;
		rho_slope	= 2*gsize;
		
		theta_core	= F2_5Cell.DEFAULT_ANGLE_WIDTH;
		theta_slope	= F2_5Cell.DEFAULT_ANGLE_SLOPE;
		
		f_local_cell = new F2_5Cell();
		
		boolean first_time, isinited;
		
		for (int gy = 0; gy < gheight; gy++)
			for (int gx = 0; gx < gwidth; gx++)
			{
				double rho_delta;
				double cell_height;
				//double cell_height_aux;
				
				double angle_center;
				
				first_time = true;
				isinited = false;
				
				f_global_cell = getCell(gx, gy);
				
				//cell_height = 0.0;
				
				//int wall = 3;
				// We introduce the certainly possibility to the observed grid map
				//for(int wall = 0; wall < 2; wall++)
				for(int wall = 0; wall < world.walls().edges().length; wall++)
				{
					if(gridcons[gx][gy][wall].IsPossible())
					{
						rho_delta = Math.abs(gridcons[gx][gy][wall].Dist() - rho_percept);
						
						if(rho_delta < rho_core)
							cell_height = F2_5Cell.FULL;
						else if (rho_delta < rho_slope)
							cell_height = 1.0 - (rho_delta - rho_core) * 0.9 / rho_slope;
						else
							cell_height = F2_5Cell.BIAS;
						
						isinited = true;
						
						//if(cell_height_aux > cell_height)
						//	cell_height = cell_height_aux;
						
						angle_center = gridcons[gx][gy][wall].Angle() - theta_percept;
						
						// Normalize to -180 - 180 degrees
						while (angle_center > (Math.PI)) angle_center -= (2*Math.PI);
						while (angle_center <= (-Math.PI)) angle_center += (2*Math.PI);
						
						// Normalize to 0 - 360 degrees
						//while (angle_center < 0.0) angle_center += PI2;
						//while (angle_center >= PI2)	angle_center -= PI2;
						
						if(first_time)
						{
							f_local_cell.set(cell_height, angle_center, theta_core, theta_core + theta_slope, F2_5Cell.BIAS);
							//f_local_cell.setHeight(cell_height);
							//f_local_cell.setBias(F2_5Cell.BIAS);
							//f_local_cell.setCore(theta_core);
							//f_local_cell.setSupport(theta_core + theta_slope);
							//f_local_cell.setCenter(angle_center);
							
							first_time = false;
						} else {
							//if (cell_height > F2_5Cell.BIAS)
							f_local_cell.union(new F2_5Cell(cell_height, angle_center, theta_core, theta_core + theta_slope, F2_5Cell.BIAS));
						}
					}
				}
				
				// I have to execute the intersection here, I'm displaying the percepted information in this moment
				if(isinited)
					f_global_cell.intersectionEnveloped(f_local_cell);
					//f_global_cell.set(f_local_cell);
			}
	}
	
	/**
	 * 
	 * @author asaffiotti
	 * 
	 * Motion update routine.
	 * Use mask constructed from odometric info to translate grid
	 * Do convolution in steps of max 'cellsize' mm
	 * Also update angles using a 'patched average' (which sometimes creates troubles!) 
	 * 
	 */
	public void convolve(double dlin, double dlat, double drot)
	{
		// First do the blurring
		Blur(Math.sqrt(dlin*dlin + dlat*dlat), drot);
		
		// And execute the convolution
		convolution(dlin, dlat, drot);
	}
	
	public void convolutionDog(double dlin, double dlat, double drot)
	{
		boolean ConvolveAngles = true;
		
		// Element for motion blurring
		ConvolutionElement motionelement = new ConvolutionElement();
		
		// Compute direction and amount of motion in GS coordinates
		
		// Linear and rotational displacement (Since robot system reference)
		double rho_robot;
		double phi_dispacement;
		
		rho_robot = Math.sqrt(dlin*dlin + dlat*dlat);
		
		if ((dlin == 0.0) && (dlat == 0.0))
			phi_dispacement = 0.0;
		else
			phi_dispacement = Math.atan2(dlat, dlin) + 0.5*drot;
		
		// Actual displacement in robot coordinates
		double dx_robot, dy_robot;
		
		dx_robot	= rho_robot*Math.cos(phi_dispacement);
		dy_robot	= rho_robot*Math.sin(phi_dispacement);
		
		// For rotate to global coordinates
		double phi_global;
		double sinphi, cosphi;
		
		phi_global	= gspos.getPosition().getTheta() + 0.5*drot;
		sinphi		= Math.sin(phi_global);
		cosphi		= Math.cos(phi_global);
		
		double dx_global, dy_global;
		
		dx_global	= dx_robot*cosphi - dy_robot*sinphi;
		dy_global	= dx_robot*sinphi + dy_robot*cosphi;
		
///*
		// Divide global displacement in chunks no larger then cellsize
		int chunkno = 1;
		
		double dx_global_aux, dy_global_aux;
		
		dx_global_aux = dx_global;
		dy_global_aux = dy_global;
		
		double adx  = Math.abs(dx_global_aux);
		double ady  = Math.abs(dy_global_aux);
		double adth = Math.abs(drot);
		
		double dx, dy;
		double tmp;
		
		while (
			(adx  > MinDisplacement) ||
			(ady  > MinDisplacement) ||
			(adth > MinRotation))
		{
			// still some update to be done
			if ( 
				(adx > ady) && // X is larger direction
				(adx > gsize) // and must be split into chunks
			) {
				tmp = gsize / adx;
				dx = (dx_global_aux > 0.0) ? gsize : -gsize;
				//dy = (Math.abs(dy_global_aux) < 0.0001) ? 0.0 : dy_global_aux * tmp;
				dy = dy_global_aux * tmp;
				//System.out.println("Case 1: dx " + dx + " > dy " + dy);
			} else if (
				(ady > adx) && // Y is larger direction
				(ady > gsize) // and must be split into chunks
			) {
				tmp = gsize / ady;
				//dx = (Math.abs(dx_global_aux) < 0.0001) ? 0.0 : dx_global_aux * tmp;
				dx  = dx_global_aux * tmp;
				dy  = (dy_global_aux > 0.0) ? gsize : -gsize;
				//System.out.println("Case 2: dy " + dy + " > dx " + dx);
			} else { // can be done in one step
				dx  = dx_global_aux;
				dy  = dy_global_aux;
				//System.out.println("Case 3: dx " + dx + " dy " + dy);
			}
			
			// how much is left to displace after this chunk
			dx_global_aux -= dx;
			dy_global_aux -= dy;
			adx = Math.abs(dx_global_aux);
			ady = Math.abs(dy_global_aux);
			
			// Compute translating element
			// Note: since map is stored bot-up and left-right, the coords of SE are:
			//  +-----+-----+-----+--> Dx
			//   | 0:0  | 0:1  | 0:2 |
			//  +-----+-----+-----+
			//   | 1:0  | 1:1  | 1:2 |
			//  +-----+-----+-----+
			//   | 2:0  | 2:1  | 2:2 |
			//  +-----+-----+-----+
			//   |
			//   v
			//  Dy
			// Below, 'up' means to UP in Gs, ie, postive Y, etc.
			
			double side = gsize;
			double side2 = side * side;
			
			if ((dx >= 0.0) && (dy < 0.0)) // motion in 2nd quadrant
			{
				dy = -dy;
				
				//System.out.println("Motion in 2nd quadrant");
				
				motionelement.set(2, 1, (side - dx) * dy / side2);
				motionelement.set(1, 0, (side - dy) * dx / side2);
				motionelement.set(2, 0, dx * dy / side2);
			} else if ((dx < 0.0) && (dy < 0.0)) { // motion in 3rd quadrant
				dx = -dx;
				dy = -dy;
				
				//System.out.println("Motion in 3rd quadrant");
				
				motionelement.set(2, 1, (side - dx) * dy / side2);
				motionelement.set(1, 2, (side - dy) * dx / side2);
				motionelement.set(2, 2, dx * dy / side2);
			} else if ((dx < 0.0) && (dy >= 0.0)) { // motion in 4th quadrant
				dx = -dx;
				
				//System.out.println("Motion in 4th quadrant");
				
				motionelement.set(0, 1, (side - dx) * dy / side2);
				motionelement.set(1, 2, (side - dy) * dx / side2);
				motionelement.set(0, 2, dx * dy / side2);
			} else { // if motion in 1st quadrant ((dx > 0.0) && (dy > 0.0))
				
				//System.out.println("Motion in 1st quadrant");
				
				motionelement.set(0, 1, (side - dx) * dy / side2);
				motionelement.set(1, 0, (side - dy) * dx / side2);
				motionelement.set(0, 0, dx * dy / side2);
			}
			
			motionelement.set(1, 1, ((side - dx) * (side - dy)) / side2);
			
			//System.out.println(motionelement.toString());
			
			// Do the convolution
			
			FMKMap fmkMap_temp;
			fmkMap_temp = (FMKMap)fmkMap.clone();
			
			F2_5Cell out;
			F2_5Cell cell; // grid cell for inner loop
			double height, bias; // cumulative result of convolution
			double center, core, supp; // for angle convolution
			double oldcenter;
			double seVal;
			
			int maxX = gwidth - 2; // Max row
			int maxY = gheight - 2; // Max column
			
			for (int x = 0; x < maxX; ++x) // map row scan (left-to-right in GS)
				for (int y = 0; y < maxY; ++y) // map column scan (bottom-up in GS)
				{
					out = fmkMap.getCell(x + 1, y + 1);
					
					height = 0.0;
					bias = 0.0;
					center = 0.0;
					core = 0.0;
					supp = 0.0;
					oldcenter = out.getCenter(); // old angle is still in out map
					
					for (int i = 0; i < 3; ++i) // SE row scan
						for (int j = 0; j < 3; ++j) // SE column scan
						{
							seVal = motionelement.get(j, i);
							if (seVal > 0.0)
							{
								cell = fmkMap_temp.getCell(x + i, y + j);
								
								height += seVal * cell.getHeight();
								bias += seVal * cell.getBias();
								
								if(ConvolveAngles)
								{
									center += seVal * cell.getCenter();
									core += seVal * cell.getCore();
									supp += seVal * cell.getSupport();
								}
							}
						}
					
					// set result in grid
					if (height > 1.0) height = 1.0;
					if (height < F2_5Cell.BIAS) height = F2_5Cell.BIAS;
					out.setHeight(height);
					
					if (bias > height) out.setBias(height);
					else out.setBias(bias);
					
					if (ConvolveAngles)
					{
						// cannot make average of angles, so just take max
						// out->Center = (in + iMaxSE*MAPYSIZE + jMaxSE)->Center;
						// no, just taking max gives systematic errors
						// better to risk an average, and make a sanity check
						while (center > F2_5Cell.PI2) center -= F2_5Cell.PI2;
						if (Math.abs(center-oldcenter) > F2_5Cell.PIq) // sanity check
						{
							center = oldcenter;
						}
						
						if (chunkno == 1) // do the rotation
						{
							center += drot;
							adth = 0.0;
							if (center > F2_5Cell.PI2) center -= F2_5Cell.PI2;
							if (center < 0.0) center += F2_5Cell.PI2;
						}
						
						out.setCenter(center);	// we have our new angle
						
						if (core > F2_5Cell.PI2) core = F2_5Cell.PI2;
						out.setCore(core);
						
						if (supp > F2_5Cell.PI2) supp = F2_5Cell.PI2;
						out.setSupport(supp);
					}
				}
			
			++chunkno;
			// done with this chunk
		}
//*/
	}
	
	public void convolution(double dlin, double dlat, double drot)
	{
		boolean ConvolveAngles = true;
		
		// Element for motion blurring
		ConvolutionElement motionelement = new ConvolutionElement();
		
		// Linear and rotational displacement (Since robot system reference)
		double rho_robot;
		double phi_dispacement;
		
		rho_robot = Math.sqrt(dlin*dlin + dlat*dlat);
		phi_dispacement = Math.atan2(dlat, dlin);
		
		// Actual displacement in robot coordinates
		double dx_robot, dy_robot;
		
		dx_robot	= rho_robot*Math.cos(phi_dispacement);
		dy_robot	= rho_robot*Math.sin(phi_dispacement);
		
		// For rotate to global coordinates
		double phi_global;
		double sinphi, cosphi;
		
		phi_global	= gspos.getPosition().getTheta() + drot;
		sinphi		= Math.sin(phi_global);
		cosphi		= Math.cos(phi_global);
		
		double dx_global, dy_global;
		
		dx_global	= dx_robot*cosphi - dy_robot*sinphi;
		dy_global	= dx_robot*sinphi + dy_robot*cosphi;
		
///*
		// Divide global displacement in chunks no larger then cellsize
		int chunkno = 1;
		
		double dx_global_aux, dy_global_aux;
		
		dx_global_aux = dx_global;
		dy_global_aux = dy_global;
		
		double adx  = Math.abs(dx_global_aux);
		double ady  = Math.abs(dy_global_aux);
		double adth = Math.abs(drot);
		
		double dx, dy;
		double tmp;
		
		while (
			(adx  > MinDisplacement) ||
			(ady  > MinDisplacement) ||
			(adth > MinRotation))
		{
			// still some update to be done
			if ( 
				(adx > ady) && // X is larger direction
				(adx > gsize) // and must be split into chunks
			) {
				tmp = gsize / adx;
				dx = (dx_global_aux > 0.0) ? gsize : -gsize;
				//dy = (Math.abs(dy_global_aux) < 0.0001) ? 0.0 : dy_global_aux * tmp;
				dy = dy_global_aux * tmp;
				//System.out.println("Case 1: dx " + dx + " > dy " + dy);
			} else if (
				(ady > adx) && // Y is larger direction
				(ady > gsize) // and must be split into chunks
			) {
				tmp = gsize / ady;
				//dx = (Math.abs(dx_global_aux) < 0.0001) ? 0.0 : dx_global_aux * tmp;
				dx  = dx_global_aux * tmp;
				dy  = (dy_global_aux > 0.0) ? gsize : -gsize;
				//System.out.println("Case 2: dy " + dy + " > dx " + dx);
			} else { // can be done in one step
				dx  = dx_global_aux;
				dy  = dy_global_aux;
				//System.out.println("Case 3: dx " + dx + " dy " + dy);
			}
			
			// how much is left to displace after this chunk
			dx_global_aux -= dx;
			dy_global_aux -= dy;
			adx = Math.abs(dx_global_aux);
			ady = Math.abs(dy_global_aux);
			
			// Compute translating element
			// Note: since map is stored bot-up and left-right, the coords of SE are:
			//  +-----+-----+-----+--> Dx
			//   | 0:0  | 0:1  | 0:2 |
			//  +-----+-----+-----+
			//   | 1:0  | 1:1  | 1:2 |
			//  +-----+-----+-----+
			//   | 2:0  | 2:1  | 2:2 |
			//  +-----+-----+-----+
			//   |
			//   v
			//  Dy
			// Below, 'up' means to UP in Gs, ie, postive Y, etc.
			
			double side = gsize;
			double side2 = side * side;
			
			if ((dx >= 0.0) && (dy < 0.0)) // motion in 2nd quadrant
			{
				dy = -dy;
				
				//System.out.println("Motion in 2nd quadrant");
				
				motionelement.set(2, 1, (side - dx) * dy / side2);
				motionelement.set(1, 0, (side - dy) * dx / side2);
				motionelement.set(2, 0, dx * dy / side2);
			} else if ((dx < 0.0) && (dy < 0.0)) { // motion in 3rd quadrant
				dx = -dx;
				dy = -dy;
				
				//System.out.println("Motion in 3rd quadrant");
				
				motionelement.set(2, 1, (side - dx) * dy / side2);
				motionelement.set(1, 2, (side - dy) * dx / side2);
				motionelement.set(2, 2, dx * dy / side2);
			} else if ((dx < 0.0) && (dy >= 0.0)) { // motion in 4th quadrant
				dx = -dx;
				
				//System.out.println("Motion in 4th quadrant");
				
				motionelement.set(0, 1, (side - dx) * dy / side2);
				motionelement.set(1, 2, (side - dy) * dx / side2);
				motionelement.set(0, 2, dx * dy / side2);
			} else { // if motion in 1st quadrant ((dx > 0.0) && (dy > 0.0))
				
				//System.out.println("Motion in 1st quadrant");
				
				motionelement.set(0, 1, (side - dx) * dy / side2);
				motionelement.set(1, 0, (side - dy) * dx / side2);
				motionelement.set(0, 0, dx * dy / side2);
			}
			
			motionelement.set(1, 1, ((side - dx) * (side - dy)) / side2);
			
			//System.out.println(motionelement.toString());
			
			// Do the convolution
			
			FMKMap fmkMap_temp;
			fmkMap_temp = (FMKMap)fmkMap.clone();
			
			F2_5Cell out;
			F2_5Cell cell; // grid cell for inner loop
			double height, bias; // cumulative result of convolution
			double center, core, supp; // for angle convolution
			double oldcenter;
			double seVal;
			
			int maxX = gwidth - 2; // Max row
			int maxY = gheight - 2; // Max column
			
			for (int x = 0; x < maxX; ++x) // map row scan (left-to-right in GS)
				for (int y = 0; y < maxY; ++y) // map column scan (bottom-up in GS)
				{
					out = fmkMap.getCell(x + 1, y + 1);
					
					height = 0.0;
					bias = 0.0;
					center = 0.0;
					core = 0.0;
					supp = 0.0;
					oldcenter = out.getCenter(); // old angle is still in out map
					
					for (int i = 0; i < 3; ++i) // SE row scan
						for (int j = 0; j < 3; ++j) // SE column scan
						{
							seVal = motionelement.get(j, i);
							if (seVal > 0.0)
							{
								cell = fmkMap_temp.getCell(x + i, y + j);
								
								height += seVal * cell.getHeight();
								bias += seVal * cell.getBias();
								
								if(ConvolveAngles)
								{
									center += seVal * cell.getCenter();
									core += seVal * cell.getCore();
									supp += seVal * cell.getSupport();
								}
							}
						}
					
					// set result in grid
					if (height > 1.0) height = 1.0;
					if (height < F2_5Cell.BIAS) height = F2_5Cell.BIAS;
					out.setHeight(height);
					
					if (bias > height) out.setBias(height);
					else out.setBias(bias);
					
					if (ConvolveAngles)
					{
						// cannot make average of angles, so just take max
						// out->Center = (in + iMaxSE*MAPYSIZE + jMaxSE)->Center;
						// no, just taking max gives systematic errors
						// better to risk an average, and make a sanity check
						while (center > F2_5Cell.PI2) center -= F2_5Cell.PI2;
						if (Math.abs(center-oldcenter) > F2_5Cell.PIq) // sanity check
						{
							center = oldcenter;
						}
						
						if (chunkno == 1) // do the rotation
						{
							center += drot;
							adth = 0.0;
							if (center > F2_5Cell.PI2) center -= F2_5Cell.PI2;
							if (center < 0.0) center += F2_5Cell.PI2;
						}
						
						out.setCenter(center);	// we have our new angle
						
						if (core > F2_5Cell.PI2) core = F2_5Cell.PI2;
						out.setCore(core);
						
						if (supp > F2_5Cell.PI2) supp = F2_5Cell.PI2;
						out.setSupport(supp);
					}
				}
			
			++chunkno;
			// done with this chunk
		}
//*/
	}
	
	/**
	 * 
	 * @author asaffiotti
	 * 
	 *	This does the pure blurring thing:
	 *	Add uncertainty in position and angle depending on current velocity
	 * 
	 */
	public void Blur(double rho, double theta)
	{
		if ((rho > MaxDisplacement) | (theta > MaxRotation))
		{
			System.out.println(" Warning:: Odometry value incorrect , it is more the  maximun allowed ");
			return;
		}
		
		ConvolutionElement blur_element = new ConvolutionElement();
		
		// Set up an omnidirectional blurring element
		double blurD, blurA, blur1, blur2; // how much to blur
		
		blurD = Math.abs(rho) * one_over_maxdis; // displacement component
		blurA = Math.abs(theta) * one_over_maxrot; // rotation component
		
		blur1 = blurD + blurA - blurD*blurA; // 4-neighbors
		if (blur1 < BlurPosBias) blur1 = BlurPosBias;
		blur2 = blur1 * one_over_sqrt_2;	// diagonal neighbors
		
		blur_element.set(0, 0, blur2);
		blur_element.set(0, 1, blur1);
		blur_element.set(0, 2, blur2);
		
		blur_element.set(1, 0, blur1);
		blur_element.set(1, 1, 1.0);
		blur_element.set(1, 2, blur1);
		
		blur_element.set(2, 0, blur2);
		blur_element.set(2, 1, blur1);
		blur_element.set(2, 2, blur2);
		
		//System.out.println(blur_element.toString());
		
		// Do the dilation
		//int maxX = gwidth - SE_XDIM + 1; // max column to scan
		//int maxY = gheight - SE_YDIM + 1; // max row
		
		int maxX = gwidth - 2; // max column to scan
		int maxY = gheight - 2; // max row
		
		FMKMap fmkMap_temp;
		fmkMap_temp = (FMKMap)fmkMap.clone();
		
		F2_5Cell out;
		F2_5Cell cell; // grid cell for inner loop
		double height, bias; // cumulative result of dilation
		double val;
		
		for (int x = 0; x < maxX; ++x)	// map row scan (left-to-right in GS)
			for (int y = 0; y < maxY; ++y) // map column scan (bottom-up in GS)
			{
				out = fmkMap.getCell(x + 1, y + 1);
				
				height = 0.0;
				bias = 0.0;
				
				for (int i = 0; i < 3; ++i) // SE row scan
					for (int j = 0; j < 3; ++j) // SE column scan
					{
						cell = fmkMap_temp.getCell(x + i, y + j);
						val = blur_element.get(i, j) * cell.getHeight();
						if (val > height) height = val;
						
						val = blur_element.get(i, j) * cell.getBias();
						if (val > bias) bias = val;
					}
				
				out.setHeight(height);
				
				if (bias > height) 
					out.setBias(height);
				else
					out.setBias(bias);
				
				// blur the angle as well (old angle still in out map)
				out.setCore(out.getCore() + (blurA * BlurAngleMax + BlurAngleBias));
				out.setSupport(out.getCore() + (blurA * BlurAngleMax * 2.0 + BlurAngleBias));
				
				if (out.getCore() > F2_5Cell.PI2) out.setCore(F2_5Cell.PI2);
				if (out.getSupport() > F2_5Cell.PI2) out.setSupport(F2_5Cell.PI2);
			}
	}
	
	/**
	 * 
	 * @author asaffiotti
	 *  
	 * Center of gravity self-location
	 * 
	 */
	public void locate()
	{
		// Get the center of gravity for the highest values in the grid.
		// We start with a normalize, by finding highest value.
		
		// First pass. Find highest value
		double highest, bias, current;
		F2_5Cell cell; // grid cell for inner loop
		
		highest = 0.0001;
		bias = 0.0;
		
		for(int gx = 0; gx < gwidth; gx++)
			for(int gy = 0; gy < gheight; gy++)
			{
				cell = fmkMap.getCell(gx, gy);
				
				current = cell.getHeight();
				if (current > highest) highest = current;
				
				current = cell.getBias();
				if (current > bias) bias = current;
			}
		
		if (highest > bias)
			gspos.setReliability(highest - bias);
		else
			gspos.setReliability(0.0);
		
		if (highest < 0.000101)
		{
			// We have a problem here. All values are (almost) zero.
			// So we will reset all values...
			fmkMap.clearGrid();
			
			highest = 1.0;
		}
		
		// Second pass. Normalize so that highest value is 1.0
		fmkMap.Normalize(highest);
		
		// Compute CoG for the area above a given threshold
		// together with the bounding box of this area.
		
		double sumMu;		// possibility degree
		double sumX;		// X index
		double sumY;		// Y index
		double sumTh;		// theta value (cell centers)
		double sumDelta;	// theta variance (cell core)
		
		sumMu = sumX = sumY = sumTh = sumDelta = 0.0;
		
		double dwidth = (double)gwidth;
		double dheight = (double)gheight;
		
		double minX = (double)dwidth;
		double maxX = 0.0;
		double minY = (double)dheight;
		double maxY = 0.0;
		
		double h_cell; // for inner loop
		
		int gx, gy;
		
		gx = 0;
		for (double xd = 0.0; xd < dwidth; xd++)
		{
			gy = 0;
			for (double yd = 0.0; yd < gheight; yd++)
			{
				cell = fmkMap.getCell(gx, gy);
				h_cell = cell.getHeight();
				
				if (h_cell > CoGThreshold)
				{
					sumMu += h_cell;
					sumX += h_cell * xd;
					sumY += h_cell * yd;
					sumTh += h_cell * cell.getCenter();
					sumDelta += h_cell * cell.getCore();
					
					if (xd < minX) minX = xd;
					if (xd > maxX) maxX = xd;
					if (yd < minY) minY = yd;
					if (yd > maxY) maxY = yd;
				}
				gy++;
			}
			gx++;
		}
		
		if (sumMu == 0.0) // this should never happen...
		{
			System.out.println(" Global Map --- Cannot compute GoG (empty core)");
			return;
		}
		
		// find indexes of the two cells nearest to the CoG
		int idx1, idx2;
		double w1, w2;
		
		sumX /= sumMu;
		idx1 = (int)sumX;
		idx2 = (idx1 < (gwidth - 1)) ? (idx1 + 1) : idx1;
		w2 = sumX - (double)idx1;
		w1 = 1.0 - w2;
		
		double XVal, YVal;
		double Angle;
		double AngleVariance;
		
		// and make weighted average of their coordinates
		XVal = (getGridRealPosX(idx1) * w1) + (getGridRealPosX(idx2) * w2);
		
		// do the same for Y
		sumY /= sumMu;
		idx1 = (int)sumY;
		idx2 = (idx1 < (gheight - 1)) ? (idx1 + 1) : idx1;
		w2 = sumY - (double)idx1;
		w1 = 1.0 - w2;
		
		//System.out.println(" idx1 " + idx1 + " idx2 " + idx2);
		//System.out.println(" w1 " + w1 + " w2 " + w2);
		//System.out.println(" ry1 " + mkcanvas.getGridRealPosY(idx1) + " ry2 " + mkcanvas.getGridRealPosY(idx2));
		
		// and make weighted average of their coordinates
		YVal = (getGridRealPosY(idx1) * w1) + (getGridRealPosY(idx2) * w2);
		
		//System.out.println("Localize: CoG is (" + XVal + ", " + YVal + ")");
		
		// for the orientation, just take the one of the closest cell
		cell = fmkMap.getCell((int)(sumX + 0.5), (int)(sumY + 0.5));
		
		Angle = cell.getCenter();
		//Angle = NORM(Angle);
		Angle = ((Angle) > PI) ? ((Angle)-PI2) : (((Angle) < -PI) ? ((Angle)+PI2) : (Angle));
		
		// for variance, take width of the alpha-cut at CoGThreshold
		AngleVariance = cell.getCore() * CoGThreshold + cell.getSupport() * (1.0 - CoGThreshold);
		
		// Put values in global space regarding own position
		gspos.getPosition().set(
			XVal,
			YVal,
			Angle,
			(maxX - minX + 1.0) * gsize,
			(maxY - minY + 1.0) * gsize,
			AngleVariance);
		
		//System.out.println(gspos.getPosition().toString());
		
		checkInsidePosition();
		
		MKPos mypos = gspos.getPosition();
		
		gspos.setFocus(1.0 - (Math.abs(mypos.getDX() * mypos.getDY())/(rwidth * rheight)));
		
		gspos.refreshGUI();
	}
	
	public void initialiseMap(double xpos, double ypos, double angle, double xwidth, double yheight, double anglewidth)
	{
		double xstart, xend, ystart, yend;
		
		xstart	= xpos - 0.5*xwidth;
		xend	= xpos + 0.5*xwidth;
		ystart	= ypos - 0.5*yheight;
		yend	= ypos + 0.5*yheight;
		
		double current_xpos, current_ypos;
		double dist, distX, distY;
		double height;
		
		F2_5Cell fuzzyVal;
		
		for (int gx = 0; gx < gwidth; gx++)
			for (int gy = 0; gy < gheight; gy++)
			{
				// Current position in m
				current_xpos = getGridRealPosX(gx);
				current_ypos = getGridRealPosY(gy);
				
				// Get distances to position "blob" for this cell
				if (current_xpos < xstart)
					distX = (double)(xstart - current_xpos);
				else if (current_xpos > xend)
					distX = (double)(current_xpos - xend);
				else
					distX = 0;
				
				if (current_ypos < ystart)
					distY = (double)(ystart - current_ypos);
				else if (current_ypos > yend)
					distY = (double)(current_ypos - yend);
				else
					distY = 0;
				
				dist = Math.sqrt(distX * distX + distY * distY);
				
				// Calculate height as a function of distance from position "blob"
				// Slope is such that height reaches bias one
				// half meter away... (hack!!!)
				height = 1.0 - dist;
				//height = 1.0 - 0.002*dist;
				
				if (height < F2_5Cell.BIAS)
					height = F2_5Cell.BIAS;  // And no lower than bias!
				
				fuzzyVal = fmkMap.getCell(gx, gy);
				
				fuzzyVal.setHeight(height);
				fuzzyVal.setCore(anglewidth);
				fuzzyVal.setSupport(anglewidth + F2_5Cell.ANGLE_SLOPE);
				fuzzyVal.setBias(F2_5Cell.BIAS);
				fuzzyVal.setCenter(angle);
			}
	}
	
	///*
	public void initMap(MKCanvas mkcanvas)
	{
		int x1, y1, x2, y2;
		int gx, gy;
		
		Line2 line2;
		WMWall[] wmwall;
		
		wmwall = world.walls().edges();
		
		for(int i = 0; i < wmwall.length; i++)
		{
			line2 	= wmwall[i].edge;
			
			//System.out.println(" Line " + i + " Rho: " + line2.rho() + " Theta: " + line2.theta());
			
			x1 = mkcanvas.getPosX(line2.orig().x());
			y1 = mkcanvas.getPosY(line2.orig().y());
			x2 = mkcanvas.getPosX(line2.dest().x());
			y2 = mkcanvas.getPosY(line2.dest().y());
			
			int dx = x2 - x1;
			int dy = y2 - y1;
			
			gx = mkcanvas.getGridX(x1);
			gy = mkcanvas.getGridY(y1);
			
			getCell(gx, gy).setHeight(F2_5Cell.FULL);
			
			if (Math.abs(dx) > Math.abs(dy))	// slope < 1
			{
				double m = (double) dy / (double) dx;	// compute slope
				double b = y1 - m*x1;
				dx = (dx < 0) ? -1 : 1;
				
				while (x1 != x2)
				{
					x1 += dx;
					
					gx = mkcanvas.getGridX(x1);
					gy = mkcanvas.getGridY(Math.round(m*x1 + b));
					
					getCell(gx, gy).setHeight(F2_5Cell.FULL);
				}
			} else if (dy != 0) {						// slope >= 1
				double m = (double) dx / (double) dy;		// compute slope
				double b = x1 - m*y1;
				dy = (dy < 0) ? -1 : 1;
				
				while (y1 != y2)
				{
					y1 += dy;
					
					gx = mkcanvas.getGridX(Math.round(m*y1 + b));
					gy = mkcanvas.getGridY(y1);
					
					getCell(gx, gy).setHeight(F2_5Cell.FULL);
				}
			}
		}
	}
	//*/
}
