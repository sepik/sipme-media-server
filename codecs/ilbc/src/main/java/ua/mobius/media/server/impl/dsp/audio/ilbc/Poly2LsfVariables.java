package ua.mobius.media.server.impl.dsp.audio.ilbc;

public class Poly2LsfVariables 
{
	short[] lsp=new short[10];
	
	protected Poly2LspVariables poly2LspVariables=new Poly2LspVariables();
	protected LspToLsfVariables lspToLsfVariables=new LspToLsfVariables();
	
	public void reset()
	{
		System.arraycopy(CodingFunctions.emptyArray, 0, lsp, 0, 10);
	}
}
