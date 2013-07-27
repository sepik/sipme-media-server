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
package ua.mobius.media.server.component;

import ua.mobius.media.server.spi.dsp.AudioCodec;
import ua.mobius.media.server.spi.dsp.AudioProcessor;
import ua.mobius.media.server.spi.format.Format;
import ua.mobius.media.server.spi.format.Formats;
import ua.mobius.media.server.spi.memory.ShortFrame;
import ua.mobius.media.server.spi.memory.ByteFrame;

/**
 * Digital signaling processor.
 *
 * DSP transforms media from its original format to one of the specified
 * output format. Output formats are specified as array where order of the
 * formats defines format's priority. If frame has format matching to output
 * format the frame won't be changed.
 *
 * @author yulian oifa
 */
public class Dsp implements AudioProcessor {
    private AudioCodec sourceCodec,destinationCodec;
    private AudioCodec[] codecs;

    //The current format of the frame stream
    private Format sourceFormat=null,destinationFormat=null;    
    
    /**
     * Creates new instance of processor.
     *
     * @param codecs
     */
    protected Dsp(AudioCodec[] codecs) {
        this.codecs = codecs;
    }

    /**
     * (Non Java-doc.)
     * 
     * @see ua.mobius.media.server.spi.dsp.Processor#getCodecs() 
     */
    public AudioCodec[] getCodecs() {
        return codecs;
    }    
	    
    /**
     * (Non Java-doc.)
     * 
     * @see ua.mobius.media.server.spi.dsp.Processor#process(ua.mobius.media.server.spi.memory.Frame,ua.mobius.media.server.spi.format.Format,ua.mobius.media.server.spi.format.Format)  
     */
    public ShortFrame decode(ByteFrame frame)
    {
    	if (sourceFormat==null)
			return null;
    	
    	try 
    	{
			return sourceCodec.decode(frame);
		} 
    	finally 
    	{
			frame.recycle();
		}		
    }

    public ByteFrame encode(ShortFrame frame)
    {
    	if (destinationFormat==null)
			return null;
    	
    	try 
    	{
			return destinationCodec.encode(frame);
		} 
    	finally 
    	{
			frame.recycle();
		}				
    }    
    
    public void setSourceFormat(Format source)
    {
    	 for (int i = 0; i < codecs.length; i++) {
             //select codec wich can receive current frame
             if (codecs[i].getSupportedFormat().matches(source)) {
            	 this.sourceFormat=source;
            	 sourceCodec = codecs[i];
            	 sourceCodec.reset();
                 break;
             }
    	 }    	
    }
    
    public void setDestinationFormat(Format destination)
    {
    	for (int i = 0; i < codecs.length; i++) {
            //select codec wich can receive current frame
            if (codecs[i].getSupportedFormat().matches(destination)) {
            	this.destinationFormat=destination;
            	destinationCodec = codecs[i];
            	destinationCodec.reset();
                break;
            }
   	 	}      	
    }
}