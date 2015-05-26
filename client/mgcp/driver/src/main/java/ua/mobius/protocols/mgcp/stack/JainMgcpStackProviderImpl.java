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
package ua.mobius.protocols.mgcp.stack;

import jain.protocol.ip.mgcp.*;
import jain.protocol.ip.mgcp.message.*;
import jain.protocol.ip.mgcp.message.parms.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import ua.mobius.protocols.mgcp.handlers.TransactionHandler;
import ua.mobius.protocols.mgcp.parser.commands.*;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;


public class JainMgcpStackProviderImpl implements ExtendedJainMgcpProvider {

	private static Logger logger = Logger.getLogger(JainMgcpStackProviderImpl.class);
	private final JainMgcpStackImpl runningStack;

	// a tx handle id must be between 1 and 999999999
	private static long MAX_TRANSACTION_HANDLE_ID = 999999999L;
	private static AtomicInteger transactionHandleCounter = new AtomicInteger(Integer.MIN_VALUE);			
	private static AtomicInteger callIdentifierCounter = new AtomicInteger(Integer.MIN_VALUE);
	private static AtomicInteger requestIdentifierCounter = new AtomicInteger(Integer.MIN_VALUE);
	// For now provider is only holder of listeners
	protected Set<JainMgcpListener> jainListeners = new HashSet<JainMgcpListener>();
	// This set contains upgraded listeners - one that allow to notify when tx
	// ends
	protected Set<JainMgcpExtendedListener> jainMobiusListeners = new HashSet<JainMgcpExtendedListener>();

	protected NotifiedEntity notifiedEntity = null;

	private LinkedBlockingQueue<EventWrapper> waitingQueue=new LinkedBlockingQueue<EventWrapper>();
	
	private DispatcherThread dispatcher;
	
	public JainMgcpStackProviderImpl(JainMgcpStackImpl runningStack) {
		super();
		// eventQueue = new QueuedExecutor();
		// pool = Executors.newCachedThreadPool(new ThreadFactoryImpl());
		this.runningStack = runningStack;
		dispatcher=new DispatcherThread();				
	}

	protected void start()
	{
		dispatcher.activate();		
	}
	
	protected void stop()
	{
		dispatcher.shutdown();		
	}
	
	public void setNotifiedEntity(NotifiedEntity notifiedEntity) {
		this.notifiedEntity = notifiedEntity;
	}

	public NotifiedEntity getNotifiedEntity() {
		return this.notifiedEntity;
	}

	public void addJainMgcpListener(JainMgcpListener listener) throws TooManyListenersException {
		if (listener instanceof JainMgcpExtendedListener) {
			synchronized (this.jainMobiusListeners) {
				this.jainMobiusListeners.add((JainMgcpExtendedListener) listener);
			}
		} else {
			synchronized (this.jainListeners) {
				this.jainListeners.add(listener);
			}
		}
	}

	public JainMgcpStack getJainMgcpStack() {
		return this.runningStack;
	}

	public void removeJainMgcpListener(JainMgcpListener listener) {
		if (listener instanceof JainMgcpExtendedListener) {
			synchronized (this.jainMobiusListeners) {
				this.jainMobiusListeners.remove((JainMgcpExtendedListener) listener);
			}
		} else {
			synchronized (this.jainListeners) {
				this.jainListeners.remove(listener);
			}
		}
	}

