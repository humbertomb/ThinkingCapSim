/*
 * (c) 2003 Humberto Martinez
 */
 
package tc.runtime.event;

import tc.runtime.thread.*;
import tc.shared.linda.*;

public class ExecutionDesc extends EventDesc
{	
	// Constructors
	public ExecutionDesc (StdThread object, Linda linda)
	{
		key		= "EXECUTION";
		classn	= "tc.shared.linda.ItemExecution";
		methodn	= "notify_execution";
		
		configure (object, linda);
	}
}

