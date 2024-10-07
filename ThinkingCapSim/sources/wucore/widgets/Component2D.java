/** 
 * Component2D.java
 *
 * Description:		Generic component to display 2D information
 * @author			Humberto Martinez Barbera
 * @version			3.0
 */

package wucore.widgets;

import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

import wucore.utils.geom.*;
import wucore.utils.math.*;

public class Component2D extends JComponent
{
	// Visualisation and scaling constants
	public static final double			MAXSCALE	= 10.0;			// Maximum scale factor 
	public static final double			DEFSCALE	= 1.0;			// Default scale factor 
	public static final double			MINSCALE	= 0.005;		// Minimum scale factor 
	public static final double			KCDRAG		= 0.01;					
	public static final int				MAXHUDS		= 20;

	// Basic shapes constants
	public static final int				REF_SCAL	= 2;			// scaling factor for the references
	public static final int				REF_OFF		= 25;			// offset for the references
	public static final int				REF_SIZ		= 10;			// size of the map metrics
	public static final int				REF_GRID	= 25;			// minimum grid cell size
	public static final int				REF_GCOUNT	= 6;			// maximum number of grid scales
	public static final double			REF_ISCAL	= 5.0;			// scale increment for grid spacing
	public static final int				PT_RAD		= 3;			// radius of a point

	// System references
	public int[][]						ref_icon	= { {  3, 16 }, {  1, 10 }, {  3, 10 }, {  3,  3 }, { 10,  3 }, { 10,  1 }, { 16,  3 }, { 10,  5 }, { 10,  3 }, {  3,  3 }, {  3, 10 }, {  5, 10 }, {  3, 16 } };
	protected int[]						ref_icon_x	= new int[ref_icon.length];
	protected int[]						ref_icon_y	= new int[ref_icon.length];
	public String[]						ref_label	= new String[2];	
	public Component2DGrid				ref_grid;

	// Background image management
	protected Image						img_bckg;
	protected double					img_min_x;
	protected double					img_min_y;
	protected double					img_max_x;
	protected double					img_max_y;
	protected int						img_ini_sx;
	protected int						img_ini_sy;
	protected int						img_end_sx;
	protected int						img_end_sy;
	protected int						img_ini_dx;
	protected int						img_ini_dy;
	protected int						img_end_dx;
	protected int						img_end_dy;

	// Heads-Up-Display objects (HUDs)
	public int[]						hud_x = new int[MAXHUDS];	
	public int[]						hud_y = new int[MAXHUDS];	
	public String[]						hud_label = new String[MAXHUDS];	
	public int							hud_n = 0;
	

	// Visualization stuff
	protected Model2D					model;
	protected Image 					offscreen;
	protected Color						hudbkgcol;					// HUD background color
	protected boolean					modified	= true;			// If modified, compute a new transformation matrix
	protected boolean					drawrefs	= false;		// Draw axis reference labels
	protected boolean					drawscale	= false;		// Draw the scale reference
	protected boolean					drawbkghud	= false;		// Draw a solid background to the HUD
	protected boolean					drawgrid	= false;		// Draw a grid scale reference
	protected boolean					doclipping	= true;			// Perform clipping when drawing

	// Precomputed graphic primitives
	protected Font						tfont;						// TIMES font
	protected Rectangle2D				trec;						// TIMES font rectangle
	protected Font						cfont;						// COURIER font
	protected Rectangle2D				crec;						// COURIER font rectangle
	protected Font						fhud;						// HUD display font
	protected Rectangle2D				rhud;						// HUD display font rectangle
	protected Font						afont;						// Axis font
	protected Font						sfont;						// Scale font
	protected Font						gfont;						// Grid font

	// Drawing stuff
	protected Stroke					stk_plain;
	protected Stroke					stk_dashed;
	protected Stroke					stk_thick;
	protected Hashtable					img_hash;					// Hashtable for images and icons
	protected ImageCache				img_cache;					// Cache for images and icons
	protected Component2DListener		img_listener;				// Listener for ToolTip labels

	// Scaling and projection parameters
	protected double[] 					mod_bor;					// Model boundaries (world coordinates) - Bounding Box
	protected int[] 					scr_bor = new int[4];		// Model boundaries (screen coordinates)
	protected double[] 					vis_bor = new double[4];	// Model visible area (world coordinates)
	protected Point2					mod_pt = new Point2 ();
	protected int[]						xvert = new int[10000];		// Polyline X coordinates array
	protected int[]						yvert = new int[10000];		// Polyline Y coordinates array
	protected int						prevx;
	protected int						prevy;
	protected int						prevw = -1;					// Last canvas width
	protected int						prevh = -1;					// Last canvas height
	protected double 					scale;						// Main zooming factor
	protected double					scal_fac;					// X & Y axes scaling factors
	protected Matrix2D 					mat = new Matrix2D ();
	protected Matrix2D 					amat = new Matrix2D ();
	protected Matrix2D					tmat = new Matrix2D ();
	protected Point2					pt1 = new Point2 ();		// On-screen scale reference (P1)
	protected Point2					pt2 = new Point2 ();		// On-screen scale reference (P2)

