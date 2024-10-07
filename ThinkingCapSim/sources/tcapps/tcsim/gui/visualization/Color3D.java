/*
 * (c) 2001 Humberto Martinez
 * (c) 2004 Humberto Martinez
 */
 
package tcapps.tcsim.gui.visualization;

import java.awt.*;

import javax.vecmath.*;

public class Color3D extends Object
{
	public static final Color3f white 		= new Color3f(1.0f, 1.0f, 1.0f);
	public static final Color3f black 		= new Color3f(0.0f, 0.0f, 0.0f);
	public static final Color3f gray 			= new Color3f(0.6f, 0.6f, 0.6f);
	public static final Color3f red   		= new Color3f(1.0f, 0.0f, 0.0f);
	public static final Color3f ambientred 	= new Color3f(0.4f, 0.1f, 0.0f);
	public static final Color3f medred 		= new Color3f(0.80f, 0.4f, 0.3f);
	public static final Color3f green   		= new Color3f(0.0f, 0.80f, 0.2f);
	public static final Color3f ambientgreen 	= new Color3f(0.0f, 0.3f, 0.1f);
	public static final Color3f medgreen 		= new Color3f(0.0f, 0.5f, 0.1f);
	public static final Color3f orange   		= new Color3f(0.7f, 0.4f, 0.0f);
	public static final Color3f ambientorange 	= new Color3f(0.5f, 0.02f, 0.0f);
	public static final Color3f medorange 		= new Color3f(0.5f, 0.2f, 0.1f);
	public static final Color3f blue   		= new Color3f(0.1f, 0.3f, 0.9f);
	public static final Color3f ambientblue 	= new Color3f(0.0f, 0.1f, 0.4f);
	public static final Color3f medblue 		= new Color3f(0.0f, 0.1f, 0.4f);
	public static final Color3f gold 			= new Color3f(1.0f, 0.8f, 0.0f);
	public static final Color3f yellow 		= new Color3f(1.0f, 1.0f, 0.6f);
	public static final Color3f purple 		= new Color3f(0.5f, 0.2f, 0.8f);
	public static final Color3f medpurple 		= new Color3f(0.5f, 0.2f, 0.5f);
	public static final Color3f ambient 		= new Color3f(0.2f, 0.2f, 0.2f);
	public static final Color3f diffuse 		= new Color3f(0.7f, 0.7f, 0.7f);
	public static final Color3f specular 		= new Color3f(0.7f, 0.7f, 0.7f);
	
	static public final Color3f toColor (Color color)
	{
		float		r, g, b;
		
		r	= (float) color.getRed () / 255.0f;
		g	= (float) color.getGreen () / 255.0f;
		b	= (float) color.getBlue () / 255.0f;

		return new Color3f (r, g, b);
	}
}

