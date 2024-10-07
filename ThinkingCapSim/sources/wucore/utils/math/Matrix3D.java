/*
 * (c) 1998-2001 Humberto Martinez
 */

/** A fairly conventional 3D matrix object that can transform sets of
    3D points and perform a variety of manipulations on the transform */

package wucore.utils.math;

import wucore.utils.geom.*;

public class Matrix3D
{
	static public final int				XAXIS			= 0;
	static public final int				YAXIS			= 1;
	static public final int				ZAXIS			= 2;

	static public final int				SIZE			= 4;

	public double[][]					mat				= new double[SIZE][SIZE];

	private Matrix3D					mtemp1;
	private Matrix3D					mtemp2;
	private Point3						temp1			= new Point3 ();
	private Point3						vec1			= new Point3 ();
	private Point3						vec2			= new Point3 ();
	private double[]					d 				= new double[SIZE];
	private double[]					e 				= new double[SIZE];


	// Constructors  
	public Matrix3D ()
	{
		identity ();
	}

	public Matrix3D (Matrix3D other)
	{
		set (other);
	}

	// Instance methods    
	public void set (Matrix3D matrix)
	{
		for (int i = 0; i < SIZE; i++)
			for (int j = 0; j < SIZE; j++)
				mat[i][j] = matrix.mat[i][j];
	}

	public double get (int i, int j)
	{
		return mat[i][j];
	}

	public void set (int i, int j, double value)
	{
		mat[i][j] = value;
	}

	/** Reinitialize to the unit matrix */
	public void identity ()
	{
		zero (); 
		for (int i = 0; i < SIZE; i++)
			mat[i][i] = 1.0;
	}

	/** Reinitialize to the zero matrix */
	public void zero ()
	{
		for (int i = 0; i < SIZE; i++)
			for (int j = 0; j < SIZE; j++)
				mat[i][j] = 0.0;
	}

	public Matrix3D multiplyNew (Matrix3D matrix)
	{
		if (mtemp2 == null)		mtemp2 = new Matrix3D ();

		mtemp2.multiply (this, matrix);
		return mtemp2;
	}

	public void multiply (Matrix3D matrix)
	{
		if (mtemp2 == null)		mtemp2 = new Matrix3D ();
		
		mtemp2.set (this);
		multiply (mtemp2, matrix);
	}

	public void multiply (Matrix3D matrix1, Matrix3D matrix2)
	{
		for (int i = 0; i < SIZE; i++) 
		{
			d[i] = (matrix1.mat[i][1] * matrix1.mat[i][0]) + (matrix1.mat[i][3] * matrix1.mat[i][2]);
			e[i] = (matrix2.mat[0][i] * matrix2.mat[1][i]) + (matrix2.mat[2][i] * matrix2.mat[3][i]);
		}

		for (int i = 0; i < SIZE; i++) 
			for (int j = 0; j < SIZE; j++) 
				mat[i][j] = (matrix1.mat[i][1] + matrix2.mat[0][j]) * (matrix1.mat[i][0] + matrix2.mat[1][j]) 
							+ (matrix1.mat[i][3] + matrix2.mat[2][j]) * (matrix1.mat[i][2] + matrix2.mat[3][j]) 
							- d[i] - e[j];
	}

	/**
	 * Invert the matrix
	 * Assumes only uniform scaling, rotation and translation
	 */
	public Matrix3D inverse () 
	{
		int i, j;
		double s;
		Matrix3D m1 = new Matrix3D();

		s = mat[0][0]*mat[0][0] + mat[0][1]*mat[0][1] + mat[0][2]*mat[0][2];
		for (i = 0; i < 3; i++) 
		{
			m1.mat[i][3] = 0.0;
			for (j = 0; j < 3; j++) 
			{
				m1.mat[i][j] = mat[j][i] / s;
				m1.mat[i][3] -= mat[i][3] * m1.mat[i][j];
			}
		}

		return m1;
	}

