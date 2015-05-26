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

import ua.mobius.media.server.scheduler.DefaultScheduler;
import ua.mobius.media.server.spi.dtmf.DtmfDetectorListener;
import ua.mobius.media.server.impl.resource.dtmf.DetectorImpl;
import ua.mobius.media.server.component.oob.OOBComponent;
import ua.mobius.media.server.component.oob.OOBSplitter;
import ua.mobius.media.server.component.oob.OOBInput;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.net.DatagramSocket;
import java.net.SocketException;
import ua.mobius.media.ComponentType;
import ua.mobius.media.server.spi.ConnectionMode;
import ua.mobius.media.server.spi.dtmf.DtmfEvent;
import ua.mobius.media.server.spi.format.Formats;
import ua.mobius.media.server.spi.format.AudioFormat;
import ua.mobius.media.server.spi.memory.ByteFrame;
import ua.mobius.media.server.spi.memory.ByteMemory;
import ua.mobius.media.server.component.DspFactoryImpl;
import ua.mobius.media.server.component.Dsp;
import ua.mobius.media.server.impl.AbstractOOBSource;
import ua.mobius.media.server.impl.rtp.sdp.AVProfile;
import ua.mobius.media.server.scheduler.DefaultClock;
import ua.mobius.media.server.io.network.UdpManager;
import ua.mobius.media.server.scheduler.Scheduler;
import ua.mobius.media.server.scheduler.Clock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ua.mobius.media.server.spi.format.FormatFactory;
import static org.junit.Assert.*;

/**
 *
 * @author oifa yulian
 */
public class LocalEventTest implements DtmfDetectorListener {

    //clock and scheduler
    private Clock clock;
    private Scheduler scheduler;

    private UdpManager udpManager;

    private ChannelsManager channelsManager;
    
    private DetectorImpl detector;
    
    private LocalDataChannel channel1,channel2;
    
    private Sender sender;
    
    private OOBSplitter oobSplitter1,oobSplitter2;
    
    private OOBComponent inputComponent;
    private OOBComponent outputComponent;
    
    private int count=0;
    
    public LocalEventTest() {
    }

    @Before
    public void setUp() throws Exception {
    	//use default clock
        clock = new DefaultClock();

        //create single thread scheduler
        scheduler = new DefaultScheduler();
        scheduler.setClock(clock);
        scheduler.start();

        udpManager = new UdpManager(scheduler);
        udpManager.start();
        
        channelsManager = new ChannelsManager(udpManager);
        channelsManager.setScheduler(scheduler);
        
        detector = new DetectorImpl("dtmf", scheduler);
        detector.setVolume(-35);
        detector.setDuration(40);
        detector.addListener(this);
        
        channel1 = channelsManager.getLocalChannel();
        channel2 = channelsManager.getLocalChannel();
        channel1.join(channel2);
        
        oobSplitter1=new OOBSplitter(scheduler);
        oobSplitter2=new OOBSplitter(scheduler);
        
        sender = new Sender();                

        oobSplitter1.addOutsideComponent(channel1.getOOBComponent());        
        oobSplitter2.addInsideComponent(channel2.getOOBComponent());  
        
        outputComponent=new OOBComponent(1);
        outputComponent.addOutput(detector.getOOBOutput());
        outputComponent.updateMode(true,true);
        oobSplitter1.addInsideComponent(outputComponent);               
        
        inputComponent=new OOBComponent(2);
        inputComponent.addInput(sender.getOOBInput());
        inputComponent.updateMode(true,true);
        oobSplitter2.addOutsideComponent(inputComponent);    	
    }

    @After
    public void tearDown() {
    	channel1.unjoin();
    	oobSplitter1.stop();
    	oobSplitter2.stop();
    	sender.deactivate();
        udpManager.stop();
        scheduler.stop();        
    }

    @Test
    public void testTransmission() throws Exception {
    	channel1.updateMode(ConnectionMode.SEND_RECV);
    	channel2.updateMode(ConnectionMode.SEND_RECV);
    	oobSplitter1.start();
    	oobSplitter2.start();
    	detector.activate();
    	sender.activate();
    	
        Thread.sleep(5000);
        
        channel1.updateMode(ConnectionMode.INACTIVE);
        channel2.updateMode(ConnectionMode.INACTIVE);
        oobSplitter1.stop();
        oobSplitter2.stop();
    	detector.deactivate();
    	sender.deactivate();
    	
    	assertEquals(4,count);
    }

    public void process(DtmfEvent event) {
    	count++;
        System.out.println("TONE=" + event.getTone());
    }
    
    private class Sender extends AbstractOOBSource {
        
        private ByteFrame currFrame;        
        private OOBInput oobInput;        
        int index=0;
        
        private byte[][] evt1 = new byte[][]{
            new byte[] {0x0b, 0x0a, 0x00, (byte)0xa0},
            new byte[] {0x0b, 0x0a, 0x01, (byte)0x40},
            new byte[] {0x0b, 0x0a, 0x01, (byte)0xe0},
            new byte[] {0x0b, 0x0a, 0x02, (byte)0x80},
            new byte[] {0x0b, 0x0a, 0x03, (byte)0x20},
            new byte[] {0x0b, 0x0a, 0x03, (byte)0xc0},
            new byte[] {0x0b, 0x0a, 0x04, (byte)0x60},
            new byte[] {0x0b, 0x0a, 0x05, (byte)0x00},
            new byte[] {0x0b, (byte)0x8a, 0x05, (byte)0xa0},
            new byte[] {0x0b, (byte)0x8a, 0x05, (byte)0xa0},
            new byte[] {0x0b, (byte)0x8a, 0x05, (byte)0xa0}
        };
        
        public Sender() throws SocketException {
        	super("oob generator", scheduler,scheduler.INPUT_QUEUE);            
        	
        	index=0;
        	this.oobInput=new OOBInput(ComponentType.DTMF_GENERATOR.getType());
        	this.connect(oobInput);
        }
        
        public OOBInput getOOBInput()
        {
        	return this.oobInput;
        } 
        
        @Override
        public ByteFrame evolve(long timestamp) {
    		if(index >= 200)
        		return null;    		    		
    	
    		ByteFrame frame ;
    		if(index%50>=7)
    		{
    			frame = ByteMemory.allocate(3);
    			frame.setOffset(0);
                frame.setLength(3);
                frame.setTimestamp(getMediaTime());
                frame.setDuration(20000000L);
    		}
    		else
    		{
    			frame = ByteMemory.allocate(4);
    			byte[] data=frame.getData();
    			System.arraycopy(evt1[index%50], 0, data, 0, 4);
    			frame.setOffset(0);
                frame.setLength(4);
                frame.setTimestamp(getMediaTime());
                frame.setDuration(20000000L);
    		}
    		
            index++;
            return frame;
    	}
    	
    	@Override
        public void activate() {
            start();
        }
    	
    	@Override
        public void deactivate() {
            stop();
        } 
    }
}