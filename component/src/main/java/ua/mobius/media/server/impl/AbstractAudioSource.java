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
package ua.mobius.media.server.impl;


import ua.mobius.media.AudioSink;
import ua.mobius.media.server.scheduler.Scheduler;
import ua.mobius.media.server.scheduler.Task;
import ua.mobius.media.server.spi.memory.ShortFrame;
import org.apache.log4j.Logger;

/**
 * The base implementation of the Media source.
 * 
 * <code>AbstractSource</code> and <code>AbstractSink</code> are implement general wirring contruct. All media
 * components have to extend one of these classes.
 * 
 * @author Oifa Yulian
 */
public abstract class AbstractAudioSource extends AbstractSource {
	//stream synchronization flag
    private volatile boolean isSynchronized;

    //media generator
    private final Worker worker;

    //media transmission pipe
    protected AudioSink mediaSink;        

    private Scheduler scheduler;
    
    private static final Logger logger = Logger.getLogger(AbstractSource.class);
    /**
     * Creates new instance of source with specified name.
     * 
     * @param name
     *            the name of the source to be created.
     */
    public AbstractAudioSource(String name, Scheduler scheduler,int queueNumber) {
        super(name);
        this.scheduler = scheduler;
        this.worker = new Worker(queueNumber);        
    }          

    /**
     * (Non Java-doc).
     * 
     * @see ua.mobius.media.MediaSource#start().
     */
    public void start() {
    	synchronized(worker) {
    		//check scheduler
    		try {
    			//prevent duplicate starting
    			if (started) {
    				return;
    			}

    			if (scheduler == null) {
    				throw new IllegalArgumentException("Scheduler is not assigned");
    			}

    			this.txBytes = 0;
    			this.txPackets = 0;
            
    			//reset media time and sequence number
    			timestamp = this.initialOffset;
    			this.initialOffset = 0;
            
    			sn = 0;

    			//switch indicator that source has been started
    			started = true;

    			//just started component always synchronized as well
    			this.isSynchronized = true;
    			
    			if(mediaSink!=null)
    				mediaSink.start();
    			
    			//scheduler worker    
    			worker.reinit();
    			scheduler.submit(worker,worker.getQueueNumber());
    			
    			//started!
    			started();
    		} catch (Exception e) {
    			started = false;
    			failed(e);
    			logger.error(e);
    		}
    	}
    }

    /**
     * Restores synchronization
     */
    public void wakeup() {    	
        synchronized(worker) {
            if (!started) {            	
                return;
            }
            
            if (!this.isSynchronized) {
                this.isSynchronized = true;
                scheduler.submit(worker,worker.getQueueNumber());
            }
        }
    }
    
    /**
     * (Non Java-doc).
     * 
     * @see ua.mobius.media.MediaSource#stop().
     */
    public void stop() {
        if (started) {
            stopped();
        }
        started = false;
        if (worker != null) {
            worker.cancel();
        }
        
        if(mediaSink!=null) {
        	mediaSink.stop();
        }
        	
        timestamp = 0;
    }    
    
    /**
     * (Non Java-doc).
     *
     * @see ua.mobius.media.MediaSource#connect(ua.mobius.media.MediaSink)
     */
    protected void connect(AudioSink sink) {
        this.mediaSink = sink;
        if(started)
        	this.mediaSink.start();
    }

    /**
     * (Non Java-doc).
     *
     * @see ua.mobius.media.MediaSource#disconnect(ua.mobius.media.server.spi.io.Pipe)
     */
    protected void disconnect() {
    	if(this.mediaSink!=null)
    	{
    		this.mediaSink.stop();
    		this.mediaSink=null;
    	}
    }

    /**
     * (Non Java-doc).
     * 
     * @see ua.mobius.media.MediaSink#isConnected().
     */
    public boolean isConnected() {
        return mediaSink != null;
    }    

    /**
     * This method must be overriden by concrete media source. T
     * he media have to fill buffer with media data and
     * attributes.
     * 
     * @param buffer the buffer object for media.
     * @param sequenceNumber
     *            the number of timer ticks from the begining.
     */
    public abstract ShortFrame evolve(long timestamp);

    /**
     * Media generator task
     */
    private class Worker extends Task {
    	/**
         * Creates new instance of task.
         *
         * @param scheduler the scheduler instance.
         */
    	private int queueNumber;    	
    	private long initialTime;
    	int readCount=0,length;
    	long overallDelay=0;
    	ShortFrame frame;
    	long frameDuration;
    	Boolean isEOM;
    	
    	public Worker(int queueNumber) {
            super();
            this.queueNumber=queueNumber;
            initialTime=scheduler.getClock().getTime();            
        }
        
        public void reinit()
        {
        	initialTime=scheduler.getClock().getTime();        	
        }
        
        public int getQueueNumber()
        {
        	return queueNumber;
        }

        /**
         * (Non Java-doc.)
         *
         * @see ua.mobius.media.server.scheduler.Task#perform()
         */
        public long perform() {
        	if(initialDelay+initialTime>scheduler.getClock().getTime())
        	{
        		//not a time yet
        		scheduler.submit(this,queueNumber);
                return 0;
        	}
        	
        	readCount=0;
        	overallDelay=0;
        	while(overallDelay<20000000L)
        	{
        		readCount++;
        		frame = evolve(timestamp);
        		if (frame == null) {
        			if(readCount==1)
        			{     
        				//stop if frame was not generated
        				isSynchronized = false;
        				return 0;
        			}
        			else
        			{
        				//frame was generated so continue
        				scheduler.submit(this,queueNumber);
        	            return 0;
        			}
            	}

            	//mark frame with media time and sequence number
            	frame.setTimestamp(timestamp);
            	frame.setSequenceNumber(sn);

            	//update media time and sequence number for the next frame
            	timestamp += frame.getDuration();
            	overallDelay += frame.getDuration();
            	sn= (sn==Long.MAX_VALUE) ? 0: sn+1;

            	//set end_of_media flag if stream has reached the end
            	if (duration > 0 && timestamp >= duration) {
            		frame.setEOM(true);
            	}

            	frameDuration = frame.getDuration();            	            	            	          
            	isEOM=frame.isEOM();
            	length=frame.getLength();
            	
            	//delivering data to the other party.
            	if (mediaSink != null) {
            		mediaSink.perform(frame);
            	}
            	
            	//update transmission statistics
            	txPackets++;
            	txBytes += length;
            
            	//send notifications about media termination
            	//and do not resubmit this task again if stream has bee ended
            	if (isEOM) { 
            		started=false;
        			completed();
            		return -1;
            	}

            	//check synchronization
            	if (frameDuration <= 0) {
            		//los of synchronization
                	isSynchronized = false;
                	return 0;
            	}            
        	}
        	
        	scheduler.submit(this,queueNumber);
            return 0;
        }

        @Override
        public String toString() {
            return getName();
        }

    }
}