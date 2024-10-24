package wucore.utils.color;

import java.io.*;

public class WColor implements Serializable 
{	
	// The color white.  In the default sRGB space.
	public final static WColor white = new WColor(255, 255, 255);
	public final static WColor WHITE = white;
	
	// The color light gray.  In the default sRGB space.
	public final static WColor lightGray = new WColor(192, 192, 192);
	public final static WColor LIGHT_GRAY = lightGray;
	
	// The color gray.  In the default sRGB space.
	public final static WColor gray = new WColor(128, 128, 128);
	public final static WColor GRAY = gray;
	
	// The color dark gray.  In the default sRGB space.
	public final static WColor darkGray = new WColor(64, 64, 64);
	public final static WColor DARK_GRAY = darkGray;
	
	// The color black.  In the default sRGB space.
	public final static WColor black = new WColor(0, 0, 0);
	public final static WColor BLACK = black;
	
	// The color red.  In the default sRGB space.
	public final static WColor red = new WColor(255, 0, 0);
	public final static WColor RED = red;
	
	// The color pink.  In the default sRGB space.
	public final static WColor pink = new WColor(255, 175, 175);
	public final static WColor PINK = pink;
	
	// The color orange.  In the default sRGB space.
	public final static WColor orange = new WColor(255, 200, 0);
	public final static WColor ORANGE = orange;
	
	// The color yellow.  In the default sRGB space.
	public final static WColor yellow = new WColor(255, 255, 0);
	public final static WColor YELLOW = yellow;
	
	// The color green.  In the default sRGB space.
	public final static WColor green = new WColor(0, 255, 0);
	public final static WColor GREEN = green;
	
	// The color magenta.  In the default sRGB space.
	public final static WColor magenta   = new WColor(255, 0, 255);
	public final static WColor MAGENTA = magenta;
	
	// The color cyan.  In the default sRGB space.
	public final static WColor cyan = new WColor(0, 255, 255);
	public final static WColor CYAN = cyan;
	
	// The color blue.  In the default sRGB space.
	public final static WColor blue = new WColor(0, 0, 255);
	public final static WColor BLUE = blue;
	
	// The color value.
	int value;
	
	// The color value in the default sRGB ColorSpace as float components (no alpha).
	// If null after object construction, this must be an sRGB color constructed with
	// 8-bit precision, so compute from the int color value.
	private float frgbvalue[] = null;
	
	// The color value in the native ColorSpace as float components (no alpha).
	// If null after object construction, this must be an sRGB color constructed with
	// 8-bit precision, so compute from the int color value.
	private float fvalue[] = null;
	
	// The alpha value as a float component.
	private float falpha = 0.0f;
	
	// JDK 1.1 serialVersionUID
	private static final long serialVersionUID = 118526816881161077L;
	
	// Creates an opaque sRGB color with the specified red, green, and blue values
	// in the range (0 - 255). The actual color used in rendering depends on finding
	// the best match given the color space available for a given output device.
	// Alpha is defaulted to 255.
	public WColor(int r, int g, int b) {
		this(r, g, b, 255);
	}
	
	// Creates an sRGB color with the specified red, green, blue, and alpha values in
	// the range (0 - 255).
	public WColor(int r, int g, int b, int a) {
		value =	((a & 0xFF) << 24) |
				((r & 0xFF) << 16) |
				((g & 0xFF) << 8)  |
				((b & 0xFF) << 0);
		
		testColorValueRange(r,g,b,a);
	}
	
	// Creates an opaque sRGB color with the specified combined RGB value consisting of
	// the red component in bits 16-23, the green component in bits 8-15, and the blue
	// component in bits 0-7. The actual color used in rendering depends on finding the
	// best match given the color space available for a particular output device.
	// Alpha is defaulted to 255.
	public WColor(int rgb) {
		value = 0xff000000 | rgb;
	}
	
