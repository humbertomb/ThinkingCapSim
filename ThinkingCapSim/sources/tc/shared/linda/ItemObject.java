/* * (c) 2002 Humberto Martinez */ package tc.shared.linda;import java.io.*;import devices.data.*;public class ItemObject extends Item implements Serializable{	public VisionData[]				data;		// Constructors	public ItemObject () 	{		this.set (0);	}			// Instance methods	public void set (VisionData[] data, long tstamp)	{		set (tstamp);				this.data = data;	}	public Item dup ()	{		ItemObject		item = null;		int				i;				try 		{ 			item = (ItemObject) this.clone (); 			for (i = 0; i < data.length; i++)				if (data[i] != null)					item.data[i] = data[i].dup ();		} catch (Exception e) { }				return item;	}}