	// Constructors
	public Component2D ()
	{    	
		model			= new Model2D ();
		mod_bor			= model.getBoundingBox ();
		scale			= DEFSCALE;
		scal_fac		= 1.0;

		// Initialise drawing components
		stk_plain		= new BasicStroke ();
		stk_thick		= new BasicStroke (2);
		stk_dashed		= new BasicStroke (1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {9}, 0);
		img_hash		= new Hashtable (500);
		img_cache		= new ImageCache ();
		tfont			= new Font ("Times", Font.PLAIN, 14);
		cfont			= new Font ("Courier", Font.PLAIN, 14);
		fhud			= new Font ("Courier", Font.PLAIN, 16);
		trec			= tfont.getMaxCharBounds (new FontRenderContext (tfont.getTransform (), true, false));
		crec			= cfont.getMaxCharBounds (new FontRenderContext (cfont.getTransform (), true, false));
		rhud			= fhud.getMaxCharBounds (new FontRenderContext (fhud.getTransform (), true, false));
		afont			= new Font ("Times", Font.PLAIN, 18);
		sfont			= new Font ("Times", Font.BOLD, 18);
		gfont			= new Font ("Times", Font.PLAIN, 10);
		hudbkgcol		= getBackground ();

		// Set default visualization options
		ref_label[0]	= "x";
		ref_label[1]	= "y";
	}

	/* Accessor methods */
	public final void			setListener (Component2DListener lter)	{ this.img_listener = lter; }
	public Component2DListener	getListener ()							{ return img_listener; }

	public final void 			setScale (double scale)					{ if (scale > 0.0) { this.scale = scale; modified = true; } }
	public final double			getScale ()								{ return scale; }
	public final void 			setTransform (Matrix2D amat)			{ this.amat.set (amat); modified = true; }
	public final Matrix2D		getTransform ()							{ return amat; }

	public final double[]		getVisibleBox ()						{ return vis_bor; }
	public final int[]			getScreenBox ()							{ return scr_bor; }

	public final void			setClipping (boolean doclipping)		{ this.doclipping = doclipping; }
	public final boolean		isClipping ()							{ return doclipping; }
	public final void			setDrawrefs (boolean drawrefs)			{ this.drawrefs = drawrefs; }
	public final boolean		isDrawrefs ()							{ return drawrefs; }
	public final void			setDrawscale (boolean drawscale)		{ this.drawscale = drawscale; }
	public final boolean		isDrawscale ()							{ return drawscale; }
	public final void			setDrawgrid (boolean drawgrid)			{ this.drawgrid = drawgrid; }
	public final boolean		isDrawgrid ()							{ return drawgrid; }
	public final void			setDrawbkgHUD (boolean drawbkghud)		{ this.drawbkghud = drawbkghud; }
	public final boolean		isDrawbkgHUD ()							{ return drawbkghud; }

	public final void			setModified (boolean modified)			{ this.modified = modified; }
	public final boolean		isModified ()							{ return modified; }

	public final void			setBackgroundHUD (Color color)			{ this.hudbkgcol = color; }
	public final void			setGridInfo (Component2DGrid grid)		{ this.ref_grid = grid; }

	// Instance methods
	public void setModel (Model2D model)
	{ 
		this.model	= model;
		mod_bor		= model.getBoundingBox (); 
		modified	= true; 
	}

	public final Model2D getModel ()
	{ 
		return model; 
	}

	public String getToolTipText (MouseEvent event)
	{
		String			label;

		label	= img_cache.inIcon (event.getX (), event.getY ());
		if ((label != null) && (img_listener != null))
			return img_listener.getObjectText (label);

		return super.getToolTipText (event);
	}

	public Image getImage ()
	{
		return img_bckg;
	}
	
	public void setImage (Image image)
	{
		img_bckg	= image;

		img_min_x	= 0.0;
		img_min_y	= 0.0;
		img_max_x	= 1.0;
		img_max_y	= 1.0;

		modified	= true;
	}

	public void adjustImage (double minx, double miny, double maxx, double maxy)
	{
		img_min_x	= minx;
		img_min_y	= miny;
		img_max_x	= maxx;
		img_max_y	= maxy;

		modified	= true;
	}

