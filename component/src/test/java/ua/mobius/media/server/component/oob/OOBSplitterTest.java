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

package ua.mobius.media.server.component.oob;

import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import ua.mobius.media.MediaSource;

import ua.mobius.media.server.scheduler.Clock;
import ua.mobius.media.server.scheduler.DefaultClock;
import ua.mobius.media.server.scheduler.DefaultScheduler;
import ua.mobius.media.server.scheduler.Scheduler;

/**
 *
 * @author yulian oifa
 */
public class OOBSplitterTest {

    private Clock clock;
    private Scheduler scheduler;

    private OOBSender sender;

    private OOBReceiver receiver1;
    private OOBReceiver receiver2;
    private OOBReceiver receiver3;

    private OOBComponent senderComponent;
    private OOBComponent receiver1Component;
    private OOBComponent receiver2Component;
    private OOBComponent receiver3Component;
    
    private OOBSplitter splitter;

    public OOBSplitterTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws IOException {
    	clock = new DefaultClock();

        scheduler = new DefaultScheduler();
        scheduler.setClock(clock);
        scheduler.start();

        sender = new OOBSender(scheduler);
        
        receiver1 = new OOBReceiver("receiver-1",scheduler);
        receiver2 = new OOBReceiver("receiver-2",scheduler);
        receiver3 = new OOBReceiver("receiver-3",scheduler);

        senderComponent=new OOBComponent(1);
        senderComponent.addInput(sender.getOOBInput());
        senderComponent.updateMode(true,false);
        
        receiver1Component=new OOBComponent(2);
        receiver1Component.addOutput(receiver1.getOOBOutput());
        receiver1Component.updateMode(false,true);
        
        receiver2Component=new OOBComponent(3);
        receiver2Component.addOutput(receiver2.getOOBOutput());
        receiver2Component.updateMode(false,true);
        
        receiver3Component=new OOBComponent(4);
        receiver3Component.addOutput(receiver3.getOOBOutput());
        receiver3Component.updateMode(false,true);
        
        splitter = new OOBSplitter(scheduler);
        splitter.addInsideComponent(senderComponent);
        splitter.addOutsideComponent(receiver1Component);
        splitter.addOutsideComponent(receiver2Component);
        splitter.addOutsideComponent(receiver3Component);             
    }

    @After
    public void tearDown() throws InterruptedException {
        scheduler.stop();
    }

    @Test
    public void testTransfer() throws InterruptedException {
    	sender.activate();
    	splitter.start();
        receiver1.activate();
        receiver2.activate();
        receiver3.activate();
        
        Thread.sleep(5000);

        sender.deactivate();
        splitter.stop();
        receiver1.deactivate();
        receiver2.deactivate();
        receiver3.deactivate();

        int res = receiver1.getPacketsCount();
        assertEquals(50, res, 5);

        res = receiver2.getPacketsCount();
        assertEquals(50, res, 5);

        res = receiver3.getPacketsCount();
        assertEquals(50, res, 5);       
    }
}