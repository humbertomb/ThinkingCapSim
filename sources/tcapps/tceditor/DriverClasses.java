/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The classes a description can name: the ones of the development that derive
 * from the one that part of the program asks for -- the driver of a device
 * (<code>Laser</code>, <code>Vision</code>, <code>LaserBeacon</code>,
 * <code>Radar</code>) or the kinematics model of a platform
 * (<code>RobotModel</code>) -- so that one is chosen from what there is instead
 * of being typed in.
 *
 * What is looked at is the package of that class and everything under it, which
 * is where those classes live, over the class path of the running
 * program (its directories and its jars). A class that cannot be read, or that
 * is missing something it needs, is left out rather than stopping the search.
 */
public class DriverClasses
{
	static private final Map<String, List<String>>	CACHE = new HashMap<String, List<String>> ();

	/**
	 * The classes that derive from a class, by name and in order. An empty list
	 * when the class itself is not there or nothing derives from it.
	 *
	 * A driver is built with no arguments, which is asked for; something built
	 * another way (a kinematics model takes the robot and its properties) is
	 * looked for with <code>plain</code> false.
	 */
	static public synchronized List<String> of (String base)			{ return of (base, true); }

	static public synchronized List<String> of (String base, boolean plain)
	{
		List<String>	found;
		String			cached;

		if ((base == null) || (base.trim ().length () == 0))		return new ArrayList<String> ();
		base	= base.trim ();
		cached	= base + (plain ? "|()" : "|*");
		if (CACHE.containsKey (cached))		return CACHE.get (cached);

		found	= search (base, plain);
		CACHE.put (cached, found);
		return found;
	}

	/** Forgets what was found (the development having been built again, say). */
	static public synchronized void flush ()					{ CACHE.clear (); }

	/* ------------------------------------------------------------------ */

	static private List<String> search (String base, boolean plain)
	{
		List<String>	names = new ArrayList<String> ();
		List<String>	out = new ArrayList<String> ();
		Class<?>		root;
		String			pkg;

		try
		{
			root	= Class.forName (base, false, loader ());
		} catch (Throwable e)
		{
			System.out.println ("--[DriverClasses] Cannot find the driver class <" + base + ">: " + e);
			return out;
		}

		pkg		= (base.lastIndexOf ('.') > 0) ? base.substring (0, base.lastIndexOf ('.')) : "";
		classes (pkg, names);

		for (String name : names)
		{
			Class<?>	c;

			if (name.equals (base))			continue;
			try
			{
				c	= Class.forName (name, false, loader ());
			} catch (Throwable e)			{ continue; }				// something it needs is not here
			if (!root.isAssignableFrom (c))							continue;
			if (c.isInterface () || Modifier.isAbstract (c.getModifiers ()))	continue;
			if (!Modifier.isPublic (c.getModifiers ()))				continue;
			if (plain && !buildable (c))							continue;
			if (!plain && (c.getDeclaredConstructors ().length == 0))	continue;
			out.add (name);
		}
		Collections.sort (out);
		return out;
	}

	/** True when it can be built the way the device layer builds a driver: with no arguments. */
	static private boolean buildable (Class<?> c)
	{
		try
		{
			return Modifier.isPublic (c.getDeclaredConstructor ().getModifiers ());
		} catch (Throwable e)		{ return false; }
	}

	static private ClassLoader loader ()
	{
		ClassLoader		cl = Thread.currentThread ().getContextClassLoader ();
		return (cl != null) ? cl : DriverClasses.class.getClassLoader ();
	}

	/* ------------------------------------------------------------------ */

	/** The names of every class of a package and of the packages under it. */
	static private void classes (String pkg, List<String> out)
	{
		String		path = pkg.replace ('.', '/');
		String		cp = System.getProperty ("java.class.path");

		if (cp == null)			return;
		for (String entry : cp.split (File.pathSeparator))
		{
			File	f = new File (entry);

			if (f.isDirectory ())		walk (new File (f, path), pkg, out);
			else if (f.isFile ())		jar (f, path, out);
		}
	}

	static private void walk (File dir, String pkg, List<String> out)
	{
		File[]		files;

		if (!dir.isDirectory ())		return;
		files	= dir.listFiles ();
		if (files == null)				return;
		for (File f : files)
			if (f.isDirectory ())
				walk (f, pkg + "." + f.getName (), out);
			else if (f.getName ().endsWith (".class"))
				add (pkg + "." + f.getName ().substring (0, f.getName ().length () - 6), out);
	}

	static private void jar (File f, String path, List<String> out)
	{
		ZipFile		zip = null;

		try
		{
			zip		= new ZipFile (f);
			for (Enumeration<? extends ZipEntry> e = zip.entries (); e.hasMoreElements (); )
			{
				String		name = e.nextElement ().getName ();

				if (!name.endsWith (".class"))			continue;
				if (!name.startsWith (path + "/"))		continue;
				add (name.substring (0, name.length () - 6).replace ('/', '.'), out);
			}
		} catch (Throwable e)		{ }								// not a jar, or not readable
		finally
		{
			if (zip != null)		try { zip.close (); } catch (Throwable e) { }
		}
	}

	/** Adds a class, unless it is one the compiler made (an inner or an anonymous one). */
	static private void add (String name, List<String> out)
	{
		if (name.indexOf ('$') >= 0)		return;
		if (!out.contains (name))			out.add (name);
	}
}