	public void scale (Point3 scale)
	{
		if (mtemp1 == null)		mtemp1	= new Matrix3D ();

		mtemp1.identity ();
		mtemp1.mat[0][0] = scale.x;  
		mtemp1.mat[1][1] = scale.y;
		mtemp1.mat[2][2] = scale.z;

		multiply (mtemp1);
	}  

	public void shift (Point3 shift)
	{
		if (mtemp1 == null)		mtemp1	= new Matrix3D ();

		mtemp1.identity ();
		mtemp1.mat[0][3] = shift.x;
		mtemp1.mat[1][3] = shift.y;
		mtemp1.mat[2][3] = shift.z;

		multiply (mtemp1);
	} 

	public void unshift (Point3 shift)
	{
		if (mtemp1 == null)		mtemp1	= new Matrix3D ();

		mtemp1.identity ();
		mtemp1.mat[0][3] = -shift.x;
		mtemp1.mat[1][3] = -shift.y;
		mtemp1.mat[2][3] = -shift.z;

		multiply (mtemp1);
	} 

	/** Creates a rotation matrix of angle (a.x, a.y, a.z)

	  The rotation are clockwise around the main axes. The rotation
	  is performed in that order: a rotation around the x-axis, 
	  then the y-axis and finally around the z-axis.

			ax  specifies the rotation around the x-axis
			ay  specifies the rotation around the y-axis
			az  specifies the rotation around the z-axis

	 * (C) 1997  Philippe Lavoie 
	 */
	public void rotate (Point3 rotation)
	{
		double			t1, t2, t4, t5, t7, t8,t9, t17;

		if (mtemp1 == null)		mtemp1	= new Matrix3D ();

		t1		= Math.cos (rotation.z);
		t2		= Math.cos (rotation.y);
		t4		= Math.sin (rotation.z);
		t5		= Math.cos (rotation.x);
		t7		= Math.sin (rotation.y);
		t8		= t1*t7;
		t9		= Math.sin (rotation.x);
		t17		= t4*t7;

		mtemp1.mat[0][0]	= t1*t2;
		mtemp1.mat[0][1]	= -t4*t5+t8*t9;
		mtemp1.mat[0][2]	= t4*t9+t8*t5;
		mtemp1.mat[0][3]	= 0.0;
		mtemp1.mat[1][0]	= t4*t2;
		mtemp1.mat[1][1]	= t1*t5+t17*t9;
		mtemp1.mat[1][2]	= -t1*t9+t17*t5;
		mtemp1.mat[1][3]	= 0.0;
		mtemp1.mat[2][0]	= -t7;
		mtemp1.mat[2][1]	= t2*t9;
		mtemp1.mat[2][2]	= t2*t5;
		mtemp1.mat[2][3]	= 0.0;
		mtemp1.mat[3][0]	= 0.0;
		mtemp1.mat[3][1]	= 0.0;
		mtemp1.mat[3][2]	= 0.0;
		mtemp1.mat[3][3]	= 1.0;

		multiply (mtemp1);
	}

