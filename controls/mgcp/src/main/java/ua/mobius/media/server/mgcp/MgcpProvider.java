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
package ua.mobius.media.server.mgcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.log4j.Logger;
import java.util.concurrent.ConcurrentLinkedQueue;
import ua.mobius.media.server.io.network.ProtocolHandler;
import ua.mobius.media.server.io.network.UdpManager;
import ua.mobius.media.server.mgcp.message.MgcpMessage;
import ua.mobius.media.server.mgcp.message.MgcpRequest;
import ua.mobius.media.server.mgcp.message.MgcpResponse;
import ua.mobius.media.server.scheduler.Scheduler;
import ua.mobius.media.server.scheduler.Task;
import ua.mobius.media.server.spi.listener.Listeners;
import ua.mobius.media.server.spi.listener.TooManyListenersException;

/**
 *
 * @author Oifa Yulian
 */
public class MgcpProvider {
    
    private String name;
    
    //event listeners
    private Listeners<MgcpListener> listeners = new Listeners();
    
    //Underlying network interface\
    private UdpManager transport;
    
    //datagram channel
    private DatagramChannel channel;
    
    //MGCP port number
    private int port;
    
    //Job scheduler
    private Scheduler scheduler;
    
    //transmission buffer
    private ConcurrentLinkedQueue<ByteBuffer> txBuffer = new ConcurrentLinkedQueue<ByteBuffer>();
    
    //receiver buffer
    private ByteBuffer rxBuffer = ByteBuffer.allocate(8192);
    
    //pool of events
    private ConcurrentLinkedQueue<MgcpEventImpl> events = new ConcurrentLinkedQueue<MgcpEventImpl>();
        
    private final static Logger logger = Logger.getLogger(MgcpProvider.class);
    /**
     * Creates new provider instance.
     * 
     * @param transport the UDP interface instance.
     * @param port port number to bind
     * @param scheduler job scheduler
     */
    public MgcpProvider(UdpManager transport, int port, Scheduler scheduler) {
        this.transport = transport;
        this.port = port;
        this.scheduler = scheduler;
        
        //prepare event pool
        for (int i = 0; i < 100; i++) {
            events.offer(new MgcpEventImpl(this));
        }
        
        for(int i=0;i<100;i++)
        	txBuffer.offer(ByteBuffer.allocate(8192));
    }

    /**
     * Creates new provider instance.
     * Used for tests
     * 
     * @param transport the UDP interface instance.
     * @param port port number to bind
     * @param scheduler job scheduler
     */
    protected MgcpProvider(String name, UdpManager transport, int port, Scheduler scheduler) {
        this.name = name;
        this.transport = transport;
        this.port = port;
        this.scheduler = scheduler;
        
        //prepare event pool
        for (int i = 0; i < 100; i++) {
            events.offer(new MgcpEventImpl(this));
        }
        
        for(int i=0;i<100;i++)
        	txBuffer.offer(ByteBuffer.allocate(8192));
    }
    
    /**
     * Creates new event object.
     * 
     * @param eventID the event identifier: REQUEST or RESPONSE
     * @return event object.
     */
    public MgcpEvent createEvent(int eventID, SocketAddress address) {
    	
    	MgcpEventImpl evt = events.poll();
    	if(evt==null)
    		evt=new MgcpEventImpl(this);
    	
    	evt.inQueue.set(false);
    	evt.setEventID(eventID);
    	evt.setAddress(address);
    	return evt;    	
    }
    
    
    /**
     * Sends message.
     * 
     * @param message the message to send.
     * @param destination the IP address of the destination.
     */
    public void send(MgcpEvent event, SocketAddress destination) throws IOException {
    	MgcpMessage msg = event.getMessage();
    	ByteBuffer currBuffer=txBuffer.poll();
    	if(currBuffer==null)
    		currBuffer=ByteBuffer.allocate(8192);
    	
    	msg.write(currBuffer);
    	channel.send(currBuffer, destination);
    	
    	currBuffer.clear();
    	txBuffer.offer(currBuffer);
    }

    /**
     * Sends message.
     * 
     * @param message the message to send.
     */
    public void send(MgcpEvent event) throws IOException {
    	MgcpMessage msg = event.getMessage();
    	ByteBuffer currBuffer=txBuffer.poll();
    	if(currBuffer==null)
    		currBuffer=ByteBuffer.allocate(8192);    		
    	
    	msg.write(currBuffer);
    	channel.send(currBuffer, event.getAddress());
    	
    	currBuffer.clear();
    	txBuffer.offer(currBuffer);    	
    }
    
    /**
     * Sends message.
     * 
     * @param message the message to send.
     * @param destination the IP address of the destination.
     */
    public void send(MgcpMessage message, SocketAddress destination) throws IOException {
    	ByteBuffer currBuffer=txBuffer.poll();
    	if(currBuffer==null)
    		currBuffer=ByteBuffer.allocate(8192);
    	
    	message.write(currBuffer);
    	channel.send(currBuffer, destination);
    	
    	currBuffer.clear();
    	txBuffer.offer(currBuffer);    	
    }
    
    /**
     * Registers new even listener.
     * 
     * @param listener the listener instance to be registered.
     * @throws TooManyListenersException 
     */
    public void addListener(MgcpListener listener) throws TooManyListenersException {
        listeners.add(listener);
    }
    
    /**
     * Unregisters event listener.
     * 
     * @param listener the event listener instance to be unregistered.
     */
    public void removeListener(MgcpListener listener) {
        listeners.remove(listener);
    }
    
