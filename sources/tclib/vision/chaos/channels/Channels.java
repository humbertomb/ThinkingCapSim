/**
 	@author Humberto Martinez Barbera
 */

package tclib.vision.chaos.channels;

import java.util.*;

public class Channels
{
	static public final int		NO_COLOR  = -1;

	public ArrayList<Channel> channels;
	
	public Channels()
	{
		channels = new ArrayList<Channel> ();
	}	
		
	public final int size ()						{ return channels.size (); }
	public final Channel at (int i)				{ return channels.get (i); }
	public final void at (int i, Channel ch)		{ channels.set (i, ch); }
						
	public Channel getChannelID (int id)
	{
		int			i;
		
		for (i = 0; i < channels.size (); i++)
		{
			Channel		ch;
			
			ch	= (Channel) channels.get (i);
			if (ch.id == id)
				return ch;
		}
		
		return null;
	}
	
	public int computeHash ()
	{
		int			i;
		int			hash = 0;
		
		for (i = 0; i < channels.size (); i++)
			hash += ((Channel) channels.get (i)).getSequence ();
		
		return hash;
	}
	
	public int getNumChannels()
	{
		return channels.size();
	}
				
	public Vector<Integer> testCollisions()
	{
		int i, j;
		Vector<Integer> v = new Vector<Integer>();
		
		for (i = 0; i < channels.size() - 1; i++)
			if (at(i).segmented)
				for (j = i + 1; j < channels.size(); j++)
					if (at(j).segmented)
						if (at (i).getCluster ().intersection (at (j).getCluster ()))
						{
							v.add(Integer.valueOf (i));
							v.add(Integer.valueOf (j));
						}
		
		return v;
	}
}