/*
 * (c) 2004 Humberto Martinez
 */
 
package wucore.widgets;

import java.awt.*;

class ImageIcon extends Object
{
	public String						label;
	public Image						image;
	
	public int							w;
	public int							h;
	
	public int							x;
	public int							y;
	
	public boolean inIcon (int ex, int ey)
	{
		return (ex >= x-w/2) && (ex <= x+w/2) && (ey >= y-h/2) && (ey <= y+h/2);
	}
}

public class ImageCache extends Object
{
	public ImageIcon[]					images;
	protected int						n;
	
	public ImageCache ()
	{
		images		= new ImageIcon[500];
		n			= 0;
	}
	
	public void add (String label, int w, int h)
	{
		images[n]		= new ImageIcon ();
		images[n].label	= label;
		images[n].w		= w;
		images[n].h		= h;
		
		n++;
	}
	
	public void update (String label, int x, int y)
	{
		int			i;
		
		for (i = 0; i < n; i++)
			if ((images[i].label != null) && (images[i].label.equals (label)))
			{
				images[i].x		= x;
				images[i].y		= y;				
			}
	}
		
	public String inIcon (int x, int y)
	{
		int			i;
		
		for (i = 0; i < n; i++)
			if (images[i].inIcon (x, y))
				return images[i].label;
			
		return null;
	}
}
