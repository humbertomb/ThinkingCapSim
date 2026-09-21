/**
 * Created on 24-nov-2005
 *
 * @author Humberto Martinez Barbera
 */
package tcrob.umu.quaky2.gui.ctables.colspace;

import java.awt.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.*;

import ptolemy.plot.*;

import tclib.vision.chaos.channels.*;
import tclib.vision.chaos.segment.*;
import tcrob.umu.quaky2.gui.images.CPScalableImageCanvas;

public class CPSpaceWindow extends JFrame
{
	static public final int				WIDTH	= 800;
	static public final int				HEIGHT	= 600;
	
	static protected final int			HSV		= 0;
	static protected final int			YUV		= 1;
	static protected final int			RGB		= 2;

	static protected final String[]		SPACES	= { "HSV", "YUV", "RGB" };
	static protected final String[][]		LABELS	= { { "H", "S", "V", "SV", "HV", "HS" },
													{ "Y", "U", "V", "UV", "YV", "YU" }, 
													{ "R", "G", "B", "GB", "RB", "RG" } };

	protected JPanel[] projs;
	protected CPSpacePlot	 cpspaceplot;
	protected CPScalableImageCanvas cpproj12;
	protected CPScalableImageCanvas cpproj02;
	protected CPScalableImageCanvas cpproj01;
	
	protected JPanel[] histos;
	protected Histogram cphisto0;
	protected Histogram cphisto1;
	protected Histogram cphisto2;

	protected JComboBox<String> spaceCB;
	protected int curspace;
	protected BufferedImage rgb;
	protected Channels chs;

	public CPSpaceWindow () 
	{
		JPanel		pane;
		JTabbedPane	tabs;

		tabs		= new JTabbedPane ();
		tabs.setTabPlacement (JTabbedPane.TOP);
		tabs.addTab ("Color Space", createSpacePanel ());
		tabs.addTab ("Histograms", createHistogramPanel ());	

		pane		= (JPanel) getContentPane ();
		pane.setLayout (new BorderLayout());
		pane.add (createToolsPanel (), BorderLayout.NORTH);
		pane.add (tabs, BorderLayout.CENTER);

		setTitle ("Color Space Analysis");	
		setSize (new Dimension (WIDTH, HEIGHT));
		setVisible (true);
	}
	
