/*
 * 15/07/13 - Change notice:
 * This file has been modified by Mobius Software Ltd.
 * For more information please visit http://www.mobius.ua
 */
package ua.mobius.media.server.mgcp.controller;

import java.io.IOException;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import ua.mobius.media.Component;
import ua.mobius.media.ComponentType;
import ua.mobius.media.server.spi.Connection;
import ua.mobius.media.server.spi.ConnectionType;
import ua.mobius.media.server.spi.ConnectionMode;
import ua.mobius.media.server.spi.Endpoint;
import ua.mobius.media.server.spi.EndpointState;
import ua.mobius.media.server.spi.MediaType;
import ua.mobius.media.server.spi.ResourceUnavailableException;
import ua.mobius.media.server.spi.TooManyConnectionsException;
import ua.mobius.media.server.spi.dsp.DspFactory;

import ua.mobius.media.server.scheduler.Scheduler;
import ua.mobius.media.server.scheduler.Clock;
import ua.mobius.media.server.scheduler.DefaultClock;

/**
 *
 * @author yulian oifa
 */
public class ConfiguratorTest {
    
    private Configurator configurator;
    private Scheduler scheduler;
    private Clock clock;
    MyEndpoint endpoint;
    
    public ConfiguratorTest() {    	
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() throws Exception {
    	clock = new DefaultClock();
    	
    	scheduler = new Scheduler();
        scheduler.setClock(clock);
        scheduler.start();  
        
        configurator = new Configurator(getClass().getResourceAsStream("/mgcp-conf.xml"));
        endpoint = new MyEndpoint("mobius/ivr/1",scheduler);
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of activate method, of class Configurator.
     */
    @Test
    public void testActivate() throws Exception {      
    	MgcpEndpoint activity = configurator.activate(endpoint, null, "127.0.0.1", 9201);
        assertTrue(activity != null);    	    	
    }
    
    private class MyEndpoint implements Endpoint {

        private String name;
        private Scheduler scheduler;
        
        public MyEndpoint(String name,Scheduler scheduler) {
            this.name = name;
            this.scheduler=scheduler;
        }
        
        public String getLocalName() {
            return name;
        }

        public void setScheduler(Scheduler scheduler) {
        	throw new UnsupportedOperationException("Not supported yet.");
        }
        
        public Scheduler getScheduler() {
            return scheduler;
        }
        
        public int getActiveConnectionsCount()
        {
        	return 0;
        }
        
        public EndpointState getState() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Collection<MediaType> getMediaTypes() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void start() throws ResourceUnavailableException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void stop() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Connection createConnection(ConnectionType type,Boolean isLocal) throws ResourceUnavailableException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void deleteConnection(Connection connection) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
        public void deleteConnection(Connection connection,ConnectionType type) {
            
        }

        public void deleteAllConnections() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void configure(boolean isALaw) {
        	throw new UnsupportedOperationException("Not supported yet.");
        }
        
        public String describe(MediaType mediaType) throws ResourceUnavailableException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void setDspFactory(DspFactory dspFactory) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void modeUpdated(ConnectionMode oldMode,ConnectionMode newMode) {    
        }
        
        public Component getResource(MediaType mediaType, ComponentType componentType) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    
        public boolean hasResource(MediaType mediaType, ComponentType componentType) {
        	return false;
        }
        
        public void releaseResource(MediaType mediaType, ComponentType componentType) {    
        }
    }
}
