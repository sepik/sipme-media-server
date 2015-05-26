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
package ua.mobius.media.server.component.oob;

import ua.mobius.media.server.concurrent.ConcurrentMap;
import ua.mobius.media.server.scheduler.Scheduler;
import ua.mobius.media.server.scheduler.Task;
import ua.mobius.media.server.spi.memory.ByteFrame;

import java.util.Iterator;

/**
 * Implements compound oob mixer , one of core components of mms 3.0
 * 
 * @author Yulian Oifa
 */
public class OOBMixer {
    //scheduler for mixer job scheduling
    private Scheduler scheduler;
    
    //The pool of components
    private ConcurrentMap<OOBComponent> components = new ConcurrentMap<OOBComponent>();
    
    Iterator<OOBComponent> activeComponents;
    
    private MixTask mixer;
    private volatile boolean started = false;

    public long mixCount = 0;
    
    //gain value
    private double gain = 1.0;
    
    private int emptyCount=0;
    
    public OOBMixer(Scheduler scheduler) {
        this.scheduler = scheduler;
        
        mixer = new MixTask();        
    }

    public void addComponent(OOBComponent component)
    {
    	components.put(component.getComponentId(),component);    	
    }
    
    /**
     * Releases unused input stream
     *
     * @param component the input stream previously created
     */
    public void release(OOBComponent component) {
    	components.remove(component.getComponentId());        
    }

    public void start() {
    	mixCount = 0;
    	started = true;
    	emptyCount=0;
    	scheduler.submit(mixer, Scheduler.MIXER_MIX_QUEUE);
    }    
    
    public void stop() {
    	started = false;
    	emptyCount=0;
        mixer.cancel();        
    }

    private class MixTask extends Task {
    	int sourceComponent=0;
    	private ByteFrame current;
        
        public MixTask() {
            super();
        }
        
        public int getQueueNumber()
        {
        	return  Scheduler.MIXER_MIX_QUEUE;
        }
        
        public long perform() {
        	//summarize all
        	activeComponents=components.valuesIterator();
        	while(activeComponents.hasNext())
            {
            	OOBComponent component=activeComponents.next();
            	component.perform();
            	current=component.getData();
            	if(current!=null)
            	{
            		sourceComponent=component.getComponentId();
            		break;
            	}
            }
            
            if(current==null)
            {
            	scheduler.submit(this, Scheduler.MIXER_MIX_QUEUE);
                mixCount++; 
                emptyCount++;
                return 0;            
            }
            
            current.setDuration(current.getDuration()*(1+emptyCount));
            //get data for each component
            activeComponents=components.valuesIterator();
        	while(activeComponents.hasNext())
            {        		
            	OOBComponent component=activeComponents.next();
            	if(component.getComponentId()!=sourceComponent)
            		component.offer(current.clone());            	
            }
            
            current.recycle();
            scheduler.submit(this, Scheduler.MIXER_MIX_QUEUE);
            mixCount++;   
            emptyCount=0;
            return 0;            
        }
    }
}
