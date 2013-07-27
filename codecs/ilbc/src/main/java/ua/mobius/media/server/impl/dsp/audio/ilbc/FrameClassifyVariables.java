package ua.mobius.media.server.impl.dsp.audio.ilbc;

public class FrameClassifyVariables 
{
	protected int[] ssqEn=new int[5];
	
	protected short max,tempS;
	protected int n;
	protected short scale;
	protected int ssqIndex;
	protected int currIndex;
	
	public void reset()
	{
		System.arraycopy(CodingFunctions.emptyIntArray, 0, ssqEn, 0, 5);
	}
}