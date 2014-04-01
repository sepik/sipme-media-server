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
import ua.mobius.media.MediaSink;
import ua.mobius.media.MediaSource;
import ua.mobius.media.server.component.audio.AudioInput;
import ua.mobius.media.server.component.oob.OOBInput;
import ua.mobius.media.server.impl.AbstractCompoundSource;
import ua.mobius.media.server.impl.rtp.RtpClock;
import ua.mobius.media.server.impl.rtp.sdp.RTPFormat;
import ua.mobius.media.server.io.network.ProtocolHandler;
import ua.mobius.media.server.scheduler.Scheduler;
import ua.mobius.media.server.scheduler.Task;
import ua.mobius.media.server.spi.FormatNotSupportedException;
import ua.mobius.media.server.spi.ConnectionMode;
import ua.mobius.media.server.spi.format.AudioFormat;
import ua.mobius.media.server.spi.format.FormatFactory;
import ua.mobius.media.server.spi.format.Formats;
import ua.mobius.media.server.spi.memory.ByteMemory;
import ua.mobius.media.server.spi.memory.ShortFrame;
import ua.mobius.media.server.spi.memory.ByteFrame;
import ua.mobius.media.server.spi.dsp.AudioCodec;
import ua.mobius.media.server.spi.dsp.AudioProcessor;
import org.apache.log4j.Logger;
import java.util.ArrayList;
/**
 *
 * @author Oifa Yulian
 */
/**
 * Receiver implementation.
 *
 * The Media source of RTP data.
 */
