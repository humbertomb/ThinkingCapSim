/* * (c) 2002 Humberto Martinez */ package tc.vrobot;import java.io.*;public class RobotDataCtrl extends Object implements Serializable{	// Robot sensor subsystems. Enable status	public boolean				sonar		= true;	public boolean				ir			= true;	public boolean				lrf			= true;	public boolean				lsb			= true;		public boolean				vision		= true;		/* Constructors */	public RobotDataCtrl ()	{	}		public void set (RobotDataCtrl other)	{		this.sonar		= other.sonar;		this.ir			= other.ir;		this.lrf		= other.lrf;		this.lsb		= other.lsb;		this.vision		= other.vision;	}			public RobotDataCtrl dup ()	{		RobotDataCtrl		data;				data	= new RobotDataCtrl ();		data.set (this);				return data;	}		public String toString ()	{		String			str;				str		= "DataCtrl=[";				if (sonar)		str += "SONAR ";		if (ir)			str += "IR ";		if (lrf)		str += "LRF ";		if (lsb)		str += "LSB ";		if (vision)		str += "VISION ";		str		+= "]";				return str;	}}