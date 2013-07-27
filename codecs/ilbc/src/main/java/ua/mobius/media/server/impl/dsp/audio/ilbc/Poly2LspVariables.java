package ua.mobius.media.server.impl.dsp.audio.ilbc;

public class Poly2LspVariables 
{
	protected short[] f1=new short[6]; 
	protected short[] f2=new short[6];
	
	protected short xMid,xLow,xHi,yMid,yLow,yHi,x,y;
	protected int temp;
	protected int i,j;
	protected int nBits;
	
	protected int aLowIndex;
	protected int aHighIndex;
	
	protected short[] current;
	protected int currIndex;
	protected int foundFreqs;
	
	protected ChebushevVariables chebushevVariables=new ChebushevVariables();
	
	public void reset()
	{
		System.arraycopy(CodingFunctions.emptyArray, 0, f1, 0, 6);
		System.arraycopy(CodingFunctions.emptyArray, 0, f2, 0, 6);
	}
}
