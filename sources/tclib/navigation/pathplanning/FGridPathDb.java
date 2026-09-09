/*
 * (c) 2003 Humberto Martinez
 */
 
package tclib.navigation.pathplanning;

import tclib.navigation.mapbuilding.*;

public class FGridPathDb extends GridPathDb
{
	protected double				Ke;			// Maximum cost for an empty cell. Tricky set-up
												// Ke = m means leave m cells in case the cell
												// is not completely free.

	// Constructors
	public FGridPathDb (Grid grid)
	{
		super (grid);
		
		Ke		= 3.0;
	}
		
	// Instance methods 
	/* -----------------------------------------------
	   Cost distribution function.
	   ------------------------------------------------ */
	protected double gcost (int x, int y)
	{ 
		double		cts, val;
		int			i, j;
		int			xi, yi, xf, yf;
		
		// Compute window boundaries
		xi	= x - DILATION;
		xf	= x + DILATION;
		yi	= y - DILATION;
		yf	= y + DILATION;
		
		if (xi < 1)				xi = 1;
		if (yi < 1)				yi = 1;
		if (xf > size_x - 2)	xf = size_x - 2;
		if (yf > size_y - 2)	yf = size_y - 2;

		// Perform the fuzzy-dilation step on a given place
		cts = Double.MIN_VALUE;
		for (i = xi; i <= xf; i++)
			for (j = yi; j <= yf; j++)
				if (free[i][j] > cts)		cts = free[i][j];
		
		val	= cts;
		if ((free[x][y] != cts) && (free[x][y] < (cts/2)))
			val = cts / 2.0;
		
		// Perform the exponentiation step with the previous values
		if (val > THRES)
			return Ke + Math.exp (Kc * (val - THRES));		// Cost for occupied cell
		
		return val * Ke + 1.0;								// Cost for empty cell

//		return val + 1.0;									// Oriolo-Ulivi-Vendittelli method
//		return Math.exp (Kc * val);							// Humberto's thesis method
	}

	protected double hcost (int x, int y)
	{
		double		dx, dy;
		
		dx = (robot_x - x);
		dy = (robot_y - y);
		return Math.sqrt (dx * dx + dy * dy);
	}
}
