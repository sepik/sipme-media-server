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

package ua.mobius.media.server.component;

import ua.mobius.media.server.spi.format.Formats;
import ua.mobius.media.server.spi.format.Format;
import ua.mobius.media.server.spi.format.FormatFactory;
import ua.mobius.media.server.spi.memory.ShortMemory;
import ua.mobius.media.server.spi.memory.ShortFrame;
import ua.mobius.media.server.spi.memory.ByteFrame;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author kulikov
 */
public class DspFactoryTest {

    private DspFactoryImpl dspFactory = new DspFactoryImpl();

    public DspFactoryTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of addCodec method, of class DspFactory.
     */
    @Test
    public void testLoading() throws Exception {
        dspFactory.addAudioCodec("ua.mobius.media.server.impl.dsp.audio.g711.alaw.Codec");        
        Dsp dsp = dspFactory.newAudioProcessor();
    }

    @Test
    public void testUnknownInput() throws Exception {
        dspFactory.addAudioCodec("ua.mobius.media.server.impl.dsp.audio.g711.alaw.Codec");
        
        Dsp dsp = dspFactory.newAudioProcessor();

        ShortFrame frame = ShortMemory.allocate(160);
        ByteFrame frame2 = dsp.encode(frame);

        assertEquals(frame2, null);
    }
    
    @Test
    public void testUndefinedOutput() throws Exception {
    	dspFactory.addAudioCodec("ua.mobius.media.server.impl.dsp.audio.g711.alaw.Codec");
        
        Dsp dsp = dspFactory.newAudioProcessor();

        ShortFrame frame = ShortMemory.allocate(160);
        frame.setFormat(FormatFactory.createAudioFormat("test", 8000));
        dsp.setDestinationFormat(frame.getFormat());
        ByteFrame frame2 = dsp.encode(frame);

        assertEquals(frame2, null);    	
    }

    @Test
    public void testInputToOutputMatch() throws Exception {
        Format fmt = FormatFactory.createAudioFormat("test", 8000);
        
        dspFactory.addAudioCodec("ua.mobius.media.server.impl.dsp.audio.g711.alaw.Codec");
        
        Dsp dsp = dspFactory.newAudioProcessor();
        
        ShortFrame frame = ShortMemory.allocate(160);
        frame.setFormat(fmt);
        dsp.setDestinationFormat(fmt);        
        ByteFrame frame2 = dsp.encode(frame);

        assertEquals(frame2,null);
    }

    @Test
    public void testEncoding() throws Exception {
    	Format fmt = FormatFactory.createAudioFormat("linear", 8000, 16, 1);
        Format fmt2 = FormatFactory.createAudioFormat("pcma", 8000, 8, 1);

        dspFactory.addAudioCodec("ua.mobius.media.server.impl.dsp.audio.g711.alaw.Codec");
        
        Dsp dsp = dspFactory.newAudioProcessor();
        
        ShortFrame frame = ShortMemory.allocate(160);
        frame.setFormat(fmt);
        dsp.setDestinationFormat(fmt2);
        ByteFrame frame2 = dsp.encode(frame);

        System.out.println("fmt=" + frame2.getFormat().getName());
        assertTrue("Format missmatch", fmt2.matches(frame2.getFormat()));    	
    }
}