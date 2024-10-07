/*
 * (c) 2003 Humberto Martinez
 */
 
package tc.runtime.event;

import tc.runtime.thread.*;
import tc.shared.linda.*;

public class ConfigDesc extends EventDesc
{	
	// Constructors
	public ConfigDesc (StdThread object, Linda linda)
	{
		key		= "CONFIG";
		classn	= "tc.shared.linda.ItemConfig";
		methodn	= "notify_config";
		
		configure (object, linda);
	}
}