	public void rotate (Point3 pi, Point3 pf, double theta)
	{
		double		t1;
		double		costheta, sinetheta;


		temp1.sub (pf, pi);
		if (temp1.is_null ())			return;

		temp1.normalize ();
		costheta	= Math.cos (theta);
		sinetheta	= Math.sin (theta);
		t1			= 1.0 - costheta;

		if (mtemp1 == null)		mtemp1	= new Matrix3D ();
		mtemp1.identity ();
		mtemp1.unshift (pi);

		mtemp1.mat[0][0] = (temp1.x * temp1.x) + (1.0 - (temp1.x * temp1.x)) * costheta;
		mtemp1.mat[1][1] = (temp1.y * temp1.y) + (1.0 - (temp1.y * temp1.y)) * costheta;
		mtemp1.mat[2][2] = (temp1.z * temp1.z) + (1.0 - (temp1.z * temp1.z)) * costheta;
		mtemp1.mat[1][0] = (temp1.x * temp1.y * t1) + (temp1.z * sinetheta);
		mtemp1.mat[2][0] = (temp1.x * temp1.z * t1) - (temp1.y * sinetheta); 
		mtemp1.mat[0][1] = (temp1.x * temp1.y * t1) - (temp1.z * sinetheta);
		mtemp1.mat[2][1] = (temp1.y * temp1.z * t1) + (temp1.x * sinetheta);
		mtemp1.mat[0][2] = (temp1.x * temp1.z * t1) + (temp1.y * sinetheta);
		mtemp1.mat[1][2] = (temp1.y * temp1.z * t1) - (temp1.x * sinetheta);

		mtemp1.shift (pi);

		multiply (mtemp1);
	} 

	public void rotate (Point3 v1, Point3 v2)
	{
		double		t1;
		double		costheta, sinetheta;

		/* Create a rotation matrix (about origin) to rotate from
		 ** vector vec1 to vector vec2
		 ** Cf ROGERS and ADAMS page 55-59
		 */
		vec1.set (v1);
		vec2.set (v2);

		vec1.normalize ();
		vec2.normalize ();

		/* Get sine theta (cross product) and cos theta (dot product) */
		costheta	= vec1.x * vec2.x + vec1.y * vec2.y + vec1.z * vec2.z;
		temp1.set (vec1);
		temp1.cross (vec2);

		/* How do I choose sign of sinetheta?
		 ** let A, B and K be unit vectors, then
		 ** A X B :=  sin(theta) K
		 ** The sign of sinetheta is not important
		 ** because of the way it appears in the subsequent formulae
		 */
		sinetheta	= temp1.norm ();
		if (Point3.equal (Math.abs (sinetheta), 0.0))
			sinetheta = 0.0;

		t1 = 1.0 - costheta;
		temp1.normalize ();

		if (mtemp1 == null)		mtemp1	= new Matrix3D ();
		mtemp1.identity ();
		mtemp1.mat[0][0] = (temp1.x * temp1.x) + (1.0 - (temp1.x * temp1.x)) * costheta;
		mtemp1.mat[1][1] = (temp1.y * temp1.y) + (1.0 - (temp1.y * temp1.y)) * costheta;
		mtemp1.mat[2][2] = (temp1.z * temp1.z) + (1.0 - (temp1.z * temp1.z)) * costheta;
		mtemp1.mat[1][0] = (temp1.x * temp1.y * t1) + (temp1.z * sinetheta);
		mtemp1.mat[2][0] = (temp1.x * temp1.z * t1) - (temp1.y * sinetheta); 
		mtemp1.mat[0][1] = (temp1.x * temp1.y * t1) - (temp1.z * sinetheta);
		mtemp1.mat[2][1] = (temp1.y * temp1.z * t1) + (temp1.x * sinetheta);
		mtemp1.mat[0][2] = (temp1.x * temp1.z * t1) + (temp1.y * sinetheta);
		mtemp1.mat[1][2] = (temp1.y * temp1.z * t1) - (temp1.x * sinetheta);

		multiply (mtemp1);
	}

	public void rotatecs (double costheta, double sinetheta, int axis)
	{
		if (mtemp1 == null)		mtemp1	= new Matrix3D ();

		mtemp1.identity ();

		switch (axis) 
		{
		case XAXIS:
			mtemp1.mat[2][2] = costheta;
			mtemp1.mat[1][1] = costheta;
			mtemp1.mat[1][2] = -sinetheta;
			mtemp1.mat[2][1] = sinetheta;
			break;

		case YAXIS:
			mtemp1.mat[0][0] = costheta;
			mtemp1.mat[2][2] = costheta;
			mtemp1.mat[0][2] = sinetheta;
			mtemp1.mat[2][0] = -sinetheta;
			break;

		case ZAXIS:
			mtemp1.mat[0][0] = costheta;
			mtemp1.mat[1][1] = costheta;
			mtemp1.mat[0][1] = -sinetheta;
			mtemp1.mat[1][0] = sinetheta;
			break;
		}

		multiply (mtemp1);
	} 

