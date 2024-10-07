/*
 * (c) 1998-2000 Humberto Martinez
 */
 
/** A fairly conventional 2D matrix object that can transform sets of
    2D points and perform a variety of manipulations on the transform */

package wucore.utils.math;

import java.io.*;

public class Matrix2D extends Object implements Serializable
{
    public float 			xx;
    public float 			xy;
    public float 			xo;
    public float 			yx;
    public float 			yy;
    public float 			yo;
  
    /** Create a new unit matrix */
    public Matrix2D () 
    {
        unit ();
    }
    
    public void set (Matrix2D mat)
    {
    	xx	= mat.xx;
    	xy	= mat.xy;
    	xo	= mat.xo;
    	yx	= mat.yx;
    	yy	= mat.yy;
    	yo	= mat.yo;
    }
    
    /** Scale by f in all dimensions */
    public void scale (float f) 
    {
        xx *= f;
        xy *= f;
        xo *= f;
        yx *= f;
        yy *= f;
        yo *= f;
    }
    
    /** Scale along each axis independently */
    public void scale (float xf, float yf) 
    {
        xx *= xf;
        xy *= xf;
        xo *= xf;
        yx *= yf;
        yy *= yf;
        yo *= yf;
    }
    
    /** Translate the origin */
    public void translate (float x, float y) 
    {
        xo += x;
        yo += y;
    }
    
    /** rotate theta degrees */
    public void rot (float theta) 
    {
        float 			ct;
        float 			st;
    	float			lxo, lxx, lxy;
    	float			lyo, lyx, lyy;
    	
        theta *= Angles.DTOR;
        ct = (float) Math.cos (theta);
        st = (float) Math.sin (theta);

        lxx = xx * ct + yx * st;
        lxy = xy * ct + yy * st;
        lxo = xo * ct + yo * st;

        lyx = xx * st + yx * ct;
        lyy = xy * st + yy * ct;
        lyo = xo * st + yo * ct;
        
        xo = lxo;
        xx = lxx;
        xy = lxy;
        yo = lyo;
        yx = lyx;
        yy = lyy;
    }
        
    /** Multiply this matrix by a second: M = M*R */
    public void mult (Matrix2D rhs) 
    {
    	float			lxo, lxx, lxy;
    	float			lyo, lyx, lyy;
    	
        lxx = xx * rhs.xx + yx * rhs.xy;
        lxy = xy * rhs.xx + yy * rhs.xy;
        lxo = xo * rhs.xx + yo * rhs.xy + rhs.xo;

        lyx = xx * rhs.yx + yx * rhs.yy;
        lyy = xy * rhs.yx + yy * rhs.yy;
        lyo = xo * rhs.yx + yo * rhs.yy + rhs.yo;

        xo = lxo;
        xx = lxx;
        xy = lxy;
        yo = lyo;
        yx = lyx;
        yy = lyy;
    }

    /** Reinitialize to the unit matrix */
    public void unit () 
    {
        xo = 0.0f;
        xx = 1.0f;
        xy = 0.0f;
        yo = 0.0f;
        yx = 0.0f;
        yy = 1.0f;
    }
    
    /*  Transform nvert points from v into tv.  v contains the input
        coordinates in floating point.  Two successive entries in
        the array constitute a point.  tv ends up holding the transformed
        points as integers; two successive entries per point 			*/
        
    public void transform (double v[], int tv[], int nvert) 
    {
        float 			x, y;
 
        for (int i = Math.min (nvert * 2, tv.length); (i -= 2) >= 0;) 
        {
            x = (float) v[i];
            y = (float) v[i + 1];
            tv[i    ] = (int) (x * xx + y * xy + xo);
            tv[i + 1] = (int) (x * yx + y * yy + yo);
        }
    }
        
    public String toString() 
    {
        return ("[" + xo + "," + xx + "," + xy + ";"
                + yo + "," + yx + "," + yy + "]");
    }
}
