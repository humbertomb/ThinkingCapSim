/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tc.vrobot;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
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
 * {@link #toProperties()} rebuilds them and {@link #fromProperties(Properties)}
 * reads a legacy file. Keys the model does not describe are kept in
 * {@link #extra}, so nothing of a converted file is lost.
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
		public double	orientation;				// direction the sensor looks at (deg, "<fam>feat")
		public int		step;						// reading step ("<fam>step")

		// what it detects (families with sensors of their own only)
		public String	driver;						// device it is read through (LRF0, LSB0, ...)
		public double	rangemax;					// maximum range (m)
		public double	rangemin;					// minimum range (m)
		public double	cone;						// aperture (deg)
		public int		rays;						// rays of a scan
		public double	reflect;					// lsb: maximum reflection angle (deg)
		public int		beacons;					// lsb: beacons it can see at once
		public int		objects;					// trk: objects it can track at once

		public Sensor ()							{ }
		public Sensor copy ()
		{
			Sensor	s = new Sensor ();
			s.rho = rho;	s.theta = theta;	s.height = height;	s.orientation = orientation;	s.step = step;
			s.driver = driver;	s.rangemax = rangemax;	s.rangemin = rangemin;	s.cone = cone;	s.rays = rays;
			s.reflect = reflect;	s.beacons = beacons;	s.objects = objects;
			return s;
		}

		/** True when nothing of what it detects is set (so none of it is written to the file). */
		public boolean plain ()
		{
			return (driver == null) && (rangemax == 0.0) && (rangemin == 0.0) && (cone == 0.0)
				&& (rays == 0) && (reflect == 0.0) && (beacons == 0) && (objects == 0);
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
		public String		driver;					// driver every sensor of the family uses (LRF0, LSB0, ...)
		public List<Sensor>	sensors	= new ArrayList<Sensor> ();

		public int n ()								{ return sensors.size (); }

		public Family copy ()
		{
			Family	f = new Family ();
			f.rangemax = rangemax;	f.rangemin = rangemin;	f.cone = cone;		f.cycle = cycle;
			f.rays = rays;		f.reflect = reflect;	f.beacons = beacons;	f.objects = objects;
			f.driver = driver;
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
	 * Families whose sensors are devices of their own: each one says what it
	 * detects (driver, range, cone, rays, ...) instead of taking it from the
	 * family. Only the firing cycle stays with the family.
	 */
	static public final boolean[]	FAMILY_OWN		= { false, false, true, true, true, false };

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
					.registerTypeAdapter (Sensor.class, new SensorWriter ()).create ();
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
			o.addProperty ("step", s.step);
			if (s.driver != null)		o.addProperty ("driver", s.driver);
			if (s.rangemax != 0.0)		o.addProperty ("rangemax", s.rangemax);
			if (s.rangemin != 0.0)		o.addProperty ("rangemin", s.rangemin);
			if (s.cone != 0.0)			o.addProperty ("cone", s.cone);
			if (s.rays != 0)			o.addProperty ("rays", s.rays);
			if (s.reflect != 0.0)		o.addProperty ("reflect", s.reflect);
			if (s.beacons != 0)			o.addProperty ("beacons", s.beacons);
			if (s.objects != 0)			o.addProperty ("objects", s.objects);
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

	/** Reads a robot description, in JSON or in the legacy properties format. */
	static public RobotDef load (File f) throws IOException
	{
		RobotDef	d;

		if (isJson (f))
		{
			Reader	in = new FileReader (f, java.nio.charset.StandardCharsets.UTF_8);
			try
			{
				d	= gson ().fromJson (in, RobotDef.class);
				if (d == null)		throw new IOException ("Empty robot description");
			}
			finally { in.close (); }
		}
		else
		{
			Properties	props = new Properties ();
			InputStream	in = new FileInputStream (f);
			try { props.load (in); } finally { in.close (); }
			d	= fromProperties (props);
		}

		d.normalise ();
		if ((d.name == null) || (d.name.trim ().length () == 0))		d.name = baseName (f);
		d.file		= f;
		d.original	= d.toJson ();
		return d;
	}

	/** True when the file holds a JSON description (the new format). */
	static public boolean isJson (File f)
	{
		InputStream	in = null;
		try
		{
			in	= new FileInputStream (f);
			int		c;
			while ((c = in.read ()) != -1)
			{
				if (Character.isWhitespace (c))		continue;
				return (c == '{');
			}
		}
		catch (Exception e) { }
		finally { try { if (in != null)		in.close (); } catch (Exception e) { } }
		return false;
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
			if (hasOwnDetection (fam))		migrate (f);
		}
	}

	/**
	 * A family whose sensors now say what they detect, read from a description
	 * written when the family said it: what the family had goes to every sensor
	 * that does not say it yet, and the family keeps only its firing cycle.
	 */
	static private void migrate (Family f)
	{
		for (Sensor s : f.sensors)
		{
			if (s.driver == null)			s.driver = f.driver;
			if (s.rangemax == 0.0)			s.rangemax = f.rangemax;
			if (s.rangemin == 0.0)			s.rangemin = f.rangemin;
			if (s.cone == 0.0)				s.cone = f.cone;
			if (s.rays == 0)				s.rays = f.rays;
			if (s.reflect == 0.0)			s.reflect = f.reflect;
			if (s.beacons == 0)				s.beacons = f.beacons;
			if (s.objects == 0)				s.objects = f.objects;
		}
		f.driver	= null;
		f.rangemax	= 0.0;	f.rangemin = 0.0;	f.cone = 0.0;	f.rays = 0;
		f.reflect	= 0.0;	f.beacons = 0;		f.objects = 0;
	}

	public Family family (String fam)
	{
		Family	f = sensors.get (fam);
		if (f == null)		sensors.put (fam, f = new Family ());
		return f;
	}

	/* ------------------------------------------------------------------ */
	/* Legacy properties                                                   */
	/* ------------------------------------------------------------------ */

	/** Reads a description given as the properties of a legacy .robot file. */
	static public RobotDef fromProperties (Properties props)
	{
		RobotDef	d = new RobotDef ();
		List<String>	used = new ArrayList<String> ();

		d.normalise ();
		d.radius		= num (props, "RADIUS", 0.25, used);
		d.image			= str (props, "IMAGE", used);
		d.shapeRobot	= str (props, "V3DFILE", used);
		d.shapeActuator	= str (props, "V3DLIFT", used);

		// drawing
		int		lines = (int) num (props, "LINES", 0, used);
		for (int i = 0; i < lines; i++)
			d.icon.add (new IconLine (num (props, "iconxi" + i, 0, used), num (props, "iconyi" + i, 0, used),
									  num (props, "iconxf" + i, 0, used), num (props, "iconyf" + i, 0, used)));

		// kinematics
		Kinematics	k = d.kinematics;
		k.drive			= str (props, "DRIVEMODEL", used);
		if (k.drive == null)		k.drive = "tc.vrobot.models.DifferentialDrive";
		k.vmax			= num (props, "VMAX", 0, used);
		k.rmax			= num (props, "RMAX", 0, used);
		k.maxmotor		= num (props, "MAXMOTOR", 0, used);
		k.maxsteer		= num (props, "MAXSTEER", 0, used);
		k.samax			= num (props, "SAMAX", 0, used);
		k.lamax			= num (props, "LAMAX", 0, used);
		k.ldmax			= num (props, "LDMAX", 0, used);
		k.length		= num (props, "LENGHT", 0, used);
		k.base			= num (props, "BASE", 0, used);
		k.rwheel		= num (props, "RWHEEL", 0, used);
		k.wheel			= num (props, "WHEEL", 0, used);
		k.gear			= num (props, "GEAR", 0, used);
		k.pulses		= num (props, "PULSES", 0, used);
		k.dtime			= (long) num (props, "DTIME", 100, used);
		k.odomET		= num (props, "ODOM_ET", 0, used);
		k.odomER		= num (props, "ODOM_ER", 0, used);
		k.odomBias		= num (props, "ODOM_BIAS", 0, used);

		// bumpers
		int		nbum = (int) num (props, "MAXBUMPER", 0, used);
		for (int i = 0; i < nbum; i++)
			d.bumpers.add (new Bumper (num (props, "bumxi" + i, 0, used), num (props, "bumyi" + i, 0, used),
									   num (props, "bumxf" + i, 0, used), num (props, "bumyf" + i, 0, used)));

		// range sensors, family by family
		for (int fi = 0; fi < FAMILIES.length; fi++)
		{
			String	fam = FAMILIES[fi], key = FAMILY_KEYS[fi];
			Family	f = d.family (fam);
			int		n = (int) num (props, FAMILY_COUNTS[fi], 0, used);

			f.rangemax	= num (props, "RANGE" + key, 0, used);
			f.rangemin	= num (props, "MINIM" + key, 0, used);
			f.cone		= num (props, "CONE" + key, 0, used);
			f.cycle		= (int) num (props, "CYCLE" + key, 0, used);
			f.rays		= (int) num (props, "RAY" + key, 0, used);
			f.reflect	= num (props, "REF" + key, 0, used);
			f.beacons	= (int) num (props, "BEAC" + key, 0, used);
			if (fam.equals ("trk"))		f.objects = (int) num (props, "OBJTRK", 0, used);

			for (int i = 0; i < n; i++)
			{
				Sensor	s = new Sensor ();
				s.rho			= num (props, fam + "len" + i, 0, used);
				s.theta			= num (props, fam + "rho" + i, 0, used);
				s.height		= num (props, fam + "hgt" + i, 0, used);
				s.orientation	= num (props, fam + "feat" + i, 0, used);
				s.step			= (int) num (props, fam + "step" + i, 0, used);
				if (FAMILY_OWN[fi])
				{
					// what this one detects, or what the family says when it does not say it
					s.driver	= str (props, FAMILY_DRIVERS[fi] + i, used);
					s.rangemax	= num (props, "RANGE" + key + i, f.rangemax, used);
					s.rangemin	= num (props, "MINIM" + key + i, f.rangemin, used);
					s.cone		= num (props, "CONE" + key + i, f.cone, used);
					s.rays		= (int) num (props, "RAY" + key + i, f.rays, used);
					s.reflect	= num (props, "REF" + key + i, f.reflect, used);
					s.beacons	= (int) num (props, "BEAC" + key + i, f.beacons, used);
					s.objects	= (int) num (props, "OBJ" + key + i, f.objects, used);
				}
				else if (FAMILY_DRIVERS[fi] != null)
				{
					// the driver is one for the whole family: the first one that names it wins
					String	drv = str (props, FAMILY_DRIVERS[fi] + i, used);
					if ((drv != null) && (f.driver == null))		f.driver = drv;
				}
				f.sensors.add (s);
			}
			if (FAMILY_OWN[fi])		migrate (f);				// what the family said is now of each sensor
		}

		// everything the model does not describe travels as it is
		for (String p : props.stringPropertyNames ())
			if (!used.contains (p))		d.extra.put (p, props.getProperty (p));

		return d;
	}

	/** The description as the flat properties the runtime reads. */
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
				setNZ (p, "RANGE" + key, s0.rangemax);	setNZ (p, "MINIM" + key, s0.rangemin);	setNZ (p, "CONE" + key, s0.cone);
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
				if (s.step > 0)		p.setProperty (fam + "step" + i, String.valueOf (s.step));
				if (FAMILY_OWN[fi])
				{
					// this sensor is a device of its own: its driver and what it detects
					if ((s.driver != null) && (s.driver.trim ().length () > 0))
						p.setProperty (FAMILY_DRIVERS[fi] + i, s.driver.trim ());
					setNZ (p, "RANGE" + key + i, s.rangemax);	setNZ (p, "MINIM" + key + i, s.rangemin);
					setNZ (p, "CONE" + key + i, s.cone);
					if (s.rays > 0)			p.setProperty ("RAY" + key + i, String.valueOf (s.rays));
					if (s.reflect != 0.0)	set (p, "REF" + key + i, s.reflect);
					if (s.beacons > 0)		p.setProperty ("BEAC" + key + i, String.valueOf (s.beacons));
					if (s.objects > 0)		p.setProperty ("OBJ" + key + i, String.valueOf (s.objects));
				}
				// every sensor of the family is read through the same driver
				else if ((FAMILY_DRIVERS[fi] != null) && (f.driver != null) && (f.driver.trim ().length () > 0))
					p.setProperty (FAMILY_DRIVERS[fi] + i, f.driver.trim ());
			}
		}

		return p;
	}

	/* Helpers */

	static private double num (Properties props, String key, double def, List<String> used)
	{
		used.add (key);
		try { return Double.parseDouble (props.getProperty (key).trim ()); } catch (Exception e) { return def; }
	}

	static private String str (Properties props, String key, List<String> used)
	{
		used.add (key);
		String	v = props.getProperty (key);
		return ((v == null) || (v.trim ().length () == 0)) ? null : v.trim ();
	}

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
