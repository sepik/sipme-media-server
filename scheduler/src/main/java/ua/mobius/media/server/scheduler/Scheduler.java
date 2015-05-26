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
package ua.mobius.media.server.scheduler;

/**
 * Implements scheduler with multi-level priority queue.
 *
 * This scheduler implementation follows to uniprocessor model with "super" thread.
 * The "super" thread includes IO bound thread and one or more CPU bound threads
 * with equal priorities.
 *
 * The actual priority is assigned to task instead of process and can be
 * changed dynamically at runtime using the initial priority level, feedback
 * and other parameters.
 *
 *
 * @author Oifa Yulian
 */
public interface Scheduler  {
    //SS7 QUEUES
    public static final Integer RECEIVER_QUEUE=0;
    public static final Integer SENDER_QUEUE=1;

    //MANAGEMENT QUEUE FOR CONTROL PROCESSING
    public static final Integer MANAGEMENT_QUEUE=2;

    //UDP MANAGER QUEUE FOR POOLING CHANNELS
    public static final Integer UDP_MANAGER_QUEUE=3;

    //CORE QUEUES
    public static final Integer INPUT_QUEUE=4;
    public static final Integer MIXER_MIX_QUEUE=5;
    public static final Integer OUTPUT_QUEUE=6;

    //HEARTBEAT QUEUE
    public static final Integer HEARTBEAT_QUEUE=-1;

    public int getPoolSize();

    /**
     * Sets clock.
     *
     * @param clock the clock used for time measurement.
     */
    public void setClock(Clock clock);

    /**
     * Gets the clock used by this scheduler.
     *
     * @return the clock object.
     */
    public Clock getClock();

    /**
     * Queues task for execution according to its priority.
     *
     * @param task the task to be executed.
     */
    public void submit(Task task,Integer index);

    /**
     * Queues task for execution according to its priority.
     *
     * @param task the task to be executed.
     */
    public void submitHeatbeat(Task task);

    /**
     * Queues chain of the tasks for execution.
     *
     * @param taskChain the chain of the tasks
     */
    public void submit(TaskChain taskChain);

    /**
     * Starts scheduler.
     */
    public void start();

    /**
     * Stops scheduler.
     */
    public void stop();
}
