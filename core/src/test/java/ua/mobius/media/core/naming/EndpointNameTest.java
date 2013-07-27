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

package ua.mobius.media.core.naming;

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
public class EndpointNameTest {

    public EndpointNameTest() {
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
     * Test of getCategory method, of class EndpointName.
     */
    @Test
    public void testParsing() {
        String name = "/media/packet-relay/1";
        EndpointName enpName = new EndpointName(name);
        
        assertEquals("/media/packet-relay", enpName.getCategory());
        assertEquals("1", enpName.getID());
        
        name = "/media/packet-relay/$";
        enpName = new EndpointName(name);
        
        assertEquals("/media/packet-relay", enpName.getCategory());
        assertEquals("$", enpName.getID());
        
        name = "/media/fff/xxx/$";
        enpName = new EndpointName(name);
        
        assertEquals("/media/fff/xxx", enpName.getCategory());
        assertEquals("$", enpName.getID());

        name = "media/fff/xxx/$";
        enpName = new EndpointName(name);
        
        assertEquals("media/fff/xxx", enpName.getCategory());
        assertEquals("$", enpName.getID());
    }


}