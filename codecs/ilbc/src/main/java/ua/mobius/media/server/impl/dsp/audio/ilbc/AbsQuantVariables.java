package ua.mobius.media.server.impl.dsp.audio.ilbc;

public class AbsQuantVariables 
{
	protected short[] quantLen=new short[2];
	protected short[] syntOutBuf=new short[68];
	protected short[] inWeightedVec=new short[68];
	protected short[] inWeighted=inWeightedVec;    	

	protected AbsQuantLoopVariables absQuantLoopVariables=new AbsQuantLoopVariables();
	
	public void reset()
	{
		System.arraycopy(CodingFunctions.emptyArray, 0, quantLen, 0, 2);
		System.arraycopy(CodingFunctions.emptyArray, 0, syntOutBuf, 0, 68);    	
    	System.arraycopy(CodingFunctions.emptyArray, 0, inWeightedVec, 0, 68);    	    
	}
}
