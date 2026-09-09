package  wucore.utils.math.poly;

import  java.io.Serializable;

/**
 * 
 * A Java class for performing complex number arithmetic to double precision.
 * 
 **/

public class Complex implements Cloneable, Serializable {
	
	public static final String VERSION	= "1.0 FINAL alm";
	public static final String DATE		= "Fri 29-Jul-97";
	public static final String AUTHOR	= "sandy@almide.demon.co.uk";
	public static final String REMARK	= "Class available from http://www.netlib.org/";
	
	// Switches on debugging information.
	// protected static boolean debug		= false;
	
	// Whilst debugging:  the nesting level when tracing method calls.
	// private static int trace_nesting		= 0;
	
	// Twice radians is the same thing as 360 degrees.
	protected static final double TWO_PI	=  2.0 * Math.PI;
	
	// A constant representing i, the famous square root of -1.
	// The other square root of -1 is -i.
	public static final Complex	i			=  new Complex(0.0, 1.0);
	
	// private static final Complex one		=  new Complex(1.0, 0.0);
	// private static double epsilon		=  Math.abs(1.0e-5);
	// private static long objectCount;
	
	private double re;
	private double im;
	
	//---------------------------------//
	//           CONSTRUCTORS          //
	//---------------------------------//
	
	// Constructs a Complex representing the number zero.
	public Complex () {
		this(0.0, 0.0);
	}
	
	// Constructs a Complex representing a real number.
	public Complex (double re) {
		this(re, 0.0);
	}
	
	// Constructs a separate new Complex from an existing Complex.
	public Complex (Complex z) {
		this(z.re, z.im);
	}
	
	// Constructs a Complex from real and imaginary parts.
	public Complex (double re, double im) {
		this.re = re;
		this.im = im;
		
		// if (debug) System.out.println(indent(trace_nesting) + "new Complex, #" + (++objectCount));
	}
	
	//---------------------------------//
	//              DEBUG              //
	//---------------------------------//
	
	/*
	// BETA Debugging methods...
	
	private static void entering (String what) {
		System.out.print(indent(trace_nesting) + what);
		trace_nesting++;
	}
	
	private static void enter (String what, double param1, double param2) {
		entering(what);
		System.out.println("(" + param1 + ", " + param2 + ") ");
	}
	
	private static void enter (String what, double param) {
		entering(what);
		System.out.println("(" + param + ") ");
	}
	
	private static void enter (String what, Complex z) {
		entering(what);
		System.out.println("(" + z + ") ");
	}
	
	private static void enter (String what, Complex z1, Complex z2) {
		entering(what);
		System.out.println("(" + z1 + ", " + z2 + ") ");
	}
	
	private static void enter (String what, Complex z, double x) {
		entering(what);
		System.out.println("(" + z + ", " + x + ") ");
	}
	
	private static void enter (String what, Complex z, double x, double y) {
		entering(what);
		System.out.println("(" + z + ", " + cart(x, y) + ") ");
	}
	
	private static void enter (String what, Complex z1, Complex z2, double x) {
		entering(what);
		System.out.println("(" + z1 + ", " + z2 + ", " + x + ") ");
	}
	
	private static void leaving (String what) {
		trace_nesting--;
		System.out.print(indent(trace_nesting) + "is ");
	}
	
	private static void leave (String what, boolean result) {
		leaving(what);
		System.out.println(result);
	}
	
	private static void leave (String what, double result) {
		leaving(what);
		System.out.println(result);
	}
	
	private static void leave (String what, Complex result) {
		leaving(what);
		System.out.println(result);
	}
	
	private static String indent (int nesting) {
		StringBuffer indention =  new StringBuffer("");
		
		for (int i =  0; i < nesting; i++) {
			indention.append("    ");
		}
		
		return  indention.toString();
	}
	
	*/
	
