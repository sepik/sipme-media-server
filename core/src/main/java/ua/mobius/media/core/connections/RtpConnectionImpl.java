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
package ua.mobius.media.core.connections;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.ParseException;

import org.apache.log4j.Logger;

import ua.mobius.media.server.spi.Connection;
import ua.mobius.media.server.spi.MediaType;
import ua.mobius.media.server.spi.format.Formats;
import ua.mobius.media.server.spi.format.AudioFormat;
import ua.mobius.media.server.spi.format.FormatFactory;
import ua.mobius.media.server.spi.FormatNotSupportedException;
import ua.mobius.media.server.spi.ConnectionFailureListener;
import ua.mobius.media.server.spi.dsp.DspFactory;
import ua.mobius.media.core.SdpTemplate;
import ua.mobius.media.server.component.audio.AudioComponent;
import ua.mobius.media.server.component.oob.OOBComponent;
import ua.mobius.media.server.impl.rtp.ChannelsManager;
import ua.mobius.media.server.impl.rtp.RTPDataChannel;
import ua.mobius.media.server.impl.rtp.RTPChannelListener;
import ua.mobius.media.server.impl.rtp.sdp.RTPFormat;
import ua.mobius.media.server.impl.rtp.sdp.RTPFormats;
import ua.mobius.media.server.impl.rtp.sdp.AVProfile;
import ua.mobius.media.server.impl.rtp.sdp.SdpComparator;
import ua.mobius.media.server.impl.rtp.sdp.SessionDescription;
import ua.mobius.media.server.spi.dsp.AudioCodec;
import ua.mobius.media.server.spi.ConnectionMode;
import ua.mobius.media.server.spi.ConnectionType;
import ua.mobius.media.server.spi.ModeNotSupportedException;
import ua.mobius.media.server.utils.Text;

/**
 * 
 * @author Oifa Yulian
 * @author amit bhayani
 */
public class RtpConnectionImpl extends BaseConnection implements RTPChannelListener {	
	private final static AudioFormat DTMF = FormatFactory.createAudioFormat("telephone-event", 8000);
	
	private RTPDataChannel rtpAudioChannel;
    
    private SessionDescription sdp = new SessionDescription();
    private SdpComparator sdpComparator = new SdpComparator();

    private boolean isAudioCapabale;
    
    protected RTPFormats audioFormats;
    
    //SDP template
    protected SdpTemplate template;
    protected String descriptor;    
    
    //negotiated sdp
    private String descriptor2;
    private boolean isLocal=false;
    private ConnectionFailureListener connectionFailureListener;
    
    private static final Logger logger = Logger.getLogger(RtpConnectionImpl.class);
    
    private ChannelsManager channelsManager;
    
    public RtpConnectionImpl(int id,ChannelsManager channelsManager,DspFactory dspFactory,int oobToneDuration,int oobEndTonePackets,Boolean growEndDuration)
    {
        super(id,channelsManager.getScheduler());

        this.channelsManager=channelsManager;
        //check endpoint capabilities
        this.isAudioCapabale =true;                

        //create audio and video channel
        rtpAudioChannel = channelsManager.getChannel(oobToneDuration,oobEndTonePackets,growEndDuration);
        rtpAudioChannel.setRtpChannelListener(this);
        
        try {
        	rtpAudioChannel.setDsp(dspFactory.newAudioProcessor());        	
        }
        catch(Exception e) {
        	//exception may happen only if invalid classes have been set in config
        }
        
        //create sdp template
        audioFormats = getRTPMap(AVProfile.audio);
        
        template = new SdpTemplate(audioFormats,null);
    }

    public AudioComponent getAudioComponent()
    {
    	return this.rtpAudioChannel.getAudioComponent();
    }
    
    public OOBComponent getOOBComponent()
    {
    	return this.rtpAudioChannel.getOOBComponent();
    }
    