	public void paint (Graphics g) 
	{
		int 			borderx, bordery;
		int				wxmax, wymax;
		double			span;
		Point2			tmp;
		boolean			resized;

		// Extract component size
		wxmax	= getSize ().width;
		wymax	= getSize ().height;

		if ((wxmax == 0) || (wymax == 0))				return;

		// Update model state variables
		if (model.isUpdated ())
		{
			modified = true;
			model.setUpdated (false);
		}
		
		// Check if component's size has changed
		resized = (prevw != wxmax) || (prevh != wymax);
		if (resized)
		{
			offscreen = createImage (wxmax, wymax);
			prevw = wxmax;
			prevh = wymax;
		}
		
		// Compute current graphic tranformations
		if (modified || resized)
		{
			// Check for model boundaries with no dimension
			if (mod_bor[2] - mod_bor[0] == 0.0)
			{
				span = (mod_bor[3] - mod_bor[1]) * 0.5;
				mod_bor[0] -= span;
				mod_bor[2] += span;
			}
			else if (mod_bor[3] - mod_bor[1] == 0.0)			
			{
				span = (mod_bor[2] - mod_bor[0]) * 0.5;
				mod_bor[1] -= span;
				mod_bor[3] += span;
			}
	
			// Compute scaling factor (world to screen coordinates)
			pre_scaling ();

			// Initialise transformation matrix
			mat.unit ();
			mat.scale ((float) scal_fac, (float) -scal_fac);		
			mat.transform (mod_bor, scr_bor, 2);

			borderx = (int) (0.5 * (wxmax - (scr_bor[2] - scr_bor[0])));			
			bordery = (int) (0.5 * (wymax + (scr_bor[3] - scr_bor[1])));

			mat.translate (-scr_bor[0] + borderx, -scr_bor[3] + bordery);
			mat.mult (amat);

			// Compute visible area boundaries
			tmp			= screen2world (0, 0);		
			vis_bor[0]	= tmp.x;
			vis_bor[1]	= tmp.y;
			tmp			= screen2world (wxmax, wymax);					
			vis_bor[2]	= tmp.x;
			vis_bor[3]	= tmp.y;

			// Apply the transformation to the background image
			if (img_bckg != null)
			{
				double		scalx, scaly;
				int			imagex, imagey;
				int			spixx, spixy;

				// Compute image pixels to world coordinates
				imagex		= img_bckg.getWidth (this);
				imagey		= img_bckg.getHeight (this);
				scalx		= (img_max_x - img_min_x) / (double) imagex;
				scaly		= (img_max_y - img_min_y) / (double) imagey;

				// Compute image to screen origin offset (pixels)
				img_ini_sx	= (int) ((vis_bor[0] - img_min_x) / scalx);
				img_ini_sy	= imagey - (int) ((vis_bor[1] - img_min_y) / scaly);

				// Compute image to screen limit offset (pixels)
				img_end_sx	= (int) ((vis_bor[2] - img_min_x) / scalx);
				img_end_sy	= imagey - (int) ((vis_bor[3] - img_min_y) / scaly);

				// Compute size of image to display
				spixx		= Math.max (img_end_sx - img_ini_sx, 0);
				spixy		= Math.max (img_end_sy - img_ini_sy, 0);

				img_ini_dx	= 0;
				img_ini_dy	= 0;
				img_end_dx	= wxmax;
				img_end_dy	= wymax;

//				System.out.println ("\nImage area "+img_min_x+","+img_min_y+" to " + img_max_x+", "+img_max_y + " m");
//				System.out.println ("Visible screen area "+vis_bor[0]+","+vis_bor[3]+" to " + vis_bor[2]+", "+vis_bor[1] + " m");
//				System.out.println ("Visible image area "+img_ini_sx+","+img_ini_sy+" to " + img_end_sx+", "+img_end_sy + " pix");

				// Check for the visiblity and limits of the image
				if (img_ini_sx < 0)
				{
					img_ini_dx	= -(int) ((double) img_ini_sx * ((double) wxmax / (double) spixx));
					img_ini_sx	= 0;
				}
				if (img_ini_sy < 0)
				{
					img_ini_dy	= -(int) ((double) img_ini_sy * ((double) wymax / (double) spixy));
					img_ini_sy	= 0;
				}
				if (img_ini_sx > imagex)
				{
					img_ini_dx	-= (int) ((double) (img_ini_sx - imagex) * ((double) wxmax / (double) spixx));
					img_ini_sx	= imagex;
				}
				if (img_ini_sy > imagey)
				{
					img_ini_dy	-= (int) ((double) (img_ini_sy - imagey) * ((double) wymax / (double) spixy));
					img_ini_sy	= imagey;
				}

				if (img_end_sx < 0)
				{
					img_end_dx	= -(int) ((double) img_end_sx * ((double) wxmax / (double) spixx));
					img_end_sx	= 0;
				}
				if (img_end_sy < 0)
				{
					img_end_dy	= -(int) ((double) img_end_sy * ((double) wymax / (double) spixy));
					img_end_sy	= 0;
				}
				if (img_end_sx > imagex)
				{
					img_end_dx	-= (int) ((double) (img_end_sx - imagex) * ((double) wxmax / (double) spixx));
					img_end_sx	= imagex;
				}
				if (img_end_sy > imagey)
				{
					img_end_dy	-= (int) ((double) (img_end_sy - imagey) * ((double) wymax / (double) spixy));
					img_end_sy	= imagey;
				}

//				System.out.println ("Corrected source "+img_ini_sx+","+img_ini_sy+" to " + img_end_sx+", "+img_end_sy + " pix");
//				System.out.println ("Corrected destination "+img_ini_dx+","+img_ini_dy+" to " + img_end_dx+", "+img_end_dy+" pix\n");
			}
			modified	= false;
			
			paintg (offscreen.getGraphics ());							
		}

		g.drawImage (offscreen, 0, 0, this);
	}

