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
package ua.mobius.media.server.impl.rtp;

import ua.mobius.media.server.impl.rtp.sdp.RTPFormats;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.text.Format;
import ua.mobius.media.server.component.audio.AudioOutput;
import ua.mobius.media.server.component.oob.OOBOutput;
import ua.mobius.media.MediaSink;
import ua.mobius.media.MediaSource;
import ua.mobius.media.server.concurrent.ConcurrentCyclicFIFO;
import ua.mobius.media.server.impl.AbstractCompoundSink;
import ua.mobius.media.server.impl.rtp.sdp.RTPFormat;
import ua.mobius.media.server.io.network.ProtocolHandler;
import ua.mobius.media.server.scheduler.Scheduler;
import ua.mobius.media.server.scheduler.Task;
import ua.mobius.media.server.spi.dtmf.DtmfTonesData;
import ua.mobius.media.server.spi.FormatNotSupportedException;
import ua.mobius.media.server.spi.ConnectionMode;
import ua.mobius.media.server.spi.format.AudioFormat;
import ua.mobius.media.server.spi.format.FormatFactory;
import ua.mobius.media.server.spi.format.Formats;
import ua.mobius.media.server.spi.memory.ByteFrame;
import ua.mobius.media.server.spi.memory.ShortFrame;
import ua.mobius.media.server.spi.memory.ShortMemory;
import ua.mobius.media.server.spi.dsp.AudioCodec;
import ua.mobius.media.server.spi.dsp.AudioProcessor;
import org.apache.log4j.Logger;
/**
 *
 * @author Yulian oifa
 */
/**
 * Transmitter implementation.
 *
 */
public class RTPOutput extends AbstractCompoundSink {
	private AudioFormat format = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);	
	private long period = 20000000L;
    private int packetSize = (int)(period / 1000000) * format.getSampleRate()/1000 * format.getSampleSize() / 16;
    
    private RTPDataChannel channel;
    
    //signaling processor
    private AudioProcessor dsp;                               
        
    private static final Logger logger = Logger.getLogger(RTPOutput.class);
    
    private AudioOutput output;
    private OOBOutput oobOutput;
    
    private byte[] data;
    private byte[] cacheData;
	private short[] shortData;
	private short[] toneData,audioData;
    private int eventDuration;
    private int count=0;
    
    private ConcurrentCyclicFIFO<ShortFrame> oobFrames=new ConcurrentCyclicFIFO<ShortFrame>();
    private ShortFrame currFrame=null,outputFrame=null;
    /**
     * Creates new transmitter
     */
    protected RTPOutput(Scheduler scheduler,RTPDataChannel channel) {
        super("Output");
        this.channel=channel;
        output=new AudioOutput(scheduler,1);
        output.join(this); 
        
        oobOutput=new OOBOutput(scheduler,1);
        oobOutput.join(this);        
    }
    
    public AudioOutput getAudioOutput()
    {
    	return this.output;
    }
    
    public OOBOutput getOOBOutput()
    {
    	return this.oobOutput;
    }
    
    public void activate()
    {
    	output.start();
    	oobOutput.start();
    }
    
    public void deactivate()
    {
    	output.stop();
    	oobOutput.stop();
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
    }
    
    /**
     * Gets the digital signaling processor associated with this media source
     *
     * @return DSP instance.
     */
    public AudioProcessor getDsp() {
        return this.dsp;
    }

    @Override
    public void onMediaTransfer(ShortFrame frame) throws IOException {
    	outputFrame=oobFrames.poll();
    	if(outputFrame==null)
    		outputFrame=frame;
    	else
    		frame.recycle();
    	
    	//do transcoding
    	ByteFrame byteFrame=null;
    	if (dsp != null) {
    		try
    		{
    			byteFrame = dsp.encode(outputFrame);            			
    		}
    		catch(Exception e)
    		{
    			//transcoding error , print error and try to move to next frame
    			logger.error(e);
    			return;
    		} 
    	}
    	
    	if(byteFrame==null)
    		return;
    	
    	channel.send(byteFrame);
    }   
    
    @Override
    public void onMediaTransfer(ByteFrame frame) throws IOException {
    	if(channel.couldSendDtmf())
    		channel.sendDtmf(frame);
    	else
    	{
    		//get duration , if duration is lower then 1600 create packet
    		data=frame.getData();
    		eventDuration=(data[2]<<8) | (data[3] & 0xFF);    	
        	if(eventDuration<800)
        	{
        		currFrame=ShortMemory.allocate(packetSize);
        		cacheData=DtmfTonesData.buffer[data[0]];
        		shortData=currFrame.getData();
        		count=eventDuration;
        		for(int j=0;j<shortData.length;j++,count+=2)
        			shortData[j] = ((short) ((cacheData[count+1] << 8) | (cacheData[count] & 0xFF)));
        		
        		//may be in pool only due to order change , should not be otherwise
        		if(oobFrames.size()>5)
        			oobFrames.poll().recycle();
        		
        		oobFrames.offer(currFrame);
        	}
    	}
    		
    } 
}