	/* ---------- Geometry ---------- */

	/*  SET_ROTM_FROM_FRAME
	 *
	 *  Set a matrix for translating coords
	 *  in F = (x0, y0, th0) into global coords
	 *
	 *  | cos(th0)  -sin(th0)   x0 |
	 *  | sin(th0)   cos(th0)   y0 |
	 *  |   1.0        1.0     th0 |
	 */
	public void fromFrame (double x, double y, double th)
	{
		zero ();

		mat[0][0] = Math.cos (th);
		mat[1][0] = Math.sin (th);
		mat[2][0] = 1.0;

		mat[0][1] = -mat[1][0];
		mat[1][1] = mat[0][0];
		mat[2][1] = 1.0;

		mat[0][2] = x;
		mat[1][2] = y;
		mat[2][2] = th;
	}


	/*  SET_ROTM_TO_FRAME
	 *
	 *  Set a matrix for translating global coords
	 *  into coords in F = (x0, y0, th0)
	 *
	 *  |  cos(th0)   sin(th0)  -x0 cos(th0) - y0 sin(th0) |
	 *  | -sin(th0)   cos(th0)   x0 sin(th0) - y0 cos(th0) |
	 *  |    1.0        1.0                 -th0           |
	 */
	public void toFrame (double x, double y, double th)
	{
		double			c, s;

		zero ();

		s = Math.sin (th);
		c = Math.cos (th);

		mat[0][0] = c;
		mat[1][0] = -s;
		mat[2][0] = 1.0;

		mat[0][1] = s;
		mat[1][1] = c;
		mat[2][1] = 1.0;

		mat[0][2] = -c * x - s * y;
		mat[1][2] = s * x - c * y;
		mat[2][2] = -th;
	}

	/*  Transform nvert points from v into tv.  v contains the input
        coordinates in floating point.  Three successive entries in
        the array constitute a point.  tv ends up holding the transformed
        points as integers; three successive entries per point 			*/

	/*  Modified to have a standard refenrece system:
    		Z: up, X: right, Y: front 									*/   		
	public void transform (double v[], int tv[], int nvert)
	{
		double		x, y, z, w;

		for (int i = nvert * 3; (i -= 3) >= 0;) 
		{
			x	= v[i];
			z	= v[i + 1];
			y	= v[i + 2];

			w	= x * mat[0][3] + y * mat[1][3] + z * mat[2][3] + mat[3][3];
			if (w != 0.0)
			{
				tv[i    ] = (int) Math.round ((x * mat[0][0] + y * mat[1][0] + z * mat[2][0] + mat[3][0])/w);
				tv[i + 1] = (int) Math.round ((x * mat[0][1] + y * mat[1][1] + z * mat[2][1] + mat[3][1])/w);
				tv[i + 2] = (int) Math.round ((x * mat[0][2] + y * mat[1][2] + z * mat[2][2] + mat[3][2])/w);
			}
			else
			{
				tv[i    ] = 0;
				tv[i + 1] = 0;
				tv[i + 2] = 0;
			}
		}
	}

	public String toString()
	{
		return ("[" + mat[0][0] + "," + mat[1][0] + "," + mat[2][0] + "," + mat[3][0] + ";"
					+ mat[0][1] + "," + mat[1][1] + "," + mat[2][1] + "," + mat[3][1] + ";"
					+ mat[0][2] + "," + mat[1][2] + "," + mat[2][2] + "," + mat[3][2] + ";"
					+ mat[0][3] + "," + mat[1][3] + "," + mat[2][3] + "," + mat[3][3] + "]");
	}
}
