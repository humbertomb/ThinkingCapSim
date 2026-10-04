/**
 * Copyright:    Copyright (c) 2002
 * Company:      Grupo ANTS - Proyecto MIMICS
 * @author Humberto Martinez barbera
 * @version 1.0
 */

package devices.data;

import java.io.*;

import tclib.utils.pos.*;

public class BeaconData implements Serializable
{
	private int				number;
	private int				quality;
	private Position		pos;
	private boolean			valid;
	private double[]		bearings	= new double[0];	// the reflectors seen: where each is from the scanner (rad, from where it looks)
	private double[]		ranges		= new double[0];	// ... and how far (m)
	
	// Constructors
	public BeaconData ()
	{
		quality		= -1;
		number 		= -1;
		valid		= false;
		pos			= new Position ();
	}

	// Accessors
	public final int			getQuality ()				{ return quality; }
	public final int			getNumber ()				{ return number; }
	public final Position		getPosition ()				{ return pos; }
	public final boolean		isValid ()					{ return valid; }
	
	public final void			setQuality (int qlty)		{ quality = qlty; }
	public final void			setNumber (int n)			{ number = n; }
	public final void			setPosition (Position pos)	{ this.pos.set (pos); }
	public final void			setValid (boolean ok)		{ this.valid = ok; }

	/** How many reflectors it sees. */
	public final int			seen ()						{ return Math.min (bearings.length, ranges.length); }
	/** Where the k-th reflector it sees is, from the scanner and from where it looks (rad). */
	public final double			bearing (int k)				{ return bearings[k]; }
	/** How far it is (m). */
	public final double			range (int k)				{ return ranges[k]; }
	/** The reflectors it sees: the bearing (rad) and range (m) of each. */
	public final void			setSeen (double[] bearings, double[] ranges)
	{
		this.bearings	= (bearings != null) ? bearings.clone () : new double[0];
		this.ranges		= (ranges != null) ? ranges.clone () : new double[0];
	}
	
	// Instance methods
	public BeaconData dup ()
	{
		BeaconData		ret;

		ret 	= new BeaconData ();
		ret.set (this);

		return ret;
	}

	public void set (BeaconData other)
	{
		quality		= other.quality;
		number		= other.number;
		valid		= other.valid;
		pos.set (other.pos);
		bearings	= other.bearings.clone ();
		ranges		= other.ranges.clone ();
	}
	
	public String toString(){
	    if(valid)
	        return "Pos: "+pos+" Q: "+quality+" N: "+number;
	    return "Invalid data";
	}
	
}