	protected void paintg (Graphics g) 
	{
		int				i;
		int				x1, y1;
		int				wxmax, wymax;

		// Clear the background with the default color
		wxmax	= getSize ().width;
		wymax	= getSize ().height;
		g.setColor (getBackground ());
		g.fillRect (0, 0, wxmax, wymax);

		// Draw reference grid
		if (ref_grid != null)
		{
			int			xx, yy;
			int			xcell, ycell;
			int			count;
			double		xi, yi, xf, yf;
			double		xmin, xmax, ymin, ymax;
			double		gscale;
			Point2		screen;
			Color		color1, color2;

			gscale	= ref_grid.scale / REF_ISCAL;
			count	= 0;
			do
			{
				gscale	= gscale * REF_ISCAL;
				count ++;
				
				// Compute grid boundaries
				xi		= Math.ceil (vis_bor[0] / gscale) * gscale;
				yi		= Math.ceil (vis_bor[3] / gscale) * gscale;
				xf		= Math.floor (vis_bor[2] / gscale) * gscale;
				yf		= Math.floor (vis_bor[1] / gscale) * gscale;
	
				// Compute grid cell size
				screen	= world2screen (xi, yf);
				xmin	= screen.x;
				ymin	= screen.y;
				screen	= world2screen (xf, yi);
				xmax	= screen.x;
				ymax	= screen.y;		
				xcell	= (int) Math.floor ((xmax - xmin) / (xf - xi) * gscale);
				ycell	= (int) Math.floor ((ymax - ymin) / (yf - yi) * gscale);
			} while ((xcell < REF_GRID) && (ycell < REF_GRID) && (count < REF_GCOUNT));
			
			if (count < REF_GCOUNT)
			{
				color1	= ref_grid.color;
				color2	= color1.darker ();
				((Graphics2D) g).setStroke (stk_plain);
				g.setFont (gfont);
	
				// Draw vertical lines
				if (xcell >= REF_GRID)
					for (double x = xi; x <= xf; x += gscale)
						if (!drawrefs || (Math.abs (x) > gscale * 0.5))
						{
							screen	= world2screen (x, 0.0);
							xx		= (int) screen.x;
							
							g.setColor (color1);	
							g.drawLine (xx, 0, xx, wymax);
							g.setColor (color2);	
							if (gscale >= 1.0)
								g.drawString ((int) x + ref_grid.units, xx+3, 10);
							else
								g.drawString (((int) (x / gscale) * gscale) + ref_grid.units, xx+3, 10);
						}
		
				// Draw horizontal lines
				if (ycell >= REF_GRID)
					for (double y = yi; y <= yf; y += gscale)
						if (!drawrefs || (Math.abs (y) > gscale * 0.5))
						{
							screen	= world2screen (0.0, y);
							yy		= (int) screen.y;
							
							g.setColor (color1);	
							g.drawLine (0, yy, wxmax, yy);
							g.setColor (color2);	
							if (gscale >= 1.0)
								g.drawString ((int) y + ref_grid.units, 10, yy+10);
							else
								g.drawString (((int) (y / gscale) * gscale) + ref_grid.units, 10, yy+10);
						}
			}
		}

		// Draw background image
		if (img_bckg != null)
			g.drawImage (img_bckg, img_end_dx, img_ini_dy, img_ini_dx, img_end_dy, img_end_sx, img_ini_sy, img_ini_sx, img_end_sy, this);

		// Draw the corresponding objects
		try
		{
			draw_objects (g, model.verts, model.attr, model.nattr, wxmax, wymax);
		} catch (Exception e) { e.printStackTrace(); }
		
		// Draw axis references
		if (drawrefs && (ref_icon.length > 0))
		{
			// Recompute reference axis polygon
			x1	= REF_OFF;
			y1	= wymax - REF_OFF;
			for (i = 0; i < ref_icon.length; i++)
			{
				ref_icon_x[i]	= x1 + ref_icon[i][0] * REF_SCAL;
				ref_icon_y[i]	= y1 - ref_icon[i][1] * REF_SCAL;
			}

			// Draw reference axis
			g.setColor (Color.RED);
			g.drawPolygon (ref_icon_x, ref_icon_y, ref_icon.length);

			// Draw axis labels
			// g.setColor (Color.red);
			g.setFont (afont);
			if (ref_label[0] != null)		g.drawString (ref_label[0], x1 + 18 * REF_SCAL, y1 - REF_SCAL);
			if (ref_label[1] != null)		g.drawString (ref_label[1], x1 + REF_SCAL, y1 - 19 * REF_SCAL);
		}

		// Draw map scale
		if (drawscale)
		{
			boolean		filled	= true;
			double		dy, k;
			double		len;
			int			num, rng, ilen;

			rng	= 4 * wymax / 6;
			x1	= wxmax - (REF_OFF * 2);
			y1	= wymax / 6;

			pt1.set (screen2world (0, y1));		
			pt2.set (screen2world (0, y1 + rng));		

			// Compute scale range
			k	= 0.0001;
			num	= Integer.MAX_VALUE;
			len	= 1.0;
			while (num > 20)
			{
				k	= k * 10.0;
				dy	= (pt1.y - pt2.y) / k;		
				len	= (double) rng / dy;
				if (len != 0.0)
					num	= (int) Math.floor ((double) rng / len);
				else
					num	= 0;
			}

			// Draw reference boxes
			ilen	= (int) len;
			for (i = 0; i < num; i++)
			{
				if (!filled)
				{
					g.setColor (Color.WHITE);
					g.fillRect (x1, y1 + i * ilen, REF_SIZ, ilen);
					g.setColor (Color.BLACK);
					g.drawRect (x1, y1 + i * ilen, REF_SIZ, ilen);
				}
				else
				{
					g.setColor (Color.BLACK);
					g.fillRect (x1, y1 + i * ilen, REF_SIZ+1, ilen);
				}
				filled	= !filled;
			}

			if (num > 0)
			{
				g.setColor (Color.BLACK);
				g.setFont (sfont);
				if (k < 1.0)
					g.drawString ((int) (k * 1000.0) + "mm", x1 - 20, y1 + rng + 15);
				else if (k <= 1000.0)
					g.drawString ((int) k + "m", x1 - 20, y1 + rng + 15);
				else
					g.drawString ((int) (k / 1000.0) + "Km", x1 - 20, y1 + rng + 15);
			}
		}

		// Draw HUD information
		g.setFont (fhud);
		for (i = 0; i < hud_n; i++)
			if (hud_label[i] != null)
			{
				if (drawbkghud)
				{
					g.setColor (hudbkgcol);
					g.fillRect (hud_x[i] + (int) rhud.getX (), hud_y[i] + (int) rhud.getY (), (int) (hud_label[i].length () * rhud.getWidth ()), (int) rhud.getHeight ());
				}

				g.setColor (Color.GREEN.darker ());
				g.drawString (hud_label[i], hud_x[i], hud_y[i]);
			}
	}	