	// Useful for checking up on the exact version.
	public static void main (String[] args) {
		System.out.println();
		System.out.println("Module : " + Complex.class.getName());
		System.out.println("Version: " + Complex.VERSION);
		System.out.println("Date   : " + Complex.DATE);
		System.out.println("Author : " + Complex.AUTHOR);
		System.out.println("Remark : " + Complex.REMARK);
		System.out.println();
		System.out.println("Hint:  use TestComplex to test the class.");
		System.out.println();
	}
	
	//---------------------------------//
	//             STATIC              //
	//---------------------------------//
	
	// Returns a Complex representing a real number.
	public static Complex real (double real) {
		return  new Complex(real, 0.0);
	}
	
	// Returns a Complex from real and imaginary parts.
	public static Complex cart (double re, double im) {
		return  new Complex(re, im);
	}
	
	// Returns a Complex from a size and direction.
	public static Complex polar (double r, double theta) {
		if (r < 0.0) {
			theta	+=	Math.PI;
			r		= 	-r;
        }
		
		theta =  theta % TWO_PI;
		
		return  cart(r * Math.cos(theta), r * Math.sin(theta));
	}
	
	// Returns the Complex base raised to the power of the exponent.
	public static Complex pow (Complex base, double exponent) {
		// return  base.log().scale(exponent).exp();
		
		double re = exponent * Math.log(base.abs());
		double im = exponent * base.arg();
		double scalar =  Math.exp(re);
		
		return  cart( scalar * Math.cos(im), scalar * Math.sin(im) );
	}
	
	// Returns the base raised to the power of the <tt>Complex</tt> exponent.
	public static Complex pow (double base, Complex exponent) {
		// return  real(base).log().mul(exponent).exp();
		
		double re	= Math.log(Math.abs(base));
		double im	= Math.atan2(0.0, base);
		
		double re2	= (re*exponent.re) - (im*exponent.im);
		double im2	= (re*exponent.im) + (im*exponent.re);
		
		double scalar = Math.exp(re2);
		
		return  cart( scalar * Math.cos(im2), scalar * Math.sin(im2) );
	}
	
	// Returns the Complex base raised to the power of the Complex exponent.
	public static Complex pow (Complex base, Complex exponent) {
		// return  base.log().mul(exponent).exp();
		
		double re =  Math.log(base.abs());
		double im =  base.arg();
		
		double re2 =  (re*exponent.re) - (im*exponent.im);
		double im2 =  (re*exponent.im) + (im*exponent.re);
		
		double scalar =  Math.exp(re2);
		
		return  cart( scalar * Math.cos(im2), scalar * Math.sin(im2) );
	}
	
	//---------------------------------//
	//             PUBLIC              //
	//---------------------------------//
	
	// Returns <tt>true</tt> if either the real or imaginary component of this
	public boolean isInfinite () {
		return  ( Double.isInfinite(re) || Double.isInfinite(im) );
	}
	
	// Returns <tt>true</tt> if either the real or imaginary component of this
	public boolean isNaN () {
		return  ( Double.isNaN(re) || Double.isNaN(im) );
	}
	
	// Decides if two Complex numbers are "sufficiently" alike to be considered equal.
	// tolerance is the maximum magnitude of the difference between them before they
	// are considered not equal.
	// Checking for equality between two real numbers on computer hardware is a tricky
	// business. Try System.out.println((1.0/3.0 * 3.0)); and you'll see the nature of
	// the problem!  It's just as tricky with Complex numbers.
	// Realize that because of these complications, it's possible to find that the
	// magnitude of one Complex number a is less than another, b, and yet
	// a.equals(b, myTolerance) returns true. Be aware!
	public boolean equals (Complex z, double tolerance) {
		// still true when _equal_ to tolerance? ...
		return  abs(re - z.re, im -z.im) <= Math.abs(tolerance);
		// ...and tolerance is always non-negative
	}
	
	// Indicates whether some other object is "equal to" this one.
	// The result is true if and only if the argument is not null and is a Complex
	// object that has exactly the same imaginary and real parts as this object.
	public boolean equals(Object obj) {
		
		if (obj == this)						return true;
		if (obj == null)						return false;
		if (getClass() != obj.getClass())		return false;
		
		Complex other = (Complex)obj;
		if (re == other.re && im == other.im)	return true;
		
		return false;
	}
	
