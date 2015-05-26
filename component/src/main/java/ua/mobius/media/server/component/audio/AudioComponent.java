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
import ua.mobius.media.server.concurrent.ConcurrentMap;
import ua.mobius.media.server.spi.format.AudioFormat;
import ua.mobius.media.server.spi.format.Format;
import ua.mobius.media.server.spi.format.FormatFactory;
import ua.mobius.media.server.spi.memory.ShortFrame;
import ua.mobius.media.server.spi.memory.ShortMemory;

/**
 * Implements compound components used by mixer and splitter.
 * 
 * @author Yulian Oifa
 */
public class AudioComponent {
	//the format of the output stream.
    private AudioFormat format = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);
    private long period = 20000000L;
    private int packetSize = (int)(period / 1000000) * format.getSampleRate()/1000 * format.getSampleSize() / 8;    

    private ConcurrentMap<AudioInput> inputs = new ConcurrentMap<AudioInput>();
	private ConcurrentMap<AudioOutput> outputs = new ConcurrentMap<AudioOutput>();
	
	private Iterator<AudioInput> activeInputs;
	private Iterator<AudioOutput> activeOutputs;
	
	protected Boolean shouldRead=false;
	protected Boolean shouldWrite=false;
	
	//samples storage
	private int[] data;
	
	private short[] dataArray;
	private ShortFrame inputFrame;
	private ShortFrame outputFrame;
	
	int inputCount,outputCount;
	boolean first;
	
	private int componentId;
    /**
     * Creates new instance with default name.
     */
    public AudioComponent(int componentId) {
    	this.componentId=componentId;
    	data=new int[packetSize/2];
    }

    public int getComponentId()
    {
    	return componentId;
    }
    
    public void updateMode(Boolean shouldRead,Boolean shouldWrite)
    {
    	this.shouldRead=shouldRead;
    	this.shouldWrite=shouldWrite;
    }
    
    public void addInput(AudioInput input) {
    	inputs.put(input.getInputId(),input);
    }

    public void addOutput(AudioOutput output) {
    	outputs.put(output.getOutputId(),output);
    }
    
    public void remove(AudioInput input)
    {
    	inputs.remove(input.getInputId());
    }
    
    public void remove(AudioOutput output)
    {
    	outputs.remove(output.getOutputId());
    }
    
    public void perform()
    {
    	first=true;  
    	activeInputs=inputs.valuesIterator();
    	while(activeInputs.hasNext())
        {
        	AudioInput input=activeInputs.next();
        	inputFrame=input.poll();
        	if(inputFrame!=null)
        	{
        		dataArray=inputFrame.getData();
        		if(first)
        		{
        			for(inputCount=0;inputCount<dataArray.length;inputCount++)
        				data[inputCount]=dataArray[inputCount];
        			
        			first=false;
        		}
        		else
        		{
        			for(inputCount=0;inputCount<dataArray.length;inputCount++)
        				data[inputCount]+=dataArray[inputCount];
        		}
        		
        		inputFrame.recycle();
        	}        	   	   
        }
    }
    
    public int[] getData()
    {
    	if(!this.shouldRead)
    		return null;
    	
    	if(first)
    		return null;
        
        return data;
    }
    
    public void offer(int[] data,int emptyCount)
    {
    	if(!this.shouldWrite)
    		return;
    	
    	outputFrame=ShortMemory.allocate(packetSize/2);
    	dataArray=outputFrame.getData();
    	
    	for(outputCount=0;outputCount<data.length;outputCount++)
    		dataArray[outputCount]=(short) (data[outputCount]);    		
    	
    	outputFrame.setOffset(0);
    	outputFrame.setLength(packetSize/2);
        outputFrame.setDuration(period*(1+emptyCount));
        outputFrame.setFormat(format);
        
        activeOutputs=outputs.valuesIterator();
        while(activeOutputs.hasNext())
        {
        	AudioOutput output=activeOutputs.next();
        	if(!activeOutputs.hasNext())
        		output.offer(outputFrame);
        	else
        		output.offer(outputFrame.clone());        		        	
        	
        	output.wakeup();
        }
    }

	public void offer(ShortFrame outputFrame,int emptyCount)
	{
		if(!this.shouldWrite)
			return;

		activeOutputs=outputs.valuesIterator();
		while(activeOutputs.hasNext())
		{
			AudioOutput output=activeOutputs.next();
			outputFrame.setDuration(period*(1+emptyCount));

			if(!activeOutputs.hasNext())
				output.offer(outputFrame);
			else
				output.offer(outputFrame.clone());

			output.wakeup();
		}
	}

	public boolean hasOneInput() {
		return inputs.size() <=1;
	}

	public ShortFrame poll()
	{
		ShortFrame inputFrame = null;
		Iterator<AudioInput> activeInputs=inputs.valuesIterator();
		if (activeInputs.hasNext())
		{
			AudioInput input=activeInputs.next();
			inputFrame = input.poll();
		}
		return inputFrame;
	}
}