    /**
     * Gets whether connection should be bound to local or remote interface , supported only for rtp connections.
     *
     * @return boolean value
     */
    @Override
    public boolean getIsLocal()
    {
    	return this.isLocal;
    }
    
    /**
     * Gets whether connection should be bound to local or remote interface , supported only for rtp connections.
     *
     * @return boolean value
     */
    @Override
    public void setIsLocal(boolean isLocal)
    {
    	this.isLocal=isLocal;
    }
    
    /**
     * Constructs RTP payloads for given channel.
     *
     * @param channel the media channel
     * @param profile AVProfile part for media type of given channel
     * @return collection of RTP formats.
     */
    private RTPFormats getRTPMap(RTPFormats profile) {
        RTPFormats list = new RTPFormats();
        Formats fmts=new Formats();
		if(rtpAudioChannel.getDsp()!=null)
		{
			AudioCodec[] currCodecs=rtpAudioChannel.getDsp().getCodecs();
			for(int i=0;i<currCodecs.length;i++)
				fmts.add(currCodecs[i].getSupportedFormat());
		}
		
		fmts.add(DTMF);
        
        if(fmts!=null)
        {
        	for (int i = 0; i < fmts.size(); i++) {
        		RTPFormat f = profile.find(fmts.get(i));
        		if (f != null) list.add(f.clone());
        	}
        }
        
        return list;
    }
    
    @Override
    public void setOtherParty(Connection other) throws IOException {
        throw new IOException("Applicable on locale connections only");        
    }

    @Override
    public void setMode(ConnectionMode mode) throws ModeNotSupportedException  {    	
    	rtpAudioChannel.updateMode(mode);     	
    	super.setMode(mode);
    }
    
    public void setOtherParty(byte[] descriptor) throws IOException {
        try {
            sdp.parse(descriptor);
        } catch (ParseException e) {
            throw new IOException(e.getMessage());
        }

        sdpComparator.negotiate(sdp, this.audioFormats, null);

        RTPFormats audio = sdpComparator.getAudio();
        if (audio.isEmpty() || !audio.hasNonDTMF()) {
            throw new IOException("Codecs are not negotiated");
        }
        
        if (!audio.isEmpty()) {
        	rtpAudioChannel.setFormatMap(audio);            
        }

        String address = null;
        if (sdp.getConnection() != null) {
            address = sdp.getConnection().getAddress();
        }

        if (sdp.getAudioDescriptor() != null) {
            rtpAudioChannel.setPeer(new InetSocketAddress(address,sdp.getAudioDescriptor().getPort()));
        }

        if(!isLocal)
        	descriptor2 = new SdpTemplate(audio,null).getSDP(channelsManager.getBindAddress(),
                "IN", "IP4",
                channelsManager.getBindAddress(),
                rtpAudioChannel.getLocalPort(),0);
        else
        	descriptor2 = new SdpTemplate(audio,null).getSDP(channelsManager.getLocalBindAddress(),
                    "IN", "IP4",
                    channelsManager.getLocalBindAddress(),
                    rtpAudioChannel.getLocalPort(),0);
        try {
            this.join();
        } catch (Exception e) {
        	//exception is possible here when already joined , should not log
        }
    }