	protected void draw_objects (Graphics g, Model2DCoord[] verts, Model2DAttr[] attr, int nattr, int wxmax, int wymax) 
	{
		int				i, j;
		int				x1 = 0, y1 = 0;
		int				x2 = 0, y2 = 0;
		int				mx, my;
		int				amax, adel;
		int				lmode;
		Color			lcolor;
		Model2DCoord	coord1, coord2;

		// Draw basic objects
		amax		= Math.min (nattr, attr.length);
		adel		= 0;
		lmode 		= -1;
		lcolor		= null;
		for (i = 0; i < amax; i++) 
		{	
			if (attr[i].attype != Model2DAttr.ATTR_POLY)
			{
				// Obtain coordinates
				coord1 = verts[attr[i].vorig];
				coord1.transform (mat);
				x1	= coord1.projx;
				y1	= coord1.projy;

				// Perform clipping
				if (doclipping && !((x1 >= 0) && (x1 <= wxmax) && (y1 >= 0) && (y1 <= wymax))) 
				{
					adel ++;
					continue;
				}
			}
			
			if (attr[i].color != lcolor)
			{
				lcolor = attr[i].color;
				g.setColor (attr[i].color);
			}
			
			if (attr[i].mode != lmode)
			{
				lmode = attr[i].mode;
				switch (attr[i].mode)
				{
				case Model2D.DASHED:	((Graphics2D) g).setStroke (stk_dashed); break;
				
				case Model2D.THICK:
				case Model2D.SELECT:	((Graphics2D) g).setStroke (stk_thick); break;
				
				case Model2D.PLAIN:
				default:				((Graphics2D) g).setStroke (stk_plain);
				}
			}
			
			if (attr[i].attype == Model2DAttr.ATTR_LINE)
			{
				// Obtain coordinates
				coord2 = verts[attr[i].vdest];
				coord2.transform (mat);
				x2	= coord2.projx;
				y2	= coord2.projy;

				// Perform clipping
				if (doclipping && !((x2 >= 0) && (x2 <= wxmax) && (y2 >= 0) && (y2 <= wymax)))
				{
					adel ++;
					continue;
				}

				switch (attr[i].type)
				{
				case Model2D.BOX:
					mx	= Math.min (x1, x2);
					my	= Math.min (y1, y2);
					switch (attr[i].mode)
					{
					case Model2D.FILLED:
						g.fillRect (mx, my, Math.abs (x2 - x1), Math.abs (y2 - y1));
						break;
					default:
						g.drawRect (mx, my, Math.abs (x2 - x1), Math.abs (y2 - y1));
					}
					break;

				case Model2D.ARC:
					mx	= Math.min (x1, x2);
					my	= Math.min (y1, y2);
					switch (attr[i].mode)
					{
					case Model2D.FILLED:
						g.fillArc (mx, my, Math.abs (x2 - x1), Math.abs (y2 - y1), attr[i].vset[0], attr[i].vset[1]);
						break;
					default:
						g.drawArc (mx, my, Math.abs (x2 - x1), Math.abs (y2 - y1), attr[i].vset[0], attr[i].vset[1]);
					}
					break;										

				case Model2D.CIRCLE:
					mx	= Math.min (x1, x2);
					my	= Math.min (y1, y2);
					switch (attr[i].mode)
					{
					case Model2D.FILLED:
						g.fillOval (mx, my, Math.abs (x2 - x1), Math.abs (y2 - y1));
						break;
					default:
						g.drawOval (mx, my, Math.abs (x2 - x1), Math.abs (y2 - y1));
					}
					break;

				case Model2D.AXIS:
					g.drawLine (0, y1, wxmax, y1);
					g.drawLine (x1, 0, x1, wymax);
					if (attr[i].label != null)
					{
						g.drawString (attr[i].label, x1, y1); 
						g.drawString (attr[i].label, x1, y1);
					}
					break;

				case Model2D.LINE:
					g.drawLine (x1, y1, x2, y2);
					if (attr[i].mode == Model2D.SELECT)
					{
						((Graphics2D) g).setStroke (stk_plain);
						g.drawLine (x1+4, y1+4, x2+4, y2+4);
						lmode = Model2D.PLAIN;
					}
				}
			}		
			else if (attr[i].attype == Model2DAttr.ATTR_POINT)
			{
				// Draw basic shapes
				switch (attr[i].type)
				{					
				case Model2D.POINT:
					switch (attr[i].mode)
					{
					case Model2D.FILLED:
						g.fillOval (x1 - PT_RAD, y1 - PT_RAD, PT_RAD * 2, PT_RAD * 2);
						break;
					default:
						g.drawOval (x1 - PT_RAD, y1 - PT_RAD, PT_RAD * 2, PT_RAD * 2);
					}
					break;

				case Model2D.DOT:
					switch (attr[i].mode)
					{
					case Model2D.THICK:
						g.drawLine (x1, y1, x1+1, y1+1);
						break;
					default:
						g.drawLine (x1, y1, x1, y1);
					}
					break;

				case Model2D.ICON:
					Image			image;
					URL				url;
					int				w, h;
					boolean			newimage = false;

					if (attr[i].src == null)			break;
					
					if (attr[i].src instanceof Image)
					{
						image	= (Image) attr[i].src;
					}
					else if (attr[i].src instanceof String)
					{
						image	= (Image) img_hash.get (attr[i].src);
						if (image == null)
						{
							image		= getToolkit ().createImage ((String) attr[i].src);
							img_hash.put (attr[i].src, image);
							newimage	= true;
						}					
					}
					else /* attr[i].src instanceof URL */
					{
						url		= (URL) attr[i].src;
						image	= (Image) img_hash.get (url.toString ());
						if (image == null)
						{
							image		= getToolkit ().createImage (url);
							img_hash.put (url.toString (), image);
							newimage	= true;
						}					
					}

					w	= image.getWidth (this);
					h	= image.getHeight (this);
					g.drawImage (image, x1-w/2 , y1-h/2, this);

					if (attr[i].mode == Model2D.SELECT)
						g.drawOval (x1-w/2-4, y1-h/2-4, w+8, h+8);
					
					if (attr[i].label != null)
					{
						g.setFont (tfont);
						g.drawString (attr[i].label, x1+w/2, y1+h); 

						if (newimage)
							img_cache.add (attr[i].label, image.getWidth (this), image.getHeight (this));
						img_cache.update (attr[i].label, x1, y1);
					}
					break;

				case Model2D.TEXT:
					if (attr[i].label != null)
					{
						int			xx, yy;
						int			ww, hh;
						FontMetrics	fm;

						g.setFont (tfont);
						fm	= g.getFontMetrics();
						ww	= fm.stringWidth (attr[i].label);
						hh	= fm.getHeight ();
						switch (attr[i].mode)
						{
						case Model2D.J_CENTER:
							xx	= x1 - (ww / 2);
							yy	= y1 + hh / 2;
							break;
						case Model2D.J_RIGHT:
							xx	= x1 - ww;
							yy	= y1 + hh / 2;
							break;
						case Model2D.J_LEFT:
						default:
							xx	= x1;
							yy	= y1 + hh / 2;
						}
						g.drawString (attr[i].label, xx, yy); 
					}
					break;

				case Model2D.LABEL:
					if (attr[i].label != null)
					{
						g.setFont (cfont);
						g.fillRect (x1 + (int) crec.getX () - 3, y1 + (int) crec.getY () - 2, (int) (attr[i].label.length () * crec.getWidth ()), (int) crec.getHeight ());
						g.setColor (Color.black);
						g.drawRect (x1 + (int) crec.getX () - 3, y1 + (int) crec.getY () - 2, (int) (attr[i].label.length () * crec.getWidth ()), (int) crec.getHeight ());
						g.drawString (attr[i].label, x1, y1); 			
						lcolor = Color.BLACK;
					}
					break;
				}
			}
			else if (attr[i].attype == Model2DAttr.ATTR_POLY)
			{
				int				k;

				// Obtain coordinates
				k		= 0;
				if (attr[i].vset.length > xvert.length)
				{
					xvert	= new int[attr[i].vset.length];
					yvert	= new int[attr[i].vset.length];
				}
				for (j = 0; j < attr[i].vset.length; j++)
				{
					coord1 = verts[attr[i].vset[j]];
					coord1.transform (mat);

					x1	= Math.min (Math.max (coord1.projx, 0), wxmax);
					y1	= Math.min (Math.max (coord1.projy, 0), wymax);

					// Remove duplicated verteces
					if ((k > 0) && (x1 == xvert[k-1]) && (y1 == yvert[k-1]))
						continue;								

					xvert[k]	= x1;
					yvert[k]	= y1;
					k++;
				}

				if (k > 0)
					switch (attr[i].mode)
					{
					case Model2D.FILLED:
						g.fillPolygon (xvert, yvert, k);
						break;
					default:
						g.drawPolygon (xvert, yvert, k);
					}
			}
		}

		// System.out.println ("Elements discarded "+adel+"/"+amax);
	}	
	
