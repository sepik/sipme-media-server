/*
 * 15/07/13 - Change notice:
 * This file has been modified by Mobius Software Ltd.
 * For more information please visit http://www.mobius.ua
 */
package ua.mobius.media.server.mgcp.tx.cmd;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import ua.mobius.media.server.io.network.UdpManager;
import ua.mobius.media.server.mgcp.MgcpEvent;
import ua.mobius.media.server.mgcp.MgcpProvider;
import ua.mobius.media.server.mgcp.message.MgcpRequest;
import ua.mobius.media.server.mgcp.tx.Action;
import ua.mobius.media.server.scheduler.DefaultClock;
import ua.mobius.media.server.scheduler.DefaultScheduler;
import ua.mobius.media.server.scheduler.Scheduler;
import ua.mobius.media.server.utils.Text;

/**
 *
 * @author kulikov
 */
public class ActionSelectorTest {
    
    private DefaultClock clock;
    private Scheduler scheduler;
    
    private ActionSelector selector;
    private MgcpProvider mgcpProvider;
    
    private UdpManager udpInterface;
    private SocketAddress address;
    
    public ActionSelectorTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() throws IOException {
        clock = new DefaultClock();
        
        scheduler = new DefaultScheduler();
        scheduler.setClock(clock);
        scheduler.start();
        
        udpInterface = new UdpManager(scheduler);
        udpInterface.setBindAddress("localhost");
        udpInterface.start();
        
        mgcpProvider = new MgcpProvider(udpInterface, 1024, scheduler);
        address = new InetSocketAddress("localhost", 2425);
        
        selector = new ActionSelector(scheduler);
    }
    
    @After
    public void tearDown() {
        mgcpProvider.shutdown();
        scheduler.stop();
    }

    /**
     * Test of getAction method, of class ActionSelector.
     */
    @Test
    public void testCrcx() {
        MgcpEvent evt = (MgcpEvent) mgcpProvider.createEvent(MgcpEvent.REQUEST, address);
        MgcpRequest msg = (MgcpRequest) evt.getMessage();
        
        msg.setCommand(new Text("CRCX"));
        msg.setEndpoint(new Text("test@localhost:2427"));
        msg.setTxID(1);
        
        Action action = selector.getAction(evt);
        assertTrue("Unexpected action", action instanceof CreateConnectionCmd);
        
    }
    
    @Test
    public void testMdxc() {
        MgcpEvent evt = (MgcpEvent) mgcpProvider.createEvent(MgcpEvent.REQUEST, address);
        MgcpRequest msg = (MgcpRequest) evt.getMessage();
        
        msg.setCommand(new Text("MDCX"));
        msg.setEndpoint(new Text("test@localhost:2427"));
        msg.setTxID(1);
        
        Action action = selector.getAction(evt);
        assertTrue("Unexpected action", action instanceof ModifyConnectionCmd);        
    }
    
    @Test
    public void testDlcx() {
        MgcpEvent evt = (MgcpEvent) mgcpProvider.createEvent(MgcpEvent.REQUEST, address);
        MgcpRequest msg = (MgcpRequest) evt.getMessage();
        
        msg.setCommand(new Text("DLCX"));
        msg.setEndpoint(new Text("test@localhost:2427"));
        msg.setTxID(1);
        
        Action action = selector.getAction(evt);
        assertTrue("Unexpected action", action instanceof DeleteConnectionCmd);        
    }
    
    @Test
    public void testRqnt() {
        MgcpEvent evt = (MgcpEvent) mgcpProvider.createEvent(MgcpEvent.REQUEST, address);
        MgcpRequest msg = (MgcpRequest) evt.getMessage();
        
        msg.setCommand(new Text("RQNT"));
        msg.setEndpoint(new Text("test@localhost:2427"));
        msg.setTxID(1);
        
        Action action = selector.getAction(evt);
        assertTrue("Unexpected action", action instanceof NotificationRequestCmd);        
    }

    @Test
    public void testNtfy() {
        MgcpEvent evt = (MgcpEvent) mgcpProvider.createEvent(MgcpEvent.REQUEST, address);
        MgcpRequest msg = (MgcpRequest) evt.getMessage();
        
        msg.setCommand(new Text("NTFY"));
        msg.setEndpoint(new Text("test@localhost:2427"));
        msg.setTxID(1);
        
        Action action = selector.getAction(evt);
        assertTrue("Unexpected action", action instanceof NotifyCmd);        
    }
    
    
}
