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
import ua.mobius.media.core.MyTestEndpoint;
import static org.junit.Assert.*;
import ua.mobius.media.core.connections.BaseConnection;
import ua.mobius.media.core.ResourcesPool;
import ua.mobius.media.server.component.DspFactoryImpl;
import ua.mobius.media.server.impl.rtp.ChannelsManager;
import ua.mobius.media.server.io.network.UdpManager;
import ua.mobius.media.server.scheduler.Clock;
import ua.mobius.media.server.scheduler.DefaultClock;
import ua.mobius.media.server.scheduler.Scheduler;
import ua.mobius.media.server.spi.ConnectionState;
import ua.mobius.media.server.spi.ConnectionType;
import ua.mobius.media.server.spi.ResourceUnavailableException;
import ua.mobius.media.server.spi.TooManyConnectionsException;

/**
 *
 * @author yulian oifa
 */
public class BaseConnectionFSMTest1 {

    //clock and scheduler
    private Clock clock;
    private Scheduler scheduler;

    //endpoint and connection
    private BaseConnection connection;
    private MyTestEndpoint endpoint;
    private ResourcesPool resourcesPool;
    
    //RTP
    private ChannelsManager channelsManager;

    protected DspFactoryImpl dspFactory = new DspFactoryImpl();
        
    public BaseConnectionFSMTest1() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws ResourceUnavailableException, IOException, TooManyConnectionsException {
        ConnectionState.OPEN.setTimeout(5);

        //use default clock
        clock = new DefaultClock();
        
        //create single thread scheduler 
        scheduler = new Scheduler();
        scheduler.setClock(clock);
        scheduler.start();

        channelsManager = new ChannelsManager(new UdpManager(scheduler));
        channelsManager.setScheduler(scheduler);

        resourcesPool=new ResourcesPool(scheduler, channelsManager, dspFactory);
        
        //assign scheduler to the endpoint
        endpoint = new MyTestEndpoint("test");
        endpoint.setScheduler(scheduler);
        endpoint.setResourcesPool(resourcesPool);
        endpoint.start();

        connection = (BaseConnection) endpoint.createConnection(ConnectionType.LOCAL,false);

    }

    @After
    public void tearDown() {
        endpoint.stop();
        scheduler.stop();
    }

    @Test
    public void testCreateTransition() throws Exception {
        assertEquals(ConnectionState.NULL, connection.getState());
        connection.bind();

        Thread.sleep(1000);
        assertEquals(ConnectionState.HALF_OPEN, connection.getState());
    }

    @Test
    public void test_HALF_OPEN_Timeout() throws Exception {
        assertEquals(ConnectionState.NULL, connection.getState());
        connection.bind();

        Thread.sleep(1000);
        assertEquals(ConnectionState.HALF_OPEN, connection.getState());

        Thread.sleep(5000);
        assertEquals(ConnectionState.NULL, connection.getState());
    }

    @Test
    public void testOpenTransition() throws Exception {
        assertEquals(ConnectionState.NULL, connection.getState());
        connection.bind();

        Thread.sleep(1000);
        assertEquals(ConnectionState.HALF_OPEN, connection.getState());

        connection.join();

        Thread.sleep(1000);
        assertEquals(ConnectionState.OPEN, connection.getState());
    }

    @Test
    public void test_OPEN_timeout() throws Exception {
        assertEquals(ConnectionState.NULL, connection.getState());
        connection.bind();

        Thread.sleep(1000);
        assertEquals(ConnectionState.HALF_OPEN, connection.getState());

        connection.join();

        Thread.sleep(1000);
        assertEquals(ConnectionState.OPEN, connection.getState());

        Thread.sleep(5000);
        assertEquals(ConnectionState.NULL, connection.getState());
    }

    @Test
    public void test_HALF_OPEN_Close() throws Exception {
        assertEquals(ConnectionState.NULL, connection.getState());
        connection.bind();

        Thread.sleep(1000);
        assertEquals(ConnectionState.HALF_OPEN, connection.getState());

        connection.close();
        Thread.sleep(1000);
        assertEquals(ConnectionState.NULL, connection.getState());
    }


    @Test
    public void test_OPEN_Close() throws Exception {
        assertEquals(ConnectionState.NULL, connection.getState());
        connection.bind();

        Thread.sleep(1000);
        assertEquals(ConnectionState.HALF_OPEN, connection.getState());

        connection.join();

        Thread.sleep(1000);
        assertEquals(ConnectionState.OPEN, connection.getState());

        connection.close();
        Thread.sleep(1000);
        assertEquals(ConnectionState.NULL, connection.getState());
    }

}