	protected void pre_scaling ()
	{
		double			fx, fy;
		double			nscl;

		fx = getSize ().width / (mod_bor[2] - mod_bor[0]);
		fy = getSize ().height / (mod_bor[3] - mod_bor[1]);

		// Compute scaling factor (world to screen coordinates)
		if (scale >= 1.0)
			nscl	= Math.exp (scale) - Math.exp (1.0) + 1.0;
		else
			nscl	= scale;
		scal_fac = nscl * Math.min (fx, fy);
	}

	public Point2 screen2world (int x, int y) 
	{
		double		mwx, mwy;
		double		mx, my;
		int			swx, swy;

		// Compute model boundaries
		mwx	= mod_bor[2] - mod_bor[0];
		mwy	= mod_bor[3] - mod_bor[1];

		// Compute screen transformation
		mat.transform (mod_bor, scr_bor, 2);
		swx	= scr_bor[2] - scr_bor[0];
		swy	= scr_bor[3] - scr_bor[1];

		mx	= ((double)(x - scr_bor[0]) * mwx  / (double) swx) + mod_bor[0];
		my	= ((double)(y - scr_bor[1]) * mwy  / (double) swy) + mod_bor[1];

		mod_pt.set (mx, my);        		
		return mod_pt;
	}

	public Point2 world2screen (double x, double y) 
	{
		double		mwx, mwy;
		int			mx, my;
		int			swx, swy;

		// Compute model boundaries
		mwx	= mod_bor[2] - mod_bor[0];
		mwy	= mod_bor[3] - mod_bor[1];

		// Compute screen transformation
		mat.transform (mod_bor, scr_bor, 2);
		swx	= scr_bor[2] - scr_bor[0];
		swy	= scr_bor[3] - scr_bor[1];

		mx	= (int) ((x - mod_bor[0]) * swx  / (double) mwx) + scr_bor[0];
		my	= (int) ((y - mod_bor[1]) * swy  / (double) mwy) + scr_bor[1];		

		mod_pt.set (mx, my);        		
		return mod_pt;
	}

