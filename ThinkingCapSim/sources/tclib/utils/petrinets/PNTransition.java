package tclib.utils.petrinets;

import java.util.*;

import wucore.widgets.*;
import wucore.utils.color.*;

public class PNTransition extends PNObject 
{
	public static final int ORIENTATION_VERTICAL = 1;
	public static final int ORIENTATION_DIAGONAL1 = 2;
	public static final int ORIENTATION_HORIZONTAL = 3;
	public static final int ORIENTATION_DIAGONAL2 = 4;
	public static final int ORIENTATION_ALL = 5;
	
	private int orientation;
	
	public final int height = 30;
	public final int width = 3;
	
	int priority;
	
	public boolean highlight;
	
	public double ranval;
	
	public PNTransition() 
	{
		super ();
		
		color	= WColor.BLUE;
		
		this.priority = 0;
		this.orientation = ORIENTATION_VERTICAL;
		highlight = false;
		ranval = 0;
	}
	
	public void setPriority(int p){
		/*System.out.print("Set priority in TRANSITION ");
		 System.out.print(this.name);
		 System.out.print(" to ");
		 System.out.print(p);
		 System.out.println(" !");*/
		this.priority = p;
	}
	
	public void setRan(double ran){      //NEW 23.5.97 AH
		ranval = ran;
	}
	
	public double getRan(){             //NEW 22.5.97 AH
		return ranval;
	}
	
	public int getPriority(){
		return this.priority;
	}
	
	public int getHeight() {
		return height;
	}
	public int getWidth() {
		return width;
	}
	
	public int getOrientation() {
		return orientation;
	}
	
	public void setOrientation(int o) {
		orientation = o;
	}
	
	public void cycleOrientation() {
		switch (orientation) {
		case ORIENTATION_VERTICAL:
			orientation = ORIENTATION_DIAGONAL1;
			break;
		case ORIENTATION_DIAGONAL1:
			orientation = ORIENTATION_HORIZONTAL;
			break;
		case ORIENTATION_HORIZONTAL:
			orientation = ORIENTATION_DIAGONAL2;
			break;
		case ORIENTATION_DIAGONAL2:
			orientation = ORIENTATION_ALL;
			break;
		case ORIENTATION_ALL:
			orientation = ORIENTATION_VERTICAL;
			break;
		}
	}
	
