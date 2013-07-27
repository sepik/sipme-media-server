package ua.mobius.media.server.impl.dsp.audio.ilbc;

public class CbConstructVariables 
{
	protected short[] cbIndex;
	protected short[] gainIndex;
	
	protected short gain[]=new short[3];
	protected short cbVec0[]=new short[40];
	protected short cbVec1[]=new short[40];
	protected short cbVec2[]=new short[40];
	
	protected int i;
	
	protected GetCbVecVariables getCbVecVariables=new GetCbVecVariables();
	
	public void reset()
	{
		System.arraycopy(CodingFunctions.emptyArray, 0, gain, 0, 3);
		System.arraycopy(CodingFunctions.emptyArray, 0, cbVec0, 0, 40);
		System.arraycopy(CodingFunctions.emptyArray, 0, cbVec1, 0, 40);
		System.arraycopy(CodingFunctions.emptyArray, 0, cbVec2, 0, 40);
	}
}
