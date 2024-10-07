/*
 * (c) 2001-2004 Humberto Martinez
 */

package tc.shared.linda;

public interface Linda
{    
	// Linda data commands
	public boolean	write (Tuple tuple);
	public Tuple		read (Tuple template);
	public Tuple		take (Tuple template);
	
	// Linda control commands
	public void		register (Tuple template, LindaListener listener);
	public void		unregister (Tuple template, LindaListener listener);
	
	// Execution control
	public void		stop ();
}
