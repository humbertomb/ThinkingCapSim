/*
 * (c) 2003 Humberto Martinez
 */
 
package tc.runtime.event;

import tc.runtime.thread.*;
import tc.shared.linda.*;

public class DebugDesc extends EventDesc
{	
	// Constructors
	public DebugDesc (StdThread object, Linda linda)
	{
		key		= "DEBUG";
		classn	= "tc.shared.linda.ItemDebug";
		methodn	= "notify_debug";
		
		configure (object, linda);
	}
}

