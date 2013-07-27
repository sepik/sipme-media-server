package ua.mobius.media.server.impl.dsp.audio.ilbc;

public class SimpleInterpolateLsfVariables 
{
	protected short[] lsfOld;
	protected short[] lsfDeqOld;
	
	protected short[] lp=new short[11];
	
	protected int step;    	
	protected int index;
	protected int i;
	
	protected LsfInterpolate2PolyEncVariables lsfInterpolate2PolyEncVariables=new LsfInterpolate2PolyEncVariables();
	
	public void reset()
	{
		System.arraycopy(CodingFunctions.emptyArray, 0, lp, 0, 11);
		index=0;
	}
}
