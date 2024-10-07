/**
 * Copyright:    Copyright (c) 2002
 * Company:      Grupo ANTS - Proyecto MIMICS
 * @author Humberto Martinez barbera
 * @version 1.0
 */

package devices.data;

import java.io.*;

import devices.pos.*;

public class BeaconData implements Serializable
{
	private int				number;
	private int				quality;
	private Position		pos;
	private boolean			valid;
	
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
	}
	
	public String toString(){
	    if(valid)
	        return "Pos: "+pos+" Q: "+quality+" N: "+number;
	    return "Invalid data";
	}
	
}