/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

/*
 * 15/07/13 - Change notice:
 * This file has been modified by Mobius Software Ltd.
 * For more information please visit http://www.mobius.ua
 */
package ua.mobius.media.server.impl.dsp.audio.ilbc;

/**
 * 
 * @author oifa yulian 
 */

public class CodingFunctions 
{
	protected static final short[] emptyArray=new short[643];
	protected static final int[] emptyIntArray=new int[128];
	
	private static final short bestIndexMin=(short)-21299;
	private static final short bestIndexMax=(short)21299;
	private static final int bestIndexMinI=-21299;
	private static final int bestIndexMaxI=21299;
	
	public static void hpInput(EncoderState encoderState,short[] data,int startIndex,int length,HpInputVariables variables)
    {
    	variables.ba=Constants.HP_IN_COEFICIENTS;
    	variables.y=encoderState.getHpiMemY();
    	variables.x=encoderState.getHpiMemX();
    	
    	variables.currIndex=startIndex+length;
    	
    	for (variables.i=startIndex; variables.i<variables.currIndex; variables.i++) 
    	{    	   
    		variables.current  = variables.y[1]*variables.ba[3];
    		variables.current += variables.y[3]*variables.ba[4];
    		variables.current = variables.current>>15;
    		variables.current += variables.y[0]*variables.ba[3];
    		variables.current += variables.y[2]*variables.ba[4];
    		variables.current = variables.current<<1;
    	    
    		variables.current += data[variables.i]*variables.ba[0];
    		variables.current += variables.x[0]*variables.ba[1];
    		variables.current += variables.x[1]*variables.ba[2];

    		variables.x[1] = variables.x[0];
    		variables.x[0] = data[variables.i];

    	    if(variables.current>268431359)    	    	
    	    	data[variables.i] = Short.MAX_VALUE;
    	    else if(variables.current< -268439552)
    	    	data[variables.i] = Short.MIN_VALUE;
   	    	else    	    		
   	    		data[variables.i] = (short)((variables.current + 4096)>>13);

    	    variables.y[2] = variables.y[0];
    	    variables.y[3] = variables.y[1];

    	    if (variables.current>268435455)
    	    	variables.current = Integer.MAX_VALUE;
    	    else if (variables.current<-268435456)
    	    	variables.current = Integer.MIN_VALUE;
    	    else
    	    	variables.current = variables.current<<3;    	    

    	    variables.y[0] = (short)(variables.current >> 16);
    	    variables.y[1] = (short)((variables.current - (variables.y[0]<<16))>>1);    	    
        }
    }
	
	public static void hpOutput(short[] signal,int signalIndex,short[] ba,short[] y,short[] x,short len,HpOutputVariables variables)
    {
		
    	for (variables.i=0; variables.i<len; variables.i++) 
    	{
    	    /*
    	      y[i] = b[0]*x[i] + b[1]*x[i-1] + b[2]*x[i-2]
    	      + (-a[1])*y[i-1] + (-a[2])*y[i-2];
    	    */
    		variables.temp  = y[1]*ba[3];     /* (-a[1])*y[i-1] (low part) */
    		variables.temp += y[3]*ba[4];     /* (-a[2])*y[i-2] (low part) */
    		variables.temp = variables.temp>>15;
    		variables.temp += y[0]*ba[3];     /* (-a[1])*y[i-1] (high part) */
    		variables.temp += y[2]*ba[4];     /* (-a[2])*y[i-2] (high part) */
    		variables.temp = variables.temp<<1;

    		variables.temp += signal[variables.i]*ba[0];   /* b[0]*x[0] */
    		variables.temp += x[0]*ba[1];   /* b[1]*x[i-1] */
    		variables.temp += x[1]*ba[2];   /* b[2]*x[i-2] */

    	    /* Update state (input part) */
    	    x[1] = x[0];
    	    x[0] = signal[variables.i];

    	    /* Rounding in Q(12-1), i.e. add 2^10 */
    	    variables.temp2 = variables.temp + 1024;

    	    /* Saturate (to 2^26) so that the HP filtered signal does not overflow */
    	    if(variables.temp2>67108863)
    	    	variables.temp2=67108863;
    	    else if(variables.temp2<-67108864)
    	    	variables.temp2=-67108864;
    	    
    	    /* Convert back to Q0 and multiply with 2 */
    	    signal[signalIndex++] = (short)(variables.temp2>>11);

    	    /* Update state (filtered part) */
    	    y[2] = y[0];
    	    y[3] = y[1];

    	    /* upshift tmpW32 by 3 with saturation */
    	    if (variables.temp>268435455)
    	    	variables.temp = Integer.MAX_VALUE;
    	    else if (variables.temp<-268435456)
    	    	variables.temp = Integer.MIN_VALUE;
    	    else
    	    	variables.temp = variables.temp<<3;    	    

    	    y[0] = (short)(variables.temp>>16);
    	    variables.tempShift = y[0]<<16;
    	    y[1] = (short)((variables.temp - variables.tempShift)>>1);
   	    }
    }
	
	public static void lpcEncode(EncoderState encoderState,EncoderBits encoderBits,short[] synthDenum,int synthDenumIndex,short[] weightDenum,int weightDenumIndex,short[] data,int startIndex,LpcEncodeVariables variables)
    {
		variables.reset();             
    	
        simpleLpcAnalysis(encoderState,variables.lsf,0,data,startIndex,variables.simpleLpcAnalysisVariables);    	
        simpleLsfQ(encoderBits,variables.lsfDeq, 0, variables.lsf, 0,variables.simpleLsfQVariables);        
        lsfCheck(variables.lsfDeq , 0, 10,variables.lsfCheckVariables);
        simpleInterpolateLsf(encoderState,synthDenum, synthDenumIndex, weightDenum, weightDenumIndex, variables.lsf, 0, variables.lsfDeq , 0, 10,variables.simpleInterpolateLsfVariables);    	    	
    }
    
	public static short[] simpleLpcAnalysis(EncoderState encoderState,short[] lsf,int lsfIndex,short[] data,int startIndex,SimpleLpcAnalysisVariables variables)
    {
		variables.reset();
		variables.lpcBuffer=encoderState.getLpcBuffer();    	  
    	  
		System.arraycopy(data, startIndex, variables.lpcBuffer, 300-EncoderState.SIZE, EncoderState.SIZE);    	  

		BasicFunctions.multWithRightShift(variables.windowedData,0, variables.lpcBuffer,60, Constants.LPC_ASYM_WIN, 0, 240, 15);    		  
		autoCorrelation(variables.windowedData,0, 240, 10, variables.R, 0, variables.autoCorrelationVariables);
		windowMultiply(variables.R,0,variables.R,0,Constants.LPC_LAG_WIN, 11);    		      		      	    		    		  
		if (!levinsonDurbin(variables.R, 0, variables.A, 0, variables.rc, 0, 10, variables.levinsonDurbinVariables)) 
	    {
			variables.A[0]=4096;
			for(variables.j=1;variables.j<11;variables.j++)
				variables.A[variables.j]=0;    			  
	    }

		BasicFunctions.expand(variables.A,0,variables.A,0,Constants.LPC_CHIRP_SYNT_DENUM,11);    		      		      		  
		poly2Lsf(lsf,lsfIndex, variables.A,0,variables.poly2LsfVariables);
	      
		System.arraycopy(variables.lpcBuffer, EncoderState.SIZE, variables.lpcBuffer, 0, 300-EncoderState.SIZE);
		return variables.A;
    }
    
	public static void autoCorrelation(short[] input,int inputIndex,int inputLength, int order, int[] result,int resultIndex,AutoCorrelationVariables variables)
    {
		variables.max=0;
        
    	if (order < 0)
            order = inputLength;

    	for(variables.i=0;variables.i<inputLength;variables.i++)
    	{
    		variables.tempS=BasicFunctions.abs(input[inputIndex++]);
    		if(variables.tempS>variables.max)
    			variables.max=variables.tempS;
    	}

    	inputIndex-=inputLength;    	
        if (variables.max == 0)        
        	variables.scale = 0;
        else
        {        	
        	variables.nBits = BasicFunctions.getSize(inputLength);
        	variables.tempS = BasicFunctions.norm(variables.max*variables.max);
                        
            if (variables.tempS > variables.nBits)
            	variables.scale = 0;
            else
            	variables.scale = (short)(variables.nBits - variables.tempS);                        
        }
             
        for (variables.i = 0; variables.i < order + 1; variables.i++)
        {
        	result[resultIndex] = 0;
        	variables.currIndex1=inputIndex;
        	variables.currIndex2=inputIndex+variables.i;
            for (variables.j = inputLength - variables.i; variables.j > 0; variables.j--)
            	result[resultIndex] += ((input[variables.currIndex1++]*input[variables.currIndex2++])>>variables.scale);
            
    		resultIndex++;
        } 
    }

	public static void windowMultiply(int[] output,int outputIndex,int[] input,int inputIndex,int[] window,int length)
    {
		  int i;
		  short xHi,xLow,yHi,yLow;
    	  int nBits = BasicFunctions.norm(input[inputIndex]);
    	  BasicFunctions.bitShiftLeft(input, inputIndex, input, inputIndex,length, nBits);    	  		      	  
	    	  
    	  for (i = 0; i < length; i++) 
    	  {
    	    xHi = (short)(input[inputIndex]>>16);
    	    yHi = (short)(window[i]>>16);

    	    xLow = (short)((input[inputIndex++] - (xHi<<16))>>1);
    	    yLow = (short)((window[i] - (yHi<<16))>>1);

    	    output[outputIndex]=(xHi*yHi)<<1;
    	    output[outputIndex]+=(xHi*yLow)>>14;
    	    output[outputIndex++]+=(xLow*yHi)>>14;    	        	    
    	  }    	      	  		
    	  
    	  outputIndex-=length;
    	  BasicFunctions.bitShiftRight(output, outputIndex, output, outputIndex,length, nBits);    	  
    }
    
	public static boolean levinsonDurbin(int[] R,int rIndex,short[] A,int aIndex,short[] K,int kIndex,int order,LevinsonDurbinVariables variables)
    {
    	variables.reset();
    	variables.nBits = BasicFunctions.norm(R[rIndex]);
    	variables.currIndex=rIndex+order;
    	
        for (variables.i = order; variables.i >= 0; variables.i--)
        {
        	variables.temp = R[variables.currIndex--]<<variables.nBits;
        	variables.rHi[variables.i] = (short)(variables.temp>>16);
        	variables.rLow[variables.i] = (short)((variables.temp - (variables.rHi[variables.i]<<16))>>1);
        }

        variables.temp2 = variables.rHi[1]<<16; 
        variables.temp3 = variables.rLow[1]<<1;
        variables.temp2 += variables.temp3;
        
        if(variables.temp2>0)
        	variables.temp3=variables.temp2;
        else
        	variables.temp3=-variables.temp2;                        
        
        variables.temp = BasicFunctions.div(variables.temp3, variables.rHi[0], variables.rLow[0]);        
        if (variables.temp2 > 0)
        	variables.temp = -variables.temp;                
        
        variables.xHi = (short)(variables.temp>>16);
        variables.xLow = (short)((variables.temp - (variables.xHi<<16))>>1);

        K[kIndex++] = variables.xHi;
        variables.temp = variables.temp>>4;
    	       	
        variables.aHi[1] = (short)(variables.temp>>16);
        variables.aLow[1] = (short)((variables.temp - (variables.aHi[1]<<16))>>1);
        
        variables.temp=(variables.xHi*variables.xLow) >> 14;
        variables.temp+=variables.xHi*variables.xHi;
        variables.temp<<=1;    	
    	
        if(variables.temp<0)
        	variables.temp=0-variables.temp;
                
        variables.temp = Integer.MAX_VALUE - variables.temp;
        
        variables.tempS = (short)(variables.temp>>16);
        variables.tempS2 = (short)((variables.temp - (variables.tempS<<16))>>1);

        variables.temp=variables.rHi[0]*variables.tempS;
        variables.temp+=(variables.rHi[0]*variables.tempS2) >> 15;
        variables.temp+=(variables.rLow[0]*variables.tempS) >> 15;
        variables.temp <<= 1;

        variables.alphaExp = BasicFunctions.norm(variables.temp);
        variables.temp = variables.temp<<variables.alphaExp;
        variables.yHi = (short)(variables.temp>>16);
        variables.yLow = (short)((variables.temp - (variables.yHi<<16))>>1);
        
        for (variables.i = 2; variables.i <= order; variables.i++)
        {
        	variables.temp = 0;
        	variables.currIndex=variables.i-1;
            for (variables.j = 1; variables.j < variables.i; variables.j++)
            {
            	variables.temp2=(variables.rHi[variables.j]*variables.aLow[variables.currIndex]) >> 15;
            	variables.temp2+=(variables.rLow[variables.j]*variables.aHi[variables.currIndex]) >> 15;
        		variables.temp+=variables.temp2<<1;        		
        		variables.temp+=(variables.rHi[variables.j]*variables.aHi[variables.currIndex]) << 1;
        		variables.currIndex--;
            }

            variables.temp = variables.temp<<4;
            variables.temp+=variables.rHi[variables.i]<<16;
            variables.temp += variables.rLow[variables.i]<<1;            
            
            variables.temp2 = Math.abs(variables.temp);   
            variables.temp3 = BasicFunctions.div(variables.temp2, variables.yHi, variables.yLow);            
            
            if (variables.temp > 0)
            	variables.temp3 = -variables.temp3;
            
            variables.nBits = BasicFunctions.norm(variables.temp3);
            
            if (variables.alphaExp <= variables.nBits || variables.temp3 == 0)
            	variables.temp3 = variables.temp3<<variables.alphaExp;
            else
            {
                if (variables.temp3 > 0)
                	variables.temp3 = Integer.MAX_VALUE;
                else
                	variables.temp3 = Integer.MIN_VALUE;                
            }
               
            variables.xHi = (short)(variables.temp3>>16);
            variables.xLow = (short)((variables.temp3 - (variables.xHi<<16))>>1);

            K[kIndex++] = variables.xHi;
            
            if (BasicFunctions.abs(variables.xHi) > 32750)
            	return false; // Unstable filter
            
            variables.currIndex=variables.i-1;
            variables.currIndex=variables.i-1;
            for (variables.j = 1; variables.j < variables.i; variables.j++)
            {
            	variables.temp=variables.aHi[variables.j]<<16;
            	variables.temp += variables.aLow[variables.j]<<1;
            	variables.temp2=(variables.xLow*variables.aHi[variables.currIndex]) >> 15;
            	variables.temp2+=(variables.xHi*variables.aLow[variables.currIndex]) >> 15;
            	variables.temp2+=variables.xHi*variables.aHi[variables.currIndex];            	
            	variables.temp += variables.temp2 << 1;
                
            	variables.aUpdHi[variables.j] = (short)(variables.temp>>16);
            	variables.aUpdLow[variables.j] = (short)((variables.temp - (variables.aUpdHi[variables.j]<<16))>>1);
            	variables.currIndex--;
            }

            variables.temp3 = variables.temp3>>4;
            
            variables.aUpdHi[variables.i] = (short)(variables.temp3>>16);
            variables.aUpdLow[variables.i] = (short)((variables.temp3 - (variables.aUpdHi[variables.i]<<16))>>1);

            variables.temp = (((variables.xHi*variables.xLow) >> 14) + variables.xHi*variables.xHi) << 1;
            if(variables.temp<0)
            	variables.temp = 0-variables.temp;
            
            variables.temp = Integer.MAX_VALUE - variables.temp;            
            variables.tempS = (short)(variables.temp>>16);
            variables.tempS2 = (short)((variables.temp - (variables.tempS<<16))>>1);
            
            variables.temp2=(variables.yHi*variables.tempS2) >> 15;
            variables.temp2+=(variables.yLow*variables.tempS) >> 15;        	
        	variables.temp2+=variables.yHi*variables.tempS;
        	variables.temp=variables.temp2<<1;
            
        	variables.nBits = BasicFunctions.norm(variables.temp);
        	variables.temp = variables.temp<<variables.nBits;            
            
        	variables.yHi = (short)(variables.temp>>16);
        	variables.yLow = (short)((variables.temp - (variables.yHi<<16))>>1);
        	variables.alphaExp = (short)(variables.alphaExp + variables.nBits);
            
            for (variables.j = 1; variables.j <= variables.i; variables.j++)
            {
            	variables.aHi[variables.j] = variables.aUpdHi[variables.j];
            	variables.aLow[variables.j] = variables.aUpdLow[variables.j];                
            }                                   
        }

        A[aIndex++] = 4096;        
        for (variables.i = 1; variables.i <= order; variables.i++)
        {
        	variables.temp=variables.aHi[variables.i]<<16;
        	variables.temp +=variables.aLow[variables.i]<<1;
        	variables.temp<<=1;
        	variables.temp+=32768;
            A[aIndex++] = (short)(variables.temp>>16);
        }
        
        return true;
    }   
	
	public static void simpleInterpolateLsf(EncoderState encoderState,short[] synthdenum,int synthDenumIndex,short[] weightDenum,int weightDenumIndex,short[] lsf,int lsfIndex,short[] lsfDeq,int lsfDeqIndex,int length,SimpleInterpolateLsfVariables variables)
    {
		variables.reset();
		variables.lsfOld=encoderState.getLsfOld();
		variables.lsfDeqOld=encoderState.getLsfDeqOld();
    	
		variables.step=length + 1;    	
    	
		for (variables.i = 0; variables.i < EncoderState.SUBFRAMES; variables.i++) 
	    {
	    	lsfInterpolate2PolyEnc(variables.lp, (short)0 ,variables.lsfDeqOld, 0, lsfDeq, lsfDeqIndex , Constants.LSF_WEIGHT_20MS[variables.i], length,variables.lsfInterpolate2PolyEncVariables);    	    	
	    	System.arraycopy(variables.lp, 0, synthdenum, synthDenumIndex + variables.index, variables.step);    	      

	    	lsfInterpolate2PolyEnc(variables.lp, (short)0 , variables.lsfOld, 0, lsf, lsfIndex, Constants.LSF_WEIGHT_20MS[variables.i], length, variables.lsfInterpolate2PolyEncVariables);
	    	BasicFunctions.expand(weightDenum,weightDenumIndex + variables.index,variables.lp,0,Constants.LPC_CHIRP_WEIGHT_DENUM,variables.step);    	      

	    	variables.index += variables.step;
	    }

	    System.arraycopy(lsf, lsfIndex, variables.lsfOld, 0, length);
	    System.arraycopy(lsfDeq, lsfDeqIndex, variables.lsfDeqOld, 0, length);  	   
    }
	
    public static void lsfInterpolate2PolyEnc(short[] a, short aIndex, short[] lsf1, int lsf1Index, short[] lsf2, int lsf2Index, short coef, int length,LsfInterpolate2PolyEncVariables variables)
    {
		variables.reset();		
    	interpolate(variables.lsfTemp, 0, lsf1, lsf1Index, lsf2, lsf2Index, coef, length,variables.interpolateVariables);
    	lsf2Poly(a, aIndex, variables.lsfTemp, 0,variables.lsf2PolyVariables);
    }
    
	public static void interpolate(short[] out,int outIndex,short[] in1,int in1Index,short[] in2,int in2Index,short coef,int length,InterpolateVariables variables)
    {
		variables.tempS = (short)(16384 - coef);
    	for (variables.k = 0; variables.k < length; variables.k++) 
    	    out[outIndex++] = (short) ((coef*in1[in1Index++] + variables.tempS*in2[in2Index++]+8192)>>14);    	
    }
     
	public static void lsf2Poly(short[] a,int aIndex,short[] lsf,int lsfIndex,Lsf2PolyVariables variables)
    {
		variables.reset();
		lsf2Lsp(lsf, lsfIndex, variables.lsp, 0, 10,variables.lsf2LspVariables);    	    	           	
    	
    	getLspPoly(variables.lsp, 0, variables.f1, 0,variables.getLspPolyVariables);
    	getLspPoly(variables.lsp, 1, variables.f2, 0,variables.getLspPolyVariables);

    	for (variables.k=5; variables.k>0; variables.k--)
    	{
    		variables.f1[variables.k]+=variables.f1[variables.k-1];
    		variables.f2[variables.k]-=variables.f2[variables.k-1];    		
    	}

    	a[aIndex]=4096;
    	
    	int aStartIndex=aIndex+1;
    	int aEndIndex=aIndex+10;
    	
    	int currIndex=1;
    	for (variables.k=5; variables.k>0; variables.k--)
    	{
    		a[aStartIndex++] = (short)(((variables.f1[currIndex] + variables.f2[currIndex])+4096)>>13);
    		a[aEndIndex--] = (short)(((variables.f1[currIndex] - variables.f2[currIndex])+4096)>>13);
    		currIndex++;
    	}
    }
    
	public static void lsf2Lsp(short[] lsf,int lsfIndex,short[] lsp,int lspIndex,int count,Lsf2LspVariables variables)
    {
    	for(variables.j=0; variables.j<count; variables.j++)
    	{
    		variables.tempS = (short)((lsf[lsfIndex++]*20861)>>15);
    		variables.tempS2 = (short)(variables.tempS>>8);
    		variables.tempS = (short)(variables.tempS & 0x00ff);

    	    if (variables.tempS2>63 || variables.tempS2<0)
    	    	variables.tempS2 = 63;    

    	    lsp[lspIndex++] = (short)(((Constants.COS_DERIVATIVE[variables.tempS2]*variables.tempS)>>12) + Constants.COS[variables.tempS2]);
    	}
    }
    
