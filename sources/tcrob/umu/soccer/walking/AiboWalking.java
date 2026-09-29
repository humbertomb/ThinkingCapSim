/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcrob.umu.soccer.walking;

import tc.vrobot.articulated.KineModel;
import tc.vrobot.articulated.KineNode;

/**
 * A walking engine for the Aibo ERS-7, after the one of the GermanTeam
 * (RoboCup 2007): a trot in which the diagonal pairs of legs move in
 * opposite phase, each foot drawn along a closed path in the frame of the
 * body -- on the ground it is dragged back with the speed the body is to have,
 * so that the body goes forward over it; in the air it is lifted and brought
 * round to where the next step starts. The path of each foot is set by the
 * speed asked of the robot (m/s forward and to the left, rad/s about the
 * vertical, as everywhere in the simulator) and by the parameters of the gait
 * (where the feet rest, how high the body is over them, how much a foot is
 * lifted, how long a step lasts, how much of a step a foot is on the ground),
 * and the joints of each leg come out of the inverse kinematics of the leg.
 * The head goes where the camera is pointed: pan and tilt (rad), the tilt
 * spread over the two tilt joints of the neck when the head's own is not
 * enough.
 *
 * The engine works on a {@link KineModel} read from the Aibo's .kine file
 * (the names of its joints are the Aibo's, LEFT_FORELEG_J1 and so on), takes
 * the lengths of the legs from it, and writes the angles of the joints into
 * it every {@link #step}: whoever draws the model sees it walk.
 *
 * Parameters (m, s), the GermanTeam's names where they had one:
 * <pre>
 *   foreHeight, hindHeight     how high the body is over the front and the hind feet
 *   foreWidth, hindWidth       how far out from the middle of the body the feet rest
 *   foreCenterX, hindCenterX   where the feet rest fore and aft, from the shoulder and the hip
 *   footLift                   how high a foot is lifted in the air
 *   stepDuration               how long a full cycle of the gait lasts (both feet of a pair down and up)
 *   groundRatio                the part of the cycle a foot is on the ground (0.5: a trot)
 *   maxStep                    how far a foot may be dragged in one step; a longer stride shortens the cycle instead
 *   maxSpeed, maxTurn          what the robot is asked for beyond this is cut down to it (m/s, rad/s)
 * </pre>
 */
public class AiboWalking implements tc.vrobot.articulated.WalkingModel
{
	/* The legs, in the order of the phases of a trot */
	static public final String[]	LEGS	= { "LEFT_FORELEG", "RIGHT_HINDLEG", "RIGHT_FORELEG", "LEFT_HINDLEG" };
	static public final double[]	PHASES	= { 0.0, 0.0, 0.5, 0.5 };

	/* The parameters of the gait, close to the GermanTeam's 2007 fast walk (m, s) */
	public double					foreHeight		= 0.088;
	public double					hindHeight		= 0.100;
	public double					foreWidth		= 0.087;
	public double					hindWidth		= 0.085;
	public double					foreCenterX		= 0.008;
	public double					hindCenterX		= -0.010;
	public double					footLift		= 0.022;
	public double					stepDuration	= 0.48;
	public double					groundRatio		= 0.5;
	public double					maxStep			= 0.050;
	public double					maxSpeed		= 0.35;
	public double					maxTurn			= 1.6;

	/* What is asked for */
	protected double				vlin, vlat, vrot;
	protected double				pan, tilt;

	/* Where it is */
	protected KineModel				model;
	protected Leg[]					legs = new Leg[4];
	protected double				phase;					// of the cycle, 0..1
	protected double				period;					// the cycle as it goes now [s]
	protected double[][]			feet = new double[4][3];	// where each foot is now, in the frame of the body

