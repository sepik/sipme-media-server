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

package ua.mobius.media.server.test;

import ua.mobius.media.Component;
import ua.mobius.media.ComponentType;
import ua.mobius.media.MediaSink;
import ua.mobius.media.MediaSource;
import ua.mobius.media.core.endpoints.BaseMixerEndpointImpl;
import ua.mobius.media.server.spi.ConnectionMode;
import ua.mobius.media.server.component.audio.AudioComponent;
import ua.mobius.media.server.component.audio.Sine;
import ua.mobius.media.server.component.audio.SoundCard;
import ua.mobius.media.server.spi.Endpoint;
import ua.mobius.media.server.spi.MediaType;
import ua.mobius.media.server.spi.ResourceUnavailableException;

/**
 *
 * @author yulian oifa
 */
public class SoundSystem extends BaseMixerEndpointImpl implements Endpoint {

    private int f;
    private Sine sine;
    private SoundCard soundcard;

    private AudioComponent audioComponent;
    
    public SoundSystem(String localName) {
        super(localName);
        audioComponent=new AudioComponent(-1);
    }

    public void setFreq(int f) {
        this.f = f;
    }
    
    @Override
    public void start() throws ResourceUnavailableException {
    	super.start();
        
        sine = new Sine(this.getScheduler());
        sine.setFrequency(f);
        sine.setAmplitude((short)(Short.MAX_VALUE / 3));
        soundcard = new SoundCard(this.getScheduler());
        
        audioComponent.addInput(sine.getAudioInput());
        audioComponent.addOutput(soundcard.getAudioOutput());
        audioComponent.updateMode(true,true);
        audioMixer.addComponent(audioComponent);
        modeUpdated(ConnectionMode.INACTIVE,ConnectionMode.SEND_RECV);
    }    

    public Component getResource(MediaType mediaType, ComponentType componentType) {
        switch(mediaType)
        {
        	case AUDIO:
        		switch(componentType)
        		{
        			case SINE:
        				return sine;
        			case SOUND_CARD:
        				return soundcard;        				
        		}
        }
        
        return null;
    }

    public void releaseResource(MediaType mediaType, ComponentType componentType) {
    	throw new UnsupportedOperationException("Not supported yet.");	
    }    
}