    public static void getLspPoly(short[] lsp,int lspIndex,int[] f,int fIndex,GetLspPolyVariables variables)
    {
    	f[fIndex++]=16777216;
    	f[fIndex++] = lsp[lspIndex]*(-1024);
    	lspIndex+=2;

    	for(variables.k=2; variables.k<=5; variables.k++)
    	{
    		f[fIndex]=f[fIndex-2];    		

    	    for(variables.j=variables.k; variables.j>1; variables.j--)
    	    {
    	    	variables.xHi = (short)(f[fIndex-1]>>16);
    	    	variables.xLow = (short)((f[fIndex-1]-(variables.xHi<<16))>>1);

    	    	f[fIndex] += f[fIndex-2];    	    	
    	    	f[fIndex--] -= ((variables.xHi*lsp[lspIndex])<<2) + (((variables.xLow*lsp[lspIndex])>>15)<<2);         	    	    	    	
    	    }
    	    
    	    f[fIndex] -= lsp[lspIndex]<< 10;
    	    
    	    fIndex+=variables.k;
    	    lspIndex+=2;
    	}
    }
    
    public static void poly2Lsf(short[] lsf,int lsfIndex,short[] A,int aIndex,Poly2LsfVariables variables)
    {
    	variables.reset();
    	
    	poly2Lsp(A, aIndex, variables.lsp, 0, variables.poly2LspVariables);    	
    	lspToLsf(variables.lsp, 0,lsf, lsfIndex, 10, variables.lspToLsfVariables);    	        	
    }
    
    public static void poly2Lsp(short[] A,int aIndex,short[] lsp,int lspIndex,Poly2LspVariables variables)
    {
    	variables.reset();
    	variables.aLowIndex=aIndex + 1;
    	variables.aHighIndex=aIndex + 10;
    	
    	variables.f1[0]=1024;
    	variables.f2[0]=1024;
    	
    	for (variables.i = 0; variables.i < 5; variables.i++) 
    	{
    		variables.f1[variables.i+1] = (short)(((A[variables.aLowIndex] + A[variables.aHighIndex])>>2) - variables.f1[variables.i]);    	    
    		variables.f2[variables.i+1] = (short)(((A[variables.aLowIndex] - A[variables.aHighIndex])>>2) + variables.f2[variables.i]);        	        	
        	
    		variables.aLowIndex++;
    		variables.aHighIndex--;        	
    	}

    	variables.current=variables.f1;
    	variables.currIndex = lspIndex;
    	variables.foundFreqs = 0;

    	variables.xLow = Constants.COS_GRID[0];
    	variables.yLow = chebushev(variables.xLow, variables.current, 0, variables.chebushevVariables);
    	
    	for (variables.j = 1; variables.j < Constants.COS_GRID.length && variables.foundFreqs < 10; variables.j++) 
    	{
    		variables.xHi = variables.xLow;
    		variables.yHi = variables.yLow;
    		variables.xLow = Constants.COS_GRID[variables.j];
    		variables.yLow = chebushev(variables.xLow, variables.current, 0, variables.chebushevVariables);    	    
    	    
    	    if (variables.yLow*variables.yHi <= 0) 
    	    {
    	    	for (variables.i = 0; variables.i < 4; variables.i++) 
    	    	{
    	    		variables.xMid = (short)((variables.xLow>>1) + (variables.xHi>>1));
    	    		variables.yMid = chebushev(variables.xMid, variables.current, 0, variables.chebushevVariables);
    	    		
    	    		if (variables.yLow*variables.yMid <= 0) 
    	    		{
    	    			variables.yHi = variables.yMid;
    	    			variables.xHi = variables.xMid;
    	    		} 
    	    		else 
    	    		{
    	    			variables.yLow = variables.yMid;
    	    			variables.xLow = variables.xMid;
    	    		}
    	    	}

    	    	variables.x = (short)(variables.xHi - variables.xLow);
    	    	variables.y = (short)(variables.yHi - variables.yLow);
    	    	
    	    	if (variables.y == 0)
    	    		lsp[variables.currIndex++] = variables.xLow;
    	      	else 
    	      	{
    	      		variables.temp = variables.y;
    	      		variables.y = BasicFunctions.abs(variables.y);
    	      		variables.nBits = (short)(BasicFunctions.norm(variables.y)-16);
    	      		variables.y = (short)(variables.y<<variables.nBits);
    	      		
    	      		if (variables.y != 0)
    	      			variables.y=(short)(536838144 / variables.y);
    	      	    else
    	      	    	variables.y=(short)Integer.MAX_VALUE;    	      	    

    	      		variables.y = (short)(((variables.x*variables.y)>>(19-variables.nBits))&0xFFFF);

    	      		if (variables.temp < 0)
    	      			variables.y = (short)-variables.y;    	        
    	        
    	      		lsp[variables.currIndex++] = (short)(variables.xLow-(((variables.yLow*variables.y)>>10)&0xFFFF));
    	      	}

    	    	variables.foundFreqs++;
    	    	if (variables.foundFreqs<10) 
    	    	{
    	    		variables.xLow = lsp[variables.currIndex-1];
    	    		if((variables.foundFreqs%2)==0)
    	    			variables.current=variables.f1;
    	    		else
    	    			variables.current=variables.f2;    	        

    	    		variables.yLow = chebushev(variables.xLow, variables.current, 0, variables.chebushevVariables);
    	    	}
    	    }
    	}

    	if (variables.foundFreqs < 10)   
    		System.arraycopy(Constants.LSP_MEAN, 0, lsp, lspIndex, 10);    	
    }
    
    public static short chebushev(short value,short[] coefs,int coefsIndex,ChebushevVariables variables)
    {    	
    	variables.b2 = 0x1000000;
    	variables.temp = value<<10;
    	coefsIndex++;
    	variables.temp += coefs[coefsIndex++]<<14;
    	
    	for (variables.n = 2; variables.n < 5; variables.n++) 
    	{
    		variables.temp2 = variables.temp;

    		variables.b1Hi = (short)(variables.temp>>16);
    	    variables.b1Low = (short)((variables.temp-(variables.b1Hi<<16))>>1);    	    
    	    
    	    variables.temp=(((variables.b1Low*value)>>15) + (variables.b1Hi*value))<<2;    		
    	    variables.temp -= variables.b2;
    	    variables.temp += coefs[coefsIndex++]<<14;

    	    variables.b2 = variables.temp2;    	        	    
    	}

    	variables.b1Hi = (short)(variables.temp>>16);
    	variables.b1Low = (short)((variables.temp-(variables.b1Hi<<16))>>1);

    	variables.temp=(((variables.b1Low*value)>>15)<<1) + ((variables.b1Hi*value)<<1);    	
    	variables.temp -= variables.b2;
    	variables.temp += coefs[coefsIndex]<<13;
    	
    	if (variables.temp>33553408) 
    	  	return Short.MAX_VALUE;
    	else if (variables.temp<-33554432)
    		return Short.MIN_VALUE;
    	else
    		return (short)(variables.temp>>10);    	  
    }
    
    public static void lspToLsf(short[] lsp, int lspIndex,short[] lsf,int lsfIndex,int coefsNumber,LspToLsfVariables variables)
    {
    	variables.j = 63;
    	variables.currLspIndex=lspIndex+coefsNumber-1;  
    	variables.currLsfIndex=lsfIndex+coefsNumber-1;    	
    	    	
    	for(variables.i=coefsNumber-1; variables.i>=0; variables.i--)
    	{
    		while(Constants.COS[variables.j]<lsp[variables.currLspIndex] && variables.j>0)
    			variables.j--;
    	    
    		variables.currValue = (variables.j<<9)+((Constants.ACOS_DERIVATIVE[variables.j]*(lsp[variables.currLspIndex--]-Constants.COS[variables.j]))>>11);
    	    lsf[variables.currLsfIndex--] = (short)((variables.currValue*25736)>>15);    	        		    	        	        	   
    	  }
    }
    
    public static void simpleLsfQ(EncoderBits encoderBits,short[] lsfdeq,int lsfdeqIndex,short[] lsfArray,int lsfArrrayIndex,SimpleLsfQVariables variables)
    {
    	splitVq(lsfdeq, lsfdeqIndex,encoderBits.getLSF(), 0, lsfArray, lsfArrrayIndex,variables.splitVqVariables);    	    
    }
    
    public static void splitVq(short[] qX,int qXIndex,short[] lsf,int lsfIndex,short[] X,int xIndex,SplitVqVariables variables)
    {
    	vq3(qX, qXIndex, lsf, lsfIndex, Constants.LSF_INDEX_CB[0], X, xIndex, Constants.LSF_SIZE_CB[0],variables.vq3Variables);
    	vq3(qX, qXIndex + Constants.LSF_DIM_CB[0], lsf, lsfIndex+1, Constants.LSF_INDEX_CB[1], X, xIndex + Constants.LSF_DIM_CB[0], Constants.LSF_SIZE_CB[1],variables.vq3Variables);    	
    	vq4(qX, qXIndex + Constants.LSF_DIM_CB[0] + Constants.LSF_DIM_CB[1], lsf, lsfIndex+2, Constants.LSF_INDEX_CB[2], X, xIndex + Constants.LSF_DIM_CB[0] + Constants.LSF_DIM_CB[1], Constants.LSF_SIZE_CB[2],variables.vq4Variables);
    }       

    public static void vq3(short[] qX,int qXIndex,short[] lsf,int lsfIndex,int cbIndex,short[] X,int xIndex,int cbSize,Vq3Variables variables)
    {
    	variables.minValue = Integer.MAX_VALUE;
    	variables.currIndex=0;
    	
    	for (variables.j = 0; variables.j < cbSize; variables.j++) 
    	{
    		variables.tempS = (short)(X[xIndex++] - Constants.LSF_CB[cbIndex++]);
    		variables.temp = variables.tempS*variables.tempS;
    		variables.tempS = (short)(X[xIndex++] - Constants.LSF_CB[cbIndex++]);
    		variables.temp+=variables.tempS*variables.tempS;
    		variables.tempS = (short)(X[xIndex++] - Constants.LSF_CB[cbIndex++]);
    		variables.temp+=variables.tempS*variables.tempS;	    		    	    	        	  

	    	xIndex-=3;
    	    if (variables.temp < variables.minValue) 
    	    {
    	    	variables.minValue = variables.temp;
    	    	variables.currIndex=variables.j;    	    	    	    	
    	    }    	        	   
    	}

    	cbIndex-=3*cbSize;
    	lsf[lsfIndex] = (short)variables.currIndex;
    	variables.currIndex*=3;
    	variables.currIndex+=cbIndex;
    	for (variables.i = 0; variables.i < 3; variables.i++)
    	    qX[qXIndex++] = Constants.LSF_CB[variables.currIndex++];    	      	      	
    }
    
    public static void vq4(short[] qX,int qXIndex,short[] lsf,int lsfIndex,int cbIndex,short[] X,int xIndex,int cbSize,Vq4Variables variables)
    {
    	variables.minValue = Integer.MAX_VALUE;
    	variables.currIndex=0;
    	
    	for (variables.j = 0; variables.j < cbSize; variables.j++) 
    	{
    		variables.tempS = (short)(X[xIndex++] - Constants.LSF_CB[cbIndex++]);
    		variables.temp = variables.tempS*variables.tempS;
    	    for (variables.i = 1; variables.i < 4; variables.i++) 
    	    {
    	    	variables.tempS = (short)(X[xIndex++] - Constants.LSF_CB[cbIndex++]);
    	    	variables.temp += variables.tempS*variables.tempS;
    	    }

    	    xIndex-=4;
    	    if (variables.temp < variables.minValue) 
    	    {
    	    	variables.minValue = variables.temp;
    	    	variables.currIndex=variables.j;
    	    }    	        	   
    	}

    	cbIndex-=4*cbSize;
    	lsf[lsfIndex] = (short)variables.currIndex;
    	variables.currIndex*=4;
    	variables.currIndex+=cbIndex;
      	for (variables.i = 0; variables.i < 4; variables.i++)
      	    qX[qXIndex++] = Constants.LSF_CB[variables.currIndex++];      	
    }
    
    public static void lsfCheck(short[] lsf,int lsfIndex,int lsfSize,LsfCheckVariables variables)
    {
    	for (variables.n=0;variables.n<2;variables.n++) 
    	{
    		for (variables.k=0;variables.k<lsfSize-1;variables.k++) 
	    	{
    			variables.currIndex1=lsfIndex + variables.k;
    			variables.currIndex2=variables.currIndex1+1;
	    		
	    		if ((lsf[variables.currIndex2]-lsf[variables.currIndex1])<Constants.EPS) 
	    		{
	    			if (lsf[variables.currIndex2]<lsf[variables.currIndex1]) 
	    			{
	    				lsf[variables.currIndex2]= (short)(lsf[variables.currIndex1]+Constants.HALF_EPS);
	    				lsf[variables.currIndex1]= (short)(lsf[variables.currIndex2]-Constants.HALF_EPS);
	    			} 
	    			else 
	    			{
	    				lsf[variables.currIndex1]-=Constants.HALF_EPS;
	    				lsf[variables.currIndex2]+=Constants.HALF_EPS;
	    			}
	    		}

	    		if (lsf[variables.currIndex1]<Constants.MIN_LSF) 
	    			lsf[variables.currIndex1]=Constants.MIN_LSF;
	    		
	    		if (lsf[variables.currIndex1]>Constants.MAX_LSF) 
	    			lsf[variables.currIndex1]=Constants.MAX_LSF;    	    			    	    		
	    	}
    	}
    }
    
	public static short gainDequant(short index, short maxIn, short stage)
    {
    	if(maxIn<0)
    		maxIn=(short)(0-maxIn);
    	
    	if(maxIn<1638)
    		maxIn=1638;    	    	    	

    	return (short)((maxIn*Constants.GAIN[stage][index] +8192)>>14);
    }        
        
	public static short gainQuant(short gain,short maxIn,short stage,short[] index,int indexIndex,GainQuantVariables variables)
    {
		if(maxIn>1638)
			variables.scale = maxIn;
    	else
    		variables.scale = 1638;

		variables.cb = Constants.GAIN[stage];
		variables.temp = gain<<14;

		variables.cbIndex=(32>>stage) >> 1;
	    		variables.nBits = (short)variables.cbIndex;
    	for (variables.n=4-stage;variables.n>0;variables.n--) 
    	{
    		variables.nBits>>=1;
    	    if (variables.temp > variables.scale*variables.cb[variables.cbIndex]) 
    	    	variables.cbIndex+=variables.nBits;
    	    else 
    	    	variables.cbIndex-=variables.nBits;    	        	   
    	}

    	variables.temp2=variables.scale*variables.cb[variables.cbIndex];
    	if (variables.temp>variables.temp2) 
    	{
    	    if ((variables.scale*variables.cb[variables.cbIndex+1]-variables.temp)<(variables.temp-variables.temp2))
    	    	variables.cbIndex++;    	    
    	}
    	else if ((variables.temp-variables.scale*variables.cb[variables.cbIndex-1])<=(variables.temp2-variables.temp)) 
    		variables.cbIndex--;    	    
    	
    	variables.temp=(32>>stage) - 1;
    	if(variables.cbIndex>variables.temp)
    		variables.cbIndex=variables.temp;
    	
    	index[indexIndex]=(short)variables.cbIndex;    	
    	return (short)((variables.scale*variables.cb[variables.cbIndex]+8192)>>14); 
    }    
	
	public static void cbMemEnergyAugmentation(short[] interpSamples, int interpSamplesIndex, short[] cbMem, int cbMemIndex, short scale, short baseSize, short[] energy, int energyIndex, short[] energyShifts, int energyShiftsIndex,CbMemEnergyAugmentationVariables variables)    	    
    {
		energyIndex = energyIndex + baseSize-20;
    	energyShiftsIndex = energyShiftsIndex + baseSize-20;
    	cbMemIndex = cbMemIndex + 147;    	

    	variables.en1 = BasicFunctions.scaleRight(cbMem,cbMemIndex-19,cbMem,cbMemIndex-19,15,scale);    		
    	variables.currIndex=cbMemIndex - 20;
    	
    	for (variables.n=20; variables.n<=39; variables.n++) 
    	{
    		variables.en1 += (cbMem[variables.currIndex]*cbMem[variables.currIndex])>>scale;
    		variables.currIndex--;
    		variables.currValue = variables.en1;

    	    /* interpolation */
    		variables.currValue += BasicFunctions.scaleRight(interpSamples,interpSamplesIndex,interpSamples,interpSamplesIndex,4,scale);        	    	
    	    interpSamplesIndex += 4;

    	    /* Compute energy for the remaining samples */
    	    variables.currValue += BasicFunctions.scaleRight(cbMem,cbMemIndex - variables.n,cbMem,cbMemIndex - variables.n,40-variables.n,scale);    	    	

    	    /* Normalize the energy and store the number of shifts */
    	    energyShifts[energyShiftsIndex] = BasicFunctions.norm(variables.currValue);
    	    variables.currValue = variables.currValue<<energyShifts[energyShiftsIndex++];
    	    
    	    energy[energyIndex++] = (short)(variables.currValue>>16);    	    
    	}
    }
	
	public static void cbMemEnergy(short range,short[] cb,int cbIndex,short[] filteredCB,int filteredCbIndex, short length, short targetLength, short[] energy, int energyIndex, short[] energyShifts, int energyShiftsIndex, short scale, short baseSize,CbMemEnergyVariables variables)
    {
		variables.currValue = BasicFunctions.scaleRight(cb, cbIndex + length - targetLength, cb, cbIndex + length - targetLength, targetLength, scale);
    	energyShifts[energyShiftsIndex] = (short)BasicFunctions.norm(variables.currValue);
    	energy[energyIndex] = (short)((variables.currValue<<energyShifts[energyShiftsIndex])>>16);
    	energyCalc(variables.currValue, range, cb, cbIndex + length - targetLength - 1, cb, cbIndex + length - 1, energy, energyIndex, energyShifts, energyShiftsIndex, scale, (short)0);
    	
    	variables.currValue = BasicFunctions.scaleRight(filteredCB, filteredCbIndex + length - targetLength, filteredCB, filteredCbIndex + length - targetLength, targetLength, scale);    	
    	energyShifts[baseSize + energyShiftsIndex]=BasicFunctions.norm(variables.currValue);    	
    	energy[baseSize + energyIndex] = (short)((variables.currValue<<energyShifts[baseSize + energyShiftsIndex])>>16);
    	energyCalc(variables.currValue, range, filteredCB, filteredCbIndex + length - targetLength - 1, filteredCB, filteredCbIndex + length - 1, energy, energyIndex, energyShifts, energyShiftsIndex, scale, baseSize);    	    		    
    }
	
