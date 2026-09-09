//**********************************************************************

//<copyright>

//BBN Technologies, a Verizon Company
//10 Moulton Street
//Cambridge, MA 02138
//(617) 873-8000

//Copyright (C) BBNT Solutions LLC. All rights reserved.

//</copyright>
//**********************************************************************

//$Source: /cvs/WUCore/sources/wucore/utils/math/MoreMath.java,v $
//$RCSfile: MoreMath.java,v $
//$Revision: 1.1 $
//$Date: 2003/11/26 19:51:25 $
//$Author: humberto $

//**********************************************************************


package wucore.utils.math;

/**
 * MoreMath provides functions that are not part of the standard Math class.
 * <p> <pre>
 * Functions:
 *	asinh(float x) - hyperbolic arcsine
 *	sinh(float x) - hyperbolic sine
 *
 * Need to Implement:
 * Function                Definition                              
 * Hyperbolic cosine       (e^x+e^-x)/2                            
 * Hyperbolic tangent      (e^x-e^-x)/(e^x+e^-x)                   
 * Hyperbolic arc cosine   2 log  (sqrt((x+1)/2) + sqrt((x-1)/2))  
 * Hyperbolic arc tangent  (log  (1+x) - log (1-x))/2
 * </pre>
 */
public abstract class MoreMath {

	/**
	 * 2*Math.PI
	 */
	final public static transient float TWO_PI = (float)Math.PI*2.0f;

	/**
	 * 2*Math.PI
	 */
	final public static transient double TWO_PI_D = Math.PI*2.0d;

	/**
	 * Math.PI/2
	 */
	final public static transient float HALF_PI = (float)Math.PI/2.0f;

	/**
	 * Math.PI/2
	 */
	final public static transient double HALF_PI_D = Math.PI/2.0d;

	/**
	 * Returns the lower two words of a long. This is intended to be
	 * used like this:
	 * <code>getLowDWord(Double.doubleToLongBits(x))</code>.
	 */
	private static int getLowDWord(long x)
	{
		return (int) (x & 0x00000000ffffffffL);
	}

	/**
	 * Returns the higher two words of a long. This is intended to be
	 * used like this:
	 * <code>getHighDWord(Double.doubleToLongBits(x))</code>.
	 */
	private static int getHighDWord(long x)
	{
		return (int) ((x & 0xffffffff00000000L) >> 32);
	}

	/**
	 * Returns a double with the IEEE754 bit pattern given in the lower
	 * and higher two words <code>lowDWord</code> and <code>highDWord</code>.
	 */
	private static double buildDouble(int lowDWord, int highDWord)
	{
		return Double.longBitsToDouble((((long) highDWord & 0xffffffffL) << 32)
				| ((long) lowDWord & 0xffffffffL));
	}

	public static String roundDecimal (double dou, int dec)
	{
		double value;
		String str;

		if (dec < 0)
			return null;	

		value 	= ((double) Math.rint(dou * Math.pow(10, dec))) * Math.pow(10, -dec); 	
		str 	= new Double (value).toString();

		if (str.indexOf(".") == -1)
			return null;		

		if (dec == 0)
			return str.substring(0, str.indexOf("."));			

		if (str.length() <= (str.indexOf(".") + dec))
			return str;

		return str.substring(0, str.indexOf(".") + dec + 1);
	}		

	/**
	 * Rounds the quantity away from 0.
	 */
	static public final double qint1 (double x) 
	{
		return (((int) x) < 0) ? (x - 0.5) : (x + 0.5);
	}
	
	/**
	 * Rounds the quantity away from 0.
	 */
	static public final double qint05 (double x) 
	{
		return (x <= 0.0) ? (x - 1.0) : (x + 1.0);
	}

	static public double mod (double y, double x) 
	{
		if (y >= 0)
			return y - x * (int) (y / x);
		else
			return y + x * ((int) (-y / x) + 1);
	}

	/** sqrt(a^2 + b^2) without under/overflow. **/

	public static final double hypot (double a, double b) 
	{
		double r;

		if (Math.abs(a) > Math.abs(b)) 
		{
			r = b/a;
			r = Math.abs(a)*Math.sqrt(1+r*r);
		} 
		else if (b != 0) 
		{
			r = a/b;
			r = Math.abs(b)*Math.sqrt(1+r*r);
		} 
		else 
			r = 0.0;

		return r;
	}

