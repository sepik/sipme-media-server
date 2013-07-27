package ua.mobius.media.server.impl.dsp.audio.ilbc;

public class StateConstructVariables 
{
	protected short[] numerator=new short[11];
	protected short[] sampleValVec=new short[126];
	protected short[] sampleMaVec=new short[126];
	protected short[] sampleVal=sampleValVec;
	protected short[] sampleMa=sampleMaVec;
	protected short[] sampleAr=sampleValVec;    	  
	
	protected short[] idxVec;
	
	protected int coef,bitShift,currIndex,currIndex2;
	protected int k;
	protected int max;
	
	public void reset()
	{
		System.arraycopy(CodingFunctions.emptyArray, 0, numerator, 0, 11);
		System.arraycopy(CodingFunctions.emptyArray, 0, sampleValVec, 0, 126);
		System.arraycopy(CodingFunctions.emptyArray, 0, sampleMaVec, 0, 126);
	}
}