	public static void cbSearch(EncoderState encoderState,EncoderBits encoderBits,CbSearchData searchData,CbUpdateIndexData updateIndexData,short[] inTarget,int inTargetIndex,short[] decResidual,int decResidualIndex,int length,int vectorLength,short[] weightDenum,int weightDenumindex, int blockNumber,int cbIndexIndex,int gainIndexIndex,CbSearchVariables variables)
    {	
		variables.reset();
    	variables.cbIndex=encoderBits.getCbIndex();
    	variables.gainIndex=encoderBits.getGainIndex();
    	
    	variables.cDot=new int[128];
    	variables.crit=new int[128];

    	variables.baseSize=(short)(length-vectorLength+1);
    	if (vectorLength==40)
    		variables.baseSize=(short)(length-19);    	  
    	
    	variables.numberOfZeroes=length-Constants.FILTER_RANGE[blockNumber];
    	BasicFunctions.filterAR(decResidual, decResidualIndex + variables.numberOfZeroes, variables.buf, 10+variables.numberOfZeroes, weightDenum, weightDenumindex, 11, Constants.FILTER_RANGE[blockNumber]);    	
    	System.arraycopy(variables.cbBuf, length, variables.targetVec, 0, 10);
    	
    	BasicFunctions.filterAR(inTarget, inTargetIndex, variables.target, 10, weightDenum, weightDenumindex, 11, vectorLength);    	
    	System.arraycopy(variables.target, 10, variables.codedVec, 0, vectorLength);  	

    	variables.currIndex=10;    	
    	variables.tempS=0;
    	for(variables.i=0;variables.i<length;variables.i++)
    	{
    		if(variables.buf[variables.currIndex]>0 && variables.buf[variables.currIndex]>variables.tempS)
    			variables.tempS=variables.buf[variables.currIndex];
    		else if((0-variables.buf[variables.currIndex])>variables.tempS)
    			variables.tempS=(short)(0-variables.buf[variables.currIndex]);
    		
    		variables.currIndex++;
    	}
    	
    	variables.currIndex=10;
    	variables.tempS2=0;
    	for(variables.i=0;variables.i<vectorLength;variables.i++)
    	{
    		if(variables.target[variables.currIndex]>0 && variables.target[variables.currIndex]>variables.tempS2)
    			variables.tempS2=variables.target[variables.currIndex];
    		else if((0-variables.target[variables.currIndex])>variables.tempS2)
    			variables.tempS2=(short)(0-variables.target[variables.currIndex]);
    		
    		variables.currIndex++;
    	}    	 

    	if ((variables.tempS>0)&&(variables.tempS2>0)) 
    	{
    		if(variables.tempS2>variables.tempS)
    			variables.tempS = variables.tempS2;
    		
    		variables.scale = BasicFunctions.getSize(variables.tempS*variables.tempS);
    	} 
    	else 
    		variables.scale = 30;
    	
    	variables.scale = (short)(variables.scale - 25);
    	if(variables.scale<0)
    		variables.scale=0;    	

    	variables.scale2=variables.scale;
    	variables.targetEner = BasicFunctions.scaleRight(variables.target,10,variables.target,10,vectorLength, variables.scale2);    	 
    	
    	filteredCBVecs(variables.cbVectors, 0, variables.buf, 10, length,Constants.FILTER_RANGE[blockNumber]);
    	
    	variables.range = Constants.SEARCH_RANGE[blockNumber][0];    	
    	if(vectorLength == 40) 
    	{
    		interpolateSamples(variables.interpSamples,0,variables.buf,10,length,variables.interpolateSamplesVariables);
    		interpolateSamples(variables.interSamplesFilt,0,variables.cbVectors,0,length,variables.interpolateSamplesVariables);
    	    
    		cbMemEnergyAugmentation(variables.interpSamples, 0, variables.buf, 10, variables.scale2, (short)20, variables.energy, 0, variables.energyShifts, 0,variables.cbMemEnergyAugmentationVariables);    	    
    		cbMemEnergyAugmentation(variables.interSamplesFilt, 0, variables.cbVectors, 0, variables.scale2, (short)(variables.baseSize+20), variables.energy, 0, variables.energyShifts, 0,variables.cbMemEnergyAugmentationVariables);

    		cbMemEnergy(variables.range, variables.buf, 10, variables.cbVectors, 0, (short)length, (short)vectorLength, variables.energy, 20, variables.energyShifts, 20, variables.scale2, variables.baseSize,variables.cbMemEnergyVariables);    	        	    	
    	}
    	else 
    		cbMemEnergy(variables.range, variables.buf, 10, variables.cbVectors, 0, (short)length, (short)vectorLength, variables.energy, 0, variables.energyShifts, 0, variables.scale2, variables.baseSize,variables.cbMemEnergyVariables);
    	
    	energyInverse(variables.energy,0,variables.baseSize*2);
    	
    	variables.gains[0] = 16384;
    	for (variables.stage=0; variables.stage<3; variables.stage++) 
    	{
    		variables.range = Constants.SEARCH_RANGE[blockNumber][variables.stage];

    	    /* initialize search measures */
    	    updateIndexData.setCritMax(0);
    	    updateIndexData.setShTotMax((short)-100);
    	    updateIndexData.setBestIndex((short)0);
    	    updateIndexData.setBestGain((short)0);    	    

    	    /* Calculate all the cross correlations (augmented part of CB) */
    	    if (vectorLength==40) 
    	    {
    	    	augmentCbCorr(variables.target, 10, variables.buf, 10+length, variables.interpSamples, 0, variables.cDot, 0,20, 39, variables.scale2);    	    	    	    	
    	    	variables.currIndex=20;
    	    } 
    	    else 
    	    	variables.currIndex=0;
    	    
    	    crossCorrelation(variables.cDot, variables.currIndex, variables.target, 10, variables.buf, 10 + length - vectorLength, (short)vectorLength, variables.range, variables.scale2, (short)-1,variables.crossCorrelationVariables);    	        	    
        	
    	    if (vectorLength==40)
    	    	variables.range=(short)(Constants.SEARCH_RANGE[blockNumber][variables.stage]+20);
    	    else
    	    	variables.range=Constants.SEARCH_RANGE[blockNumber][variables.stage];    	    

    	    cbSearchCore(searchData,variables.cDot, 0, variables.range, (short)variables.stage, variables.inverseEnergy, 0, variables.inverseEnergyShifts, 0, variables.crit, 0,variables.cbSearchCoreVariables);
    	    updateBestIndex(updateIndexData,searchData.getCritNew(),searchData.getCritNewSh(),searchData.getIndexNew(),variables.cDot[searchData.getIndexNew()],variables.inverseEnergy[searchData.getIndexNew()],variables.inverseEnergyShifts[searchData.getIndexNew()],variables.updateBestIndexVariables);    	        	    
    	    
    	    variables.sInd=(short)(updateIndexData.getBestIndex()-17);
    	    variables.eInd=(short)(variables.sInd+34);
    	    
    	    if (variables.sInd<0) 
    	    {
    	    	variables.eInd-=variables.sInd;
    	    	variables.sInd=0;
    	    }
    	    
    	    if (variables.eInd>=variables.range) 
    	    {
    	    	variables.eInd=(short)(variables.range-1);
    	    	variables.sInd=(short)(variables.eInd-34);
    	    }

    	    variables.range = Constants.SEARCH_RANGE[blockNumber][variables.stage];

    	    if (vectorLength==40) 
    	    {
    	    	variables.i=variables.sInd;
    	    	
    	    	if (variables.sInd<20) 
    	    	{    	    		    	    	
    	    		if(variables.eInd+20>39)
    	    			augmentCbCorr(variables.target, 10, variables.cbVectors, length, variables.interSamplesFilt, 0, variables.cDot, 0, variables.sInd+20, 39, variables.scale2);
    	    		else
    	    			augmentCbCorr(variables.target, 10, variables.cbVectors, length, variables.interSamplesFilt, 0, variables.cDot, 0, variables.sInd+20, variables.eInd+20, variables.scale2);
    	    		
    	    		variables.i=20;
    	    	}

    	    	if(20-variables.sInd>0)
    	    		variables.currIndex=20-variables.sInd;
    	    	else
    	    		variables.currIndex=0;
    	    	
    	    	crossCorrelation(variables.cDot, variables.currIndex, variables.target, 10, variables.cbVectors, length - 20 - variables.i, (short)vectorLength, (short)(variables.eInd-variables.i+1), variables.scale2, (short)-1,variables.crossCorrelationVariables);    	    	
    	    } 
    	    else 
    	    	crossCorrelation(variables.cDot, 0, variables.target, 10, variables.cbVectors, length - vectorLength - variables.sInd, (short)vectorLength, (short)(variables.eInd-variables.sInd+1), variables.scale2, (short)-1,variables.crossCorrelationVariables);    	    	
    	    
    	    cbSearchCore(searchData,variables.cDot, 0, (short)(variables.eInd-variables.sInd+1), (short)variables.stage, variables.inverseEnergy, variables.baseSize+variables.sInd, variables.inverseEnergyShifts, variables.baseSize+variables.sInd, variables.crit, 0,variables.cbSearchCoreVariables);    	    
    	    updateBestIndex(updateIndexData,searchData.getCritNew(),searchData.getCritNewSh(),(short)(searchData.getIndexNew()+variables.baseSize+variables.sInd),variables.cDot[searchData.getIndexNew()],variables.inverseEnergy[searchData.getIndexNew()+variables.baseSize+variables.sInd],variables.inverseEnergyShifts[searchData.getIndexNew()+variables.baseSize+variables.sInd],variables.updateBestIndexVariables);    	    	

    	    variables.cbIndex[cbIndexIndex+variables.stage] = updateIndexData.getBestIndex();
    	    if(variables.gains[variables.stage]>0)    	    	
    	    	updateIndexData.setBestGain(gainQuant(updateIndexData.getBestGain(), variables.gains[variables.stage], (short)variables.stage, variables.gainIndex, gainIndexIndex+variables.stage,variables.gainQuantVariables));
    	    else
    	    	updateIndexData.setBestGain(gainQuant(updateIndexData.getBestGain(), (short)(0-variables.gains[variables.stage]), (short)variables.stage, variables.gainIndex, gainIndexIndex+variables.stage,variables.gainQuantVariables));

    	    if(vectorLength == 80-EncoderState.STATE_SHORT_LEN) 
    	    {
    	    	if(variables.cbIndex[cbIndexIndex+variables.stage]<variables.baseSize)
    	    	{
    	    		variables.pp=variables.buf;
    	    		variables.ppIndex = 10+length-vectorLength-variables.cbIndex[cbIndexIndex+variables.stage];
    	    	}
    	    	else
    	    	{
    	    		variables.pp=variables.cbVectors;
    	    		variables.ppIndex = length-vectorLength- variables.cbIndex[cbIndexIndex+variables.stage]+variables.baseSize;
    	    	}
    	    	} 
    	    else 
    	    { 
    	    	if (variables.cbIndex[cbIndexIndex+variables.stage]<variables.baseSize) 
    	    	{    	    		
    	    		if (variables.cbIndex[cbIndexIndex+variables.stage]>=20) 
    	    		{
    	    			variables.cbIndex[cbIndexIndex+variables.stage]-=20;
    	    			variables.pp=variables.buf;
    	    			variables.ppIndex = 10+length-vectorLength-variables.cbIndex[cbIndexIndex+variables.stage];
    	    		} 
    	    		else 
    	    		{
    	    			variables.cbIndex[cbIndexIndex+variables.stage]+=(variables.baseSize-20);
    	    			createAugmentVector((short)(variables.cbIndex[cbIndexIndex+variables.stage]-variables.baseSize+40),variables.buf, 10+length, variables.augVec, 0,variables.createAugmentVectorVariables);
    	    			variables.pp=variables.augVec;
    	    			variables.ppIndex = 0;
    	    		}
    	    	} 
    	    	else 
    	    	{
    	    		if ((variables.cbIndex[cbIndexIndex+variables.stage] - variables.baseSize) >= 20) 
    	    		{
    	    			variables.cbIndex[cbIndexIndex+variables.stage]-=20;
    	    			variables.pp=variables.cbVectors;
    	    			variables.ppIndex = length-vectorLength- variables.cbIndex[cbIndexIndex+variables.stage]+variables.baseSize;
    	    		} 
    	    		else 
    	    		{
    	    			variables.cbIndex[cbIndexIndex+variables.stage]+=(variables.baseSize-20);
    	    			createAugmentVector((short)(variables.cbIndex[cbIndexIndex+variables.stage]-2*variables.baseSize+40),variables.cbVectors, length, variables.augVec, 0,variables.createAugmentVectorVariables);
    	    			variables.pp=variables.augVec;
    	    			variables.ppIndex = 0;
    	    		}
    	    	}
    	    }

    	    BasicFunctions.addAffineVectorToVector(variables.target, 10, variables.pp, variables.ppIndex, (short)(0-updateIndexData.getBestGain()), 8192, (short)14, vectorLength);   	    
    	    variables.gains[variables.stage+1] = updateIndexData.getBestGain();    	        	        	   
        }
    	
    	variables.currIndex=10;
    	for (variables.i=0;variables.i<vectorLength;variables.i++)
    		variables.codedVec[variables.i]-=variables.target[variables.currIndex++];
    	
    	variables.codedEner = BasicFunctions.scaleRight(variables.codedVec,0,variables.codedVec,0,vectorLength, variables.scale2);
        
    	variables.j=variables.gainIndex[gainIndexIndex];

    	variables.tempS = BasicFunctions.norm(variables.codedEner);
    	variables.tempS2 = BasicFunctions.norm(variables.targetEner);

    	if(variables.tempS < variables.tempS2)
    		variables.nBits = (short)(16 - variables.tempS);
    	else
    		variables.nBits = (short)(16 - variables.tempS2);    	  

    	if(variables.nBits<0)
    		variables.targetEner = (variables.targetEner<<(0-variables.nBits))*((variables.gains[1]*variables.gains[1])>>14);
    	else
    		variables.targetEner = (variables.targetEner>>variables.nBits)*((variables.gains[1]*variables.gains[1])>>14);
    	
    	variables.gainResult = ((variables.gains[1]-1)<<1);

    	if(variables.nBits<0)
    		variables.tempS=(short)(variables.codedEner<<(-variables.nBits));
    	else
    		variables.tempS=(short)(variables.codedEner>>variables.nBits);
    	
    	for (variables.i=variables.gainIndex[gainIndexIndex];variables.i<32;variables.i++) 
    	{
    	    if ((variables.tempS*Constants.GAIN_SQ5_SQ[variables.i] - variables.targetEner) < 0 && Constants.GAIN_SQ5[variables.j] < variables.gainResult) 
    	    	variables.j=variables.i;      	    	        	    
    	}
    	
    	variables.gainIndex[gainIndexIndex]=(short)variables.j;    	
    }
	
	public static void cbSearchCore(CbSearchData searchData,int[] cDot, int cDotIndex, short range, short stage, short[] inverseEnergy, int inverseEnergyIndex, short[] inverseEnergyShift, int inverseEnergyShiftIndex, int[] crit, int critIndex,CbSearchCoreVariables variables)
    {        
		if (stage==0) 
    	{
    		for (variables.n=0;variables.n<range;variables.n++) 
    	    {
    	    	if(cDot[cDotIndex]<0)
    	    		cDot[cDotIndex]=0;
    	    	
    	    	cDotIndex++;
    	    }
    		cDotIndex-=range;
    	}

		variables.current=0;
    	for(variables.n=0;variables.n<range;variables.n++)
    	{
    		if(cDot[cDotIndex]>0 && cDot[cDotIndex]>variables.current)
    			variables.current=cDot[cDotIndex];
    		else if((0-cDot[cDotIndex])>variables.current)
    			variables.current=0-cDot[cDotIndex];
    		
    		cDotIndex++;
    	}
    	
    	cDotIndex-=range;
    	variables.nBits=BasicFunctions.norm(variables.current);
    	variables.max=Short.MIN_VALUE;

    	for (variables.n=0;variables.n<range;variables.n++) 
    	{
    		variables.tempS = (short)((cDot[cDotIndex++]<<variables.nBits)>>16);    	    
    	    crit[critIndex]=((variables.tempS*variables.tempS)>>16)*inverseEnergy[inverseEnergyIndex++];
    	    
    	    if (crit[critIndex]!=0 && inverseEnergyShift[inverseEnergyShiftIndex]>variables.max)
    	    	variables.max = inverseEnergyShift[inverseEnergyShiftIndex];    	    	
    	    
    	    inverseEnergyShiftIndex++;
    	    critIndex++;
    	}
    	
    	if (variables.max==Short.MIN_VALUE)
    		variables.max = 0;    	  

    	critIndex-=range;
    	inverseEnergyShiftIndex-=range;
    	for (variables.n=0;variables.n<range;variables.n++) 
    	{
    	    if(variables.max-inverseEnergyShift[inverseEnergyShiftIndex]>16)
    	    	crit[critIndex]=crit[critIndex]>>16;    		
    		else
    		{
    			variables.tempS = (short)(variables.max-inverseEnergyShift[inverseEnergyShiftIndex]);    		
    			if(variables.tempS<0)
    				crit[critIndex]<<=-variables.tempS;
    			else
    				crit[critIndex]>>=variables.tempS;
    		}
    	    
    	    inverseEnergyShiftIndex++;
    	    critIndex++;
    	}

    	critIndex-=range;
    	variables.maxCrit=crit[critIndex];
    	critIndex++;
    	searchData.setIndexNew((short)0);
    	for(variables.n=1;variables.n<range;variables.n++)
    	{
    		if(crit[critIndex]>variables.maxCrit)
    		{
    			variables.maxCrit=crit[critIndex];
    			searchData.setIndexNew((short)variables.n);    			
    		}
    		
    		critIndex++;
    	}
    	
    	searchData.setCritNew(variables.maxCrit);
    	searchData.setCritNewSh((short)(32 - 2*variables.nBits + variables.max));    	
    }
	
	public static void cbConstruct(EncoderBits encoderBits,short[] decVector,int decVectorIndex,short[] mem,int memIndex,short length,short vectorLength,int cbIndexIndex,int gainIndexIndex,CbConstructVariables variables)
    {
		variables.reset();
		variables.cbIndex=encoderBits.getCbIndex();
		variables.gainIndex=encoderBits.getGainIndex();
    	
		variables.gain[0] = gainDequant(variables.gainIndex[gainIndexIndex], (short)16384, (short)0);
		variables.gain[1] = gainDequant(variables.gainIndex[gainIndexIndex+1], (short)variables.gain[0], (short)1);
		variables.gain[2] = gainDequant(variables.gainIndex[gainIndexIndex+2], (short)variables.gain[1], (short)2);

    	System.arraycopy(emptyArray, 0, variables.cbVec0, 0, 40);
    	System.arraycopy(emptyArray, 0, variables.cbVec1, 0, 40);
    	System.arraycopy(emptyArray, 0, variables.cbVec2, 0, 40);
    	
    	getCbVec(variables.cbVec0, 0, mem, memIndex, variables.cbIndex[cbIndexIndex], length, vectorLength, variables.getCbVecVariables);
    	getCbVec(variables.cbVec1, 0, mem, memIndex, variables.cbIndex[cbIndexIndex+1], length, vectorLength, variables.getCbVecVariables);
    	getCbVec(variables.cbVec2, 0, mem, memIndex, variables.cbIndex[cbIndexIndex+2], length, vectorLength, variables.getCbVecVariables);
    	    	
		for(variables.i=0;variables.i<vectorLength;variables.i++)
    		decVector[decVectorIndex++]=(short)((variables.gain[0]*variables.cbVec0[variables.i] + variables.gain[1]*variables.cbVec1[variables.i] + variables.gain[2]*variables.cbVec2[variables.i] + 8192)>>14);
    }
	
	public static void stateSearch(EncoderState encoderState,EncoderBits encoderBits,short[] residual,int residualIndex,short[] syntDenum,int syntIndex,short[] weightDenum,int weightIndex,StateSearchVariables variables)
    {
		variables.reset();
		variables.max=0;
    	
    	for(variables.n=0;variables.n<EncoderState.STATE_SHORT_LEN;variables.n++)
    	{
    		variables.tempS=residual[residualIndex++];
    		if(variables.tempS<0)
    			variables.tempS=(short)(0-variables.tempS);
    		
    		if(variables.tempS>variables.max)
    			variables.max=variables.tempS;    		
    	}
    	
    	variables.tempS = (short)(BasicFunctions.getSize(variables.max)-12);
    	if(variables.tempS<0)
    		variables.tempS = 0;
    	      	
    	variables.currIndex=syntIndex+10;
    	for (variables.n=0; variables.n<11; variables.n++)
    		variables.numerator[variables.n] = (short)(syntDenum[variables.currIndex--]>>variables.tempS);
    	
    	residualIndex-=EncoderState.STATE_SHORT_LEN;
    	System.arraycopy(residual, residualIndex, variables.residualLong, 10, EncoderState.STATE_SHORT_LEN);
    	System.arraycopy(emptyArray, 0, variables.residualLong, 10+EncoderState.STATE_SHORT_LEN, EncoderState.STATE_SHORT_LEN);
    	System.arraycopy(emptyArray, 0, variables.residualLongVec, 0, 10);
    	BasicFunctions.filterMA(variables.residualLong,10,variables.sampleMa,0,variables.numerator,0,11,EncoderState.STATE_SHORT_LEN+10);
    	
    	System.arraycopy(emptyArray, 0, variables.sampleMa, EncoderState.STATE_SHORT_LEN + 10, EncoderState.STATE_SHORT_LEN-10);
    	BasicFunctions.filterAR(variables.sampleMa,0,variables.sampleAr,10,syntDenum,syntIndex,11,2*EncoderState.STATE_SHORT_LEN);
    	
    	int arIndex=10;
    	int arIndex2=10+EncoderState.STATE_SHORT_LEN;
    	for(variables.n=0;variables.n<EncoderState.STATE_SHORT_LEN;variables.n++)
    		variables.sampleAr[arIndex++] += variables.sampleAr[arIndex2++];    	  

    	variables.max=0;    	
    	arIndex=10;
    	for(variables.n=0;variables.n<EncoderState.STATE_SHORT_LEN;variables.n++)
    	{
    		variables.tempS2=variables.sampleAr[arIndex++];
    		if(variables.tempS2<0)
    			variables.tempS2=(short)(0-variables.tempS2);
    		
    		if(variables.tempS2>variables.max)
    			variables.max=variables.tempS2;    		
    	}    	
    	
    	/* Find the best index */
    	if ((variables.max<<variables.tempS)<23170)
    		variables.temp=(variables.max*variables.max)<<(2+2*variables.tempS);
    	else
    		variables.temp=Integer.MAX_VALUE;    	  

    	variables.currIndex=0;
    	for (variables.n=0;variables.n<63;variables.n++) 
    	{
    	    if (variables.temp>=Constants.CHOOSE_FRG_QUANT[variables.n])
    	    	variables.currIndex=variables.n+1;
    	    else
    	    	variables.n=63;
    	}
    	
    	encoderBits.setIdxForMax((short)variables.currIndex);    	
    	if (variables.currIndex<27)
    		variables.nBits=4;
    	else
    		variables.nBits=9;    	  

    	BasicFunctions.scaleVector(variables.sampleAr, 10, variables.sampleAr, 10, Constants.SCALE[variables.currIndex], EncoderState.STATE_SHORT_LEN, variables.nBits-variables.tempS);
    	absQuant(encoderBits,variables.sampleAr, 10, weightDenum, weightIndex,variables.absQuantVariables);    	    	
    }
	
