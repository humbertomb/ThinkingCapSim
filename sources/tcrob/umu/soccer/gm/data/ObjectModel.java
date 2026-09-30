/*
 * Created on 16-jun-2006
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tcrob.umu.soccer.gm.data;

public class ObjectModel
{
	static public final int		NET		= 0;
	static public final int		LANDMARK	= 1;

	protected int posx, posy;			// Position in world coordinates (mm)
	protected int width, height;		// Dimensions (mm)
	protected int type;				// Type (net, landmark, opponent...)

	public ObjectModel(int type_)
	{
		type = type_;
		
		posx = posy = 0;
		width = height = 0;
	}

	public ObjectModel(int type_, int posx_, int posy_)
	{
		type = type_;
		
		posx = posx_;
		posy = posy_;
		
		width = height = 0;
	}

	public ObjectModel(int type_, int posx_, int posy_, int width_, int height_)
	{
		type = type_;
		
		posx = posx_;
		posy = posy_;
		
		width = width_;
		height = height_;
	}

	public int getPosX()			{ return posx; }
	public int getPosY()			{ return posy; }
	public int getWidth()			{ return width; }
	public int getHeight()		{ return height; }

	public void setPosition (int posx_, int posy_)
	{
		posx = posx_;
		posy = posy_;
	}

	public void setDimensions (int width_, int height_)
	{
		width = width_;
		height = height_;
	}

	public String toString ()
	{
		String		out;
		
		switch(type)
		{
			case NET:
				out = "NET <" + posx +", "+ posy +", "+ width +", "+ height + ">";
				break;
				
			case LANDMARK:
				out = "LM <" + posx +", "+ posy + ">";
				break;

			default:
				out = "N/A type=" + type;
		}
		
		return out;
	}
}
