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

package ua.mobius.media.server.impl.dsp.audio.nativeopus;

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
 * Uses libopus native implementation to transcode data
 */

public class Codec implements AudioCodec {
	private final static String LIB_NAME = "mobius-nativeopus";

    static {
        try {
            System.loadLibrary(LIB_NAME);
            System.out.println("Loaded library mobius-nativeopus");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    //used to resolve concurrency when generating jni interface linked list
    private static Semaphore accessSemaphore=new Semaphore(1);
    
	private final static Format opus = FormatFactory.createAudioFormat("opus", 48000);
	private final static Format linear = FormatFactory.createAudioFormat("linear", 8000, 16, 1);

	private long linkReference;
	private int i,encodeLength,decodeLength;
	
	//maximum packet duration (120ms; 5760 for 48kHz) , do not change may be usefull in future, currently will use much less
	private short[] decodedCache=new short[5760];
	
	//maximum rate for opus is 512000 bits per second=64000 bytes per second = 1280 bytes per packet , we use 20ms frames on mms
	private byte[] encodedCache=new byte[1280];
	
	private native int encode(short[] rawAudio,byte[] encodedAudio,long linkReference);

	private native int decode(byte[] encodedAudio, short[] rawAudio,long linkReference);

	private native long createCodec(int bandwidth);    
    
	private native int resetCodec(long linkReference,int bandwidth);
	
	public Codec()
	{
		try
		{
			accessSemaphore.acquire();
		}
		catch(Exception ex)
		{
			
		}
		
		linkReference=createCodec(8000);		
		accessSemaphore.release();
	}
	
    
    public Format getSupportedFormat() {
        return opus;
    }

    public ShortFrame decode(ByteFrame frame)
    {
    	byte[] inputData = frame.getData();
    	decodeLength=decode(frame.getData(), decodedCache, linkReference);
    	ShortFrame res = ShortMemory.allocate(decodeLength);
    	System.arraycopy(decodedCache, 0, res.getData(), 0, decodeLength);
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
		encodeLength=encode(data,encodedCache,linkReference);		
		ByteFrame res = ByteMemory.allocate(encodeLength);		
		System.arraycopy(encodedCache, 0, res.getData(), 0, encodeLength);
		res.setOffset(0);
        res.setLength(res.getData().length);
        res.setTimestamp(frame.getTimestamp());
        res.setDuration(frame.getDuration());
        res.setSequenceNumber(frame.getSequenceNumber());
        res.setEOM(frame.isEOM());
        res.setFormat(opus);
		return res;
	}       
	
	public void reset()
	{
		resetCodec(linkReference,8000);
	}
}