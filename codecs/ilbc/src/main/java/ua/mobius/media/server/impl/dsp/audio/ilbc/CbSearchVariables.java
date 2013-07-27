package ua.mobius.media.server.impl.dsp.audio.ilbc;

public class CbSearchVariables 
{
	protected short[] gains=new short[4];
	protected short[] cbBuf=new short[161];   
	protected short[] energyShifts=new short[256];
	protected short[] targetVec=new short[50];
	protected short[] cbVectors=new short[147];
	protected short[] codedVec=new short[40];
	protected short[] interpSamples=new short[80];
	protected short[] interSamplesFilt=new short[80];
	protected short[] energy=new short[256];    	
	protected short[] augVec=new short[256];
	
	protected short[] cbIndex;
	protected short[] gainIndex;
	
	protected short[] inverseEnergy=energy;
	protected short[] inverseEnergyShifts=energyShifts;
	protected short[] buf=cbBuf;
	protected short[] target=targetVec;    	

	protected int[] cDot=new int[128];
	protected int[] crit=new int[128];

	protected short[] pp;
	protected int ppIndex,sInd,eInd,targetEner,codedEner,gainResult;
	protected int stage,i,j,nBits;
	protected short scale,scale2;
	protected short range,tempS,tempS2;
	
	protected short baseSize;
	protected int numberOfZeroes;
	protected int currIndex;
	
	protected InterpolateSamplesVariables interpolateSamplesVariables=new InterpolateSamplesVariables();
	protected CbMemEnergyAugmentationVariables cbMemEnergyAugmentationVariables=new CbMemEnergyAugmentationVariables();
	protected CbMemEnergyVariables cbMemEnergyVariables=new CbMemEnergyVariables();
	protected CrossCorrelationVariables crossCorrelationVariables=new CrossCorrelationVariables();
	protected CbSearchCoreVariables cbSearchCoreVariables=new CbSearchCoreVariables();
	protected CreateAugmentVectorVariables createAugmentVectorVariables=new CreateAugmentVectorVariables();
	protected GainQuantVariables gainQuantVariables=new GainQuantVariables();
	protected UpdateBestIndexVariables updateBestIndexVariables=new UpdateBestIndexVariables();
	
	public void reset()
	{
		System.arraycopy(CodingFunctions.emptyArray, 0, gains, 0, 4);
		System.arraycopy(CodingFunctions.emptyArray, 0, cbBuf, 0, 161);
		System.arraycopy(CodingFunctions.emptyArray, 0, energyShifts, 0, 256);
		System.arraycopy(CodingFunctions.emptyArray, 0, targetVec, 0, 50);
		System.arraycopy(CodingFunctions.emptyArray, 0, cbVectors, 0, 147);
		System.arraycopy(CodingFunctions.emptyArray, 0, codedVec, 0, 40);
		System.arraycopy(CodingFunctions.emptyArray, 0, interpSamples, 0, 80);
		System.arraycopy(CodingFunctions.emptyArray, 0, interSamplesFilt, 0, 80);
		System.arraycopy(CodingFunctions.emptyArray, 0, energy, 0, 256);
		System.arraycopy(CodingFunctions.emptyArray, 0, augVec, 0, 256);
		
		System.arraycopy(CodingFunctions.emptyIntArray, 0, cDot, 0, 128);
		System.arraycopy(CodingFunctions.emptyIntArray, 0, crit, 0, 128);
	}
}
