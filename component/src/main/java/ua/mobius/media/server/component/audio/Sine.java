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

import ua.mobius.media.ComponentType;
import ua.mobius.media.server.impl.AbstractAudioSource;
import ua.mobius.media.server.scheduler.Scheduler;
import ua.mobius.media.server.spi.format.AudioFormat;
import ua.mobius.media.server.spi.format.FormatFactory;
import ua.mobius.media.server.spi.format.Formats;
import ua.mobius.media.server.spi.memory.ShortFrame;
import ua.mobius.media.server.spi.memory.ShortMemory;

/**
 *
 * @author yulian oifa
 */
public class Sine extends AbstractAudioSource {

    //the format of the output stream.
    private final static AudioFormat LINEAR_AUDIO = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);
    private final static Formats formats = new Formats();

    private volatile long period = 20000000L;
    private int packetSize = (int)(period / 1000000) * LINEAR_AUDIO.getSampleRate()/1000 * LINEAR_AUDIO.getSampleSize() / 8;

    private int f;
    private short A = Short.MAX_VALUE;
    private double dt;
    private double time;

    private AudioInput input;
    
    static {
        formats.add(LINEAR_AUDIO);
    }
    
    public Sine(Scheduler scheduler) {
        super("sine.generator", scheduler,scheduler.INPUT_QUEUE);
        //number of seconds covered by one sample
        dt = 1. / LINEAR_AUDIO.getSampleRate();
        
        this.input=new AudioInput(ComponentType.SINE.getType(),packetSize);
        this.connect(this.input); 
    }

    public AudioInput getAudioInput()
    {
    	return this.input;
    }
    
    public void setAmplitude(short A) {
        this.A = A;
    }

    public short getAmplitude() {
        return A;
    }

    public void setFrequency(int f) {
        this.f = f;
    }

    public int getFrequency() {
        return f;
    }

    private short getValue(double t) {
        return (short) (A * Math.sin(2 * Math.PI * f * t));
    }

    @Override
    public ShortFrame evolve(long timestamp) {
    	ShortFrame frame = ShortMemory.allocate(packetSize/2);
        int k = 0;

        int frameSize = packetSize / 2;

        short[] data = frame.getData();
        for (int i = 0; i < frameSize; i++) {
            data[i] = getValue(time + dt * i);            
        }

        frame.setOffset(0);
        frame.setLength(packetSize);
        frame.setDuration(period);
        frame.setFormat(LINEAR_AUDIO);
        
        time += ((double) period) / 1000000000.0;
        return frame;
    }
}