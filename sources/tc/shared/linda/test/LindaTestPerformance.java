/*
 * Created on 11-mar-2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package tc.shared.linda.test;

import java.util.*;

import tc.runtime.thread.*;
import tc.shared.linda.*;

/**
 * @author Humberto Martinez Barbera
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class LindaTestPerformance
{
	static public final String		PREFFIX		= "MOD";
	static public final long			TIME			= 2000;
	
	protected LindaDesc				ldesc;				// Linda Space description
	protected LindaServer			linda;				// Linda Space server
	
	protected ThreadDesc[]			cthdesc;				// Consumer client processes
	protected LindaTestConsumer[]	cthread;				// Consumer threads
	protected ThreadDesc[]			pthdesc;				// Producer client processes
	protected LindaTestProducer[]	pthread;				// Producer threads
	protected ThreadDesc[]			rthdesc;				// Reader client processes
	protected LindaTestReader[]		rthread;				// Reader threads
	
	protected int					ccount;				// Consumer events count
	protected int					pcount;				// Producer events count
	protected int					rcount;				// Reader events count

	protected double					ctime;				// Consumer event processing time

	// Constructors
	public LindaTestPerformance (int consumers, int producers, int readers, String mode)
	{
		int				i;
		Properties		sprops;
		Properties		cprops;
		Properties		pprops;
		Properties		rprops;
		
		// Linda Space properties
		sprops	= new Properties ();
		sprops.setProperty ("LINADDR", "localhost");
		sprops.setProperty ("LINPORT", "5500");

		// Consumer process properties
		cprops	= new Properties ();
		cprops.setProperty (PREFFIX + "CLASS", "tc.shared.linda.test.LindaTestConsumer");
		cprops.setProperty (PREFFIX + "MODE", mode);
		cprops.setProperty (PREFFIX + "CONNECT", "DATA tc.shared.linda.ItemData notify_data");			

		// Producer process properties
		pprops	= new Properties ();
		pprops.setProperty (PREFFIX + "CLASS", "tc.shared.linda.test.LindaTestProducer");
		pprops.setProperty (PREFFIX + "MODE", mode);
//		pprops.setProperty (PREFFIX + "CONNECT", "DATA tc.shared.linda.ItemData notify_data");			
		
		// Reader process properties
		rprops	= new Properties ();
		rprops.setProperty (PREFFIX + "CLASS", "tc.shared.linda.test.LindaTestReader");
		rprops.setProperty (PREFFIX + "MODE", mode);
		
		try
		{
			// Create service threads
			ldesc		= new LindaDesc ("LIN", LindaDesc.L_GLOBAL, sprops);			
			linda		= ldesc.start_server ();
			
			cthdesc		= new ThreadDesc[consumers];
			cthread		= new LindaTestConsumer[consumers];
			for (i = 0; i < consumers; i++)
			{
				cthdesc[i]	= new ThreadDesc (PREFFIX, cprops);
				cthdesc[i].start_thread ("Consumer-" + i, cprops, ldesc, linda);
				cthread[i]	= (LindaTestConsumer) cthdesc[i].thread;
			}

			pthdesc		= new ThreadDesc[producers];
			pthread		= new LindaTestProducer[producers];
			for (i = 0; i < producers; i++)
			{
				pthdesc[i]	= new ThreadDesc (PREFFIX, pprops);
				pthdesc[i].start_thread ("Producer-" + i, pprops, ldesc, linda);
				pthread[i]	= (LindaTestProducer) pthdesc[i].thread;
			}
			
			rthdesc		= new ThreadDesc[readers];
			rthread		= new LindaTestReader[readers];
			for (i = 0; i < readers; i++)
			{
				rthdesc[i]	= new ThreadDesc (PREFFIX, rprops);
				rthdesc[i].start_thread ("Reader-" + i, rprops, ldesc, linda);
				rthread[i]	= (LindaTestReader) rthdesc[i].thread;
			}
			
			// Perform processing
			Tuple				dtuple;
			ItemDebug			ditem;

			ditem	= new ItemDebug ();
			dtuple	= new Tuple (Tuple.DEBUG, ditem);
			ditem.command (ItemDebug.START, System.currentTimeMillis ());
			linda.write (dtuple);
			
			try { Thread.sleep (TIME); } catch (Exception e) { }

			ditem.command (ItemDebug.STOP, System.currentTimeMillis ());
			linda.write (dtuple);

			// Finish processing
			for (i = 0; i < producers; i++)
				pthread[i].stop ();
			for (i = 0; i < consumers; i++)
				cthread[i].stop ();
			for (i = 0; i < readers; i++)
				rthread[i].stop ();

			// Compute statistics
			for (i = 0; i < producers; i++)
				pcount	+= pthread[i].count;
			for (i = 0; i < readers; i++)
				rcount	+= rthread[i].count;
			for (i = 0; i < consumers; i++)
			{
				ccount	+= cthread[i].count;
				ctime	+= cthread[i].tavg;
			}
			ctime	/= (double) ccount;
					
			// Stop Linda server
			linda.stop ();
		}
		catch (Exception e) { e.printStackTrace (); }
	}
	
	// Class methods
	static public void main (String[] argv)
	{
/*		int			i, j, k;
		int			consumers = 1;
		int			producers = 1; // 5;
		int			readers = 1;
		
		for (i = 1; i <= producers; i++)
			for (j = 1; j <= consumers; j++)
				for (k = 1; k <= readers; k++)
					test (1, j, i, k);
*/
		test (1, 10, 5, 5);
	}
	
	static public void test (int times, int consumers, int producers, int readers)
	{
		LindaTestPerformance		test;
		int						i;
		int						upcount = 0, uccount = 0, urcount = 0, uloss = 0;
		int						tpcount = 0, tccount = 0, trcount = 0, tloss = 0;
		int						spcount = 0, sccount = 0, srcount = 0, sloss = 0;
		int						utime, ttime, stime;
		double					uploss, tploss, sploss;
		double					uavg = 0.0, tavg = 0.0, savg = 0.0;
		
		for (i = 0; i < times; i++)
		{
			System.err.println ("################## Running ITERATION "+i);
			
			System.out.println ("\t########## Running <UDP> test");
			test	= new LindaTestPerformance (consumers, producers, readers, "udp");
			upcount	+= test.pcount;
			uccount	+= test.ccount;
			urcount	+= test.rcount;
			uloss	+= test.pcount * consumers - test.ccount;
			uavg		+= test.ctime;
			
			try { Thread.sleep (1000); } catch (Exception e) { } 
			
			System.out.println ("\t########## Running <TCP> test");
			test 	= new LindaTestPerformance (consumers, producers, readers, "tcp");
			tpcount	+= test.pcount;
			tccount	+= test.ccount;
			trcount	+= test.rcount;
			tloss	+= test.pcount * consumers - test.ccount;
			tavg		+= test.ctime;
			
			try { Thread.sleep (1000); } catch (Exception e) { }
			
			System.out.println ("\t########## Running <SHARED> test");
			test 	= new LindaTestPerformance (consumers, producers, readers, "shared");
			spcount	+= test.pcount;
			sccount	+= test.ccount;
			srcount	+= test.rcount;
			sloss	+= test.pcount * consumers - test.ccount; 
			savg		+= test.ctime;
			
			try { Thread.sleep (1000); } catch (Exception e) { }
		}

		uploss	= Math.round ((1.0 - (double) uccount / (double) (upcount * consumers)) * 10000.0) / 100.0;
		tploss	= Math.round ((1.0 - (double) tccount / (double) (tpcount * consumers)) * 10000.0) / 100.0;
		sploss	= Math.round ((1.0 - (double) sccount / (double) (spcount * consumers)) * 10000.0) / 100.0;

		utime	= (int) Math.round (uavg / times);
		ttime	= (int) Math.round (tavg / times);
		stime	= (int) Math.round (savg / times);

		System.err.println ("################## Results ("+i+"/"+consumers+"/"+producers+"/"+readers+")");
		System.out.println ("\tMode\tWrite\tRead\tEvent\tLoss\t\t\tTime");
		System.out.println ("\tUDP\t"+upcount+"\t"+urcount+"\t"+uccount+"\t"+uloss+"\t("+uploss+"%)\t\t"+utime);
		System.out.println ("\tTCP\t"+tpcount+"\t"+trcount+"\t"+tccount+"\t"+tloss+"\t("+tploss+"%)\t\t"+ttime);
		System.out.println ("\tSHARED\t"+spcount+"\t"+srcount+"\t"+sccount+"\t"+sloss+"\t("+sploss+"%)\t\t"+stime);
	}
}
