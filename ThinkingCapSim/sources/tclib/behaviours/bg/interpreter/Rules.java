/* * (c) 1997-2001 Humberto Martinez */ package tclib.behaviours.bg.interpreter;public class Rules extends Object{	protected Rule[]			rs;	protected int				max;	private int				n;	/* Constructors */	private Rules ()	{	}		public Rules (int max)	{		this.create (max);	}		/* Accessor methods */	public final int	 	n () 					{ return n; }	/* Instance methods */	private void create (int max)	{		this.max		= max;		this.n		= 0;		this.rs		= new Rule [max];	}		public void add (Rule r)	{		if (n >= max) return;				rs[n] = r;		n++;	}		public Rule at (int i)	{		if ((i < 0) || (i >= max)) return null;		return rs[i];	}		protected void at (int i, Rule r)	{		if ((i < 0) || (i >= max)) return;		rs[i] = r;	}		/* ----------------------------------------------------------- */	/* Apply a set of MISO fuzzy rules. The rules are applied to	 * an unique output.	 */	public void inference ()	{		int			i, j, k;		int			vs;		boolean		modi;		double		sum, mom;			if (n == 0) return;			/* Check how many different output variables we have */		vs = 1;			rs[0].hash (0);	    	for (i = 1; i < n; i++)	   	{	    		modi = false;			for (j = 0; j < i; j++)				if (rs[i].sto () == rs[j].sto ())				{					rs[i].hash (rs[j].hash ());					modi = true;				}			if (!modi)			{				rs[i].hash (vs);				vs ++;			}		}			/* Apply defuzzification to each different output variable */    		k = 0;		for (i = 0; i < vs; i++)    		{    			sum = 0.0;    			mom = 0.0;    			for (j = 0; j < n; j++)    			{    				if (rs[j].hash () == i)    				{    					k = j;     					mom += rs[j].output ().moment ();    					sum += rs[j].output ().integral ();    				}    			}    			if (sum == 0.0)    				rs[k].sto ().value (0.0);    	// No rule applies     			else 	    				rs[k].sto ().value (mom / sum);    	    		}	}}