/**
 * Created on 15-may-2006
 *
 * @author Humberto Martinez Barbera
 */
package tclib.vision.chaos.segment;

import java.awt.*;
import javax.swing.*;

import tclib.vision.chaos.channels.*;

public class LUTGrowing extends LUTStandard
{
	static protected final String[]	DMODES		= { "Box", "Euclidean" };

	static public final int			MAX_TABLE	= 1000;

	static public final int 			M_BOX		= 0;
	static public final int 			M_EUCLIDEAN	= 1;

	static protected int				MODE			= M_BOX;

	private int[]					xlutTable	= new int[MAX_TABLE];
	private int[]					ylutTable	= new int[MAX_TABLE];
	private int[]					zlutTable	= new int[MAX_TABLE];

	public void initialise (Channels chs)
	{
		int				r, g, b;
		int				ch;
		int				ttime;
		long				ctime;

		shiftsR		= 8 - BITS_R;
		sizeR		= 1 << BITS_R;
		shiftsG		= 8 - BITS_G;
		sizeG		= 1 << BITS_G;
		shiftsB		= 8 - BITS_B;
		sizeB		= 1 << BITS_B;

		System.out.print ("  [LUTGrowing] Recomputing LUT values (" + BITS_R + ":" + BITS_G + ":" + BITS_B + " bits) ... ");
		lut		= new int[sizeR][sizeG][sizeB];
		ctime	= System.currentTimeMillis ();

		for (r = 0; r < sizeR; r++)
			for (g = 0; g < sizeG; g++)
				for (b = 0; b < sizeB; b++)
					lut[r][g][b] = NO_COLOR;

		for (r = 0; r < sizeR; r++)
			for (g = 0; g < sizeG; g++)
				for (b = 0; b < sizeB; b++)
				{
					if (lut[r][g][b] != NO_COLOR)		continue;

					// the first channel that takes some colour of the cell grows from it
					for (ch = 0; ch < chs.getNumChannels(); ch++)
						if (chs.at(ch).segmented && takes (chs.at(ch), r, g, b))
						{
							growRGBspace (r, g, b, chs.at (ch));
							break;
						}
				}
		ttime	= (int) (System.currentTimeMillis () - ctime);
		System.out.println (ttime + " ms");
	}

	public void update (Channels chs, int ch)
	{
		int				r, g, b;
		int				ttime;
		long				ctime;

		shiftsR		= 8 - BITS_R;
		sizeR		= 1 << BITS_R;
		shiftsG		= 8 - BITS_G;
		sizeG		= 1 << BITS_G;
		shiftsB		= 8 - BITS_B;
		sizeB		= 1 << BITS_B;

		System.out.print ("  [LUTGrowing] Updating LUT values (" + BITS_R + ":" + BITS_G + ":" + BITS_B + " bits) ... ");
		ctime	= System.currentTimeMillis ();

		// the cells of the channel are made again; the ones of the others are kept (a new table lost them)
		if (lut == null)
		{
			initialise (chs);
			return;
		}
		for (r = 0; r < sizeR; r++)
			for (g = 0; g < sizeG; g++)
				for (b = 0; b < sizeB; b++)
					if (lut[r][g][b] == ch)		lut[r][g][b] = NO_COLOR;

		for (r = 0; r < sizeR; r++)
			for (g = 0; g < sizeG; g++)
				for (b = 0; b < sizeB; b++)
				{
//					if (lut[r][g][b] != NO_COLOR)		continue;

					if ((lut[r][g][b] == NO_COLOR) && takes (chs.at(ch), r, g, b))
						growRGBspace (r, g, b, chs.at (ch));
				}
		ttime	= (int) (System.currentTimeMillis () - ctime);
		System.out.println (ttime + " ms");
	}
	
