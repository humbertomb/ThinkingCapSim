/**
 * Created on 26-nov-2008
 *
 * @author Humberto Martinez Barbera
 */
package wucore.widgets;

import wucore.utils.math.*;

public class Model2DCoord 
{
	public float			x;
	public float			y;
	public int				projx;
	public int				projy;
	
	public final void transform (Matrix2D mat)
	{
        projx = (int) (x * mat.xx + y * mat.xy + mat.xo);
        projy = (int) (x * mat.yx + y * mat.yy + mat.yo);	
	}
}
