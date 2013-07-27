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

/**
 * Implements compound audio mixer , one of core components of mms 3.0
 * 
 * @author Yulian Oifa
 */
public class AudioMixer {
    //scheduler for mixer job scheduling
    private Scheduler scheduler;
    
    //the format of the output stream.
    private AudioFormat format = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);
    
    //The pool of components
    private ConcurrentMap<AudioComponent> components = new ConcurrentMap();
    
    Iterator<AudioComponent> activeComponents;
    
    private long period = 20000000L;
    private int packetSize = (int)(period / 1000000) * format.getSampleRate()/1000 * format.getSampleSize() / 8;    

    private MixTask mixer;
    private volatile boolean started = false;

    public long mixCount = 0;
    
    //gain value
    private double gain = 1.0;
    private int emptyCount=0;
    
    public AudioMixer(Scheduler scheduler) {
        this.scheduler = scheduler;
        
        mixer = new MixTask();        
    }

    public void addComponent(AudioComponent component)
    {
    	components.put(component.getComponentId(),component);    	
    }
    
    protected int getPacketSize() {
        return this.packetSize;
    }
    
    /**
     * Releases unused input stream
     *
     * @param input the input stream previously created
     */
    public void release(AudioComponent component) {
    	components.remove(component.getComponentId());    	
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
    	emptyCount=0;
    	scheduler.submit(mixer,scheduler.MIXER_MIX_QUEUE);
    }    
    
    public void stop() {
    	started = false;
    	emptyCount=0;
        mixer.cancel();        
    }

    private class MixTask extends Task {
    	int sourcesCount=0;
    	private int i;
    	private int minValue=0;
    	private int maxValue=0;
    	private double currGain=0;
        private int[] total=new int[packetSize/2];
        private int[] current;
        
        public MixTask() {
            super();
        }
        
        public int getQueueNumber()
        {
        	return scheduler.MIXER_MIX_QUEUE;
        }
        
        public long perform() {
        	//summarize all
        	sourcesCount=0;
        	activeComponents=components.valuesIterator();
        	while(activeComponents.hasNext())
            {
            	AudioComponent component=activeComponents.next();
            	component.perform();
            	current=component.getData();
            	if(current!=null)
            	{
            		if(sourcesCount==0)
            			System.arraycopy(current, 0, total, 0, total.length);            			
            		else
            			for(i=0;i<total.length;i++)
            				total[i]+=current[i];

            		sourcesCount++;
            	}
            }
            
            if(sourcesCount==0)
            {
            	scheduler.submit(this,scheduler.MIXER_MIX_QUEUE);
                mixCount++;  
                emptyCount++;
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
				total[i]=(short)((double) total[i] * currGain);
            
            //get data for each component
            activeComponents=components.valuesIterator();
        	while(activeComponents.hasNext())
            {
            	AudioComponent component=activeComponents.next();
            	current=component.getData();
                if(current!=null && sourcesCount>1)
            	{
            		for(i=0;i<total.length;i++)
            			current[i]=total[i]-(short)((double)current[i]* currGain);
            		
                    component.offer(current,emptyCount);
            	}
            	else if(current==null)
            		component.offer(total,emptyCount);
            }
            
            scheduler.submit(this,scheduler.MIXER_MIX_QUEUE);
            mixCount++;    
            emptyCount=0;
            return 0;            
        }
    }
}
