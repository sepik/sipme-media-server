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
package ua.mobius.media.core.endpoints.impl;

import ua.mobius.media.Component;
import ua.mobius.media.ComponentType;
import ua.mobius.media.server.spi.Connection;
import ua.mobius.media.server.spi.ConnectionType;
import ua.mobius.media.server.spi.ConnectionMode;
import ua.mobius.media.server.spi.MediaType;
import ua.mobius.media.server.spi.ResourceUnavailableException;
import ua.mobius.media.core.endpoints.BaseSplitterEndpointImpl;
import ua.mobius.media.core.endpoints.MediaGroup;

/**
 * Bridge endpoint implementation
 * 
 * @author yulian oifa 
 */
public class BridgeEndpoint extends BaseSplitterEndpointImpl {
    
	public BridgeEndpoint(String localName) {
    	super(localName);              
    }

	/**
     * (Non Java-doc).
     * 
     * @see ua.mobius.media.server.spi.Endpoint#getResource();
     */
    public Component getResource(MediaType mediaType, ComponentType componentType)
    {
    	return null;
    }        
}
