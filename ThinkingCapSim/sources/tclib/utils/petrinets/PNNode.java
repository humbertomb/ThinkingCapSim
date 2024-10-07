package tclib.utils.petrinets;

import java.util.*;

import wucore.widgets.*;
import wucore.utils.color.*;

public class PNNode extends PNObject
{
	// Height and width of tokens
	static private final double 			TOKEN_H 		= 10.0;
	static private final double 			TOKEN_W 		= 3.0;
	
	private int      					radius = 15;
	
	protected int 						tokens;
	protected int						capacity;
	
	public PNNode()
	{
		super ();
		
		this.tokens 		= 0;
		this.capacity	= Integer.MAX_VALUE;
		
		color 			= WColor.LIGHT_GRAY;
	}

	public PNNode (String name)
	{
		this ();
		
		setName (name);
	}

	public int getRadius() {
		return radius;
	}
	
	public int getTokens() {
		return tokens;
	}
	
	public void setTokens(int items) {
		this.tokens = items;
	}
	
	public void decTokens(int w) {
		if (tokens >= w) {
			this.tokens = this.tokens - w;
		}
	}
	
	public void incTokens(int w) {
		this.tokens = this.tokens + w;		// TODO check maxium
	}
	
	// neu (15.4.97 JW)
	public void decTokens() {
		if (tokens > 0) {
			this.tokens--;
		}
	}
	
	public void incTokens() {
		this.tokens++;		// TODO check maxium
	}
	
	public boolean checkNode (int w)
	{
		if (tokens >= w) return true;
		else return false;
	}
	
	/**
	 * @return Returns the capacity.
	 */
	public int getCapacity() {
		return capacity;
	}
	/**
	 * @param capacity The capacity to set.
	 */
	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}
	
	public boolean isLimited ()
	{
		return (capacity < Integer.MAX_VALUE);
	}

	public boolean isFull ()
	{
		return (tokens >= capacity);
	}

	public void set (PNNode n) 
	{
		super.set (n);
		
		setTokens(n.tokens);
	}
	
	public void draw (Model2D model, PetriNet pn)
	{
		double			x, y;
		double			x1, x2, x3, yy;
		
		x 	= (double) x ();
		y	= (double) y ();
	
		model.addRawCircle (x,  y, (double) radius, Model2D.FILLED, ColorTool.fromWColorToColor(color));
		model.addRawCircle (x,  y, (double) radius, ColorTool.fromWColorToColor(WColor.BLACK));
		
		switch (tokens)
		{
		case 1:
			x1	= x - (TOKEN_W / 2);
			yy	= y - (TOKEN_H / 2);
			model.addRawBox (x1, yy, x1 + TOKEN_W, yy + TOKEN_H, Model2D.FILLED, ColorTool.fromWColorToColor(WColor.BLACK));
			break;
		case 2:
			x1	= x + (TOKEN_W / 2);
			x2	= x - (3 * (TOKEN_W / 2));
			yy	= y - (TOKEN_H / 2);
			model.addRawBox (x1, yy, x1 + TOKEN_W, yy + TOKEN_H, Model2D.FILLED, ColorTool.fromWColorToColor(WColor.BLACK));
			model.addRawBox (x2, yy, x2 + TOKEN_W, yy + TOKEN_H, Model2D.FILLED, ColorTool.fromWColorToColor(WColor.BLACK));
			break;
		case 3:
			x1	= x - (5 * (TOKEN_W / 2));
			x2	= x - (TOKEN_W / 2);
			x3	= x + (3 * (TOKEN_W / 2));
			yy	= y - (TOKEN_H / 2);
			model.addRawBox (x1, yy, x1 + TOKEN_W, yy + TOKEN_H, Model2D.FILLED, ColorTool.fromWColorToColor(WColor.BLACK));
			model.addRawBox (x2, yy, x2 + TOKEN_W, yy + TOKEN_H, Model2D.FILLED, ColorTool.fromWColorToColor(WColor.BLACK));
			model.addRawBox (x3, yy, x3 + TOKEN_W, yy + TOKEN_H, Model2D.FILLED, ColorTool.fromWColorToColor(WColor.BLACK));
			break;
		default:
			if (tokens > 0)
				model.addRawText (x, y, Integer.toString (tokens), Model2D.J_CENTER, ColorTool.fromWColorToColor(WColor.BLACK));
		}
		if (isNamed ())
			model.addRawText (x, y + radius * 2.2, name, Model2D.J_CENTER, ColorTool.fromWColorToColor(WColor.BLACK));
	}
	
	public void fromPropString (StringTokenizer st)
	{
		super.fromPropString (st);
		
		tokens	= Integer.parseInt (st.nextToken());
		
		if (st.hasMoreTokens ())
			capacity	= Integer.parseInt (st.nextToken());
	}
	
	public String toPropString ()
	{
		String			out;
		
		out	= super.toPropString () + ", " + tokens;
		
		if (capacity < Integer.MAX_VALUE)
			out += ", " + capacity;
		
		return out;
	}
}
