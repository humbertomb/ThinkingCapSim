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

	static public synchronized List<String> of (String base, boolean plain)		{ return of (base, plain, false); }

	/**
	 * The same, over the whole development rather than over the package of the
	 * base class: the modules of an architecture derive from a class of the
	 * runtime (<code>StdThread</code>) but live wherever the robot they were
	 * written for does, so there is no one package to look in.
	 *
	 * Only the directories of the class path are walked, which is where the
	 * classes of the development are; the jars it is built against are left
	 * alone.
	 */
	static public synchronized List<String> of (String base, boolean plain, boolean project)
	{
		return of (base, plain, project, (String[]) null);
	}

	/**
	 * The same, less what derives from any of <code>not</code>: the modules of an
	 * architecture and its robots are all threads of the runtime, so what a module
	 * may be is said by what it may not -- a robot, a monitor.
	 */
	static public synchronized List<String> of (String base, boolean plain, boolean project, String... not)
	{
		List<String>	found;
		String			cached;

		if ((base == null) || (base.trim ().length () == 0))		return new ArrayList<String> ();
		base	= base.trim ();
		cached	= base + (plain ? "|()" : "|*") + (project ? "|all" : "")
					+ ((not != null) ? "|-" + String.join (",", not) : "");
		if (CACHE.containsKey (cached))		return CACHE.get (cached);

		found	= search (base, plain, project, not);
		CACHE.put (cached, found);
		return found;
	}

	/** Forgets what was found (the development having been built again, say). */
	static public synchronized void flush ()					{ CACHE.clear (); }

	/* ------------------------------------------------------------------ */

	static private List<String> search (String base, boolean plain, boolean project, String[] not)
	{
		List<String>	names = new ArrayList<String> ();
		List<String>	out = new ArrayList<String> ();
		List<Class<?>>	barred = new ArrayList<Class<?>> ();
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

		if (project)		development (names);
		else
		{
			pkg		= (base.lastIndexOf ('.') > 0) ? base.substring (0, base.lastIndexOf ('.')) : "";
			classes (pkg, names);
		}

		if (project)		names = descendants (base, names);		// before loading them: see descendants ()
		if (not != null)
			for (String bar : not)
				try { barred.add (Class.forName (bar, false, loader ())); }	catch (Throwable e)		{ }

		for (String name : names)
		{
			Class<?>	c;

			// the base itself is offered when it can be used as it is (a router is; a
			// thread of the runtime is abstract and falls at the tests below)
			if (!project && name.equals (base))			continue;
			try
			{
				c	= Class.forName (name, false, loader ());
			} catch (Throwable e)			{ continue; }				// something it needs is not here
			if (!root.isAssignableFrom (c))							continue;
			if (c.isInterface () || Modifier.isAbstract (c.getModifiers ()))	continue;
			if (!Modifier.isPublic (c.getModifiers ()))				continue;
			if (plain && !buildable (c))							continue;
			if (!plain && (c.getDeclaredConstructors ().length == 0))	continue;
			if (barredFrom (barred, c))								continue;
			out.add (name);
		}
		Collections.sort (out);
		return out;
	}

	/* ------------------------------------------------------------------ */

	/**
	 * The ones of a list of classes that derive from another, read off the class
	 * files themselves.
	 *
	 * Loading every class of the development to ask it (thousands of them, each
	 * dragging in whatever it is written against) takes seconds, which is too
	 * long for opening the editor of a cell. The name of the superclass sits in
	 * the class file, a few bytes in, so the family tree is read from there and
	 * only what is already known to belong to it is loaded.
	 */
	static private List<String> descendants (String base, List<String> names)
	{
		Map<String, String>		parent = new HashMap<String, String> ();
		List<String>			out = new ArrayList<String> ();

		for (String name : names)
		{
			String		up = superOf (name);

			if (up != null)		parent.put (name, up);
		}
		for (String name : names)
		{
			String		up = name;

			for (int i = 0; (up != null) && (i < 64); i++)			// 64: a tree, not a knot
			{
				if (up.equals (base))		{ out.add (name); break; }
				up	= parent.get (up);
			}
		}
		return out;
	}

	/** The superclass a class file names, or null when it cannot be read. */
	static private String superOf (String name)
	{
		java.io.InputStream		in = null;

		try
		{
			in	= loader ().getResourceAsStream (name.replace ('.', '/') + ".class");
			if (in == null)				return null;
			return superOf (new java.io.DataInputStream (new java.io.BufferedInputStream (in)));
		} catch (Throwable e)			{ return null; }
		finally
		{
			if (in != null)		try { in.close (); } catch (Throwable e) { }
		}
	}

	/**
	 * Reads the constant pool of a class file and, with it, the name of its
	 * superclass: what comes before it is the magic number and the version, and
	 * what comes after is its own name and the one that is wanted.
	 */
	static private String superOf (java.io.DataInputStream d) throws java.io.IOException
	{
		int			n;
		int[]		kind;
		int[]		ref;
		String[]	text;
		int			up;

		if (d.readInt () != 0xCAFEBABE)			return null;
		d.readUnsignedShort ();					d.readUnsignedShort ();			// minor, major
		n		= d.readUnsignedShort ();
		kind	= new int[n];
		ref		= new int[n];
		text	= new String[n];
		for (int i = 1; i < n; i++)
		{
			int		tag = d.readUnsignedByte ();

			kind[i]	= tag;
			switch (tag)
			{
			case 1:								text[i] = d.readUTF ();					break;	// Utf8
			case 7: case 8: case 16: case 19: case 20:
												ref[i] = d.readUnsignedShort ();		break;	// Class, String, MethodType, Module, Package
			case 15:							d.skipBytes (3);						break;	// MethodHandle
			case 5: case 6:						d.skipBytes (8);	i++;				break;	// Long, Double: two slots
			default:							d.skipBytes (4);						break;	// the rest are four bytes
			}
		}
		d.readUnsignedShort ();					d.readUnsignedShort ();			// access flags, its own name
		up		= d.readUnsignedShort ();
		if ((up <= 0) || (up >= n) || (kind[up] != 7))			return null;		// Object, or something unexpected
		up		= ref[up];
		if ((up <= 0) || (up >= n) || (kind[up] != 1))			return null;
		return text[up].replace ('/', '.');
	}

	/** True when a class is one of those asked to be left out, or derives from one. */
	static private boolean barredFrom (List<Class<?>> barred, Class<?> c)
	{
		for (Class<?> bar : barred)		if (bar.isAssignableFrom (c))		return true;
		return false;
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

	/** The names of every class of the development: what the directories of the class path hold. */
	static private void development (List<String> out)
	{
		String		cp = System.getProperty ("java.class.path");

		if (cp == null)			return;
		for (String entry : cp.split (File.pathSeparator))
		{
			File	f = new File (entry);

			if (f.isDirectory ())		walk (f, "", out);
		}
	}

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
				walk (f, sub (pkg, f.getName ()), out);
			else if (f.getName ().endsWith (".class"))
				add (sub (pkg, f.getName ().substring (0, f.getName ().length () - 6)), out);
	}

	/** A name inside a package, which at the root of the class path is the name itself. */
	static private String sub (String pkg, String name)
	{
		return ((pkg == null) || (pkg.length () == 0)) ? name : pkg + "." + name;
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
