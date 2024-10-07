/** 
 * ItemCoordination.java
 *
 * Title:			TCApps
 * Description:		
 * @author			juanpe
 * @version			04-nov-03
 *
 */
package tcrob.ingenia.ifork.linda;

import java.io.*;

import tc.shared.linda.*;
import devices.pos.*;

/**
 * @author Juan Pedro Canovas Qui�onero
 *
 */
public class ItemCoordination extends Item implements Serializable
{
	public static final int			ACTION_OCCUPED = 2; 
	public static final int			ACTION_FREE	= 1;
	public static final int			ACTION_NONE	= 0;
	
	
	
	public long		priority;		// Prioridad
	public Position	position;		// Posicion
	public boolean	passed;
	
	public int		task;			// Tarea actual
	public String	goal;			// nombre del destino de la tarea
	
	public String	booked;			// WP para ocupar/desocupar
	public int		bk_action;		// Accion a realizar

	public ItemCoordination()
	{
		this.set (0, null, 0);		
	}
	
	public void set (long priority, Position pos, long tstamp)
	{
		set (tstamp);
		
		this.priority		= priority;
		this.position		= pos;
		
		passed = false;
		booked = null;
	}
	
	public void set (String wp) { booked = wp; }
	
	public String toString ()
	{
		return "<POS: "+position.toString() + ", PRIO: "+ priority +" ACTION: "+bk_action+" BOOKED: "+booked+" TASK:"+task+" GOAL:"+goal+">";
	}

}
