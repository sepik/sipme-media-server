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

import java.io.Serializable;
import java.util.ArrayList;

import ua.mobius.media.server.impl.rtp.sdp.RTPFormat;
import ua.mobius.media.server.impl.rtp.sdp.RTPFormats;
import ua.mobius.media.server.spi.format.AudioFormat;
import ua.mobius.media.server.spi.format.FormatFactory;
import ua.mobius.media.server.spi.memory.ByteFrame;
import ua.mobius.media.server.spi.memory.ByteMemory;
import ua.mobius.media.server.utils.Text;
import ua.mobius.media.server.spi.dsp.AudioProcessor;

import org.apache.log4j.Logger;

/**
 * Implements jitter buffer.
 * 
 * A jitter buffer temporarily stores arriving packets in order to minimize
 * delay variations. If packets arrive too late then they are discarded. A
 * jitter buffer may be mis-configured and be either too large or too small.
 * 
 * If a jitter buffer is too small then an excessive number of packets may be
 * discarded, which can lead to call quality degradation. If a jitter buffer is
 * too large then the additional delay can lead to conversational difficulty.
 * 
 * A typical jitter buffer configuration is 30mS to 50mS in size. In the case of
 * an adaptive jitter buffer then the maximum size may be set to 100-200mS. Note
 * that if the jitter buffer size exceeds 100mS then the additional delay
 * introduced can lead to conversational difficulty.
 *
 * @author oifa yulian
 */
public class JitterBuffer implements Serializable {    
    //The underlying buffer size
    private static final int QUEUE_SIZE = 10;
    //the underlying buffer
    private ArrayList<ByteFrame> queue = new ArrayList(QUEUE_SIZE);
    
    //RTP clock
    private RtpClock rtpClock;
    //first received sequence number
    private long isn = -1;

    //allowed jitter
    private long jitterBufferSize;
    
    //current jitter
    private long currentJitterBufferSize;
    
    //allowed max jitter
    private long maxJitterBufferSize=900;
    
    //packet arrival dead line measured on RTP clock.
    //initial value equals to infinity
    private long arrivalDeadLine = 0;

    //packet arrival dead line measured on RTP clock.
    //initial value equals to infinity
    private long droppedInRaw = 0;
    
    //The number of dropped packets
    private int dropCount;

    //known duration of media wich contains in this buffer.
    private volatile long duration;

    //buffer's monitor
    private BufferListener listener;

    private volatile boolean ready;
    
    private AudioProcessor dsp;
    
    /**
     * used to calculate network jitter.
     * currentTransit measures the relative time it takes for an RTP packet 
     * to arrive from the remote server to MMS
     */
    private long currentTransit = 0;
    
    /**
     * continuously updated value of network jitter 
     */
    private long currentJitter = 0;
    
    //transmission formats
    private RTPFormats rtpFormats = new RTPFormats();
    
    //currently used format
    private RTPFormat format;
    
    private Boolean useBuffer=true;
    
    private RTPDataChannel channel=null;
    
    private int[] lastBursts=new int[3];  
    
    private final static Logger logger = Logger.getLogger(JitterBuffer.class);
    /**
     * Creates new instance of jitter.
     * 
     * @param clock the rtp clock.
     */
    public JitterBuffer(RtpClock clock, int jitterBufferSize,int maxJitterBufferSize,RTPDataChannel channel) 
    {
        this.rtpClock = clock;
        this.jitterBufferSize = jitterBufferSize;
        this.currentJitterBufferSize=this.jitterBufferSize*2;
        this.channel=channel;
    }

    private void initJitter(RtpPacket firstPacket) {
        long arrival = rtpClock.getLocalRtpTime();
        long firstPacketTimestamp = firstPacket.getTimestamp();
        currentTransit = arrival - firstPacketTimestamp;
    }
    
    public void setDsp(AudioProcessor dsp) {
        //assign processor
        this.dsp = dsp; 
        if(this.format!=null)
        {
        	this.dsp.setSourceFormat(this.format.getFormat());
    		this.dsp.setDestinationFormat(this.format.getFormat());
        }   
        else if(this.rtpFormats!=null && this.rtpFormats.size()>0)
        {
        	RTPFormat current=this.rtpFormats.getRtpFormatByIndex(0);
    		this.dsp.setSourceFormat(current.getFormat());
    		this.dsp.setDestinationFormat(current.getFormat());
        }
    }
    
