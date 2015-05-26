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

import java.util.Iterator;

import ua.mobius.media.server.scheduler.Scheduler;
import ua.mobius.media.server.scheduler.Task;
import ua.mobius.media.server.concurrent.ConcurrentMap;
import ua.mobius.media.server.spi.format.AudioFormat;
import ua.mobius.media.server.spi.format.Format;
import ua.mobius.media.server.spi.format.FormatFactory;
import ua.mobius.media.server.spi.memory.ShortFrame;

/**
 * Implements compound audio splitter , one of core components of mms 3.0
 * 
 * @author Yulian Oifa
 */
public class AudioSplitter {
    //scheduler for mixer job scheduling
    private Scheduler scheduler;
    
    //the format of the output stream.
    private AudioFormat format = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);
    
    //The pools of components
    private ConcurrentMap<AudioComponent> insideComponents = new ConcurrentMap<AudioComponent>();
    private ConcurrentMap<AudioComponent> outsideComponents = new ConcurrentMap<AudioComponent>();
    
    private Iterator<AudioComponent> insideRIterator;
    private Iterator<AudioComponent> insideSIterator;
    
    private Iterator<AudioComponent> outsideRIterator;
    private Iterator<AudioComponent> outsideSIterator;
    
    private long period = 20000000L;
    private int packetSize = (int)(period / 1000000) * format.getSampleRate()/1000 * format.getSampleSize() / 8;    

    private InsideMixTask insideMixer;
    private OutsideMixTask outsideMixer;
    private volatile boolean started = false;

    protected long mixCount = 0;
    
    private int emptyInsideCount=0;
    private int emptyOutsideCount=0;
    //gain value
    private double gain = 1.0;
    
    public AudioSplitter(Scheduler scheduler) {
        this.scheduler = scheduler;
        
        insideMixer = new InsideMixTask();        
        outsideMixer = new OutsideMixTask();
    }

    public void addInsideComponent(AudioComponent component)
    {
    	insideComponents.put(component.getComponentId(),component);    	
    }
    
    public void addOutsideComponent(AudioComponent component)
    {
    	outsideComponents.put(component.getComponentId(),component);    	
    }
    
    protected int getPacketSize() {
        return this.packetSize;
    }

    /**
     * Releases inside component
     *
     * @param component
     */
    public void releaseInsideComponent(AudioComponent component) {
    	insideComponents.remove(component.getComponentId());        
    }
    
    /**
     * Releases outside component
     *
     * @param component
     */
    public void releaseOutsideComponent(AudioComponent component) {
    	outsideComponents.remove(component.getComponentId());        
    }
    
    /**
     * Modify gain of the output stream.
     * 
     * @param gain the new value of the gain in dBm.
     */
    public void setGain(double gain) {
        this.gain = gain > 0 ? gain * 1.26 : gain == 0 ? 1 : 1/(gain * 1.26);
    }
    
    public void start() {
    	mixCount = 0;
    	started = true;
    	emptyInsideCount=0;
    	emptyOutsideCount=0;
    	scheduler.submit(insideMixer, Scheduler.MIXER_MIX_QUEUE);
    	scheduler.submit(outsideMixer, Scheduler.MIXER_MIX_QUEUE);
    }    
    
    public void stop() {
    	started = false;
    	emptyInsideCount=0;
    	emptyOutsideCount=0;
    	insideMixer.cancel();
    	outsideMixer.cancel();
    }

    private class InsideMixTask extends Task {
    	Boolean first=false;
    	private int i;
    	private int minValue=0;
    	private int maxValue=0;
    	private double currGain=0;
        private int[] total=new int[packetSize/2];
        private int[] current;
        
        public InsideMixTask() {
            super();
        }
        
        public int getQueueNumber()
        {
        	return Scheduler.MIXER_MIX_QUEUE;
        }
        
        public long perform() {
            Iterator<AudioComponent> insideIterator = insideComponents.valuesIterator();
            AudioComponent insideComponent = null;
            if (insideIterator.hasNext()) {
                insideComponent = insideIterator.next();
            }
            if (insideComponent != null && insideComponent.hasOneInput() && !insideIterator.hasNext()) {
                ShortFrame inputFrame = insideComponent.poll();
                if (inputFrame != null) {
                    Iterator<AudioComponent> outsideIterator = outsideComponents.valuesIterator();
                    while(outsideIterator.hasNext())
                    {
                        AudioComponent outsideComponent = outsideIterator.next();
                        outsideComponent.offer(inputFrame, emptyInsideCount);
                    }
                    emptyInsideCount=0;
                }
                else {
                    emptyInsideCount++;
                }
                scheduler.submit(this, Scheduler.MIXER_MIX_QUEUE);
                mixCount++;
                return 0;
            }

        	//summarize all
            first=true;
            insideRIterator=insideComponents.valuesIterator();
            while(insideRIterator.hasNext())
            {
            	AudioComponent component=insideRIterator.next();
            	component.perform();
            	current=component.getData();
            	if(current!=null)
            	{
            		if(first)
            		{
            			System.arraycopy(current, 0, total, 0, total.length);
            			first=false;
            		}
            		else
            		{
            			for(i=0;i<total.length;i++)
            				total[i]+=current[i];
            		}
            	}
            }

            if(first)
            {
            	scheduler.submit(this, Scheduler.MIXER_MIX_QUEUE);
                mixCount++;  
                emptyInsideCount++;
                return 0;            
            }
            
            minValue=0;
            maxValue=0;
            for(i=0;i<total.length;i++)
            	if(total[i]>maxValue)
                    maxValue=total[i];
            	else if(total[i]<minValue)
                    minValue=total[i];
            
            if(minValue>0)
            	minValue=0-minValue;
            
            if(minValue>maxValue)
                    maxValue=minValue;
            
            currGain=gain;
            if(maxValue>Short.MAX_VALUE)
                    currGain=(currGain*(double)Short.MAX_VALUE)/(double)maxValue;
            
            for(i=0;i<total.length;i++)
				total[i]=(short)Math.round((double) total[i] * currGain);
            
            //get data for each component
            outsideSIterator=outsideComponents.valuesIterator();
            while(outsideSIterator.hasNext())
            {
            	AudioComponent component=outsideSIterator.next();
            	component.offer(total,emptyInsideCount);
            		
            }
            
            scheduler.submit(this, Scheduler.MIXER_MIX_QUEUE);
            mixCount++;   
            emptyInsideCount=0;
            return 0;         	
        }
    }
    
    private class OutsideMixTask extends Task {
    	Boolean first=false;
    	private int i;
    	private int minValue=0;
    	private int maxValue=0;
    	private double currGain=0;
        private int[] total=new int[packetSize/2];
        private int[] current;
        
        public OutsideMixTask() {
            super();
        }
        
        public int getQueueNumber()
        {
        	return Scheduler.MIXER_MIX_QUEUE;
        }
        
        public long perform() {

            Iterator<AudioComponent> outsideIterator = outsideComponents.valuesIterator();
            AudioComponent outsideComponent = null;
            if (outsideIterator.hasNext()) {
                outsideComponent = outsideIterator.next();
            }
            if (outsideComponent != null && outsideComponent.hasOneInput() && !outsideIterator.hasNext()) {
                ShortFrame inputFrame = outsideComponent.poll();
                if (inputFrame != null) {
                    Iterator<AudioComponent> insideIterator = insideComponents.valuesIterator();
                    while(insideIterator.hasNext())
                    {
                        AudioComponent insideComponent = insideIterator.next();
                        insideComponent.offer(inputFrame, emptyOutsideCount);
                    }
                    emptyOutsideCount=0;
                }
                else {
                    emptyOutsideCount++;
                }
                scheduler.submit(this, Scheduler.MIXER_MIX_QUEUE);
                mixCount++;
                return 0;
            }

        	//summarize all
            first=true;
            outsideRIterator=outsideComponents.valuesIterator();
            while(outsideRIterator.hasNext())
            {
            	AudioComponent component=outsideRIterator.next();
            	component.perform();
            	current=component.getData();
            	if(current!=null)
            	{
            		if(first)
            		{
            			System.arraycopy(current, 0, total, 0, total.length);
            			first=false;
            		}
            		else
            		{
            			for(i=0;i<total.length;i++)
            				total[i]+=current[i];
            		}
            	}
            }

            if(first)
            {
            	scheduler.submit(this, Scheduler.MIXER_MIX_QUEUE);
                mixCount++;       
                emptyOutsideCount++;
                return 0;            
            }
            
            minValue=0;
            maxValue=0;
            for(i=0;i<total.length;i++)
            	if(total[i]>maxValue)
                    maxValue=total[i];
            	else if(total[i]<minValue)
                    minValue=total[i];
            
            minValue=0-minValue;            
            if(minValue>maxValue)
                    maxValue=minValue;
            
            currGain=gain;
            if(maxValue>Short.MAX_VALUE)
                    currGain=(currGain*Short.MAX_VALUE)/maxValue;
            
            for(i=0;i<total.length;i++)
				total[i]=(short)Math.round((double) total[i] * currGain);
            
            //get data for each component
            insideSIterator=insideComponents.valuesIterator();
            while(insideSIterator.hasNext())
            {
            	AudioComponent component=insideSIterator.next();
            	component.offer(total,emptyOutsideCount);
            		
            }
            
            scheduler.submit(this, Scheduler.MIXER_MIX_QUEUE);
            mixCount++;            
            emptyOutsideCount=0;
            return 0;        	
        }
    }
}
