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
package ua.mobius.media.server.impl.resource.mediaplayer.audio;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.apache.log4j.Logger;

import ua.mobius.media.ComponentType;
import ua.mobius.media.server.component.audio.AudioInput;
import ua.mobius.media.server.impl.AbstractAudioSource;
import ua.mobius.media.server.impl.resource.mediaplayer.Track;
import ua.mobius.media.server.impl.resource.mediaplayer.audio.gsm.GsmTrackImpl;
import ua.mobius.media.server.impl.resource.mediaplayer.audio.mpeg.AMRTrackImpl;
import ua.mobius.media.server.impl.resource.mediaplayer.audio.tts.TtsTrackImpl;
import ua.mobius.media.server.impl.resource.mediaplayer.audio.tts.VoicesCache;
import ua.mobius.media.server.impl.resource.mediaplayer.audio.vox.VoxTrackImpl;
import ua.mobius.media.server.impl.resource.mediaplayer.audio.wav.WavTrackImpl;
import ua.mobius.media.server.impl.resource.mediaplayer.audio.tone.ToneTrackImpl;
import ua.mobius.media.server.scheduler.Scheduler;
import ua.mobius.media.server.scheduler.Task;
import ua.mobius.media.server.spi.dsp.AudioProcessor;
import ua.mobius.media.server.spi.ResourceUnavailableException;
import ua.mobius.media.server.spi.format.AudioFormat;
import ua.mobius.media.server.spi.format.FormatFactory;
import ua.mobius.media.server.spi.player.Player;
import ua.mobius.media.server.spi.player.PlayerListener;
import ua.mobius.media.server.spi.listener.Listeners;
import ua.mobius.media.server.spi.listener.TooManyListenersException;
import ua.mobius.media.server.spi.memory.ShortFrame;
import ua.mobius.media.server.spi.memory.ByteFrame;
import ua.mobius.media.server.spi.resource.TTSEngine;

/**
 * @author
 * @author yulian oifa
 */
public class AudioPlayerImpl extends AbstractAudioSource implements Player, TTSEngine {

    //define natively supported formats
    private final static AudioFormat LINEAR = FormatFactory.createAudioFormat("linear", 8000, 16, 1);
    private long period = 20000000L;
    private int packetSize = (int)(period / 1000000) * LINEAR.getSampleRate()/1000 * LINEAR.getSampleSize() / 8;    

    private AudioFormat activeFormat;
    
    //digital signaling processor
    private AudioProcessor dsp;
    
    //audio track
    private Track track;

    private String voiceName = "kevin";
    private int volume;

    private Listeners<PlayerListener> listeners = new Listeners<PlayerListener>();

    private final static Logger logger = Logger.getLogger(AudioPlayerImpl.class);
    
    private AudioInput input;
    
    private Scheduler scheduler;
    
    //event sender task
    private EventSender eventSender;
    
    private AudioPlayerEvent playerStartedEvent;
    private AudioPlayerEvent playerStoppedEvent;
    
    private VoicesCache voiceCache;
    /**
     * Creates new instance of the Audio player.
     * 
     * @param name the name of the AudioPlayer to be created.
     * @param scheduler EDF job scheduler
     * @param vc  the TTS voice cache. 
     */
    public AudioPlayerImpl(String name, Scheduler scheduler) {
        super(name, scheduler,scheduler.INPUT_QUEUE);
        
        this.input=new AudioInput(ComponentType.PLAYER.getType(),packetSize);
        this.connect(this.input);
        
        this.scheduler=scheduler;
        
        eventSender = new EventSender(); 
        playerStartedEvent=new AudioPlayerEvent(this, AudioPlayerEvent.START);
        playerStoppedEvent=new AudioPlayerEvent(this, AudioPlayerEvent.STOP);
    }

    public AudioInput getAudioInput()
    {
    	return this.input;
    }
    
    /**
     * Assigns the digital signaling processor of this component.
     * The DSP allows to get more output formats.
     *
     * @param dsp the dsp instance
     */
    public void setDsp(AudioProcessor dsp) {
        //assign processor
        this.dsp = dsp; 
        if(this.activeFormat!=null)
        	this.dsp.setSourceFormat(activeFormat);
    }
    
    /**
     * Gets the digital signaling processor associated with this media source
     *
     * @return DSP instance.
     */
    public AudioProcessor getDsp() {
        return this.dsp;
    }
    
