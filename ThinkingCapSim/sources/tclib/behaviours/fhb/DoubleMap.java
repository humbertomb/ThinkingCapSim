/*
 * @(#)DoubleMap.java		1.0 2003/12/07
 * 
 * (c) 2003 Denis Remondini
 *
 */
package tclib.behaviours.fhb;

import tclib.behaviours.fhb.exceptions.ElementNotFound;

import java.text.DecimalFormat;
import java.io.Serializable;


/**
 * An object that maps keys to double variables.
 * A DoubleMap object cannot contain duplicate keys; each key can map to 
 * at most one double value. 
 * 
 * @version	1.0		07 Dic 2003
 * @author 	Denis Remondini
 */
public class DoubleMap implements Serializable {
	
	/* Array with the keys stored in the map */
	private String[] keys;
	/* Array with the values stored in the map */
	private double[] values;
	/* The capacity of the map */
	private int capacity;
	/* The number of empty places that are added to the map when it is resized */
	private int capacityIncrement;
	/* The number of values stored in the map */
	private int size;
	
	
	/**
	 * Constructs an empty DoubleMap with the default initial capacity (10) and
	 * the default load factor (0.75).
	 * 
	 */
	public DoubleMap() {
		this(10,0.75);
	}
	
	/**
	 * Constructs an empty DoubleMap with the specified initial capacity and
	 * the default load factor (0.75).
	 * 
	 * @param initialCapacity The initial capacity
	 */
	public DoubleMap(int initialCapacity) {
		this(initialCapacity,0.75);
	}
	
	/**
	 * Constructs an empty DoubleMap with the specified initial capacity and
	 * load factor
	 * @param initialCapacity The initial capacity
	 * @param loadFactor The load factor
	 */
	public DoubleMap(int initialCapacity, double loadFactor) {
		if (initialCapacity == 0)
			initialCapacity = 1;
		keys = new String[initialCapacity];
		values = new double[initialCapacity];
		capacity = initialCapacity;
		capacityIncrement = ((int) (loadFactor * capacity)) + 1;
		size = 0;
	}
	
	/*
	 * Finds a key in the map. Return -1 if the key is not in the map.
	 */
	private int find(String key) {
		int pos = -1;
		
		for (int i = 0; i < size; i++)
			if (keys[i].equals(key)) {
				pos = i;
				break;
			}
			
		return pos;
	}
	
	
	/*
	 * Resizes an array.
	 */
	private static Object resizeArray (Object oldArray, int newSize) {
		int oldSize = java.lang.reflect.Array.getLength(oldArray);
		Class elementType = oldArray.getClass().getComponentType();
		
		/* Creates a new array with the same element type but with the new size */
		Object newArray = java.lang.reflect.Array.newInstance(
				elementType,newSize);
		
		/* Calcutates how many elements have to be copied into the new array */
		int preserveLength = Math.min(oldSize,newSize);
		if (preserveLength > 0)
			System.arraycopy (oldArray,0,newArray,0,preserveLength);
		
		return newArray; 
	}

	/**
	 * Associates the specified value with the specified key in the map. 
	 * If the map previously contained a mapping for this key, the old value 
	 * is replaced.
	 * @param key key with which the specified value is to be associated
	 * @param value value to be associated with the specified key
	 */
	public void setValue(String key, double value) {
//		key = key.toLowerCase();
		int keyPos = find(key);
		
		if (keyPos == -1) {
			/* the key is not already in the map */
			if (size == capacity) {
				/* the map is full and has to be resized */
				keys = (String[]) resizeArray(keys,size + capacityIncrement);
				values = (double[]) resizeArray(values,size + capacityIncrement);
				/* update the capacity value */
				capacity = size + capacityIncrement;
			}	
			
			/* 
			 * Insert the new key with the associated value and update the size of
			 * the map
			 */
			keys[size] = key;
			values[size++] = value;
		}
		else {
			/* update the value associated with the existing key */
			values[keyPos] = value;
		}
	}
	
	/**
	 * Returns the value to which the specified key is mapped in this map.
	 * @param key the key whose associated value is to be returned
	 * @return the value to which this map maps the specified key.
	 * @throws ElementNotFound if there is no value associated with the specified key
	 */
	public double getValue(String key) throws ElementNotFound {
//		key = key.toLowerCase();
		int keyPos = find(key);

		if (keyPos == -1)
			throw new ElementNotFound(key);
		else {
			return values[keyPos];
		}
	}
	
	/**
	 * Returns an array of the keys contained in the map
	 * @return array of the keys contained in the map
	 */
	public String[] getAntecedentKeys() {
		return keys;
	}
	
	/**
	 * Returns the size of the map
	 * @return the size of the map
	 */
	public int getSize() {
		return size;
	}
	
	public String toString() {
		String res = "";
		
		DecimalFormat fmt = new DecimalFormat();
		fmt.setMaximumFractionDigits(2);
		for (int i = 0; i < size; i++) {
			res = res + "" + keys[i]+" "+fmt.format(values[i])+" ";
		}
		res = res + "";
		return res;
	}
			
}
