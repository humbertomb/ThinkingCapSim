/*
 * (c) 2026 Humberto Martinez Barbera
 */

package wucore.widgets;

/**
 * Something drawn in a {@link Component2D} that can be drawn wider than it
 * reaches on its own, so that zooming out past the fit shows more of the world
 * instead of showing the same thing smaller.
 *
 * A local perceptual space is the case this is for: what it holds is bounded by
 * how far the sensors of the robot read, and everything beyond that is cut away,
 * so drawing it smaller inside a bigger window gains nothing but empty margins.
 * Asked to widen, it takes its boundary further out and keeps what lies there.
 */
public interface Component2DWider
{
	/**
	 * Widens (a factor over one) or narrows (under one) what is drawn, and says
	 * what it actually did it by: one when it would not move, which is what a view
	 * already as narrow as what it holds, or as wide as it will go, answers.
	 */
	public double widen (double factor);
}
