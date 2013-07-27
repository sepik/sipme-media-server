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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;

import ua.mobius.media.MediaSource;
import ua.mobius.media.server.impl.AbstractAudioSource;
import ua.mobius.media.AudioSink;
import ua.mobius.media.server.scheduler.Scheduler;
import ua.mobius.media.server.scheduler.Task;
import ua.mobius.media.server.concurrent.ConcurrentCyclicFIFO;
import ua.mobius.media.server.spi.memory.ShortFrame;

import org.apache.log4j.Logger;
/**
 * Implements output for compound components.
 * 
 * @author Yulian Oifa
 */
public class AudioOutput extends AbstractAudioSource {
	private int outputId;
    private ConcurrentCyclicFIFO<ShortFrame> buffer = new ConcurrentCyclicFIFO();
    
    /**
     * Creates new instance with default name.
     */
    public AudioOutput(Scheduler scheduler,int outputId) {
        super("compound.output", scheduler,scheduler.OUTPUT_QUEUE);
        this.outputId=outputId;
    }

    public int getOutputId()
    {
    	return outputId;
    }
    
    public void join(AudioSink sink)
    {
    	connect(sink);
    }
    
    public void unjoin()
    {
    	disconnect();
    }
    
    @Override
    public ShortFrame evolve(long timestamp) {
    	return buffer.poll();
    }        

    @Override
    public void stop() {
    	while(buffer.size()>0)
    		buffer.poll().recycle();
    	
    	super.stop();            
    }
    
    public void resetBuffer()
    {
    	while(buffer.size()>0)
    		buffer.poll().recycle();
    }
    
    public void offer(ShortFrame frame)
    {
    	if(buffer.size()>1)
        	buffer.poll().recycle();
    	
    	buffer.offer(frame);
    }
}
