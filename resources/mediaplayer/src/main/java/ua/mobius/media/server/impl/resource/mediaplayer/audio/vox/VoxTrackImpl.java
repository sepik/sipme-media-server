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
package ua.mobius.media.server.impl.resource.mediaplayer.audio.vox;

import org.apache.log4j.Logger;
import ua.mobius.media.server.impl.resource.mediaplayer.Track;
import ua.mobius.media.server.spi.format.AudioFormat;
import ua.mobius.media.server.spi.format.FormatFactory;
import ua.mobius.media.server.spi.memory.ByteFrame;
import ua.mobius.media.server.spi.memory.ByteMemory;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteOrder;

/**
 *
 * @author Oifa Yulian
 */
public class VoxTrackImpl implements Track {

	/** audio stream */
    private InputStream inStream;
    private AudioFormat format;
    private int period = 20;
    private int frameSize;
    private boolean eom;
    //private long timestamp;
    private long duration=0;
    private int totalRead=0;

    private boolean first = true;

    private static final Logger LOGGER = Logger.getLogger(VoxTrackImpl.class);
    private final static byte PCM_PADDING_BYTE = 0;

    public VoxTrackImpl(URL url) throws UnsupportedAudioFileException, IOException {
    	inStream=new VoxInputStream(url.openStream(), ByteOrder.nativeOrder());
        format=FormatFactory.createAudioFormat("l16", 8000, 16, 1);
        frameSize = (int) (period * format.getChannels() * format.getSampleSize() * format.getSampleRate() / 8000);
    }

    public void setPeriod(int period) {
        this.period = period;
        frameSize = (int) (period * format.getChannels() * format.getSampleSize() *
                format.getSampleRate() / 8000);
    }

    public int getPeriod() {
        return period;
    }

    public long getMediaTime() {
        return 0;
    }

    public long getDuration() {
        return duration;
    }

    public void setMediaTime(long timestamp) {
    }

    private void skip(long timestamp) {
        try {
            long offset = frameSize * (timestamp / period/ 1000000L);
            byte[] skip = new byte[(int)offset];
            int bytesRead=0;
            while(bytesRead<skip.length && inStream.available()>0)
            {
            	int len=inStream.read(skip,bytesRead,skip.length-bytesRead);
            	if(len==-1)
            		return;

            	totalRead+=len;
            	bytesRead+=len;
            }

        } catch (IOException e) {
        	LOGGER.error(e);
        }
    }

    /**
     * Reads packet from currently opened stream.
     *
     * @param packet
     *            the packet to read
     * @param offset
     *            the offset from which new data will be inserted
     * @return the number of actualy read bytes.
     * @throws IOException
     */
    private int readPacket(byte[] packet, int offset, int psize) throws IOException {
        int length = 0;
        try {
            while (length < psize && inStream.available()>0) {
                int len = inStream.read(packet, offset + length, psize - length);
                if (len == -1) {
                	return length;
                }
                length += len;
            }
            return length;
        } catch (Exception e) {
        	LOGGER.error(e);
        }
        return length;
    }

    private void padding(byte[] data, int count) {
        int offset = data.length - count;
        for (int i = 0; i < count; i++) {
            data[i + offset] = PCM_PADDING_BYTE;
        }
    }
    
    public ByteFrame process(long timestamp) throws IOException {
    	if (first) {
            if (timestamp > 0) {
                skip(timestamp);
            }
            first = false;
        }
        
    	ByteFrame frame = ByteMemory.allocate(frameSize);
        byte[] data =frame.getData();
        if (data == null) {
            data = new byte[frameSize];
        }
        
        int len = readPacket(data, 0, frameSize);
        totalRead+=len;
        if (len == 0) {
        	eom = true;
        }

        if (len < frameSize) {
        	padding(data, frameSize - len);
            eom = true;
        }

        frame.setOffset(0);
        frame.setLength(frameSize);
        frame.setEOM(eom);
        frame.setDuration(period* 1000000L);
        frame.setFormat(format);
        
        return frame;
    }

    public void close() {
        try {
        	inStream.close();
        } catch (Exception e) {
        }
    }

    public AudioFormat getFormat() {
        return format;
    }
}