	// Returns a hash code value for the object.
	public int hashcode() {
		return (int)(re*1024*31 + im*1024*37);
	}
	
	// Extracts the real part of a Complex as a double.
	public double re () {
		return  re;
	}
	
	// Extracts the imaginary part of a Complex as a double.
	public double im () {
		return  im;
	}
	
	// Returns the square of the "length" of a Complex number.
	public double norm () {
		return  (re*re) + (im*im);
	}
	
	// Returns the magnitude of a Complex number.
	// In other words, it's Pythagorean distance from the origin
	// (0 + 0i, or zero).
	// The magnitude is also referred to as the "modulus" or "length".
	// Always non-negative.
	public double abs () {
		return  abs(re, im);
	}
	
	static private double abs (double x, double y) {
		//  abs(z)  =  sqrt(norm(z))
		
		// Adapted from "Numerical Recipes in Fortran 77: The Art of Scientific Computing" (ISBN 0-521-43064-X)
		
		double absX =  Math.abs(x);
		double absY =  Math.abs(y);
		
		if (absX == 0.0 && absY == 0.0) { // !!! Numerical Recipes, mmm?
			return  0.0;
		} else if (absX >= absY) {
			double d = y / x;
			return  absX*Math.sqrt(1.0 + d*d);
		} else {
			double d = x / y;
			return  absY*Math.sqrt(1.0 + d*d);
		}
	}
	
	// Returns the principal angle of a Complex number, in radians, measured
	// counter-clockwise from the real axis. (Think of the reals as the x-axis,
	// and the imaginaries as the y-axis.)
	// There are infinitely many solutions, besides the principal solution.
	// If A is the principal solution of arg(z), the others are of the form: A + 2*k*PI
	// where k is any integer.
	public double arg () {
		return  Math.atan2(im, re);
	}
	
	// Returns the "negative" of a Complex number.
	public Complex neg () {
		return  this.scale(-1.0);
	}
	
	//  Returns the Complex "conjugate".
	public Complex conj () {
		return  cart(re, -im);
	}
	
	// Returns the Complex multiplicative inverse.
	// DEPRECATED !!!
	//public Complex inv () {
	//	double scalar =  1.0 / ((re*re)+(im*im));
	//	return  cart(re*scalar, - im*scalar);
	//}
	
	static private void inv (Complex z) {
		double zRe, zIm;
		double scalar;
		if (Math.abs(z.re) >= Math.abs(z.im)) {
			scalar =  1.0 / ( z.re + z.im*(z.im/z.re) );
			
			zRe =    scalar;
			zIm =    scalar * (- z.im/z.re);
		} else {
			scalar =  1.0 / ( z.re*(z.re/z.im) + z.im );
			
			zRe =    scalar * (  z.re/z.im);
			zIm =  - scalar;
		}
		
		z.re = zRe;
		z.im = zIm;
	}
	
	// Returns the Complex scaled by a real number.
	public Complex scale (double scalar) {
		return  cart(scalar*re, scalar*im);
	}
	
	// To perform z1 + z2, you write z1.add(z2).
	public Complex add (Complex z) {
		return  cart(re + z.re, im + z.im);
	}
	
	// To perform z1 - z2, you write z1.sub(z2).
	public Complex sub (Complex z) {
		return  cart(re - z.re, im - z.im);
	}
	
	// To perform z1 * z2, you write z1.mul(z2).
	public Complex mul (Complex z) {
		return  cart( (re*z.re) - (im*z.im), (re*z.im) + (im*z.re) );
		// return  cart( (re*z.re) - (im*z.im), (re + im)*(z.re + z.im) - re*z.re - im*z.im);
	}
	
	// To perform z1 / z2, you write z1.div(z2).
	public Complex div (Complex z) {
		Complex result =  new Complex(this);
		div(result, z.re, z.im);
		return  result;
	}
	