	public boolean canFire(PetriNet p, boolean priorEnabled){        //NEW 23.5.97 AH
		int i;
		boolean canfire = true;
		boolean nonode = true;
		boolean higherprio = false;
		boolean negatedEdge = false;
		int nodeindex;
		
		int index = p.transitions.indexOf(this);
		
		for (i=0; i<p.edges.size(); i++){
			PNEdge e = ((PNEdge) p.edges.elementAt(i));
			if (e.isNegated()) negatedEdge = true;
			
			
			if (((e.getTFrom() == PNEdge.TRANSITION) && (e.getIFrom() == index))
					|| ((e.getTTo() == PNEdge.TRANSITION) && (e.getITo() == index))) 
			{
				nonode = false;
				
				if (e.getTTo() == PNEdge.NODE) {
					int ind = e.getITo();
					PNNode z = ((PNNode) p.nodes.elementAt(ind));
					
					if (z.isFull ()) 
					{
						canfire = false;
						break;
					}
				}

				// System.out.println("4");
				if (e.getTFrom() == PNEdge.NODE) {
					if (negatedEdge)  {
						int ind = e.getIFrom();
						PNNode z = ((PNNode) p.nodes.elementAt(ind));
						
						if (z.getTokens() > 0) { canfire = false;
						break;
						}          //leave **-for-loop
						Vector testTransition = new Vector();                   //
						testTransition.addElement(this);                        //
						if (p.getConnectedItems(testTransition) < 1) {          // neu
							canfire = false;                                   // 11.5.97
							break;                                             // JW
						}                                                       //
					}

					if (priorEnabled) {
						for (int k = 0; k < p.edges.size(); k++){
							PNEdge f = (PNEdge) p.edges.elementAt(k);
							if ((f.getTTo() == PNEdge.TRANSITION) && (index != f.getITo())
									&& (p.getTransition(f.getITo()).priority > this.priority)
									&& (p.getNode(f.getIFrom()).checkNode(f.getWeight())))
								higherprio = true;
						}
					}
					
					
					if (negatedEdge == false){ // System.out.println("7");
						nodeindex = e.getIFrom();
						int w = e.getWeight();
						canfire = (((PNNode) p.nodes.elementAt(nodeindex)).checkNode(w))
						/*&& (checkTrans(p))*/;
						if (canfire == false) break; //leave **-for-loop
					}
				}
			}
			negatedEdge = false;
		}
		if (nonode == true) canfire = false;
		if (priorEnabled == true) {
			if (higherprio == true) canfire = false;}
		
		
		return canfire;
	}
	
	
	public boolean fire(PetriNet p, boolean priorEnabled){          //NEW 23.4.97 AH
		int i;
		int nodeindexFrom;
		int nodeindexTo;
		int index;
		boolean ret = false;
		int w;
		
		
		index = p.transitions.indexOf(this);
		
		if (canFire(p, priorEnabled) == true)
		{
			for (i=0; i<p.edges.size(); i++){
				PNEdge e = (PNEdge) p.edges.elementAt(i);
				if (((e.getTFrom() == PNEdge.TRANSITION) && (e.getIFrom() == index))
						|| ((e.getTTo() == PNEdge.TRANSITION) && (e.getITo() == index)))
				{
					
					if (e.getTFrom() == PNEdge.NODE)
					{  w = e.getWeight();
					nodeindexFrom = ((PNEdge) p.edges.elementAt(i)).getIFrom();
					((PNNode) p.nodes.elementAt(nodeindexFrom)).decTokens(w);
					}
					if (e.getTTo() == PNEdge.NODE)
					{
						w = e.getWeight();
						nodeindexTo = ((PNEdge) p.edges.elementAt(i)).getITo();
						if (e.isNegated()) ((PNNode) p.nodes.elementAt(nodeindexTo)).decTokens(1);
						else            ((PNNode) p.nodes.elementAt(nodeindexTo)).incTokens(w);
					}
				}
			}
			ret = true;
			/*System.out.print("TRANSITION ");
			 System.out.print(this.name);
			 System.out.println(" fired ! "); */
		}
		else {/*System.out.print("TRANSITION ");
		System.out.print(this.name);
		System.out.println(" cannot fire !");*/}
		
		return ret;
		
	}
	
	public boolean checkTrans(PetriNet p){
		int i;
		boolean ret = false;
		int index = p.transitions.indexOf(this);
		
		for (i=0; i< p.edges.size(); i++){
			PNEdge e = (PNEdge) p.edges.elementAt(i);
			if ((e.getTTo() == PNEdge.NODE) && (e.getIFrom() == index) && (e.getTFrom() == PNEdge.TRANSITION))
				ret = true;
		}
		return ret;
	}
	
	
	public double distance(double x, double y){
		double dist;
		double dx;
		double dy;
		dx = (this.x - x);
		dy = (this.y - y);
		dist = Math.sqrt(dx*dx + dy*dy);
		return dist;
	}
	
	public void set (PNTransition t)
	{
		super.set (t);
		
		setPriority(t.priority);
		setOrientation(t.orientation);
		setRan(t.ranval);
	}
	
	public void randomize(){          //NEW 22.5.97 AH		
		this.ranval = Math.random();
	}
	
	public boolean equals(PNTransition t){
		boolean isequal = false;
		
		if ( (x == t.x) && (y == t.y) && (priority == t.priority) && (name == t.name)
				&& (orientation == t.orientation)
				&& (highlight == t.highlight) && (ranval == t.ranval))
			isequal = true;
		
		return isequal;
	}
	
