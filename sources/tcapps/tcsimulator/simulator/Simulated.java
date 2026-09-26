/*
 * (c) 2026 Humberto Martinez Barbera
 */

package tcapps.tcsimulator.simulator;

/**
 * A module that wants the simulator it runs in: a referee that looks at where
 * the ball really is, say. When an architecture is run inside the simulator
 * ({@link tc.ExecArch}), every module that says so is given the simulator once
 * it is started; run anywhere else, it is given nothing and does without.
 */
public interface Simulated
{
	/** The simulator this module runs in. */
	public void simulator (Simulator sim);
}
