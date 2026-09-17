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
			for (Sensor s : sensors)		f.sensors.add (s.copy ());
			return f;
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

	/** Kinematics and dynamics of the platform (what RobotModel reads). */
	static public class Kinematics
	{
		public String	drive		= "tc.vrobot.models.DifferentialDrive";	// DRIVEMODEL
		public double	vmax;						// maximum linear speed (m/s)
		public double	rmax;						// maximum angular speed (deg/s)
		public double	maxmotor;					// maximum speed of the driving wheel (m/s)
		public double	maxsteer;					// maximum angle of the driving wheel (deg)
		public double	samax;						// maximum turning speed of the driving wheel (deg/s)
		public double	lamax;						// maximum acceleration (m/s2)
		public double	ldmax;						// maximum deceleration (m/s2)
		public double	length;						// LENGHT (m)
		public double	base;						// BASE (m)
		public double	rwheel;						// RWHEEL (m)
		public double	wheel;						// WHEEL (m)
		public double	gear;						// GEAR
		public double	pulses;						// PULSES
		public long		dtime	= 100;				// control cycle (ms)
		public double	odomET, odomER, odomBias;	// odometry errors of the simulation

		public Kinematics copy ()
		{
			Kinematics	k = new Kinematics ();
			k.drive = drive;	k.vmax = vmax;		k.rmax = rmax;		k.maxmotor = maxmotor;	k.maxsteer = maxsteer;
			k.samax = samax;	k.lamax = lamax;	k.ldmax = ldmax;	k.length = length;		k.base = base;
			k.rwheel = rwheel;	k.wheel = wheel;	k.gear = gear;		k.pulses = pulses;		k.dtime = dtime;
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

	public String toJson ()					{ return gson ().toJson (this); }
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
		}
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
		set (p, "VMAX", kinematics.vmax);			set (p, "RMAX", kinematics.rmax);
		setNZ (p, "MAXMOTOR", kinematics.maxmotor);	setNZ (p, "MAXSTEER", kinematics.maxsteer);
		setNZ (p, "SAMAX", kinematics.samax);		setNZ (p, "LAMAX", kinematics.lamax);
		setNZ (p, "LDMAX", kinematics.ldmax);		setNZ (p, "LENGHT", kinematics.length);
		setNZ (p, "BASE", kinematics.base);			setNZ (p, "RWHEEL", kinematics.rwheel);
		setNZ (p, "WHEEL", kinematics.wheel);		setNZ (p, "GEAR", kinematics.gear);
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

		return p;
	}

	/* Helpers */

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