	static private void div (Complex z, double x, double y) {
		// Adapted from "Numerical Recipes in Fortran 77: The Art of Scientific Computing" (ISBN 0-521-43064-X)
		
		double zRe, zIm;
		double scalar;
		
		if (Math.abs(x) >= Math.abs(y)) {
			scalar =  1.0 / ( x + y*(y/x) );
			
			zRe =  scalar * (z.re + z.im*(y/x));
			zIm =  scalar * (z.im - z.re*(y/x));
		} else {
			scalar =  1.0 / ( x*(x/y) + y );
			
			zRe =  scalar * (z.re*(x/y) + z.im);
			zIm =  scalar * (z.im*(x/y) - z.re);
		}
		
		z.re = zRe;
		z.im = zIm;
	}
	
	// Returns a Complex representing one of the two square roots.
	public Complex sqrt () {
		Complex result =  new Complex(this);
		sqrt(result);
		return  result;
	}
	
	static private void sqrt (Complex z) {
		// Jim Shapiro <jnshapi@argo.ecte.uswc.uswest.com>
		// adapted from "Numerical Recipies in C" (ISBN 0-521-43108-5) by William H. Press et al
		
		double mag	= z.abs();
		
		if (mag > 0.0) {
			if (z.re > 0.0) {
				double temp = Math.sqrt(0.5 * (mag + z.re));
				
				z.re = temp;
				z.im = 0.5 * z.im / temp;
			} else {
				double temp =  Math.sqrt(0.5 * (mag - z.re));
				
				if (z.im < 0.0) {
					temp =  -temp;
				}
				
				z.re =  0.5 * z.im / temp;
				z.im =  temp;
			}
		} else {
			z.re =  0.0;
			z.im =  0.0;
		}
	}
	
	// Returns this Complex raised to the power of a Complex exponent.
	public Complex pow (Complex exponent) {
		return  Complex.pow(this, exponent);
	}
	
	// Returns the number e "raised to" a Complex power.
	public Complex exp () {
		double scalar =  Math.exp(re); // e^ix = cis x
		return cart( scalar * Math.cos(im), scalar * Math.sin(im) );
	}
	
	// Returns the principal natural logarithm of a Complex number.
	public Complex log () {
		return  cart( Math.log(this.abs()), this.arg() ); // principal value
	}
	
	// Returns the principallogarithm (base 10) of a Complex number.
	// DEPRECATED !!!
	//public Complex log10 () {
	//	Complex result;
		// if (debug) enter("log10", this);
		
	//	double scalar = 1.0/Math.log(10.0);
		
		// result = this.log().scale(scalar);
		
	//	result =  cart( scalar * Math.log(this.abs()), scalar * this.arg() );
		
		// if (debug) leave("log10", result);
		
	//	return  result;
	//}
	
	// Returns the sine of a Complex number.
	public Complex sin () {
		Complex result;
		// sin(z)  =  ( exp(i*z) - exp(-i*z) ) / (2*i)
		
		double scalar;
		double iz_re, iz_im;
		double _re1, _im1;
		double _re2, _im2;
		
		// iz: i.mul(z) ...
		iz_re = -im;
		iz_im = re;
		
		// _1: iz.exp() ...
		scalar =  Math.exp(iz_re);
		_re1 =  scalar * Math.cos(iz_im);
		_im1 =  scalar * Math.sin(iz_im);
		
		// _2: iz.neg().exp() ...
		scalar =  Math.exp(-iz_re);
		_re2 = scalar * Math.cos(-iz_im);
		_im2 = scalar * Math.sin(-iz_im);
		
		// _1: _1.sub(_2) ...
		_re1 = _re1 - _re2; // !!!
		_im1 = _im1 - _im2; // !!!
		
		// result: _1.div(2*i) ...
		result =  cart( 0.5*_im1, -0.5*_re1 );
		// result = cart(_re1, _im1);
		// div(result, 0.0, 2.0);
		
		return  result;
	}
	
