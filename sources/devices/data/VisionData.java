/*
 * (c) 2002 Humberto Martinez
 */
 
package devices.data;

import java.io.*;
import java.awt.*;

import devices.pos.*;

public class VisionData extends Object implements Serializable 
{
	// Type of object recognised
	public String				id;
	public int					device;
	public boolean				valid;
	
	// Robot current position
	public Position				cpos;					// Capture robot position (m, m, rad)

	// Object robot-local position
	public double				rho;					// Current object coordinates (m, rad)
	public double				phi;
	
	// Object sensor-local position
	public double				pos3_x;					// Current object coordinates (m, m, m)
	public double				pos3_y;
	public double				pos3_z;

	// Raw object features (vision space)
	public int					pos2_x;					// Object position (pixels)
	public int					pos2_y;
	public int					width;					// Object size (pixels)
	public int					height;
	public Color			color;					// Segmented color code
	
	/* Constructors */
	public VisionData ()
	{
		this.id			= "noname";
		this.device		= 0;
		this.valid		= false;
		this.cpos		= new Position ();
	}
	
	// Instance methods
	public void set (VisionData data)
	{
		this.id			= data.id;
		this.device		= data.device;

		this.pos2_x		= data.pos2_x;
		this.pos2_y		= data.pos2_y;
		this.width		= data.width;
		this.height		= data.height;
		this.color		= data.color;
		
		this.pos3_x		= data.pos3_x;
		this.pos3_y		= data.pos3_y;
		this.pos3_z		= data.pos3_z;

		this.rho		= data.rho;
		this.phi		= data.phi;
		
		this.cpos.set (data.cpos);

		this.valid		= data.valid;
	}

	public void set_dev (int device)
	{
		this.device		= device;
	}

	public void set_blob (String id, int px, int py, int w, int h, Color c)
	{
		this.id			= id;
		
		this.pos2_x		= px;
		this.pos2_y		= py;
		this.width		= w;
		this.height		= h;
		this.color		= c;
		
		this.valid		= true;
	}

	public void percept_pos (double px, double py, double pz)
	{
		pos3_x	= px;
		pos3_y	= py;
		pos3_z	= pz;
	}
	
	public void sensor_pos (double sx, double sy, double sa)
	{
		double			ll, aa;
		double			xx, yy;
		
		ll		= Math.sqrt (pos3_z * pos3_z + pos3_x * pos3_x);
		aa		= Math.atan2 (pos3_x, pos3_z);

		xx		= sx + ll * Math.cos (sa + aa);
		yy		= sy + ll * Math.sin (sa + aa);
		
		rho		= Math.sqrt (xx * xx + yy * yy);
		phi		= Math.atan2 (yy, xx);
	}

	public void capture_pos (Position prev, Position cur)
	{
		cpos.set (cur);
		cpos.delta (prev);
	}

	public VisionData dup ()
	{
		VisionData		data;
		
		data		= new VisionData ();
		data.set (this);
		
		return data;
	}
} 