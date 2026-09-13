/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import tc.shared.world.World;

/**
 * Manages the synchronised Java 3D view ({@link WorldView3DWindow}) of a
 * {@link WorldCanvas}: lazy creation beside the owner window, the toolbar
 * toggle and menu item that show/hide it, and the propagation of world and
 * selection changes. Shared by the editor and the simulator.
 */
public class View3DController
{
	protected java.awt.Component	owner;					// window or component whose window the 3D view sits beside
	protected WorldCanvas			canvas;
	protected WorldView3DWindow		view3d;
	protected JToggleButton			button;
	protected JCheckBoxMenuItem		item;

	public View3DController (java.awt.Component owner, WorldCanvas canvas)
	{
		this.owner	= owner;
		this.canvas	= canvas;
	}

	/** Toolbar toggle (created once). */
	public JToggleButton button ()
	{
		if (button == null)
		{
			button = ToolButtons.flatToggle (new ToolIcon (ToolIcon.VIEW3D), "3D view  [Ctrl+3]");
			button.addActionListener (new java.awt.event.ActionListener ()
			{
				public void actionPerformed (ActionEvent e)		{ show (button.isSelected ()); }
			});
		}
		return button;
	}

	/** View menu item (created once). */
	public JCheckBoxMenuItem menuItem (int shortcutMask)
	{
		if (item == null)
		{
			item = new JCheckBoxMenuItem ("3D View", false);
			item.setAccelerator (KeyStroke.getKeyStroke (KeyEvent.VK_3, shortcutMask));
			item.addActionListener (new java.awt.event.ActionListener ()
			{
				public void actionPerformed (ActionEvent e)		{ show (item.isSelected ()); }
			});
		}
		return item;
	}

	public boolean isVisible ()		{ return (view3d != null) && view3d.isVisible (); }

	/** Shows or hides the 3D window, creating it on first use. */
	public void show (boolean show)
	{
		World	world = canvas.getWorld ();
		if (show && (view3d == null))
		{
			try
			{
				view3d = new WorldView3DWindow (world, new Runnable ()
				{
					public void run ()		{ setToggles (false); }
				});
				view3d.setSize (900, 700);
				// place it beside the owner when there is room
				java.awt.Window	win = (owner instanceof java.awt.Window) ? (java.awt.Window) owner : SwingUtilities.getWindowAncestor (owner);
				Rectangle	r = (win != null) ? win.getBounds () : new Rectangle (0, 0, 0, 0);
				Rectangle	scr = ((win != null) ? win : owner).getGraphicsConfiguration ().getBounds ();
				if (r.x + r.width + 900 <= scr.x + scr.width)	view3d.setLocation (r.x + r.width, r.y);
				else											view3d.setLocation (r.x + 60, r.y + 60);
			} catch (Throwable e)
			{
				e.printStackTrace ();
				view3d = null;
				setToggles (false);
				JOptionPane.showMessageDialog (owner, "The 3D view cannot be created. Check that Java 3D and JOGL (jarlibs/jsdn_java3d.jar, jarlibs/jogl/*.jar) are in the classpath.\n\n" + e,
						"3D View", JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		if (view3d == null)				return;
		if (show)
		{
			view3d.setWorld (world);
			view3d.setSelection (canvas.getSelection ());
		}
		view3d.setVisible (show);
		setToggles (show);
	}

	private void setToggles (boolean on)
	{
		if (button != null)		button.setSelected (on);
		if (item != null)		item.setSelected (on);
	}

	/** The canvas shows another World instance, or the world structure changed (rebuilds the scene). */
	public void worldChanged ()
	{
		if (isVisible ())		view3d.setWorld (canvas.getWorld ());
	}

	/** Positions changed during a drag (cheap update). */
	public void worldPreview ()
	{
		if (isVisible ())		view3d.worldChanged ();
	}

	public void selectionChanged (WorldItem item)
	{
		if (view3d != null)		view3d.setSelection (item);
	}

	/* --- simulated robots (delegated to the 3D window; no-ops while it does not exist) --- */

	public int addRobot (tc.vrobot.RobotDesc rdesc, tcapps.tcsimulator.simulator.SimulatorDesc sdesc, double x, double y, double a, String name)
	{
		return (view3d != null) ? view3d.addRobot (rdesc, sdesc, x, y, a, name) : -1;
	}

	public void updateRobot (int index, tc.vrobot.RobotData data)
	{
		if (view3d != null)		view3d.updateRobot (index, data);
	}

	public void clearRobots ()
	{
		if (view3d != null)		view3d.clearRobots ();
	}

	public void dispose ()
	{
		if (view3d != null)		view3d.dispose ();
		view3d = null;
	}
}
