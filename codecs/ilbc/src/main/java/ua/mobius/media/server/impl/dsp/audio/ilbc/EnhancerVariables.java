package ua.mobius.media.server.impl.dsp.audio.ilbc;

public class EnhancerVariables 
{
	protected int newBlocks,startPos,plcBlock,iBlock,targetIndex,regressorIndex,i,corrSh,enerSh,index,pos,temp1,temp2,tempIndex,increment,window,syntIndex,ener,start,stop;
	protected int lag=20,tLag=20;
	protected int inputLength;
	protected short max16,max,shifts,scale,scale1,sqrtEnChange,sh;
	
	protected short[] enhancementBuffer;
	protected short[] enhancementPeriod;
	
	protected short[] downsampled=new short[180];
	protected short[] surround=new short[80];
	protected short[] lagMax=new short[3];
	protected short[] corr16=new short[3];
	protected short[] en16=new short[3];
	protected short[] totSh=new short[3];
	protected int[] corr32=new int[50];
	protected int[] corrMax=new int[3];
	
	protected CrossCorrelationVariables crossCorrelationVariables=new CrossCorrelationVariables();
	protected HpOutputVariables hpOutputVariables=new HpOutputVariables();
	protected GetSyncSeqVariables getSyncSeqVariables=new GetSyncSeqVariables();
	protected SmoothVariables smoothVariables=new SmoothVariables();
	
	public void reset()
	{
		System.arraycopy(CodingFunctions.emptyArray, 0, downsampled, 0, 180);
		System.arraycopy(CodingFunctions.emptyArray, 0, surround, 0, 80);
		System.arraycopy(CodingFunctions.emptyArray, 0, lagMax, 0, 3);
		System.arraycopy(CodingFunctions.emptyArray, 0, corr16, 0, 3);
		System.arraycopy(CodingFunctions.emptyArray, 0, en16, 0, 3);
		System.arraycopy(CodingFunctions.emptyArray, 0, totSh, 0, 3);
		
		System.arraycopy(CodingFunctions.emptyIntArray, 0, corr32, 0, 50);
		System.arraycopy(CodingFunctions.emptyIntArray, 0, corrMax, 0, 3);
	}
}