	public void sendMgcpEvents(JainMgcpEvent[] events) throws IllegalArgumentException {
		for (JainMgcpEvent event : events) {

			// For any onther than CRCX wildcard does not count?
			if (event instanceof JainMgcpCommandEvent) {
				// SENDING REQUEST
				JainMgcpCommandEvent commandEvent = (JainMgcpCommandEvent) event;

				// This is for TCK
				if (commandEvent.getTransactionHandle() < 1) {
					commandEvent.setTransactionHandle(this.getUniqueTransactionHandler());
				}

				TransactionHandler handle = null;
				switch (commandEvent.getObjectIdentifier()) {

				case Constants.CMD_AUDIT_CONNECTION:
					if (logger.isDebugEnabled()) {
						logger.debug("Sending EndpointConfiguration object to " + commandEvent.getEndpointIdentifier());
					}
					handle = new AuditConnectionHandler(this.runningStack);
					break;

				case Constants.CMD_AUDIT_ENDPOINT:
					if (logger.isDebugEnabled()) {
						logger.debug("Sending EndpointConfiguration object to " + commandEvent.getEndpointIdentifier());
					}
					handle = new AuditEndpointHandler(this.runningStack);
					break;

				case Constants.CMD_CREATE_CONNECTION:
					if (logger.isDebugEnabled()) {
						logger.debug("Sending CreateConnection object to " + commandEvent.getEndpointIdentifier());
					}
					handle = new CreateConnectionHandler(this.runningStack);
					break;

				case Constants.CMD_DELETE_CONNECTION:
					if (logger.isDebugEnabled()) {
						logger.debug("Sending DeleteConnection object to " + commandEvent.getEndpointIdentifier());
					}
					handle = new DeleteConnectionHandler(this.runningStack);
					break;

				case Constants.CMD_ENDPOINT_CONFIGURATION:
					if (logger.isDebugEnabled()) {
						logger.debug("Sending EndpointConfiguration object to " + commandEvent.getEndpointIdentifier());
					}
					handle = new EndpointConfigurationHandler(this.runningStack);
					break;

				case Constants.CMD_MODIFY_CONNECTION:
					if (logger.isDebugEnabled()) {
						logger.debug("Sending ModifyConnection object to " + commandEvent.getEndpointIdentifier());
					}
					handle = new ModifyConnectionHandler(this.runningStack);
					break;

				case Constants.CMD_NOTIFICATION_REQUEST:
					if (logger.isDebugEnabled()) {
						logger.debug("Sending NotificationRequest object to " + commandEvent.getEndpointIdentifier());
					}
					handle = new NotificationRequestHandler(this.runningStack);
					break;

				case Constants.CMD_NOTIFY:
					if (logger.isDebugEnabled()) {
						logger.debug("Sending Notify object to NotifiedEntity"
								+ ((Notify) commandEvent).getNotifiedEntity());
					}
					handle = new NotifyHandler(this.runningStack);
					break;

				case Constants.CMD_RESP_UNKNOWN:
					if (logger.isDebugEnabled()) {
						logger.debug("Sending ResponseUnknown object to " + commandEvent.getEndpointIdentifier());
					}
					handle = new RespUnknownHandler(this.runningStack);
					break;

				case Constants.CMD_RESTART_IN_PROGRESS:
					if (logger.isDebugEnabled()) {
						logger.debug("Sending RestartInProgress object to " + commandEvent.getEndpointIdentifier());
					}
					handle = new RestartInProgressHandler(this.runningStack);
					break;

				default:
					throw new IllegalArgumentException("Could not send type of the message yet");
				}
				handle.setCommand(true);
				handle.setCommandEvent(commandEvent);
				handle.send();				
			} else {

				// SENDING RESPONSE
				int tid = event.getTransactionHandle();
				TransactionHandler handle = (TransactionHandler) runningStack.getLocalTransactions().get(
						Integer.valueOf(tid));

				if (handle != null) {
					handle.setCommand(false);
					handle.setResponseEvent((JainMgcpResponseEvent) event);
					handle.send();					
				} else {
					logger.error("The TransactionHandler not found for TransactionHandle " + tid
							+ " May be the Tx timed out. Event = " + (JainMgcpResponseEvent) event);
				}

			}
		}

	}

	public int getUniqueTransactionHandler() {
		// retreives current counter value and sets next one
		return (int) (((long)transactionHandleCounter.incrementAndGet()-(long)Integer.MIN_VALUE)%MAX_TRANSACTION_HANDLE_ID + 1L);		
	}

	public void processMgcpResponseEvent(JainMgcpResponseEvent response, JainMgcpEvent command) {
		waitingQueue.offer(new EventWrapper(response,EventWrapper.response));				
	}

	public void processMgcpCommandEvent(JainMgcpCommandEvent command) {
		waitingQueue.offer(new EventWrapper(command,EventWrapper.request));		
	}

	public void processTxTimeout(JainMgcpCommandEvent command) {
		waitingQueue.offer(new EventWrapper(command,EventWrapper.txTimeout));		
	}

	public void processRxTimeout(JainMgcpCommandEvent command) {
		waitingQueue.offer(new EventWrapper(command,EventWrapper.rxTimeout));		
	}

	public CallIdentifier getUniqueCallIdentifier() {
		return new CallIdentifier(Long.toHexString((long)callIdentifierCounter.incrementAndGet() - (long)Integer.MIN_VALUE));
	}

	public RequestIdentifier getUniqueRequestIdentifier() {		
		return new RequestIdentifier(Long.toHexString((long)requestIdentifierCounter.incrementAndGet()-(long)Integer.MIN_VALUE));
	}

	//Async part
	private LinkedList<JainMgcpEvent[]> asyncBuffer = new LinkedList<JainMgcpEvent[]>();
	
	

	public void sendAsyncMgcpEvents(JainMgcpEvent[] events)
			throws IllegalArgumentException {
		asyncBuffer.addLast(events);
		
	}

	public void flush() {
		
		JainMgcpEvent[] events;
		while( !asyncBuffer.isEmpty() )
		{
			events = this.asyncBuffer.removeFirst();
			this.sendMgcpEvents(events);
		}
	}

	private class EventWrapper
	{
		private static final int request=1;
		private static final int response=2;
		private static final int txTimeout=3;
		private static final int rxTimeout=4;
		
