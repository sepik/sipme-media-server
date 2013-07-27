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

import ua.mobius.media.server.spi.format.Format;
import ua.mobius.media.server.spi.format.FormatFactory;
import ua.mobius.media.server.spi.memory.ShortFrame;
import ua.mobius.media.server.spi.memory.ByteFrame;
import ua.mobius.media.server.spi.memory.ShortMemory;

/**
 * 
 * @author oifa yulian
 */
public class Decoder {	
	private final static Format linear = FormatFactory.createAudioFormat("linear", 8000, 16, 1);

    private short[] decResidual=new short[240];
    private short[] plcResidual=new short[250];
    private short[] syntDenum=new short[66];
    private short[] output=new short[240];
    private short[] plcLpc=new short[11];
    private short[] signal=new short[25];
    private short[] lsfDeq=new short[20];
    private short[] weightDenum=new short[66];
    
    private int i, mode , temp;
    
    private EncoderBits encoderBits=new EncoderBits();   
    private DecoderState decoderState=new DecoderState();    
    private CorrData corrData=new CorrData();        
    
    private UnpackBitsVariables unpackBitsVariables=new UnpackBitsVariables();
    private UpdateDecIndexVariables updateDecIndexVariables=new UpdateDecIndexVariables();
    private SimpleLsfDeqVariables simpleLsfDeqVariables=new SimpleLsfDeqVariables();
    private LsfCheckVariables lsfCheckVariables=new LsfCheckVariables();
    private DecodeInterpolateLsfVariables decodeInterpolateLsfVariables=new DecodeInterpolateLsfVariables();    
    private DecodeResidualVariables decodeResidualVariables=new DecodeResidualVariables();
    private DoThePlcVariables doThePlcVariables=new DoThePlcVariables();
    private EnhancerVariables enhancerVariables=new EnhancerVariables();
    private XCorrCoefVariables xCorrCoefVariables=new XCorrCoefVariables();
    private HpOutputVariables hpOutputVariables=new HpOutputVariables();
    
    public void reset()
    {
    	decoderState.reset();
    }
    
