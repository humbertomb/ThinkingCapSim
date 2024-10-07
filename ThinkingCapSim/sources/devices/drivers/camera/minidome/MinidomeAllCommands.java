/*
 * MinidomeAllCommands
 *
 */
package devices.drivers.camera.minidome;

import java.util.*;
import devices.drivers.camera.*;

public class MinidomeAllCommands
{

	Hashtable commands= new Hashtable();

	public MinidomeAllCommands()
	{
		commands.put("DIGITAL_ZOOM_ON",new MinidomeCommand("ZOOM","DIGITAL_ZOOM_ON",new byte[] {2,1,28,3,2,2},"On"));
		commands.put("DIGITAL_ZOOM_OFF",new MinidomeCommand("ZOOM","DIGITAL_ZOOM_OFF",new byte[] {2,1,28,3,3,3},"Off"));
		commands.put("DEGREE_POSITION_MODE",new MinidomeCommand("ORIENTATION","DEGREE_POSITION_MODE",new byte [] {2,1,34,2,1,0,0,0,0,0},""));
		/** Sets the azimut elevation and zoom in degrees b0 b1 -> Azimut  b2 b3 -> Elevation b4 b5 -> Zoom */
		commands.put("SET_AZ_EL_ZOOM",new MinidomeCommand("ORIENTATION","SET_AZ_EL_ZOOM",new byte [] {2,1,34,2},"",6));
		commands.put("ZOOM_IN",new MinidomeCommand("ZOOM","ZOOM_IN",new byte [] {2,1,1,32,0},""));
		commands.put("ZOOM_OUT",new MinidomeCommand("ZOOM","ZOOM_OUT",new byte [] {2,1,1,16,0},""));
		commands.put("FOCUS_IN",new MinidomeCommand("FOCUS","FOCUS_CLOSER",new byte [] {2,1,1,64,0},""));
		commands.put("FOCUS_OUT",new MinidomeCommand("FOCUS","FOCUS_FARTHER",new byte [] {2,1,1,(byte)128,0},""));
		commands.put("AUTOFOCUS_ON",new MinidomeCommand("FOCUS","FOCUS_AUTO",new byte [] {2,1,1,(byte)192,0},""));
		/** Sets the movement velocity in the 2 camera axis b0 -> right-left b1 -> up-down */
		commands.put("MOVEMENT_SPEED",new MinidomeCommand("ORIENTATION","MOVEMENT_SPEED",new byte [] {2,1,14},"",2));
		commands.put("POSITION_RESET",new MinidomeCommand("ORIENTATION","POSITION_RESET",new byte [] {2,1,25,1},""));
		commands.put("DAY_MODE",new MinidomeCommand("VISUAL MODE","DAY_MODE",new byte [] {2,1,32,3,0},"Day mode"));
		commands.put("NIGHT_MODE",new MinidomeCommand("VISUAL MODE","NIGHT_MODE",new byte [] {2,1,32,4,0},"Night mode"));
		commands.put("MOVE_LEFT",new MinidomeCommand("ORIENTATION","MOVE_LEFT",new byte [] {2,1,1,2,0},""));
		commands.put("MOVE_RIGHT",new MinidomeCommand("ORIENTATION","MOVE_RIGHT",new byte [] {2,1,1,1,0},""));
		commands.put("MOVE_UP",new MinidomeCommand("ORIENTATION","MOVE_UP",new byte [] {2,1,1,4,0},""));
		commands.put("MOVE_DOWN",new MinidomeCommand("ORIENTATION","MOVE_DOWN",new byte [] {2,1,1,8,0},""));
		commands.put("STOP_MOVEMENT",new MinidomeCommand("ORIENTATION","STOP_MOVEMENT",new byte [] {2,1,1,0,0},""));
		/** BACK LIGHT ON AND OFF ORDER IS THE SAME????? */
		commands.put("BACK_LIGHT_ON",new MinidomeCommand("BACK LIGHT","BACK_LIGHT_ON",new byte [] {2,1,1,0,3},""));
		commands.put("BACK_LIGHT_OFF",new MinidomeCommand("BACK LIGHT","BACK_LIGHT_OFF",new byte [] {2,1,1,0,3},""));
	
		/** Mixed movements. 2 params array: b0 = 
		  right    -> 00000001
		  left     -> 00000010
		  up       -> 00000100
		  down     -> 00001000
		  zoom-    -> 00010000
		  zoom+    -> 00100000
		  focus-   -> 01000000
		  focus+   -> 10000000
		  autofocus-> 11000000
	  
		  b1 = 0
		*/
		commands.put("MIXED_MOVEMENT",new MinidomeCommand("ORIENTATION","MIXED_MOVEMENT",new byte [] {2,1,1},"",2));
	}

	public Comando getCommand(Comando command)
	{
		return (Comando)commands.get(command.orden);
	}
	
	public Collection getAvaiableCommands()
	{
		return (commands.values());
	}

	/** Return true if the command is included in Minidome */
	public boolean isSupported(Comando comando)
	{
		return commands.containsKey(comando.orden);
	}

}