	public static void stateConstruct(EncoderBits encoderBits,short[] syntDenum,int syntDenumIndex,short[] outFix,int outFixIndex,int stateLen,StateConstructVariables variables)    
    {
		variables.reset();
    	variables.idxVec=encoderBits.getIdxVec();    	
    	variables.currIndex=syntDenumIndex+10;   
    	
    	for (variables.k=0; variables.k<11; variables.k++)
    		variables.numerator[variables.k] = syntDenum[variables.currIndex--];
    	
    	variables.max = Constants.FRQ_QUANT_MOD[encoderBits.getIdxForMax()];
    	if (encoderBits.getIdxForMax()<37) 
    	{
    		variables.coef=2097152;
    		variables.bitShift=22;    		    	        	   
    	} 
    	else if (encoderBits.getIdxForMax()<59) 
    	{
    		variables.coef=262144;
    		variables.bitShift=19;    	        	   
    	} 
    	else 
    	{
    		variables.coef=65536;
    		variables.bitShift=17;    	        	   
    	}

    	variables.currIndex=10;
    	variables.currIndex2 = stateLen-1;
    	for(variables.k=0; variables.k<stateLen; variables.k++)
    		variables.sampleVal[variables.currIndex++]=(short) ((variables.max*Constants.STATE_SQ3[variables.idxVec[variables.currIndex2--]]+variables.coef) >> variables.bitShift);
    	
    	System.arraycopy(emptyArray, 0, variables.sampleVal, 10+stateLen, stateLen);
    	System.arraycopy(emptyArray, 0, variables.sampleValVec, 0, 10);    	
    	BasicFunctions.filterMA(variables.sampleVal, 10, variables.sampleMa, 10, variables.numerator, 0, 11, 11+stateLen);    	
    	System.arraycopy(emptyArray, 0, variables.sampleMa, 20+stateLen, stateLen-10);    	
    	BasicFunctions.filterAR(variables.sampleMa, 10, variables.sampleAr, 10, syntDenum, syntDenumIndex, 11, 2*stateLen);
    	
    	variables.currIndex=10+stateLen-1;
    	variables.currIndex2=10+2*stateLen-1;
    	for(variables.k=0;variables.k<stateLen;variables.k++)
    		outFix[outFixIndex++]=(short)(variables.sampleAr[variables.currIndex--]+variables.sampleAr[variables.currIndex2--]);    	
    }
	
	public static void filteredCBVecs(short[] cbVectors, int cbVectorsIndex, short[] cbMem, int cbMemIndex, int length,int samples)
    {
		System.arraycopy(emptyArray, 0, cbMem, cbMemIndex+length, 4);
		System.arraycopy(emptyArray, 0, cbMem, cbMemIndex-4, 4);
		System.arraycopy(emptyArray, 0, cbVectors, cbVectorsIndex, length-samples);
		
    	BasicFunctions.filterMA(cbMem, cbMemIndex+4+length-samples, cbVectors, cbVectorsIndex+length-samples, Constants.CB_FILTERS_REV, 0, 8, samples);    	
    }
    
    public static void crossCorrelation(int[] crossCorrelation,int crossCorrelationIndex,short[] seq1, int seq1Index,short[] seq2, int seq2Index,short dimSeq,short dimCrossCorrelation,short rightShifts,short stepSeq2,CrossCorrelationVariables variables)
    {
		for (variables.i = 0; variables.i < dimCrossCorrelation; variables.i++)
        {
            crossCorrelation[crossCorrelationIndex]=0;
            
            for (variables.j = 0; variables.j < dimSeq; variables.j++)
            	crossCorrelation[crossCorrelationIndex] += (seq1[seq1Index++]*seq2[seq2Index++])>>rightShifts;
            
            seq1Index-=dimSeq;
            seq2Index=seq2Index+stepSeq2-dimSeq;
            crossCorrelationIndex++;
        }
    }
       
	public static void updateBestIndex(CbUpdateIndexData updateIndexData,int critNew,short critNewSh,short indexNew,int cDotNew,short inverseEnergyNew,short energyShiftNew,UpdateBestIndexVariables variables)
    {
		variables.current=critNewSh-updateIndexData.getShTotMax();		
		if(variables.current>31)
		{
			variables.shOld=31;
			variables.shNew=0;
		}
		else if(variables.current>0)
		{
			variables.shOld=variables.current;
			variables.shNew=0;
		}
		else if(variables.current>-31)
		{			
			variables.shNew=0-variables.current;
			variables.shOld=0;
		}
		else
		{
			variables.shNew=31;
			variables.shOld=0;
		}
		
    	if ((critNew>>variables.shNew) > (updateIndexData.getCritMax()>>variables.shOld)) 
    	{
    		variables.tempShort = (short)(16 - BasicFunctions.norm(cDotNew));
    		variables.tempScale=(short)(31-energyShiftNew-variables.tempShort);
    		if(variables.tempScale>31)
    			variables.tempScale=31;    	    

    		if(variables.tempShort<0)
    			variables.gain = ((cDotNew<<(-variables.tempShort))*inverseEnergyNew)>>variables.tempScale;    			
    		else
    			variables.gain = ((cDotNew>>variables.tempShort)*inverseEnergyNew)>>variables.tempScale;    		    		
    			
    	    if (variables.gain>bestIndexMaxI)
    	    	updateIndexData.setBestGain(bestIndexMax);    	      
    	    else if (variables.gain<bestIndexMinI) 
    	    	updateIndexData.setBestGain(bestIndexMin); 
    	    else 
    	    	updateIndexData.setBestGain((short)variables.gain);

    	    updateIndexData.setCritMax(critNew);
    	    updateIndexData.setShTotMax(critNewSh);
    	    updateIndexData.setBestIndex(indexNew);    	    
    	}
    }
	
	public static void getCbVec(short[] cbVec,int cbVecIndex,short[] mem,int memIndex,short index,int length,int vectorLength,GetCbVecVariables variables)
    {
		variables.reset();
		variables.baseSize=(short)(length-vectorLength+1);
    	
    	if (vectorLength==40)
    		variables.baseSize+=vectorLength>>1;    	  

    	if (index<length-vectorLength+1) 
    	{
    		variables.k=index+vectorLength;
    	    System.arraycopy(mem, memIndex+length-variables.k, cbVec, cbVecIndex, vectorLength);    	    
    	} 
    	else if (index < variables.baseSize) 
    	{
    		variables.k=2*(index-(length-vectorLength+1))+vectorLength;
    	    createAugmentVector((short)(variables.k>>1),mem,memIndex+length,cbVec,cbVecIndex,variables.createAugmentVectorVariables);    	    
    	}
    	else 
    	{
    	    if (index-variables.baseSize<length-vectorLength+1) 
    	    {
    	    	System.arraycopy(emptyArray, 0, mem, memIndex-4, 4);
    	    	System.arraycopy(emptyArray, 0, mem, memIndex+length, 4);
    	    	BasicFunctions.filterMA(mem, memIndex+length-(index-variables.baseSize+vectorLength)+4, cbVec, cbVecIndex, Constants.CB_FILTERS_REV, 0, 8, vectorLength);    	    	
    	    }
    	    else 
    	    {
    	    	System.arraycopy(emptyArray, 0, mem, memIndex+length, 4);    	    	
    	    	BasicFunctions.filterMA(mem, memIndex+length-vectorLength-1, variables.tempBuffer, 0, Constants.CB_FILTERS_REV, 0, 8, vectorLength+5);
    	    	createAugmentVector((short)((vectorLength<<1)-20+index-variables.baseSize-length-1),variables.tempBuffer,45,cbVec,cbVecIndex,variables.createAugmentVectorVariables);    	    	
    	    }
    	}
    }
	
	public static void createAugmentVector(short index,short[] buf,int bufIndex,short[] cbVec,int cbVecIndex,CreateAugmentVectorVariables variables)
    {
		variables.reset();
		variables.currIndex=cbVecIndex+index-4;
    	
    	System.arraycopy(buf, bufIndex-index, cbVec, cbVecIndex, index);
    	BasicFunctions.multWithRightShift(cbVec, variables.currIndex, buf, bufIndex-index-4, Constants.ALPHA, 0, 4, 15);
    	BasicFunctions.reverseMultiplyRight(variables.cbVecTmp, 0, buf, bufIndex-4, Constants.ALPHA, 3, 4, 15);
    	BasicFunctions.addWithRightShift(cbVec, variables.currIndex, cbVec, variables.currIndex, variables.cbVecTmp, 0, 4, 0);    	
    	System.arraycopy(buf, bufIndex-index, cbVec, cbVecIndex + index, 40-index);    	
    }
	
	public static void energyInverse(short[] energy,int energyIndex,int length)
    {
		int n;
    	for (n=0; n<length; n++)
    	{
    		if(energy[energyIndex]<16384)
    			energy[energyIndex++]=Short.MAX_VALUE;	
    		else
    		{
    			energy[energyIndex]=(short)(0x1FFFFFFF/energy[energyIndex]);
    			energyIndex++;
    		}    		
    	}
    }
	
	public static void energyCalc(int energy, short range, short[] ppi, int ppiIndex, short[] ppo, int ppoIndex, short[] energyArray, int energyArrayIndex, short[] energyShifts, int energyShiftsIndex, short scale, short baseSize)
    {
		int n;
		
    	energyShiftsIndex += 1 + baseSize;    	
    	energyArrayIndex += 1 + baseSize;    	

    	for(n=0;n<range-1;n++) 
    	{
    	    energy += ((ppi[ppiIndex] * ppi[ppiIndex])-(ppo[ppoIndex] * ppo[ppoIndex]))>>scale;
    	    if(energy<0)
    	    	energy=0;    	    

    	    ppiIndex--;
    	    ppoIndex--;

    	    energyShifts[energyShiftsIndex] = BasicFunctions.norm(energy);
    	    energyArray[energyArrayIndex++] = (short)((energy<<energyShifts[energyShiftsIndex])>>16);
    	    energyShiftsIndex++;
    	}
    }
	
	public static void augmentCbCorr(short[] target, int targetIndex, short[] buf, int bufIndex, short[] interpSamples, int interpSamplesIndex, int[] cDot,int cDotIndex,int low,int high,int scale)
    {
		int n;
    	for (n=low; n<=high; n++) 
    	{
    	    cDot[cDotIndex] = BasicFunctions.scaleRight(target, targetIndex, buf, bufIndex-n, n-4, scale);    	    
    		
    	    cDot[cDotIndex]+=BasicFunctions.scaleRight(target, targetIndex+n-4, interpSamples, interpSamplesIndex, 4, scale);    	    
    	    interpSamplesIndex += 4;

    	    cDot[cDotIndex]+=BasicFunctions.scaleRight(target, targetIndex+n, buf, bufIndex-n, 40-n, scale);
    	    cDotIndex++;
    	}
    }
	
	public static void absQuant(EncoderBits encoderBits,short[] in,int inIndex,short[] weightDenum, int weightDenumIndex,AbsQuantVariables variables)
    {
		variables.reset();
    	if (encoderBits.getStateFirst()) 
    	{
    		variables.quantLen[0]=40;
    		variables.quantLen[1]=(short)(EncoderState.STATE_SHORT_LEN-40);
    	} 
    	else 
    	{
    		variables.quantLen[0]=(short)(EncoderState.STATE_SHORT_LEN-40);
    		variables.quantLen[1]=40;
    	}

    	BasicFunctions.filterAR(in, inIndex, variables.inWeighted, 10, weightDenum, weightDenumIndex, 11, variables.quantLen[0]);
    	BasicFunctions.filterAR(in, inIndex+variables.quantLen[0], variables.inWeighted, 10+variables.quantLen[0], weightDenum, weightDenumIndex+11, 11, variables.quantLen[1]);    	

    	absQUantLoop(encoderBits,variables.syntOutBuf, 10, variables.inWeighted, 10,weightDenum, weightDenumIndex, variables.quantLen, 0,variables.absQuantLoopVariables);
    }  
	
	public static void absQUantLoop(EncoderBits encoderBits,short[] syntOut, int syntOutIndex, short[] inWeighted, int inWeightedIndex, short[] weightDenum, int weightDenumIndex, short[] quantLen, int quantLenIndex,AbsQuantLoopVariables variables)
    {
    	variables.idxVec=encoderBits.getIdxVec();    	
    	variables.startIndex=0;
    	
    	for(variables.i=0;variables.i<2;variables.i++) 
    	{
    		variables.currIndex=quantLenIndex+variables.i;    		
        	for(variables.j=0;variables.j<quantLen[variables.currIndex];variables.j++)
    	    {
    	    	BasicFunctions.filterAR(syntOut, syntOutIndex, syntOut, syntOutIndex, weightDenum, weightDenumIndex, 11, 1);    	    	

    	    	variables.temp = inWeighted[inWeightedIndex] - syntOut[syntOutIndex];
    	    	variables.temp2 = variables.temp<<2;
    	    	
    	    	if (variables.temp2 > Short.MAX_VALUE)
    	    		variables.temp2 = Short.MAX_VALUE;
    	    	else if (variables.temp2 < Short.MIN_VALUE)
    	    		variables.temp2 = Short.MIN_VALUE;    	      

    	    	if (variables.temp< -7577) 
    	    	{
    	    		variables.idxVec[variables.startIndex + variables.j] = (short)0;
	    	    	syntOut[syntOutIndex++] = (short)(((Constants.STATE_SQ3[0] + 2) >> 2) + inWeighted[inWeightedIndex++] - variables.temp);
	      		}
    	      	else if (variables.temp>8151) 
    	      	{
    	      		variables.idxVec[variables.startIndex + variables.j] = (short)7;
	    	    	syntOut[syntOutIndex++] = (short)(((Constants.STATE_SQ3[7] + 2) >> 2) + inWeighted[inWeightedIndex++] - variables.temp);
	      		}
    	      	else 
    	      	{
    	      		if (variables.temp2 <= Constants.STATE_SQ3[0])
    	      		{
    	      			variables.idxVec[variables.startIndex + variables.j] = (short)0;
    	    	    	syntOut[syntOutIndex++] = (short)(((Constants.STATE_SQ3[0] + 2) >> 2) + inWeighted[inWeightedIndex++] - variables.temp);
    	      		}
    	      		else 
    	      		{
    	      			variables.k = 0;
    	      		    while(variables.temp2 > Constants.STATE_SQ3[variables.k] && variables.k<Constants.STATE_SQ3.length-1)
    	      		    	variables.k++;

    	      		    if (variables.temp2 > ((Constants.STATE_SQ3[variables.k] + Constants.STATE_SQ3[variables.k - 1] + 1)>>1))
    	      		    {
    	      		    	variables.idxVec[variables.startIndex + variables.j] = (short)variables.k;
        	    	    	syntOut[syntOutIndex++] = (short)(((Constants.STATE_SQ3[variables.k] + 2) >> 2) + inWeighted[inWeightedIndex++] - variables.temp);
        	      		}
        	      		else
        	      		{
        	      			variables.idxVec[variables.startIndex + variables.j] = (short)(variables.k-1);
        	    	    	syntOut[syntOutIndex++] = (short)(((Constants.STATE_SQ3[variables.k-1] + 2) >> 2) + inWeighted[inWeightedIndex++] - variables.temp);
        	      		}        	      		   	      		   
    	      		}    	      		
    	      	}
    	    	
    	    	    	    	
    	    }
    	    
        	variables.startIndex+=quantLen[variables.currIndex];
        	weightDenumIndex += 11;
    	}
    }
	
	public static void interpolateSamples(short[] interpSamples,int interpSamplesIndex,short[] cbMem,int cbMemIndex,int length,InterpolateSamplesVariables variables)
    {		
    	for (variables.n=0; variables.n<20; variables.n++) 
    	{
    		variables.highIndex = cbMemIndex+length-4;
    		variables.lowIndex = cbMemIndex+length-variables.n-24;
    	    
    	    interpSamples[interpSamplesIndex++] = (short)(((Constants.ALPHA[3]*cbMem[variables.highIndex++])>>15) + ((Constants.ALPHA[0]*cbMem[variables.lowIndex++])>>15));
    	    interpSamples[interpSamplesIndex++] = (short)(((Constants.ALPHA[2]*cbMem[variables.highIndex++])>>15) + ((Constants.ALPHA[1]*cbMem[variables.lowIndex++])>>15));
  	      	interpSamples[interpSamplesIndex++] = (short)(((Constants.ALPHA[1]*cbMem[variables.highIndex++])>>15) + ((Constants.ALPHA[2]*cbMem[variables.lowIndex++])>>15));
  	      	interpSamples[interpSamplesIndex++] = (short)(((Constants.ALPHA[0]*cbMem[variables.highIndex++])>>15) + ((Constants.ALPHA[3]*cbMem[variables.lowIndex++])>>15));  	      	    	        	      
    	}
    }
	
	public static short frameClassify(short[] residual,FrameClassifyVariables variables)
    {
		variables.reset();
		variables.max=0;
    	
    	for(variables.n=0;variables.n<EncoderState.SIZE;variables.n++)
    	{
    		variables.tempS=residual[variables.n];
    		if(variables.tempS<0)
    			variables.tempS=(short)(0-variables.tempS);
    		
    		if(variables.tempS>variables.max)
    			variables.max=variables.tempS;    		
    	}
    	
    	variables.scale=(short)(BasicFunctions.getSize(variables.max*variables.max)-24);
    	if(variables.scale<0)
    		variables.scale=0;    	

    	variables.ssqIndex=2;
    	variables.currIndex=0;    	
    	for (variables.n=EncoderState.SUBFRAMES-1; variables.n>0; variables.n--) 
    	{
    		variables.ssqEn[variables.currIndex++] = BasicFunctions.scaleRight(residual,variables.ssqIndex, residual,variables.ssqIndex, 76, variables.scale);
    		variables.ssqIndex += 40;    	    
    	}

    	/* Scale to maximum 20 bits in order to allow for the 11 bit window */
    	variables.ssqIndex=0;
    	int maxSSq=variables.ssqEn[0];	
    	for(variables.n=1;variables.n<EncoderState.SUBFRAMES-1;variables.n++)
    	{
    		if(variables.ssqEn[variables.n]>maxSSq)
    			maxSSq=variables.ssqEn[variables.n];    		
    	}
    	
    	variables.scale = (short)(BasicFunctions.getSize(maxSSq) - 20);
    	if(variables.scale<0)
    		variables.scale = 0;

    	variables.ssqIndex=0;    	
    	variables.currIndex=1;    	    
    	
    	for (variables.n=EncoderState.SUBFRAMES-1; variables.n>0; variables.n--) 
    	{
    		variables.ssqEn[variables.ssqIndex]=(variables.ssqEn[variables.ssqIndex]>>variables.scale)*Constants.ENG_START_SEQUENCE[variables.currIndex++];
    		variables.ssqIndex++;    	    
    	}

    	/* Extract the best choise of start state */
    	variables.currIndex=0;
    	maxSSq=variables.ssqEn[0];	
    	for(variables.n=1;variables.n<EncoderState.SUBFRAMES-1;variables.n++)
    	{
    		if(variables.ssqEn[variables.n]>maxSSq)
    		{
    			variables.currIndex=variables.n;
    			maxSSq=variables.ssqEn[variables.n];    			
    		}    		    		
    	}
    	
    	return (short)(variables.currIndex + 1);
    }   
	
