/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tceditor;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JToggleButton;

/**
 * Toolbar helpers shared by the applications that show a World (editor,
 * simulator, ...): flat buttons matching the tool toggles, and the standard
 * zoom actions of a {@link WorldCanvas}.
 */
public class ToolButtons
{
	private ToolButtons ()	{ }

	/** Action button with the same flat look as the tool toggles (no push-button frame, macOS included). */
	public static JButton flatButton (Action action)
	{
		JButton	b = new JButton (action);
		b.setHideActionText (true);
		flat (b);
		return b;
	}

	/** Toggle button with a flat look and a tooltip. */
	public static JToggleButton flatToggle (Icon icon, String tip)
	{
		JToggleButton	b = new JToggleButton (icon);
		b.setToolTipText (tip);
		flat (b);
		return b;
	}

	private static void flat (javax.swing.AbstractButton b)
	{
		b.setFocusable (false);
		b.setBorderPainted (false);
		b.setContentAreaFilled (false);
		b.setOpaque (false);
		b.putClientProperty ("JButton.buttonType", "toolbar");
	}

	/** Creates an action with an icon and a tooltip. */
	public static Action action (String name, int icon, String tip, final Runnable body)
	{
		Action	a = new AbstractAction (name, new ToolIcon (icon))
		{
			private static final long	serialVersionUID = 1L;
			public void actionPerformed (ActionEvent e)		{ body.run (); }
		};
		a.putValue (Action.SHORT_DESCRIPTION, tip);
		return a;
	}

	public static Action zoomFit (final WorldCanvas canvas)
	{
		return action ("Zoom to fit", ToolIcon.ZOOM_FIT, "Zoom to fit  [Ctrl+0]", new Runnable () { public void run () { canvas.zoomToFit (); } });
	}

	public static Action zoomIn (final WorldCanvas canvas)
	{
		return action ("Zoom in", ToolIcon.ZOOM_IN, "Zoom in", new Runnable () { public void run () { canvas.zoom (1.25); } });
	}

	public static Action zoomOut (final WorldCanvas canvas)
	{
		return action ("Zoom out", ToolIcon.ZOOM_OUT, "Zoom out", new Runnable () { public void run () { canvas.zoom (0.8); } });
	}
}