	// Creates an sRGB color with the specified combined RGBA value consisting of the
	// alpha component in bits 24-31, the red component in bits 16-23, the green component
	// in bits 8-15, and the blue component in bits 0-7.
	// If the hasalpha argument is false, alpha is defaulted to 255.
	public WColor(int rgba, boolean hasalpha) {
		if (hasalpha) {
			value = rgba;
		} else {
			value = 0xff000000 | rgba;
		}
	}
	
	// Creates an opaque sRGB color with the specified red, green, and blue values in
	// the range (0.0 - 1.0).  Alpha is defaulted to 1.0. The actual color used in
	// rendering depends on finding the best match given the color space available
	// for a particular output device.
	public WColor(float r, float g, float b) {
		this( (int) (r*255+0.5), (int) (g*255+0.5), (int) (b*255+0.5));
		testColorValueRange(r,g,b,1.0f);
		frgbvalue = new float[3];
		frgbvalue[0] = r;
		frgbvalue[1] = g;
		frgbvalue[2] = b;
		falpha = 1.0f;
		fvalue = frgbvalue;
	}
	
	// Creates an sRGB color with the specified red, green, blue, and alpha values in
	// the range (0.0 - 1.0). The actual color used in rendering depends on finding the
	// best match given the color space available for a particular output device.
	public WColor(float r, float g, float b, float a) {
		this((int)(r*255+0.5), (int)(g*255+0.5), (int)(b*255+0.5), (int)(a*255+0.5));
		frgbvalue = new float[3];
		frgbvalue[0] = r;
		frgbvalue[1] = g;
		frgbvalue[2] = b;
		falpha = a;
		fvalue = frgbvalue;
	}
	
	// Checks the color integer components supplied for validity.
	private static void testColorValueRange(int r, int g, int b, int a) {
		boolean rangeError = false;
		String badComponentString = "";
		
		if ( a < 0 || a > 255) {
			rangeError = true;
			badComponentString = badComponentString + " Alpha";
		}
		
		if ( r < 0 || r > 255) {
			rangeError = true;
			badComponentString = badComponentString + " Red";
		}
		
		if ( g < 0 || g > 255) {
			rangeError = true;
			badComponentString = badComponentString + " Green";
		}
		
		if ( b < 0 || b > 255) {
			rangeError = true;
			badComponentString = badComponentString + " Blue";
		}
		
		if ( rangeError == true ) {
			throw new IllegalArgumentException("Color parameter outside of expected range:" + badComponentString);
		}
	}
	
	// Checks the color float components supplied for validity.
	private static void testColorValueRange(float r, float g, float b, float a) {
		boolean rangeError = false;
		String badComponentString = "";
		
		if ( a < 0.0 || a > 1.0) {
			rangeError = true;
			badComponentString = badComponentString + " Alpha";
		}
		
		if ( r < 0.0 || r > 1.0) {
			rangeError = true;
			badComponentString = badComponentString + " Red";
		}
		
		if ( g < 0.0 || g > 1.0) {
			rangeError = true;
			badComponentString = badComponentString + " Green";
		}
		
		if ( b < 0.0 || b > 1.0) {
			rangeError = true;
			badComponentString = badComponentString + " Blue";
		}
		
		if ( rangeError == true ) {
			throw new IllegalArgumentException("Color parameter outside of expected range:" + badComponentString);
		}
	}
	
	// Returns the red component in the range 0-255 in the default sRGB space.
	public int getRed() {
		return (getRGB() >> 16) & 0xFF;
	}
	
	// Returns the green component in the range 0-255 in the default sRGB space.
	public int getGreen() {
		return (getRGB() >> 8) & 0xFF;
	}
	
	// Returns the blue component in the range 0-255 in the default sRGB space.
	public int getBlue() {
		return (getRGB() >> 0) & 0xFF;
	}
	
	// Returns the alpha component in the range 0-255.
	public int getAlpha() {
		return (getRGB() >> 24) & 0xff;
	}
	
	// Returns the RGB value representing the color in the default sRGB ColorModel.
	public int getRGB() {
		return value;
	}
	
