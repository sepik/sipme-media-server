package ua.mobius.media.server.impl.dsp.audio.ilbc;

public class DecodeInterpolateLsfVariables 
{
	protected short[] lp=new short[11];     	    
	protected int len;
	protected short s;
	
	protected LspInterpolate2PolyDecVariables lspInterpolate2PolyDecVariables=new LspInterpolate2PolyDecVariables();
	
	public void reset()
	{
		System.arraycopy(CodingFunctions.emptyArray, 0, lp, 0, 11);
	}
}
