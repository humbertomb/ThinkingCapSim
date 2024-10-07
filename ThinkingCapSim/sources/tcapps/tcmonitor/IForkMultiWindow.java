/*
 * Created on 08-jul-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package tcapps.tcmonitor;

import java.io.*;

import tcapps.tcsim.*;
import tcapps.tcsim.gui.*;
import tcapps.tcsim.simul.*;

import tc.*;
import wucore.utils.geom.*;

/**
 * @author SergioPC
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class IForkMultiWindow 
{
	
	static public final String			TITTLE		= "BGenWeb Login Window";
	public static final int NUMAGV = 3;
	
	public IForkMultiWindow()
	{
		Simulator		simul;
		ExecArchSim		exec;
		
//		Create Standard Service
		new ExecArchMulti ("conf/archs/glinda.arch").start ();
//		Create Standard Client
		new ExecArchMulti ("conf/archs/gmon.arch").start ();
		try { Thread.sleep (2000); } catch (Exception e) { }
		
//		Create Simulator
		simul = new Simulator ();
		new SimulatorWindow(simul,SimulatorWindow.ADMIN);
		
		new ExecArchMultiPallet("."+File.separator+"conf"+File.separator+"pallet"+File.separator+"pallet.arch","."+File.separator+"conf"+File.separator+"pallet"+File.separator+"typepallet.cfg",simul).start();
		
		for(int i=1;i<=NUMAGV;i++){
			exec		= new ExecArchSim ("IFORK-"+i, "conf/archs/ifork.arch", null, simul);
			exec.start ();
			exec.setStart (new Point3 (75.0, 65.0-((i-1)*2), Math.PI));
			
			try { Thread.sleep (3000); } catch (Exception e) { }	
		}
		try { Thread.sleep (5000); } catch (Exception e) { }
		simul.smc.startSimul();
		
	}

	public static void main(String argv[]){
		new IForkMultiWindow();
	}
}
