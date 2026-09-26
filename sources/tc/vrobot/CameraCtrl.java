/*
 * (c) 2002 Humberto Martinez
 * (c) 2026 Humberto Martinez
 */
 
package tc.vrobot;

public class CameraCtrl
{
	// Camera control
	public double					pan;				// Pan movement control
	public double					tilt;				// Tilt movement control
	public double					zoom;				// Zoom aperture control

	public void set (CameraCtrl other)
	{
		this.pan		= other.pan;
		this.tilt		= other.tilt;
		this.zoom		= other.zoom;
	}
		
	public void set (double pan, double tilt)
	{
		this.pan		= pan;
		this.tilt		= tilt;
	}
	
	public void set (double pan, double tilt, double zoom)
	{
		this.pan		= pan;
		this.tilt		= tilt;
		this.zoom		= zoom;
	}
	
	public String toString ()
	{
		return "pan=" + pan + ", tilt=" + tilt + ", zoom=" + zoom;
	}	
}
