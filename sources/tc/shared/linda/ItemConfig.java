/* 
	Title:			Thinking Cap 
	Author:			Humberto Martinez Barbera
	Description:	BGA Architecture Support.
*/

package tc.shared.linda;

import java.io.*;
import java.util.*;

public class ItemConfig extends Item implements Serializable
{
	// Robot and environment properties
	public Properties			props_robot;
	public String				world;				// JSON text of the world (tc.shared.world.World, topology included), or null
	
	// Constructors
	public ItemConfig () 
	{
		this.props_robot	= null;
		this.world			= null;
		
		set (0);
	}	
	
	public ItemConfig (String name_robot, String name_world, long tstamp) 
	{
		File				file;
		FileInputStream		stream;
		
		if (name_robot != null)
		{
			this.props_robot		= new Properties ();
			try 
			{
				file 		= new File (name_robot);
				stream 		= new FileInputStream (file);
				props_robot.load (stream);
				stream.close ();
			} catch (Exception e) { e.printStackTrace (); }
		}

		if (name_world != null)
		{
			try { this.world = new String (java.nio.file.Files.readAllBytes (java.nio.file.Paths.get (name_world)), java.nio.charset.StandardCharsets.UTF_8); }
			catch (Exception e) { e.printStackTrace (); }
		}
		
		set (tstamp);
	}	
	
	/** @param world  JSON text of the world file (see {@link tc.shared.world.World#toJsonText}), or null */
	public ItemConfig (Properties props_robot, String world, long tstamp) 
	{
		this.props_robot	= props_robot;
		this.world			= world;
		
		set (tstamp);
	}	
}
