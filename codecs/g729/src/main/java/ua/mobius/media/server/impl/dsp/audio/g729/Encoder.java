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
package ua.mobius.media.server.impl.dsp.audio.g729;

import ua.mobius.media.server.spi.format.Format;
import ua.mobius.media.server.spi.format.FormatFactory;
import ua.mobius.media.server.spi.memory.ByteFrame;
import ua.mobius.media.server.spi.memory.ShortFrame;
import ua.mobius.media.server.spi.memory.ByteMemory;

public class Encoder {

    private final static Format g729 = FormatFactory.createAudioFormat("g729", 8000);
    private final static Format linear = FormatFactory.createAudioFormat("linear", 8000, 16, 1);

    int frame = 0;
    CodLD8K encoder = new CodLD8K();
    PreProc preProc = new PreProc();
    CircularBuffer circularBuffer = new CircularBuffer(16000);
    int prm[] = new int[LD8KConstants.PRM_SIZE];
    short serial[] = new short[LD8KConstants.SERIAL_SIZE];

    short[] one = new short[LD8KConstants.L_FRAME];
	short[] two = new short[LD8KConstants.L_FRAME];
    
	byte[] oneOutput = new byte[2*LD8KConstants.L_FRAME];
	byte[] twoOutput = new byte[2*LD8KConstants.L_FRAME];
	
    public Encoder() { 
    	reset();
    }

    public void reset()
    {
    	
    	preProc.init_pre_process();
        encoder.init_coder_ld8k();
        circularBuffer.reset();
    }
    
    public ByteFrame process(ShortFrame frame) {
    	ByteFrame res = null;
    	
        short[] data = frame.getData();
        circularBuffer.addData(data);

        int frameSize = LD8KConstants.L_FRAME;
        short[] speechWindow = circularBuffer.getData(2 * frameSize);
        byte[] resultingBytes = null;

        if (speechWindow == null) {
            resultingBytes = new byte[0]; // No data available right now, send
        // empty buffer
        } else {
        	System.arraycopy(speechWindow, 0, one, 0, frameSize);
        	System.arraycopy(speechWindow, frameSize, two, 0, frameSize);
            
        	oneOutput = process(one);
        	twoOutput = process(two);

            if (one.length != two.length) {
                throw new RuntimeException(
                        "The two frames are not equal in size!");
            }
            
            res = ByteMemory.allocate(oneOutput.length + twoOutput.length);
            res.setLength(oneOutput.length + twoOutput.length);
            byte[] resultBytes = res.getData();
            System.arraycopy(oneOutput, 0, resultBytes, 0, oneOutput.length);
            System.arraycopy(twoOutput, 0, resultBytes, oneOutput.length, twoOutput.length);            
        }
        
        res.setOffset(0);
        res.setTimestamp(frame.getTimestamp());
        res.setDuration(frame.getDuration());
        res.setSequenceNumber(frame.getSequenceNumber());
        res.setEOM(frame.isEOM());
        res.setFormat(g729);
        return res;
    }

    /**
     * Perform compression.
     * 
     * @param input
     *            media
     * @return compressed media.
     */
    public byte[] process(short[] media) {
        frame++;

        float[] new_speech = new float[media.length];
        for (int i = 0; i < LD8KConstants.L_FRAME; i++) {
            new_speech[i] = (float) media[i];
        }
        preProc.pre_process(new_speech, LD8KConstants.L_FRAME);

        encoder.loadSpeech(new_speech);
        encoder.coder_ld8k(prm, 0);

        //byte[] a = new byte[10];
        Bits.prm2bits_ld8k(prm, serial);
        // return a;
        return Bits.toRealBits(serial);
    }
}
