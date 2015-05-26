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
import ua.mobius.media.core.Server;
import ua.mobius.media.core.ResourcesPool;
import ua.mobius.media.core.endpoints.impl.ConferenceEndpoint;
import ua.mobius.media.core.endpoints.impl.IvrEndpoint;
import ua.mobius.media.server.component.DspFactoryImpl;
import ua.mobius.media.server.component.audio.SpectraAnalyzer;
import ua.mobius.media.server.mgcp.controller.Controller;
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
import ua.mobius.media.server.spi.recorder.Recorder;
import ua.mobius.media.server.utils.Text;

/**
 *
 * @author yulian oifa
 */
public class RecordingTest {

    //clock and scheduler
    protected Clock clock;
    protected Scheduler scheduler;

    protected ChannelsManager channelsManager;

    protected UdpManager udpManager;
    protected DspFactoryImpl dspFactory = new DspFactoryImpl();
    
    private Controller controller;
    private ResourcesPool resourcesPool;
    
    //user and ivr endpoint
    private IvrEndpoint user, ivr;    
    
    private Server server;
    
    public RecordingTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
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
        
        server=new Server();
        server.setClock(clock);
        server.setScheduler(scheduler);
        server.setUdpManager(udpManager);
        server.setResourcesPool(resourcesPool);        
        
        controller=new Controller();
        controller.setUdpInterface(udpManager);
        controller.setPort(2427);
        controller.setScheduler(scheduler); 
        controller.setServer(server);        
        controller.setConfigurationByURL(this.getClass().getResource("/mgcp-conf.xml"));
        
        controller.start();
        
        user = new IvrEndpoint("/mobius/ivr/1");
        ivr = new IvrEndpoint("/mobius/ivr/2");
        
        server.install(user,null);
        server.install(ivr,null);      	
    }

    @After
    public void tearDown() {
    	controller.stop();
    	server.stop();    	       
        
        if (user != null) {
            user.stop();
        }

        if (ivr != null) {
            ivr.stop();
        }    	
    }

    /**
     * Test of setOtherParty method, of class LocalConnectionImpl.
     */
//    @Test
    public void testRecording() throws Exception {
        long s = System.nanoTime();
        
        //create user connection
        Connection userConnection = user.createConnection(ConnectionType.RTP,false);        
        Text sd2 = new Text(userConnection.getDescriptor());
        userConnection.setMode(ConnectionMode.INACTIVE);
        Thread.sleep(50);
        
        //create server connection
        Connection ivrConnection = ivr.createConnection(ConnectionType.RTP,false);        
        Text sd1 = new Text(ivrConnection.getDescriptor());
        
        ivrConnection.setOtherParty(sd2);
        ivrConnection.setMode(ConnectionMode.SEND_RECV);
        Thread.sleep(50);
        
        //modify client
        userConnection.setOtherParty(sd1);
        userConnection.setMode(ConnectionMode.SEND_RECV);
        Thread.sleep(50);

        Recorder recorder = (Recorder) ivr.getResource(MediaType.AUDIO, ComponentType.RECORDER);
        recorder.setRecordFile("file:///home/kulikov/test-recording.wav", false);
        recorder.activate();
        
        Player player = (Player) user.getResource(MediaType.AUDIO, ComponentType.PLAYER);        
        player.setURL("file:///home/kulikov/jsr-309-tck/media/dtmfs-1-9.wav");
        player.activate();
        
        Thread.sleep(10000);
        
        player.deactivate();
        recorder.deactivate();
        
        user.deleteConnection(userConnection);
        ivr.deleteConnection(ivrConnection);
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