	protected JPanel createToolsPanel ()
	{
		JPanel		panel;
	
		spaceCB	= new JComboBox<String> (SPACES);
		curspace = 0;
		
		panel	= new JPanel ();
//		panel.setLayout (new GridLayout(2, 2));
		panel.add (spaceCB);
	
		spaceCB.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				curspace		= spaceCB.getSelectedIndex ();
				
				renameSubpanel (projs[0], LABELS[curspace][3] + " Projection");
				renameSubpanel (projs[1], LABELS[curspace][4] + " Projection");
				renameSubpanel (projs[2], LABELS[curspace][5] + " Projection");

				renameSubpanel (histos[0]	, LABELS[curspace][0] + " Histogram");
				renameSubpanel (histos[1]	, LABELS[curspace][1] + " Histogram");
				renameSubpanel (histos[2]	, LABELS[curspace][2] + " Histogram");

				if (rgb != null)
					update (rgb, chs);
			}
		});

		return panel;
	}
	
	protected JPanel createSpacePanel ()
	{
		JPanel		panel;
		
		try
		{
			cpspaceplot		= new CPSpacePlot ();
		} catch (Exception e) { cpspaceplot = null; }

		cpproj12 = new CPScalableImageCanvas ();
		cpproj12.setSize (new Dimension (SpaceOrtoProjection.SIZE, SpaceOrtoProjection.SIZE));
		cpproj02 = new CPScalableImageCanvas ();
		cpproj02.setSize (new Dimension (SpaceOrtoProjection.SIZE, SpaceOrtoProjection.SIZE));
		cpproj01 = new CPScalableImageCanvas ();
		cpproj01.setSize (new Dimension (SpaceOrtoProjection.SIZE, SpaceOrtoProjection.SIZE));

		projs	= new JPanel[3];
		projs[0]	= createSubpanel (LABELS[curspace][3] + " Projection", cpproj12);
		projs[1]	= createSubpanel (LABELS[curspace][4] + " Projection", cpproj02);
		projs[2]	= createSubpanel (LABELS[curspace][5] + " Projection", cpproj01);
		
		panel	= new JPanel ();
		panel.setLayout (new GridLayout(2, 2));
		panel.add (projs[0]);
		panel.add (projs[1]);
		panel.add (projs[2]);
		panel.add (createSubpanel ("3D View", cpspaceplot));

		return panel;
	}
	
	protected JPanel createHistogramPanel ()
	{
		JPanel		panel;
		
		cphisto0 = createHistogram ();
		cphisto1 = createHistogram ();
		cphisto2 = createHistogram ();

		histos		= new JPanel[3];
		histos[0]	= createSubpanel (LABELS[curspace][0] + " Histogram", cphisto0);
		histos[1]	= createSubpanel (LABELS[curspace][1] + " Histogram", cphisto1);
		histos[2]	= createSubpanel (LABELS[curspace][2] + " Histogram", cphisto2);

		panel	= new JPanel ();
		panel.setLayout (new GridLayout (3, 1));
		panel.add (histos[0]);
		panel.add (histos[1]);
		panel.add (histos[2]);

		return panel;
	}
	
	private Histogram createHistogram ()
	{
		Histogram		histo;
		
		histo	 = new Histogram ();
		histo.setGrid (true);
		histo.setColor (true);
		histo.setButtons (false);
		
		return histo;
	}

	private JPanel createSubpanel (String name, Component compo)
	{
		JPanel			panel;
		
		panel	= new JPanel ();
		panel.setLayout (new GridLayout (1, 1));
		renameSubpanel (panel, name);
		panel.add (compo);
		
		return panel;
	}

	private void renameSubpanel (JPanel panel, String name)
	{
		panel.setBorder(new BorderUIResource.TitledBorderUIResource(new LineBorder(new Color(153, 153, 153), 1, false), name, 4, 2, new Font("Application", 1, 12), new Color(102, 102, 153)));
	}

	public void update (BufferedImage rgb, Channels chs)
	{
		SpaceHistogram		histo;
		BufferedImage		conv;
		boolean				dchans;
		
		this.rgb		= rgb;
		this.chs		= chs;
		
		dchans = (curspace == HSV);
		switch (curspace)
		{
		case RGB:
			conv		= rgb;
			break;
		case YUV:
			conv		= Segmentation.rgbToYuv (rgb);
			break;
		case HSV:
		default:
			conv		= Segmentation.rgbToHsv (rgb);
		}

		if (cpspaceplot != null)
		{
			cpspaceplot.setCylindrical (curspace == HSV);				// before the points: it is where they go
			cpspaceplot.setLabels (LABELS[curspace][0], LABELS[curspace][1], LABELS[curspace][2]);
			cpspaceplot.setImage (conv, rgb);
			if (dchans)
				cpspaceplot.setChannels (chs);		
			else
				cpspaceplot.clearChannels ();						// the prisms of the channels are in HSV
		}
		
		switch (curspace)
		{
		case YUV:
		case RGB:
			SpaceOrtoProjection			proj;
			
			proj		= new SpaceOrtoProjection (chs, Color.GRAY.brighter ());
			cpproj12.updateImage (proj.project (conv, rgb, SpaceOrtoProjection.PROJ12, dchans));
			cpproj02.updateImage (proj.project (conv, rgb, SpaceOrtoProjection.PROJ02, dchans));
			cpproj01.updateImage (proj.project (conv, rgb, SpaceOrtoProjection.PROJ01, dchans));
			break;
			
		case HSV:
		default:
			SpaceOrtoProjection			proj1;
			SpaceCylindricalProjection	proj2;
		
			proj1	= new SpaceOrtoProjection (chs, Color.GRAY.brighter ());
			cpproj12.updateImage (proj1.project (conv, rgb, SpaceOrtoProjection.PROJ12, dchans));

			proj2	= new SpaceCylindricalProjection (chs, Color.GRAY.brighter ());
			cpproj02.updateImage (proj2.project (conv, rgb, SpaceCylindricalProjection.PROJ02, dchans));
			cpproj01.updateImage (proj2.project (conv, rgb, SpaceCylindricalProjection.PROJ01, dchans));
		}
		
		histo	= new SpaceHistogram (chs, Color.GRAY.brighter ());
		histo.histogram (cphisto0, conv, SpaceHistogram.COMP0);
		histo.histogram (cphisto1, conv, SpaceHistogram.COMP1);
		histo.histogram (cphisto2, conv, SpaceHistogram.COMP2);		
	}
}
