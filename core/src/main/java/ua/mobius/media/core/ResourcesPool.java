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
package ua.mobius.media.core;

import org.apache.log4j.Logger;
import java.util.concurrent.atomic.AtomicInteger;

import ua.mobius.media.Component;
import ua.mobius.media.ComponentFactory;
import ua.mobius.media.ComponentType;
import ua.mobius.media.server.spi.Connection;
import ua.mobius.media.server.spi.ResourceUnavailableException;
import ua.mobius.media.server.spi.dsp.DspFactory;

import ua.mobius.media.server.scheduler.Scheduler;
import java.util.concurrent.ConcurrentLinkedQueue;

import ua.mobius.media.server.impl.rtp.ChannelsManager;

import ua.mobius.media.core.connections.RtpConnectionImpl;
import ua.mobius.media.core.connections.LocalConnectionImpl;

import ua.mobius.media.server.impl.resource.dtmf.DetectorImpl;
import ua.mobius.media.server.impl.resource.dtmf.GeneratorImpl;
import ua.mobius.media.server.impl.resource.phone.PhoneSignalGenerator;
import ua.mobius.media.server.impl.resource.phone.PhoneSignalDetector;
import ua.mobius.media.server.impl.resource.audio.AudioRecorderImpl;
import ua.mobius.media.server.impl.resource.mediaplayer.audio.AudioPlayerImpl;
/**
 * Implements connection's FSM.
 *
 * @author Oifa Yulian
 */
public class ResourcesPool implements ComponentFactory {

	//local arrays
    //getters , setters
	
	private Scheduler scheduler;
	private ChannelsManager channelsManager;
	private DspFactory dspFactory;
	
	private ConcurrentLinkedQueue<Component> players;
	private ConcurrentLinkedQueue<Component> recorders;
	private ConcurrentLinkedQueue<Component> dtmfDetectors;
	private ConcurrentLinkedQueue<Component> dtmfGenerators;
	private ConcurrentLinkedQueue<Component> signalDetectors;
	private ConcurrentLinkedQueue<Component> signalGenerators;
	
	private int defaultPlayers;
	private int defaultRecorders;
	private int defaultDtmfDetectors;
	private int defaultDtmfGenerators;
	private int defaultSignalDetectors;
	private int defaultSignalGenerators;
	
	private int dtmfDetectorDbi=-35;
	
	private ConcurrentLinkedQueue<Connection> localConnections;
	private ConcurrentLinkedQueue<Connection> remoteConnections;
	
	private int defaultLocalConnections;
	private int defaultRemoteConnections;
	
	private AtomicInteger connectionId=new AtomicInteger(1);
	
	public  Logger logger = Logger.getLogger(ResourcesPool.class);
	
	private AtomicInteger localConnectionsCount=new AtomicInteger(0);
	private AtomicInteger rtpConnectionsCount=new AtomicInteger(0);
	
	private AtomicInteger playersCount=new AtomicInteger(0);
	private AtomicInteger recordersCount=new AtomicInteger(0);
	private AtomicInteger dtmfDetectorsCount=new AtomicInteger(0);
	private AtomicInteger dtmfGeneratorsCount=new AtomicInteger(0);
	private AtomicInteger signalDetectorsCount=new AtomicInteger(0);
	private AtomicInteger signalGeneratorsCount=new AtomicInteger(0);
	
	private Boolean growEndDuration=false;
	private Integer oobToneDuration=140;
	private Integer oobEndTonePackets=3;
	
	private int initialAudioChannelBuffer=3;
	private int maxAudioChannelBuffer=3;
	
	public ResourcesPool(Scheduler scheduler,ChannelsManager channelsManager,DspFactory dspFactory)
	{
		this.scheduler=scheduler;
		this.channelsManager=channelsManager;
		this.dspFactory=dspFactory;
		
		players=new ConcurrentLinkedQueue<Component>();
		recorders=new ConcurrentLinkedQueue<Component>();
		dtmfDetectors=new ConcurrentLinkedQueue<Component>();
		dtmfGenerators=new ConcurrentLinkedQueue<Component>();
		signalDetectors=new ConcurrentLinkedQueue<Component>();
		signalGenerators=new ConcurrentLinkedQueue<Component>();
		localConnections=new ConcurrentLinkedQueue<Connection>();
		remoteConnections=new ConcurrentLinkedQueue<Connection>();
	}
	
	public DspFactory getDspFactory()
	{
		return dspFactory;
	}
	
	public void setDefaultPlayers(int value)
	{
		this.defaultPlayers=value;
	}
	
	public void setDefaultRecorders(int value)
	{
		this.defaultRecorders=value;
	}
	
	public void setDefaultDtmfDetectors(int value)
	{
		this.defaultDtmfDetectors=value;
	}
	
	public void setDefaultDtmfGenerators(int value)
	{
		this.defaultDtmfGenerators=value;
	}
	
	public void setDefaultSignalDetectors(int value)
	{
		this.defaultSignalDetectors=value;
	}
	
