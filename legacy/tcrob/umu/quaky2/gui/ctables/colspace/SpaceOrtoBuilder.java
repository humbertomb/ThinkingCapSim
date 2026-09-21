/**
 * A simple convenience class to produce 3D plots. This is a 
 * heavyweight object. Based on Joy Kyriakopulos' LegoPlot
 * 
 * @author Hu8mberto Martinez Barbera
 */

package tcrob.umu.quaky2.gui.ctables.colspace;

import java.awt.image.*;
import javax.media.j3d.*;
import javax.vecmath.*;
import org.freehep.j3d.plot.*;

import tclib.vision.chaos.channels.*;

import com.sun.j3d.utils.geometry.*;

public class SpaceOrtoBuilder extends AbstractPlotBuilder
{
	static protected int 			BITS		= 7;
	static protected int 			STEP		= 3;
	static protected float 		SIZE		= (1f / 255f) * 0.5f;
	static protected int			MINPTS	= 1;

	protected BranchGroup			plot;
	protected int[][][]			hist;
	protected int[][][]			color;
	
	static public Color3f rgbToColor (int rgb)
	{
		int			r, g, b;
		
		r	= Pixel.getR (rgb);
		g	= Pixel.getG (rgb);
		b	= Pixel.getB (rgb);

		return new Color3f ((float) r / 255f, (float) g / 255f, (float) b / 255f);
	}

	static public Color3f rgbToColor (int r, int g, int b)
	{
		return new Color3f ((float) r / 255f, (float) g / 255f, (float) b / 255f);
	}

	public Node buildPlot ()
	{		
		plot = new BranchGroup();
		plot.setCapability(BranchGroup.ALLOW_CHILDREN_READ);	
		plot.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		plot.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		
		return plot;
	}
	
	public void updatePoints (BufferedImage image, BufferedImage colors)
	{
		int			x, y;
		int			i, j, k;
		int			col;
		int			co1, co2, co3;
		int			sco1, sco2, sco3;
		int			shifts, size;
		Node			node;
		BranchGroup	branch;
				
		shifts	= 8 - BITS;
		size		= 1 << BITS;
		if ((hist == null) || (color == null))
		{
			hist		= new int [size][size][size];
			color	= new int [size][size][size];
		}

		// Initialise histogram computation
		for (i = 0; i < size; i ++)
			for (j = 0; j < size; j ++)
				for (k = 0; k < size; k ++)
					hist[i][j][k]	= 0;
				
		// Compute 3D color histogram
		for (x = 0; x < image.getWidth (); x += STEP)
			for (y = 0; y < image.getHeight (); y += STEP)
			{
				col	= image.getRGB (x,y);			
				co1	= Pixel.getComponent0 (col);
				co2	= Pixel.getComponent1 (col);
				co3	= Pixel.getComponent2 (col);
				sco1	= co1 >> shifts;
				sco2	= co2 >> shifts;
				sco3	= co3 >> shifts;
				
				hist[sco1][sco2][sco3] ++;
				color[sco1][sco2][sco3] = colors.getRGB (x, y);
			}

		// Draw 3D color histogram
		branch = new BranchGroup ();
		branch.setCapability(BranchGroup.ALLOW_DETACH);
		for (i = 0; i < size; i ++)
			for (j = 0; j < size; j ++)
				for (k = 0; k < size; k ++)
					if (hist[i][j][k] >= MINPTS)
					{
						co1	= i << shifts;
						co2	= j << shifts;
						co3	= k << shifts;
						
						node = drawPoint (co1, co2, co3, hist[i][j][k], rgbToColor (color[i][j][k]));
						branch.addChild (node);
					}		
		plot.removeAllChildren ();
		plot.addChild (branch);
	}
		
	public void updateClusters (Channels chs)
	{
		int			i;
		Channel		ch;
		BranchGroup	branch;
		
		branch = new BranchGroup ();
		branch.setCapability(BranchGroup.ALLOW_DETACH);
		for (i = 0; i < chs.getNumChannels(); i++)
		{			
			ch	= chs.at (i);
			if (ch.segmented)
			{
				if (ch.getCluster() instanceof ColorPrism)
					branch.addChild (drawCluster (ch));
			}
		}
		plot.addChild (branch);
	}
	
	protected Node drawPoint (int co1, int co2, int co3, int count, Color3f color)
	{
		float				side;
		Sphere				point;
		Appearance			app;
		TransformGroup		node;
		Transform3D			trans;
		ColoringAttributes	cattrs;
				
		cattrs	= new ColoringAttributes (color, ColoringAttributes.SHADE_FLAT);
		app		= new Appearance();
		app.setColoringAttributes (cattrs);
		
		side		= SIZE * (float) Math.log (Math.exp (1.0) + count * 3f);
		point	= new Sphere (side, app);	
		
		trans = new Transform3D();
		trans.setTranslation(new Vector3d ((double) co1 / 255-0.5f, (double) co2 / 255-0.5f, (double) co3 / 255));	
		
		node = new TransformGroup (trans);
		node.addChild (point);
		
		return node;
	}	
	protected Node drawCluster (Channel ch)
	{
		float					x, y, z;
		float					xsize, ysize, zsize;
		Color3f					color;
		ColorPrism				cprism;
		Box						box;
		Appearance				app;
		TransformGroup			node;
		Transform3D				trans;
		ColoringAttributes		cattrs;
		TransparencyAttributes	tattrs;
		
		cprism	= (ColorPrism) ch.getCluster ();
		x		= (float) cprism.getMin0() / 255f;
		y		= (float) cprism.getMin1() / 255f;
		z		= (float) cprism.getMin2() / 255f;
		xsize	= (float) (cprism.getMax0() - cprism.getMin0()) / 255f * 0.5f;
		ysize	= (float) (cprism.getMax1() - cprism.getMin1()) / 255f * 0.5f;
		zsize	= (float) (cprism.getMax2() - cprism.getMin2()) / 255f * 0.5f;
		
		color	= rgbToColor (ch.color.getRGB ());
		cattrs	= new ColoringAttributes (color, ColoringAttributes.SHADE_FLAT);
		tattrs	= new TransparencyAttributes (TransparencyAttributes.NICEST, 0.70f);
		app		= new Appearance();
		app.setColoringAttributes (cattrs);
		app.setTransparencyAttributes (tattrs);
		box		= new Box (xsize, ysize, zsize, app);	
		
		trans = new Transform3D();
		trans.setTranslation(new Vector3d (x+xsize-0.5f, y+ysize-0.5f, z+zsize));	
		
		node = new TransformGroup (trans);
		node.addChild (box);
		
		return node;
	}	
}

