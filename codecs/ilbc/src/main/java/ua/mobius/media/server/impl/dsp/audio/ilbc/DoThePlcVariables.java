package ua.mobius.media.server.impl.dsp.audio.ilbc;

public class DoThePlcVariables 
{
	protected short[] randVec=new short[240];
	protected short scale,scale1,scale2,totScale;
	protected short shift1,shift2,shift3,shiftMax;
	protected short useGain,totGain,maxPerSquare,pitchFact,useLag,randLag,pick,crossSquareMax, crossSquare,tempS,tempS2,lag,max,denom,nom,corrLen;
	
	protected int temp,temp2,tempShift,i,energy,j,ind,measure,maxMeasure;
	CorrData tempCorrData=new CorrData(),corrData=new CorrData();
	
	public void reset()
	{
		System.arraycopy(CodingFunctions.emptyArray, 0, randVec, 0, 240);
	}
}