	public static void packBits(EncoderState encoderState,EncoderBits encoderBits,byte[] result,PackBitsVariables variables)
    {   
		variables.lsf=encoderBits.getLSF();
		variables.cbIndex=encoderBits.getCbIndex();
		variables.gainIndex=encoderBits.getGainIndex();
		variables.idxVec=encoderBits.getIdxVec();
    	
    	result[0]=(byte)((variables.lsf[0]<<2) | ((variables.lsf[1]>>5) & 0x3));
    	result[1]=(byte)((variables.lsf[1] & 0x1F)<<3 | ((variables.lsf[2]>>4) & 0x7));
    	result[2]=(byte)((variables.lsf[2] & 0xF)<<4);    	
    	    	
    	if (EncoderState.ENCODER_MODE==20) 
    	{    		
    		if(encoderBits.getStateFirst())
    			result[2]|=(encoderBits.getStartIdx()&0x3)<<2 | 0x2;
    		else
    			result[2]|=(encoderBits.getStartIdx()&0x3)<<2;

    		result[2]|=(encoderBits.getIdxForMax()>>5) & 0x1;    		
    		result[3]=(byte)(((encoderBits.getIdxForMax() & 0x1F)<<3) | ((variables.cbIndex[0]>>4) & 0x7));
    	    result[4]=(byte)(((variables.cbIndex[0] & 0xE)<<4) | (variables.gainIndex[0] & 0x18) | ((variables.gainIndex[1] & 0x8)>>1) | ((variables.cbIndex[3]>>6) & 0x3));
    		result[5]=(byte)(((variables.cbIndex[3] & 0x3E)<<2) | ((variables.gainIndex[3]>>2) & 0x4)  | ((variables.gainIndex[4]>>2) & 0x2) | ((variables.gainIndex[6]>>4) & 0x1));
    		variables.resIndex=6;
    	} 
    	else 
    	{ 
    		result[2]|=(variables.lsf[3]>>2) & 0xF;
    		result[3]=(byte)(((variables.lsf[3] & 0x3)<<6) | ((variables.lsf[4]>>1) & 0x3F));
    		result[4]=(byte)(((variables.lsf[4] & 0x1)<<7) | (variables.lsf[5] & 0x7F));
    		if(encoderBits.getStateFirst())
    			result[5]=(byte)((encoderBits.getStartIdx()<<5) | 0x10 | (encoderBits.getIdxForMax()>>2) & 0xF); 
    		else
    			result[5]=(byte)((encoderBits.getStartIdx()<<5) | (encoderBits.getIdxForMax()>>2) & 0xF);
    		
    		result[6]=(byte)(((encoderBits.getIdxForMax() & 0x3)<<6) | ((variables.cbIndex[0]&0x78)>>1) | ((variables.gainIndex[0]&0x10)>>3) | ((variables.gainIndex[1]&0x80)>>3)); 
    		result[7]=(byte)(variables.cbIndex[3]&0xFC | ((variables.gainIndex[3] & 0x10)>>3) | ((variables.gainIndex[4] & 0x80)>>3));
    		variables.resIndex=8;
    	}
    	
    	variables.idxVecIndex=0;
    	for (variables.k=0; variables.k<7; variables.k++) 
    	{
    		result[variables.resIndex]=0;
    	    for (variables.i=7; variables.i>=0; variables.i--) 
    	    	result[variables.resIndex] |= (((variables.idxVec[variables.idxVecIndex++] & 0x4)>>2)<<variables.i);
    	    
    	    variables.resIndex++;    	    
    	}
    	
    	result[variables.resIndex] = (byte)((variables.idxVec[variables.idxVecIndex++] & 0x4)<<5);
    	
    	if (EncoderState.ENCODER_MODE==20) 
    	{
    		result[variables.resIndex] |= (variables.gainIndex[1] & 0x4)<<4;
    		result[variables.resIndex] |= (variables.gainIndex[3] & 0xC)<<2;
    		result[variables.resIndex] |= (variables.gainIndex[4] & 0x4)<<1;
    		result[variables.resIndex] |= (variables.gainIndex[6] & 0x8)>>1;
    		result[variables.resIndex] |= (variables.gainIndex[7] & 0xC)>>2;
    	}
    	else
    	{
    		result[variables.resIndex] |= (variables.idxVec[variables.idxVecIndex++] & 0x4)<<4;
    		result[variables.resIndex] |= (variables.cbIndex[0] & 0x6)<<3;
    		result[variables.resIndex] |= (variables.gainIndex[0] & 0x8);
    		result[variables.resIndex] |= (variables.gainIndex[1] & 0x4);
    		result[variables.resIndex] |= (variables.cbIndex[3] & 0x2);
    		result[variables.resIndex] |= (variables.cbIndex[6] & 0x80)>>7;

    		variables.resIndex++;
    		result[variables.resIndex] = (byte)((variables.cbIndex[6] & 0x7E)<<1 | (variables.cbIndex[9] & 0xC0)>>6);
    		
    		variables.resIndex++;
    		result[variables.resIndex] = (byte)((variables.cbIndex[9] & 0x3E)<<2 | (variables.cbIndex[12] & 0xE0)>>5);
    		    		    	    
    		variables.resIndex++;
    		result[variables.resIndex] = (byte)((variables.cbIndex[12] & 0x1E)<<3 | (variables.gainIndex[3] & 0xC) | (variables.gainIndex[4] & 0x6)>>1);
    		
    		variables.resIndex++;
    		result[variables.resIndex] = (byte)((variables.gainIndex[6] & 0x18)<<3 | (variables.gainIndex[7] & 0xC)<<2 | (variables.gainIndex[9] & 0x10)>>1 | (variables.gainIndex[10] & 0x8)>>1 | (variables.gainIndex[12] & 0x10)>>3 | (variables.gainIndex[13] & 0x8)>>3);    	    
    	}
    	
    	variables.idxVecIndex=0;
    	variables.resIndex++;
    	for (variables.k=0; variables.k<14; variables.k++) 
    	{
    		result[variables.resIndex]=0;
    		
    		for (variables.i=6; variables.i>=0; variables.i-=2) 
    	    	result[variables.resIndex] |= ((variables.idxVec[variables.idxVecIndex++] & 0x3)<<variables.i);
    	    
    		variables.resIndex++;
    	}
    	
    	if (EncoderState.ENCODER_MODE==20) 
    	{
    	    result[variables.resIndex++] =(byte)((variables.idxVec[56]& 0x3)<<6 | (variables.cbIndex[0] & 0x1)<<5 | (variables.cbIndex[1] & 0x7C)>>2);
    		result[variables.resIndex++] =(byte)(((variables.cbIndex[1]& 0x3)<<6) | ((variables.cbIndex[2]>>1) & 0x3F));
    			    	    
    	    result[variables.resIndex++] =(byte)((variables.cbIndex[2] & 0x1)<<7 | (variables.gainIndex[0] & 0x7)<<4 | (variables.gainIndex[1] & 0x3)<<2 | (variables.gainIndex[2] & 0x6)>>1);
    	    result[variables.resIndex++] =(byte)((variables.gainIndex[2] & 0x1)<<7 | (variables.cbIndex[3] & 0x1)<<6 | (variables.cbIndex[4] & 0x7E)>>1);  
    	    
    	    result[variables.resIndex++] = (byte)((variables.cbIndex[4] & 0x1)<<7 | (variables.cbIndex[5] & 0x7F));
    	    result[variables.resIndex++] = (byte)(variables.cbIndex[6] & 0xFF);
    	    
    	    result[variables.resIndex++] = (byte)(variables.cbIndex[7] & 0xFF);
    	    result[variables.resIndex++] = (byte)(variables.cbIndex[8] & 0xFF);
    	    
    	    result[variables.resIndex++] = (byte)(((variables.gainIndex[3] & 0x3)<<6) | ((variables.gainIndex[4] & 0x3)<<4) | ((variables.gainIndex[5] & 0x7)<<1) | ((variables.gainIndex[6] & 0x4)>>2));
    	    result[variables.resIndex++] = (byte)(((variables.gainIndex[6] & 0x3)<<6) | ((variables.gainIndex[7] & 0x3)<<4) | ((variables.gainIndex[8] & 0x7)<<1));     	    	    	    
    	} 
    	else 
    	{ 
    		result[variables.resIndex++] = (byte)((variables.idxVec[56] & 0x3)<<6 | (variables.idxVec[57] & 0x3)<<4 | (variables.cbIndex[0] & 0x1)<<3 | (variables.cbIndex[1] & 0x70)>>4);
    		result[variables.resIndex++] = (byte)((variables.cbIndex[1] & 0xF)<<4 | (variables.cbIndex[2] & 0x78)>>3); 
    		
    		result[variables.resIndex++] = (byte)((variables.cbIndex[2] & 0x7)<<5 | (variables.gainIndex[0] & 0x7)<<2 | (variables.gainIndex[1] & 0x3));
    		result[variables.resIndex++] = (byte)((variables.gainIndex[2] & 0x7)<<7 | (variables.cbIndex[3] & 0x1)<<4 | (variables.cbIndex[4] & 0x78)>>3);
    		
    		result[variables.resIndex++] = (byte)((variables.cbIndex[4] & 0x7)<<5 | (variables.cbIndex[5] & 0x7C)>>2);
    		result[variables.resIndex++] = (byte)((variables.cbIndex[5] & 0x3)<<6 | (variables.cbIndex[6] & 0x1)<<1 | (variables.cbIndex[7] & 0xF8)>>3); 
    	    
    		result[variables.resIndex++] = (byte)((variables.cbIndex[7] & 0x7)<<5 | (variables.cbIndex[8] & 0xF8)>>3);
    		result[variables.resIndex++] = (byte)((variables.cbIndex[8] & 0x7)<<5 | (variables.cbIndex[9] & 0x1)<<4 | (variables.cbIndex[10] & 0xF0)>>4);  
    		
    	    result[variables.resIndex++] = (byte)((variables.cbIndex[10] & 0xF)<<4 | (variables.cbIndex[11] & 0xF0)>>4);
    		result[variables.resIndex++] = (byte)((variables.cbIndex[11] & 0xF)<<4 | (variables.cbIndex[12] & 0x1)<<3 | (variables.cbIndex[13] & 0xE0)>>5);  
    	    
    		result[variables.resIndex++] = (byte)((variables.cbIndex[13] & 0x1F)<<3 | (variables.cbIndex[14] & 0xE0)>>5);  
    		result[variables.resIndex++] = (byte)((variables.cbIndex[14] & 0x1F)<<3 | (variables.gainIndex[3] & 0x3)<<1 | (variables.gainIndex[4] & 0x1));
    	    
    		result[variables.resIndex++] = (byte)((variables.gainIndex[5] & 0x7)<<5 | (variables.gainIndex[6] & 0x7)<<2 | (variables.gainIndex[7] & 0x3)); 
    		result[variables.resIndex++] = (byte)((variables.gainIndex[8] & 0x7)<<5 | (variables.gainIndex[9] & 0xF)<<1 | (variables.gainIndex[10] & 0x4)>>2); 
    			
    	    result[variables.resIndex++] = (byte)((variables.gainIndex[10] & 0x3)<<6 | (variables.gainIndex[11] & 0x7)<<3 | (variables.gainIndex[12] & 0xE)>>1); 
    		result[variables.resIndex++] = (byte)((variables.gainIndex[12] & 0x1)<<7 | (variables.gainIndex[13] & 0x7)<<4 | (variables.gainIndex[14] & 0x7)<<1);     		
    	}
    }
	
	public static void unpackBits(EncoderBits encoderBits,short[] data,int mode,UnpackBitsVariables variables) 
    {
		variables.lsf=encoderBits.getLSF();
		variables.cbIndex=encoderBits.getCbIndex();
		variables.gainIndex=encoderBits.getGainIndex();
		variables.idxVec=encoderBits.getIdxVec();
    	
    	variables.reset();
    	
    	/* First WebRtc_Word16 */
    	variables.lsf[0]  = (short)((data[variables.tempIndex1]>>10) & 0x3F);       /* Bit 0..5  */
    	variables.lsf[1]  = (short)((data[variables.tempIndex1]>>3)&0x7F);      /* Bit 6..12 */
    	variables.lsf[2]  = (short)((data[variables.tempIndex1]&0x7)<<4);      /* Bit 13..15 */
    	  
    	variables.tempIndex1++;
    	/* Second WebRtc_Word16 */
    	variables.lsf[2] |= (data[variables.tempIndex1]>>12)&0xF;      /* Bit 0..3  */

    	if (mode==20) 
    	{
    		encoderBits.setStartIdx((short)((data[variables.tempIndex1]>>10)&0x3));  /* Bit 4..5  */
    	    
    	    encoderBits.setStateFirst(false);
    	    if(((data[variables.tempIndex1]>>9)&0x1)!=0)
    	    	encoderBits.setStateFirst(true);  /* Bit 6  */
    	    
    	    encoderBits.setIdxForMax((short)((data[variables.tempIndex1]>>3)&0x3F));  /* Bit 7..12 */
    	    variables.cbIndex[0] = (short)((data[variables.tempIndex1]&0x7)<<4);  /* Bit 13..15 */
    	    variables.tempIndex1++;
    	    /* Third WebRtc_Word16 */
    	    variables.cbIndex[0] |= (data[variables.tempIndex1]>>12)&0xE;  /* Bit 0..2  */
    	    variables.gainIndex[0] = (short)((data[variables.tempIndex1]>>8)&0x18);  /* Bit 3..4  */
    	    variables.gainIndex[1] = (short)((data[variables.tempIndex1]>>7)&0x8);  /* Bit 5  */
    	    variables.cbIndex[3] = (short)((data[variables.tempIndex1]>>2)&0xFE);  /* Bit 6..12 */
    	    variables.gainIndex[3] = (short)((data[variables.tempIndex1]<<2)&0x10);  /* Bit 13  */
    	    variables.gainIndex[4] = (short)((data[variables.tempIndex1]<<2)&0x8);  /* Bit 14  */
    	    variables.gainIndex[6] = (short)((data[variables.tempIndex1]<<4)&0x10);  /* Bit 15  */
    	} 
    	else 
    	{ /* mode==30 */
    		variables.lsf[3] = (short)((data[variables.tempIndex1]>>6)&0x3F);  /* Bit 4..9  */
    		variables.lsf[4] = (short)((data[variables.tempIndex1]<<1)&0x7E);  /* Bit 10..15 */
    		variables.tempIndex1++;
    	    /* Third WebRtc_Word16 */
    		variables.lsf[4] |= (data[variables.tempIndex1]>>15)&0x1;  /* Bit 0  */
    	    variables.lsf[5] = (short)((data[variables.tempIndex1]>>8)&0x7F);  /* Bit 1..7  */
    	    encoderBits.setStartIdx((short)((data[variables.tempIndex1]>>5)&0x7));  /* Bit 8..10 */
    	    
    	    encoderBits.setStateFirst(false);
    	    if((short)((data[variables.tempIndex1]>>4)&0x1)!=0)
    	    	encoderBits.setStateFirst(true);/* Bit 11  */
    	      
    	    variables.tempS=(short)((data[variables.tempIndex1]<<2)&0x3C);/* Bit 12..15 */
    	    variables.tempIndex1++;    	     
    	    
    	    /* 4:th WebRtc_Word16 */
    	    variables.tempS |= (data[variables.tempIndex1]>>14)&0x3;  /* Bit 0..1  */
    	    encoderBits.setIdxForMax(variables.tempS);
    	    variables.cbIndex[0] = (short)((data[variables.tempIndex1]>>7)&0x78);  /* Bit 2..5  */
    	    variables.gainIndex[0] = (short)((data[variables.tempIndex1]>>5)&0x10);  /* Bit 6  */
    	    variables.gainIndex[1] = (short)((data[variables.tempIndex1]>>5)&0x8);  /* Bit 7  */
    	    variables.cbIndex[3] = (short)((data[variables.tempIndex1])&0xFC);  /* Bit 8..13 */
    	    variables.gainIndex[3] = (short)((data[variables.tempIndex1]<<3)&0x10);  /* Bit 14  */
    	    variables.gainIndex[4] = (short)((data[variables.tempIndex1]<<3)&0x8);  /* Bit 15  */
    	}
    	
    	/* Class 2 bits of ULP */
    	/* 4:th to 6:th WebRtc_Word16 for 20 ms case
    	   5:th to 7:th WebRtc_Word16 for 30 ms case */
    	variables.tempIndex1++;
    	variables.tempIndex2=0;
    	  
    	for (variables.k=0; variables.k<3; variables.k++) 
    	{
    	   for (variables.i=15; variables.i>=0; variables.i--) 
    		   variables.idxVec[variables.tempIndex2++] = (short)(((data[variables.tempIndex1]>>variables.i)<<2)&0x4);/* Bit 15-i  */    	      
    	    
    	   variables.tempIndex1++;
    	}

    	if (mode==20) 
    	{
    	    /* 7:th WebRtc_Word16 */
    	    for (variables.i=15; variables.i>6; variables.i--)
    	    	variables.idxVec[variables.tempIndex2++] = (short)(((data[variables.tempIndex1]>>variables.i)<<2)&0x4); /* Bit 15-i  */    	    
    	    
    	    variables.gainIndex[1] |= (data[variables.tempIndex1]>>4)&0x4; /* Bit 9  */
    	    variables.gainIndex[3] |= (data[variables.tempIndex1]>>2)&0xC; /* Bit 10..11 */
    	    variables.gainIndex[4] |= (data[variables.tempIndex1]>>1)&0x4; /* Bit 12  */
    	    variables.gainIndex[6] |= (data[variables.tempIndex1]<<1)&0x8; /* Bit 13  */
    	    variables.gainIndex[7] = (short)((data[variables.tempIndex1]<<2)&0xC); /* Bit 14..15 */

    	} 
    	else 
    	{   /* mode==30 */
    	    /* 8:th WebRtc_Word16 */
    	    for (variables.i=15; variables.i>5;variables. i--)
    	    	variables.idxVec[variables.tempIndex2++] = (short)(((data[variables.tempIndex1]>>variables.i)<<2)&0x4);/* Bit 15-i  */    	      
    	    
    	    variables.cbIndex[0] |= (data[variables.tempIndex1]>>3)&0x6; /* Bit 10..11 */
    	    variables.gainIndex[0] |= (data[variables.tempIndex1])&0x8;  /* Bit 12  */
    	    variables.gainIndex[1] |= (data[variables.tempIndex1])&0x4;  /* Bit 13  */
    	    variables.cbIndex[3] |= (data[variables.tempIndex1])&0x2;  /* Bit 14  */
    	    variables.cbIndex[6] = (short)((data[variables.tempIndex1]<<7)&0x80); /* Bit 15  */
    	    variables.tempIndex1++;
    	    /* 9:th WebRtc_Word16 */
    	    variables.cbIndex[6] |= (data[variables.tempIndex1]>>9)&0x7E; /* Bit 0..5  */
    	    variables.cbIndex[9] = (short)((data[variables.tempIndex1]>>2)&0xFE); /* Bit 6..12 */
    	    variables.cbIndex[12] = (short)((data[variables.tempIndex1]<<5)&0xE0); /* Bit 13..15 */
    	    variables.tempIndex1++;
    	    /* 10:th WebRtc_Word16 */
    	    variables.cbIndex[12] |= (data[variables.tempIndex1]>>11)&0x1E;/* Bit 0..3 */
    	    variables.gainIndex[3] |= (data[variables.tempIndex1]>>8)&0xC; /* Bit 4..5  */
    	    variables.gainIndex[4] |= (data[variables.tempIndex1]>>7)&0x6; /* Bit 6..7  */
    	    variables.gainIndex[6] = (short)((data[variables.tempIndex1]>>3)&0x18); /* Bit 8..9  */
    	    variables.gainIndex[7] = (short)((data[variables.tempIndex1]>>2)&0xC); /* Bit 10..11 */
    	    variables.gainIndex[9] = (short)((data[variables.tempIndex1]<<1)&0x10); /* Bit 12  */
    	    variables.gainIndex[10] = (short)((data[variables.tempIndex1]<<1)&0x8); /* Bit 13  */
    	    variables.gainIndex[12] = (short)((data[variables.tempIndex1]<<3)&0x10); /* Bit 14  */
    	    variables.gainIndex[13] = (short)((data[variables.tempIndex1]<<3)&0x8); /* Bit 15  */
    	}
    	variables.tempIndex1++;
    	  
    	/* Class 3 bits of ULP */
    	/* 8:th to 14:th WebRtc_Word16 for 20 ms case
    	   11:th to 17:th WebRtc_Word16 for 30 ms case */
    	variables.tempIndex2=0;
    	for (variables.k=0; variables.k<7; variables.k++) 
    	{
    	    for (variables.i=14; variables.i>=0; variables.i-=2)
    	    	variables.idxVec[variables.tempIndex2++] |= (data[variables.tempIndex1]>>variables.i)&0x3; /* Bit 15-i..14-i*/
    	      
    	    variables.tempIndex1++;
    	}

    	if (mode==20) 
    	{
    	    /* 15:th WebRtc_Word16 */
    		variables.idxVec[56] |= (data[variables.tempIndex1]>>14)&0x3; /* Bit 0..1  */
    		variables.cbIndex[0] |= (data[variables.tempIndex1]>>13)&0x1; /* Bit 2  */
    		variables.cbIndex[1] = (short)((data[variables.tempIndex1]>>6)&0x7F); /* Bit 3..9  */
    		variables.cbIndex[2] = (short)((data[variables.tempIndex1]<<1)&0x7E); /* Bit 10..15 */
    		variables.tempIndex1++;
    	    /* 16:th WebRtc_Word16 */
    		variables.cbIndex[2] |= (data[variables.tempIndex1]>>15)&0x1; /* Bit 0  */
    	    variables.gainIndex[0] |= (data[variables.tempIndex1]>>12)&0x7; /* Bit 1..3  */
    	    variables.gainIndex[1] |= (data[variables.tempIndex1]>>10)&0x3; /* Bit 4..5  */
    	    variables.gainIndex[2] = (short)((data[variables.tempIndex1]>>7)&0x7); /* Bit 6..8  */
    	    variables.cbIndex[3] |= (data[variables.tempIndex1]>>6)&0x1; /* Bit 9  */
    	    variables.cbIndex[4] = (short)((data[variables.tempIndex1]<<1)&0x7E); /* Bit 10..15 */
    	    variables.tempIndex1++;
    	    /* 17:th WebRtc_Word16 */
    	    variables.cbIndex[4] |= (data[variables.tempIndex1]>>15)&0x1; /* Bit 0  */
    	    variables.cbIndex[5] = (short)((data[variables.tempIndex1]>>8)&0x7F); /* Bit 1..7  */
    	    variables.cbIndex[6] = (short)((data[variables.tempIndex1])&0xFF); /* Bit 8..15 */
    	    variables.tempIndex1++;
    	    /* 18:th WebRtc_Word16 */
    	    variables.cbIndex[7] = (short)((data[variables.tempIndex1]>>8) & 0xFF);  /* Bit 0..7  */
    	    variables.cbIndex[8] = (short)(data[variables.tempIndex1]&0xFF);  /* Bit 8..15 */
    	    variables.tempIndex1++;
    	    /* 19:th WebRtc_Word16 */
    	    variables.gainIndex[3] |= (data[variables.tempIndex1]>>14)&0x3; /* Bit 0..1  */
    	    variables.gainIndex[4] |= (data[variables.tempIndex1]>>12)&0x3; /* Bit 2..3  */
    	    variables.gainIndex[5] = (short)((data[variables.tempIndex1]>>9)&0x7); /* Bit 4..6  */
    	    variables.gainIndex[6] |= (data[variables.tempIndex1]>>6)&0x7; /* Bit 7..9  */
    	    variables.gainIndex[7] |= (data[variables.tempIndex1]>>4)&0x3; /* Bit 10..11 */
    	    variables.gainIndex[8] = (short)((data[variables.tempIndex1]>>1)&0x7); /* Bit 12..14 */
    	} 
    	else 
    	{   /* mode==30 */
    	    /* 18:th WebRtc_Word16 */
    		variables.idxVec[56] |= (data[variables.tempIndex1]>>14)&0x3; /* Bit 0..1  */
    		variables.idxVec[57] |= (data[variables.tempIndex1]>>12)&0x3; /* Bit 2..3  */
    		variables.cbIndex[0] |= (data[variables.tempIndex1]>>11)&1; /* Bit 4  */
    		variables.cbIndex[1] = (short)((data[variables.tempIndex1]>>4)&0x7F); /* Bit 5..11 */
    		variables.cbIndex[2] = (short)((data[variables.tempIndex1]<<3)&0x78); /* Bit 12..15 */
    		variables.tempIndex1++;
    	    /* 19:th WebRtc_Word16 */
    		variables.cbIndex[2] |= (data[variables.tempIndex1]>>13)&0x7; /* Bit 0..2  */
    		variables.gainIndex[0] |= (data[variables.tempIndex1]>>10)&0x7; /* Bit 3..5  */
    		variables.gainIndex[1] |= (data[variables.tempIndex1]>>8)&0x3; /* Bit 6..7  */
    		variables.gainIndex[2] = (short)((data[variables.tempIndex1]>>5)&0x7); /* Bit 8..10 */
    		variables.cbIndex[3] |= (data[variables.tempIndex1]>>4)&0x1; /* Bit 11  */
    		variables.cbIndex[4] = (short)((data[variables.tempIndex1]<<3)&0x78); /* Bit 12..15 */
    		variables.tempIndex1++;
    	    /* 20:th WebRtc_Word16 */
    		variables.cbIndex[4] |= (data[variables.tempIndex1]>>13)&0x7; /* Bit 0..2  */
    		variables.cbIndex[5] = (short)((data[variables.tempIndex1]>>6)&0x7F); /* Bit 3..9  */
    		variables.cbIndex[6] |= (data[variables.tempIndex1]>>5)&0x1; /* Bit 10  */
    		variables.cbIndex[7] = (short)((data[variables.tempIndex1]<<3)&0xF8); /* Bit 11..15 */
    		variables.tempIndex1++;
    	    /* 21:st WebRtc_Word16 */
    		variables.cbIndex[7] |= (data[variables.tempIndex1]>>13)&0x7; /* Bit 0..2  */
    		variables.cbIndex[8] = (short)((data[variables.tempIndex1]>>5)&0xFF); /* Bit 3..10 */
    		variables.cbIndex[9] |= (data[variables.tempIndex1]>>4)&0x1; /* Bit 11  */
    		variables.cbIndex[10] = (short)((data[variables.tempIndex1]<<4)&0xF0); /* Bit 12..15 */
    		variables.tempIndex1++;
    	    /* 22:nd WebRtc_Word16 */
    		variables.cbIndex[10] |= (data[variables.tempIndex1]>>12)&0xF; /* Bit 0..3  */
    		variables.cbIndex[11] = (short)((data[variables.tempIndex1]>>4)&0xFF); /* Bit 4..11 */
    		variables.cbIndex[12] |= (data[variables.tempIndex1]>>3)&0x1; /* Bit 12  */
    		variables.cbIndex[13] = (short)((data[variables.tempIndex1]<<5)&0xE0); /* Bit 13..15 */
    		variables.tempIndex1++;
    	    /* 23:rd WebRtc_Word16 */
    		variables.cbIndex[13] |= (data[variables.tempIndex1]>>11)&0x1F;/* Bit 0..4  */
    		variables.cbIndex[14] = (short)((data[variables.tempIndex1]>>3)&0xFF); /* Bit 5..12 */
    		variables.gainIndex[3] |= (data[variables.tempIndex1]>>1)&0x3; /* Bit 13..14 */
    		variables.gainIndex[4] |= (data[variables.tempIndex1]&0x1);  /* Bit 15  */
    		variables.tempIndex1++;
    	    /* 24:rd WebRtc_Word16 */
    		variables.gainIndex[5] = (short)((data[variables.tempIndex1]>>13)&0x7); /* Bit 0..2  */
    		variables.gainIndex[6] |= (data[variables.tempIndex1]>>10)&0x7; /* Bit 3..5  */
    		variables.gainIndex[7] |= (data[variables.tempIndex1]>>8)&0x3; /* Bit 6..7  */
    		variables.gainIndex[8] = (short)((data[variables.tempIndex1]>>5)&0x7); /* Bit 8..10 */
    		variables.gainIndex[9] |= (data[variables.tempIndex1]>>1)&0xF; /* Bit 11..14 */
    		variables.gainIndex[10] |= (data[variables.tempIndex1]<<2)&0x4; /* Bit 15  */
    		variables.tempIndex1++;
    	    /* 25:rd WebRtc_Word16 */
    		variables.gainIndex[10] |= (data[variables.tempIndex1]>>14)&0x3; /* Bit 0..1  */
    		variables.gainIndex[11] = (short)((data[variables.tempIndex1]>>11)&0x7); /* Bit 2..4  */
    		variables.gainIndex[12] |= (data[variables.tempIndex1]>>7)&0xF; /* Bit 5..8  */
    		variables.gainIndex[13] |= (data[variables.tempIndex1]>>4)&0x7; /* Bit 9..11 */
    		variables.gainIndex[14] = (short)((data[variables.tempIndex1]>>1)&0x7); /* Bit 12..14 */
    	}    	      	  
    }  

