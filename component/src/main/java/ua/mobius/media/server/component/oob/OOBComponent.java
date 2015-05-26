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
import ua.mobius.media.server.spi.format.AudioFormat;
import ua.mobius.media.server.spi.format.FormatFactory;
import ua.mobius.media.server.spi.memory.ByteFrame;

import java.util.Iterator;

/**
 * Implements compound components used by mixer and splitter.
 * 
 * @author Yulian Oifa
 */
public class OOBComponent {
	//the format of the output stream.
	private final static AudioFormat dtmf = FormatFactory.createAudioFormat("telephone-event", 8000);

    private ConcurrentMap<OOBInput> inputs = new ConcurrentMap<OOBInput>();
	private ConcurrentMap<OOBOutput> outputs = new ConcurrentMap<OOBOutput>();
    
	Iterator<OOBInput> activeInputs;
	Iterator<OOBOutput> activeOutputs;
	
	protected Boolean shouldRead=false;
	protected Boolean shouldWrite=false;
	
	private ByteFrame frame;
	
	private int componentId;
    /**
     * Creates new instance with default name.
     */
    public OOBComponent(int componentId) {
    	this.componentId=componentId;    	
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
    
    public void addInput(OOBInput input) {
    	inputs.put(input.getInputId(),input);
    }

    public void addOutput(OOBOutput output) {
    	outputs.put(output.getOutputId(),output);
    }
    
    public void remove(OOBInput input)
    {
    	inputs.remove(input.getInputId());
    }
    
    public void remove(OOBOutput output)
    {
    	outputs.remove(output.getOutputId());
    }
    
    public void perform()
    {
    	frame=null;    	
    	activeInputs=inputs.valuesIterator();
    	while(activeInputs.hasNext())
        {
        	OOBInput input=activeInputs.next();
        	frame=input.poll();
        	if(frame!=null)
        		break;        	        	   	  
        }
    }
    
    public ByteFrame getData()
    {
    	if(!this.shouldRead)
    	{
    		if(frame!=null)
    			frame.recycle();
    		
    		return null;
    	}
    	
    	return frame;
    }
    
    public void offer(ByteFrame frame)
    {
    	if(!this.shouldWrite)
    	{
    		frame.recycle();
    		return;
    	}
    	
    	activeOutputs=outputs.valuesIterator();
    	while(activeOutputs.hasNext())
        {
    		OOBOutput output=activeOutputs.next();
        	if(!activeOutputs.hasNext())
        		output.offer(frame);
        	else
        		output.offer(frame.clone());        		        	
        	
        	output.wakeup();
        }
    }
}
