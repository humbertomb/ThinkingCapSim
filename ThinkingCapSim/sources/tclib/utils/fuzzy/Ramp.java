/*
 * @(#)Ramp.java	1.0 2003/12/08
 * 
 * (c) 2003 Denis Remondini
 */

package tclib.utils.fuzzy;


/**
 * This class implements a ramp fuzzy set.
 *  
 * @version	1.0 	08 Dic 2003
 * @author 	Denis Remondini
 */
public class Ramp extends FSet {
	/* RampUp fuzzy sets have the following shape. 
						  ____________ yB
						 /
						/
					   /
		yA ___________/
					 xA  xB 
	*/
	
	/**
	 * Specifies you want to use a ramp up
	 */
	public final static int RAMP_UP = 0;
	
	/**
	 * Specifies you want to use a ramp down
	 */
	public final static int RAMP_DOWN = 1;
	
	/**
	 * Specifies what kind of ramp you want to use
	 */
	public int type = RAMP_UP;
	
	/**
	 * Creates a ramp up where the first point has the y coordinate equal to 0 
	 * and the second one has the y coordinate equal to 1 
	 * @param xA x coordinate of the first point
	 * @param xB x coordinate of the second point
	 */
	public Ramp(double xA, double xB) {
		setPoints(xA, xB);
	}
	
	/**
	 * Creates a ramp up where the first point has the y coordinate equal to 0 
	 * and the second one has the y coordinate equal to 1 
	 * @param type type of ramp (RAMP_UP or RAMP_DOWN)
	 * @param xA x coordinate of the first point
	 * @param xB x coordinate of the second point
	 */
	public Ramp (int type, double xA, double xB) {
		this.type	= type;
		setPoints(xA, xB);
	}
	
	/**
	 * Creates a ramp up
	 * @param xA x coordinate of the first point
	 * @param yA y coordinate of the first point
	 * @param xB x coordinate of the second point
	 * @param yB y coordinate of the second point
	 */
	public Ramp(double xA, double yA, double xB, double yB) {
		setPoints(xA,yA,xB,yB);
	}
	
	/**
	 * Specifies the first and the second point of the ramp
	 * @param xA x coordinate of the first point
	 * @param yA y coordinate of the first point
	 * @param xB x coordinate of the second point
	 * @param yB y coordinate of the second point
	 */
	public void setPoints(double xA, double yA, double xB, double yB) {
//		TODO: check if it's better to throws an exception
		if (xA > xB)
			System.out.println("WARNING: the Ramp must have xB greater or equal to xA");
		
		shape[0].x (xA);
		shape[0].y (yA);
		shape[1].x (xB);
		shape[1].y (yB);
	}
	
	/**
	 * Specifies the x coordinates for the first and the second point of a ramp
	 * that goes from 0 to 1 or viceversa (depends on the value of the type predicate)
	 * @param xA x coordinate of the first point
	 * @param xB x coordinate of the second point
	 */
	public void setPoints(double xA, double xB) {
		if (type == RAMP_UP)		
			setPoints(xA,0,xB,1);
		else
			setPoints(xA,1,xB,0);
	}
	
	/**
	 * Duplicates the ramp fuzzy set
	 */
	public FSet dupset() {
		return new Ramp( shape[0].x(), shape[0].y(), shape[1].x(),shape[1].y() );
	}

	/**
	 * Calculates the membership degree of the fuzzy predicate
	 * @param x fuzzy predicate 
	 * @return membership degree
	 */
	public double dmember(double x) {
		if (x >= shape[1].x()) 
			return shape[1].y();
		
		if (x < shape[0].x())  
			return shape[0].y();
		else {
			double tmp;
			tmp = (x * (shape[1].y()-shape[0].y()));
			tmp -= shape[1].x()*(shape[1].y()-shape[0].y());
			tmp += shape[1].y()*(shape[1].x()-shape[0].x());
			tmp = tmp / (shape[1].x() - shape[0].x());
			return tmp;
		}
	}
	
	/**
	 * Calculates the membership degree of the fuzzy predicate individuated by the 
	 * x coordinates for the first and the second point of a ramp that goes from 0 to 1 
	 * or viceversa (depends on the value of the type predicate)
	 * @param x fuzzy predicate 
	 * @param xA x coordinate of the first point of the ramp 
	 * @param xB x coordinate of the second point of the ramp
	 * @return membership degree
	 */
	public double dmember(double x, double xA, double xB) {
		setPoints(xA,xB);
		return dmember(x);
	}

	/**
	 * Discounts the fuzzy set
	 * @param alpha discount value
	 * @return a new fuzzy set discounted
	 */
	public FSet alphacut(double alpha) {
			
		FSet set;
		
		set = dupset();
		alphacut (set, alpha);
		
		return set;
	}
	
	/**
	 * Discounts the fuzzy set
	 * @param set the Ramp fuzzy set to discount
	 * @param alpha discount value
	 */
	//	TODO: consider the use of the other T-NORM in addition to the MIN
	public void alphacut(FSet set, double alpha) {
		Ramp rampSet = (Ramp) set;
		
		// use the MIN tnorm
		if (rampSet.type == RAMP_UP) {
		    if (alpha <= rampSet.shape[0].y()) {
			    rampSet.shape[0].y (alpha);
			    rampSet.shape[1].y (alpha);
		    }
		    if ((alpha > rampSet.shape[0].y()) && (alpha < rampSet.shape[1].y())) {
			   double tmp;
			   tmp = alpha*(rampSet.shape[1].x()-rampSet.shape[0].x());
			   tmp -= rampSet.shape[0].y()*(rampSet.shape[1].x()-rampSet.shape[0].x());
			   tmp += rampSet.shape[0].x()*(rampSet.shape[1].y()-rampSet.shape[0].y());
			   tmp = tmp / (rampSet.shape[1].y()-rampSet.shape[0].y());
			   rampSet.shape[1].x (tmp);
			   rampSet.shape[1].y (alpha);
		    }
		}
		else {
			if (alpha <= rampSet.shape[1].y()) {
				rampSet.shape[0].y (alpha);
				rampSet.shape[1].y (alpha);
			}
			if ((alpha > rampSet.shape[1].y()) && (alpha < rampSet.shape[0].y())) {
				double tmp;
				tmp = alpha*(rampSet.shape[1].x()-rampSet.shape[0].x());
				tmp -= rampSet.shape[0].y()*(rampSet.shape[1].x()-rampSet.shape[0].x());
				tmp += rampSet.shape[0].x()*(rampSet.shape[1].y()-rampSet.shape[0].y());
				tmp = tmp / (rampSet.shape[1].y()-rampSet.shape[0].y());
				rampSet.shape[0].x (tmp);
				rampSet.shape[0].y (alpha);
			}
		}
			
	}

	/**
	 * This method MUST not be used with this kind of fuzzy set
	 * @return a value of 1.0
	 */
	public double integral ()  
	{
		return 1.0;
	}

	/**
	 * This method MUST not be used with this kind of fuzzy set
	 * @return a value of 1.0
	 */
	public double moment ()  
	{
		return 1.0;
	}
	
	/**
	 * Returns a string representation of this fuzzy set
	 */
	public String toString() {
		return new String ("[" + shape[0].toString() + ", " + shape[1].toString() + "]");
	}
}
