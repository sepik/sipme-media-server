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

package ua.mobius.media.core;

import ua.mobius.media.Component;
import ua.mobius.media.ComponentType;
import ua.mobius.media.MediaSink;
import ua.mobius.media.MediaSource;
import ua.mobius.media.core.endpoints.BaseMixerEndpointImpl;
import ua.mobius.media.server.spi.ConnectionMode;
import ua.mobius.media.server.component.audio.AudioComponent;
import ua.mobius.media.server.component.oob.OOBComponent;
import ua.mobius.media.server.component.audio.Sine;
import ua.mobius.media.server.component.audio.SpectraAnalyzer;
import ua.mobius.media.server.impl.resource.dtmf.DetectorImpl;
import ua.mobius.media.server.impl.resource.dtmf.GeneratorImpl;
import ua.mobius.media.server.spi.Endpoint;
import ua.mobius.media.server.spi.MediaType;
import ua.mobius.media.server.spi.ResourceUnavailableException;

/**
 *
 * @author yulian oifa
 */
public class MyTestEndpoint extends BaseMixerEndpointImpl {

    private int f;
    private Sine sine;
    private SpectraAnalyzer analyzer;
    private Component dtmfDetector;
	private Component dtmfGenerator;
	
    private AudioComponent audioComponent;
    private OOBComponent oobComponent;
	
    public MyTestEndpoint(String localName) {
        super(localName);        
        audioComponent=new AudioComponent(1);
        oobComponent=new OOBComponent(-1);
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
        analyzer = new SpectraAnalyzer("analyzer",this.getScheduler());
        
        audioComponent.addInput(sine.getAudioInput());
        audioComponent.addOutput(analyzer.getAudioOutput());
        this.dtmfDetector=resourcesPool.newAudioComponent(ComponentType.DTMF_DETECTOR);
		this.dtmfDetector.setEndpoint(this);
				
		audioComponent.addOutput(((DetectorImpl)this.dtmfDetector).getAudioOutput());
		oobComponent.addOutput(((DetectorImpl)this.dtmfDetector).getOOBOutput());
		
		this.dtmfGenerator=resourcesPool.newAudioComponent(ComponentType.DTMF_GENERATOR);
		this.dtmfGenerator.setEndpoint(this);
		
		audioComponent.addInput(((GeneratorImpl)this.dtmfGenerator).getAudioInput());
		oobComponent.addInput(((GeneratorImpl)this.dtmfGenerator).getOOBInput());
		
		audioMixer.addComponent(audioComponent);
        oobMixer.addComponent(oobComponent);
        
        audioComponent.updateMode(true,true);
        oobComponent.updateMode(true,true);
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
        			case SPECTRA_ANALYZER:
        				return analyzer;
        			case DTMF_GENERATOR:
        				return dtmfGenerator;
        			case DTMF_DETECTOR:
        				return dtmfDetector;        				
        		}
        }
        
        return null;
    }

    public void releaseResource(MediaType mediaType, ComponentType componentType) {
    	throw new UnsupportedOperationException("Not supported yet.");	
    }    
}
