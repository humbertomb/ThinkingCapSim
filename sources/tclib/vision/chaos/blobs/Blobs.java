/**
 * Created on 07-oct-2005
 *
 * @author Humberto Martinez Barbera
 */
package tclib.vision.chaos.blobs;

public class Blobs
{
	static public final int		MAX_BLOBS	= 100;
	
	protected int				blobNumber;
	protected Blob[]				blob;

	public Blobs ()
	{
		initialise ();
		blob = new Blob[MAX_BLOBS];		
		for (int i = 0; i < MAX_BLOBS; i++)
			blob[i]	= new Blob ();
	}
	
	public final void	initialise ()		{ blobNumber = -1; }
	public final int		getBlobNumber ()		{ return blobNumber+1; }
	public final Blob	getBlob (int i)		{ return blob[i]; }
	
	public Blob getNewBlob ()
	{
		if (blobNumber < MAX_BLOBS-1)
			blobNumber ++;
		
		return blob[blobNumber];
	}
	
	public void discardBlob ()
	{
		blobNumber --;
		
		if (blobNumber < 0)
			blobNumber = -1;
	}
	
	public void removeBlob (int k)
	{
		blobNumber --;		
		if (blobNumber == 0)		return;
		if (k == blobNumber)		return;
		
		System.arraycopy (blob, k+1, blob, k, blobNumber+1 - k);	
	}

	protected void swap (int i, int j)
	{
		Blob			dummy;
		
		dummy	= blob[i];
		blob[i]	= blob[j];
		blob[j]	= dummy;		
	}
	
	public void sortByArea ()
	{
		int			i, j;
		int			largest;
		
		for (i = 0; i < blobNumber; i++)
		{
			largest = i;
			for (j = i+1; j < blobNumber+1; j++)
				if ((blob[j].getArea () > blob[largest].getArea ()) || (blob[j].valid && !blob[largest].valid))
					largest = j;
			
			if (largest != i)
				swap (i, largest);
		}
	}

	public void sortByPixels ()
	{
		int			i, j;
		int			largest;
		
		for (i = 0; i < blobNumber; i++)
		{
			largest = i;
			for (j = i+1; j < blobNumber+1; j++)
				if ((blob[j].getNumPixels () > blob[largest].getNumPixels()) || (blob[j].valid && !blob[largest].valid))
					largest = j;
			
			if (largest != i)
				swap (i, largest);
		}
	}

	public void prune ()
	{
		int			pruned;
		
		pruned = 0;
		for (int i = blobNumber; i >= 0; i--)
			if (!blob[i].valid)
				pruned ++;
			else
				break;
		
		blobNumber -= pruned;
	}

	public void merge (int gap)
	{
		int			i, j;
		int			num;
		
		// merge blobs
		num = blobNumber+1;		
		for (i = 0; i < num; i++)
		{
			if (!blob[i].valid)			continue;
						
			for (j = i+1; j < num; j++)
			{
				if (!blob[j].valid)		continue;				
				if (blob[i].contains (blob[j]) || blob[j].contains (blob[i]) || blob[i].overlaps (blob[j], gap))
				{
					blob[i].merge (blob[j]);
					blob[j].valid = false;
					j = i+1;
				}
			}
		}
	}
}
