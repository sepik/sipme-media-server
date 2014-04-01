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
package ua.mobius.media.server.component.audio;

import java.io.IOException;

import ua.mobius.media.server.impl.AbstractAudioSink;
import ua.mobius.media.server.concurrent.ConcurrentCyclicFIFO;
import ua.mobius.media.server.spi.memory.ShortFrame;
import ua.mobius.media.server.spi.memory.ShortMemory;

/**
 * Implements input for compound components
 * 
 * @author Yulian Oifa
 */
public class AudioInput extends AbstractAudioSink {
    private int inputId;
    private int limit=3;
    private int currLimit=3;
    private int maxLimit=12;
    
    private ConcurrentCyclicFIFO<ShortFrame> buffer = new ConcurrentCyclicFIFO();
    private ShortFrame activeFrame=null;
    private short[] activeData;
    private short[] oldData;
    private int byteIndex=0;
    private int count=0;
    private int packetSize=0;
    
    /**
     * Creates new stream
     */
    public AudioInput(int inputId,int packetSize) {
        super("compound.input");
        this.inputId=inputId;
        this.packetSize=packetSize;
    }
    
    public int getInputId()
    {
    	return inputId;
    }
    
    public void activate()
    {
    	
    }
    
    public void deactivate()
    {
    	
    }
    
    public void setInitialAudioChannelBuffer(int value)
    {
    	limit=value;
    }
    
    public void setMaxAudioChannelBuffer(int value)
    {
    	maxLimit=value;
    }
    
    @Override
    public void onMediaTransfer(ShortFrame frame) throws IOException {
    	//generate frames with correct size here , aggregate frames if needed.
    	//allows to accept several sources with different ptime ( packet time ) 
    	oldData=frame.getData();
    	count=0;
    	while(count<oldData.length)
    	{
    		if(activeData==null)
    		{
    			activeFrame=ShortMemory.allocate(packetSize/2);
    			activeFrame.setOffset(0);
    			activeFrame.setLength(packetSize/2);
    			activeData=activeFrame.getData(); 
    			byteIndex=0;
    		}
    		
    		if(oldData.length-count<activeData.length-byteIndex)
    		{
    			System.arraycopy(oldData, count, activeData, byteIndex, oldData.length-count);
    			byteIndex+=oldData.length-count;
    			count=oldData.length;    			
    		}
    		else
    		{
    			System.arraycopy(oldData, count, activeData, byteIndex, activeData.length-byteIndex);
    			count+=activeData.length-byteIndex;
    			
    			if (buffer.size() >= currLimit) 
    			{
    				while(buffer.size()>0)
    					buffer.poll().recycle();
    				
    				if(currLimit<maxLimit)
    					currLimit+=limit;	    			    				    				    	
    			}
    			
            	buffer.offer(activeFrame);
            	
            	activeFrame=null;
    			activeData=null;    			    			
    		}
    	}
    	
    	frame.recycle();
    }

    /**
     * Indicates the state of the input buffer.
     *
     * @return true if input buffer has no frames.
     */
    public boolean isEmpty() {
        return buffer.size()==0;
    }

    /**
     * Retrieves frame from the input buffer.
     *
     * @return the media frame.
     */
    public ShortFrame poll() {
        return buffer.poll();
    }

    /**
     * Recycles input stream
     */
    public void recycle() {
    	while(buffer.size()>0)
    		buffer.poll().recycle();
    	
    	if(activeFrame!=null)
    		activeFrame.recycle();
    	
        activeFrame=null;
		activeData=null;
		byteIndex=0;		        
    }
    
    public void resetBuffer()
    {
    	currLimit=limit;
    	while(buffer.size()>0)
    		buffer.poll().recycle();
    }        
}