	// Returns the cosine of a Complex number.
	public Complex cos () {
		Complex result;
		// cos(z) = ( exp(i*z) + exp(-i*z) ) / 2
		
		double scalar;
		double iz_re, iz_im;
		double _re1, _im1;
		double _re2, _im2;
		
		// iz: i.mul(z) ...
		iz_re = -im;
		iz_im = re;
		
		// _1: iz.exp() ...
		scalar =  Math.exp(iz_re);
		_re1 =  scalar * Math.cos(iz_im);
		_im1 =  scalar * Math.sin(iz_im);
		
		// _2: iz.neg().exp() ...
		scalar = Math.exp(-iz_re);
		_re2 =  scalar * Math.cos(-iz_im);
		_im2 =  scalar * Math.sin(-iz_im);
		
		// _1: _1.add(_2) ...
		_re1 = _re1 + _re2; // !!!
		_im1 = _im1 + _im2; // !!!
		
		// result: _1.scale(0.5) ...
		result =  cart( 0.5 * _re1, 0.5 * _im1 );
		
		return  result;
	
	}
	
	// Returns the tangent of a Complex number.
	public Complex tan () {
		Complex result;
		// tan(z)  =  sin(z) / cos(z)
		
		double scalar;
		double iz_re, iz_im;
		double _re1, _im1;
		double _re2, _im2;
		double _re3, _im3;
		
		double cs_re, cs_im;
		
		// sin() ...
		
		// iz: i.mul(z) ...
		iz_re = -im;
		iz_im = re;
		
		// _1: iz.exp() ...
		scalar = Math.exp(iz_re);
		_re1 = scalar * Math.cos(iz_im);
		_im1 = scalar * Math.sin(iz_im);
		
		// _2: iz.neg().exp() ...
		scalar = Math.exp(-iz_re);
		_re2 = scalar * Math.cos(-iz_im);
		_im2 = scalar * Math.sin(-iz_im);
		
		// _3: _1.sub(_2) ...
		_re3 = _re1 - _re2;
		_im3 = _im1 - _im2;
		
		// result: _3.div(2*i) ...
		result = cart( 0.5*_im3, -0.5*_re3 );
		// result =  cart(_re3, _im3);
		// div(result, 0.0, 2.0);
		
		// cos() ...
		
		// _3: _1.add(_2) ...
		_re3 = _re1 + _re2;
		_im3 = _im1 + _im2;
		
		// cs: _3.scale(0.5) ...
		cs_re = 0.5 * _re3;
		cs_im = 0.5 * _im3;
		
		// result:  result.div(cs) ...
		div(result, cs_re, cs_im);
		
		return  result;
	}
	
	// Returns the cosecant of a Complex number.
	public Complex cosec () {
		Complex result;
		//  cosec(z)  =  1 / sin(z)
		
		double scalar;
		double iz_re, iz_im;
		double _re1, _im1;
		double _re2, _im2;
		
		// iz: i.mul(z) ...
		iz_re = -im;
		iz_im = re;
		
		// _1: iz.exp() ...
		scalar = Math.exp(iz_re);
		_re1 = scalar * Math.cos(iz_im);
		_im1 = scalar * Math.sin(iz_im);
		
		// _2: iz.neg().exp() ...
		scalar = Math.exp(-iz_re);
		_re2 = scalar * Math.cos(-iz_im);
		_im2 = scalar * Math.sin(-iz_im);
		
		// _1: _1.sub(_2) ...
		_re1 = _re1 - _re2; // !!!
		_im1 = _im1 - _im2; // !!!
		
		// _result: _1.div(2*i) ...
		result = cart( 0.5*_im1, -0.5*_re1 );
		// result =  cart(_re1, _im1);
		// div(result, 0.0, 2.0);
		
		// result:  one.div(_result) ...
		inv(result);
		
		return result;
		
	}
	