	static public double log10 (double x)
	{
		return Math.log (x) / Math.log (10.0);
	}	
	
	/**
	 * Returns the cube root of <code>x</code>. The sign of the cube root
	 * is equal to the sign of <code>x</code>.
	 *
	 * Special cases:
	 * <ul>
	 * <li>If the argument is NaN, the result is NaN</li>
	 * <li>If the argument is positive infinity, the result is positive
	 * infinity.</li>
	 * <li>If the argument is negative infinity, the result is negative
	 * infinity.</li>
	 * <li>If the argument is zero, the result is zero with the same
	 * sign as the argument.</li>
	 * </ul>
	 *
	 * @param x the number to take the cube root of
	 * @return the cube root of <code>x</code>
	 * @see #sqrt(double)
	 */
	public static final double cbrt(double x)
	{
		boolean negative = (x < 0);
		double r;
		double s;
		double t;
		double w;

		long bits;
		int l;
		int h;

		// handle the special cases
		if (x == Double.POSITIVE_INFINITY)
			return Double.POSITIVE_INFINITY;
		if (x == Double.NEGATIVE_INFINITY)
			return Double.NEGATIVE_INFINITY;
		if (x == 0)
			return x;

		x = Math.abs(x);
		bits = Double.doubleToLongBits(x);

		if (bits < 0x0010000000000000L)   // subnormal number
		{
//			t = TWO_54;
			t = Math.pow(2, 54);
			t *= x;

			// __HI(t)=__HI(t)/3+B2;
			bits = Double.doubleToLongBits(t);
			h = getHighDWord(bits);
			l = getLowDWord(bits);

			h = h / 3 + CBRT_B2;

			t = buildDouble(l, h);
		}
		else
		{
			// __HI(t)=__HI(x)/3+B1;
			h = getHighDWord(bits);
			l = 0;

			h = h / 3 + CBRT_B1;
			t = buildDouble(l, h);
		}

		// new cbrt to 23 bits
		r =  t * t / x;
		s =  CBRT_C + r * t;
		t *= CBRT_G + CBRT_F / (s + CBRT_E + CBRT_D / s);

		// chopped to 20 bits and make it larger than cbrt(x)
		bits = Double.doubleToLongBits(t);
		h = getHighDWord(bits);

		// __LO(t)=0;
		// __HI(t)+=0x00000001;
		l = 0;
		h += 1;
		t = buildDouble(l, h);

		// one step newton iteration to 53 bits with error less than 0.667 ulps
		s = t * t;		    // t * t is exact
		r = x / s;
		w = t + t;
		r = (r - t) / (w + r);  // r - s is exact
		t = t + t * r;

		return negative ? -t : t;
	}

	/**
	 * Constants for computing {@link #cbrt(double)}.
	 */
	private static final int
	CBRT_B1 = 715094163, // B1 = (682-0.03306235651)*2**20
	CBRT_B2 = 696219795; // B2 = (664-0.03306235651)*2**20

	/**
	 * Constants for computing {@link #cbrt(double)}.
	 */
	private static final double
	CBRT_C =  5.42857142857142815906e-01, // Long bits  0x3fe15f15f15f15f1L
	CBRT_D = -7.05306122448979611050e-01, // Long bits  0xbfe691de2532c834L
	CBRT_E =  1.41428571428571436819e+00, // Long bits  0x3ff6a0ea0ea0ea0fL
	CBRT_F =  1.60714285714285720630e+00, // Long bits  0x3ff9b6db6db6db6eL
	CBRT_G =  3.57142857142857150787e-01; // Long bits  0x3fd6db6db6db6db7L

	/**
	 * Checks if a ~= b.
	 * Use this to test equality of floating point numbers.
	 * <p>
	 * @param a double
	 * @param b double
	 * @param epsilon the allowable error
	 * @return boolean
	 */
	final public static boolean approximately_equal(double a, double b, double epsilon) {
		return (Math.abs(a-b) <= epsilon);
	}

	/**
	 * Checks if a ~= b.
	 * Use this to test equality of floating point numbers.
	 * <p>
	 * @param a float
	 * @param b float
	 * @param epsilon the allowable error
	 * @return boolean
	 */
	final public static boolean approximately_equal(float a, float b, float epsilon) {
		return (Math.abs(a-b) <= epsilon);
	}

