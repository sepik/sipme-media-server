package ua.mobius.media.server.impl.dsp.audio.ilbc;

public class LevinsonDurbinVariables 
{
	protected short[] rHi=new short[21];
	protected short[] rLow=new short[21];
	protected short[] aHi=new short[21];
	protected short[] aLow=new short[21];
	protected short[] aUpdHi=new short[21];
	protected short[] aUpdLow=new short[21];
	
	protected int nBits;
	protected int alphaExp;
	protected int temp,temp2,temp3;
	protected int i,j;
	protected short tempS,tempS2;
	protected short yHi,yLow,xHi,xLow;
    
	protected int currIndex;
	
	public void reset()
	{
		System.arraycopy(CodingFunctions.emptyArray, 0, rHi, 0, 21);
		System.arraycopy(CodingFunctions.emptyArray, 0, rLow, 0, 21);
		System.arraycopy(CodingFunctions.emptyArray, 0, aHi, 0, 21);
		System.arraycopy(CodingFunctions.emptyArray, 0, aLow, 0, 21);
		System.arraycopy(CodingFunctions.emptyArray, 0, aUpdHi, 0, 21);
		System.arraycopy(CodingFunctions.emptyArray, 0, aUpdLow, 0, 21);
	}
}
