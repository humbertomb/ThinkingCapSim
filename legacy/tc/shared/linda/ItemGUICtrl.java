/*
 * Created on 26-may-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.linda;

import java.io.*;
import java.util.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ItemGUICtrl extends Item implements Serializable
{
	// Service actions
	static public final int		ACT_BASIC		= 0;
	static public final int		ACT_DEBUG		= 1;
	static public final int		ACT_SERVICE		= 3;
	static public final int		ACT_EVENT		= 4;
	
	// Service control
	static public final int		SVC_START		= 10;
	static public final int		SVC_STOP			= 11;
	static public final int		SVC_TIMEOUT		= 12;
	
	// GUI monitor properties
	public int					type;
	public String				service;
	public String				action;
	public Object[]				params;
	public EventObject			event;
	public long					timeout;
	
	// Constructors
	public ItemGUICtrl () 
	{
	}	
	
	// Instance methods
	public void clear ()
	{
		service	= null;
		action	= null;
		params	= null;
		event	= null;
	}
	
	public void set (int type, long timeout, long tstamp)
	{
		clear ();
		
		this.type		= type;
		this.timeout		= timeout;
		
		set (tstamp);
	}	

	public void set (int type, String service, long tstamp)
	{
		clear ();
		
		this.type		= type;
		this.service		= service;
		
		set (tstamp);
	}	

	public void set (int type, String service, String action, EventObject event, long tstamp)
	{
		clear ();
		
		this.type		= type;
		this.service		= service;
		this.action		= action;
		this.event		= event;
		
		set (tstamp);
	}	

	public void set (int type, String service, String action, Object[] params, long tstamp)
	{
		clear ();
		
		this.type		= type;
		this.service		= service;
		this.action		= action;
		this.params		= params;
		
		set (tstamp);
	}	
}
