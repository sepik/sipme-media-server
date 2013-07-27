package ua.mobius.media.server.impl.dsp.audio.ilbc;

public class LsfInterpolate2PolyEncVariables 
{
	short[] lsfTemp=new short[10];  
	
	protected InterpolateVariables interpolateVariables=new InterpolateVariables();
	protected Lsf2PolyVariables lsf2PolyVariables=new Lsf2PolyVariables();
	public void reset()
	{
		System.arraycopy(CodingFunctions.emptyArray, 0, lsfTemp, 0, 10);
	}
}