	/**
	 * Hyperbolic arcsin.
	 * <p>
	 * Hyperbolic arc sine: log (x+sqrt(1+x^2))                    
	 * @param x float
	 * @return float asinh(x)
	 */
	public static final float asinh(float x) {
		return (float)Math.log(x + Math.sqrt(x * x + 1));
	}

	/**
	 * Hyperbolic arcsin.
	 * <p>
	 * Hyperbolic arc sine: log (x+sqrt(1+x^2))                    
	 * @param x double
	 * @return double asinh(x)
	 */
	public static final double asinh(double x) {
		return Math.log(x + Math.sqrt(x * x + 1));
	}

	/**
	 * Hyperbolic sin.
	 * <p>
	 * Hyperbolic sine: (e^x-e^-x)/2                            
	 * @param x float
	 * @return float sinh(x)
	 */
	public static final float sinh(float x) {
		return (float)(Math.pow(Math.E,x) - Math.pow(Math.E,-x)) / 2.0f;
	}

	/**
	 * Hyperbolic sin.
	 * <p>
	 * Hyperbolic sine: (e^x-e^-x)/2                            
	 * @param x double
	 * @return double sinh(x)
	 */
	public static final double sinh(double x) {
		return (Math.pow(Math.E,x) - Math.pow(Math.E,-x)) / 2.0;
	}

	static public final double sqrt (final double a) 
	{  
		final long x = Double.doubleToLongBits(a) >> 32;  
		return Double.longBitsToDouble((x + 1072632448) << 31);  
	}  
	   
	static public final double pow (final double a, final double b) 
	{  
		final int x = (int) (Double.doubleToLongBits (a) >> 32);  
		final int y = (int) (b * (x - 1072632447) + 1072632447);  
		return Double.longBitsToDouble (((long) y) << 32);  
	}  

	static public final double ipow (final double a, final int b) 
	{  
		double res = 1.0;
		for (int i = 0; i < b; i++)
			res *= a;	
		return res;
	}  

	// HACK - are there functions that already exist?
	/**
	 * Return sign of number.
	 * @param x short
	 * @return int sign -1, 1
	 */
	public static final int sign(short x) {
		return (x < 0) ? -1 : 1;
	}

	/**
	 * Return sign of number.
	 * @param x int
	 * @return int sign -1, 1
	 */
	public static final int sign(int x) {
		return (x < 0) ? -1 : 1;
	}

	/**
	 * Return sign of number.
	 * @param x long
	 * @return int sign -1, 1
	 */
	public static final int sign(long x) {
		return (x < 0) ? -1 : 1;
	}

	/**
	 * Return sign of number.
	 * @param x float
	 * @return int sign -1, 1
	 */
	public static final int sign(float x) {
		return (x < 0f) ? -1 : 1;
	}

	/**
	 * Return sign of number.
	 * @param x double
	 * @return int sign -1, 1
	 */
	public static final int sign(double x) {
		return (x < 0d) ? -1 : 1;
	}

	/**
	 * Check if number is odd.
	 * @param x short
	 * @return boolean
	 */
	public static final boolean odd(short x) {
		return !even(x);
	}

	/**
	 * Check if number is odd.
	 * @param x int
	 * @return boolean
	 */
	public static final boolean odd(int x) {
		return !even(x);
	}

	/**
	 * Check if number is odd.
	 * @param x long
	 * @return boolean
	 */
	public static final boolean odd(long x) {
		return !even(x);
	}

	/**
	 * Check if number is even.
	 * @param x short
	 * @return boolean
	 */
	public static final boolean even(short x) {
		return ((x & 0x1) == 0);
	}

	/**
	 * Check if number is even.
	 * @param x int
	 * @return boolean
	 */
	public static final boolean even(int x) {
		return ((x & 0x1) == 0);
	}

	/**
	 * Check if number is even.
	 * @param x long
	 * @return boolean
	 */
	public static final boolean even(long x) {
		return ((x & 0x1) == 0);
	}

	/**
	 * Converts a byte in the range of -128 to 127 to an int
	 * in the range 0 - 255.
	 * @param b (-128 &lt;= b &lt;= 127)
	 * @return int (0 &lt;= b &lt;= 255)
	 */
	public static final int signedToInt(byte b) {
		return ((int)b & 0xff);
	}