	protected void growRGBspace (int ro, int go, int bo, Channel channel)
	{
		int		endTable, startTable;
		int		r, g, b;
		int		nr, ng, nb;

		endTable = 0;
		startTable = -1;

		r	= ro;
		g	= go;
		b	= bo;

		// look for neighbours, until the queue is full
		while (startTable != endTable)
		{
			lut[r][g][b] = channel.id;

			// check Y previous cell
			nr = r - 1;
			if (	(nr >= 0) && (lut[nr][g][b] == NO_COLOR) && growRGBpixel (ro, go, bo, nr, g, b, channel.threshold))
			{
				xlutTable[endTable] = nr; 
				ylutTable[endTable] = g; 
				zlutTable[endTable] = b; 

				if (endTable < MAX_TABLE - 1)
					endTable++;
			}

			// check Y next cell
			nr = r + 1;
			if (	(nr < sizeR) && (lut[nr][g][b] == NO_COLOR) && growRGBpixel (ro, go, bo, nr, g, b, channel.threshold))
			{
				xlutTable[endTable] = nr; 
				ylutTable[endTable] = g; 
				zlutTable[endTable] = b; 

				if (endTable < MAX_TABLE - 1)
					endTable++;
			}

			// check U previous cell
			ng = g - 1;
			if (	(ng >= 0) && (lut[r][ng][b] == NO_COLOR) && growRGBpixel (ro, go, bo, r, ng, b, channel.threshold))
			{
				xlutTable[endTable] = r; 
				ylutTable[endTable] = ng; 
				zlutTable[endTable] = b; 

				if (endTable < MAX_TABLE - 1)
					endTable++;
			}

			// check U next cell
			ng = g + 1;
			if (	(ng < sizeG) && (lut[r][ng][b] == NO_COLOR) && growRGBpixel (ro, go, bo, r, ng, b, channel.threshold))
			{
				xlutTable[endTable] = r; 
				ylutTable[endTable] = ng; 
				zlutTable[endTable] = b; 

				if (endTable < MAX_TABLE - 1)
					endTable++;
			}

			// check V previous cell
			nb = b - 1;
			if (	(nb >= 0) && (lut[r][g][nb] == NO_COLOR) && growRGBpixel (ro, go, bo, r, g, nb, channel.threshold))
			{
				xlutTable[endTable] = r; 
				ylutTable[endTable] = g; 
				zlutTable[endTable] = nb; 

				if (endTable < MAX_TABLE - 1)
					endTable++;
			}

			// check V next cell
			nb = b + 1;
			if (	(nb < sizeB) && (lut[r][g][nb] == NO_COLOR) && growRGBpixel (ro, go, bo, r, g, nb, channel.threshold))
			{
				xlutTable[endTable] = r; 
				ylutTable[endTable] = g; 
				zlutTable[endTable] = nb; 

				if (endTable < MAX_TABLE - 1)
					endTable++;
			}

			startTable++;

			// take next position
			r = xlutTable[startTable];
			g = ylutTable[startTable];
			b = zlutTable[startTable];
		}
	}

	protected final boolean growRGBpixel (int r, int g, int b, int nr, int ng, int nb, int thresh)
	{
		r	= r<<shiftsR;
		g	= g<<shiftsG;
		b	= b<<shiftsB;

		nr	= nr<<shiftsR;
		ng	= ng<<shiftsG;
		nb	= nb<<shiftsB;

		switch (MODE)
		{
		case M_BOX:
			if (Math.abs (r - nr) > thresh)		return false;
			if (Math.abs (g - ng) > thresh)		return false;
			if (Math.abs (b - nb) > thresh)		return false;
			break;

		case M_EUCLIDEAN:
			if (Math.sqrt ((r-nr)*(r-nr)+(g-ng)*(g-ng)+(b-nb)*(b-nb)) > thresh)		return false;
			break;
		}

		return true;
	}

	public boolean configurable ()
	{
		return true;
	}

	public void configureDialog (Channels chs)
	{
		new LUTGrowingDialog (chs);
	}

	class LUTGrowingDialog extends JDialog
	{
		protected JComboBox<String> qbitsR = new JComboBox<String> (QBITS);
		protected JComboBox<String> qbitsG = new JComboBox<String> (QBITS);
		protected JComboBox<String> qbitsB = new JComboBox<String> (QBITS);
		protected JComboBox<String> dmodes = new JComboBox<String> (DMODES);

		private Channels chs;

		public LUTGrowingDialog (Channels chs)
		{
			JPanel panel;

			this.chs = chs;

			panel = new JPanel ();
			panel.setLayout (new GridLayout (4, 2));

			panel.add (new JLabel ("Quantization Bits (R)"));
			panel.add (qbitsR);
			panel.add (new JLabel ("Quantization Bits (G)"));
			panel.add (qbitsG);
			panel.add (new JLabel ("Quantization Bits (B)"));
			panel.add (qbitsB);
			panel.add (new JLabel ("Growing Mode"));
			panel.add (dmodes);

			qbitsR.setSelectedIndex (BITS_R-5);
			qbitsG.setSelectedIndex (BITS_G-5);
			qbitsB.setSelectedIndex (BITS_B-5);
			dmodes.setSelectedIndex (MODE);

			setTitle ("LUTRegionGrowing Configuration");
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
			BITS_R	= qbitsR.getSelectedIndex () + 5;
			BITS_G	= qbitsG.getSelectedIndex () + 5;
			BITS_B	= qbitsB.getSelectedIndex () + 5;
			MODE		= dmodes.getSelectedIndex ();			

			initialise (chs);
			setVisible (false);
			dispose ();
		}
	}
}