	// Creates a new Color that is a brighter version of this Color.
	// This method applies an arbitrary scale factor to each of the three RGB components
	// of this Color to create a brighter version of this Color. Although brighter and
	// darker are inverse operations, the results of a series of invocations of these two
	// methods might be inconsistent because of rounding errors.
	private static final double FACTOR = 0.7;
	public WColor brighter() {
		int r = getRed();
		int g = getGreen();
		int b = getBlue();
		
		// From 2D group:
		//   1. black.brighter() should return grey
		//   2. applying brighter to blue will always return blue, brighter
		//   3. non pure color (non zero rgb) will eventually return white
		int i = (int)(1.0/(1.0-FACTOR));
		if ( r == 0 && g == 0 && b == 0) {
			return new WColor(i, i, i);
		}
		
		if ( r > 0 && r < i ) r = i;
		if ( g > 0 && g < i ) g = i;
		if ( b > 0 && b < i ) b = i;
		
		return new WColor(Math.min((int)(r/FACTOR), 255), Math.min((int)(g/FACTOR), 255), Math.min((int)(b/FACTOR), 255));
	}
	
	// Creates a new Color that is a darker version of this Color.
	// This method applies an arbitrary scale factor to each of the three RGB components
	// of this Color to create a darker version of this Color. Although brighter and
	// darker are inverse operations, the results of a series of invocations of these two
	// methods might be inconsistent because of rounding errors.
	public WColor darker() {
		return new WColor(Math.max((int)(getRed()  *FACTOR), 0), Math.max((int)(getGreen()*FACTOR), 0), Math.max((int)(getBlue() *FACTOR), 0));
	}
	
	// Computes the hash code for this Color.
	public int hashCode() {
		return value;
	}
	
	// Determines whether another object is equal to this Color.
	// The result is true if and only if the argument is not null and is a Color object
	// that has the same red, green, blue, and alpha values as this object.
	public boolean equals(Object obj) {
		return obj instanceof WColor && ((WColor)obj).value == this.value;
	}
	
	// Returns a string representation of this Color. This method is intended to be used
	// only for debugging purposes. The content and format of the returned string might
	// vary between implementations. The returned string might be empty but cannot be null.
	public String toString() {
		return getClass().getName() + "[r=" + getRed() + ",g=" + getGreen() + ",b=" + getBlue() + "]";
	}
	
