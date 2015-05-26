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

import java.util.Iterator;

import ua.mobius.media.server.scheduler.Scheduler;
import ua.mobius.media.server.scheduler.Task;
import ua.mobius.media.server.concurrent.ConcurrentMap;
import ua.mobius.media.server.spi.format.AudioFormat;
import ua.mobius.media.server.spi.format.Format;
import ua.mobius.media.server.spi.format.FormatFactory;
import ua.mobius.media.server.spi.memory.ByteFrame;
import ua.mobius.media.server.spi.memory.ByteMemory;

/**
 * Implements compound oob splitter , one of core components of mms 3.0
 * 
 * @author Yulian Oifa
 */
public class OOBSplitter {
    //scheduler for mixer job scheduling
    private Scheduler scheduler;
    
    //The pools of components
    private ConcurrentMap<OOBComponent> insideComponents = new ConcurrentMap<OOBComponent>();
    private ConcurrentMap<OOBComponent> outsideComponents = new ConcurrentMap<OOBComponent>();
        
    private Iterator<OOBComponent> insideRIterator=insideComponents.valuesIterator();
    private Iterator<OOBComponent> insideSIterator=insideComponents.valuesIterator();
    
    private Iterator<OOBComponent> outsideRIterator=outsideComponents.valuesIterator();
    private Iterator<OOBComponent> outsideSIterator=outsideComponents.valuesIterator();
    
    private InsideMixTask insideMixer;
    private OutsideMixTask outsideMixer;
    private volatile boolean started = false;

    protected long mixCount = 0;
    
    private int emptyInsideCount=0;
    private int emptyOutsideCount=0;
    
    public OOBSplitter(Scheduler scheduler) {
        this.scheduler = scheduler;
        
        insideMixer = new InsideMixTask();        
        outsideMixer = new OutsideMixTask();
    }

    public void addInsideComponent(OOBComponent component)
    {
    	insideComponents.put(component.getComponentId(),component);    	
    }
    
    public void addOutsideComponent(OOBComponent component)
    {
    	outsideComponents.put(component.getComponentId(),component);    	
    }
    
    /**
     * Releases inside component
     *
     * @param component
     */
    public void releaseInsideComponent(OOBComponent component) {
    	insideComponents.remove(component.getComponentId());    	
    }
    
    /**
     * Releases outside component
     *
     * @param component
     */
    public void releaseOutsideComponent(OOBComponent component) {
    	outsideComponents.remove(component.getComponentId());    	
    }
    
    public void start() {
    	mixCount = 0;
    	started = true;
    	emptyInsideCount=0;
    	emptyOutsideCount=0;
    	scheduler.submit(insideMixer,scheduler.MIXER_MIX_QUEUE);
    	scheduler.submit(outsideMixer,scheduler.MIXER_MIX_QUEUE);
    }    
    
    public void stop() {
    	started = false;
    	emptyInsideCount=0;
    	emptyOutsideCount=0;
    	insideMixer.cancel();
    	outsideMixer.cancel();
    }

    private class InsideMixTask extends Task {
    	private ByteFrame current;
        
        public InsideMixTask() {
            super();
        }
        
        public int getQueueNumber()
        {
        	return scheduler.MIXER_MIX_QUEUE;
        }
        
        public long perform() {
        	//summarize all
        	current=null;
        	insideRIterator=insideComponents.valuesIterator();
            while(insideRIterator.hasNext())
            {
            	OOBComponent component=insideRIterator.next();
            	component.perform();
            	current=component.getData();
            	if(current!=null)            	
            		break;            	
            }

            if(current==null)
            {
            	scheduler.submit(this,scheduler.MIXER_MIX_QUEUE);
                mixCount++;   
                emptyInsideCount++;
                return 0;            
            }
            
            current.setDuration(current.getDuration()*(1+emptyInsideCount));
            //get data for each component
            outsideSIterator=outsideComponents.valuesIterator();            
            while(outsideSIterator.hasNext())
            {
            	OOBComponent component=outsideSIterator.next();
            	if(!outsideSIterator.hasNext())
            		component.offer(current);
            	else
            		component.offer(current.clone());
            }
            
            scheduler.submit(this,scheduler.MIXER_MIX_QUEUE);
            mixCount++;     
            emptyInsideCount=0;
        	return 0;            
        }
    }
    
    private class OutsideMixTask extends Task {
    	private ByteFrame current;
        
        public OutsideMixTask() {
            super();
        }
        
        public int getQueueNumber()
        {
        	return scheduler.MIXER_MIX_QUEUE;
        }
        
        public long perform() {
        	//summarize all
            current=null;
            outsideRIterator=outsideComponents.valuesIterator();
            while(outsideRIterator.hasNext())
            {
            	OOBComponent component=outsideRIterator.next();
            	component.perform();
            	current=component.getData();
            	if(current!=null)
            		break;            	
            }

            if(current==null)
            {
            	scheduler.submit(this,scheduler.MIXER_MIX_QUEUE);
                mixCount++; 
                emptyOutsideCount++;
                return 0;            
            }
            
            current.setDuration(current.getDuration()*(1+emptyOutsideCount));
            //get data for each component
            insideSIterator=insideComponents.valuesIterator();
            while(insideSIterator.hasNext())
            {
            	OOBComponent component=insideSIterator.next();
            	if(!insideSIterator.hasNext())
            		component.offer(current);
            	else
            		component.offer(current.clone());            	
            }
            
            scheduler.submit(this,scheduler.MIXER_MIX_QUEUE);
            mixCount++;  
            emptyOutsideCount=0;
        	return 0;
        }
    }
}
