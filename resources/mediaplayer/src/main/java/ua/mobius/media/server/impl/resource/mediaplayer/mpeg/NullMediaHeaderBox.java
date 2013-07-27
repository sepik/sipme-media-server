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
package ua.mobius.media.server.impl.resource.mediaplayer.mpeg;

/**
 * <b>8.4.5.5 Null Media Header Box</b>
 * <p>
 * Streams other than visual and audio (e.g., timed metadata streams) may use a null Media Header Box, as defined here.
 * </p>
 * 
 * @author kulikov
 */
public class NullMediaHeaderBox extends FullBox {

	// File Type = moov
	static byte[] TYPE = new byte[] { AsciiTable.ALPHA_n, AsciiTable.ALPHA_m, AsciiTable.ALPHA_h, AsciiTable.ALPHA_d };
	static String TYPE_S = "nmhd";
	static {
		bytetoTypeMap.put(TYPE, TYPE_S);
	}

	public NullMediaHeaderBox(long size) {
		super(size, TYPE_S);
	}

}
