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
package ua.mobius.media.server.impl;

import java.io.IOException;

import ua.mobius.media.AudioSink;
import ua.mobius.media.OOBSink;
import ua.mobius.media.server.scheduler.Scheduler;
import ua.mobius.media.server.scheduler.Task;
import ua.mobius.media.server.spi.memory.ShortFrame;
import ua.mobius.media.server.spi.memory.ByteFrame;

import org.apache.log4j.Logger;
/**
 * The base implementation of the media sink.
 * 
 * <code>AbstractSource</code> and <code>AbstractSink</code> are implement 
 * general wiring construct. 
 * All media components have to extend one of these classes.
 * 
 * @author Oifa Yulian
 */
public abstract class AbstractCompoundSink extends AbstractSink implements AudioSink,OOBSink {

    /**
     * Creates new instance of sink with specified name.
     * 
     * @param name the name of the sink to be created.
     */
    public AbstractCompoundSink(String name) {
        super(name);               
    }        

    /**
     * This methos is called when new portion of media arrives.
     * 
     * @param buffer the new portion of media data.
     */
    public abstract void onMediaTransfer(ByteFrame frame) throws IOException;

    /**
     * This methos is called when new portion of media arrives.
     * 
     * @param buffer the new portion of media data.
     */
    public abstract void onMediaTransfer(ShortFrame frame) throws IOException;
    
    public void perform(ByteFrame frame) {
    	if(!started)
    		return;
    	
    	if(frame==null)
    		return;
    	
    	rxPackets++;
    	rxBytes += frame.getLength();

    	//frame is not null, let's handle it
    	try {
    		onMediaTransfer(frame);
    	} catch (IOException e) {  
    		logger.error(e);
    		started = false;
        	failed(e);
    	}
    }    
    
    public void perform(ShortFrame frame) {
    	if(!started)
    		return;
    	
    	if(frame==null)
    		return;
    	
    	rxPackets++;
    	rxBytes += frame.getLength();

    	//frame is not null, let's handle it
    	try {
    		onMediaTransfer(frame);
    	} catch (IOException e) {  
    		logger.error(e);
    		started = false;
        	failed(e);
    	}
    }    
}