    public ShortFrame process(ByteFrame frame) {
    	
    	byte[] inputData = frame.getData();
    	
    	if(inputData.length==50)
    		mode=30;
    	else if(inputData.length==38)
    		mode=20;
    	else
    		throw new IllegalArgumentException("INVALID FRAME SIZE");
    	
    	decoderState.setMode(mode);
    	temp=inputData.length/2;
    	
    	for (i = 0; i < temp; i++) {
    		signal[i] = ((short) ((inputData[i*2] << 8) | (inputData[i*2 + 1] & 0xFF)));
        }
    	
    	CodingFunctions.unpackBits(encoderBits,signal,mode,unpackBitsVariables);
    	
    	if (encoderBits.getStartIdx()<1)
	      mode = 0;
	    if (decoderState.DECODER_MODE==20 && encoderBits.getStartIdx()>3)
	      mode = 0;
	    if (decoderState.DECODER_MODE==30 && encoderBits.getStartIdx()>5)
	      mode = 0;	    

	    if (mode>0) 
	    { 	
	    	CodingFunctions.updateDecIndex(encoderBits,updateDecIndexVariables);	    		    		    	
	    	CodingFunctions.simpleLsfDeq(lsfDeq, 0, encoderBits.getLSF(), 0, decoderState.LPC_N,simpleLsfDeqVariables);	 	    
	    	CodingFunctions.lsfCheck(lsfDeq, 0, 10,lsfCheckVariables);	    	
	    	CodingFunctions.decoderInterpolateLsf(decoderState,syntDenum, 0, weightDenum, 0, lsfDeq, 0, (short)10,decodeInterpolateLsfVariables);	 	    
	    	CodingFunctions.decodeResidual(decoderState, encoderBits, decResidual, 0, syntDenum, 0,decodeResidualVariables);	    		 	   	
	    	CodingFunctions.doThePlc(decoderState,plcResidual, 0, plcLpc, 0, (short)0, decResidual, 0, syntDenum, 11*(decoderState.SUBFRAMES-1), (short)(decoderState.getLastLag()),doThePlcVariables);
	    	System.arraycopy(plcResidual, 0, decResidual, 0, decoderState.SIZE);	    	
	    }
	    else 
        {
    	    CodingFunctions.doThePlc(decoderState,plcResidual, 0, plcLpc, 0, (short)1,decResidual, 0, syntDenum, 0, (short)(decoderState.getLastLag()),doThePlcVariables);
    	    System.arraycopy(plcResidual, 0, decResidual, 0, decoderState.SIZE);
    	   
    	    for (i = 0; i < decoderState.SUBFRAMES; i++)
    	    	System.arraycopy(plcLpc, 0, syntDenum, i*11, 11);    	          	   
        }

	    if(decoderState.getUseEnhancer()==1)
	    {
		    if(decoderState.getPrevEnchPl()==2)
		    	for (i=0;i<decoderState.SUBFRAMES;i++)
				   System.arraycopy(syntDenum, 0, decoderState.getOldSyntDenum(), i*11, 11);		          		  
		    
		    decoderState.setLastLag(CodingFunctions.enchancher(decResidual, 0, plcResidual,10, decoderState,enhancerVariables));
		    System.arraycopy(decoderState.getSynthMem(), 0, plcResidual, 0, 10);
		     
		    if(decoderState.DECODER_MODE==20) 
		    {
		        BasicFunctions.filterAR(plcResidual,10, plcResidual,10,decoderState.getOldSyntDenum(),(decoderState.SUBFRAMES-1)*11,11, 40);
		        
		        for (i=1; i < decoderState.SUBFRAMES; i++) 
		        	BasicFunctions.filterAR(plcResidual,10+40*i, plcResidual,10+40*i,syntDenum,(i-1)*11,11, 40);		          
		    }
		    else if(decoderState.DECODER_MODE==30)
		    {
			   for (i=0; i < 2; i++) 
				   BasicFunctions.filterAR(plcResidual,10+40*i, plcResidual,10+40*i,decoderState.getOldSyntDenum(),(i+4)*11,11, 40);
		        
			   for (i=2; i < decoderState.SUBFRAMES; i++) 
				   BasicFunctions.filterAR(plcResidual,10+40*i, plcResidual,10+40*i,syntDenum,(i-2)*11,11, 40);				   	       
		    }			    		  
	   }
	   else
	   {
	        if (mode!=30)
	           decoderState.setLastLag(CodingFunctions.xCorrCoef(decResidual, decoderState.SIZE-60, decResidual, decoderState.SIZE-80, (short)60, (short)80, (short)20, (short)-1, xCorrCoefVariables));  		 	
	        else
	  	    	decoderState.setLastLag(CodingFunctions.xCorrCoef(decResidual, decoderState.SIZE-80, decResidual, decoderState.SIZE-100, (short)80, (short)100, (short)20, (short)-1, xCorrCoefVariables));  		
	  	   
	  	    System.arraycopy(decResidual, 0, plcResidual, 10, decoderState.SIZE);
	  	    System.arraycopy(decoderState.getSynthMem(), 0, plcResidual, 0, 10);
	  	   
	  	    for (i=0; i < decoderState.SUBFRAMES; i++)
	  	      BasicFunctions.filterAR(plcResidual,10+40*i,plcResidual,10+40*i,syntDenum,11*i,11,40);
	   }
  	   
	   System.arraycopy(plcResidual, decoderState.SIZE, decoderState.getSynthMem(), 0, 10);
	   System.arraycopy(plcResidual, 10, output, 0, decoderState.SIZE);
	   CodingFunctions.hpOutput(output,0,Constants.HP_OUT_COEFICIENTS,decoderState.getHpiMemY(),decoderState.getHpiMemX(),decoderState.SIZE, hpOutputVariables);
	   System.arraycopy(syntDenum, 0, decoderState.getOldSyntDenum(), 0, decoderState.SUBFRAMES*11);              
       
       if (mode==0)
    	   decoderState.setPrevEnchPl(1);
       else
    	   decoderState.setPrevEnchPl(0);
       
       ShortFrame res;
       if(decoderState.DECODER_MODE==20)
          res = ShortMemory.allocate(160);    	       	  
       else
          res = ShortMemory.allocate(240);
    	   
       System.arraycopy(output, 0, res.getData(), 0, res.getData().length);
       res.setOffset(0);
       res.setLength(res.getData().length);
       res.setTimestamp(frame.getTimestamp());
       res.setDuration(frame.getDuration());
       res.setSequenceNumber(frame.getSequenceNumber());
       res.setEOM(frame.isEOM());
       res.setFormat(linear);
       return res;
    }               
}