public class RTPInput extends AbstractCompoundSource implements BufferListener {
	private AudioFormat format = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);	
	private final static AudioFormat dtmf = FormatFactory.createAudioFormat("telephone-event", 8000);
    
	private long period = 20000000L;
    private int packetSize = (int)(period / 1000000) * format.getSampleRate()/1000 * format.getSampleSize() / 8;
    private int dtmfPacketSize = 4;
    
    private ArrayList<ByteFrame> frameBuffer=new ArrayList(5);
    private ByteFrame currFrame;
    
    //jitter buffer
    private JitterBuffer rxBuffer;
    
	//digital signaling processor
    private AudioProcessor dsp;
           
    protected Integer preEvolveCount=0;
    protected Integer evolveCount=0;
    
    private static final Logger logger = Logger.getLogger(RTPInput.class);
    
    private byte currTone=(byte)0xFF;
    private int latestDuration=0;
    private int latestSeq=0;
    
    private boolean hasEndOfEvent;
    private long endTime=0;
    private int endSeq=0;
    
    private int eventDuration=0;
    byte[] data = new byte[4];
    
    boolean endOfEvent;
    
    private RtpClock clock;    
    
    private AudioInput input;
    private OOBInput oobInput;
    
    private int oobToneDuration;
    private int oobEndTonePackets;
    private Boolean growEndDuration;
	/**
     * Creates new receiver.
     */
    protected RTPInput(Scheduler scheduler,JitterBuffer rxBuffer,RtpClock clock,int oobToneDuration,int oobEndTonePackets,Boolean growEndDuration) {
        super("rtpinput", scheduler,scheduler.INPUT_QUEUE);
        this.rxBuffer=rxBuffer;        
        
        this.clock=clock;
    	this.clock.setClockRate(8000);
    	
    	input=new AudioInput(1,packetSize);
        this.connect(input);  
        
        oobInput=new OOBInput(1);
        this.connect(oobInput);  
        
        this.oobEndTonePackets=oobEndTonePackets;
        this.oobToneDuration=oobToneDuration;
        this.growEndDuration=growEndDuration;
    }

    public AudioInput getAudioInput()
    {
    	return this.input;
    }
    
    public OOBInput getOOBInput()
    {
    	return this.oobInput;
    }
    
    public void setClock(RtpClock clock) 
    {
        this.clock = clock;
        this.clock.setClockRate(8000);        
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
    
    protected int getPacketsLost() {
        return 0;
    }    

    @Override
    public ShortFrame evolve(long timestamp) {
    	ByteFrame currFrame=rxBuffer.read(timestamp);
    	
    	ShortFrame outputFrame=null;
    	if(currFrame!=null)
        {
    		if (dsp != null) {
        		try
        		{
        			outputFrame = dsp.decode(currFrame);
        		}
        		catch(Exception e)
        		{        			
        			logger.error(e);
        		}
        	}        	
        }
    	
    	return outputFrame; 
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
    
    public void write(RtpPacket event) 
    {
    	//obtain payload        
        event.getPyalod(data, 0);
        
        if(data.length==0)
        	return;
    	
    	boolean endOfEvent=false;
        if(data.length>1)
            endOfEvent=(data[1] & 0X80)!=0;
        
       //lets ignore end of event packets
        if(endOfEvent)
        {
        	hasEndOfEvent=true;
        	endTime=event.getTimestamp();
        	endSeq=event.getSeqNumber();
        	return;                                       
        }
        
        eventDuration=(data[2]<<8) | (data[3] & 0xFF);    	
    	
        //lets update sync data , allowing same tone come after 160ms from previous tone , not including end of tone
        if(currTone==data[0])
        {
        	if(hasEndOfEvent)
        	{
        		if((event.getSeqNumber()<=endSeq && event.getSeqNumber()>(endSeq-8)))
        			//out of order , belongs to same event 
        			//if comes after end of event then its new one
        			return;
        	}
        	else
        	{
        		if((event.getSeqNumber()<(latestSeq+8)) && event.getSeqNumber()>(latestSeq-8))
        		{
        			if(event.getSeqNumber()>latestSeq)
        			{
        				latestSeq=event.getSeqNumber();
        				latestDuration=eventDuration;
        			}
        		
        			return;
        		}
        	
        		if(eventDuration<(latestDuration+1280) && eventDuration>(latestDuration-1280))        		
        		{
        			if(eventDuration>latestDuration)
        			{
        				latestSeq=event.getSeqNumber();
        				latestDuration=eventDuration;
        			}
        		
        			return;
        		}
        	}
        }
        
        hasEndOfEvent=false;
    	endTime=0;
    	endSeq=0;
        latestSeq=event.getSeqNumber();
        latestDuration=eventDuration;
        currTone=data[0];
        for(int i=0;i<(oobToneDuration/20);i++)
        {
        	currFrame = ByteMemory.allocate(dtmfPacketSize);
        	byte[] newData=currFrame.getData();
        	newData[0]=data[0];
        	newData[1]=(byte)(0x3F & data[1]);
        	eventDuration=(short)(160*i);
        	newData[2]=(byte)((eventDuration>>8) & 0xFF);
        	newData[3]=(byte)(eventDuration & 0xFF);        	
        	currFrame.setSequenceNumber(event.getSeqNumber()+i);
        	currFrame.setOffset(0);
            currFrame.setLength(dtmfPacketSize);
            currFrame.setFormat(dtmf);
            currFrame.setDuration(period);
            currFrame.setTimestamp(clock.convertToAbsoluteTime(event.getTimestamp() + 20*i));
            frameBuffer.add(currFrame);                       
        }
        
        for(int i=0;i<oobEndTonePackets;i++)
        {
        	currFrame = ByteMemory.allocate(dtmfPacketSize);
        	byte[] newData=currFrame.getData();
        	newData[0]=data[0];
        	newData[1]=(byte)(0x80 | data[1]);
        	
        	if(growEndDuration)
        		eventDuration=(short)(160*(i + oobToneDuration/20));
        	else
        		eventDuration=(short)(160*(oobToneDuration/20));
        	
        	newData[2]=(byte)((eventDuration>>8) & 0xFF);
        	newData[3]=(byte)(eventDuration & 0xFF);
        	currFrame.setSequenceNumber(event.getSeqNumber()+i);
        	currFrame.setOffset(0);
            currFrame.setLength(dtmfPacketSize);
            currFrame.setFormat(dtmf);
            currFrame.setDuration(period);
            currFrame.setTimestamp(clock.convertToAbsoluteTime(event.getTimestamp() + 20*i));
            frameBuffer.add(currFrame);                       
        }
        
        wakeup();
    }
    
    @Override
    public void activate()
    {
    	input.resetBuffer();
    	super.activate();
    }
    
    public void resetBuffer()
    {
    	input.resetBuffer();
    }
    
    public void setInitialAudioChannelBuffer(int value)
    {
    	this.input.setInitialAudioChannelBuffer(value);
    }
    
    public void setMaxAudioChannelBuffer(int value)
    {
    	this.input.setMaxAudioChannelBuffer(value);
    }
    
    @Override
    public ByteFrame evolveOOB(long timestamp) 
    {
    	if (frameBuffer.size()==0)
    		return null;    	
    	
    	return frameBuffer.remove(0);    	
    }
    
    public void reset() 
    {
    	hasEndOfEvent=false;
    	endTime=0;
    	endSeq=0;
    	latestSeq=0;
    	latestDuration=0;
		currTone=(byte)0xFF;		
    }
}