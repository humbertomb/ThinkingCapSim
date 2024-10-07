/**
 * Copyright: Copyright (c) 2002
 * Company: Grupo ANTS - Proyecto MIMICS
 * @author Humberto Martinez Barbera
 * @author Juan Pedro Canovas Qui–onero (juanpe@dif.um.es)
 * @version 1.0
 */
 
package devices.drivers.camera;

public class CameraException extends Exception
{
	public CameraException ()
	{
		super ();
	}

	public CameraException (String message)
	{
		super (message);
	}
}