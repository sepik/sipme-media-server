package ua.mobius.media.server.impl.dsp.audio.ilbc;

public class GetCbVecVariables 
{
	protected short tempBuffer[]=new short[45];
	protected int baseSize;
	protected int k;
	
	protected CreateAugmentVectorVariables createAugmentVectorVariables=new CreateAugmentVectorVariables();
	
	public void reset()
	{
		System.arraycopy(CodingFunctions.emptyArray, 0, tempBuffer, 0, 45);
	}
}