	/** One leg: its geometry from the model, and how its frame stands to the body's. */
	protected class Leg
	{
		String						name;
		KineNode					j1, j2, j3, paw;
		double[]					origin;					// J1 on the body
		boolean						front;					// a foreleg: the elbow points back, the forearm forward
		boolean						mirror;					// its outward side is -y of its frame
		double						s1, s3;					// the sense of J1 and J3: +1 when a positive angle turns about +y (the leg swings back), -1 about -y
		double						d;						// J2 out from J1, along its own axis (the flap)
		double						kx, ky, kz;				// the knee (J3) in the frame of J2, its outward side +y
		double						fx, fz;					// the foot in the frame of J3 (in the plane of the leg)
		double[]					rest = new double[3];	// where the foot rests, on the body
		double						ph;						// its phase in the cycle

		Leg (String name, double ph)
		{
			this.name	= name;
			this.ph		= ph;
			j1			= model.node (name + "_J1");
			j2			= model.node (name + "_J2");
			j3			= model.node (name + "_J3");
			paw			= model.node (name.replace ("LEG", "PAW") + "_SENSOR");
			if ((j1 == null) || (j2 == null) || (j3 == null))
				throw new IllegalArgumentException ("The model has no leg " + name + " (J1, J2, J3)");
			origin		= j1.translation;
			front		= origin[0] > 0.0;
			mirror		= j2.translation[1] < 0.0;
			s1			= (j1.joint.axis[1] < 0.0) ? -1.0 : 1.0;
			s3			= (j3.joint.axis[1] < 0.0) ? -1.0 : 1.0;
			d			= Math.abs (j2.translation[1]);
			kx			= j3.translation[0];
			ky			= mirror ? -j3.translation[1] : j3.translation[1];
			kz			= j3.translation[2];
			fx			= (paw != null) ? paw.translation[0] : 0.0;
			fz			= (paw != null) ? paw.translation[2] : -j3.length ();

			boolean	left = (origin[1] + j2.translation[1]) > 0.0;			// J1 may sit on the middle of the body: J2 says which side

			rest[0]		= origin[0] + (front ? foreCenterX : hindCenterX);
			rest[1]		= (left ? 1.0 : -1.0) * (front ? foreWidth : hindWidth);
			rest[2]		= -(front ? foreHeight : hindHeight);
		}

		/**
		 * The joints that put the foot at a point of the body's frame: the target
		 * taken to the frame of J1 (mirrored for a leg whose outward side is -y, so
		 * that one solution serves all four), then the inverse kinematics of a swing
		 * (J1, about y), a flap (J2, about x) and a knee (J3, about y), the knee and
		 * the foot where the model has them in their frames -- a little forward or
		 * back and out of the axis of the segment, as the Aibo's are -- and not on
		 * a straight line. The angles are worked out about +y (positive: the
		 * leg swings back, the shank folds back) and given to each joint in its own
		 * sense. The knee folds the way the GermanTeam walks the Aibo: a hind leg
		 * bends its knee forward, the shank going back to the paw; a foreleg bends
		 * its elbow back, the forearm going forward to the paw, so that it lies on
		 * the ground. Out of reach, the leg stretches towards the point.
		 */
		void reach (double[] p)
		{
			double	x = p[0] - origin[0], y = p[1] - origin[1], z = p[2] - origin[2];

			if (mirror)		y = -y;

			// how far the foot is from J2, which J1 (about y, through J2's own axis) does not change:
			// the knee bends so that the knee and the foot, both offset in the frame of J2, span it
			double	S = x * x + y * y + z * z - 2.0 * d * y + d * d;								// |q|^2, from J2 to the foot in its frame
			double	A = Math.hypot (kx, kz), B = Math.hypot (fx, fz);								// upper and lower leg, in the plane of the leg
			double	pa = Math.atan2 (kx, -kz), pb = Math.atan2 (fx, -fz);							// where each points from straight down (forward positive)
			double	c3 = (S - ky * ky - A * A - B * B) / (2.0 * A * B);

			c3	= Math.max (-1.0, Math.min (1.0, c3));

			double	bend = (front ? -1.0 : 1.0) * Math.acos (c3);									// the shank folds back (hind) or forward (fore)
			double	t3 = bend - (pa - pb);															// the knee, the offsets of the two segments taken out
			// the foot in the frame of J2 before the flap: the knee, and the shank turned by the knee
			double	qx = kx + fx * Math.cos (t3) + fz * Math.sin (t3);
			double	qz = kz - fx * Math.sin (t3) + fz * Math.cos (t3);
			double	qy = ky;
			// the flap, from how far out the foot is: y - d = qy cos t2 - qz sin t2
			double	R = Math.hypot (qy, qz);
			double	c2 = (R > 1e-9) ? Math.max (-1.0, Math.min (1.0, (y - d) / R)) : 0.0;
			double	t2 = Math.atan2 (-qz, qy) - Math.acos (c2);
			// the swing, from where the whole leg points against where the foot is asked for
			double	vx = qx, vz = qy * Math.sin (t2) + qz * Math.cos (t2);
			double	t1 = Math.atan2 (vx, -vz) - Math.atan2 (x, -z);

			model.setAngle (j1.name, s1 * t1);
			model.setAngle (j2.name, t2);
			model.setAngle (j3.name, s3 * t3);
		}
	}

