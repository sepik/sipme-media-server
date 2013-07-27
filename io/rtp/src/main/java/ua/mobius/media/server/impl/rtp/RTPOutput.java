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
package ua.mobius.media.server.impl.rtp;

import ua.mobius.media.server.impl.rtp.sdp.RTPFormats;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.text.Format;
import ua.mobius.media.server.component.audio.AudioOutput;
import ua.mobius.media.server.component.oob.OOBOutput;
import ua.mobius.media.MediaSink;
import ua.mobius.media.MediaSource;
import ua.mobius.media.server.impl.AbstractCompoundSink;
import ua.mobius.media.server.impl.rtp.sdp.RTPFormat;
import ua.mobius.media.server.io.network.ProtocolHandler;
import ua.mobius.media.server.scheduler.Scheduler;
import ua.mobius.media.server.scheduler.Task;
import ua.mobius.media.server.spi.FormatNotSupportedException;
import ua.mobius.media.server.spi.ConnectionMode;
import ua.mobius.media.server.spi.format.AudioFormat;
import ua.mobius.media.server.spi.format.FormatFactory;
import ua.mobius.media.server.spi.format.Formats;
import ua.mobius.media.server.spi.memory.ByteFrame;
import ua.mobius.media.server.spi.memory.ShortFrame;
import ua.mobius.media.server.spi.dsp.AudioCodec;
import ua.mobius.media.server.spi.dsp.AudioProcessor;
import org.apache.log4j.Logger;
/**
 *
 * @author Yulian oifa
 */
/**
 * Transmitter implementation.
 *
 */
public class RTPOutput extends AbstractCompoundSink {
	private AudioFormat format = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);	
	
    private RTPDataChannel channel;
    
    //signaling processor
    private AudioProcessor dsp;                               
        
    private static final Logger logger = Logger.getLogger(RTPOutput.class);
    
    private AudioOutput output;
    private OOBOutput oobOutput;
    
    /**
     * Creates new transmitter
     */
    protected RTPOutput(Scheduler scheduler,RTPDataChannel channel) {
        super("Output");
        this.channel=channel;
        output=new AudioOutput(scheduler,1);
        output.join(this); 
        
        oobOutput=new OOBOutput(scheduler,1);
        oobOutput.join(this);        
    }
    
    public AudioOutput getAudioOutput()
    {
    	return this.output;
    }
    
    public OOBOutput getOOBOutput()
    {
    	return this.oobOutput;
    }
    
    public void activate()
    {
    	output.start();
    	oobOutput.start();
    }
    
    public void deactivate()
    {
    	output.stop();
    	oobOutput.stop();
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
    }
    
    /**
     * Gets the digital signaling processor associated with this media source
     *
     * @return DSP instance.
     */
    public AudioProcessor getDsp() {
        return this.dsp;
    }

    @Override
    public void onMediaTransfer(ShortFrame frame) throws IOException {
    	/*System.out.print("INPUT:");
    	for(int i=0;i<5;i++)
    		System.out.print(frame.getData()[i] + ",");
		
		System.out.println("");*/
    	//do transcoding
    	ByteFrame outputFrame=null;
    	if (dsp != null) {
    		try
    		{
    			outputFrame = dsp.encode(frame);            			
    		}
    		catch(Exception e)
    		{
    			//transcoding error , print error and try to move to next frame
    			logger.error(e);
    			return;
    		} 
    	}
    	
    	if(outputFrame==null)
    		return;
    	
    	/*for(int i=0;i<5;i++)
    		System.out.print(outputFrame.getData()[i] + ",");
		
		System.out.println("");*/
		
    	channel.send(outputFrame);
    }   
    
    @Override
    public void onMediaTransfer(ByteFrame frame) throws IOException {
    	channel.sendDtmf(frame);
    } 
}