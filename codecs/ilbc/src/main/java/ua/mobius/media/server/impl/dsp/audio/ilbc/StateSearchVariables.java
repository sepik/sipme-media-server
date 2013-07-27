package ua.mobius.media.server.impl.dsp.audio.ilbc;

public class StateSearchVariables 
{
	protected short[] numerator=new short[11];
	protected short[] residualLongVec=new short[126];    	  
    protected short[] sampleMa=new short[116];
	protected short[] residualLong=residualLongVec; 
	protected short[] sampleAr=residualLongVec;

	protected int nBits,n,currIndex,temp;
	protected short max,tempS,tempS2;
	
	protected AbsQuantVariables absQuantVariables=new AbsQuantVariables();
	
	public void reset()
	{
		System.arraycopy(CodingFunctions.emptyArray, 0, numerator, 0, 11);
		System.arraycopy(CodingFunctions.emptyArray, 0, residualLongVec, 0, 126);
		System.arraycopy(CodingFunctions.emptyArray, 0, sampleMa, 0, 116);
	}
}
