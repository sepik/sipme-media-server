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
package ua.mobius.media.server.mgcp.pkg.ann;

import java.util.ArrayList;
import java.net.MalformedURLException;

import org.apache.log4j.Logger;
import ua.mobius.media.ComponentType;
import ua.mobius.media.server.mgcp.controller.signal.Event;
import ua.mobius.media.server.mgcp.controller.signal.NotifyImmediately;
import ua.mobius.media.server.mgcp.controller.signal.Signal;
import ua.mobius.media.server.spi.Connection;
import ua.mobius.media.server.spi.Endpoint;
import ua.mobius.media.server.spi.MediaType;
import ua.mobius.media.server.spi.player.Player;
import ua.mobius.media.server.spi.player.PlayerEvent;
import ua.mobius.media.server.spi.player.PlayerListener;
import ua.mobius.media.server.spi.listener.TooManyListenersException;
import ua.mobius.media.server.spi.ResourceUnavailableException;
import ua.mobius.media.server.utils.Text;

/**
 * Implements play announcement signal.
 * 
 * @author yulian oifa
 */
public class Play extends Signal implements PlayerListener {
    
	private Event oc = new Event(new Text("oc"));
    private Event of = new Event(new Text("of"));
    
    private Player player;
    private String uri;
    
    private final static Logger logger = Logger.getLogger(Play.class);
    
    public Play(String name) {
        super(name);
        oc.add(new NotifyImmediately("N"));
        of.add(new NotifyImmediately("N"));
    }
    
    @Override
    public void execute() {
    	logger.info("Executing...");
        
        player = this.getPlayer();
        
        try {
            player.addListener(this);
            
          //get options of the request
            this.uri =getTrigger().getParams().toString();
        
            player.setURL(uri);
            logger.info("Assigned url " + player);
        }
        catch (TooManyListenersException e) {
        	of.fire(this, null);    
            this.complete();                       
            logger.error("OPERATION FAILURE", e);
            return;
        } 
        catch (MalformedURLException e) {
        	logger.info("Received URL in invalid format , firing of");
        	of.fire(this, null);    
            this.complete();           
            return;
        } catch (ResourceUnavailableException e) {
        	logger.info("Received URL can not be found , firing of");
        	of.fire(this, null);    
            this.complete();
            return;
        } 
        
        player.activate();
    }

    @Override
    public boolean doAccept(Text event) {
    	if (!oc.isActive() && oc.matches(event)) {
            return true;
        }

        if (!of.isActive() && of.matches(event)) {
            return true;
        }
        
        return false;
    }

    @Override
    public void cancel() {
    	terminate();
    }

    private Player getPlayer() {
    	Endpoint endpoint = getEndpoint();
        return (Player) getEndpoint().getResource(MediaType.AUDIO, ComponentType.PLAYER);
    }
    
    private void terminate()
    {
    	if (player != null) {
            player.removeListener(this);
            player.deactivate();
            player=null;
        } 
    }
    
    @Override
    public void reset() {
        super.reset();
        terminate();
        
        oc.reset();
        of.reset();
    }
    
    public void process(PlayerEvent event) {
        switch (event.getID()) {
            case PlayerEvent.STOP :
            	terminate();
            	oc.fire(this, null);
                this.complete();
                break;
            case PlayerEvent.FAILED :
            	terminate();
            	of.fire(this, null);
                this.complete();
                break;
        }
    }
}
