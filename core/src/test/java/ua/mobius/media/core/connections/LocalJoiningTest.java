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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import ua.mobius.media.Component;
import ua.mobius.media.ComponentType;
import ua.mobius.media.core.MyTestEndpoint;
import ua.mobius.media.core.ResourcesPool;
import ua.mobius.media.server.impl.rtp.ChannelsManager;
import ua.mobius.media.server.component.DspFactoryImpl;
import ua.mobius.media.server.component.audio.SpectraAnalyzer;
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

/**
 *
 * @author yulian oifa
 */
public class LocalJoiningTest {

    //clock and scheduler
    private Clock clock;
    private Scheduler scheduler;

    //endpoint and connection
    private MyTestEndpoint endpoint1;
    private MyTestEndpoint endpoint2;

    private ResourcesPool resourcesPool;
    
    private ChannelsManager channelsManager;
    
    private int count;

    Component sine1,sine2;
    Component analyzer1,analyzer2;
    
    protected DspFactoryImpl dspFactory = new DspFactoryImpl();
    
    public LocalJoiningTest() {
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

        //create single thread scheduler
        scheduler = new DefaultScheduler();
        scheduler.setClock(clock);
        scheduler.start();

        channelsManager = new ChannelsManager(new UdpManager(scheduler));
        channelsManager.setScheduler(scheduler);
        
        resourcesPool=new ResourcesPool(scheduler, channelsManager, dspFactory);
        
        //assign scheduler to the endpoint
        endpoint1 = new MyTestEndpoint("test-1");
        endpoint1.setScheduler(scheduler);
        endpoint1.setResourcesPool(resourcesPool);
        endpoint1.setFreq(200);
        endpoint1.start();

        endpoint2 = new MyTestEndpoint("test-2");
        endpoint2.setScheduler(scheduler);
        endpoint2.setResourcesPool(resourcesPool);
        endpoint2.setFreq(200);
        endpoint2.start();
        
        sine1=endpoint1.getResource(MediaType.AUDIO,ComponentType.SINE);
    	sine2=endpoint2.getResource(MediaType.AUDIO,ComponentType.SINE);
    	
    	analyzer1=endpoint1.getResource(MediaType.AUDIO,ComponentType.SPECTRA_ANALYZER);
    	analyzer2=endpoint2.getResource(MediaType.AUDIO,ComponentType.SPECTRA_ANALYZER);    	
    }

    @After
    public void tearDown() {
        if (endpoint1 != null) {
            endpoint1.stop();
        }

        if (endpoint2 != null) {
            endpoint2.stop();
        }

        scheduler.stop();
    }

    /**
     * Test of setOtherParty method, of class LocalConnectionImpl.
     */
    @Test
    public void testFullduplex() throws Exception {
    	long s = System.nanoTime();

        sine1.activate();
    	sine2.activate();
    	
    	analyzer1.activate();
    	analyzer2.activate();
    	
    	Connection connection1 = endpoint1.createConnection(ConnectionType.LOCAL,false);
        Connection connection2 = endpoint2.createConnection(ConnectionType.LOCAL,false);
        
        connection1.setOtherParty(connection2);

        connection1.setMode(ConnectionMode.SEND_RECV);
        connection2.setMode(ConnectionMode.SEND_RECV);
        System.out.println("Duration= " + (System.nanoTime() - s));

        Thread.sleep(5000);

        sine1.deactivate();
    	sine2.deactivate();
    	
    	analyzer1.deactivate();
    	analyzer2.deactivate();
    	
    	SpectraAnalyzer a1 = (SpectraAnalyzer) analyzer1;
        SpectraAnalyzer a2 = (SpectraAnalyzer) analyzer2;

        int[] s1 = a1.getSpectra();
        int[] s2 = a2.getSpectra();

        endpoint1.deleteConnection(connection1);
        endpoint2.deleteConnection(connection2);

        for (int i = 0; i < s2.length; i++) {
            System.out.println(i + " " + s2[i]);
        }

        assertEquals(1, s1.length);
        assertEquals(1, s2.length);    	
    }

    @Test
    public void testHalfDuplex() throws Exception {
    	long s = System.nanoTime();

        sine1.activate();
    	sine2.activate();
    	
    	analyzer1.activate();
    	analyzer2.activate();
    	
    	Connection connection1 = endpoint1.createConnection(ConnectionType.LOCAL,false);
        Connection connection2 = endpoint2.createConnection(ConnectionType.LOCAL,false);
        
        connection1.setOtherParty(connection2);

        connection1.setMode(ConnectionMode.SEND_ONLY);
        connection2.setMode(ConnectionMode.RECV_ONLY);
        
        System.out.println("Duration= " + (System.nanoTime() - s));

        Thread.sleep(5000);

        sine1.deactivate();
    	sine2.deactivate();
    	
    	analyzer1.deactivate();
    	analyzer2.deactivate();
    	
    	SpectraAnalyzer a1 = (SpectraAnalyzer) analyzer1;
        SpectraAnalyzer a2 = (SpectraAnalyzer) analyzer2;

        int[] s1 = a1.getSpectra();
        int[] s2 = a2.getSpectra();

        endpoint1.deleteConnection(connection1);
        endpoint2.deleteConnection(connection2);

        for (int i = 0; i < s2.length; i++) {
            System.out.println(i + " " + s2[i]);
        }

        assertEquals(0, s1.length);
        assertEquals(1, s2.length);    	
    }
}