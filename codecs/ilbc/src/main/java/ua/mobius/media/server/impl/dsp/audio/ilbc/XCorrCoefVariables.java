package ua.mobius.media.server.impl.dsp.audio.ilbc;

public class XCorrCoefVariables 
{
	protected short max,energyScale,totScale,scaleDiff,crossCorrSqMod,energyMod,crossCorrMod,crossCorrScale;
	protected short energyModMax;
	protected short totScaleMax;
	protected short crossCorrSqModMax;
	protected short maxLag;
	
	protected int pos;
	protected int tempIndex1,tempIndex2,tempIndex3,tempIndex4,shifts,newCrit,maxCrit,k,energy,temp,crossCorr;
	
}