	public static void updateDecIndex(EncoderBits encoderBits,UpdateDecIndexVariables variables)
    {
		variables.index=encoderBits.getCbIndex();    	  
    	
    	for (variables.k=4;variables.k<6;variables.k++) 
    	{
    		if (variables.index[variables.k]>=44 && variables.index[variables.k]<108)
    			variables.index[variables.k]+=64;
    	    else if (variables.index[variables.k]>=108 && variables.index[variables.k]<128)
    	    	variables.index[variables.k]+=128;    	    
    	}
    }
	
	public static void simpleLsfDeq(short[] lsfDeq,int lsfDeqIndex,short[] index,int indexIndex,int lpcN,SimpleLsfDeqVariables variables)
    {
		variables.reset();
		
		for (variables.i = 0; variables.i < 3; variables.i++) 
    	{			
			variables.cbIndex=Constants.LSF_INDEX_CB[variables.i];
    		for (variables.j = 0; variables.j < Constants.LSF_DIM_CB[variables.i]; variables.j++)
    			lsfDeq[lsfDeqIndex++] = Constants.LSF_CB[variables.cbIndex + index[indexIndex]*Constants.LSF_DIM_CB[variables.i] + variables.j];    	    	
    		
    	    indexIndex++;    	    
    	}    	
    		    
    	if (lpcN>1) 
    	{
    		/* decode last LSF */
    		for (variables.i = 0; variables.i < 3; variables.i++) 
        	{
    			variables.cbIndex=Constants.LSF_INDEX_CB[variables.i];
        	    for (variables.j = 0; variables.j < Constants.LSF_DIM_CB[variables.i]; variables.j++)
        	    	lsfDeq[lsfDeqIndex++] = Constants.LSF_CB[variables.cbIndex + index[indexIndex]*Constants.LSF_DIM_CB[variables.i] + variables.j];        	    	
        	
        	    indexIndex++;        	    
    	    }
    	}
    }    

	public static void decoderInterpolateLsf(DecoderState decoderState,short[] syntDenum,int syntDenumIndex,short[] weightDenum,int weightDenumIndex,short[] lsfDeq,int lsfDeqIndex,short length,DecodeInterpolateLsfVariables variables)
    {
		variables.reset();
		variables.len=length+1;
    	
    	if (decoderState.DECODER_MODE==30) 
    	{
    	    lspInterpolate2PolyDec(variables.lp, 0, decoderState.getLsfDeqOld(), 0, lsfDeq, lsfDeqIndex,Constants.LSF_WEIGHT_30MS[0], length,variables.lspInterpolate2PolyDecVariables);
	    	System.arraycopy(variables.lp, 0, syntDenum, syntDenumIndex, variables.len);
    	    BasicFunctions.expand(weightDenum, weightDenumIndex, variables.lp, 0, Constants.LPC_CHIRP_SYNT_DENUM, variables.len);

    	    for (variables.s = 1; variables.s < decoderState.SUBFRAMES; variables.s++) 
    	    {
    	    	syntDenumIndex += variables.len;
    	    	weightDenumIndex+= variables.len;
    	    	lspInterpolate2PolyDec(variables.lp, 0, lsfDeq, lsfDeqIndex, lsfDeq, lsfDeqIndex + length,Constants.LSF_WEIGHT_30MS[variables.s], length,variables.lspInterpolate2PolyDecVariables);
    	    	System.arraycopy(variables.lp, 0, syntDenum, syntDenumIndex, variables.len);
    	    	BasicFunctions.expand(weightDenum,weightDenumIndex, variables.lp, 0, Constants.LPC_CHIRP_SYNT_DENUM, variables.len);    	    	
    	    }
    	}
    	else 
    	{ 	
    		for (variables.s = 0; variables.s < decoderState.SUBFRAMES; variables.s++) 
    	    {
    	    	lspInterpolate2PolyDec(variables.lp, 0, decoderState.getLsfDeqOld(), 0, lsfDeq, lsfDeqIndex,Constants.LSF_WEIGHT_20MS[variables.s], length,variables.lspInterpolate2PolyDecVariables);
    	    	System.arraycopy(variables.lp, 0, syntDenum, syntDenumIndex, variables.len);
    	    	BasicFunctions.expand(weightDenum, weightDenumIndex, variables.lp, 0, Constants.LPC_CHIRP_SYNT_DENUM, variables.len);
    	    	syntDenumIndex += variables.len;
    	    	weightDenumIndex+= variables.len;
    	    }
    	}

    	if (decoderState.DECODER_MODE==30) 
    		System.arraycopy(lsfDeq, lsfDeqIndex + length, decoderState.getLsfDeqOld(), 0, length);
    	else
    		System.arraycopy(lsfDeq, lsfDeqIndex, decoderState.getLsfDeqOld(), 0, length);    	        	 
    }
	
	public static void lspInterpolate2PolyDec(short[] a,int aIndex, short[] lsf1,int lsf1Index, short[] lsf2,int lsf2Index, short coef,int length,LspInterpolate2PolyDecVariables variables)
    {
		variables.reset();
    	interpolate(variables.lsfTemp, 0, lsf1, lsf1Index, lsf2, lsf2Index, coef, length,variables.interpolateVariables);
    	lsf2Poly(a,aIndex,variables.lsfTemp,0,variables.lsf2PolyVariables);
    }
	
	public static void decodeResidual(DecoderState decoderState,EncoderBits encoderBits,short[] decResidual,int decResidualIndex,short[] syntDenum,int syntDenumIndex,DecodeResidualVariables variables) 
    {
    	variables.reverseDecresidual = decoderState.getEnhancementBuffer();
    	variables.memVec = decoderState.getPrevResidual();

    	variables.diff = (short)(80 - decoderState.STATE_SHORT_LEN);
    	
    	if (encoderBits.getStateFirst())
    		variables.startPos = (short)((encoderBits.getStartIdx()-1)*40);
    	else
    		variables.startPos = (short)((encoderBits.getStartIdx()-1)*40 + variables.diff);    	  

    	stateConstruct(encoderBits,syntDenum,syntDenumIndex + (encoderBits.getStartIdx()-1)*11,decResidual,decResidualIndex + variables.startPos,decoderState.STATE_SHORT_LEN, variables.stateConstructVariables);    	
    	
    	if(encoderBits.getStateFirst()) 
    	{ 
    		for(variables.i=4;variables.i<151-decoderState.STATE_SHORT_LEN;variables.i++)
    			variables.memVec[variables.i]=0;
    		
    		System.arraycopy(decResidual, variables.startPos, variables.memVec, 151-decoderState.STATE_SHORT_LEN, decoderState.STATE_SHORT_LEN);
    	    cbConstruct(encoderBits,decResidual,decResidualIndex + variables.startPos + decoderState.STATE_SHORT_LEN, variables.memVec,66, (short)85, variables.diff, 0, 0, variables.cbConstructVariables);
    	}
    	else 
    	{
    		variables.memlGotten = decoderState.STATE_SHORT_LEN;
    	    BasicFunctions.reverseCopy(variables.memVec, 150, decResidual, decResidualIndex + variables.startPos, variables.memlGotten);
    	    
    	    for(variables.i=4;variables.i<151-variables.memlGotten;variables.i++)
    	    	variables.memVec[variables.i]=0;    		

    	    cbConstruct(encoderBits,variables.reverseDecresidual, 0, variables.memVec, 66, (short)85, variables.diff, 0, 0, variables.cbConstructVariables);
    	    BasicFunctions.reverseCopy(decResidual, decResidualIndex + variables.startPos-1, variables.reverseDecresidual, 0, variables.diff);    	    
    	}

    	variables.subCount=1;
    	variables.nFor = (short)(decoderState.SUBFRAMES - encoderBits.getStartIdx() -1);
    	  
    	if(variables.nFor > 0) 
    	{
    		for(variables.i=4;variables.i<71;variables.i++)
    			variables.memVec[variables.i]=0;
    		
    	    System.arraycopy(decResidual, decResidualIndex + 40 * (encoderBits.getStartIdx()-1), variables.memVec, 71, 80);    	    	
    	    
    	    for (variables.subFrame=0; variables.subFrame<variables.nFor; variables.subFrame++) 
    	    {
    	    	cbConstruct(encoderBits,decResidual, decResidualIndex + 40*(encoderBits.getStartIdx()+1+variables.subFrame), variables.memVec, 4, (short)147, (short)40, variables.subCount*3, variables.subCount*3, variables.cbConstructVariables);
        	    
    	    	for(variables.i=4;variables.i<111;variables.i++)
    	    		variables.memVec[variables.i]=variables.memVec[variables.i+40];
    	    	
    	    	System.arraycopy(decResidual, decResidualIndex + 40 * (encoderBits.getStartIdx()+1+variables.subFrame) , variables.memVec, 111, 40);    	    	
    	    	variables.subCount++;    	    	
    	    }
    	}

    	variables.nBack = (short)(encoderBits.getStartIdx()-1);

    	if(variables.nBack > 0)
    	{
    		variables.memlGotten = (short)(40*(decoderState.SUBFRAMES + 1 - encoderBits.getStartIdx()));
    	    if(variables.memlGotten > 147)
    	    	variables.memlGotten=147;    	    

    	    BasicFunctions.reverseCopy(variables.memVec, 150, decResidual, decResidualIndex + 40 * (encoderBits.getStartIdx()-1), variables.memlGotten);
    	    
    	    for(variables.i=4;variables.i<151-variables.memlGotten;variables.i++)
    	    	variables.memVec[variables.i]=0;
    	    
    	    for (variables.subFrame=0; variables.subFrame<variables.nBack; variables.subFrame++) 
    	    {
    	    	cbConstruct(encoderBits,variables.reverseDecresidual, 40*variables.subFrame, variables.memVec, 4, (short)147, (short)40, variables.subCount*3, variables.subCount*3, variables.cbConstructVariables);
        	    
    	    	for(variables.i=4;variables.i<111;variables.i++)
    	    		variables.memVec[variables.i]=variables.memVec[variables.i+40];
    	    	
    	    	System.arraycopy(variables.reverseDecresidual, 40 * variables.subFrame, variables.memVec, 111, 40);
    	    	variables.subCount++;
    	    }

    	    BasicFunctions.reverseCopy(decResidual, decResidualIndex+40*variables.nBack-1, variables.reverseDecresidual, 0, 40*variables.nBack);    	    
    	}  	    	        	
    }   
	
	public static int xCorrCoef(short[] target,int targetIndex,short[] regressor,int regressorIndex,short subl,short searchLen,short offset,short step,XCorrCoefVariables variables)
    {    	  
    	variables.energyModMax=Short.MAX_VALUE;
    	variables.totScaleMax=-500;
    	variables.crossCorrSqModMax=0;
    	variables.maxLag=0;
    	
    	variables.pos=0;
    	
    	if (step==1) 
    	{
    		variables.max=BasicFunctions.getMaxAbsValue(regressor,regressorIndex,subl+searchLen-1);
    		variables.tempIndex1=regressorIndex;
    		variables.tempIndex2=regressorIndex + subl;    	    
    	}
    	else 
    	{ 
    		variables.max=BasicFunctions.getMaxAbsValue(regressor,regressorIndex-searchLen,subl+searchLen-1);
    		variables.tempIndex1=regressorIndex-1;
    		variables.tempIndex2=regressorIndex + subl-1;    	    
    	}

    	if (variables.max>5000)
    		variables.shifts=2;
    	else
    		variables.shifts=0;    	  

    	variables.energy=BasicFunctions.scaleRight(regressor, regressorIndex, regressor, regressorIndex, subl, variables.shifts);
    	for (variables.k=0;variables.k<searchLen;variables.k++) 
    	{
    		variables.tempIndex3=targetIndex;
    		variables.tempIndex4=regressorIndex + variables.pos;
    	    
    		variables.crossCorr=BasicFunctions.scaleRight(target, variables.tempIndex3, regressor, variables.tempIndex4, subl, variables.shifts);    			  
    		if ((variables.energy>0)&&(variables.crossCorr>0)) 
    		{
    			/* Put cross correlation and energy on 16 bit word */
    			variables.crossCorrScale=(short)(BasicFunctions.norm(variables.crossCorr)-16);
    			
    			if(variables.crossCorrScale>0)
    				variables.crossCorrMod=(short)(variables.crossCorr<<variables.crossCorrScale);
    			else
    				variables.crossCorrMod=(short)(variables.crossCorr>>(0-variables.crossCorrScale));
    			
    			variables.energyScale=(short)(BasicFunctions.norm(variables.energy)-16);
    			
    			if(variables.energyScale>0)
    				variables.energyMod=(short)(variables.energy<<variables.energyScale);
    			else
    				variables.energyMod=(short)(variables.energy>>(0-variables.energyScale));
    			
    			/* Square cross correlation and store upper WebRtc_Word16 */
    			variables.crossCorrSqMod=(short)((variables.crossCorrMod * variables.crossCorrMod) >> 16);

    			/* Calculate the total number of (dynamic) right shifts that have
    	        been performed on (crossCorr*crossCorr)/energy
    			*/
    			variables.totScale=(short)(variables.energyScale-(variables.crossCorrScale<<1));

    			/* Calculate the shift difference in order to be able to compare the two
    	        (crossCorr*crossCorr)/energy in the same domain
    			*/
    			variables.scaleDiff=(short)(variables.totScale-variables.totScaleMax);
    			if(variables.scaleDiff>31)
    				variables.scaleDiff=31;
    			else if(variables.scaleDiff<-31)
    				variables.scaleDiff=-31;

    			/* Compute the cross multiplication between the old best criteria
    	        and the new one to be able to compare them without using a
    	        division */

    			if (variables.scaleDiff<0) 
    			{
    				variables.newCrit = ((variables.crossCorrSqMod*variables.energyModMax)>>(-variables.scaleDiff));
    				variables.maxCrit = variables.crossCorrSqModMax*variables.energyMod;
    			} 
    			else 
    			{
    				variables.newCrit = variables.crossCorrSqMod*variables.energyModMax;
    				variables.maxCrit = ((variables.crossCorrSqModMax*variables.energyMod)>>variables.scaleDiff);
    			}

    			/* Store the new lag value if the new criteria is larger
    	        than previous largest criteria */

    			if (variables.newCrit > variables.maxCrit) 
    			{
    				variables.crossCorrSqModMax = variables.crossCorrSqMod;
    				variables.energyModMax = variables.energyMod;
    				variables.totScaleMax = variables.totScale;
    				variables.maxLag = (short)variables.k;
    			}    			    			    			    		
    		}
    		  
    		variables.pos+=step;

    		/* Do a +/- to get the next energy */
    		variables.temp=regressor[variables.tempIndex2]*regressor[variables.tempIndex2] - regressor[variables.tempIndex1]*regressor[variables.tempIndex1];
    		variables.temp>>=variables.shifts;
    		variables.energy += step*variables.temp;
    		
    		variables.tempIndex1+=step;
    		variables.tempIndex2+=step;
    	}

    	return(variables.maxLag+offset);
    }   
	