	public void autoScale () 
	{
		scale		= DEFSCALE;		
		modified	= true;
	}

	public void autoScale (double scl)
	{
		scale		= DEFSCALE;
		pre_scaling ();
		scale		= Math.log (scl * -scal_fac);
		modified	= true;
	}

	public void autoCenter () 
	{
		amat.unit ();

		modified	= true;
	}

	public void autoCenter (double cx, double cy)
	{
		float			dx, dy;
		double			medx, medy;

		medx	= (mod_bor[2] - mod_bor[0]) * 0.5 + mod_bor[0];
		medy	= (mod_bor[3] - mod_bor[1]) * 0.5 + mod_bor[1];

		dx 		= (float) ((medx - cx) * scal_fac);		
		dy 		= (float) ((medy - cy) * -scal_fac);		

		amat.unit ();
		amat.translate (dx, dy);

		modified	= true;
	}

	public void mouseAxis (int x, int y) 
	{
		Graphics g = getGraphics ();
		g.drawImage (offscreen, 0, 0, this);
		
		// Set drawing mode
		g.setXORMode (Color.WHITE);
		g.setColor (Color.BLACK);

		// Draw current axis
		g.drawLine (0, y, getSize ().width, y);
		g.drawLine (x, 0, x, getSize ().height);
	}

