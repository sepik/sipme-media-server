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
import ua.mobius.media.server.component.DspFactoryImpl;
import ua.mobius.media.server.impl.rtp.ChannelsManager;
import ua.mobius.media.server.io.network.UdpManager;
import ua.mobius.media.server.scheduler.Clock;
import ua.mobius.media.server.scheduler.DefaultClock;
import ua.mobius.media.server.scheduler.Scheduler;

/**
 * Super class for all RTP transmission tests
 * 
 * @author yulian oifa
 */
public class RTPEnvironment {
    //clock and scheduler
    protected Clock clock;
    protected Scheduler scheduler;

    protected ChannelsManager channelsManager;

    protected UdpManager udpManager;
    protected DspFactoryImpl dspFactory = new DspFactoryImpl();
    
    public void setup() throws IOException {
        //use default clock
        clock = new DefaultClock();

        dspFactory.addAudioCodec("ua.mobius.media.server.impl.dsp.audio.g711.alaw.Codec");        
        
        //create single thread scheduler
        scheduler = new Scheduler();
        scheduler.setClock(clock);
        scheduler.start();

        udpManager = new UdpManager(scheduler);
        udpManager.setBindAddress("127.0.0.1");
        udpManager.start();

        channelsManager = new ChannelsManager(udpManager);
        channelsManager.setScheduler(scheduler);
    }
    
    public void tearDown() {        
        udpManager.stop();
        scheduler.stop();
    }
}