	// Returns the secant of a Complex number. sec(z)  =  1 / cos(z)
	public Complex sec () {
		Complex result;
		// sec(z)  =  1 / cos(z)
		
		double scalar;
		double iz_re, iz_im;
		double _re1, _im1;
		double _re2, _im2;
		
		// iz: i.mul(z) ...
		iz_re =  -im;
		iz_im =   re;
		
		// _1: iz.exp() ...
		scalar =  Math.exp(iz_re);
		_re1 =  scalar * Math.cos(iz_im);
		_im1 =  scalar * Math.sin(iz_im);
		
		// _2: iz.neg().exp() ...
		scalar =  Math.exp(-iz_re);
		_re2 =  scalar * Math.cos(-iz_im);
		_im2 =  scalar * Math.sin(-iz_im);
		
		// _1: _1.add(_2) ...
		_re1 = _re1 + _re2;
		_im1 = _im1 + _im2;
		
		// result: _1.scale(0.5) ...
		result = cart(0.5*_re1, 0.5*_im1);
		
		// result: one.div(result) ...
		inv(result);
		
		return  result;
		
	}
	
	// Returns the cotangent of a Complex number. cot(z)  =  1 / tan(z)
	public Complex cot () {
		Complex result;
		//  cot(z)  =  1 / tan(z)  =  cos(z) / sin(z)
		
		double scalar;
		double iz_re, iz_im;
		double _re1, _im1;
		double _re2, _im2;
		double _re3, _im3;
		
		double sn_re, sn_im;
		
		// cos() ...
		
		// iz: i.mul(z) ...
		iz_re = -im;
		iz_im = re;
		
		// _1: iz.exp() ...
		scalar =  Math.exp(iz_re);
		_re1 =  scalar * Math.cos(iz_im);
		_im1 =  scalar * Math.sin(iz_im);
		
		// _2: iz.neg().exp() ...
		scalar =  Math.exp(-iz_re);
		_re2 =  scalar * Math.cos(-iz_im);
		_im2 =  scalar * Math.sin(-iz_im);
		
		// _3: _1.add(_2) ...
		_re3 = _re1 + _re2;
		_im3 = _im1 + _im2;
		
		// result: _3.scale(0.5) ...
		result =  cart( 0.5*_re3, 0.5*_im3 );
		
		// sin() ...
		
		// _3: _1.sub(_2) ...
		_re3 = _re1 - _re2;
		_im3 = _im1 - _im2;
		
		// sn: _3.div(2*i) ...
		sn_re = 0.5 * _im3; // !!!
		sn_im = -0.5 * _re3; // !!!
		
		// result:  result.div(sn) ...
		div(result, sn_re, sn_im);
		
		return  result;
		
	}
	
	// Returns the hyperbolic sine of a Complex number. sinh(z)  =  ( exp(z) - exp(-z) ) / 2.
	public Complex sinh () {
		Complex result;
		// sinh(z)  =  ( exp(z) - exp(-z) ) / 2
		
		double scalar;
		double _re1, _im1;
		double _re2, _im2;
		
		// _1: z.exp() ...
		scalar =  Math.exp(re);
		_re1 =  scalar * Math.cos(im);
		_im1 =  scalar * Math.sin(im);
		
		// _2: z.neg().exp() ...
		scalar =  Math.exp(-re);
		_re2 =  scalar * Math.cos(-im);
		_im2 =  scalar * Math.sin(-im);
		
		// _1: _1.sub(_2) ...
		_re1 = _re1 - _re2; // !!!
		_im1 = _im1 - _im2; // !!!
		
		// result:  _1.scale(0.5) ...
		result =  cart( 0.5 * _re1, 0.5 * _im1 );
		
		return  result;
		
	}
	
	// Returns the hyperbolic cosine of a Complex number. cosh(z)  =  ( exp(z) + exp(-z) ) / 2 .
	public Complex cosh () {
		Complex result;
		// cosh(z)  =  ( exp(z) + exp(-z) ) / 2
		
		double scalar;
		double _re1, _im1;
		double _re2, _im2;
		
		// _1: z.exp() ...
		scalar =  Math.exp(re);
		_re1 =  scalar * Math.cos(im);
		_im1 =  scalar * Math.sin(im);
		
		// _2: z.neg().exp() ...
		scalar = Math.exp(-re);
		_re2 = scalar * Math.cos(-im);
		_im2 = scalar * Math.sin(-im);
		
		// _1:  _1.add(_2) ...
		_re1 = _re1 + _re2; // !!!
		_im1 = _im1 + _im2; // !!!
		
		// result:  _1.scale(0.5) ...
		result =  cart( 0.5 * _re1, 0.5 * _im1 );
		
		return  result;
		
	}
	
