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
package ua.mobius.media.server.mgcp.pkg.dtmf;

import ua.mobius.media.server.mgcp.controller.signal.Event;
import ua.mobius.media.server.utils.Text;

/**
 *
 * @author kulikov
 */
public class Dtmf4 extends AbstractDtmfEvent {
    private final static Event dtmf_4 = new Event(new Text("4"));

    public Dtmf4(String name) {
        super(name);
    }

    @Override
    public void onEvent(String tone) {
        if (tone.equals("4")) {
            dtmf_4.fire(this, null);
        }
    }
    @Override
	protected Event getTone() {
		return dtmf_4;
	}
}