	public void setDefaultSignalGenerators(int value)
	{
		this.defaultSignalGenerators=value;
	}
	
	public void setDefaultLocalConnections(int value)
	{
		this.defaultLocalConnections=value;
	}
	
	public void setDefaultRemoteConnections(int value)
	{
		this.defaultRemoteConnections=value;
	}
	
	public void setDtmfDetectorDbi(int value)
	{
		this.dtmfDetectorDbi=value;
	}
	
	public void setOobToneDuration(int value)
	{
		this.oobToneDuration=value;
	}
	
	public void setOobEndTonePackets(int value)
	{
		this.oobEndTonePackets=value;
	}
	
	public void setGrowEndDuration(Boolean value)
	{
		this.growEndDuration=value;
	}
	
	public void setInitialAudioChannelBuffer(int value)
	{
		this.initialAudioChannelBuffer=value;
	}
	
	public void setMaxAudioChannelBuffer(int value)
	{
		this.maxAudioChannelBuffer=value;
	}
	
	public void start()
	{
		for(int i=0;i<defaultPlayers;i++)
		{
			AudioPlayerImpl player=new AudioPlayerImpl("player",scheduler);
			
			try {
				player.setDsp(dspFactory.newAudioProcessor());
			}
			catch(Exception ex) {				
			}
			
			players.offer(player);
		}
		
		playersCount.set(defaultPlayers);
		
		for(int i=0;i<defaultRecorders;i++)
			recorders.offer(new AudioRecorderImpl(scheduler));
		
		recordersCount.set(defaultRecorders);
		
		for(int i=0;i<defaultDtmfDetectors;i++)
		{
			DetectorImpl detector=new DetectorImpl("detector",scheduler);
			detector.setVolume(dtmfDetectorDbi);	        
			dtmfDetectors.offer(detector);
		}
		
		dtmfDetectorsCount.set(defaultDtmfDetectors);
				
		for(int i=0;i<defaultDtmfGenerators;i++)
		{
			GeneratorImpl generator=new GeneratorImpl("generator",scheduler,growEndDuration,oobEndTonePackets);
			dtmfGenerators.offer(generator);
			generator.setToneDuration(100);
	        generator.setVolume(-20);
		}
		
		dtmfGeneratorsCount.set(defaultDtmfGenerators);
		
		for(int i=0;i<defaultSignalDetectors;i++)
			signalDetectors.offer(new PhoneSignalDetector("signal detector",scheduler));

		signalDetectorsCount.set(defaultSignalDetectors);
		
		for(int i=0;i<defaultSignalGenerators;i++)
			signalGenerators.offer(new PhoneSignalGenerator("signal generator",scheduler));
		
		signalGeneratorsCount.set(defaultSignalGenerators);
		
		for(int i=0;i<defaultLocalConnections;i++)
			localConnections.offer(new LocalConnectionImpl(connectionId.incrementAndGet(),channelsManager));

		localConnectionsCount.set(defaultLocalConnections);
		
		for(int i=0;i<defaultRemoteConnections;i++)
		{
			RtpConnectionImpl current=new RtpConnectionImpl(connectionId.incrementAndGet(),channelsManager,dspFactory,oobToneDuration,oobEndTonePackets,growEndDuration);
			current.setInitialAudioChannelBuffer(initialAudioChannelBuffer);
			current.setMaxAudioChannelBuffer(maxAudioChannelBuffer);
			remoteConnections.offer(current);
			
		}
		
		rtpConnectionsCount.set(defaultRemoteConnections);				
	}
	
