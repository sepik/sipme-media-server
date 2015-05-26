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

package ua.mobius.media.server.io.network;

import ua.mobius.media.server.scheduler.DefaultScheduler;
import ua.mobius.media.server.scheduler.Scheduler;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author yulian oifa
 */
public class UdpPeripheryTest {

    private UdpManager udpPeriphery;

    public UdpPeripheryTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws IOException {
    	Scheduler scheduler=new DefaultScheduler();
        udpPeriphery = new UdpManager(scheduler);
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of open method, of class UdpPeriphery.
     */
    @Test
    public void testOpen() throws Exception {
    	DatagramChannel channel = udpPeriphery.open(new TestHandler());
        udpPeriphery.bind(channel, 1024);
        assertTrue("Excepted bound socket", channel.socket().isBound());    	
    }

    /**
     * Test of poll method, of class UdpPeriphery.
     */
    @Test
    public void testPoll() throws IOException {
        long s = System.nanoTime();
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 9201);
        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.socket().bind(address);
        long duration = System.nanoTime() - s;
        System.out.println("dur=" + duration);
        channel.socket().close();
    }

    private class TestHandler implements ProtocolHandler {

        public void receive(DatagramChannel channel) {
        }

        public void send(DatagramChannel channel) {
        }

        public void setKey(SelectionKey key) {
        }

        public boolean isReadable() {
            return true;
        }

        public boolean isWriteable() {
            return true;
        }

        public void onClosed() {
        	
        }
    }

}