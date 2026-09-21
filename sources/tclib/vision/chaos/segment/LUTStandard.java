/**
 * Created on 15-may-2006
 *
 * @author Humberto Martinez Barbera
 */
package tclib.vision.chaos.segment;

import java.awt.*;
import javax.swing.*;

import tclib.vision.chaos.channels.*;


public class LUTStandard extends LUT
{
	static protected final String[]	QBITS		= { "5", "6", "7", "8" };

	public LUTStandard ()
	{
	}

	public void initialise (Channels chs)
	{
		int				ttime;
		long				ctime;

		shiftsR		= 8 - BITS_R;
		sizeR		= 1 << BITS_R;
		shiftsG		= 8 - BITS_G;
		sizeG		= 1 << BITS_G;
		shiftsB		= 8 - BITS_B;
		sizeB		= 1 << BITS_B;

		System.out.print ("  [LUTStandard] Recomputing LUT values (" + BITS_R + ":" + BITS_G + ":" + BITS_B + " bits) ... ");
		ctime	= System.currentTimeMillis ();

		// the first channel that takes some colour of each cell (a plane of the table per core: they are apart),
		// in a table of its own, which replaces the one in use when it is done (frames go on being segmented)
		final int[][][]	table = new int[sizeR][sizeG][sizeB];
		java.util.stream.IntStream.range (0, sizeR).parallel ().forEach (pr ->
		{
			for (int pg = 0; pg < sizeG; pg++)
				for (int pb = 0; pb < sizeB; pb++)
					table[pr][pg][pb] = labelOf (chs, pr, pg, pb);
		});
		lut		= table;
		ttime	= (int) (System.currentTimeMillis () - ctime);
		System.out.println (ttime + " ms");
	}

	/**
	 * A channel changed (its seeds): the table is made again, so that the cells
	 * it no longer takes are free and the ones it shares go to the first channel,
	 * as when it is made.
	 */
	public void update (Channels chs, int ch)
	{
		initialise (chs);
	}
	
	public boolean configurable ()
	{
		return true;
	}

	public void configureDialog (Channels chs)
	{
		new LUTStandardDialog (chs);
	}

	class LUTStandardDialog extends JDialog
	{
		protected JComboBox<String> qbitsR = new JComboBox<String> (QBITS);
		protected JComboBox<String> qbitsG = new JComboBox<String> (QBITS);
		protected JComboBox<String> qbitsB = new JComboBox<String> (QBITS);

		private Channels chs;

		public LUTStandardDialog (Channels chs)
		{
			JPanel panel;

			this.chs = chs;

			panel = new JPanel ();
			panel.setLayout (new GridLayout (3, 2));

			panel.add (new JLabel ("Quantization Bits (R)"));
			panel.add (qbitsR);
			panel.add (new JLabel ("Quantization Bits (G)"));
			panel.add (qbitsG);
			panel.add (new JLabel ("Quantization Bits (B)"));
			panel.add (qbitsB);

			qbitsR.setSelectedIndex (BITS_R-5);
			qbitsG.setSelectedIndex (BITS_G-5);
			qbitsB.setSelectedIndex (BITS_B-5);

			setTitle ("LUTStandard Configuration");
			setModal (true);
			setLocation (new Point (50, 50));
			setSize (new Dimension (300, 80));
			getContentPane().setLayout (new GridLayout (1, 1));
			getContentPane().add (panel);

			// event handling
			addWindowListener(new java.awt.event.WindowAdapter() {
				public void windowClosing(java.awt.event.WindowEvent e) {
					thisWindowClosing(e);
				}
			});

			setVisible (true);
		}

		protected void thisWindowClosing (java.awt.event.WindowEvent e) 
		{
			BITS_R		= qbitsR.getSelectedIndex () + 5;
			BITS_G		= qbitsG.getSelectedIndex () + 5;
			BITS_B		= qbitsB.getSelectedIndex () + 5;

			initialise (chs);

			setVisible (false);
			dispose ();
		}
	}
}