	public AiboWalking (KineModel model)
	{
		this.model	= model;
		for (int i = 0; i < 4; i++)
		{
			legs[i]	= new Leg (LEGS[i], PHASES[i]);
			System.arraycopy (legs[i].rest, 0, feet[i], 0, 3);
		}
		period	= stepDuration;
		stand ();
	}

	public KineModel model ()								{ return model; }

	/** The speed asked for: forward and to the left (m/s), about the vertical (rad/s), cut down to what the gait allows. */
	public void setVelocities (double vlin, double vlat, double vrot)
	{
		double	v = Math.sqrt (vlin * vlin + vlat * vlat);

		if (v > maxSpeed)		{ vlin *= maxSpeed / v;	vlat *= maxSpeed / v; }
		this.vlin	= vlin;
		this.vlat	= vlat;
		this.vrot	= Math.max (-maxTurn, Math.min (maxTurn, vrot));
	}

	/** Where the camera is pointed: pan to the left and tilt up (rad). */
	public void setHead (double pan, double tilt)
	{
		this.pan	= pan;
		this.tilt	= tilt;
	}

	public double vlin ()									{ return vlin; }
	public double vlat ()									{ return vlat; }
	public double vrot ()									{ return vrot; }
	public double phase ()									{ return phase; }
	public boolean walking ()								{ return (Math.abs (vlin) > 1e-4) || (Math.abs (vlat) > 1e-4) || (Math.abs (vrot) > 1e-4); }

	/**
	 * How the body pitches to stand on its four feet (rad, positive nose down):
	 * the front feet are carried higher under the body than the hind ones (or
	 * lower), and the body tilts by the slope between them.
	 */
	public double pitch ()
	{
		Leg		f = null, h = null;

		for (Leg l : legs)		{ if (l.front && (f == null)) f = l;	if (!l.front && (h == null)) h = l; }
		if ((f == null) || (h == null))		return 0.0;
		return Math.atan2 (f.rest[2] - h.rest[2], f.rest[0] - h.rest[0]);
	}

	/** How high the origin of the body is over the ground with this gait (m), pitched as {@link #pitch} says: the feet at rest touch it. */
	public double height ()
	{
		double	p = pitch (), s = Math.sin (p), c = Math.cos (p), low = 0.0;

		for (Leg l : legs)		low = Math.min (low, -l.rest[0] * s + l.rest[2] * c);
		return -low;
	}

	/** Where a foot is now in the frame of the body (m), by leg (see {@link #LEGS}). */
	public double[] foot (int leg)							{ return feet[leg]; }

	/** Every foot at rest and the head where it is pointed; the cycle starts over. */
	public void stand ()
	{
		phase	= 0.0;
		for (int i = 0; i < 4; i++)
		{
			System.arraycopy (legs[i].rest, 0, feet[i], 0, 3);
			legs[i].reach (feet[i]);
		}
		head ();
		model.forward ();
	}

