/*
 * Created on 29-jun-2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package tcapps.tcsim;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import tc.ExecArchMulti;
import tc.runtime.thread.StdThread;
import tc.runtime.thread.ThreadDesc;
import tc.shared.linda.Linda;
import tc.shared.linda.LindaNetClient;
import tc.shared.linda.net.LindaNet;
import tcapps.tcsim.simul.Simulator;
import tcapps.tcsim.simul.objects.SimMultiCargo;


/**
 * @author SergioPC
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class ExecArchMultiPallet extends ExecArchMulti
{
	protected Simulator			sim;
	protected Properties 		typepalletprops;
	
	public ExecArchMultiPallet (String name,String pathtypepallet, Simulator sim){
				
		super (name);
		this.sim	= sim;
		
		File			file;
		FileInputStream	stream;

		typepalletprops		= new Properties ();
		try{
			file 		= new File (pathtypepallet);
			stream 		= new FileInputStream (file);
			props.load (stream);
			stream.close ();
		}catch (Exception e) {
			e.printStackTrace (); 
		}
		initialise (props, typepalletprops);
	}
	
	public void run(){
		StdThread 	thread;
		String 		vcmodule;
		ThreadDesc	vcdesc=null;
		
		vcmodule		= props.getProperty ("VCARGO");
		if (vcmodule != null)
			vcdesc		= new ThreadDesc (vcmodule, props);
			if (vcdesc != null){
				System.out.println (">> Starting Simulated Pallet [" + vcdesc.preffix + "@" + null + "]");

				Linda linda = null;	
			
				if (vcdesc.mode == ThreadDesc.M_UDP){
					try{
						linda = new LindaNetClient (LindaNet.UDP, null, gldesc.addr, gldesc.port);
					}
					catch (Exception e){
						e.printStackTrace();
						linda = null;
					}
				}		
				else if (vcdesc.mode == ThreadDesc.M_TCP){
				try{
					linda = new LindaNetClient (LindaNet.TCP, null, gldesc.addr, gldesc.port);
				}
				catch (Exception e)
				{
					e.printStackTrace();
					linda = null;
				}
			}
			
			if (linda != null)
			{
				thread	= new SimMultiCargo (props, linda, sim);
				thread.setTDesc (vcdesc);
				thread.start ();

			}else{
				System.out.println("--[ExecArchMultiPallet] run() linda=null");
			}
		}
	}
	
}
