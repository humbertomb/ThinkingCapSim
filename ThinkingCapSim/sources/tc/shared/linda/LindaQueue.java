/*
 * (c) 2001 Humberto Martinez, Juan Pedro Canovas Qui–onero
 * (c) 2002-2004 Humberto Martinez
 */

package tc.shared.linda;

public class LindaQueue extends Object
{
	// Class constants
	static public final int			MAX_QUEUE		= 200;
	
	// Queue data
	protected Tuple[]				queue;
	protected int					ndx_k;
	protected int					ndx_n;
	
	// Constructors
	public LindaQueue () 
	{
		queue	= new Tuple[MAX_QUEUE];
		
		ndx_k	= 0;
		ndx_n	= 0;
	}	
	
	// Instance methods
	public boolean isEmpty ()
	{
		return (ndx_k == ndx_n);
	}
	
	public void add (Tuple tuple)
	{
		ndx_n ++;
		if (ndx_n >= MAX_QUEUE)			ndx_n = 0;
		
		queue[ndx_n]	= tuple;
	}
	
	public Tuple extractFIFO ()
	{
		int			k;
		
		if (isEmpty ())					return null;
		
		k	= ndx_k;
		ndx_k ++;
		if (ndx_k >= MAX_QUEUE)			ndx_k = 0;
		
		return queue[k];
	}
}