    public void setOtherParty(Text descriptor) throws IOException {
    	try {
            sdp.init(descriptor);
        } catch (ParseException e) {
            throw new IOException(e.getMessage());
        }

        if (sdp != null && sdp.getAudioDescriptor() != null && sdp.getAudioDescriptor().getFormats() != null) {
            logger.info("Formats" + sdp.getAudioDescriptor().getFormats());
        }
        sdpComparator.negotiate(sdp, this.audioFormats, null);

        RTPFormats audio = sdpComparator.getAudio();
        if (audio.isEmpty() || !audio.hasNonDTMF()) {
            throw new IOException("Codecs are not negotiated");
        }
        
        if (!audio.isEmpty()) {
        	rtpAudioChannel.setFormatMap(audio);            
        }

        String address = null;
        if (sdp.getAudioDescriptor() != null) {
            address = sdp.getAudioDescriptor().getConnection() != null? 
                    sdp.getAudioDescriptor().getConnection().getAddress() :
                    sdp.getConnection().getAddress();
            rtpAudioChannel.setPeer(new InetSocketAddress(address,sdp.getAudioDescriptor().getPort()));
        }

        if(!isLocal)
        	descriptor2 = new SdpTemplate(audio,null).getSDP(channelsManager.getBindAddress(),
                "IN", "IP4",
                channelsManager.getBindAddress(),
                rtpAudioChannel.getLocalPort(),0);
        else
        	descriptor2 = new SdpTemplate(audio,null).getSDP(channelsManager.getLocalBindAddress(),
                    "IN", "IP4",
                    channelsManager.getLocalBindAddress(),
                    rtpAudioChannel.getLocalPort(),0);
        try {
            this.join();
        } catch (Exception e) {
        	//exception is possible here when already joined , should not log
        }                      
    }
    

    /**
     * (Non Java-doc).
     *
     * @see ua.mobius.media.server.spi.Connection#getDescriptor()
     */
    @Override
    public String getDescriptor() {
        return descriptor2 != null ? descriptor2 : descriptor;
    }
    
    public long getPacketsReceived() {
    	return rtpAudioChannel.getPacketsReceived();       	
    }

    public long getBytesReceived() {
        return 0;
    }
    
    /**
     * (Non Java-doc).
     * 
     * @see ua.mobius.media.server.spi.Connection#getPacketsTransmitted(ua.mobius.media.server.spi.MediaType) 
     */
    public long getPacketsTransmitted() {
    	return rtpAudioChannel.getPacketsTransmitted();
    }

    /**
     * (Non Java-doc).
     * 
     * @see ua.mobius.media.server.spi.Connection#getBytesTransmitted() 
     */
    public long getBytesTransmitted() {
        return 0;
    }
    
    /**
     * (Non Java-doc).
     * 
     * @see ua.mobius.media.server.spi.Connection#getJitter() 
     */
    public double getJitter() {
        return 0;
    }
    
    @Override
    public String toString() {
        return "RTP Connection [" + getEndpoint().getLocalName() ;
    }

    public void onRtpFailure() {
    	onFailed();
    }
    
    @Override
    public void setConnectionFailureListener(ConnectionFailureListener connectionFailureListener)
    {
    	this.connectionFailureListener=connectionFailureListener;
    }
    
    @Override
    protected void onCreated() throws Exception {
        if (this.isAudioCapabale)
            rtpAudioChannel.bind(isLocal);        

        if(!isLocal)
        	descriptor = template.getSDP(channelsManager.getBindAddress(),
                "IN", "IP4",
                channelsManager.getBindAddress(),
                rtpAudioChannel.getLocalPort(),0);
        else
        	descriptor = template.getSDP(channelsManager.getLocalBindAddress(),
                    "IN", "IP4",
                    channelsManager.getLocalBindAddress(),
                    rtpAudioChannel.getLocalPort(),0);
    }    
    
    @Override
    protected void onFailed() {
    	if(this.connectionFailureListener!=null)
        	connectionFailureListener.onFailure();
        
    	descriptor2 = null;
    	try {
            setMode(ConnectionMode.INACTIVE);
        } catch (ModeNotSupportedException e) {
        }
    	
        if (rtpAudioChannel != null)        	
            rtpAudioChannel.close();        
    }

    @Override
    protected void onOpened() throws Exception {
    }

    @Override
    protected void onClosed() {
        descriptor2 = null;
        
        try {
            setMode(ConnectionMode.INACTIVE);
        } catch (ModeNotSupportedException e) {
        }

        if (this.isAudioCapabale)
        	this.rtpAudioChannel.close();        

        releaseConnection(ConnectionType.RTP);        
        this.connectionFailureListener=null;          
    }
}
