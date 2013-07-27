package ua.mobius.media.server.impl.dsp.audio.ilbc;

public class UnpackBitsVariables 
{
	protected short tempIndex1=0;
	protected short tempIndex2=0;
	protected short tempS;
	protected int i;
	protected int k;
	
	protected short[] lsf;
	protected short[] cbIndex;
	protected short[] gainIndex;
	protected short[] idxVec;
	
	public void reset()
	{
		tempIndex1=0;
		tempIndex2=0;
	}
}