		protected Object event;
		protected int eventType;
		
		public EventWrapper(Object event,int eventType)
		{
			this.event=event;
			this.eventType=eventType;
		}			
	}
	
	private class DispatcherThread extends Thread
	{
		private volatile boolean active;
        JainMgcpCommandEvent command;
		JainMgcpResponseEvent response;
		
		EventWrapper currEvent;
		public void run() {
    		while(active)
    		{
    			currEvent=null;
    			while(currEvent==null)
    			{
    				try {
    					currEvent=waitingQueue.take();
    				}
    				catch(Exception e)
    				{
    					
    				}
    			}
    			
    			try {    				    			
    				switch(currEvent.eventType)
    				{
    					case EventWrapper.request:
    						command=(JainMgcpCommandEvent)currEvent.event;
    						synchronized (jainListeners) {
    							for (JainMgcpListener listener : jainListeners) {
    								listener.processMgcpCommandEvent(command);
    							}
    						}

    						synchronized (jainMobiusListeners) {
    							for (JainMgcpListener listener : jainMobiusListeners) {
    								listener.processMgcpCommandEvent(command);
    							}
    						}
    						break;
    					case EventWrapper.response:
    						response=(JainMgcpResponseEvent)currEvent.event;
    						synchronized (jainListeners) {
    							for (JainMgcpListener listener : jainListeners) {
    								listener.processMgcpResponseEvent(response);
    							}
    						}

    						synchronized (jainMobiusListeners) {
    							for (JainMgcpListener listener : jainMobiusListeners) {
    								listener.processMgcpResponseEvent(response);
    							}
    						}
    						break;
    					case EventWrapper.rxTimeout:
    						command=(JainMgcpCommandEvent)currEvent.event;
    						synchronized (jainMobiusListeners) {
    							for (JainMgcpExtendedListener listener : jainMobiusListeners) {
    								listener.transactionRxTimedOut(command);
    							}
    						}
    						// reply to server
    						response = null;
    						// FIXME - how to change o return code of transaction timeout?!?
    						switch (command.getObjectIdentifier()) {
    						case Constants.CMD_AUDIT_CONNECTION:
    							response = new AuditConnectionResponse(this, ReturnCode.Transient_Error);
    							break;
    						case Constants.CMD_AUDIT_ENDPOINT:
    							response = new AuditEndpointResponse(this, ReturnCode.Transient_Error);
    							break;
    						case Constants.CMD_CREATE_CONNECTION:
    							response = new CreateConnectionResponse(this, ReturnCode.Transient_Error, new ConnectionIdentifier(Long
    									.toHexString(new Random(System.currentTimeMillis()).nextLong())));
    							break;
    						case Constants.CMD_DELETE_CONNECTION:
    							response = new DeleteConnectionResponse(this, ReturnCode.Transient_Error);
    							break;
    						case Constants.CMD_ENDPOINT_CONFIGURATION:
    							response = new DeleteConnectionResponse(this, ReturnCode.Transient_Error);
    							break;
    						case Constants.CMD_MODIFY_CONNECTION:
    							response = new ModifyConnectionResponse(this, ReturnCode.Transient_Error);
    							break;
    						case Constants.CMD_NOTIFICATION_REQUEST:
    							response = new NotificationRequestResponse(this, ReturnCode.Transient_Error);
    							break;
    						case Constants.CMD_NOTIFY:
    							response = new NotifyResponse(this, ReturnCode.Transient_Error);
    							break;
    						case Constants.CMD_RESP_UNKNOWN:
    							// FIXME - what response?!?
    							response = new NotifyResponse(this, ReturnCode.Transient_Error);
    							break;
    						case Constants.CMD_RESTART_IN_PROGRESS:
    							response = new RestartInProgressResponse(this, ReturnCode.Transient_Error);
    							break;
    						default:
    							throw new IllegalArgumentException("Could not send type of the message yet");
    						}
    						response.setTransactionHandle(command.getTransactionHandle());
    						JainMgcpEvent[] events = { response };
    						sendMgcpEvents(events);
    						break;
    					case EventWrapper.txTimeout:
    						command=(JainMgcpCommandEvent)currEvent.event;
    						synchronized (jainMobiusListeners) {
    							for (JainMgcpExtendedListener listener : jainMobiusListeners) {
    								listener.transactionTxTimedOut(command);
    							}
    						}
    						break;
    				}
    			}
    			catch(Exception e)
    			{
    				//catch everything, so worker wont die.
    				if(logger.isEnabledFor(Level.ERROR))
    					logger.error("Unexpected exception occured:", e);    				    		
    			}
    		}
    	}    
		
		public void activate() {        	        	
        	this.active = true;
        	this.start();
        }
    	
    	/**
         * Terminates thread.
         */
        private void shutdown() {
            this.active = false;
        }
	}	
}
