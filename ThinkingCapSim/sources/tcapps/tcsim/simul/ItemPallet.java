/*
 * Created on 28-jun-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package tcapps.tcsim.simul;

/**
 * @author SergioPC
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
import java.io.*;

import tc.shared.linda.*;
import devices.pos.*;

/**
 * @author Juan Pedro Canovas Qui�onero
 *
 */
public class ItemPallet extends Item implements Serializable
{
	//Constantes para la variable typepallet
	public static final int			FREE_WITHOUT_BOX 	= 1; 
	public static final int			FREE_WITH_BOX		= 2;
	public static final int			LOADED				= 3;
	
//	Constantes para la variable action
	public static final int 		UNKNOWN_ACTION		=-1;
	public static final int 		ADD					=0;
	public static final int 		DEL					=1;
	public static final int 		MOVE				=2;
	public static final int 		MOVEANDCHANGETYPE	=3;
	
//	Constantes para la variable destiny
	public static final int 		DOCK				=0;
	public static final int 		AGV					=1;
	
	
	
	public String 	idpallet;		// Prioridad
	public Position	position;		// Posicion
	public int 		typepallet;
	public int 		action;
	public int 		destiny;
	public String	robotid;
	

	public ItemPallet()
	{
		this.set (null, null, -1,-1 ,UNKNOWN_ACTION, 0);		
	}
	
	public void set (String idpallet, Position pos, int destiny,int typepallet,int action, long tstamp)
	{
		set (tstamp);
		
		this.idpallet	= 	idpallet;
		this.position	= 	pos;
		this.destiny	= 	destiny;
		this.typepallet	= 	typepallet;
		this.action		= 	action;
	}
	public void setRobotid(String robotid){
		this.robotid=robotid;
	}
	public static String strTypePallet(int typepallet){
		switch(typepallet){
			case FREE_WITHOUT_BOX: return "FREE_WITHOUT_BOX"; 
			case FREE_WITH_BOX: return "FREE_WITH_BOX"; 
			case LOADED: return "LOADED"; 
			default: return "UNKNOWN";
		}
	}
	public static String strAction(int action){
		switch(action){
			case UNKNOWN_ACTION: return "UNKNOWN"; 
			case ADD: return "ADD"; 
			case DEL: return "DEL"; 
			default: return "";
		}
	}
	public static String strDestiny(int destiny){
		switch(destiny){
			case DOCK: return "DOCK"; 
			case AGV: return "AGV"; 
			default: return "UNKNOWN";
		}
	}
	
	public String toString ()
	{
		return "[IDPALLET:"+idpallet + ", POS:"+ position +" STATUS:"+strTypePallet(typepallet)+" ACTION:"+strAction(action)+" DESTINY:"+strDestiny(destiny)+" ROBOT:"+robotid+"]";
	}

}