	public void draw (Model2D model, PetriNet pn)
	{
		double			x, y;
		double			x1, y1;
		
		x 	= (double) x ();
		y	= (double) y ();
		
		switch (orientation)
		{
/*			case ORIENTATION_DIAGONAL1:
			{
				
				int[] xpoints = new int[4];
				int[] ypoints = new int[4];
				
				double Sin45 = 0.707106781;
				
				xpoints[0] = (int) (t.getX() + ((-t.getWidth() / 2.0) * Sin45 + (-t.getHeight() / 2.0) * Sin45));
				ypoints[0] = (int) (t.getY() + ((-t.getWidth() / 2.0) * Sin45 + (-t.getHeight() / 2.0) * (-Sin45)));
				xpoints[1] = (int) (t.getX() + (( t.getWidth() / 2.0) * Sin45 + (-t.getHeight() / 2.0) * Sin45));
				ypoints[1] = (int) (t.getY() + (( t.getWidth() / 2.0) * Sin45 + (-t.getHeight() / 2.0) * (-Sin45)));
				xpoints[2] = (int) (t.getX() + (( t.getWidth() / 2.0) * Sin45 + ( t.getHeight() / 2.0) * Sin45));
				ypoints[2] = (int) (t.getY() + (( t.getWidth() / 2.0) * Sin45 + ( t.getHeight() / 2.0) * (-Sin45)));
				xpoints[3] = (int) (t.getX() + ((-t.getWidth() / 2.0) * Sin45 + ( t.getHeight() / 2.0) * Sin45));
				ypoints[3] = (int) (t.getY() + ((-t.getWidth() / 2.0) * Sin45 + ( t.getHeight() / 2.0) * (-Sin45)));
				
				g.fillPolygon(xpoints, ypoints, 4);
			} 
			break;
			case ORIENTATION_DIAGONAL2:
			{
				int[] xpoints = new int[4];
				int[] ypoints = new int[4];
				
				double Sin45 = 0.707106781;
				
				xpoints[0] = (int) (t.getX() + ((-t.getWidth() / 2.0) * Sin45 + (-t.getHeight() / 2.0) * Sin45));
				ypoints[0] = (int) (t.getY() + ((-t.getWidth() / 2.0) * (-Sin45) + (-t.getHeight() / 2.0) * Sin45));
				xpoints[1] = (int) (t.getX() + (( t.getWidth() / 2.0) * Sin45 + (-t.getHeight() / 2.0) * Sin45));
				ypoints[1] = (int) (t.getY() + (( t.getWidth() / 2.0) * (-Sin45) + (-t.getHeight() / 2.0) * Sin45));
				xpoints[2] = (int) (t.getX() + (( t.getWidth() / 2.0) * Sin45 + ( t.getHeight() / 2.0) * Sin45));
				ypoints[2] = (int) (t.getY() + (( t.getWidth() / 2.0) * (-Sin45) + ( t.getHeight() / 2.0) * Sin45));
				xpoints[3] = (int) (t.getX() + ((-t.getWidth() / 2.0) * Sin45 + ( t.getHeight() / 2.0) * Sin45));
				ypoints[3] = (int) (t.getY() + ((-t.getWidth() / 2.0) * (-Sin45) + ( t.getHeight() / 2.0) * Sin45));
				
				g.fillPolygon(xpoints, ypoints, 4);
			}
			break;
*/
		case ORIENTATION_ALL:
			// Vertical bars
			x1	= x - height * 0.5;
			y1	= y - height * 0.5;
			model.addRawBox (x1, y1, x1 + width, y1 + height, Model2D.FILLED, ColorTool.fromWColorToColor(color));
			x1	= x + height * 0.5;
			y1	= y - height * 0.5;
			model.addRawBox (x1, y1, x1 + width, y1 + height, Model2D.FILLED, ColorTool.fromWColorToColor(color));
			
			// Horizontal bars
			x1	= x - height * 0.5;
			y1	= y - height * 0.5;
			model.addRawBox (x1, y1, x1 + height + width, y1 + width, Model2D.FILLED, ColorTool.fromWColorToColor(color));
			x1	= x - height * 0.5;
			y1	= y + height * 0.5;
			model.addRawBox (x1, y1, x1 + height + width, y1 + width, Model2D.FILLED, ColorTool.fromWColorToColor(color));
			break;
			
		case ORIENTATION_HORIZONTAL:
			x1	= x - height * 0.5;
			y1	= y - width * 0.5;
			model.addRawBox (x1, y1, x1 + height, y1 + width, Model2D.FILLED, ColorTool.fromWColorToColor(color));
			break;
			
		case ORIENTATION_VERTICAL:
		default:
			x1	= x - width * 0.5;
			y1	= y - height * 0.5;
			model.addRawBox (x1, y1, x1 + width, y1 + height, Model2D.FILLED, ColorTool.fromWColorToColor(color));
		}
		if (isNamed ())
			model.addRawText (x, y + height, name, Model2D.J_CENTER, ColorTool.fromWColorToColor(color));				
	}
	
	public void fromPropString (StringTokenizer st)
	{
		super.fromPropString (st);
		
		orientation	= Integer.parseInt (st.nextToken());
		priority		= Integer.parseInt (st.nextToken());
	}
		
	public String toPropString ()
	{
		return super.toPropString () + ", " + orientation + ", " + priority;
	}
}