    /**
     * (Non Java-doc.)
     * 
     * @see ua.mobius.media.server.spi.player.Player#setURL(java.lang.String)
     */
    public void setURL(String passedURI) throws ResourceUnavailableException, MalformedURLException {
    	//close previous track if was opened
    	if(this.track!=null)
    	{
    		track.close();
            track = null;
    	}
    	
        // let's disallow to assign file is player is not connected
        if (!this.isConnected()) {
            throw new IllegalStateException("Component should be connected");
        }
        URL targetURL;
     // now using extension we have to determne the suitable stream parser
    	int pos = passedURI.lastIndexOf('.');

    	// extension is not specified?
    	if (pos == -1) {
    		throw new MalformedURLException("Unknow file type: " + passedURI);
    	}

    	String ext = passedURI.substring(pos + 1).toLowerCase();
    	targetURL = new URL(passedURI);
    	
    	// creating required extension
    	try {
    		//check scheme, if its file, we should try to create dirs
    		if (ext.matches(Extension.WAV)) {       
    			track = new WavTrackImpl(targetURL);
            } else if (ext.matches(Extension.VOX)) {
                track = new VoxTrackImpl(targetURL);
    		} else if (ext.matches(Extension.GSM)) {
    			track = new GsmTrackImpl(targetURL);
    		} else if (ext.matches(Extension.TONE)) {
    			track = new ToneTrackImpl(targetURL);
    		} else if (ext.matches(Extension.TXT)) {
    			track = new TtsTrackImpl(targetURL, voiceName, getVoicesCache());
    		} else if (ext.matches(Extension.MOV) || ext.matches(Extension.MP4) || ext.matches(Extension.THREE_GP)) {
    			track = new AMRTrackImpl(targetURL);
    		} else {
    			if(getEndpoint()==null)
    				logger.info("unknown extension:" + passedURI);
    			else
    				logger.info("(" + getEndpoint().getLocalName() + ") unknown extension:" + passedURI);
    			
    			throw new ResourceUnavailableException("Unknown extension: " + passedURI);
    		}
    	} catch (Exception e) {        
    		if(getEndpoint()==null)
    			logger.error("error occured",e);
    		else
    			logger.error("(" + getEndpoint().getLocalName() + ") error occured",e);
    		
    		throw new ResourceUnavailableException(e);
    	}
    	
    	this.activeFormat=track.getFormat();
    	if(this.dsp!=null)
    		this.dsp.setSourceFormat(activeFormat);
    	
        //update duration
        this.duration = track.getDuration();
    }

    @Override
    public void activate() {
        if (track == null) {        	
            throw new IllegalStateException("The media source is not specified");
        }
        start();
        
        fireEvent(playerStartedEvent);        
    }

    @Override
    public void deactivate() {
    	stop();
        if (track != null) {
            track.close();
            track = null;
        }
    }

    @Override
    protected void stopped() {
    	fireEvent(playerStoppedEvent);     	
    }
    /**
     * Sends notification that signal is completed.
     * 
     */
    @Override
    protected void completed() {
        super.completed();
        fireEvent(playerStoppedEvent);         
    }
    
    @Override
    public ShortFrame evolve(long timestamp) {
        try {
        	ByteFrame frame = track.process(timestamp);
            if(frame==null)
            	return null;
            
            frame.setTimestamp(timestamp);

            if (frame.isEOM()) {
            	if(getEndpoint()==null)
            		logger.info("End of file reached");
            	else
            		logger.info("(" + getEndpoint().getLocalName() + ") End of file reached");            		
            }

            ShortFrame outputFrame=null;
            //do the transcoding job
            if (dsp != null) {
        		try
        		{
        			outputFrame = dsp.decode(frame);
        		}
        		catch(Exception e)
        		{
        			//transcoding error , print error and try to move to next frame
        			if(getEndpoint()==null)
        				logger.error(e);
        			else
        				logger.error("(" + getEndpoint().getLocalName() + ")",e);        			        		
        		}                	
        	}  
            
            if(outputFrame==null || outputFrame.isEOM())
            	track.close();                
            
            return outputFrame;
        } catch (IOException e) {
        	if(getEndpoint()==null)
        		logger.error(e);
        	else        		
        		logger.error("(" + getEndpoint().getLocalName() + ")",e);
        	
            track.close();            
        }
        return null;
    }

    public void setVoiceName(String voiceName) {
        this.voiceName = voiceName;
    }

    public String getVoiceName() {
        return voiceName;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public int getVolume() {
        return volume;
    }

    public void setText(String text) {
        track = new TtsTrackImpl(text, voiceName, null);
    }    

    public void addListener(PlayerListener listener) throws TooManyListenersException {
        listeners.add(listener);
    }

    public void removeListener(PlayerListener listener) {
    	listeners.remove(listener);
    }

    public void clearAllListeners() {
    	listeners.clear();
    }
    
    public void setMaxDuration(long duration) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    private VoicesCache getVoicesCache() {
    	if (voiceCache != null) return voiceCache;    	
    	return voiceCache = new VoicesCache();
    }
    
    private void fireEvent(AudioPlayerEvent event) {
        eventSender.event = event;
        scheduler.submit(eventSender,scheduler.MIXER_MIX_QUEUE);
    }
    
    private class EventSender extends Task {

        protected AudioPlayerEvent event;
        
        public EventSender() {
            super();
        }        

        @Override
        public long perform() {
        	listeners.dispatch(event);        	
            return 0;
        }
    
        public int getQueueNumber() {
            return scheduler.MIXER_MIX_QUEUE;
        }
    }
}
