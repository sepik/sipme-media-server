/*
 * Copyright 2013, Mobius Software Ltd. and individual contributors
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
 * For webrtc ilbc implementation license please see license file in 
 * native/src/main/native folder
 */

package ua.mobius.media.server.impl.dsp.audio.nativeilbc;

import ua.mobius.media.server.spi.dsp.AudioCodec;
import ua.mobius.media.server.spi.format.Format;
import ua.mobius.media.server.spi.format.FormatFactory;
import ua.mobius.media.server.spi.memory.ShortFrame;
import ua.mobius.media.server.spi.memory.ByteFrame;
import ua.mobius.media.server.spi.memory.ShortMemory;
import ua.mobius.media.server.spi.memory.ByteMemory;

import java.util.concurrent.Semaphore;
/**
 * 
 * @author oifa yulian
 * Uses webrtc ilbc native implementation to transcode data
 */

public class Codec implements AudioCodec {
	private final static String LIB_NAME = "mobius-nativeilbc";

    static {
        try {
            System.loadLibrary(LIB_NAME);
            System.out.println("Loaded library mobius-nativeilbc");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    //used to resolve concurrency when generating jni interface linked list
    private static Semaphore accessSemaphore=new Semaphore(1);
    
	private final static Format ilbc = FormatFactory.createAudioFormat("ilbc", 8000, 16, 1);
	private final static Format linear = FormatFactory.createAudioFormat("linear", 8000, 16, 1);

	private long linkReference;
	private int currentMode=20;
	private short[] signal30=new short[25];
	private short[] signal20=new short[19];
	private short[] currentSignal;
	private int i;
	
	private native int encode(short[] rawAudio,byte[] encodedAudio,long linkReference);

	private native int decode(short[] encodedAudio, short[] rawAudio,long linkReference);

	private native long createCodec();    
    
	private native int resetCodec(long linkReference,int currentMode);
	
	public Codec()
	{
		try
		{
			accessSemaphore.acquire();
		}
		catch(Exception ex)
		{
			
		}
		
		linkReference=createCodec();
		resetCodec(linkReference,currentMode);
		
		accessSemaphore.release();
	}
	
    
    public Format getSupportedFormat() {
        return ilbc;
    }

    public ShortFrame decode(ByteFrame frame)
    {
    	byte[] inputData = frame.getData();    	
    	
    	ShortFrame res;
    	if(inputData.length==50)
    	{
    		if(currentMode!=30)
    		{
    			currentMode=30;
    			resetCodec(linkReference,currentMode);    			
    		}
    		
    		res = ShortMemory.allocate(240);
    		for (i = 0; i < 25; i++)
        		signal30[i] = ((short) ((inputData[i*2] << 8) | (inputData[i*2 + 1] & 0xFF)));
            
    		currentSignal=signal30;
    	}
    	else if(inputData.length==38)
    	{
    		if(currentMode!=20)
    		{
    			currentMode=20;
    			resetCodec(linkReference,currentMode);    			
    		}
    		
    		res = ShortMemory.allocate(160);
    		for (i = 0; i < 19; i++)
        		signal20[i] = ((short) ((inputData[i*2] << 8) | (inputData[i*2 + 1] & 0xFF)));
            
    		currentSignal=signal20;
    	}
    	else
    		throw new IllegalArgumentException("INVALID FRAME SIZE");
    	
    	decode(currentSignal, res.getData(), linkReference);
        res.setOffset(0);
        res.setLength(res.getData().length);
        res.setTimestamp(frame.getTimestamp());
        res.setDuration(frame.getDuration());
        res.setSequenceNumber(frame.getSequenceNumber());
        res.setEOM(frame.isEOM());
        res.setFormat(linear);
        return res;        
    }

	public ByteFrame encode(ShortFrame frame)
	{
		short[] data = frame.getData();
		
		ByteFrame res = ByteMemory.allocate(38);
		encode(data,res.getData(),linkReference);		
		res.setOffset(0);
        res.setLength(38);
        res.setTimestamp(frame.getTimestamp());
        res.setDuration(frame.getDuration());
        res.setSequenceNumber(frame.getSequenceNumber());
        res.setEOM(frame.isEOM());
        res.setFormat(ilbc);
		return res;
	}       
	
	public void reset()
	{
		currentMode=20;
		resetCodec(linkReference,currentMode);		
	}
}