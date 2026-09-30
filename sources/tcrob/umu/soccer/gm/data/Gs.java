/**
 * @author Francisco Bas Esparza
 */
package tcrob.umu.soccer.gm.data;

import wucore.utils.math.*;

public class Gs
{
	protected float			quality;
	protected float			reliability;
	protected float			focus;
	
	protected GsPosition		mypos;

	public Gs ()
	{
		mypos = new GsPosition ();
	}
	
	public Gs (int x,int y,float theta,int dx,int dy,float dtheta,float quality)
	{
		mypos = new GsPosition ();
		
		mypos.x = x;
		mypos.y = y;
		mypos.theta = theta;
		mypos.dx = dx;
		mypos.dy = dy;
		mypos.dtheta = dtheta;
		
		this.quality = quality;
	}
	
	public int getX()								{return mypos.x;}
	public int getY()								{return mypos.y;}
	public float getTheta()							{return mypos.theta;}
	public int getDX()								{return mypos.dx;}
	public int getDY()								{return mypos.dy;}
	public float getDTheta()							{return mypos.dtheta;}
	public float getQuality()							{return quality;}
	public float getFocus ()							{return focus;}
	
	public GsPosition getPosition ()					{ return mypos; }
	public void setPosition (GsPosition pos)			{ mypos.set (pos); }
	
	public void setReliability (float reliability)		{ this.reliability = reliability; }
	public void setFocus (float focus)				{ this.focus = focus; }
	public void setQuality (float quality)				{ this.quality = quality; }
	public void updateQuality ()						{ quality = focus * reliability; }
	
	public String toString()
	{
		return (mypos.x+"  "+mypos.y+" "+(int) (Math.round (mypos.theta*Angles.RTOD))+" ("+((int) (quality * 100))/100.0+")");
	}
}