    /**
     * 
     * Calculates the current network jitter, which is 
     * an estimate of the statistical variance of the RTP data packet interarrival time:
     * http://tools.ietf.org/html/rfc3550#appendix-A.8
     * 
     */
    private void estimateJitter(RtpPacket newPacket) {
            long arrival = rtpClock.getLocalRtpTime();
            long newPacketTimestamp = newPacket.getTimestamp();
            long transit = arrival - newPacketTimestamp;
        long d = transit - currentTransit;
        if (d<0) d = -d;
        // logger.info(String.format("recalculating jitter: arrival=%d, newPacketTimestamp=%d, transit=%d, transit delta=%d", arrival, newPacketTimestamp, transit, d ));
        currentTransit = transit;           
            currentJitter += d - ((currentJitter + 8) >> 4);  
    }
    
    /**
     * 
     * @return the current value of the network RTP jitter. The value is in normalized form as specified in RFC 3550 
     * http://tools.ietf.org/html/rfc3550#appendix-A.8
     */
    public long getEstimatedJitter() {
            long jitterEstimate = currentJitter >> 4; 
            // logger.info(String.format("Jitter estimated at %d. Current transit time is %d.", jitterEstimate, currentTransit));
            return jitterEstimate;
    }
    
    public void setFormats(RTPFormats rtpFormats) {
        this.rtpFormats = rtpFormats;
        if(this.dsp!=null && rtpFormats.size()>0)
		{
        	RTPFormat current=this.rtpFormats.getRtpFormatByIndex(0);
    		this.dsp.setSourceFormat(current.getFormat());
    		this.dsp.setDestinationFormat(current.getFormat());
		}
    }
    
    /**
     * Gets the interarrival jitter.
     *
     * @return the current jitter value.
     */
    public double getJitter() {
        return 0;
    }

    /**
     * Gets the maximum interarrival jitter.
     *
     * @return the jitter value.
     */
    public double getMaxJitter() {
        return 0;
    }
    
    /**
     * Get the number of dropped packets.
     * 
     * @return the number of dropped packets.
     */
    public int getDropped() {
        return dropCount;
    }
    
    public boolean bufferInUse()
    {
    	return this.useBuffer;
    }
    
    public void setBufferInUse(boolean useBuffer)
    {
    	this.useBuffer=useBuffer;
    }
    
    /**
     * Assigns listener for this buffer.
     * 
     * @param listener the listener object.
     */
    public void setListener(BufferListener listener) {
        this.listener = listener;
    }
    
