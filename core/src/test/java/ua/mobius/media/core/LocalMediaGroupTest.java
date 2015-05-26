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

import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import ua.mobius.media.core.MyTestEndpoint;
import ua.mobius.media.core.ResourcesPool;
import ua.mobius.media.core.connections.LocalConnectionImpl;
import ua.mobius.media.core.endpoints.BaseMixerEndpointImpl;
import ua.mobius.media.core.endpoints.impl.IvrEndpoint;
import ua.mobius.media.core.endpoints.impl.BridgeEndpoint;
import ua.mobius.media.ComponentType;
import ua.mobius.media.server.scheduler.DefaultScheduler;
import ua.mobius.media.server.spi.MediaType;
import ua.mobius.media.server.spi.Connection;
import ua.mobius.media.server.spi.ConnectionMode;
import ua.mobius.media.server.impl.resource.dtmf.DetectorImpl;
import ua.mobius.media.server.impl.resource.dtmf.GeneratorImpl;
import ua.mobius.media.server.component.DspFactoryImpl;
import ua.mobius.media.server.impl.rtp.ChannelsManager;
import ua.mobius.media.server.io.network.UdpManager;
import ua.mobius.media.server.scheduler.Clock;
import ua.mobius.media.server.scheduler.DefaultClock;
import ua.mobius.media.server.scheduler.Scheduler;
import ua.mobius.media.server.spi.dtmf.DtmfDetectorListener;
import ua.mobius.media.server.spi.dtmf.DtmfEvent;
import ua.mobius.media.server.spi.ConnectionType;
import ua.mobius.media.server.utils.Text;
import ua.mobius.media.server.spi.ResourceUnavailableException;

/**
 *
 * @author yulian oifa
 */
public class LocalMediaGroupTest implements DtmfDetectorListener {

    //clock and scheduler
    private Clock clock;
    private Scheduler scheduler;

    //RTP
    private ChannelsManager channelsManager;

    protected DspFactoryImpl dspFactory = new DspFactoryImpl();
    
    //endpoint and connection
    private LocalConnectionImpl connection;
    private BaseMixerEndpointImpl endpoint1,endpoint2;
    private BridgeEndpoint endpoint3;
    private ResourcesPool resourcesPool;
    protected UdpManager udpManager;
    
    private String tone;
    
    public LocalMediaGroupTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws ResourceUnavailableException, IOException {
    	//use default clock
        clock = new DefaultClock();

        //create single thread scheduler
        scheduler = new DefaultScheduler();
        scheduler.setClock(clock);
        scheduler.start();

        udpManager = new UdpManager(scheduler);
        udpManager.setBindAddress("127.0.0.1");
        udpManager.start();
        
        channelsManager = new ChannelsManager(udpManager);
        channelsManager.setScheduler(scheduler);        

        dspFactory.addAudioCodec("ua.mobius.media.server.impl.dsp.audio.g711.alaw.Codec");
                
        resourcesPool=new ResourcesPool(scheduler, channelsManager, dspFactory);
        //assign scheduler to the endpoint
        endpoint1 = new IvrEndpoint("test");
        endpoint1.setScheduler(scheduler);
        endpoint1.setResourcesPool(resourcesPool);
        endpoint1.start();
        
        endpoint2 = new IvrEndpoint("test 2");
        endpoint2.setScheduler(scheduler);
        endpoint2.setResourcesPool(resourcesPool);
        endpoint2.start();    	
        
        endpoint3 = new BridgeEndpoint("test 3");
        endpoint3.setScheduler(scheduler);
        endpoint3.setResourcesPool(resourcesPool);
        endpoint3.start();
    }

    @After
    public void tearDown() {
        endpoint1.deleteAllConnections();
        endpoint2.deleteAllConnections();
        endpoint3.deleteAllConnections();
        endpoint1.releaseResource(MediaType.AUDIO,ComponentType.DTMF_GENERATOR);
        endpoint2.releaseResource(MediaType.AUDIO,ComponentType.DTMF_DETECTOR);
        endpoint1.stop();
        endpoint2.stop();
        endpoint3.stop();
        udpManager.stop();
        scheduler.stop();          
    }

    /**
     * Test of setOtherParty method, of class RtpConnectionImpl.
     */
    @Test
    public void testResources() throws Exception {
    	Connection connection1 = endpoint1.createConnection(ConnectionType.LOCAL,false);        
        Connection connection2 = endpoint3.createConnection(ConnectionType.LOCAL,false);       
        connection1.setOtherParty(connection2);        
        
        connection1.setMode(ConnectionMode.SEND_RECV);
        connection2.setMode(ConnectionMode.SEND_RECV);
        
        Connection connection3 = endpoint3.createConnection(ConnectionType.RTP,false);        
        Connection connection4 = endpoint2.createConnection(ConnectionType.RTP,false);       
        
        Text sd1 = new Text(connection3.getDescriptor());
        Text sd2 = new Text(connection4.getDescriptor());

        connection3.setOtherParty(sd2);        
        connection4.setOtherParty(sd1);
        
        connection3.setMode(ConnectionMode.SEND_RECV);
        connection4.setMode(ConnectionMode.SEND_RECV);
        
        GeneratorImpl generator1=(GeneratorImpl)endpoint1.getResource(MediaType.AUDIO,ComponentType.DTMF_GENERATOR);
        GeneratorImpl generator2=(GeneratorImpl)endpoint2.getResource(MediaType.AUDIO,ComponentType.DTMF_GENERATOR);
        DetectorImpl detector1=(DetectorImpl)endpoint2.getResource(MediaType.AUDIO,ComponentType.DTMF_DETECTOR);
        DetectorImpl detector2=(DetectorImpl)endpoint1.getResource(MediaType.AUDIO,ComponentType.DTMF_DETECTOR);
        
        detector1.addListener(this);
        detector2.addListener(this);
        
        generator1.setToneDuration(200);
        generator1.setVolume(-20);
        
        generator1.setDigit("1");
        generator1.activate();
        detector1.activate();
        
        Thread.sleep(1000);
        
        assertEquals("1", tone);        
        generator1.deactivate();
        
        tone="";
        generator1.setOOBDigit("1");
        generator1.activate();
        detector1.activate();
        
        Thread.sleep(1000);
        
        assertEquals("1", tone);
        
        tone="";
        generator2.setToneDuration(200);
        generator2.setVolume(-20);
        
        generator2.setDigit("1");
        generator2.activate();
        detector2.activate();
        
        Thread.sleep(1000);
        
        assertEquals("1", tone);        
        generator2.deactivate();
        
        tone="";
        generator2.setOOBDigit("1");
        generator2.activate();
        detector2.activate();
        
        Thread.sleep(1000);
        
        assertEquals("1", tone);
    }

    public void process(DtmfEvent event) {
    	tone = event.getTone();
    }    
}