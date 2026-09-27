/*
 * (c) 2026 Humberto Martinez
 */

package tcrob.umu.soccer.linda;

import java.io.*;

import tc.shared.linda.*;

/**
 * What the referee says (tuple REFEREE): the state of the game, as the
 * RoboCup game controller has it, and the last thing that happened -- a goal,
 * the ball out, a robot where it may not be -- with whom it concerns and what
 * the referee said of it, word for word. One is written every time the
 * referee decides something or the state changes, so whoever reads it has the
 * state at all times and the decisions as they are made.
 */
public class ItemReferee extends Item implements Serializable
{
	private static final long		serialVersionUID = 1L;

	/** The states of the game, as the game controller of the league has them. */
	public enum GameStates			{ INITIAL, READY, SET, PLAYING, PENALIZED, FINISHED }

	/** What happened: a change of state, or a decision of the referee. */
	public enum Events				{ STATE, KICKOFF, GOAL, KICKOFF_SHOT, BALL_OUT, ILLEGAL_DEFENDER, TIME_UP }

	public GameStates				state	= GameStates.INITIAL;
	public int						player	= -1;				// the robot the state or the decision is about (its number), -1 for all
	public Events					event	= Events.STATE;
	public int						team	= -1;				// the team the decision concerns (0, 1), -1 for none
	public String					robot;						// the robot named in the decision, or null
	public String					text	= "";				// what the referee said
	public int						score1;						// the goals of each team when it was said
	public int						score2;
	public long						time;						// how long the match had been running [ms]

	//Constructors
	public ItemReferee ()
	{
		this.set (0);
	}

	// Instance methods
	public void setState (GameStates state, int player)
	{
		this.player		= player;
		this.state		= state;
	}

	/** Something happened: what, whom it concerns and what the referee said. */
	public void setEvent (Events event, int team, String robot, String text)
	{
		this.event		= event;
		this.team		= team;
		this.robot		= robot;
		this.text		= (text != null) ? text : "";
	}

	public void setScore (int score1, int score2, long time)
	{
		this.score1		= score1;
		this.score2		= score2;
		this.time		= time;
	}

	public String toString ()
	{
		return "STATE [" + state + "] for player <" + player + "> " + event + ((text.length () > 0) ? (": " + text) : "");
	}
}
