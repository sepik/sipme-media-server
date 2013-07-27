package ua.mobius.media.server.impl.dsp.audio.ilbc;

public class CreateAugmentVectorVariables 
{
	protected short[] cbVecTmp=new short[4];    	
	protected int currIndex;
	
	public void reset()
	{
		System.arraycopy(CodingFunctions.emptyArray, 0, cbVecTmp, 0, 4);
	}
}