	/**
	 * Converts a short in the range of -32768 to 32767 to
	 * an int in the range 0 - 65535.
	 * @param b (-32768 &lt;= b &lt;= 32767)
	 * @return int (0 &lt;= b &lt;= 65535)
	 */
	public static final int signedToInt(short w) {
		return ((int)w & 0xffff);
	}

	/**
	 * Convert an int in the range of -2147483648 to 2147483647 to a long in
	 * the range 0 to 4294967295.
	 * @param x (-2147483648 &lt;= x &lt;= 2147483647)
	 * @return long (0 &lt;= x &lt;= 4294967295)
	 */
	public static final long signedToLong(int x) {
		return ((long)x & 0xFFFFFFFFL);
	}

	/**
	 * Converts an int in the range of 0 - 65535 to an int in the range
	 * of 0 - 255.
	 * @param w int (0 &lt;= w &lt;= 65535)
	 * @return int (0 &lt;= w &lt;= 255)
	 */
	public static final int wordToByte(int w) {
		return w>>8;
	}

	/**
	 * Build short out of bytes (in big endian order).
	 * @param bytevec[] bytes
	 * @param offset byte offset
	 * @return short
	 */
	public static final short BuildShortBE(byte bytevec[], int offset) {
		return (short)(((int)(bytevec[0+offset]) << 8) |
				(signedToInt(bytevec[1+offset])));
	}

	/**
	 * Build short out of bytes (in little endian order).
	 * @param bytevec[] bytes
	 * @param offset byte offset
	 * @return short
	 */
	public static final short BuildShortLE(byte bytevec[], int offset) {
		return (short)(((int)(bytevec[1+offset]) << 8) |
				(signedToInt(bytevec[0+offset])));
	}

	/**
	 * Build short out of bytes.
	 * @param bytevec[] bytes
	 * @param offset byte offset
	 * @param MSBFirst BE or LE?
	 * @return short
	 */
	public static final short BuildShort(byte bytevec[], int offset,
			boolean MSBFirst) {
		if (MSBFirst) {
			return(BuildShortBE(bytevec, offset));
		} else {
			return(BuildShortLE(bytevec, offset));
		}
	}

	/**
	 * Build short out of bytes (in big endian order).
	 * @param bytevec[] bytes
	 * @param MSBFirst BE or LE?
	 * @return short
	 */
	public static final short BuildShortBE(byte bytevec[], boolean MSBFirst) {
		return BuildShortBE(bytevec, 0);
	}

	/**
	 * Build short out of bytes (in little endian order).
	 * @param bytevec[] bytes
	 * @param MSBFirst BE or LE?
	 * @return short
	 */
	public static final short BuildShortLE(byte bytevec[], boolean MSBFirst) {
		return BuildShortLE(bytevec, 0);
	}

	/**
	 * Build short out of bytes.
	 * @param bytevec[] bytes
	 * @param MSBFirst BE or LE?
	 * @return short
	 */
	public static final short BuildShort(byte bytevec[], boolean MSBFirst) {
		return BuildShort(bytevec, 0, MSBFirst);
	}

	/**
	 * Build int out of bytes (in big endian order).
	 * @param bytevec[] bytes
	 * @param offset byte offset
	 * @return int
	 */
	public static final int BuildIntegerBE(byte bytevec[], int offset) {
		return(((int)(bytevec[0+offset]) << 24) |
				(signedToInt(bytevec[1+offset]) << 16) |
				(signedToInt(bytevec[2+offset]) << 8) |
				(signedToInt(bytevec[3+offset])));
	}

	/**
	 * Build int out of bytes (in little endian order).
	 * @param bytevec[] bytes
	 * @param offset byte offset
	 * @return int
	 */
	public static final int BuildIntegerLE(byte bytevec[], int offset) {
		return(((int)(bytevec[3+offset]) << 24) |
				(signedToInt(bytevec[2+offset]) << 16) |
				(signedToInt(bytevec[1+offset]) << 8) |
				(signedToInt(bytevec[0+offset])));
	}       

	/**
	 * Build int out of bytes.
	 * @param bytevec[] bytes
	 * @param offset byte offset
	 * @param MSBFirst BE or LE?
	 * @return int
	 */
	public static final int BuildInteger(byte bytevec[], int offset,
			boolean MSBFirst) {
		if (MSBFirst)
			return BuildIntegerBE(bytevec, offset);
		else
			return BuildIntegerLE(bytevec, offset);
	}

