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
package ua.mobius.media.server.impl.dsp.audio.g711.ulaw;

import ua.mobius.media.server.spi.format.Format;
import ua.mobius.media.server.spi.format.FormatFactory;
import ua.mobius.media.server.spi.memory.ShortFrame;
import ua.mobius.media.server.spi.memory.ByteFrame;
import ua.mobius.media.server.spi.memory.ByteMemory;

/**
 * Implements G.711 U-law compressor.
 * 
 * @author Yulian Oifa
 */
public class Encoder {
	
    private final static Format ulaw = FormatFactory.createAudioFormat("pcmu", 8000, 8, 1);
    
    private int i,count;
    
    public ByteFrame process(ShortFrame frame) {
    	count = frame.getLength();
        ByteFrame res = ByteMemory.allocate(count);
        
        short[] data=frame.getData();
        byte[] resData=res.getData();
        
        for (i = 0; i < count; i++)
        	resData[i]=EncoderData.muLawCompressTable[data[i] & 0xFFFF];        	
        
        res.setOffset(0);
        res.setLength(count);
        res.setFormat(ulaw);
        res.setTimestamp(frame.getTimestamp());
        res.setDuration(frame.getDuration());
        res.setEOM(frame.isEOM());
        res.setSequenceNumber(frame.getSequenceNumber());

        return res;               
    }
}