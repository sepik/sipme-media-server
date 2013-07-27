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
package ua.mobius.media.server.impl.dsp.audio.l16;

import ua.mobius.media.server.spi.dsp.AudioCodec;
import ua.mobius.media.server.spi.format.Format;
import ua.mobius.media.server.spi.format.FormatFactory;
import ua.mobius.media.server.spi.memory.ShortFrame;
import ua.mobius.media.server.spi.memory.ByteFrame;

/**
 * 
 * @author oifa yulian
 * 
 */
public class Codec implements AudioCodec {

    private final static Format l16 = FormatFactory.createAudioFormat("l16", 8000, 16, 1);
    
    private Decoder decoder=new Decoder();
    private Encoder encoder=new Encoder();
    
    public Format getSupportedFormat() {
        return l16;
    }

    public ShortFrame decode(ByteFrame frame)
    {
    	return decoder.process(frame);
    }

	public ByteFrame encode(ShortFrame frame)
	{
		return encoder.process(frame);
	}
	
	public void reset()
	{
		
	}
}
