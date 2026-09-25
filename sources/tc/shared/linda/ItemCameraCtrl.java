/*
 * (c) 2002 Humberto Martinez
 */
 
package tc.shared.linda;

import java.io.*;

import tc.vrobot.*;

public class ItemCameraCtrl extends Item implements Serializable
{
	// Camera control
	public CameraCtrl				camera_ctrl;

	// Constructors
	public ItemCameraCtrl () 
	{
		set (0);
	}	
	
	public void set (CameraCtrl camera_ctrl, long tstamp)
	{
		set (tstamp);
		
		this.camera_ctrl	= camera_ctrl;
	}

	public String toString ()
	{
		return camera_ctrl.toString ();
	}	
}
