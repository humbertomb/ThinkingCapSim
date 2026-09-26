/*
 * (c) 2002 Humberto Martinez
 * (c) 2026 Humberto Martinez
 */
 
package tc.shared.linda;

import java.io.*;

import tc.vrobot.*;

public class ItemCameraCtrl extends Item implements Serializable
{
	// Camera control
	public int 						device;
	
	public CameraCtrl				camera_ctrl;

	// Constructors
	public ItemCameraCtrl () 
	{
		set (0);
	}	
	
	public void set (int device, CameraCtrl camera_ctrl, long tstamp)
	{
		set (tstamp);
		
		this.device			= device;
		this.camera_ctrl	= camera_ctrl;
	}

	public String toString ()
	{
		return "CAM"+device+": " + camera_ctrl.toString ();
	}	
}