	// Converts a String to an integer and returns the specified opaque Color. This method
	// handles string formats that are used to represent octal and hexadecimal numbers.
	public static WColor decode(String nm) throws NumberFormatException {
		Integer intval = Integer.decode(nm);
		int i = intval.intValue();
		return new WColor((i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF);
	}
	
	// Finds a color in the system properties.
	// The argument is treated as the name of a system property to be obtained. The string
	// value of this property is then interpreted as an integer which is then converted to
	// a Color object.
	public static WColor getColor(String nm) {
		return getColor(nm, null);
	}
	
	// Finds a color in the system properties.
	// The first argument is treated as the name of a system property to be obtained. The
	// string value of this property is then interpreted as an integer which is then
	// converted to a Color object.
	// If the specified property is not found or cannot be parsed as an integer then the
	// Color specified by the second.
	public static WColor getColor(String nm, WColor v) {
		Integer intval = Integer.getInteger(nm);
		if (intval == null) {
			return v;
		}
		int i = intval.intValue();
		return new WColor((i >> 16) & 0xFF, (i >> 8) & 0xFF, i & 0xFF);
	}
	
	// Finds a color in the system properties.
	// The first argument is treated as the name of a system property to be obtained. The
	// string value of this property is then interpreted as an integer which is then
	// converted to a Color object.
	// If the specified property is not found or could not be parsed as an integer then
	// the integer value <code>v</code> is used instead, and is converted to a Color object.
	public static WColor getColor(String nm, int v) {
		Integer intval = Integer.getInteger(nm);
		int i = (intval != null) ? intval.intValue() : v;
		return new WColor((i >> 16) & 0xFF, (i >> 8) & 0xFF, (i >> 0) & 0xFF);
	}
	
	// Converts the components of a color, as specified by the HSB model, to an equivalent
	// set of values for the default RGB model.
	// The saturation and brightness components should be floating-point values between
	// zero and one (numbers in the range 0.0-1.0). The hue component can be any
	// floating-point number. The floor of this number is subtracted from it to create a
	// fraction between 0 and 1. This fractional number is then multiplied by 360 to
	// produce the hue angle in the HSB color model.
	// The integer that is returned by HSBtoRGB encodes the value of a color in bits 0-23
	// of an integer value that is the same format used by the method getRGB.
	// This integer can be supplied as an argument to the Color constructor that takes a
	// single integer argument.
	public static int HSBtoRGB(float hue, float saturation, float brightness) {
		int r = 0, g = 0, b = 0;
		if (saturation == 0) {
			r = g = b = (int) (brightness * 255.0f + 0.5f);
		} else {
			float h = (hue - (float)Math.floor(hue)) * 6.0f;
			float f = h - (float)java.lang.Math.floor(h);
			float p = brightness * (1.0f - saturation);
			float q = brightness * (1.0f - saturation * f);
			float t = brightness * (1.0f - (saturation * (1.0f - f)));
			switch ((int) h) {
				case 0:
					r = (int) (brightness * 255.0f + 0.5f);
					g = (int) (t * 255.0f + 0.5f);
					b = (int) (p * 255.0f + 0.5f);
					break;
				case 1:
					r = (int) (q * 255.0f + 0.5f);
					g = (int) (brightness * 255.0f + 0.5f);
					b = (int) (p * 255.0f + 0.5f);
					break;
				case 2:
					r = (int) (p * 255.0f + 0.5f);
					g = (int) (brightness * 255.0f + 0.5f);
					b = (int) (t * 255.0f + 0.5f);
					break;
				case 3:
					r = (int) (p * 255.0f + 0.5f);
					g = (int) (q * 255.0f + 0.5f);
					b = (int) (brightness * 255.0f + 0.5f);
					break;
				case 4:
					r = (int) (t * 255.0f + 0.5f);
					g = (int) (p * 255.0f + 0.5f);
					b = (int) (brightness * 255.0f + 0.5f);
					break;
				case 5:
					r = (int) (brightness * 255.0f + 0.5f);
					g = (int) (p * 255.0f + 0.5f);
					b = (int) (q * 255.0f + 0.5f);
					break;
			}
		}
		
		return 0xff000000 | (r << 16) | (g << 8) | (b << 0);
	}
	
	// Converts the components of a color, as specified by the default RGB model, to an
	// equivalent set of values for hue, saturation, and brightness that are the three
	// components of the HSB model.
	// If the <code>hsbvals</code> argument is null, then a new array is allocated to
	// return the result. Otherwise, the method returns the array hsbvals, with the values
	// put into that array.
	public static float[] RGBtoHSB(int r, int g, int b, float[] hsbvals) {
		float hue, saturation, brightness;
		
		if (hsbvals == null) {
			hsbvals = new float[3];
		}
		
		int cmax = (r > g) ? r : g;
		if (b > cmax) cmax = b;
		int cmin = (r < g) ? r : g;
		if (b < cmin) cmin = b;
		
		brightness = ((float) cmax) / 255.0f;
		
		if (cmax != 0)
			saturation = ((float) (cmax - cmin)) / ((float) cmax);
		else
			saturation = 0;
		
		if (saturation == 0)
			hue = 0;
		else {
			float redc = ((float) (cmax - r)) / ((float) (cmax - cmin));
			float greenc = ((float) (cmax - g)) / ((float) (cmax - cmin));
			float bluec = ((float) (cmax - b)) / ((float) (cmax - cmin));
			if (r == cmax)
				hue = bluec - greenc;
			else if (g == cmax)
				hue = 2.0f + redc - bluec;
			else
				hue = 4.0f + greenc - redc;
			
			hue = hue / 6.0f;
			if (hue < 0)
				hue = hue + 1.0f;
		}
		hsbvals[0] = hue;
		hsbvals[1] = saturation;
		hsbvals[2] = brightness;
		
		return hsbvals;
	}
	
	// Creates a <code>Color</code> object based on the specified values for the HSB
	// color model.
	// The s and b components should be floating-point values between zero and one
	// (numbers in the range 0.0-1.0). The h component can be any floating-point number.
	// The floor of this number is subtracted from it to create a fraction between 0 and
	// 1. This fractional number is then multiplied by 360 to produce the hue angle in
	// the HSB color model.
	public static WColor getHSBColor(float h, float s, float b) {
		return new WColor(HSBtoRGB(h, s, b));
	}
	
	// Returns a float array containing the color and alpha
	// components of the Color, as represented in the default sRGB color space.
	// If compArray is null, an array of length 4 is created for the return value.
	// Otherwise, compArray must have length 4 or greater, and it is filled in with the
	// components and returned.
	public float[] getRGBComponents(float[] compArray) {
		float[] f;
		
		if (compArray == null) {
			f = new float[4];
		} else {
			f = compArray;
		}
		
		if (frgbvalue == null) {
			f[0] = ((float)getRed())/255f;
			f[1] = ((float)getGreen())/255f;
			f[2] = ((float)getBlue())/255f;
			f[3] = ((float)getAlpha())/255f;
		} else {
			f[0] = frgbvalue[0];
			f[1] = frgbvalue[1];
			f[2] = frgbvalue[2];
			f[3] = falpha;
		}
		
		return f;
	}
	
	// Returns a float array containing only the color components of the Color, in the
	// default sRGB color space. If compArray is null an array of length 3 is created
	// for the return value. Otherwise, compArray must have length 3 or greater, and it
	// is filled in with the components and returned.
	public float[] getRGBColorComponents(float[] compArray) {
		float[] f;
		
		if (compArray == null) {
			f = new float[3];
		} else {
			f = compArray;
		}
		
		if (frgbvalue == null) {
			f[0] = ((float)getRed())/255f;
			f[1] = ((float)getGreen())/255f;
			f[2] = ((float)getBlue())/255f;
		} else {
			f[0] = frgbvalue[0];
			f[1] = frgbvalue[1];
			f[2] = frgbvalue[2];
		}
		
		return f;
	}
	
	// Returns a <code>float</code> array containing the color and alpha components of
	// the Color, in the ColorSpace of the Color. If compArray is null, an array with
	// length equal to the number of components in the associated ColorSpace plus one is
	// created for the return value. Otherwise, compArray must have at least this length
	// and it is filled in with the components and returned.
	public float[] getComponents(float[] compArray) {
		if (fvalue == null)
			return getRGBComponents(compArray);
		
		float[] f;
		int n = fvalue.length;
		if (compArray == null) {
			f = new float[n + 1];
		} else {
			f = compArray;
		}
		
		for (int i = 0; i < n; i++) {
			f[i] = fvalue[i];
		}
		
		f[n] = falpha;
		
		return f;
	
	}
	
	// Returns a float array containing only the color components of the Color, in the
	// ColorSpace of the Color. If compArray is null, an array with length equal to the
	// number of components in the associated ColorSpace is created for the return value.
	// Otherwise, compArray must have at least this length and it is filled in with the
	// components and returned.
	public float[] getColorComponents(float[] compArray) {
		if (fvalue == null)
			return getRGBColorComponents(compArray);
		
		float[] f;
		int n = fvalue.length;
		if (compArray == null) {
			f = new float[n];
		} else {
			f = compArray;
		}
		
		for (int i = 0; i < n; i++) {
			f[i] = fvalue[i];
		}
		
		return f;
	}
}
