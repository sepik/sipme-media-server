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

package ua.mobius.protocols.mgcp.utils;

import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.log4j.Logger;

public class PacketRepresentationFactory {

	private static final Logger logger = Logger.getLogger(PacketRepresentationFactory.class);

	private ConcurrentLinkedQueue<PacketRepresentation> list = new ConcurrentLinkedQueue<PacketRepresentation>();
	private int size = 0;
	private int dataArrSize = 0;
	private int count = 0;

	public PacketRepresentationFactory(int size, int dataArrSize) {
		this.size = size;
		this.dataArrSize = dataArrSize;
		for (int i = 0; i < size; i++) {
			PacketRepresentation pr = new PacketRepresentation(dataArrSize, this); 
			list.offer(pr);
		}				
	}

	public PacketRepresentation allocate() {
		PacketRepresentation pr = list.poll();

		if(pr!=null){
			pr.setLength(0);
			return pr;
		}

		pr = new PacketRepresentation(this.dataArrSize, this);
		count++;

		if (logger.isDebugEnabled()) {
			logger.debug("PRFactory underflow. Count = " + count);			
		}
		
		logger.error("PRFactory underflow. Count = " + count);
		
		return pr;
	}

	public void deallocate(PacketRepresentation pr) {
		//lets not discard will cause gc
		list.offer(pr);
	}

	public int getSize() {
		return this.size;
	}

	public int getDataArrSize() {
		return this.dataArrSize;
	}

	public int getCount() {
		return this.count;
	}

}
