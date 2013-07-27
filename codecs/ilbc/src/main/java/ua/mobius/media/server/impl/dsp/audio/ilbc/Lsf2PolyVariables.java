package ua.mobius.media.server.impl.dsp.audio.ilbc;

public class Lsf2PolyVariables 
{
	protected int k;
	
	protected int[] f1=new int[6];
	protected int[] f2=new int[6];
	
	protected short[] lsp=new short[10];
	
	protected Lsf2LspVariables lsf2LspVariables=new Lsf2LspVariables();
	protected GetLspPolyVariables getLspPolyVariables=new GetLspPolyVariables();
	public void reset()
	{
		System.arraycopy(CodingFunctions.emptyArray, 0, lsp, 0, 10);
		System.arraycopy(CodingFunctions.emptyIntArray, 0, f1, 0, 6);
		System.arraycopy(CodingFunctions.emptyIntArray, 0, f2, 0, 6);
	}
}
