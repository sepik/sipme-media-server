package ua.mobius.media.server.impl.dsp.audio.ilbc;

public class RefinerVariables 
{
	protected short estSegPosRounded,searchSegStartPos,searchSegEndPos,corrDim;
	protected short tLoc,tLoc2,st,en,fraction,max,scale;
	protected int i,maxTemp, scaleFact;
	
	protected short[] filt=new short[7];
	protected int[] corrVecUps=new int[20];
	protected int[] corrVecTemp=new int[5];
	protected short[] vect=new short[86];
	protected short[] corrVec=new short[5];
	
	protected int filterStateIndex,polyIndex;
	
	protected CrossCorrelationVariables crossCorrelationVariables=new CrossCorrelationVariables();
	protected EnhanceUpSampleVariables enhanceUpSampleVariables=new EnhanceUpSampleVariables();
	
	public void reset()
	{
		System.arraycopy(CodingFunctions.emptyArray, 0, filt, 0, 7);
		System.arraycopy(CodingFunctions.emptyArray, 0, vect, 0, 86);
		System.arraycopy(CodingFunctions.emptyArray, 0, corrVec, 0, 5);
		
		System.arraycopy(CodingFunctions.emptyIntArray, 0, corrVecUps, 0, 20);
		System.arraycopy(CodingFunctions.emptyIntArray, 0, corrVecTemp, 0, 5);
	}
}