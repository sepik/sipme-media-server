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


import ua.mobius.media.MediaSource;
import ua.mobius.media.MediaSink;
import ua.mobius.media.server.scheduler.Task;
import org.apache.log4j.Logger;

/**
 * The base implementation of the Media source.
 * 
 * <code>AbstractSource</code> and <code>AbstractSink</code> are implement general wirring contruct. All media
 * components have to extend one of these classes.
 * 
 * @author Oifa Yulian
 */
public abstract class AbstractSource extends BaseComponent implements MediaSource {

    //transmission statisctics
	protected volatile long txPackets;
	protected volatile long txBytes;
    
    //shows if component is started or not.
	protected volatile boolean started;

    //local media time
    protected volatile long timestamp = 0;
    
    //initial media time
    protected long initialOffset;
    
    //frame sequence number
    protected long sn = 1;

    //duration of media stream in nanoseconds
    protected long duration = -1;

    //intial delay for media processing
    protected long initialDelay = 0;
    
    private static final Logger logger = Logger.getLogger(AbstractSource.class);
    /**
     * Creates new instance of source with specified name.
     * 
     * @param name
     *            the name of the source to be created.
     */
    public AbstractSource(String name) {
        super(name);        
    }

    /**
     * (Non Java-doc.)
     * 
     * @see ua.mobius.media.server.impl.AbstractSource#setInitialDelay(long) 
     */
    public void setInitialDelay(long initialDelay) {
        this.initialDelay = initialDelay;
    }

    
    /**
     * (Non Java-doc).
     * 
     * @see ua.mobius.media.MediaSource#getMediaTime();
     */
    public long getMediaTime() {
        return timestamp;
    }
    
    /**
     * (Non Java-doc).
     * 
     * @see ua.mobius.media.MediaSource#setDuration(long duration);
     */
    public void setDuration(long duration) {
        this.duration = duration;
    }
    
    /**
     * (Non Java-doc).
     * 
     * @see ua.mobius.media.MediaSource#getDuration();
     */
    public long getDuration() {
        return this.duration;
    }
    
    /**
     * (Non Java-doc).
     * 
     * @see ua.mobius.media.MediaSource#setMediaTime(long timestamp);
     */
    public void setMediaTime(long timestamp) {
        this.initialOffset = timestamp;
    }       

    public void activate()
    {
    	start();
    }
    
    public void deactivate()
    {
    	stop();
    }
    
    /**
     * (Non Java-doc).
     * 
     * @see ua.mobius.media.MediaSource#isStarted().
     */
    public boolean isStarted() {
        return this.started;
    }

    /**
     * Sends notification that media processing has been started.
     */
    protected void started() {
    }

    /**
     * Sends failure notification.
     * 
     * @param e the exception caused failure.
     */
    protected void failed(Exception e) {
    }

    /**
     * Sends notification that signal is completed.
     * 
     */
    protected void completed() {
        this.started = false;
    }

    /**
     * Called when source is stopped by request
     * 
     */
    protected void stopped() {
    }

    /**
     * (Non Java-doc).
     * 
     * @see ua.mobius.media.MediaSource#getPacketsReceived()
     */
    public long getPacketsTransmitted() {
    	return txPackets;
    }

    /**
     * (Non Java-doc).
     * 
     * @see ua.mobius.media.MediaSource#getBytesTransmitted()
     */
    public long getBytesTransmitted() {
        return txBytes;
    }

    @Override
    public void reset() {
        this.txPackets = 0;
        this.txBytes = 0;        
    }
    
    public String report() {
        return "";
    }    
}
