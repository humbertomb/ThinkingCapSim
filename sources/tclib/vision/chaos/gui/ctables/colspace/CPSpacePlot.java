/**
 * A simple convenience class to produce 3D plots. This is a 
 * heavyweight object. Based on Joy Kyriakopulos' LegoPlot
 * 
 * @author Hu8mberto Martinez Barbera
 */

package tclib.vision.chaos.gui.ctables.colspace;

import java.awt.image.*;
import javax.media.j3d.*;
import javax.vecmath.*;

import org.freehep.j3d.plot.*;

import tclib.vision.chaos.channels.*;

public class CPSpacePlot extends Plot3D
{
	static protected final Color3f		AXISCOL	= new Color3f (0, 0, 0);
	
	private SpaceCylindricalBuilder builder;
	private Node plot;
	private AxisBuilder xAxis;
	private AxisBuilder yAxis;
	private ZAxisBuilder zAxis;
	
	public CPSpacePlot ()
	{
		super ();
	}
	
	public void setImage (BufferedImage space, BufferedImage colors)
	{
		builder.updatePoints (space, colors);
	}

	public void setChannels (Channels channels)
	{
		builder.updateClusters (channels);
	}

	public void setLabels (String xAxisLabel, String yAxisLabel, String zAxisLabel)
	{
		xAxis.setLabel(xAxisLabel);
		yAxis.setLabel(yAxisLabel);
		zAxis.setLabel(zAxisLabel);
		
		xAxis.apply();
		yAxis.apply();
		zAxis.apply();
	}
	
	protected Node createPlot()
	{
		Shape3D				box;
		Group				group;
		Appearance			app;
		ColoringAttributes	cattrs;
			
		cattrs	= new ColoringAttributes (AXISCOL, ColoringAttributes.SHADE_GOURAUD);
		app		= new Appearance();
		app.setColoringAttributes (cattrs);
		
		builder	= new SpaceCylindricalBuilder ();
		box		= builder.buildOutsideBox();
		box.setAppearance (app);
		plot		= builder.buildPlot();
		
		xAxis	= new XAxisBuilder ();
		yAxis	= new YAxisBuilder ();
		zAxis	= new ZAxisBuilder ();
		xAxis.createLabelsNTicks(0, 255);
		yAxis.createLabelsNTicks(0, 255);
		zAxis.createLabelsNTicks(0, 255);
		xAxis.setAppearance (app);
		yAxis.setAppearance (app);
		zAxis.setAppearance (app);
		xAxis.apply();
		yAxis.apply();
		zAxis.apply();
		
		group	= new Group();
		group.addChild(box);
		group.addChild(plot);
		group.addChild(xAxis.getNode());
		group.addChild(yAxis.getNode());
		group.addChild(zAxis.getNode());
		
		return group;
	}
	
	protected void setupLights(BranchGroup root)
	{
		Background			bkg;
		AmbientLight			amb;		
		
		bkg		= new Background (new Color3f (0.9f, 0.9f, 0.9f));
		bkg.setApplicationBounds (getDefaultBounds ());
		root.addChild (bkg);

//		amb		= new AmbientLight (new Color3f (1.0f, 1.0f, 1.0f));
		amb		= new AmbientLight ();
		amb.setInfluencingBounds (getDefaultBounds ());
		amb.setEnable (true);
		root.addChild (amb);
	}

	// Add behavior to switch to "wire-frame" display when mouse is pressed
	//			by overriding defineMouseBehavior
	protected BranchGroup defineMouseBehaviour(Node scene)
	{
		BranchGroup bg = super.defineMouseBehaviour(scene);
		Bounds bounds = getDefaultBounds();
		
		// change this to be switch node below i.e. not scene
		if (plot instanceof Switch) {
			Switch sw = (Switch)plot;
			MouseDownUpBehavior mouseDnUp = new MouseDownUpBehavior(bounds, sw);
			bg.addChild(mouseDnUp);
		}
		
		return bg;
	}
}