    /**
     * Accepts specified packet
     *
     * @param packet the packet to accept
     */
    public void write(RtpPacket packet,RTPFormat format) {    	
    	//checking format
    	if(format==null)
    		return;
    	
    	if(this.format==null || this.format.getID() != format.getID())
    	{
    		restart();
    		this.format=format;
    		if(this.dsp!=null)
    		{
	    		this.dsp.setSourceFormat(this.format.getFormat());
	    		this.dsp.setDestinationFormat(this.format.getFormat());
    		}
    		
    		logger.info("Format has been changed: " + this.format.toString()); 
    	}
    	    	
    	//if this is first packet then synchronize clock
    	if (isn == -1) {
    		rtpClock.synchronize(packet.getTimestamp());
    		isn = packet.getSeqNumber();
    		initJitter(packet);
    	}
    	else
            estimateJitter(packet);
            
    	//update clock rate
    	//all our codecs works on real 8000
    	rtpClock.setClockRate(8000);            		    		
        
    	ByteFrame f=null;
    	//drop outstanding packets
		//packet is outstanding if its timestamp of arrived packet is less
		//then consumer media time
		if (packet.getTimestamp() < this.arrivalDeadLine) {
			logger.info("drop packet: dead line=" + arrivalDeadLine
                + ", packet time=" + packet.getTimestamp() + ", seq=" + packet.getSeqNumber()
                + ", payload length=" + packet.getPayloadLength() + ", format=" + this.format.toString());
			dropCount++;
			
			//checking if not dropping too much  			
			droppedInRaw++;
			if(droppedInRaw==QUEUE_SIZE/2 || queue.size()==0)
				arrivalDeadLine=0;
			else
				return;
		}
			
		f=ByteMemory.allocate(packet.getPayloadLength());
		//put packet into buffer irrespective of its sequence number
		f.setHeader(null);
		f.setSequenceNumber(packet.getSeqNumber());
		//here time is in milliseconds
		f.setTimestamp(rtpClock.convertToAbsoluteTime(packet.getTimestamp()));
		f.setOffset(0);
		f.setLength(packet.getPayloadLength());
		packet.getPyalod(f.getData(), 0);

		//set format
		f.setFormat(this.format.getFormat());
    		
    	//make checks only if have packet
    	if(f!=null)
    	{    
    		droppedInRaw=0;
    		
    		//find correct position to insert a packet 
    		//use timestamp since its always positive
    		int currIndex=queue.size()-1;    		    		
    		while (currIndex>=0 && queue.get(currIndex).getTimestamp() > f.getTimestamp())
    			currIndex--;
    		
    		if(currIndex>=0 && queue.get(currIndex).getSequenceNumber() == f.getSequenceNumber())
    		{
    			//duplicate packet
    			return;
    		}
    				    			
    		queue.add(currIndex+1, f);
    		
    		//recalculate duration of each frame in queue and overall duration , since we could insert the
    		//frame in the middle of the queue    			
    		duration=0;    			
    		if(queue.size()>1)
    			duration=queue.get(queue.size()-1).getTimestamp() - queue.get(0).getTimestamp();
    		
    		for(int i=0;i<queue.size()-1;i++)
    		{
    			//duration measured by wall clock
    			long d = queue.get(i+1).getTimestamp() - queue.get(i).getTimestamp();
    			//in case of RFC2833 event timestamp remains same
    			if (d > 0)    				
    				queue.get(i).setDuration(d);    					
    			else
    				queue.get(i).setDuration(0);
    		}
    			
    		//if overall duration is negative we have some mess here,try to reset
    		if(duration<0 && queue.size()>1)
    		{
    			reset();
    			return;
    		}
    			    			
    		//overflow?
    		//only now remove packet if overflow , possibly the same packet we just received
    		//lets keep last packet to block UDP resending which may occure on 3G network
    		if (duration>currentJitterBufferSize) 
    		{    			
    			if(currentJitterBufferSize<maxJitterBufferSize)
    			{
    				dropCount+=queue.size();        			
        			while(queue.size()>0)
        				queue.remove(0).recycle();
        			
	    			currentJitterBufferSize+=jitterBufferSize;
	    			logger.info("Clearing and increasing jitter buffer size to " + currentJitterBufferSize);
    			}
    			else
    			{
    				while(queue.size()>0)
    					queue.remove(0).recycle();
    				
    				logger.info("Dropping packet");
    			}
    		}
    			    		       
    		//check if this buffer already full
    		if (!ready) {    			
    			ready = !useBuffer || (duration >= jitterBufferSize && queue.size() > 1);
    			if (ready) {    	
    				if (listener != null) {
    					listener.onFill();
    				}
    			}
    		}
    	}
    }

    /**
     * Polls packet from buffer's head.
     *
     * @param timestamp the media time measured by reader
     * @return the media frame.
     */
    public ByteFrame read(long timestamp) {
    	if (queue.size()==0) {
    		this.ready = false;
    		return null;
    	}
    	
    	//extract packet
    	ByteFrame frame = queue.remove(0);
    		
    	//buffer empty now? - change ready flag.
    	if (queue.size() == 0) {
    		this.ready = false;
    		//arrivalDeadLine = 0;
    		//set it as 1 ms since otherwise will be dropped by pipe
    		frame.setDuration(1);
    	}    		
    		
    	arrivalDeadLine = rtpClock.convertToRtpTime(frame.getTimestamp() + frame.getDuration());
    	
    	//convert duration to nanoseconds
    	frame.setDuration(frame.getDuration() * 1000000L);
    	frame.setTimestamp(frame.getTimestamp() * 1000000L);
        
    	return frame;    	
    }
    
    /**
     * Resets buffer.
     */
    public void reset() {
    	while(queue.size()>0)
    		queue.remove(0).recycle();    	
    }
    
    public void restart() {
    	this.channel.codecChanged();
    	reset();
    	this.ready=false;
    	arrivalDeadLine = 0;
    	dropCount=0;
    	droppedInRaw=0;
    	format=null;
    	isn=-1;
    	lastBursts[0]=lastBursts[1]=lastBursts[2]=0;
    	this.currentJitterBufferSize=jitterBufferSize*2;
    }
}
