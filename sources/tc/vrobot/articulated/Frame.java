/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tc.vrobot.articulated;

/**
 * A rigid transform (a rotation and a translation), as a 4x4 matrix in row
 * major order, of the kind the kinematics of an articulated robot is made of:
 * the frame of every link, and how each is placed on its parent. Kept apart
 * from any 3D library so that the model can be computed anywhere.
 */
public class Frame
{
	public final double[]			m = new double[16];

	public Frame ()										{ identity (); }
	public Frame (Frame f)								{ System.arraycopy (f.m, 0, m, 0, 16); }

	public Frame identity ()
	{
		for (int i = 0; i < 16; i++)		m[i] = ((i % 5) == 0) ? 1.0 : 0.0;
		return this;
	}

	/** A pure translation. */
	static public Frame translation (double x, double y, double z)
	{
		Frame	f = new Frame ();

		f.m[3] = x;		f.m[7] = y;		f.m[11] = z;
		return f;
	}

	/** A pure rotation of an angle (rad) about an axis (need not be unit). */
	static public Frame rotation (double ax, double ay, double az, double angle)
	{
		Frame	f = new Frame ();
		double	n = Math.sqrt (ax * ax + ay * ay + az * az);

		if ((n < 1e-12) || (angle == 0.0))	return f;
		ax /= n;	ay /= n;	az /= n;

		double	c = Math.cos (angle), s = Math.sin (angle), t = 1.0 - c;

		f.m[0]  = t * ax * ax + c;			f.m[1]  = t * ax * ay - s * az;		f.m[2]  = t * ax * az + s * ay;
		f.m[4]  = t * ax * ay + s * az;		f.m[5]  = t * ay * ay + c;			f.m[6]  = t * ay * az - s * ax;
		f.m[8]  = t * ax * az - s * ay;		f.m[9]  = t * ay * az + s * ax;		f.m[10] = t * az * az + c;
		return f;
	}

	/** A rotation given as {x, y, z, angle}; the identity for null. */
	static public Frame rotation (double[] axisAngle)
	{
		if ((axisAngle == null) || (axisAngle.length < 4))		return new Frame ();
		return rotation (axisAngle[0], axisAngle[1], axisAngle[2], axisAngle[3]);
	}

	/** A translation given as {x, y, z}; the identity for null. */
	static public Frame translation (double[] t)
	{
		if ((t == null) || (t.length < 3))		return new Frame ();
		return translation (t[0], t[1], t[2]);
	}

	/** this * f, as a new frame: f in the coordinates of this. */
	public Frame times (Frame f)
	{
		Frame	r = new Frame ();

		for (int i = 0; i < 4; i++)
			for (int j = 0; j < 4; j++)
			{
				double	s = 0.0;

				for (int k = 0; k < 4; k++)		s += m[4 * i + k] * f.m[4 * k + j];
				r.m[4 * i + j] = s;
			}
		return r;
	}

	/** A point of this frame in the coordinates of its parent. */
	public double[] apply (double x, double y, double z)
	{
		return new double[] { m[0] * x + m[1] * y + m[2] * z + m[3],
							  m[4] * x + m[5] * y + m[6] * z + m[7],
							  m[8] * x + m[9] * y + m[10] * z + m[11] };
	}

	/** Where the origin of this frame is, in the coordinates of its parent. */
	public double[] origin ()							{ return new double[] { m[3], m[7], m[11] }; }

	public String toString ()
	{
		StringBuilder	sb = new StringBuilder ();

		for (int i = 0; i < 4; i++)
			sb.append (String.format ("[%7.3f %7.3f %7.3f %7.3f]%s", m[4 * i], m[4 * i + 1], m[4 * i + 2], m[4 * i + 3], (i < 3) ? "\n" : ""));
		return sb.toString ();
	}
}