	public static void doThePlc(DecoderState decoderState,short[] plcResidual,int plcResidualIndex,short[] plcLpc,int plcLpcIndex,short pli,short[] decResidual,int decResidualIndex,short[] lpc,int lpcIndex,short inLag,DoThePlcVariables variables)
    {    	      	     
    	variables.tempCorrData.setEnergy(0);
    	
    	if (pli == 1) 
    	{
    	    decoderState.setConsPliCount(decoderState.getConsPliCount()+1);
    	    if (decoderState.getPrevPli() != 1) 
    	    {
    	    	variables.max=BasicFunctions.getMaxAbsValue(decoderState.getPrevResidual(),0,decoderState.SIZE);
    	    	variables.scale = (short)((BasicFunctions.getSize(variables.max)<<1) - 25);
    	    	if (variables.scale < 0)
    	    		variables.scale = 0;    	      

    	      decoderState.setPrevScale(variables.scale);
    	      variables.lag = (short)(inLag - 3);

    	      if(60 > decoderState.SIZE-inLag-3)
    	    	  variables.corrLen=60;
    	      else
    	    	  variables.corrLen=(short)(decoderState.SIZE-inLag-3);
    	      
    	      compCorr(variables.corrData, decoderState.getPrevResidual(), 0, variables.lag, decoderState.SIZE, variables.corrLen, variables.scale);

    	      variables.shiftMax = (short)(BasicFunctions.getSize(Math.abs(variables.corrData.getCorrelation()))-15);
    	      if(variables.shiftMax>0)
    	      {
    	    	  variables.tempShift=variables.corrData.getCorrelation()>>variables.shiftMax;
    	    		variables.tempShift=variables.tempShift*variables.tempShift;
    	    		variables.crossSquareMax=(short)(variables.tempShift>>15);    	    	  
    	      }
    	      else
    	      {
    	    	  variables.tempShift=variables.corrData.getCorrelation()<<(0-variables.shiftMax);
    	    	  variables.tempShift=variables.tempShift*variables.tempShift;
    	    	  variables.crossSquareMax=(short)(variables.tempShift>>15);  
    	      }

    	      for (variables.j=inLag-2;variables.j<=inLag+3;variables.j++) 
    	      {
    	    	  compCorr(variables.tempCorrData, decoderState.getPrevResidual(), 0, (short)variables.j, decoderState.SIZE, variables.corrLen, variables.scale);

    	    	  variables.shift1 = (short)(BasicFunctions.getSize(Math.abs(variables.tempCorrData.getCorrelation())-15));
    	    	  if(variables.shift1>0)
        	      {
    	    		  variables.tempShift=variables.tempCorrData.getCorrelation()>>variables.shift1;
    	    		  variables.tempShift=variables.tempShift*variables.tempShift;
    	    		  variables.crossSquare=(short)(variables.tempShift>>15);    	    	  
        	      }
        	      else
        	      {
        	    	  variables.tempShift=variables.tempCorrData.getCorrelation()<<(0-variables.shift1);
        	    	  variables.tempShift=variables.tempShift*variables.tempShift;
        	    	  variables.crossSquare=(short)(variables.tempShift>>15);  
        	      }    	    	  

    	    	  variables.shift2 = (short)(BasicFunctions.getSize(variables.corrData.getEnergy())-15);
    	    	  if(variables.shift2>0)
    	    		  variables.measure=(variables.corrData.getEnergy()>>variables.shift2)*variables.crossSquare;
    	    	  else
    	    		  variables.measure=(variables.corrData.getEnergy()<<(0-variables.shift2))*variables.crossSquare;

    	    	  variables.shift3 = (short)(BasicFunctions.getSize(variables.tempCorrData.getEnergy())-15);
    	    	  if(variables.shift3>0)
    	    		  variables.maxMeasure=(variables.tempCorrData.getEnergy()>>variables.shift3)*variables.crossSquareMax;
    	    	  else
    	    		  variables.maxMeasure=(variables.tempCorrData.getEnergy()<<(0-variables.shift3))*variables.crossSquareMax;
    	    	  
    	    	  if(((variables.shiftMax<<1)+variables.shift3) > ((variables.shift1<<1)+variables.shift2)) 
    	    	  {
    	    		  variables.tempShift=(variables.shiftMax<<1);
    	    		  variables.tempShift-=(variables.shift1<<1);
    	    		  variables.tempShift=variables.tempShift+variables.shift3-variables.shift2;
    	    		  if(variables.tempShift>31)
    	    			  variables.tempS = 31;
    	    		  else
    	    			  variables.tempS= (short)variables.tempShift;
    	    		  
    	    		  variables.tempS2 = 0;
    	    	  } 
    	    	  else 
    	    	  {
    	    		  variables.tempS = 0;
    	    		  variables.tempShift=(variables.shift1<<1);
    	    		  variables.tempShift-=(variables.shiftMax<<1);
    	    		  variables.tempShift=variables.tempShift+variables.shift2-variables.shift3;
    	    		  if(variables.tempShift>31)
    	    			  variables.tempS2 = 31;
    	    		  else
    	    			  variables.tempS2=(short)variables.tempShift;
    	    	  }

    	    	  if ((variables.measure>>variables.tempS) > (variables.maxMeasure>>variables.tempS2)) 
    	    	  {
    	    		  variables.lag = (short)variables.j;
    	    		  variables.crossSquareMax = variables.crossSquare;
    	    		  variables.corrData.setCorrelation(variables.tempCorrData.getCorrelation());
    	    		  variables.shiftMax = variables.shift1;
    	    		  variables.corrData.setEnergy(variables.tempCorrData.getEnergy());
    	    	  }
    	      }

    	      variables.temp2=BasicFunctions.scaleRight(decoderState.getPrevResidual(),decoderState.SIZE-variables.corrLen,decoderState.getPrevResidual(),decoderState.SIZE-variables.corrLen,variables.corrLen, variables.scale);
    	      
    	      if ((variables.temp2>0)&&(variables.tempCorrData.getEnergy()>0)) 
    	      {    	    	
    	    	  variables.scale1=(short)(BasicFunctions.norm(variables.temp2)-16);
    	    	  if(variables.scale1>0)
    	    		  variables.tempS=(short)(variables.temp2<<variables.scale1);
    	    	  else
    	    		  variables.tempS=(short)(variables.temp2>>(0-variables.scale1));
    	    	  
    	    	  variables.scale2=(short)(BasicFunctions.norm(variables.corrData.getEnergy())-16);
    	    	  if(variables.scale2>0)
    	    		  variables.tempS2=(short)(variables.corrData.getEnergy()<<variables.scale2);
    	    	  else
    	    		  variables.tempS2=(short)(variables.corrData.getEnergy()>>(0-variables.scale2));
    	    	      	    	  
    	    	  variables.denom=(short)((variables.tempS*variables.tempS2)>>16);

    	    	  variables.totScale = (short)(variables.scale1+variables.scale2-1);
    	    	  variables.tempShift=(variables.totScale>>1);
    	    	  if(variables.tempShift>0)
    	    		  variables.tempS = (short)(variables.corrData.getCorrelation()<<variables.tempShift);
    	    	  else
    	    		  variables.tempS = (short)(variables.corrData.getCorrelation()>>(0-variables.tempShift));
    	    	  
    	    	  variables.tempShift=variables.totScale-variables.tempShift;
    	    	  if(variables.tempShift>0)
    	    		  variables.tempS2 = (short)(variables.corrData.getCorrelation()<<variables.tempShift);
    	    	  else
    	    		  variables.tempS2 = (short)(variables.corrData.getCorrelation()>>(0-variables.tempShift));
    	    	  
    	    	  variables.nom = (short)(variables.tempS*variables.tempS2);
    	    	  variables.maxPerSquare = (short)(variables.nom/variables.denom);

    	      } 
    	      else 
    	    	  variables.maxPerSquare = 0;    	      
    	  }    	  
    	  else 
    	  {
    		  variables.lag = decoderState.getPrevLag();
    		  variables.maxPerSquare = decoderState.getPerSquare();
    	  }

    	    variables.useGain = 32767;
    	  
    	  if (decoderState.getConsPliCount()*decoderState.SIZE>320)
    		  variables.useGain = 29491;
    	  else if (decoderState.getConsPliCount()*decoderState.SIZE>640)
    		  variables.useGain = 22938;
    	  else if (decoderState.getConsPliCount()*decoderState.SIZE>960)
    		  variables.useGain = 16384;
    	  else if (decoderState.getConsPliCount()*decoderState.SIZE>1280)
    		  variables.useGain = 0;
    	  
    	  if (variables.maxPerSquare>7868) 
    		  variables.pitchFact = 32767;
    	  else if (variables.maxPerSquare>839) 
    	  { 
    		  variables.ind = 5;
    	      while ((variables.maxPerSquare<Constants.PLC_PER_SQR[variables.ind]) && (variables.ind>0))
    	    	  variables.ind--;
    	      
    	      variables.temp = Constants.PLC_PITCH_FACT[variables.ind];
    	      variables.temp += ((Constants.PLC_PF_SLOPE[variables.ind]*(variables.maxPerSquare-Constants.PLC_PER_SQR[variables.ind])) >> 11);

    	      if(variables.temp>Short.MIN_VALUE)
    	    	  variables.pitchFact=Short.MIN_VALUE;
    	      else
    	    	  variables.pitchFact=(short)variables.temp;    	      
    	  } 
    	  else 
    		  variables.pitchFact = 0;
    	  
    	  variables.useLag = variables.lag;
    	  if (variables.lag<80) 
    		  variables.useLag = (short)(2*variables.lag);
    	  
    	  variables.energy = 0;
    	  for (variables.i=0; variables.i<decoderState.SIZE; variables.i++) 
    	  {
    	      decoderState.setSeed((short)((decoderState.getSeed()*31821) + 13849));
    	      variables.randLag = (short)(53 + (decoderState.getSeed() & 63));

    	      variables.pick = (short)(variables.i - variables.randLag);
    	      if (variables.pick < 0)
    	    	  variables.randVec[variables.i] = decoderState.getPrevResidual()[decoderState.SIZE+variables.pick];
    	      else
    	    	  variables.randVec[variables.i] = decoderState.getPrevResidual()[variables.pick];    	      

    	      variables.pick = (short)(variables.i - variables.useLag);

    	      if (variables.pick < 0)
    	        plcResidual[plcResidualIndex + variables.i] = decoderState.getPrevResidual()[decoderState.SIZE+variables.pick];
    	      else
    	    	  plcResidual[plcResidualIndex + variables.i] = plcResidual[plcResidualIndex + variables.pick];    	      

    	      if (variables.i<80)
    	    	  variables.totGain=variables.useGain;
    	      else if (variables.i<160)
    	    	  variables.totGain=(short)((31130 * variables.useGain) >> 15);
    	      else
    	    	  variables.totGain=(short)((29491 * variables.useGain) >> 15);    	      


    	      variables.tempShift=variables.pitchFact * plcResidual[plcResidualIndex + variables.i];
    	      variables.tempShift+=(32767-variables.pitchFact)*variables.randVec[variables.i];
    	      variables.tempShift+=16384;
    	      variables.temp=(short)(variables.tempShift>>15);
    	      plcResidual[plcResidualIndex + variables.i] = (short)((variables.totGain*variables.temp)>>15);

    	      variables.tempShift=plcResidual[plcResidualIndex + variables.i] * plcResidual[plcResidualIndex + variables.i];
    	      variables.energy += (short)(variables.tempShift>>(decoderState.getPrevScale()+1));
    	  }

    	  variables.tempShift=decoderState.SIZE*900;
    	  if(decoderState.getPrevScale()+1>0)
    		  variables.tempShift=variables.tempShift>>(decoderState.getPrevScale()+1);
    	  else
    		  variables.tempShift=variables.tempShift<<(0-decoderState.getPrevScale()-1);
    	  
    	  if (variables.energy < variables.tempShift) 
    	  {
    		  variables.energy = 0;
    	      for (variables.i=0; variables.i<decoderState.SIZE; variables.i++)
    	        plcResidual[plcResidualIndex + variables.i] = variables.randVec[variables.i];    	      
    	  }

    	  System.arraycopy(decoderState.getPrevLpc(), 0, plcLpc, plcLpcIndex, 10);    	  
    	  decoderState.setPrevLag(variables.lag);
    	  decoderState.setPerSquare(variables.maxPerSquare);    	  
       }
       else 
       {
     	  System.arraycopy(decResidual, decResidualIndex, plcResidual, plcResidualIndex, decoderState.SIZE);    	  
     	  System.arraycopy(lpc, lpcIndex, plcLpc, plcLpcIndex, 11);    	  
    	  decoderState.setConsPliCount(0);    	    
       }

       decoderState.setPrevPli(pli);
       System.arraycopy(plcLpc, plcLpcIndex, decoderState.getPrevLpc(), 0, 11);    	  
       System.arraycopy(plcResidual, plcResidualIndex, decoderState.getPrevResidual(), 0, decoderState.SIZE); 	       
    }    
	
	public static void compCorr(CorrData currData,short[] buffer,int bufferIndex,short lag,short bLen,short sRange,short scale)
    {
    	int currIndex=bLen-sRange-lag;
    	
    	if(scale>0)
    	{
    		currData.setCorrelation(BasicFunctions.scaleRight(buffer, bufferIndex + bLen - sRange, buffer, currIndex, sRange, scale));
    		currData.setEnergy(BasicFunctions.scaleRight(buffer, currIndex, buffer, currIndex, sRange, scale));    		
    	}
    	else
    	{
    		currData.setCorrelation(BasicFunctions.scaleLeft(buffer, bufferIndex + bLen - sRange, buffer, currIndex, sRange, (0-scale)));
    		currData.setEnergy(BasicFunctions.scaleLeft(buffer, currIndex, buffer, currIndex, sRange, scale));
    	}
    	
    	if (currData.getCorrelation() == 0) 
    	{
    		currData.setCorrelation(0);
    		currData.setEnergy(1);    	    
    	}
    }
	
	public static int enchancher(short[] in,int inIndex,short[] out,int outIndex,DecoderState decoderState,EnhancerVariables variables)
    {
		variables.reset();
		variables.lag=20;
		variables.tLag=20;
		variables.inputLength=decoderState.SIZE+120;
		
		variables.enhancementBuffer=decoderState.getEnhancementBuffer();
		variables.enhancementPeriod=decoderState.getEnhancementPeriod();		
		
		System.arraycopy(variables.enhancementBuffer, decoderState.SIZE, variables.enhancementBuffer, 0, variables.enhancementBuffer.length-decoderState.SIZE);
		System.arraycopy(in, inIndex, variables.enhancementBuffer, 640-decoderState.SIZE, decoderState.SIZE);
		
		if(decoderState.DECODER_MODE==30) 
		{
			variables.plcBlock=80;
			variables.newBlocks=3;
			variables.startPos=320;
		}
		else 
		{
			variables.plcBlock=40;
			variables.newBlocks=2;
			variables.startPos=440;
		}

		System.arraycopy(variables.enhancementPeriod, variables.newBlocks, variables.enhancementPeriod, 0, variables.enhancementPeriod.length-variables.newBlocks);
		BasicFunctions.downsampleFast(variables.enhancementBuffer,640-variables.inputLength,variables.inputLength+3,variables.downsampled,0,variables.inputLength>>1,Constants.LP_FILT_COEFS,7,2,3);

		for(variables.iBlock = 0; variables.iBlock<variables.newBlocks; variables.iBlock++)
		{
			variables.targetIndex=60+variables.iBlock*40;
			variables.regressorIndex=variables.targetIndex-10;
			variables.max16=BasicFunctions.getMaxAbsValue(variables.downsampled,variables.regressorIndex-50,89);
			variables.shifts=(short)(BasicFunctions.getSize(variables.max16*variables.max16)-25);
		    if(variables.shifts<0)
		    	variables.shifts=0;
		    
		    crossCorrelation(variables.corr32, 0, variables.downsampled, variables.targetIndex, variables.downsampled, variables.regressorIndex, (short)40, (short)50, variables.shifts, (short)-1, variables.crossCorrelationVariables);
		    
		    for (variables.i=0;variables.i<2;variables.i++) 
		    {
		    	variables.lagMax[variables.i] =(short)BasicFunctions.getMaxIndex(variables.corr32,0,50);
		    	variables.corrMax[variables.i] = variables.corr32[variables.lagMax[variables.i]];
		    	variables.start = variables.lagMax[variables.i] - 2;
		    	variables.stop = variables.lagMax[variables.i] + 2;
		    	if(variables.start<0)
		    		variables.start=0;
		    	
		    	if(variables.stop>49)
		    		variables.stop=49;
		    	
		    	System.arraycopy(emptyIntArray, 0, variables.corr32, variables.start, variables.stop-variables.start);		    			     
		    }
		    
		    variables.lagMax[2] =(short)BasicFunctions.getMaxIndex(variables.corr32,0,50);
		    variables.corrMax[2] = variables.corr32[variables.lagMax[2]];

		    for (variables.i=0;variables.i<3;variables.i++)
		    {
		    	variables.corrSh = 15-BasicFunctions.getSize(variables.corrMax[variables.i]);
		    	variables.ener = BasicFunctions.scaleRight(variables.downsampled,variables.regressorIndex-variables.lagMax[variables.i],variables.downsampled,variables.regressorIndex-variables.lagMax[variables.i],40, variables.shifts);		      
		    	variables.enerSh = 15-BasicFunctions.getSize(variables.ener);	
		    	
		    	if(variables.corrSh>0)
		    		variables.corr16[variables.i] = (short)(variables.corrMax[variables.i]>>variables.corrSh);
		    	else
		    		variables.corr16[variables.i] = (short)(variables.corrMax[variables.i]<<variables.corrSh);
		    	
		    	variables.corr16[variables.i] = (short)((variables.corr16[variables.i]*variables.corr16[variables.i])>>16);
		    	
		    	if(variables.enerSh>0)
		    		variables.en16[variables.i] = (short)(variables.ener>>variables.enerSh);
		    	else
		    		variables.en16[variables.i] = (short)(variables.ener<<variables.enerSh);
		    	
		    	variables.totSh[variables.i] = (short)(variables.enerSh - (variables.corrSh<<1));
		    }

		    variables.index = 0;
		    for (variables.i=1; variables.i<3; variables.i++) 
		    {
		      if (variables.totSh[variables.index] > variables.totSh[variables.i]) 
		      {
		    	  variables.sh=(short)(variables.totSh[variables.index]-variables.totSh[variables.i]);
		    	  if(variables.sh>31)
		    		  variables.sh=31;
		        
		    	  if (variables.corr16[variables.index] * variables.en16[variables.i] < ((variables.corr16[variables.i]*variables.en16[variables.index])>> variables.sh)) 
		    		  variables.index = variables.i;		        
		      }
		      else 
		      {
		    	  variables.sh=(short)(variables.totSh[variables.i] - variables.totSh[variables.index]);
		    	  if(variables.sh>31)
		    		  variables.sh=31;
		        
		    	  if ( ((variables.corr16[variables.index]*variables.en16[variables.i])>> variables.sh) < variables.corr16[variables.i] * variables.en16[variables.index]) 
		    		  variables.index = variables.i;		    	  		    	  	       
		      }
		    }

		    variables.lag = variables.lagMax[variables.index] + 10;

		    variables.enhancementPeriod[8-variables.newBlocks+variables.iBlock] = (short)(variables.lag*8);

		    if (decoderState.getPrevEnchPl()==1) 
		    {
		    	if (variables.iBlock==0) 
		    		variables.tLag = variables.lag*2;		      
		    } 
		    else if (variables.iBlock==1) 
		    	variables.tLag = variables.lag*2;		      
		    
		    variables.lag = variables.lag*2;
		}

		if (decoderState.getPrevEnchPl()==1 || decoderState.getPrevEnchPl()==2) 
		{
			variables.targetIndex=inIndex;
			variables.regressorIndex=inIndex+variables.tLag-1;

			variables.max16=BasicFunctions.getMaxAbsValue(in,inIndex,variables.plcBlock+2);		    	
		    if (variables.max16>5000)
		    	variables.shifts=2;
		    else
		    	variables.shifts=0;

		    crossCorrelation(variables.corr32,0,in, variables.targetIndex,in, variables.regressorIndex,(short)variables.plcBlock,(short)3,(short)variables.shifts,(short)1, variables.crossCorrelationVariables);
		    variables.lag=BasicFunctions.getMaxIndex(variables.corr32,0,3);
		    variables.lag+=variables.tLag-1;

		    if (decoderState.getPrevEnchPl()==1) 
		    {
		    	if (variables.lag>variables.plcBlock)
		    		System.arraycopy(in, inIndex+variables.lag-variables.plcBlock, variables.downsampled, 0, variables.plcBlock);		    		
		    	else 
		    	{
		    		System.arraycopy(in, inIndex, variables.downsampled, variables.plcBlock-variables.lag, variables.lag);
		    		System.arraycopy(variables.enhancementBuffer, 640-decoderState.SIZE-variables.plcBlock+variables.lag, variables.downsampled, 0, variables.plcBlock-variables.lag);		    		
		    	}
		    } 
		    else 
		    {
		    	variables.pos = variables.plcBlock;

		    	while (variables.lag<variables.pos) 
		    	{
		    		System.arraycopy(in, inIndex, variables.downsampled, variables.pos-variables.lag, variables.lag);
		    		variables.pos = variables.pos - variables.lag;
		    	}
		      
		    	System.arraycopy(in, inIndex+variables.lag-variables.pos, variables.downsampled, 0, variables.pos);		    	
		    }

		    if (decoderState.getPrevEnchPl()==1) 
		    {		    
		    	variables.max=BasicFunctions.getMaxAbsValue(variables.enhancementBuffer,640-decoderState.SIZE-variables.plcBlock,variables.plcBlock);		    		
		    	variables.max16=BasicFunctions.getMaxAbsValue(variables.downsampled,0,variables.plcBlock);
		    	if(variables.max16>variables.max)
		    		variables.max=variables.max16;
		    	
		    	variables.scale=(short)(22-BasicFunctions.norm(variables.max));
		    	if(variables.scale<0)
		    		variables.scale=0;
		    	
		    	variables.temp2 =BasicFunctions.scaleRight(variables.enhancementBuffer,640-decoderState.SIZE-variables.plcBlock,variables.enhancementBuffer,640-decoderState.SIZE-variables.plcBlock,variables.plcBlock,variables.scale);
		    	variables.temp1 =BasicFunctions.scaleRight(variables.downsampled,0,variables.downsampled,0,variables.plcBlock,variables.scale); 		    		

		    	if ((variables.temp1>0)&&((variables.temp1>>2)>variables.temp2)) 
		    	{		        
		    		variables.scale1=(short)BasicFunctions.norm(variables.temp1);
		    		if(variables.scale1>16)		    			
		    			variables.temp1=(variables.temp1>>(variables.scale1-16));
		    		else
		    			variables.temp1=(variables.temp1<<(variables.scale1-16));
		    			
		    		variables.temp2=(variables.temp2>>variables.scale1);		    		
		    		variables.sqrtEnChange=(short)BasicFunctions.sqrtFloor((variables.temp2/variables.temp1)<<14); 		    			
		    		BasicFunctions.scaleVector(variables.downsampled, 0, variables.downsampled, 0, variables.sqrtEnChange, variables.plcBlock-16, 14);
		    		variables.increment=(2048-(variables.sqrtEnChange>>3));
		    		variables.window=0;
		    		variables.tempIndex=variables.downsampled[variables.plcBlock-16];

		    		for (variables.i=16;variables.i>0;variables.i--,variables.window += variables.increment,variables.tempIndex++) 
		    			variables.downsampled[variables.tempIndex]=(short)((variables.downsampled[variables.tempIndex]*(variables.sqrtEnChange+(variables.window>>1)))>>14);
		    	}

		    	if (variables.plcBlock==40) 
		    		variables.increment=400;
		      	else 
		      		variables.increment=202;
		      
		    	variables.window=variables.increment;
		    	variables.tempIndex=640-1-decoderState.SIZE;
		    	for (variables.i=0; variables.i<variables.plcBlock; variables.i++,variables.tempIndex--,variables.window+=variables.increment) 
		    		variables.enhancementBuffer[variables.tempIndex] =(short)(((variables.enhancementBuffer[variables.tempIndex]*variables.window)>>14) + (((16384-variables.window)*variables.downsampled[variables.plcBlock-1-variables.i])>>14));
			}
			else 
			{
				variables.syntIndex=10;
				variables.tempIndex=640-1-decoderState.SIZE-variables.plcBlock;
				System.arraycopy(variables.downsampled, 0, variables.enhancementBuffer, variables.tempIndex, variables.plcBlock);
				System.arraycopy(emptyArray,0,decoderState.getSynthMem(), 0, 11);
				System.arraycopy(emptyArray,0,decoderState.getHpiMemX(), 0, 4);
				System.arraycopy(emptyArray,0,decoderState.getHpiMemY(), 0, 2);				
				System.arraycopy(decoderState.getSynthMem(), 0, variables.downsampled, 0, 10);
				BasicFunctions.filterAR(variables.enhancementBuffer,variables.tempIndex,variables.downsampled,variables.syntIndex,decoderState.getOldSyntDenum(),(decoderState.SUBFRAMES-1)*11,11,variables.lag);
				System.arraycopy(variables.downsampled, variables.lag, variables.downsampled, 0, 10);
				hpOutput(variables.downsampled,variables.syntIndex,Constants.HP_OUT_COEFICIENTS,decoderState.getHpiMemY(),decoderState.getHpiMemX(),(short)variables.lag, variables.hpOutputVariables);
				BasicFunctions.filterAR(variables.enhancementBuffer,variables.tempIndex,variables.downsampled,variables.syntIndex,decoderState.getOldSyntDenum(),(decoderState.SUBFRAMES-1)*11,11,variables.lag);				
				System.arraycopy(variables.downsampled, 0, decoderState.getSynthMem(), 0, 10);
				hpOutput(variables.downsampled,variables.syntIndex,Constants.HP_OUT_COEFICIENTS,decoderState.getHpiMemY(),decoderState.getHpiMemX(),(short)variables.lag, variables.hpOutputVariables);
		    }
    	}

		for (variables.iBlock = 0; variables.iBlock<variables.newBlocks; variables.iBlock++)
		{
			System.arraycopy(emptyArray, 0, variables.surround, 0, 80);			  
			getSyncSeq(variables.enhancementBuffer, 0, 640, variables.iBlock*80+variables.startPos, variables.enhancementPeriod, 0, Constants.ENHANCEMENT_PLOCS, 0, 8, (short)3, variables.surround,0, variables.getSyncSeqVariables);
			smooth(out,outIndex+variables.iBlock*80, variables.enhancementBuffer, variables.iBlock*80+variables.startPos, variables.surround,0,variables.smoothVariables);
		}
		
		return (variables.lag);    
    }
	