	public Component newAudioComponent(ComponentType componentType)
	{
		Component result=null;
		switch(componentType)
		{
			case DTMF_DETECTOR:
				result=dtmfDetectors.poll();
				if(result==null)
				{
					result=new DetectorImpl("detector",scheduler);
					((DetectorImpl)result).setVolume(dtmfDetectorDbi);	        
					dtmfDetectorsCount.incrementAndGet();
				}
 
				if(logger.isDebugEnabled())				
					logger.debug("Allocated new dtmf detector,pool size:" + dtmfDetectorsCount.get() + ",free:" + dtmfDetectors.size());				
				break;
			case DTMF_GENERATOR:
				result=dtmfGenerators.poll();
				if(result==null)
				{
					result=new GeneratorImpl("generator",scheduler,growEndDuration,oobEndTonePackets);
					((GeneratorImpl)result).setToneDuration(80);
					((GeneratorImpl)result).setVolume(-20);
					dtmfGeneratorsCount.incrementAndGet();
				}
				
				if(logger.isDebugEnabled())				
					logger.debug("Allocated new dtmf generator,pool size:" + dtmfGeneratorsCount.get() + ",free:" + dtmfDetectors.size());				
				break;
			case PLAYER:
				result=players.poll();
				if(result==null)
				{
					result=new AudioPlayerImpl("player",scheduler);
					
					try {
						((AudioPlayerImpl)result).setDsp(dspFactory.newAudioProcessor());
					}
					catch(Exception ex) {
					}			
					
					playersCount.incrementAndGet();
				}
				
				if(logger.isDebugEnabled())				
					logger.debug("Allocated new player,pool size:" + playersCount.get() + ",free:" + players.size());
				break;
			case RECORDER:
				result=recorders.poll();
				if(result==null)
				{
					result=new AudioRecorderImpl(scheduler);
					recordersCount.incrementAndGet();
				}
				
				if(logger.isDebugEnabled())				
					logger.debug("Allocated new recorder,pool size:" + recordersCount.get() + ",free:" + recorders.size());				
				break;
			case SIGNAL_DETECTOR:
				result=signalDetectors.poll();
				if(result==null)
				{
					result=new PhoneSignalDetector("signal detector",scheduler);
					signalDetectorsCount.incrementAndGet();
				}
				
				if(logger.isDebugEnabled())				
					logger.debug("Allocated new signal detector,pool size:" + signalDetectorsCount.get() + ",free:" + signalDetectors.size());				
				break;
			case SIGNAL_GENERATOR:
				result=signalGenerators.poll();
				if(result==null)
				{
					result=new PhoneSignalGenerator("signal generator",scheduler);
					signalGeneratorsCount.incrementAndGet();
				}
				
				if(logger.isDebugEnabled())				
					logger.debug("Allocated new signal generator,pool size:" + signalGeneratorsCount.get() + ",free:" + signalGenerators.size());				
				break;
		}
		
		return result;
	}
    
	public void releaseAudioComponent(Component component,ComponentType componentType)
	{
		switch(componentType)
		{
			case DTMF_DETECTOR:
				dtmfDetectors.offer(component);
				
				if(logger.isDebugEnabled())				
					logger.debug("Released dtmf detector,pool size:" + dtmfDetectorsCount.get() + ",free:" + dtmfDetectors.size());				
				break;
			case DTMF_GENERATOR:
				dtmfGenerators.offer(component);
				
				if(logger.isDebugEnabled())				
					logger.debug("Released dtmf generator,pool size:" + dtmfGeneratorsCount.get() + ",free:" + dtmfGenerators.size());				
				break;
			case PLAYER:
				players.offer(component);
				
				if(logger.isDebugEnabled())				
					logger.debug("Released player,pool size:" + playersCount.get() + ",free:" + players.size());				
				break;
			case RECORDER:
				recorders.offer(component);
				
				if(logger.isDebugEnabled())				
					logger.debug("Released recorder,pool size:" + recordersCount.get() + ",free:" + recorders.size());				
				break;
			case SIGNAL_DETECTOR:
				signalDetectors.offer(component);
				
				if(logger.isDebugEnabled())				
					logger.debug("Released signal detector,pool size:" + signalDetectorsCount.get() + ",free:" + signalDetectors.size());				
				break;
			case SIGNAL_GENERATOR:
				signalGenerators.offer(component);
				
				if(logger.isDebugEnabled())				
					logger.debug("Released signal generator,pool size:" + signalGeneratorsCount.get() + ",free:" + signalGenerators.size());				
				break;
		}
	}
    
	public Connection newConnection(boolean isLocal)
	{
		Connection result=null;
		if(isLocal)
		{
			result=localConnections.poll();
			if(result==null)
			{
				result=new LocalConnectionImpl(connectionId.incrementAndGet(),channelsManager);
				localConnectionsCount.incrementAndGet();
			}
			
			if(logger.isDebugEnabled())				
				logger.debug("Allocated new local connection,pool size:" + localConnectionsCount.get() + ",free:" + localConnections.size());							
		}
		else
		{
			result=remoteConnections.poll();
			if(result==null)
			{
				result=new RtpConnectionImpl(connectionId.incrementAndGet(),channelsManager,dspFactory,oobToneDuration,oobEndTonePackets,growEndDuration);
				((RtpConnectionImpl)result).setInitialAudioChannelBuffer(initialAudioChannelBuffer);
				((RtpConnectionImpl)result).setMaxAudioChannelBuffer(maxAudioChannelBuffer);
				
				rtpConnectionsCount.incrementAndGet();
			}
			
			if(logger.isDebugEnabled())				
				logger.debug("Allocated new rtp connection,pool size:" + rtpConnectionsCount.get() + ",free:" + remoteConnections.size());
		}
		
		return result;
	}
    
	public void releaseConnection(Connection connection,boolean isLocal)
	{
		if(isLocal)
		{
			localConnections.offer(connection);
			
			if(logger.isDebugEnabled())				
				logger.debug("Released local connection,pool size:" + localConnectionsCount.get() + ",free:" + localConnections.size());
		}
		else
		{
			remoteConnections.offer(connection);
			
			if(logger.isDebugEnabled())				
				logger.debug("Released rtp connection,pool size:" + rtpConnectionsCount.get() + ",free:" + remoteConnections.size());
		}
	}
}