	/**
	 * So much time goes by: the cycle advances, every foot goes to where its path
	 * has it, the head to where it is pointed, and the joints of the model with
	 * them. Standing still, the feet come to rest.
	 */
	public void step (double dt)
	{
		if (!walking ())
		{
			// the feet back to rest, and the cycle with them, so that the next walk starts clean
			boolean	home = true;

			for (int i = 0; i < 4; i++)
			{
				for (int k = 0; k < 3; k++)
				{
					double	e = legs[i].rest[k] - feet[i][k];

					if (Math.abs (e) > 0.002)		{ home = false;	feet[i][k] += e * Math.min (1.0, 6.0 * dt); }
					else							feet[i][k] = legs[i].rest[k];
				}
				legs[i].reach (feet[i]);
			}
			if (home)		phase = 0.0;
			head ();
			model.forward ();
			return;
		}

		// the cycle: shorter when the stride asked for would be too long for a step
		double	stride = 0.0;

		for (int i = 0; i < 4; i++)
		{
			double[]	v = groundSpeed (legs[i]);

			stride	= Math.max (stride, Math.sqrt (v[0] * v[0] + v[1] * v[1]) * stepDuration * groundRatio);
		}
		period	= (stride > maxStep) ? stepDuration * maxStep / stride : stepDuration;
		phase	= (phase + dt / period) % 1.0;

		for (int i = 0; i < 4; i++)
		{
			Leg			leg = legs[i];
			double		p = (phase + leg.ph) % 1.0;								// where this leg is in its own cycle
			double[]	v = groundSpeed (leg);
			double		ground = period * groundRatio;								// how long it is down
			double		dx = v[0] * ground, dy = v[1] * ground;						// how far it is dragged while down

			if (p < groundRatio)
			{
				// on the ground: dragged at the speed a foot has there (against the body's), from half a step before rest to half a step after
				double	s = p / groundRatio;

				feet[i][0]	= leg.rest[0] + dx * (s - 0.5);
				feet[i][1]	= leg.rest[1] + dy * (s - 0.5);
				feet[i][2]	= leg.rest[2];
			}
			else
			{
				// in the air: back the other way along a lifted arc, eased at the ends, to where the next step starts
				double	s = (p - groundRatio) / (1.0 - groundRatio);
				double	e = 0.5 - 0.5 * Math.cos (Math.PI * s);

				feet[i][0]	= leg.rest[0] - dx * (e - 0.5);
				feet[i][1]	= leg.rest[1] - dy * (e - 0.5);
				feet[i][2]	= leg.rest[2] + footLift * Math.sin (Math.PI * s);
			}
			leg.reach (feet[i]);
		}
		head ();
		model.forward ();
	}

	/**
	 * How fast a foot on the ground moves in the frame of the body: against the
	 * body's own speed, the turn included -- a foot away from the middle of the
	 * body goes round it.
	 */
	protected double[] groundSpeed (Leg leg)
	{
		return new double[] { -(vlin - vrot * leg.rest[1]), -(vlat + vrot * leg.rest[0]) };
	}

	/**
	 * The head where the camera is pointed: the pan on HEAD_PAN; the tilt on
	 * HEAD_TILT from the level (where it makes up for the lean of the neck: +30
	 * deg on a neck at -30), and what that joint cannot take on NECK_TILT. Both
	 * joints look up when turned positive, as the Aibo's do.
	 */
	protected void head ()
	{
		KineNode	neck = model.node ("NECK_TILT"), ht = model.node ("HEAD_TILT");

		model.setAngle ("HEAD_PAN", pan);
		if ((neck == null) || (ht == null))		return;

		double	level = -neck.joint.def;											// the head tilt that levels the head on the neck at rest
		double	want = level + tilt;												// a positive turn of HEAD_TILT looks up
		double	got = ht.joint.clamp (want);

		model.setAngle (ht.name, got);
		model.setAngle (neck.name, neck.joint.def + (want - got));					// the rest of the tilt, on the neck
	}
}
