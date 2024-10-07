/*
 * (c) 2002 Alessandro Saffiotti
 * (c) 2004 David Herrero Perez
 *
 * Fuzzy Markov Map
 *
 */
package tclib.navigation.localisation.fmarkov;

/**
 * @author David Herrero-Perez
 * @version 1.0
 * 
 */
public class FMKMap extends Object implements Cloneable {
	int gwidth, gheight; // World dimensions (grid cells)
	
	F2_5Cell[] mMap; // Belief that the cell is occupied
	
	// Constructor
	public FMKMap(int gwidth, int gheight)
	{
		initFGrid(gwidth, gheight);
	}
	
	// Initilise methods
	void initFGrid(int gwidth, int gheight)
	{
		int gx, gy;
		
		this.gwidth = gwidth;
		this.gheight = gheight;
		
		mMap = new F2_5Cell[gwidth*gheight];
		
		for(gy = 0; gy < gheight; gy++)
			for(gx = 0; gx < gwidth; gx++)
				mMap[gy*gwidth + gx] = new F2_5Cell();
	}
	
	//	Accessor methods
	public F2_5Cell getCell(int gx, int gy)
	{
		if ((gx>-1) && (gx<gwidth) && (gy>-1) && (gy<gheight))
			return mMap[gy*gwidth + gx];
		else
			return null;
	}
	
	// Utilities methods
	public Object clone()
	{
		FMKMap theClone = new FMKMap(gwidth, gheight);
		
		for(int i = 0; i < gwidth*gheight; i++)
			theClone.mMap[i] = (F2_5Cell)mMap[i].clone();
		
		return theClone;
	}
	
	public void resetGrid()
	{
		int gx, gy;
		
		for (gx = 0; gx < gwidth; gx++)
			for (gy = 0; gy < gheight; gy++)
				mMap[gy*gwidth + gx].reset();
	}
	
	public void clearGrid()
	{
		int gx, gy;
		
		for (gx = 0; gx < gwidth; gx++)
			for (gy = 0; gy < gheight; gy++)
				mMap[gy*gwidth + gx].clear();
	}
	
	public void Normalize(double highest)
	{
		int gx, gy;
		
		for (gx = 0; gx < gwidth; gx++)
			for (gy = 0; gy < gheight; gy++)
				mMap[gy*gwidth + gx].normalize(highest);
	}
}