	/**
	 * Build int out of bytes (in big endian order).
	 * @param bytevec[] bytes
	 * @return int
	 */
	public static final int BuildIntegerBE(byte bytevec[]) {
		return BuildIntegerBE(bytevec, 0);
	}

	/**
	 * Build int out of bytes (in little endian order).
	 * @param bytevec[] bytes
	 * @return int
	 */
	public static final int BuildIntegerLE(byte bytevec[]) {
		return BuildIntegerLE(bytevec, 0);
	}

	/**
	 * Build int out of bytes.
	 * @param bytevec[] bytes
	 * @param MSBFirst BE or LE?
	 * @return int
	 */
	public static final int BuildInteger(byte bytevec[], boolean MSBFirst) {
		if (MSBFirst)
			return BuildIntegerBE(bytevec, 0);
		else
			return BuildIntegerLE(bytevec, 0);
	}

	/**
	 * Build long out of bytes (in big endian order).
	 * @param bytevec[] bytes
	 * @param offset byte offset
	 * @return long
	 */
	public static final long BuildLongBE(byte bytevec[], int offset) {
		return(((long)signedToInt(bytevec[0+offset]) << 56) |
				((long)signedToInt(bytevec[1+offset]) << 48) |
				((long)signedToInt(bytevec[2+offset]) << 40) |
				((long)signedToInt(bytevec[3+offset]) << 32) |
				((long)signedToInt(bytevec[4+offset]) << 24) |
				((long)signedToInt(bytevec[5+offset]) << 16) |
				((long)signedToInt(bytevec[6+offset]) << 8)  | 
				((long)signedToInt(bytevec[7+offset])));
	}

	/**
	 * Build long out of bytes (in little endian order).
	 * @param bytevec[] bytes
	 * @param offset byte offset
	 * @return long
	 */
	public static final long BuildLongLE(byte bytevec[], int offset) {
		return(((long)signedToInt(bytevec[7+offset]) << 56) | 
				((long)signedToInt(bytevec[6+offset]) << 48) |
				((long)signedToInt(bytevec[5+offset]) << 40) | 
				((long)signedToInt(bytevec[4+offset]) << 32) |
				((long)signedToInt(bytevec[3+offset]) << 24) |
				((long)signedToInt(bytevec[2+offset]) << 16) |
				((long)signedToInt(bytevec[1+offset]) << 8)  | 
				((long)signedToInt(bytevec[0+offset])));
	}

	/**
	 * Build long out of bytes.
	 * @param bytevec[] bytes
	 * @param offset byte offset
	 * @param MSBFirst BE or LE?
	 * @return long
	 */
	public static final long BuildLong(byte bytevec[], int offset,
			boolean MSBFirst) {
		if (MSBFirst)
			return BuildLongBE(bytevec, offset);
		else
			return BuildLongLE(bytevec, offset);
	}

	/**
	 * Build long out of bytes (in big endian order).
	 * @param bytevec[] bytes
	 * @return long
	 */
	public static final long BuildLongBE(byte bytevec[]) {
		return BuildLongBE(bytevec, 0);
	}

	/**
	 * Build long out of bytes (in little endian order).
	 * @param bytevec[] bytes
	 * @return long
	 */
	public static final long BuildLongLE(byte bytevec[]) {
		return BuildLongLE(bytevec, 0);
	}

	/**
	 * Build long out of bytes.
	 * @param bytevec[] bytes
	 * @param MSBFirst BE or LE?
	 * @return long
	 */
	public static final long BuildLong(byte bytevec[], boolean MSBFirst) {
		if (MSBFirst)
			return BuildLongBE(bytevec, 0);
		else
			return BuildLongLE(bytevec, 0);
	}

	/*
    public static final void main(String[] args) {
	byte[] b = new byte[4];
	b[0] = (byte)0xff;
	b[1] = (byte)0x7f;
	com.bbn.openmap.util.Debug.output("32767="+BuildShortLE(b, 0));
	b[0] = (byte)0x7f;
	b[1] = (byte)0xff;
	com.bbn.openmap.util.Debug.output("32767="+BuildShortBE(b, 0));
	b[1] = (byte)0xff;
	b[2] = (byte)0xff;
	b[3] = (byte)0xff;
	com.bbn.openmap.util.Debug.output("2147483647="+BuildIntegerBE(b, 0));
	com.bbn.openmap.util.Debug.output("maxuint="+signedToLong(0xffffffff));
    }
	 */
}