    public void activate() {
        try {
            logger.info("Opening channel");
            channel = transport.open(new MGCPHandler());
        } catch (IOException e) {
            logger.info("Could not open UDP channel: " + e.getMessage());
            return;
        }
        
        try {
            logger.info("Binding channel to "+ transport.getLocalBindAddress() + ":" + port);
            transport.bindLocal(channel, port);
        } catch (IOException e) {
            try {
                channel.close();
            } catch (IOException ex) {
            }
            logger.info("Could not open UDP channel: " + e.getMessage());
            return;
        }
    }
    
    public void shutdown() {
        if (channel != null) {
            try {
            	channel.close();
            } catch (IOException e) {
            }
        }        
    }
    
    private void recycleEvent(MgcpEventImpl event) {
    	if (event.inQueue.getAndSet(true))
    		logger.warn("====================== ALARM ALARM ALARM==============");
    	else
    	{
    		event.response.clean();
        	event.request.clean();
        	events.offer(event);
    	}
    }
    
    /**
     * MGCP message handler asynchronous implementation.
     * 
     */
    private class MGCPHandler implements ProtocolHandler {
        
        //mgcp message receiver.
        private Receiver receiver = new Receiver();
        
        /**
         * (Non Java-doc.)
         * 
         * @see ua.mobius.media.server.io.network.ProtocolHandler#receive(java.nio.channels.DatagramChannel) 
         */
        public void receive(DatagramChannel channel) {
        	receiver.perform();
        }

        /**
         * (Non Java-doc.)
         * 
         * @see ua.mobius.media.server.io.network.ProtocolHandler#send(java.nio.channels.DatagramChannel) 
         */
        public void send(DatagramChannel channel) {
        }

        public boolean isReadable() {
            return false;
        }

        public boolean isWriteable() {
            return false;
        }

        public void setKey(SelectionKey key) {
        }
        
        public void onClosed()
        {
        	//try to reopen mgcp port
        	shutdown();
        	activate();
        }
    }
    
    /**
     * Receiver of the MGCP packets.
     */
    private class Receiver {
        private SocketAddress address;
        
        public Receiver() {
            super();
        }        

        public int getQueueNumber()
        {
        	return scheduler.MANAGEMENT_QUEUE;
        }
        
        public long perform() {
            rxBuffer.clear();
            try {
                while ((address = channel.receive(rxBuffer)) != null) {
                    rxBuffer.flip();
                    
                    if (logger.isDebugEnabled()) {
                        logger.debug("Receive  message " + rxBuffer.limit() + " bytes length");
                    }
                    
                    if (rxBuffer.limit() == 0) {
                        continue;
                    }
                    
                    byte b = rxBuffer.get();
                    rxBuffer.rewind();

                    
                    //update event ID.
                    int msgType = -1;
                    if (b >= 48 && b <= 57) {
                        msgType = MgcpEvent.RESPONSE;
                    } else {
                        msgType = MgcpEvent.REQUEST;
                    }
                    
                    MgcpEvent evt = createEvent(msgType, address);
                    
                    //parse message
                    if (logger.isDebugEnabled()) {
                    	final byte[] data = rxBuffer.array();
                    	logger.debug("Parsing message: " + new String(data,0,rxBuffer.limit()));
                    }
                    MgcpMessage msg = evt.getMessage();
                    msg.read(rxBuffer);
                    
                    //deliver event to listeners
                    if (logger.isDebugEnabled()) {
                        logger.debug("Dispatching message");
                    }
                    listeners.dispatch(evt);
                    
                    //clean buffer for next reading
                    rxBuffer.clear();
                }
            } catch (Exception e) {
                logger.error("Could not process message", e);
            }
            return 0;
        }       
    }

    /**
     * MGCP event object implementation.
     * 
     */
    private class MgcpEventImpl implements MgcpEvent {
       
        //provides instance
        private MgcpProvider provider;
        
        //event type
        private int eventID;
        
        //patterns for messages: request and response
        private MgcpRequest request = new MgcpRequest();
        private MgcpResponse response = new MgcpResponse();
        
        //the source address 
        private SocketAddress address;
        
        private AtomicBoolean inQueue=new AtomicBoolean(true);
        /**
         * Creates new event object.
         * 
         * @param provider the MGCP provider instance.
         */
        public MgcpEventImpl(MgcpProvider provider) {
            this.provider = provider;
        }
        
        /**
         * (Non Java-doc.)
         * 
         * @see ua.mobius.media.server.mgcp.MgcpEvent#getSource() 
         */
        public MgcpProvider getSource() {
            return provider;
        }

        /**
         * (Non Java-doc.)
         * 
         * @see ua.mobius.media.server.mgcp.MgcpEvent#getMessage()   
         */
        public MgcpMessage getMessage() {
            return eventID == MgcpEvent.REQUEST? request : response;
        }

        /**
         * (Non Java-doc.)
         * 
         * @see ua.mobius.media.server.mgcp.MgcpEvent#getEventID()  
         */
        public int getEventID() {
            return eventID;
        }

        /**
         * Modifies event type to this event objects.
         * 
         * @param eventID the event type constant.
         */
        protected void setEventID(int eventID) {
            this.eventID = eventID;
        }
        
        /**
         * (Non Java-doc.)
         * 
         * @see ua.mobius.media.server.mgcp.MgcpEvent#recycle() 
         */
        public void recycle() {
        	recycleEvent(this);
        }

        /**
         * (Non Java-doc.)
         * 
         * @see ua.mobius.media.server.mgcp.MgcpEvent#getAddress() 
         */
        public SocketAddress getAddress() {
            return address;
        }
        
        /**
         * Modify source address of the message.
         * 
         * @param address the socket address as an object.
         */
        protected void setAddress(SocketAddress address) {
            InetSocketAddress a = (InetSocketAddress) address;
            this.address = new InetSocketAddress(a.getHostName(), a.getPort());
        }
    }
}