	public static void getSyncSeq(short[] current,int currentIndex,int currentLength,int centerStartPos,short[] period,int periodIndex,short[] plocs,int plocsIndex,int periodLength,short hl,short[] surround,int surroundIndex,GetSyncSeqVariables variables)
	{
		variables.reset();
		
		variables.centerEndPos=centerStartPos+79;
		nearestNeighbor(variables.lagBlock,hl,plocs,plocsIndex,(short)(2*(centerStartPos+variables.centerEndPos)),periodLength, variables.nearestNeighborVariables);
		
		variables.blockStartPos[hl]=(short)(4*centerStartPos);

		for(variables.q=hl-1;variables.q>=0;variables.q--) 
		{
			variables.blockStartPos[variables.q]=(short)(variables.blockStartPos[variables.q+1]-period[periodIndex + variables.lagBlock[variables.q+1]]);
		    nearestNeighbor(variables.lagBlock,variables.q,plocs,plocsIndex,(short)(variables.blockStartPos[variables.q] + 160-period[periodIndex + variables.lagBlock[variables.q+1]]),periodLength, variables.nearestNeighborVariables);

		    if((variables.blockStartPos[variables.q]-8)>=0) 
		    	refiner(variables.blockStartPos,variables.q,current,currentIndex,currentLength,centerStartPos,variables.blockStartPos[variables.q],surround,surroundIndex,Constants.Enhancement_WT[variables.q],variables.refinerVariables);		    			     		  
		}

		variables.tempIndex1=plocsIndex;
		variables.tempIndex2=periodIndex;
		for(variables.i=0;variables.i<periodLength;variables.i++) 
			variables.plocs2[variables.i]=(short)(plocs[variables.tempIndex1++]-period[variables.tempIndex2++]);
		
		for(variables.q=hl+1;variables.q<=(2*hl);variables.q++) 
		{

		    nearestNeighbor(variables.lagBlock,variables.q,variables.plocs2,0,(short)(variables.blockStartPos[variables.q-1] + 160),periodLength, variables.nearestNeighborVariables);		    
		    variables.blockStartPos[variables.q]=(short)(variables.blockStartPos[variables.q-1]+period[variables.lagBlock[variables.q]]);

		    if((variables.blockStartPos[variables.q]+328)<(4*currentLength))
		    	refiner(variables.blockStartPos,variables.q,current,currentIndex,currentLength,centerStartPos,variables.blockStartPos[variables.q],surround,surroundIndex,Constants.Enhancement_WT[2*hl-variables.q],variables.refinerVariables);		    
		}
	}
	
	
	public static void nearestNeighbor(short[] index,int indexIndex,short[] array,int arrayIndex,short value,int arrayLength,NearestNeighborVariables variables)
	{
		variables.reset();

		for(variables.i=0;variables.i<arrayLength;variables.i++)
		{
			variables.diff=(short)(array[arrayIndex++]-value);
			variables.crit[variables.i]=(variables.diff*variables.diff);
		}

		index[indexIndex]=(short)BasicFunctions.getMinIndex(variables.crit,0,arrayLength);			
	}
	
	public static void refiner(short[] startPos,int startPosIndex,short[] current,int currentIndex,int currentLength,int centerStartPos,short estimatedSegmentPos,short[] surround,int surroundIndex,short gain,RefinerVariables variables)
	{
		variables.reset();
		
		variables.estSegPosRounded=(short)((estimatedSegmentPos - 2)>>2);
		variables.searchSegStartPos=(short)(variables.estSegPosRounded-2);

		if (variables.searchSegStartPos<0)
			variables.searchSegStartPos=0;
		
		variables.searchSegEndPos=(short)(variables.estSegPosRounded+2);

		if(variables.searchSegEndPos+80 >= currentLength) 
			variables.searchSegEndPos = (short)(currentLength-81);
		
		variables.corrDim=(short)(variables.searchSegEndPos-variables.searchSegStartPos+1);
		
		variables.max=BasicFunctions.getMaxAbsValue(current, currentIndex+variables.searchSegStartPos, variables.corrDim+79);
		variables.scale=(short)BasicFunctions.getSize(variables.max);
		variables.scale = (short)((2*variables.scale)-26);
		if (variables.scale<0) 
			variables.scale=0;
		
		crossCorrelation(variables.corrVecTemp, 0, current, currentIndex+centerStartPos, current, currentIndex+variables.searchSegStartPos, (short)80, variables.corrDim, variables.scale, (short)1, variables.crossCorrelationVariables);		
		variables.maxTemp=BasicFunctions.getMaxAbsValue(variables.corrVecTemp,0,variables.corrDim);			
		variables.scaleFact=BasicFunctions.getSize(variables.maxTemp)-15;

		if (variables.scaleFact>0) 
		{
			for (variables.i=0;variables.i<variables.corrDim;variables.i++)
				variables.corrVec[variables.i]=(short)(variables.corrVecTemp[variables.i]>>variables.scaleFact);		    
		} 
		else 
		{
			for (variables.i=0;variables.i<variables.corrDim;variables.i++) 
				variables.corrVec[variables.i]=(short)variables.corrVecTemp[variables.i];		    
		}
		
		System.arraycopy(emptyArray, 0, variables.corrVec, variables.corrDim, 5-variables.corrDim);
		enhanceUpSample(variables.corrVecUps,0,variables.corrVec,0, variables.enhanceUpSampleVariables);		
		variables.tLoc=(short)BasicFunctions.getMaxIndex(variables.corrVecUps,0,4*variables.corrDim);
			
		startPos[startPosIndex] = (short)(variables.searchSegStartPos*4 + variables.tLoc + 4);
		variables.tLoc2 = (short)((variables.tLoc+3)>>2);
		variables.st=(short)(variables.searchSegStartPos+variables.tLoc2-3);

		if(variables.st<0)
		{
			System.arraycopy(emptyArray, 0, variables.vect, 0, 0-variables.st);
		    System.arraycopy(current, currentIndex, variables.vect, 0-variables.st, 86+variables.st);		    
		}
		else
		{
			variables.en=(short)(variables.st+86);
		    if(variables.en>currentLength)
		    {
		    	System.arraycopy(current, currentIndex+variables.st, variables.vect, 0, 86-variables.en+currentLength);
		    	System.arraycopy(emptyArray, 0, variables.vect, 86-variables.en+currentLength,variables.en-currentLength);		    	
		    }
		    else 
		    	System.arraycopy(current, currentIndex+variables.st, variables.vect, 0, 86);
		}
		
		variables.fraction=(short)(variables.tLoc2*4-variables.tLoc);
		variables.filterStateIndex = 6;
		variables.polyIndex = variables.fraction;
		
		for (variables.i=0;variables.i<7;variables.i++)
			variables.filt[variables.filterStateIndex--] = Constants.Enhancement_POLY_PHASER[variables.polyIndex][variables.i];
		
		BasicFunctions.filterMA(variables.vect,6,variables.vect,0,variables.filt,0,7,80);
		BasicFunctions.addAffineVectorToVector(surround, surroundIndex, variables.vect, 0, gain, 32768, (short)16, 80);		
	}
	
	
	public static final void enhanceUpSample(int[] useq,int useqIndex,short[] seq,int seqIndex,EnhanceUpSampleVariables variables)
	{
		variables.pu1=useqIndex;
		for (variables.j=0;variables.j<4; variables.j++) 
		{
			variables.pu11=variables.pu1;
			variables.pp=1;
		    variables.ps=seqIndex+2;		    
		    useq[variables.pu11] =  (seq[variables.ps--]*Constants.Enhancement_POLY_PHASER[variables.j][variables.pp++]);
		    useq[variables.pu11] += (seq[variables.ps--]*Constants.Enhancement_POLY_PHASER[variables.j][variables.pp++]);
		    useq[variables.pu11] += (seq[variables.ps--]*Constants.Enhancement_POLY_PHASER[variables.j][variables.pp++]);
		    
		    variables.pu11+=4;		   
		    variables.pp=1;
		    variables.ps=seqIndex+3;
		    useq[variables.pu11] =  (seq[variables.ps--]*Constants.Enhancement_POLY_PHASER[variables.j][variables.pp++]);
		    useq[variables.pu11] += (seq[variables.ps--]*Constants.Enhancement_POLY_PHASER[variables.j][variables.pp++]);
		    useq[variables.pu11] += (seq[variables.ps--]*Constants.Enhancement_POLY_PHASER[variables.j][variables.pp++]);
		    useq[variables.pu11] += (seq[variables.ps--]*Constants.Enhancement_POLY_PHASER[variables.j][variables.pp++]);
		    
		    variables.pu11+=4;		    
		    variables.pp=1;
		    variables.ps=seqIndex+4;
		    useq[variables.pu11] =  (seq[variables.ps--]*Constants.Enhancement_POLY_PHASER[variables.j][variables.pp++]);
		    useq[variables.pu11] += (seq[variables.ps--]*Constants.Enhancement_POLY_PHASER[variables.j][variables.pp++]);
		    useq[variables.pu11] += (seq[variables.ps--]*Constants.Enhancement_POLY_PHASER[variables.j][variables.pp++]);
		    useq[variables.pu11] += (seq[variables.ps--]*Constants.Enhancement_POLY_PHASER[variables.j][variables.pp++]);
		    useq[variables.pu11] += (seq[variables.ps--]*Constants.Enhancement_POLY_PHASER[variables.j][variables.pp++]);
		    variables.pu1++;
		}

		 
		variables.pu1 = useqIndex + 12;
		for (variables.j=0;variables.j<4; variables.j++) 
		{
			variables.pu11 = variables.pu1;
			variables.pp=2;
			variables.ps = seqIndex+4;
		    useq[variables.pu11] =  (seq[variables.ps--]*Constants.Enhancement_POLY_PHASER[variables.j][variables.pp++]);
		    useq[variables.pu11] += (seq[variables.ps--]*Constants.Enhancement_POLY_PHASER[variables.j][variables.pp++]);
		    useq[variables.pu11] += (seq[variables.ps--]*Constants.Enhancement_POLY_PHASER[variables.j][variables.pp++]);
		    useq[variables.pu11] += (seq[variables.ps--]*Constants.Enhancement_POLY_PHASER[variables.j][variables.pp++]);
		    
		    variables.pu11+=4;
		    variables.pp=3;
		    variables.ps = seqIndex+4;
		    useq[variables.pu11] =  (seq[variables.ps--]*Constants.Enhancement_POLY_PHASER[variables.j][variables.pp++]);
		    useq[variables.pu11] += (seq[variables.ps--]*Constants.Enhancement_POLY_PHASER[variables.j][variables.pp++]);
		    useq[variables.pu11] += (seq[variables.ps--]*Constants.Enhancement_POLY_PHASER[variables.j][variables.pp++]);
		    variables.pu1++;
		}
	}
	
	public static void smooth(short[] out,int outIndex,short[] current,int currentIndex,short[] surround,int surroundIndex,SmoothVariables variables)
	{		
		variables.w00 = variables.w10 = variables.w11 = 0;		
		variables.max1=BasicFunctions.getMaxAbsValue(current,currentIndex,80);
		variables.maxTotal=BasicFunctions.getMaxAbsValue(surround,surroundIndex,80);
			
		if(variables.max1>variables.maxTotal)
			variables.maxTotal=variables.max1;		

		variables.scale=BasicFunctions.getSize(variables.maxTotal);
		variables.scale = (short)((2*variables.scale)-26);
		if(variables.scale<0)
			variables.scale=0;

		variables.w00=BasicFunctions.scaleRight(current,currentIndex,current,currentIndex,80,variables.scale);			  
		variables.w11=BasicFunctions.scaleRight(surround,surroundIndex,surround,surroundIndex,80,variables.scale);
		variables.w10=BasicFunctions.scaleRight(surround,surroundIndex,current,currentIndex,80,variables.scale);

		if (variables.w00<0) variables.w00 = Integer.MAX_VALUE;
		if (variables.w11<0) variables.w11 = Integer.MAX_VALUE;

		variables.bitsW00 = BasicFunctions.getSize(variables.w00);
		variables.bitsW11 = BasicFunctions.getSize(variables.w11);
		if(variables.w10>0)
			variables.bitsW10 = BasicFunctions.getSize(variables.w10);
		else
			variables.bitsW10 = BasicFunctions.getSize(-variables.w10);
		
		variables.scale1 = (short)(31 - variables.bitsW00);
		variables.scale2 = (short)(15 - variables.bitsW11);

		if (variables.scale2>(variables.scale1-16)) 
			variables.scale2 = (short)(variables.scale1 - 16);
		else 
			variables.scale1 = (short)(variables.scale2 + 16);		  

		variables.w00Prim = (variables.w00<<variables.scale1);
		if(variables.scale2>0)
			variables.w11Prim = (short)(variables.w11>>variables.scale2);
		else
			variables.w11Prim = (short)(variables.w11<<variables.scale2);
		
		if (variables.w11Prim>64) 
		{
			variables.endiff=((variables.w00Prim/variables.w11Prim)<<6);
			variables.C = (short)BasicFunctions.sqrtFloor(variables.endiff);
		} 
		else
			variables.C = 1;
		  
		variables.tempIndex1=outIndex;
		variables.tempIndex2=surroundIndex;
		for(variables.i=0;variables.i<80;variables.i++) 
			out[variables.tempIndex1++]= (short)(((variables.C*surround[variables.tempIndex2++])+1024)>>11);
		
		variables.errors=0;
		variables.tempIndex1=outIndex;
		variables.tempIndex2=currentIndex;
		for(variables.i=0;variables.i<80;variables.i++) 
		{
			variables.error=(short)((current[variables.tempIndex2++]-out[variables.tempIndex1++])>>3);
			variables.errors+=(variables.error*variables.error);
		}
		  
		if ((6-variables.scale+variables.scale1) > 31) 
			variables.crit=0;
		else 
			variables.crit = ((819*(variables.w00Prim>>14))<<((6-variables.scale+variables.scale1)));
		  
		if (variables.errors > variables.crit) 
		{
		    if(variables.w00 < 1) 
		    	variables.w00=1;
		    
		    variables.scale1 = (short)(variables.bitsW00-15);
		    variables.scale2 = (short)(variables.bitsW11-15);

		    if (variables.scale2>variables.scale1) 
		    	variables.scale = variables.scale2;
		    else
		    	variables.scale = variables.scale1;
		    
		    variables.w11W00 = (variables.w11<<variables.scale)*(variables.w00<<variables.scale);
		    variables.w10W10 = (variables.w10<<variables.scale)*(variables.w10<<variables.scale);
		    variables.w00W00 = (variables.w00<<variables.scale)*(variables.w00<<variables.scale);

		    if(variables.w00W00>65536)
		    {
		    	variables.endiff = (variables.w11W00-variables.w10W10);
		    	if(variables.endiff<0)
		    		variables.endiff=0;
		    	
		    	variables.denom = (variables.endiff/(variables.w00W00>>16));
		    }
		    else
		    	variables.denom = 65536;
		    
		    if(variables.denom > 7)
		    {
		    	variables.scale=(short)(BasicFunctions.getSize(variables.denom)-15);

		    	if (variables.scale>0) 
		    	{		        
		    		variables.denom16=(short)(variables.denom>>variables.scale);
		    		variables.num=(848256041>>variables.scale);
		    	} 
		    	else 
		    	{
		    		variables.denom16=(short)variables.denom;
		    		variables.num=848256041;
		    	}

		    	variables.A = (short)BasicFunctions.sqrtFloor(variables.num/variables.denom16);

		    	variables.scale1 = (short)(31-variables.bitsW10);
		    	variables.scale2 = (short)(21-variables.scale1);
		    	variables.w10Prim = (variables.w10<<variables.scale1);		    	
		    	variables.w00Prim = (variables.w00<<variables.scale2);
		    	variables.scale = (short)(variables.bitsW00-variables.scale2-15);

		    	if (variables.scale>0) 
		    	{
		    		variables.w10Prim=(variables.w10Prim>>variables.scale);
		    		variables.w00Prim=(variables.w00Prim>>variables.scale);
		    	}

		    	if (variables.w00Prim>0 && variables.w10Prim>0) 
		    	{
		    		variables.w11DivW00=(variables.w10Prim/variables.w00Prim);

		    		if(BasicFunctions.getSize(variables.w11DivW00)+BasicFunctions.getSize(variables.A)>31) 
		    			variables.B32 = 0;
		    		else 
		    			variables.B32 = 1073741824 - 26843546 - (variables.A*variables.w11DivW00);
		        
		    		variables.B = (short)(variables.B32>>16);
		    	} 
		    	else 
		    	{
		    		variables.A = 0;
		    		variables.B = 16384;
		    	}
		    }
		    else
		    { 
		    	variables.A = 0;
		    	variables.B = 16384;
		    }
		    
		    BasicFunctions.scaleAndAddVectors(surround, surroundIndex, variables.A, 9,current, currentIndex, variables.B, 14, out, outIndex ,80);		    
		}
	}
}