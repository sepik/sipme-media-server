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
package ua.mobius.media.server.io.ss7;

import java.io.IOException;
import java.text.Format;
import java.util.ArrayList;

import ua.mobius.media.server.component.audio.AudioInput;
import ua.mobius.media.MediaSink;
import ua.mobius.media.MediaSource;
import ua.mobius.media.hardware.dahdi.Channel;
import ua.mobius.media.server.impl.AbstractAudioSource;
import ua.mobius.media.server.io.ss7.ProtocolHandler;
import ua.mobius.media.server.scheduler.Scheduler;
import ua.mobius.media.server.scheduler.Task;
import ua.mobius.media.server.spi.FormatNotSupportedException;
import ua.mobius.media.server.spi.ConnectionMode;
import ua.mobius.media.server.spi.format.AudioFormat;
import ua.mobius.media.server.spi.format.FormatFactory;
import ua.mobius.media.server.spi.format.Formats;
import ua.mobius.media.server.spi.memory.ByteMemory;
import ua.mobius.media.server.spi.memory.ByteFrame;
import ua.mobius.media.server.spi.memory.ShortFrame;
import ua.mobius.media.server.spi.dsp.AudioCodec;
import ua.mobius.media.server.spi.dsp.AudioProcessor;
import org.apache.log4j.Logger;
/**
 *
 * @author Oifa Yulian
 */
/**
 * Receiver implementation.
 *
 * The Media source of RTP data.
 */
public class SS7Input extends AbstractAudioSource {
	private AudioFormat format = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);	
	private long period = 20000000L;
    private int packetSize = (int)(period / 1000000) * format.getSampleRate()/1000 * format.getSampleSize() / 8;
    
    private AudioFormat sourceFormat;
	
    //digital signaling processor
    private AudioProcessor dsp;
           
    private Channel channel;
    
    private byte[] smallBuffer=new byte[32];
    private byte[] tempBuffer=new byte[160];
    private int currPosition=0;
    private int seqNumber=1;
    private int readBytes=0;
    private int currIndex=0;
    
    private ArrayList<ShortFrame> framesBuffer=new ArrayList(2);
    
    private AudioInput input;
    /**
     * Creates new receiver.
     */
    protected SS7Input(Scheduler scheduler,Channel channel,AudioFormat sourceFormat) {
        super("input", scheduler,scheduler.INPUT_QUEUE);               
        this.channel=channel;
        this.sourceFormat=sourceFormat;
        
        input=new AudioInput(1,packetSize);
        this.connect(input);
    }

    public AudioInput getAudioInput()
    {
    	return this.input;
    }
    
    /**
     * Assigns the digital signaling processor of this component.
     * The DSP allows to get more output formats.
     *
     * @param dsp the dsp instance
     */
    public void setDsp(AudioProcessor dsp) {
        //assign processor
        this.dsp = dsp;  
        this.dsp.setSourceFormat(this.sourceFormat);
    }
    
    /**
     * Gets the digital signaling processor associated with this media source
     *
     * @return DSP instance.
     */
    public AudioProcessor getDsp() {
        return this.dsp;
    }
    
    protected int getPacketsLost() {
        return 0;
    }    

    @Override
    public ShortFrame evolve(long timestamp) {
    	if(framesBuffer.size()>0)
    		return framesBuffer.remove(0);
    	
    	return null;
    }
    
    public void setSourceFormat(AudioFormat sourceFormat)
    {
    	this.sourceFormat=sourceFormat;
    	this.dsp.setSourceFormat(this.sourceFormat);
    }
    
    public void readData()
    {
    	readBytes=0;    
    	try
    	{
    		readBytes=channel.read(smallBuffer);
    	}
    	catch(IOException e)
    	{    		    	
    	}
    	
    	if(readBytes==0)
    		return;
    	
    	currIndex=0;
    	if(currPosition+readBytes<=tempBuffer.length)
    	{
    		System.arraycopy(smallBuffer, 0, tempBuffer, currPosition, readBytes);
    		currPosition+=readBytes;
    		currIndex=readBytes;
    	}
    	else
    	{
    		System.arraycopy(smallBuffer, 0, tempBuffer, currPosition, tempBuffer.length-currPosition);
    		currPosition=tempBuffer.length;
    		currIndex=tempBuffer.length-currPosition;
    	}
    	
    	if(currPosition==tempBuffer.length)
    	{
    		ByteFrame currFrame=ByteMemory.allocate(tempBuffer.length);
    		//put packet into buffer irrespective of its sequence number
    		currFrame.setHeader(null);
    		currFrame.setSequenceNumber(seqNumber++);
    		//here time is in milliseconds
    		currFrame.setTimestamp(System.currentTimeMillis());
    		currFrame.setOffset(0);
    		currFrame.setLength(tempBuffer.length);
    		currFrame.setDuration(20000000L);
    		System.arraycopy(tempBuffer, 0, currFrame.getData(), 0, tempBuffer.length);

    		//set format
    		currFrame.setFormat(this.sourceFormat);
    		
    		ShortFrame outputFrame=null;
    		//do the transcoding job
			if (dsp != null && this.sourceFormat!=null) {
				try
				{
					outputFrame = dsp.decode(currFrame);
				}
				catch(Exception e)
				{
    				//transcoding error 				
    			}
    		}   
    		
    		currPosition=0;
    		if(currIndex<readBytes)
    			System.arraycopy(smallBuffer, currIndex, tempBuffer, currPosition, readBytes-currIndex);
        	
    		//lets keep 2 last packets all the time,there is no reason to keep more
    		if(framesBuffer.size()>1)
    			framesBuffer.remove(0);
    		
    		framesBuffer.add(outputFrame);
    		onFill();
    	}
    }
    
    /**
     * RX buffer's call back method.
     * 
     * This method is called when rxBuffer is full and it is time to start
     * transmission to the consumer.
     */
    public void onFill() {
    	this.wakeup();
    }        
}