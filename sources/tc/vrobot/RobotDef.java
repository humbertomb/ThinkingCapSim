/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tc.vrobot;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Description of a robot platform (a <code>.robot</code> file): its drawing,
 * its kinematics, its sensors and the devices it carries. It is the model the
 * robot editor works with, persisted as JSON.
 *
 * The runtime still reads a robot description as a flat set of properties
 * ({@link RobotDesc}, {@link RobotModel}, tclib.utils.fusion.FusionDesc, the
 * drivers), and a description travels to the modules that way, so
 * {@link #toProperties()} rebuilds them. Keys the model does not describe (the
 * errors of the simulation, the fusion, the layers, ...) are kept in
 * {@link #extra} and travel with them.
 */
public class RobotDef
{
	static public final String		EXTENSION	= "robot";

	/**
	 * A sensor on the robot: where it sits, in the polar coordinates
	 * {@link SensorPos} uses (rho, theta), where it looks at (orientation), and,
	 * for the families whose sensors are devices of their own
	 * ({@link RobotDef#hasOwnDetection(String)}: laser range finders, laser beacon
	 * scanners and radar trackers), the device it is read through and what it
	 * detects. A robot can then carry, say, two laser range finders of different
	 * makes and reaches; for the other families these are zero and what the
	 * {@link Family} says applies to all of them.
	 */
	static public class Sensor
	{
		public double	rho;						// distance from the centre of the robot (m, "<fam>len")
		public double	theta;						// angle of that distance (deg, "<fam>rho")
		public double	height;						// height over the floor (m, "<fam>hgt")
		public double	orientation;				// direction the sensor looks at over the horizontal plane (deg, "<fam>feat")
		public double	elevation;					// direction it looks at over the vertical plane (deg, "<fam>elev")
		public int		step;						// reading step ("<fam>step")

		// what it detects (families with sensors of their own only)
		public String	driver;						// class of the device it is read through
		public String	driverParams;				// what that driver is opened with (a port, an address, ...)
		public double	rangemax;					// maximum range (m)
		public double	rangemin;					// minimum range (m)
		public double	cone;						// aperture (deg)
		public int		rays;						// rays of a scan
		public double	reflect;					// lsb: maximum reflection angle (deg)
		public int		beacons;					// lsb: beacons it can see at once
		public int		objects;					// trk: objects it can track at once
		public double	hfov;						// vis: horizontal field of view (deg; it is not a cone)
		public double	vfov;						// vis: vertical field of view (deg)

		public Sensor ()							{ }
		public Sensor copy ()
		{
			Sensor	s = new Sensor ();
			s.rho = rho;	s.theta = theta;	s.height = height;	s.orientation = orientation;	s.step = step;
			s.elevation = elevation;
			s.driver = driver;	s.driverParams = driverParams;
			s.rangemax = rangemax;	s.rangemin = rangemin;	s.cone = cone;	s.rays = rays;
			s.reflect = reflect;	s.beacons = beacons;	s.objects = objects;
			s.hfov = hfov;			s.vfov = vfov;
			return s;
		}

		/** True when nothing of what it detects is set (so none of it is written to the file). */
		public boolean plain ()
		{
			return (driver == null) && (driverParams == null) && (rangemax == 0.0) && (rangemin == 0.0) && (cone == 0.0)
				&& (rays == 0) && (reflect == 0.0) && (beacons == 0) && (objects == 0)
				&& (hfov == 0.0) && (vfov == 0.0);
		}
	}

	/**
	 * A family of range sensors: where each one is and, for the families whose
	 * sensors are all alike (sonars, infrared, vision), what they reach. For the
	 * others every sensor says that for itself and only the firing cycle, which is
	 * always of the whole family, is kept here.
	 */
	/**
	 * A virtual sensor: a sector that stands for a group of the physical ones, so
	 * that a controller reads one distance where the robot has a dozen sensors.
	 *
	 * It sits where a real sensor sits, said the same way (polar), and covers what
	 * a real sensor covers. The rest of what the fusion of the runtime works it out
	 * with -- how it fuses (mode), the sensors it fuses and their weights (equ) and
	 * the width of the rectangle the buffer modes sweep (base) -- is kept as it was
	 * given, though the editor does not show it yet.
	 *
	 * The properties of the older files name these badly: what they call len is the
	 * distance, rho the angle of it and feat where it looks.
	 */
	/**
	 * What a virtual sensor is, whichever kind: something that sits where a real
	 * sensor sits, said the same way (polar), and covers what a real sensor
	 * covers, but whose reading is worked out from the other sensors instead of
	 * being taken from the world.
	 */
	static public class Sector
	{
		public double	rho;						// distance from the centre of the robot (m)
		public double	theta;						// angle of that distance (deg)
		public double	height;						// height over the floor (m)
		public double	orientation;				// direction it looks at over the horizontal plane (deg)
		public double	elevation;					// direction it looks at over the vertical plane (deg)
		public double	rangemax;					// how far it reaches (m)
		public double	rangemin;					// and from how near (m)
		public double	cone;						// aperture (deg)

		/** Copies into another what every virtual sensor has. */
		protected void copyTo (Sector o)
		{
			o.rho = rho;			o.theta = theta;		o.height = height;
			o.orientation = orientation;				o.elevation = elevation;
			o.rangemax = rangemax;	o.rangemin = rangemin;	o.cone = cone;
		}
	}

	/**
	 * A sensor of an area: the sector that stands for a group of the real ones, so
	 * that a controller reads one distance where the robot has a dozen sensors.
	 *
	 * The rest of what the fusion of the runtime works it out with -- how it fuses
	 * (mode), the sensors it fuses and their weights (equ) and the width of the
	 * rectangle the buffer modes sweep (base) -- is kept as it was given, though
	 * the editor does not show it yet.
	 *
	 * The properties of the older files name these badly: what they call len is
	 * the distance, rho the angle of it and feat where it looks.
	 */
	static public class Group extends Sector
	{
		public int		mode;						// "groupmode"
		public String	equ;						// "groupequ": the sensors it fuses and their weights
		public double	base;						// "groupbase": width of the rectangle of the buffer modes (m)

		public Group ()								{ }
		public Group copy ()
		{
			Group	g = new Group ();
			copyTo (g);
			g.mode = mode;			g.equ = equ;			g.base = base;
			return g;
		}
	}

	/**
	 * A fused sensor: one reading in one direction, taken from the real sensors
	 * that look that way -- the nearest sonar and the nearest infrared -- fused as
	 * its mode says (the sonar, the infrared, the nearer of the two, a filter, or
	 * Flynn's rule). They are what the sensors of an area are worked out from.
	 *
	 * Named "virtu" in the older files, which name them as badly as the others:
	 * len is the distance, rho the angle of it and feat where it looks.
	 */
	static public class Fused extends Sector
	{
		public int		mode;						// "virtumode"

		public Fused ()								{ }
		public Fused copy ()
		{
			Fused	f = new Fused ();
			copyTo (f);
			f.mode = mode;
			return f;
		}
	}

	static public class Family
	{
		public double		rangemax;				// maximum range (m)
		public double		rangemin;				// minimum range (m)
		public double		cone;					// aperture (deg)
		public int			cycle;					// firing cycle
		public int			rays;					// rays of a scan (lrf, lsb)
		public double		reflect;				// lsb: maximum reflection angle (deg)
		public int			beacons;				// lsb: beacons it can see at once
		public int			objects;				// tracker: tracked objects
		public String		driver;					// class of the device every sensor of the family is read through
		public String		driverParams;			// what that driver is opened with (a port, an address, ...)
		public int			simmode;				// how the simulator works a reading out ("MODESON", "MODEIR", ...)
		public List<Sensor>	sensors	= new ArrayList<Sensor> ();

		/** True when its sensors say what they detect: only the cycle is of the family. */
		transient boolean	own;

		public int n ()								{ return sensors.size (); }

		public Family copy ()
		{
			Family	f = new Family ();
			f.rangemax = rangemax;	f.rangemin = rangemin;	f.cone = cone;		f.cycle = cycle;
			f.rays = rays;		f.reflect = reflect;	f.beacons = beacons;	f.objects = objects;
			f.driver = driver;	f.driverParams = driverParams;	f.own = own;
			f.simmode = simmode;
			for (Sensor s : sensors)		f.sensors.add (s.copy ());
			return f;
		}
	}

	/**
	 * A wheel of the drive train: where it sits, in the coordinates of the robot
	 * (x, y, z); which way its plane points (orientation); how big it is; and what
	 * it does -- whether it can be steered and whether it drives.
	 *
	 * A wheel is placed in x and y rather than in the polar coordinates a sensor
	 * uses, because a wheel base is measured off the drawing of the platform, and
	 * saying "0.2 m to the left of the axle" in a distance and an angle is neither
	 * easy nor exact.
	 *
	 * The aim is that the wheels say most of what the kinematics needs, so that a
	 * platform is described by drawing it rather than by filling in numbers.
	 */
	static public class Wheel
	{
		public double	x;							// where it sits, in the coordinates of the robot (m)
		public double	y;
		public double	z;							// height of its centre over the floor (m)
		public double	orientation;				// direction it rolls towards (deg)
		public double	radius;						// radius of the wheel (m)
		public double	width;						// width of its tread (m)
		public boolean	steerable;					// it can be steered
		public double	maxsteer;					// how far it can be steered, to each side (deg; steerable only)
		public double	maxturning;					// how fast it can be steered (deg/s; steerable only)
		public boolean	traction;					// it drives
		public double	maxrpm;						// how fast it can turn (rev/min; driving wheels only)

		public Wheel ()								{ }
		public Wheel copy ()
		{
			Wheel	w = new Wheel ();
			w.x = x;			w.y = y;				w.z = z;		w.orientation = orientation;
			w.radius = radius;	w.width = width;
			w.steerable = steerable;	w.maxsteer = maxsteer;	w.maxturning = maxturning;
			w.traction = traction;		w.maxrpm = maxrpm;
			return w;
		}
	}

	/** A bumper: the segment of the platform it protects, in robot coordinates. */
	static public class Bumper
	{
		public double	xi, yi, xf, yf;

		public Bumper ()												{ }
		public Bumper (double xi, double yi, double xf, double yf)		{ this.xi = xi; this.yi = yi; this.xf = xf; this.yf = yf; }
		public Bumper copy ()											{ return new Bumper (xi, yi, xf, yf); }
	}

	/** A segment of the robot drawing, in robot coordinates. */
	static public class IconLine
	{
		public double	xi, yi, xf, yf;

		public IconLine ()												{ }
		public IconLine (double xi, double yi, double xf, double yf)	{ this.xi = xi; this.yi = yi; this.xf = xf; this.yf = yf; }
		public IconLine copy ()											{ return new IconLine (xi, yi, xf, yf); }
	}

	/**
	 * Kinematics and dynamics of the platform: what has to be given, and nothing
	 * else. Whatever the drive train can say -- how far apart the axles are, how
	 * big the wheel is, how fast the platform goes and turns -- is not here and is
	 * not kept: {@link RobotDef#derived(String)} works it out off the wheels
	 * whenever it is asked for, so it cannot fall out of step with them.
	 */
	static public class Kinematics
	{
		public String	drive		= "tc.vrobot.models.DifferentialDrive";	// DRIVEMODEL
		public double	lamax;						// maximum acceleration (m/s2)
		public double	ldmax;						// maximum deceleration (m/s2)
		public double	rwheel;						// RWHEEL (m): the trail of the steering wheel
		public double	skid		= 1.0;			// SKID: effective track over the geometric one (skid steer)
		public double	gear;						// GEAR
		public double	pulses;						// PULSES
		public long		dtime	= 100;				// control cycle (ms)
		public double	odomET, odomER, odomBias;	// odometry errors of the simulation

		public Kinematics copy ()
		{
			Kinematics	k = new Kinematics ();
			k.drive = drive;	k.lamax = lamax;	k.ldmax = ldmax;
			k.rwheel = rwheel;	k.skid = skid;		k.gear = gear;		k.pulses = pulses;		k.dtime = dtime;
			k.odomET = odomET;	k.odomER = odomER;	k.odomBias = odomBias;
			return k;
		}
	}

	/* ------------------------------------------------------------------ */

	public String				name;												// name of the platform (the file name by default)
	public double				radius;												// RADIUS (m)
	public List<IconLine>		icon		= new ArrayList<IconLine> ();			// drawing of the robot
	public String				image;												// IMAGE (2D bitmap, optional)
	public String				shapeRobot;											// V3DFILE (3D model of the platform)
	public String				shapeActuator;										// V3DLIFT (3D model of its actuator: the fork, the arm, ...)
	public Kinematics			kinematics	= new Kinematics ();
	public Map<String, Family>	sensors		= new LinkedHashMap<String, Family> ();	// by family prefix: son, ir, lrf, lsb, trk, vis
	public List<Bumper>			bumpers		= new ArrayList<Bumper> ();
	public List<Wheel>			wheels		= new ArrayList<Wheel> ();				// the drive train
	public List<Group>			groups		= new ArrayList<Group> ();				// the virtual sensors of an area
	public List<Fused>			fused		= new ArrayList<Fused> ();				// the fused ones, of a direction
	public Map<String, String>	extra		= new LinkedHashMap<String, String> ();	// everything else of the description (CAN, layers, fusion, ...)

	protected transient File	file;												// where it was loaded from / saved to
	protected transient String	original;											// JSON as loaded or saved (to detect changes)

	/** Families of range sensors, by the prefix their properties use. */
	static public final String[]	FAMILIES		= { "son", "ir", "lrf", "lsb", "trk", "vis" };
	static public final String[]	FAMILY_NAMES	= { "Sonars", "Infrared", "Laser range finders", "Laser beacon scanners", "Radar trackers", "Vision" };
	/** Property suffix of the count of each family (MAXSONAR, MAXIR, ...). */
	static public final String[]	FAMILY_COUNTS	= { "MAXSONAR", "MAXIR", "MAXLRF", "MAXLSB", "MAXTRACKER", "MAXVISION" };
	/** Suffix the range properties of each family use (RANGESON, RANGEIR, ...). */
	static public final String[]	FAMILY_KEYS		= { "SON", "IR", "LRF", "LSB", "TRK", "VIS" };
	/** How the simulator works a reading of a family out, where it has a say ("MODESON", ...); null where it has none. */
	static public final String[]	FAMILY_MODES	= { "MODESON", "MODEIR", "MODELRF", "MODELSB", null, null };

	/** True for a family whose readings the simulator works out in a way that can be chosen. */
	static public boolean hasSimMode (String fam)
	{
		int		i = familyIndex (fam);

		return (i >= 0) && (FAMILY_MODES[i] != null);
	}

	/** The property the simulator reads that choice from, or null. */
	static public String simModeKey (String fam)
	{
		int		i = familyIndex (fam);

		return (i >= 0) ? FAMILY_MODES[i] : null;
	}
	/** Prefix of the driver property of each family (LRF0, LSB0, ...); null when the family has none. */
	static public final String[]	FAMILY_DRIVERS	= { "SONAR", "IR", "LRF", "LSB", "TRK", "VISION" };

	/**
	 * The class every driver of a family derives from; null when the family has
	 * none. A driver is named by a class that extends one of these, so what a
	 * robot can be given is what the development holds, and the editor offers it
	 * instead of asking for a name.
	 */
	static public final String[]	FAMILY_BASES	= { null, null,
														"devices.drivers.laser.Laser",
														"devices.drivers.beacon.LaserBeacon",
														"devices.drivers.radar.Radar",
														"devices.drivers.vision.Vision" };

	/** The class every kinematics model of a platform derives from. */
	static public final String		DRIVE_BASE		= "tc.vrobot.RobotModel";

	/** What every model reads, whichever it is, and what the platform itself says. */
	static private final String[]	KIN_COMMON		= { "drive", "drivetype", "vmax", "rmax", "dtime",
														"odomet", "odomer", "odombias" };

	/** What each model reads beyond that: a differential drive knows nothing of a steering wheel. */
	static private final Map<String, String[]>	KIN_MODELS = kinModels ();

	static private Map<String, String[]> kinModels ()
	{
		Map<String, String[]>	m = new LinkedHashMap<String, String[]> ();

		m.put ("tc.vrobot.models.SynchroDrive",		new String[] { "samax", "lamax", "ldmax" });
		m.put ("tc.vrobot.models.DifferentialDrive",	new String[] { "base", "wheeldiameter", "gear", "pulses" });
		m.put ("tc.vrobot.models.SkidSteerDrive",		new String[] { "base", "wheeldiameter", "gear", "pulses", "skid" });
		m.put ("tc.vrobot.models.AckermanDrive",		new String[] { "samax", "length" });
		m.put ("tc.vrobot.models.TricycleDrive",		new String[] { "samax", "lamax", "ldmax",
																	   "length", "base", "rwheel" });
		return m;
	}

	/**
	 * What the drive train says on its own, so that it is read off the wheels
	 * instead of being typed in twice: the geometry of the platform, how far it
	 * steers and how fast its motors take it. The rest of the kinematics -- the
	 * speeds of the platform, the accelerations, the encoders -- is not the
	 * wheels' to say and stays as it is given.
	 */
	static private final String[]	KIN_DERIVED		= { "length", "base", "wheeldiameter", "vmax", "rmax", "samax" };

	/** True for a kinematics property the wheels of the platform work out. */
	static public boolean isCalculated (String name)
	{
		if (name == null)						return false;
		name	= name.replace (" ", "").toLowerCase ();
		for (String k : KIN_DERIVED)			if (k.equals (name))	return true;
		return false;
	}

	/**
	 * What the wheels say a geometry parameter is, or null when they cannot say
	 * it: a platform with no wheels, or none of the kind the parameter is measured
	 * between, keeps the value it was given.
	 *
	 * The wheels that can be steered make up the steering axle and the rest the
	 * fixed one, and the ones that drive give the size of the driving wheel:
	 *
	 *   length -- the wheel base: how far apart the two axles are along x
	 *   base   -- for a differential drive, how far apart the two driving wheels
	 *             are across; for a tricycle, how far the fixed axle is from the
	 *             origin along x, which is what its model measures
	 *   wheel diameter -- of the driving wheel, which is what its model asks for
	 *
	 * And what the wheels can do bounds what the platform can do, so the most
	 * restrictive of them has the say:
	 *
	 *   vmax   -- how fast the platform goes: what a driving wheel gives, from how
	 *             fast it spins and how big it is
	 *   rmax   -- how fast it turns: for a differential drive, both wheels at full
	 *             speed the other way; for a steered one, going flat out with the
	 *             wheel hard over, each as its own model works it out
	 *   samax  -- how fast the steering wheels are turned
	 */
	public Double derived (String name)
	{
		List<Wheel>		turning, fixed, driving;

		if ((wheels == null) || wheels.isEmpty ())		return null;
		turning	= steerables ();
		fixed	= fixedWheels ();
		driving	= driving ();

		name	= name.replace (" ", "").toLowerCase ();
		if (name.equals ("wheeldiameter"))
		{
			double	r = 0.0;
			for (Wheel w : driving)		r += w.radius;
			r	/= driving.size ();
			return (r > 0.0) ? Double.valueOf (2 * r) : null;
		}
		if (name.equals ("length"))
		{
			if (turning.isEmpty () || fixed.isEmpty ())		return null;
			return Double.valueOf (Math.abs (meanX (turning) - meanX (fixed)));
		}
		if (name.equals ("vmax"))		return speed (driving);
		if (name.equals ("samax"))		return steerRate (turning);
		if (name.equals ("rmax"))
		{
			Double	v = speed (driving);
			Double	s = steering (turning);
			double	l = 0.0, b = 0.0;

			if (v == null)												return null;
			if ("tc.vrobot.models.DifferentialDrive".equals (kinematics.drive)
					|| "tc.vrobot.models.SkidSteerDrive".equals (kinematics.drive))
			{
				Double	base = derived ("base");						// both sides at full speed the other way
				if (base != null)		b = base.doubleValue ();
				if ("tc.vrobot.models.SkidSteerDrive".equals (kinematics.drive))
					b	*= kinematics.skid;								// it drags its wheels: a wider track
				if (b <= 0.0)											return null;
				return Double.valueOf (Math.toDegrees (2 * v.doubleValue () / b));
			}
			if ("tc.vrobot.models.SynchroDrive".equals (kinematics.drive))
				return steerRate (wheels);						// it turns the whole drive train at once
			if (s == null)												return null;
			Double	len = derived ("length");
			if (len != null)		l = len.doubleValue ();
			if (l <= 0.0)												return null;
			if ("tc.vrobot.models.AckermanDrive".equals (kinematics.drive))
				return Double.valueOf (Math.toDegrees (v.doubleValue () * Math.tan (Math.toRadians (s.doubleValue ())) / l));
			if ("tc.vrobot.models.TricycleDrive".equals (kinematics.drive))
				return Double.valueOf (Math.toDegrees (v.doubleValue () * Math.sin (Math.toRadians (s.doubleValue ())) / l));
			return null;											// a model nobody here knows about
		}
		if (name.equals ("base"))
		{
			if ("tc.vrobot.models.TricycleDrive".equals (kinematics.drive))
			{
				if (fixed.isEmpty ())						return null;
				return Double.valueOf (0.0 - meanX (fixed) + 0.0);		// how far the axle is from the origin
			}
			if (driving.size () < 2)						return null;
			return Double.valueOf (spreadY (driving));		// how far apart the driving wheels are
		}
		return null;
	}

	/** The wheels that can be steered: the steering axle. */
	public List<Wheel> steerables ()
	{
		List<Wheel>		out = new ArrayList<Wheel> ();

		if (wheels != null)
			for (Wheel w : wheels)		if (w.steerable)		out.add (w);
		return out;
	}

	/** The wheels that cannot: the fixed axle. */
	public List<Wheel> fixedWheels ()
	{
		List<Wheel>		out = new ArrayList<Wheel> ();

		if (wheels != null)
			for (Wheel w : wheels)		if (!w.steerable)		out.add (w);
		return out;
	}

	/** The wheels that drive, or all of them when none says it does. */
	public List<Wheel> driving ()
	{
		List<Wheel>		out = new ArrayList<Wheel> ();

		if (wheels == null)				return out;
		for (Wheel w : wheels)			if (w.traction)			out.add (w);
		return out.isEmpty () ? new ArrayList<Wheel> (wheels) : out;
	}

	/** How fast the driving wheels take the platform, at the slowest of them (m/s), or null. */
	static private Double speed (List<Wheel> ws)
	{
		double		lo = Double.MAX_VALUE;

		for (Wheel w : ws)
			if ((w.maxrpm > 0.0) && (w.radius > 0.0))
				lo	= Math.min (lo, (w.maxrpm / 60.0) * 2 * Math.PI * w.radius);
		return (lo < Double.MAX_VALUE) ? Double.valueOf (lo) : null;
	}

	/** How fast the steering wheels are turned, at the slowest of them (deg/s), or null. */
	static private Double steerRate (List<Wheel> ws)
	{
		double		lo = Double.MAX_VALUE;

		for (Wheel w : ws)		if (w.maxturning > 0.0)		lo = Math.min (lo, w.maxturning);
		return (lo < Double.MAX_VALUE) ? Double.valueOf (lo) : null;
	}

	/** How far the steering wheels turn, at the most restrictive of them (deg), or null. */
	static private Double steering (List<Wheel> ws)
	{
		double		lo = Double.MAX_VALUE;

		for (Wheel w : ws)		if (w.maxsteer > 0.0)		lo = Math.min (lo, w.maxsteer);
		return (lo < Double.MAX_VALUE) ? Double.valueOf (lo) : null;
	}

	static private double meanX (List<Wheel> ws)
	{
		double		s = 0.0;
		for (Wheel w : ws)		s += w.x;
		return s / ws.size ();
	}

	static private double spreadY (List<Wheel> ws)
	{
		double		lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
		for (Wheel w : ws)		{ lo = Math.min (lo, w.y);	hi = Math.max (hi, w.y); }
		return hi - lo;
	}


	/**
	 * True when a kinematics property says something to a model. A model nobody
	 * here knows about is taken to read everything, so that a model of one's own
	 * is not left without its parameters.
	 */
	static public boolean usesKinematics (String drive, String name)
	{
		String[]	own;

		if (name == null)						return false;
		name	= name.replace (" ", "").toLowerCase ();
		for (String k : KIN_COMMON)				if (k.equals (name))	return true;
		own		= (drive == null) ? null : KIN_MODELS.get (drive.trim ());
		if (own == null)						return true;
		for (String k : own)					if (k.equals (name))	return true;
		return false;
	}

	/** The class the drivers of a family derive from, or null when there is none. */
	static public String driverBase (String fam)
	{
		int		i = familyIndex (fam);
		return (i < 0) ? null : FAMILY_BASES[i];
	}

	/**
	 * Families whose sensors are devices of their own: each one says what it
	 * detects (driver, range, cone, rays, ...) instead of taking it from the
	 * family. Only the firing cycle stays with the family.
	 */
	static public final boolean[]	FAMILY_OWN		= { false, false, true, true, true, true };

	/**
	 * True for a family that sees a rectangle and not a cone: a camera says a
	 * horizontal and a vertical field of view instead of an aperture, and its near
	 * limit is always zero.
	 */
	static public boolean hasFov (String fam)			{ return "vis".equals (fam); }

	/** True for a family whose sensors carry their own detection properties. */
	static public boolean hasOwnDetection (String fam)
	{
		int		i = familyIndex (fam);
		return (i >= 0) && FAMILY_OWN[i];
	}

	static public String familyName (String fam)
	{
		for (int i = 0; i < FAMILIES.length; i++)
			if (FAMILIES[i].equals (fam))		return FAMILY_NAMES[i];
		return fam;
	}

	static private int familyIndex (String fam)
	{
		for (int i = 0; i < FAMILIES.length; i++)
			if (FAMILIES[i].equals (fam))		return i;
		return -1;
	}

	/* Construction */

	static protected Gson gson ()
	{
		return new GsonBuilder ().setPrettyPrinting ().disableHtmlEscaping ()
					.registerTypeAdapter (Family.class, new FamilyWriter ())
					.registerTypeAdapter (Sensor.class, new SensorWriter ()).create ();
	}

	/**
	 * Writes a family leaving out what it does not say: the detection properties
	 * of a family whose sensors carry their own (the laser range finders, the
	 * beacon scanners and the radar trackers keep only their firing cycle), and
	 * anything left at its default in the others.
	 */
	static private class FamilyWriter implements com.google.gson.JsonSerializer<Family>
	{
		public com.google.gson.JsonElement serialize (Family f, java.lang.reflect.Type type, com.google.gson.JsonSerializationContext ctx)
		{
			com.google.gson.JsonObject	o = new com.google.gson.JsonObject ();

			if (!f.own)
			{
				if (f.driver != null)		o.addProperty ("driver", f.driver);
				if (f.driverParams != null)	o.addProperty ("driverParams", f.driverParams);
				if (f.rangemax != 0.0)		o.addProperty ("rangemax", f.rangemax);
				if (f.rangemin != 0.0)		o.addProperty ("rangemin", f.rangemin);
				if (f.cone != 0.0)			o.addProperty ("cone", f.cone);
				if (f.rays != 0)			o.addProperty ("rays", f.rays);
				if (f.reflect != 0.0)		o.addProperty ("reflect", f.reflect);
				if (f.beacons != 0)			o.addProperty ("beacons", f.beacons);
				if (f.objects != 0)			o.addProperty ("objects", f.objects);
			}
			if (f.cycle != 0)				o.addProperty ("cycle", f.cycle);
			o.add ("sensors", ctx.serialize (f.sensors));
			return o;
		}
	}

	/**
	 * Writes a sensor leaving out what it does not detect, so that the sensors of
	 * the families that take their detection from the family (the sonars, say)
	 * read as they always did. What is missing is zero when it is read back.
	 */
	static private class SensorWriter implements com.google.gson.JsonSerializer<Sensor>
	{
		public com.google.gson.JsonElement serialize (Sensor s, java.lang.reflect.Type type, com.google.gson.JsonSerializationContext ctx)
		{
			com.google.gson.JsonObject	o = new com.google.gson.JsonObject ();

			o.addProperty ("rho", s.rho);
			o.addProperty ("theta", s.theta);
			o.addProperty ("height", s.height);
			o.addProperty ("orientation", s.orientation);
			if (s.elevation != 0.0)		o.addProperty ("elevation", s.elevation);
			o.addProperty ("step", s.step);
			if (s.driver != null)		o.addProperty ("driver", s.driver);
			if (s.driverParams != null)	o.addProperty ("driverParams", s.driverParams);
			if (s.rangemax != 0.0)		o.addProperty ("rangemax", s.rangemax);
			if (s.rangemin != 0.0)		o.addProperty ("rangemin", s.rangemin);
			if (s.cone != 0.0)			o.addProperty ("cone", s.cone);
			if (s.rays != 0)			o.addProperty ("rays", s.rays);
			if (s.reflect != 0.0)		o.addProperty ("reflect", s.reflect);
			if (s.beacons != 0)			o.addProperty ("beacons", s.beacons);
			if (s.objects != 0)			o.addProperty ("objects", s.objects);
			if (s.hfov != 0.0)			o.addProperty ("hfov", s.hfov);
			if (s.vfov != 0.0)			o.addProperty ("vfov", s.vfov);
			return o;
		}
	}

	/** An empty description with every family created (and empty). */
	static public RobotDef create ()
	{
		RobotDef	d = new RobotDef ();
		d.name		= "robot";
		d.radius	= 0.25;
		d.normalise ();
		d.original	= d.toJson ();
		return d;
	}

	/** Reads a robot description. */
	static public RobotDef load (File f) throws IOException
	{
		RobotDef	d;
		Reader		in = new FileReader (f, java.nio.charset.StandardCharsets.UTF_8);

		try
		{
			d	= gson ().fromJson (in, RobotDef.class);
			if (d == null)		throw new IOException ("Empty robot description");
		}
		finally { in.close (); }

		d.normalise ();
		if ((d.name == null) || (d.name.trim ().length () == 0))		d.name = baseName (f);
		d.file		= f;
		d.original	= d.toJson ();
		return d;
	}

	static private String baseName (File f)
	{
		String	n = f.getName ();
		int		dot = n.lastIndexOf ('.');
		return (dot > 0) ? n.substring (0, dot) : n;
	}

	public void save (File f) throws IOException
	{
		String	json = toJson ();
		Writer	out = new FileWriter (f, java.nio.charset.StandardCharsets.UTF_8);
		try { out.write (json); out.write ('\n'); }
		finally { out.close (); }
		file		= f;
		original	= json;
	}

	/** The description as JSON, leaving out a drive train with nothing in it. */
	public String toJson ()
	{
		List<Wheel>		w = wheels;
		List<Group>		g = groups;
		List<Fused>		u = fused;

		if ((w != null) && w.isEmpty ())		wheels = null;
		if ((g != null) && g.isEmpty ())		groups = null;
		if ((u != null) && u.isEmpty ())		fused = null;
		try { return gson ().toJson (this); }
		finally { wheels = w;	groups = g;		fused = u; }
	}

	public File getFile ()					{ return file; }
	public boolean isModified ()			{ return (original == null) || !original.equals (toJson ()); }

	public RobotDef copy ()
	{
		RobotDef	d = new RobotDef ();
		d.name			= name;
		d.radius		= radius;
		for (IconLine l : icon)			d.icon.add (l.copy ());
		d.image			= image;
		d.shapeRobot	= shapeRobot;
		d.shapeActuator	= shapeActuator;
		d.kinematics	= kinematics.copy ();
		for (Map.Entry<String, Family> e : sensors.entrySet ())		d.sensors.put (e.getKey (), e.getValue ().copy ());
		for (Bumper b : bumpers)		d.bumpers.add (b.copy ());
		for (Wheel w : wheels)			d.wheels.add (w.copy ());
		for (Group g : groups)			d.groups.add (g.copy ());
		for (Fused f : fused)			d.fused.add (f.copy ());
		d.extra.putAll (extra);
		d.file			= file;
		d.original		= original;
		return d;
	}

	/** Fills what a hand-written or older file may have left out. */
	protected void normalise ()
	{
		if (icon == null)			icon = new ArrayList<IconLine> ();
		if (bumpers == null)		bumpers = new ArrayList<Bumper> ();
		if (wheels == null)			wheels = new ArrayList<Wheel> ();
		if (groups == null)			groups = new ArrayList<Group> ();
		if (fused == null)			fused = new ArrayList<Fused> ();
		if (extra == null)			extra = new LinkedHashMap<String, String> ();
		if (kinematics == null)		kinematics = new Kinematics ();
		if (sensors == null)		sensors = new LinkedHashMap<String, Family> ();
		for (String fam : FAMILIES)
		{
			Family	f = sensors.get (fam);
			if (f == null)			sensors.put (fam, f = new Family ());
			if (f.sensors == null)	f.sensors = new ArrayList<Sensor> ();
			f.own	= hasOwnDetection (fam);
			if (f.own)				migrate (fam, f);
			else
			{
				String[]	d = split (f.driver, f.driverParams);
				f.driver		= d[0];
				f.driverParams	= d[1];
			}
			for (Sensor s : f.sensors)		split (s);
			// how the simulator works its readings out, which used to sit among the
			// properties that are not understood
			if (FAMILY_MODES[familyIndex (fam)] != null)
			{
				String	m = extra.remove (FAMILY_MODES[familyIndex (fam)]);
				if (m != null)		f.simmode = (int) number (m, 0.0);
			}
		}
		if (groups.isEmpty ())		readGroups ();
		if (fused.isEmpty ())		readFused ();
	}

	/**
	 * The virtual sensors of an older description, which named them as loose
	 * properties (groupfeat0, grouplen0, ...) and now sit among the rest of what
	 * is not understood. What is read is taken out of there, so that it is not
	 * written twice.
	 *
	 * What the whole lot of them says (RANGEGROUP, CONEGROUP) stays where it is:
	 * it is read as the default of a sensor that says nothing of its own.
	 */
	protected void readGroups ()
	{
		int			n = 0;
		double		range = number (extra.get ("RANGEGROUP"), 1.0);		// the defaults of the fusion
		double		cone = number (extra.get ("CONEGROUP"), 30.0);

		for (String k : extra.keySet ())
		{
			int		i = groupIndex (k);
			if (i >= n)		n = i + 1;
		}
		for (int i = 0; i < n; i++)
		{
			Group	g = new Group ();

			g.rho			= number (take ("grouplen" + i), 0.0);
			g.theta			= number (take ("grouprho" + i), 0.0);
			g.orientation	= number (take ("groupfeat" + i), g.theta);
			g.rangemax		= number (take ("grouprng" + i), range);
			g.cone			= number (take ("groupcone" + i), cone);
			g.base			= number (take ("groupbase" + i), 0.3);
			g.mode			= (int) number (take ("groupmode" + i), 0.0);
			g.equ			= take ("groupequ" + i);
			groups.add (g);
		}
		extra.remove ("MAXGROUP");						// it is however many there are
	}

	/**
	 * The fused sensors of an older description, named "virtu" there and, like the
	 * others, named badly: len is the distance, rho the angle of it and feat where
	 * it looks. What is read is taken out of the properties that travel along.
	 *
	 * An older description says how far they all reach and how wide they all are
	 * (RANGEVIRTU, CONEVIRTU) and not how far each one does, so each one is read
	 * as reaching what they all do; those two stay where they are, as what a
	 * sensor that says nothing of its own falls back on.
	 */
	protected void readFused ()
	{
		int			n = 0;
		double		range = number (extra.get ("RANGEVIRTU"), 8.0);		// the defaults of the fusion
		double		cone = number (extra.get ("CONEVIRTU"), 20.0);

		for (String k : extra.keySet ())
		{
			int		i = fusedIndex (k);
			if (i >= n)		n = i + 1;
		}
		for (int i = 0; i < n; i++)
		{
			Fused	f = new Fused ();

			f.rho			= number (take ("virtulen" + i), 0.0);
			f.theta			= number (take ("virturho" + i), 0.0);
			f.orientation	= number (take ("virtufeat" + i), f.theta);
			f.rangemax		= number (take ("virturng" + i), range);
			f.cone			= number (take ("virtucone" + i), cone);
			f.mode			= (int) number (take ("virtumode" + i), -1.0);
			fused.add (f);
		}
		extra.remove ("MAXVIRTU");						// it is however many there are
	}

	/** The fused sensor a property of an older description belongs to, or -1. */
	static private int fusedIndex (String key)
	{
		String[]	names = { "virtulen", "virturho", "virtufeat", "virturng", "virtucone", "virtumode" };

		return indexOf (key, names);
	}

	/** The virtual sensor a property of an older description belongs to, or -1. */
	static private int groupIndex (String key)
	{
		String[]	names = { "grouplen", "grouprho", "groupfeat", "grouprng", "groupcone", "groupbase", "groupmode", "groupequ" };

		return indexOf (key, names);
	}

	/** The number a property of an older description ends with, when it is one of those named. */
	static private int indexOf (String key, String[] names)
	{
		if (key == null)		return -1;
		for (String name : names)
			if (key.startsWith (name))
				try { return Integer.parseInt (key.substring (name.length ()).trim ()); }	catch (Exception e)		{ }
		return -1;
	}

	/** Reads a property of an older description and takes it out of what travels along. */
	private String take (String key)						{ return extra.remove (key); }

	static private double number (String text, double none)
	{
		if (text == null)		return none;
		try { return Double.parseDouble (text.trim ()); }	catch (Exception e)		{ return none; }
	}

	/**
	 * A driver read from a description that named it as the class and what it is
	 * opened with in one string, separated by a bar: the class stays in the driver
	 * and the rest goes to its parameters.
	 */
	static private void split (Sensor s)
	{
		String[]	d = split (s.driver, s.driverParams);

		s.driver		= d[0];
		s.driverParams	= d[1];
	}

	/** The class and the parameters of a driver named the old way: {class, parameters}. */
	static private String[] split (String driver, String params)
	{
		int		bar;

		if (driver == null)						return new String[] { null, params };
		bar		= driver.indexOf ('|');
		if (bar < 0)							return new String[] { driver, params };
		if (params == null)						params = driver.substring (bar + 1).trim ();
		driver	= driver.substring (0, bar).trim ();
		if (driver.length () == 0)				driver = null;
		if ((params != null) && (params.length () == 0))		params = null;
		return new String[] { driver, params };
	}

	/**
	 * The driver of a sensor as the device layer asks for it: the class and what
	 * it is opened with, separated by a bar. Null when it has no driver.
	 */
	static public String driverProperty (Sensor s)			{ return driverProperty (s.driver, s.driverParams); }
	/** The same for a family whose sensors are all read through the one driver. */
	static public String driverProperty (Family f)			{ return driverProperty (f.driver, f.driverParams); }

	static private String driverProperty (String driver, String params)
	{
		if ((driver == null) || (driver.trim ().length () == 0))		return null;
		return driver.trim () + "|" + ((params != null) ? params.trim () : "");
	}

	/**
	 * A family whose sensors now say what they detect, read from a description
	 * written when the family said it: what the family had goes to every sensor
	 * that does not say it yet, and the family keeps only its firing cycle.
	 */
	static private void migrate (String fam, Family f)
	{
		for (Sensor s : f.sensors)
		{
			if (s.driver == null)			s.driver = f.driver;
			if (s.driverParams == null)		s.driverParams = f.driverParams;
			if (s.rangemax == 0.0)			s.rangemax = f.rangemax;
			if (s.rangemin == 0.0)			s.rangemin = f.rangemin;
			if (s.cone == 0.0)				s.cone = f.cone;
			if (s.rays == 0)				s.rays = f.rays;
			if (s.reflect == 0.0)			s.reflect = f.reflect;
			if (s.beacons == 0)				s.beacons = f.beacons;
			if (s.objects == 0)				s.objects = f.objects;
			if (hasFov (fam))						// what a camera said as a cone is its horizontal field of view
			{
				if (s.hfov == 0.0)		s.hfov = s.cone;
				s.cone		= 0.0;
				s.rangemin	= 0.0;
			}
		}
		f.driver	= null;	f.driverParams = null;
		f.rangemax	= 0.0;	f.rangemin = 0.0;	f.cone = 0.0;	f.rays = 0;
		f.reflect	= 0.0;	f.beacons = 0;		f.objects = 0;
	}

	/**
	 * What a sensor detects: {rangemax, rangemin, cone}, taken from the sensor
	 * itself in the families whose sensors are devices of their own, and from the
	 * family in the others.
	 */
	public double[] detection (String fam, Sensor s)
	{
		Family	f = family (fam);

		if (hasFov (fam))				return new double[] { s.rangemax, 0.0, s.hfov };
		if (hasOwnDetection (fam))		return new double[] { s.rangemax, s.rangemin, s.cone };
		return new double[] { f.rangemax, f.rangemin, f.cone };
	}

	public Family family (String fam)
	{
		Family	f = sensors.get (fam);
		if (f == null)		sensors.put (fam, f = new Family ());
		return f;
	}

	/* ------------------------------------------------------------------ */
	/* The properties the runtime still reads                              */
	/* ------------------------------------------------------------------ */

	/**
	 * The description as the flat properties the runtime reads. It is the only
	 * place properties are left: a description travels to the modules this way
	 * (ItemConfig.props_robot) and {@link RobotDesc} and
	 * tclib.utils.fusion.FusionDesc are built from them. Descriptions themselves
	 * are read and written as JSON only.
	 */
	public Properties toProperties ()
	{
		Properties	p = new Properties ();

		normalise ();
		for (Map.Entry<String, String> e : extra.entrySet ())		p.setProperty (e.getKey (), e.getValue ());

		set (p, "RADIUS", radius);
		if (image != null)			p.setProperty ("IMAGE", image);
		if (shapeRobot != null)		p.setProperty ("V3DFILE", shapeRobot);
		if (shapeActuator != null)	p.setProperty ("V3DLIFT", shapeActuator);

		p.setProperty ("LINES", String.valueOf (icon.size ()));
		for (int i = 0; i < icon.size (); i++)
		{
			IconLine	l = icon.get (i);
			set (p, "iconxi" + i, l.xi);	set (p, "iconyi" + i, l.yi);
			set (p, "iconxf" + i, l.xf);	set (p, "iconyf" + i, l.yf);
		}

		if (kinematics.drive != null)		p.setProperty ("DRIVEMODEL", kinematics.drive);
		// what the drive train says is asked for, not stored
		setNZ (p, "VMAX", value (derived ("vmax")));	setNZ (p, "RMAX", value (derived ("rmax")));
		setNZ (p, "LENGHT", value (derived ("length")));	setNZ (p, "BASE", value (derived ("base")));
		setNZ (p, "WHEEL", value (derived ("wheel diameter")));	setNZ (p, "SAMAX", value (derived ("samax")));
		setNZ (p, "LAMAX", kinematics.lamax);		setNZ (p, "LDMAX", kinematics.ldmax);
		setNZ (p, "RWHEEL", kinematics.rwheel);		setNZ (p, "SKID", kinematics.skid);
		setNZ (p, "GEAR", kinematics.gear);
		setNZ (p, "PULSES", kinematics.pulses);
		p.setProperty ("DTIME", String.valueOf (kinematics.dtime));
		setNZ (p, "ODOM_ET", kinematics.odomET);	setNZ (p, "ODOM_ER", kinematics.odomER);
		setNZ (p, "ODOM_BIAS", kinematics.odomBias);

		if (bumpers.size () > 0)		p.setProperty ("MAXBUMPER", String.valueOf (bumpers.size ()));
		for (int i = 0; i < bumpers.size (); i++)
		{
			Bumper	b = bumpers.get (i);
			set (p, "bumxi" + i, b.xi);		set (p, "bumyi" + i, b.yi);
			set (p, "bumxf" + i, b.xf);		set (p, "bumyf" + i, b.yf);
		}

		for (int fi = 0; fi < FAMILIES.length; fi++)
		{
			String	fam = FAMILIES[fi], key = FAMILY_KEYS[fi];
			Family	f = family (fam);

			// a family with no sensors and no parameters is simply not there
			if ((f.n () == 0) && (f.rangemax == 0.0) && (f.rangemin == 0.0) && (f.cone == 0.0) && (f.cycle == 0)
					&& (f.rays == 0) && (f.reflect == 0.0) && (f.beacons == 0) && (f.objects == 0))		continue;
			p.setProperty (FAMILY_COUNTS[fi], String.valueOf (f.n ()));

			if (f.cycle > 0)		p.setProperty ("CYCLE" + key, String.valueOf (f.cycle));
			if (FAMILY_MODES[fi] != null)		p.setProperty (FAMILY_MODES[fi], String.valueOf (f.simmode));
			if (FAMILY_OWN[fi])
			{
				// the runtime still reads one set of values for the whole family: the
				// first sensor stands for it, and each one writes its own as well
				Sensor	s0 = (f.n () > 0) ? f.sensors.get (0) : new Sensor ();
				// a camera has no cone: what it sees wide is its horizontal field of view
				setNZ (p, "RANGE" + key, s0.rangemax);	setNZ (p, "MINIM" + key, s0.rangemin);
				setNZ (p, "CONE" + key, hasFov (fam) ? s0.hfov : s0.cone);
				if (hasFov (fam))		{ setNZ (p, "HFOV" + key, s0.hfov);		setNZ (p, "VFOV" + key, s0.vfov); }
				if (s0.rays > 0)		p.setProperty ("RAY" + key, String.valueOf (s0.rays));
				if (s0.reflect != 0.0)	set (p, "REF" + key, s0.reflect);
				if (s0.beacons > 0)		p.setProperty ("BEAC" + key, String.valueOf (s0.beacons));
				if (fam.equals ("trk") && (s0.objects > 0))		p.setProperty ("OBJTRK", String.valueOf (s0.objects));
			}
			else
			{
				setNZ (p, "RANGE" + key, f.rangemax);	setNZ (p, "MINIM" + key, f.rangemin);	setNZ (p, "CONE" + key, f.cone);
				if (f.rays > 0)			p.setProperty ("RAY" + key, String.valueOf (f.rays));
				if (f.reflect != 0.0)	set (p, "REF" + key, f.reflect);
				if (f.beacons > 0)		p.setProperty ("BEAC" + key, String.valueOf (f.beacons));
				if (fam.equals ("trk") && (f.objects > 0))		p.setProperty ("OBJTRK", String.valueOf (f.objects));
			}

			for (int i = 0; i < f.n (); i++)
			{
				Sensor	s = f.sensors.get (i);
				set (p, fam + "len" + i, s.rho);		set (p, fam + "rho" + i, s.theta);
				setNZ (p, fam + "hgt" + i, s.height);	set (p, fam + "feat" + i, s.orientation);
				setNZ (p, fam + "elev" + i, s.elevation);
				if (s.step > 0)		p.setProperty (fam + "step" + i, String.valueOf (s.step));
				if (FAMILY_OWN[fi])
				{
					// this sensor is a device of its own: its driver and what it detects
					String	dp = driverProperty (s);
					if (dp != null)		p.setProperty (FAMILY_DRIVERS[fi] + i, dp);
					setNZ (p, "RANGE" + key + i, s.rangemax);	setNZ (p, "MINIM" + key + i, s.rangemin);
					setNZ (p, "CONE" + key + i, hasFov (fam) ? s.hfov : s.cone);
					if (hasFov (fam))	{ setNZ (p, "HFOV" + key + i, s.hfov);		setNZ (p, "VFOV" + key + i, s.vfov); }
					if (s.rays > 0)			p.setProperty ("RAY" + key + i, String.valueOf (s.rays));
					if (s.reflect != 0.0)	set (p, "REF" + key + i, s.reflect);
					if (s.beacons > 0)		p.setProperty ("BEAC" + key + i, String.valueOf (s.beacons));
					if (s.objects > 0)		p.setProperty ("OBJ" + key + i, String.valueOf (s.objects));
				}
				// every sensor of the family is read through the same driver
				else if (FAMILY_DRIVERS[fi] != null)
				{
					String	fdp = driverProperty (f);
					if (fdp != null)		p.setProperty (FAMILY_DRIVERS[fi] + i, fdp);
				}
			}
		}

		// the fused sensors, under the names the older descriptions gave them
		if (!fused.isEmpty ())
		{
			p.setProperty ("MAXVIRTU", String.valueOf (fused.size ()));
			for (int i = 0; i < fused.size (); i++)
			{
				Fused	f = fused.get (i);

				set (p, "virtulen" + i, f.rho);			set (p, "virturho" + i, f.theta);
				set (p, "virtufeat" + i, f.orientation);
				if (f.mode >= 0)	p.setProperty ("virtumode" + i, String.valueOf (f.mode));
				// how far it reaches and how wide it is are not written: the fusion reads
				// one of each for the whole lot of them (RANGEVIRTU, CONEVIRTU), which
				// stay where they are, and what is kept here is what the editor draws
			}
		}

		// the virtual sensors, which the fusion of the runtime reads by the names the
		// older descriptions gave them (what it calls len is the distance, rho the
		// angle of it and feat where it looks)
		if (!groups.isEmpty ())
		{
			p.setProperty ("MAXGROUP", String.valueOf (groups.size ()));
			for (int i = 0; i < groups.size (); i++)
			{
				Group	g = groups.get (i);

				set (p, "grouplen" + i, g.rho);			set (p, "grouprho" + i, g.theta);
				set (p, "groupfeat" + i, g.orientation);
				setNZ (p, "grouprng" + i, g.rangemax);	setNZ (p, "groupcone" + i, g.cone);
				setNZ (p, "groupbase" + i, g.base);
				p.setProperty ("groupmode" + i, String.valueOf (g.mode));
				if ((g.equ != null) && (g.equ.trim ().length () > 0))		p.setProperty ("groupequ" + i, g.equ.trim ());
			}
		}

		return p;
	}

	/* Helpers */

	static private double value (Double v)			{ return (v != null) ? v.doubleValue () : 0.0; }

	static private void set (Properties p, String key, double value)
	{
		p.setProperty (key, fmt (value));
	}

	/** Writes a value only when it says something (a zero is the default of every optional parameter). */
	static private void setNZ (Properties p, String key, double value)
	{
		if (value != 0.0)		p.setProperty (key, fmt (value));
	}

	/** Compact text of a number (no trailing zeros), as the files are written by hand. */
	static public String fmt (double v)
	{
		if (Math.abs (v - Math.rint (v)) < 1e-9)		return Long.toString (Math.round (v));
		String	s = String.format (java.util.Locale.US, "%.6f", v);
		while (s.endsWith ("0"))		s = s.substring (0, s.length () - 1);
		return s;
	}

	public String toString ()
	{
		StringBuilder	sb = new StringBuilder (((name != null) ? name : "robot") + " [radius=" + fmt (radius) + ", lines=" + icon.size () + ", bumpers=" + bumpers.size ());
		for (String fam : FAMILIES)
			if (family (fam).n () > 0)		sb.append (", " + fam + "=" + family (fam).n ());
		return sb.append (']').toString ();
	}
}
