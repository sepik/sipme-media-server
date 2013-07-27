package ua.mobius.media.server.impl.dsp.audio.ilbc;

public class LpcEncodeVariables 
{
	short[] lsf=new short[20];
    short[] lsfDeq=new short[20];
    
    protected SimpleLpcAnalysisVariables simpleLpcAnalysisVariables=new SimpleLpcAnalysisVariables();
    protected SimpleLsfQVariables simpleLsfQVariables=new SimpleLsfQVariables();
    protected LsfCheckVariables lsfCheckVariables=new LsfCheckVariables();
    protected SimpleInterpolateLsfVariables simpleInterpolateLsfVariables=new SimpleInterpolateLsfVariables();
    
    public void reset()
    {
    	System.arraycopy(CodingFunctions.emptyArray, 0, lsf, 0, 20);
    	System.arraycopy(CodingFunctions.emptyArray, 0, lsfDeq, 0, 20);
    }
}
