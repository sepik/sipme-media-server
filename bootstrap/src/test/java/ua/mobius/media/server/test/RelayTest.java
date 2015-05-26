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

import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import ua.mobius.media.ComponentType;
import ua.mobius.media.core.ResourcesPool;
import ua.mobius.media.core.endpoints.impl.ConferenceEndpoint;
import ua.mobius.media.core.endpoints.impl.IvrEndpoint;
import ua.mobius.media.server.component.DspFactoryImpl;
import ua.mobius.media.server.component.audio.SpectraAnalyzer;
import ua.mobius.media.server.impl.rtp.ChannelsManager;
import ua.mobius.media.server.io.network.UdpManager;
import ua.mobius.media.server.scheduler.Clock;
import ua.mobius.media.server.scheduler.DefaultClock;
import ua.mobius.media.server.scheduler.DefaultScheduler;
import ua.mobius.media.server.scheduler.Scheduler;
import ua.mobius.media.server.spi.Connection;
import ua.mobius.media.server.spi.ConnectionMode;
import ua.mobius.media.server.spi.ConnectionType;
import ua.mobius.media.server.spi.MediaType;
import ua.mobius.media.server.spi.ResourceUnavailableException;
import ua.mobius.media.server.spi.TooManyConnectionsException;
import ua.mobius.media.server.spi.player.Player;
import ua.mobius.media.server.utils.Text;

/**
 *
 * @author yulian oifa
 */
public class RelayTest {

    //clock and scheduler
    protected Clock clock;
    protected Scheduler scheduler;

    protected ChannelsManager channelsManager;

    private ResourcesPool resourcesPool;
    
    protected UdpManager udpManager;
    protected DspFactoryImpl dspFactory = new DspFactoryImpl();
    
    //ivr endpoint
    private IvrEndpoint ivr;    
    //analyzer
    private SoundSystem soundcard;
    
    //packet relay bridge
    private ConferenceEndpoint cnfBridge;
    
    public RelayTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws ResourceUnavailableException, TooManyConnectionsException, IOException {
        //use default clock
        clock = new DefaultClock();

        dspFactory.addAudioCodec("ua.mobius.media.server.impl.dsp.audio.g711.ulaw.Codec");
        dspFactory.addAudioCodec("ua.mobius.media.server.impl.dsp.audio.g711.alaw.Codec");
        
        //create single thread scheduler
        scheduler = new DefaultScheduler();
        scheduler.setClock(clock);
        scheduler.start();

        udpManager = new UdpManager(scheduler);
        udpManager.setBindAddress("127.0.0.1");
        udpManager.start();

        channelsManager = new ChannelsManager(udpManager);
        channelsManager.setScheduler(scheduler);
        
        resourcesPool=new ResourcesPool(scheduler, channelsManager, dspFactory);
        
        //assign scheduler to the endpoint
        ivr = new IvrEndpoint("test-1");
        ivr.setScheduler(scheduler);
        ivr.setResourcesPool(resourcesPool);
        ivr.start();

        soundcard = new SoundSystem("test-2");
        soundcard.setScheduler(scheduler);
        soundcard.setResourcesPool(resourcesPool);
        soundcard.start();

        cnfBridge = new ConferenceEndpoint("test-3");
        cnfBridge.setScheduler(scheduler);
        cnfBridge.setResourcesPool(resourcesPool);
        cnfBridge.start();

    }

    @After
    public void tearDown() {
        udpManager.stop();
        scheduler.stop();
        
        if (ivr != null) {
            ivr.stop();
        }

        if (soundcard != null) {
            soundcard.stop();
        }

        if (cnfBridge != null) {
            cnfBridge.stop();
        }

    }

    /**
     * Test of setOtherParty method, of class LocalConnectionImpl.
     */
//    @Test
    public void testTransmission() throws Exception {
        long s = System.nanoTime();
        
        //create client
        Connection connection2 = soundcard.createConnection(ConnectionType.RTP,false);        
        Text sd2 = new Text(connection2.getDescriptor());
        connection2.setMode(ConnectionMode.SEND_RECV);
        Thread.sleep(50);
        
        //create server with known sdp in cnf mode
        Connection connection02 = cnfBridge.createConnection(ConnectionType.RTP,false);        
        Text sd1 = new Text(connection02.getDescriptor());
        
        connection02.setOtherParty(sd2);
        connection02.setMode(ConnectionMode.CONFERENCE);
        Thread.sleep(50);
        
        //modify client
        connection2.setOtherParty(sd1);
        connection2.setMode(ConnectionMode.SEND_RECV);
        Thread.sleep(50);
        
        //create local connection
        Connection connection1 = ivr.createConnection(ConnectionType.LOCAL,false);        
        Connection connection01 = cnfBridge.createConnection(ConnectionType.LOCAL,false);
        Thread.sleep(50);
        
        //create in send_recv mode initially
        connection1.setOtherParty(connection01);
        connection1.setMode(ConnectionMode.INACTIVE);
        connection01.setMode(ConnectionMode.SEND_RECV);

        Thread.sleep(150);

        //modify mode        
        connection01.setMode(ConnectionMode.CONFERENCE);
        connection1.setMode(ConnectionMode.SEND_RECV);
        
        Thread.sleep(350);
        
        Player player = (Player) ivr.getResource(MediaType.AUDIO, ComponentType.PLAYER);
        player.setURL("file:///home/kulikov/jsr-309-tck/media/dtmfs-1-9.wav");
        player.start();
        
        Thread.sleep(10000);
        ivr.deleteConnection(connection1);
        soundcard.deleteConnection(connection2);
        cnfBridge.deleteAllConnections();
    }

    @Test
    public void testNothing() {
        
    }
    
    private void printSpectra(String title, int[]s) {
        System.out.println(title);
        for (int i = 0; i < s.length; i++) {
            System.out.print(s[i] + " ");
        }
        System.out.println();
    }
    
}