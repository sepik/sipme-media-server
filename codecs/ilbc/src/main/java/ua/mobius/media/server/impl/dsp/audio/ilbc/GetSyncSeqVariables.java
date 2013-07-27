package ua.mobius.media.server.impl.dsp.audio.ilbc;

public class GetSyncSeqVariables 
{
	protected int i,centerEndPos,q;
	protected short[] lagBlock=new short[7];
	protected short[] blockStartPos=new short[7];
	protected short[] plocs2=new short[8];

	protected int tempIndex1,tempIndex2;
	
	protected NearestNeighborVariables nearestNeighborVariables=new NearestNeighborVariables();
	protected RefinerVariables refinerVariables=new RefinerVariables();
	
	public void reset()
	{
		System.arraycopy(CodingFunctions.emptyArray, 0, lagBlock, 0, 7);
		System.arraycopy(CodingFunctions.emptyArray, 0, blockStartPos, 0, 7);
		System.arraycopy(CodingFunctions.emptyArray, 0, plocs2, 0, 8);
	}
}