	// Returns the hyperbolic tangent of a Complex number. tanh(z)  =  sinh(z) / cosh(z).
	public Complex tanh () {
		Complex result;
		// tanh(z)  =  sinh(z) / cosh(z)
		
		double scalar;
		double _re1, _im1;
		double _re2, _im2;
		double _re3, _im3;
		
		double ch_re, ch_im;
		
		// sinh() ...
		
		// _1: z.exp() ...
		scalar =  Math.exp(re);
		_re1 =  scalar * Math.cos(im);
		_im1 =  scalar * Math.sin(im);
		
		// _2: z.neg().exp() ...
		scalar =  Math.exp(-re);
		_re2 =  scalar * Math.cos(-im);
		_im2 =  scalar * Math.sin(-im);
		
		// _3: _1.sub(_2) ...
		_re3 = _re1 - _re2;
		_im3 = _im1 - _im2;
		
		// result:  _3.scale(0.5) ...
		result = cart(0.5*_re3, 0.5*_im3);
		
		// cosh() ...
		
		// _3: _1.add(_2) ...
		_re3 = _re1 + _re2;
		_im3 = _im1 + _im2;
		
		// ch: _3.scale(0.5) ...
		ch_re = 0.5 * _re3;
		ch_im = 0.5 * _im3;
		
		// result:  result.div(ch) ...
		div(result, ch_re, ch_im);
		
		return  result;
		
	}
	
	// Returns the principal arc sine of a Complex number. asin(z)  =  -i * log(i*z + sqrt(1 - z*z))
	public Complex asin () {
		Complex result;
		//  asin(z)  =  -i * log(i*z + sqrt(1 - z*z))
		
		double _re1, _im1;
		
		// _1: one.sub(z.mul(z)) ...
		_re1 =  1.0 - ( (re*re) - (im*im) );
		_im1 =  0.0 - ( (re*im) + (im*re) );
		
		// result:  _1.sqrt() ...
		result =  cart(_re1, _im1);
		sqrt(result);
		
		// _1: z.mul(i) ...
		_re1 = - im;
		_im1 = + re;
		
		// result: _1.add(result) ...
		result.re = _re1 + result.re;
		result.im = _im1 + result.im;
		
		// _1: result.log() ...
		_re1 = Math.log(result.abs());
		_im1 = result.arg();
		
		// result:  i.neg().mul(_1) ...
		result.re = _im1;
		result.im = - _re1;
		
		return  result;
		
	}
	
	// Returns the principal arc cosine of a Complex number. acos(z)  =  -i * log( z + i * sqrt(1 - z*z) )
	public Complex acos () {
		Complex result;
		// acos(z)  =  -i * log( z + i * sqrt(1 - z*z) )
		
		double _re1, _im1;
		
		// _1: one.sub(z.mul(z)) ...
		_re1 =  1.0 - ( (re*re) - (im*im) );
		_im1 =  0.0 - ( (re*im) + (im*re) );
		
		// result: _1.sqrt() ...
		result =  cart(_re1, _im1);
		sqrt(result);
		
		// _1: i.mul(result) ...
		_re1 =  - result.im;
		_im1 =  + result.re;
		
		// result: z.add(_1) ...
		result.re =  re + _re1;
		result.im =  im + _im1;
		
		// _1: result.log()
		_re1 = Math.log(result.abs());
		_im1 = result.arg();
		
		// result: i.neg().mul(_1) ...
		result.re = _im1;
		result.im = - _re1;
		
		return  result;
		
	}
	
