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
import ua.mobius.media.core.ResourcesPool;
import ua.mobius.media.server.component.DspFactoryImpl;
import ua.mobius.media.server.impl.rtp.ChannelsManager;
import ua.mobius.media.server.io.network.UdpManager;
import ua.mobius.media.server.scheduler.Clock;
import ua.mobius.media.server.scheduler.DefaultClock;
import ua.mobius.media.server.scheduler.DefaultScheduler;
import ua.mobius.media.server.scheduler.Scheduler;
import ua.mobius.media.server.spi.ConnectionType;
import ua.mobius.media.server.spi.ResourceUnavailableException;
import ua.mobius.media.server.spi.TooManyConnectionsException;

/**
 *
 * @author yulian oifa
 */
public class LocalConnectionImplTest {

    //clock and scheduler
    private Clock clock;
    private Scheduler scheduler;
    //endpoint and connection
    private LocalConnectionImpl connection;
    private MyTestEndpoint endpoint;
    private ResourcesPool resourcesPool;

    private ChannelsManager channelsManager;

    protected DspFactoryImpl dspFactory = new DspFactoryImpl();
    
    public LocalConnectionImplTest() {
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
        endpoint = new MyTestEndpoint("test");
        endpoint.setScheduler(scheduler);
        endpoint.setResourcesPool(resourcesPool);
        endpoint.start();

        connection = (LocalConnectionImpl) endpoint.createConnection(ConnectionType.LOCAL,false);    	
    }

    @After
    public void tearDown() {
    	endpoint.deleteAllConnections();
        endpoint.stop();
        scheduler.stop();
    }

    /**
     * Test of setOtherParty method, of class LocalConnectionImpl.
     */
    @Test
    public void testDescription() throws Exception {
//        connection.bind();
        Thread.sleep(1000);

        System.out.println(connection.getDescriptor());        
    }

    @Test
    public void testDuration() throws Exception {
        long s = System.nanoTime();
        for (int i = 0; i < 9; i++) {
            connection = (LocalConnectionImpl) endpoint.createConnection(ConnectionType.LOCAL,false);
//            connection.bind();
        }
        System.out.println("Duration=" + (System.nanoTime() - s));
    }
}
