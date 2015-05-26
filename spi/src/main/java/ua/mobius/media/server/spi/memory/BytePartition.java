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
package ua.mobius.media.server.spi.memory;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author oifa yulian
 */
public class BytePartition {

    protected int size;
    private ConcurrentLinkedQueue<ByteFrame> heap = new ConcurrentLinkedQueue<ByteFrame>();

    protected BytePartition(int size) {
        this.size = size;
    }
    
    protected ByteFrame allocate() {
    	//if (true) return new ByteFrame(this, new byte[size]);
    	ByteFrame result=heap.poll();    	
    	
        if (result==null)
            return new ByteFrame(this, new byte[size]);
        
        result.inPartition.set(false);
        return result;
    }

    protected void recycle(ByteFrame frame) {
    	if(frame.inPartition.getAndSet(true))
    		//dont add duplicate,otherwise may be reused in different places
    		return;
    	
        frame.setHeader(null);
        frame.setDuration(Long.MAX_VALUE);
        frame.setEOM(false);        
        heap.offer(frame);
        //queue.offer(frame, frame.getDelay(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
    }

}
