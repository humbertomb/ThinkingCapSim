/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tc.vrobot.articulated;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The kinematic model of a robot in a .kine file: JSON, as the other
 * descriptions of the simulator (the tree of {@link KineNode}s under a root,
 * with the joints and the shapes of each, in metres and radians). Kept in
 * conf/robots beside the .robot file of the robot it articulates.
 */
public class KineJson
{
	static public final String		SUFFIX		= ".kine";
	static public final String		FOLDER		= "./conf/robots";

	static private final Gson		GSON		= new GsonBuilder ().setPrettyPrinting ().disableHtmlEscaping ().create ();

	/** The model of a file, linked and at rest. */
	static public KineModel read (File f) throws Exception
	{
		Reader		in = new FileReader (f);

		try
		{
			KineModel	m = GSON.fromJson (in, KineModel.class);

			if ((m == null) || (m.root == null))		throw new Exception ("no root link in " + f);
			if (m.name == null)							m.name = stem (f);
			m.link ();
			return m;
		}
		finally		{ in.close (); }
	}

	/** The model of the robot of that name, from where the .kine files are kept. */
	static public KineModel read (String robot) throws Exception
	{
		return read (new File (FOLDER, robot + SUFFIX));
	}

	static public void write (KineModel m, File f) throws Exception
	{
		Writer		out = new FileWriter (f);

		try			{ GSON.toJson (m, out); }
		finally		{ out.close (); }
	}

	static public String toJson (KineModel m)			{ return GSON.toJson (m); }

	static private String stem (File f)
	{
		String	n = f.getName ();
		int		dot = n.lastIndexOf ('.');

		return (dot > 0) ? n.substring (0, dot) : n;
	}
}
