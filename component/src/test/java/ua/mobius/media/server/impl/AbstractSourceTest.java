/*
 * 15/07/13 - Change notice:
 * This file has been modified by Mobius Software Ltd.
 * For more information please visit http://www.mobius.ua
 */
package ua.mobius.media.server.impl;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import ua.mobius.media.server.scheduler.Clock;
import ua.mobius.media.server.scheduler.DefaultClock;
import ua.mobius.media.server.scheduler.DefaultScheduler;
import ua.mobius.media.server.scheduler.Scheduler;
import ua.mobius.media.server.spi.format.AudioFormat;
import ua.mobius.media.server.spi.format.FormatFactory;
import ua.mobius.media.server.spi.format.Formats;
import ua.mobius.media.server.spi.memory.ShortFrame;
import ua.mobius.media.server.spi.memory.ShortMemory;

/**
 *
 * @author yulian oifa
 */
public class AbstractSourceTest {

    private final static AudioFormat LINEAR = FormatFactory.createAudioFormat("linear", 8000, 16, 1);
    private final static Formats formats = new Formats();
    static {
        formats.add(LINEAR);
    }
    
    private Clock clock;
    private Scheduler scheduler;
    
    private MyTestSource source;
    private MyTestSink sink;
    
    private Semaphore semaphore = new Semaphore(0);
    
    private long[] timestamp = new long[1000];
    private int count;
    
    public AbstractSourceTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
        clock = new DefaultClock();

        scheduler = new DefaultScheduler();
        scheduler.setClock(clock);
        scheduler.start();
        
        source = new MyTestSource(scheduler);
        sink = new MyTestSink();   
        
        source.connect(sink);
        count = 0;
    }
    
    @After
    public void tearDown() {
    	source.stop();
    	source.disconnect();
        scheduler.stop();    	
    }

    @Test
    public void testDuration() throws InterruptedException {
    	//originaly infinite stream: duration unknown
        assertEquals(-1, source.getDuration());
        
        //apply max duration and check value
        source.setDuration(3000000000L);
        assertEquals(3000000000L, source.getDuration());
        
        //start transmission
        source.start();
        
        //block for 5 seconds max
        long s = System.currentTimeMillis();
        semaphore.tryAcquire(4, TimeUnit.SECONDS);
        
        
        //check results
        long duration = System.currentTimeMillis() - s;
        assertFalse("Source still working", source.isStarted());
        assertEquals(3000, duration, 500);    	
    }
    
    /**
     * Test of setInitialDelay method, of class AbstractSource.
     */
    @Test
    public void testInitialDelay() throws InterruptedException {
        //apply max duration and check value
        source.setDuration(3000000000L);
        source.setInitialDelay(2000000000L);
        assertEquals(3000000000L, source.getDuration());
        
        //start transmission
        source.activate();
        
        //block for 5 seconds max
        long s = System.currentTimeMillis();
        semaphore.tryAcquire(6, TimeUnit.SECONDS);
        
        
        //check results
        long duration = System.currentTimeMillis() - s;
        assertFalse("Source still working", source.isStarted());
        assertEquals(5000, duration, 500);
    }


    /**
     * Test Media time
     */
    @Test
    public void testMediaTime() throws InterruptedException {
        //start transmission
        source.activate();
        
        Thread.sleep(1000);
        
        long time = source.getMediaTime();
        source.deactivate();
        
        Thread.sleep(100);
        
        System.out.println("Time=" + time);
        source.setMediaTime(time);
        source.activate();
        
        Thread.sleep(1000);
        
        assertTrue("Data expected", count > 0);
        for (int i = 0; i < count - 1; i++) {
            assertTrue("Time flows back", timestamp[i+1] - timestamp[i] >= 20000000L);
        }
    }
    
    /**
     * Test of setDsp method, of class AbstractSource.
     */

    public class MyTestSource extends AbstractAudioSource {
        
        private long seq = 0;
        
        public MyTestSource(Scheduler scheduler) {
            super("", scheduler,scheduler.OUTPUT_QUEUE);
        }

        public ShortFrame evolve(long timestamp) {
            ShortFrame frame = ShortMemory.allocate(320);
            frame.setOffset(0);
            frame.setLength(0);
            frame.setSequenceNumber(seq++);
            frame.setFormat(LINEAR);
            frame.setDuration(20000000L);
            
            return frame;
        }        
        
        @Override
        protected void completed() {
            super.completed();
            semaphore.release();
        }
    }
    
    private class MyTestSink extends AbstractAudioSink {
        
        public MyTestSink() {
            super("");
        }

        @Override
        public void onMediaTransfer(ShortFrame frame) throws IOException {
            timestamp[count++] = frame.getTimestamp();
        }
        
        public void deactivate()
        {
        	
        }
        
        public void activate()
        {
        	
        }
    }
}
