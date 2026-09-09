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
	public Properties			props_world;
	public Properties 			props_topol;
	
	// Constructors
	public ItemConfig () 
	{
		this.props_robot	= null;
		this.props_world	= null;
		this.props_topol	= null;
		
		set (0);
	}	
	
	public ItemConfig (String name_robot, String name_world, long tstamp) 
	{
		this (name_robot, name_world, null, tstamp);
	}
	
	public ItemConfig (Properties props_robot, Properties props_world, long tstamp) 
	{
		this (props_robot, props_world, null, tstamp);
	}	
	
	public ItemConfig (String name_robot, String name_world, String name_topol, long tstamp) 
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
			this.props_world		= new Properties ();
			try 
			{
				file 		= new File (name_world);
				stream 		= new FileInputStream (file);
				props_world.load (stream);
				stream.close ();
			} catch (Exception e) { e.printStackTrace (); }
		}
		
		if (name_topol != null)
		{
			this.props_topol	= new Properties ();
			try 
			{
				file 		= new File (name_topol);
				stream 		= new FileInputStream (file);
				props_topol.load (stream);
				stream.close ();
			} catch (Exception e) { e.printStackTrace (); }
		}
		set (tstamp);
	}	
	
	public ItemConfig (Properties props_robot, Properties props_world, Properties props_topol, long tstamp) 
	{
		this.props_robot	= props_robot;
		this.props_world	= props_world;
		this.props_topol 	= props_topol;
		
		set (tstamp);
	}	
}
