package ua.mobius.media.server.impl.dsp.audio.ilbc;

public class NearestNeighborVariables 
{
	int i;
	short diff;
	int[] crit=new int[8];
	
	public void reset()
	{
		System.arraycopy(CodingFunctions.emptyIntArray, 0, crit, 0, 8);
	}
}
