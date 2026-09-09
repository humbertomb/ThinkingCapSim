/*
 * (c) 2002 Alessandro Saffiotti
 * (c) 2004 David Herrero Perez
 * 
 * Convolution element.
 * For compute blur and translate fuzzy grid map
 * 
 */
package tclib.navigation.localisation.fmarkov;

/**
 * @author David Herrero-Perez
 * @version 1.0
 * 
 */

public class ConvolutionElement {
	double mSE[][]; // Element of convolution operator
	
	// Constructor
	public ConvolutionElement()
	{
		mSE = new double[3][3];
		
		mSE[0][0] = 0.0;	mSE[0][1] = 0.0;	mSE[0][2] = 0.0;
		mSE[1][0] = 0.0;	mSE[1][1] = 0.0;	mSE[1][2] = 0.0;
		mSE[2][0] = 0.0;	mSE[2][1] = 0.0;	mSE[2][2] = 0.0;
	}
	
	// Accessors
	public void set(int i, int j, double value)
	{
		mSE[i][j] = value;
	}
	
	public double get(int i, int j)
	{
		return mSE[i][j];
	}
	
	public String toString()
	{
		StringBuffer s;
		s = new StringBuffer();
		
		s.append(mSE[0][0] + " " + mSE[0][1] + " " + mSE[0][2] + "\n");
		s.append(mSE[1][0] + " " + mSE[1][1] + " " + mSE[1][2] + "\n");
		s.append(mSE[2][0] + " " + mSE[2][1] + " " + mSE[2][2]);
		
		return s.toString();
	}
}