	// Returns the principal arc tangent of a Complex number. atan(z)  =  -i/2 * log( (i-z)/(i+z) )
	public Complex atan () {
		Complex result;
		//  atan(z)  =  -i/2 * log( (i-z)/(i+z) )
		
		double _re1, _im1;
		
		// result: i.sub(z) ...
		result =  cart(- re, 1.0 - im);
		
		// _1: i.add(z) ...
		_re1 =  + re;
		_im1 =  1.0 + im;
		
		// result: result.div(_1) ...
		div(result, _re1, _im1);
		
		// _1: result.log() ...
		_re1 =  Math.log(result.abs());
		_im1 =  result.arg();
		
		// result:  half_i.neg().mul(_2) ...
		result.re = 0.5*_im1;
		result.im = -0.5*_re1;
		
		return  result;
		
	}
	
	// Returns the principal inverse hyperbolic sine of a Complex number. asinh(z)  =  log(z + sqrt(z*z + 1))
	
	/*
	 * Many thanks to the mathematicians of aus.mathematics and sci.math, and
	 * to Zdislav V. Kovarik of the Department of Mathematics and Statistics,
	 * McMaster University and John McGowan <jmcgowan@inch.com> in particular,
	 * for their advice on the current naming conventions for "area/argumentus 
	 * sinus hyperbolicus".
	 */
	
	public Complex asinh () {
		Complex result;
		// asinh(z)  =  log(z + sqrt(z*z + 1))
		
		double _re1, _im1;
		
		// _1: z.mul(z).add(one) ...
		_re1 =  ( (re*re) - (im*im) ) + 1.0;
		_im1 =  ( (re*im) + (im*re) ) + 0.0;
		
		// result: _1.sqrt() ...
		result =  cart(_re1, _im1);
		sqrt(result);
		
		// result: z.add(result) ...
		result.re =  re + result.re; // !
		result.im =  im + result.im; // !
		
		// _1: result.log() ...
		_re1 =  Math.log(result.abs());
		_im1 =  result.arg();
		
		// result: _1 ...
		result.re = _re1;
		result.im = _im1;
		
		return  result;
		
	}
	
	// Returns the principal inverse hyperbolic cosine of a Complex number. acosh(z)  =  log(z + sqrt(z*z - 1))
	public Complex acosh () {
		Complex result;
		//  acosh(z)  =  log(z + sqrt(z*z - 1))
		
		double _re1, _im1;
		
		// _1:  z.mul(z).sub(one) ...
		_re1 =  ( (re*re) - (im*im) ) - 1.0;
		_im1 =  ( (re*im) + (im*re) ) - 0.0;
		
		// result: _1.sqrt() ...
		result =  cart(_re1, _im1);
		sqrt(result);
		
		// result: z.add(result) ...
		result.re =  re + result.re; // !
		result.im =  im + result.im; // !
		
		// _1:  result.log() ...
		_re1 =  Math.log(result.abs());
		_im1 =  result.arg();
		
		// result: _1 ...
		result.re =  _re1;
		result.im =  _im1;
		
		return  result;
		
	}
	
	// Returns the principal inverse hyperbolic tangent of a Complex number. atanh(z)  =  1/2 * log( (1+z)/(1-z) )
	public Complex atanh () {
		Complex result;
		//  atanh(z)  =  1/2 * log( (1+z)/(1-z) )
		
		double _re1, _im1;
		
		// result: one.add(z) ...
		result =  cart(1.0 + re, + im);
		
		// _1: one.sub(z) ...
		_re1 =  1.0 - re;
		_im1 =  - im;
		
		// result: result.div(_1) ...
		div(result, _re1, _im1);
		
		// _1: result.log() ...
		_re1 =  Math.log(result.abs());
		_im1 =  result.arg();
		
		// result: _1.scale(0.5) ...
		result.re =  0.5 * _re1;
		result.im =  0.5 * _im1;
		
		return  result;
		
	}
	
	// Converts a Complex into a String of the form (a + bi).
	public String toString () {
		if (im < 0.0) { // ...remembering NaN & Infinity
			return  ("(" + re + " - " + (-im) + "i)");
		} else if (1.0/im == Double.NEGATIVE_INFINITY) {
			return  ("(" + re + " - " + 0.0 + "i)");
		} else {
			return  ("(" + re + " + " + (+im) + "i)");
		}
	}
}