	public void mouseBox (int ox, int oy, int x, int y) 
	{
		Graphics g = getGraphics ();
		g.drawImage (offscreen, 0, 0, this);
		
		// Set drawing mode
		g.setXORMode (Color.WHITE);
		g.setColor (Color.BLACK);

		// Draw current axis
		g.drawLine (ox, oy, ox, y);
		g.drawLine (ox, y, x, y);
		g.drawLine (x, y, x, oy);
		g.drawLine (x, oy, ox, oy);
	}

	public void mouseArrow (int ox, int oy, int x, int y) 
	{
		Graphics g = getGraphics ();
		g.drawImage (offscreen, 0, 0, this);
		
		// Set drawing mode
		g.setXORMode (Color.WHITE);
		g.setColor (Color.BLACK);

		// Draw current axis
		g.drawLine (ox, oy, x, y);
	}

	public void mouseDown (int x, int y) 
	{
		prevx = x;
		prevy = y;
	}

	public void mousePan (int x, int y) 
	{
		float			dx, dy;

		if ((prevx == x) && (prevy == y))		return;
		
		pt1.set (screen2world (x, y));		
		pt2.set (screen2world (prevx, prevy));		
		dx	= (float) ((pt1.x - pt2.x) * scal_fac);		
		dy	= (float) ((pt1.y - pt2.y) * -scal_fac);		

		tmat.unit ();
		tmat.translate (dx, dy);
		amat.mult (tmat);

		modified	= true;
		repaint ();

		prevx = x;
		prevy = y; 
	}

	public void mouseZoom (int x, int y) 
	{
		double			dy;
		Point2			pt;

		if (prevy == y)							return;
		
		pt	= screen2world (getSize ().width / 2, getSize ().height / 2);		
		dy	= prevy - y;

		if (dy != 0.0)
		{
			scale	+= KCDRAG * dy;
			scale	= Math.min (Math.max (scale, MINSCALE), MAXSCALE);
			//prescal = scal_fac;

			pre_scaling ();		
			autoCenter (pt.x, pt.y);

			modified	= true;
			repaint ();			
		}

		prevx = x;
		prevy = y;
	}

	public Point2 mouseClick (int x, int y) 
	{
		return screen2world (x, y);
	}	
}