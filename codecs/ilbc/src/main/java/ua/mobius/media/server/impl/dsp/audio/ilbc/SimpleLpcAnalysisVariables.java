package ua.mobius.media.server.impl.dsp.audio.ilbc;

public class SimpleLpcAnalysisVariables 
{
	  protected short[] A=new short[11];
	  protected short[] windowedData=new short[240];
	  protected short[] rc=new short[10];
	  protected int[] R=new int[11];
	  
	  protected int j=0;
	  
	  protected short[] lpcBuffer;
	  
	  protected AutoCorrelationVariables autoCorrelationVariables=new AutoCorrelationVariables();
	  protected LevinsonDurbinVariables levinsonDurbinVariables=new LevinsonDurbinVariables();
	  protected Poly2LsfVariables poly2LsfVariables=new Poly2LsfVariables();
	  public void reset()
	  {
		  System.arraycopy(CodingFunctions.emptyArray, 0, A, 0, 11);
		  System.arraycopy(CodingFunctions.emptyArray, 0, windowedData, 0, 240);
		  System.arraycopy(CodingFunctions.emptyArray, 0, rc, 0, 10);
		  System.arraycopy(CodingFunctions.emptyIntArray, 0, R, 0, 11);
	  }
}
