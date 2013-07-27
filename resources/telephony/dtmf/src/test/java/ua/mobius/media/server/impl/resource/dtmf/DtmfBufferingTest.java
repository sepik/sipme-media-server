/*
 * 15/07/13 - Change notice:
 * This file has been modified by Mobius Software Ltd.
 * For more information please visit http://www.mobius.ua
 */
package ua.mobius.media.server.impl.resource.dtmf;

import java.io.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import ua.mobius.media.server.spi.memory.ShortFrame;
import ua.mobius.media.server.spi.memory.ByteFrame;
import ua.mobius.media.server.component.audio.AudioComponent;
import ua.mobius.media.server.component.audio.AudioMixer;

import ua.mobius.media.server.component.oob.OOBComponent;
import ua.mobius.media.server.component.oob.OOBMixer;

import ua.mobius.media.server.scheduler.Clock;
import ua.mobius.media.server.scheduler.DefaultClock;
import ua.mobius.media.server.scheduler.Scheduler;
import ua.mobius.media.server.spi.dtmf.DtmfDetectorListener;
import ua.mobius.media.server.spi.dtmf.DtmfEvent;
import ua.mobius.media.server.spi.listener.TooManyListenersException;

/**
 *
 * @author yulian oifa
 */
public class DtmfBufferingTest implements DtmfDetectorListener {
    
    private Clock clock;
    private Scheduler scheduler;
    
    private DetectorImpl detector;
    private GeneratorImpl generator;
    
    private AudioComponent detectorComponent;
    private AudioComponent generatorComponent;
    private AudioMixer audioMixer;
    
    private OOBComponent oobDetectorComponent;
    private OOBComponent oobGeneratorComponent;
    private OOBMixer oobMixer;
    
    private String tone;
    
    public DtmfBufferingTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() throws TooManyListenersException {
        clock = new DefaultClock();

        scheduler = new Scheduler();
        scheduler.setClock(clock);
        scheduler.start();
        
        generator = new GeneratorImpl("dtmf", scheduler,false,3);
        generator.setToneDuration(100);
        generator.setVolume(-20);
        
        detector = new DetectorImpl("dtmf", scheduler);
        detector.setVolume(-35);
        detector.setDuration(40);
        
        audioMixer=new AudioMixer(scheduler);
        
        detectorComponent=new AudioComponent(1);
        detectorComponent.addOutput(detector.getAudioOutput());
        detectorComponent.updateMode(false,true);
        
        generatorComponent=new AudioComponent(2);
        generatorComponent.addInput(generator.getAudioInput());
        generatorComponent.updateMode(true,false);
                
        audioMixer.addComponent(detectorComponent);
        audioMixer.addComponent(generatorComponent);
        
        oobMixer=new OOBMixer(scheduler);
        
        oobDetectorComponent=new OOBComponent(1);
        oobDetectorComponent.addOutput(detector.getOOBOutput());
        oobDetectorComponent.updateMode(false,true);
        
        oobGeneratorComponent=new OOBComponent(2);
        oobGeneratorComponent.addInput(generator.getOOBInput());
        oobGeneratorComponent.updateMode(true,false);
        
        oobMixer.addComponent(oobDetectorComponent);
        oobMixer.addComponent(oobGeneratorComponent);
        
        tone="";
    }
    
    @After
    public void tearDown() {
    	generator.deactivate();
    	detector.deactivate();
    	audioMixer.stop();
        scheduler.stop();
    }

    /**
     * Test of setDuration method, of class DetectorImpl.
     */
    @Test
    public void testFlush() throws InterruptedException, TooManyListenersException {
        //queue "1" into detector's buffer
        generator.setDigit("1");
        generator.activate();
        detector.activate();
    	audioMixer.start();
        
        Thread.sleep(200);        

        generator.deactivate();
        detector.deactivate();
    	audioMixer.stop();
        
        //assign listener and flush digit
        detector.addListener(this);
        detector.flushBuffer();

        //wait a bit for delivery
        Thread.sleep(300);        
        
        assertEquals("1", tone);
        
        tone="";
        detector.removeListener(this);
        //queue "1" into detector's buffer
        generator.setOOBDigit("1");
        generator.activate();
        detector.activate();
    	oobMixer.start();
        
        Thread.sleep(200);        

        generator.deactivate();
        detector.deactivate();
        oobMixer.stop();
        
        //assign listener and flush digit
        detector.addListener(this);
        detector.flushBuffer();

        //wait a bit for delivery
        Thread.sleep(300);        
        
        assertEquals("1", tone);
    }

    @Test
    public void testBuffering() throws InterruptedException, TooManyListenersException {
        //queue "1" into detector's buffer
        generator.setDigit("1");
        generator.activate();
        detector.activate();
    	audioMixer.start();
        
        Thread.sleep(200);          
        
        //queue "2" into detector's buffer
        generator.setDigit("2");
        generator.wakeup();
        
        Thread.sleep(200);          

        generator.deactivate();
        detector.deactivate();
    	audioMixer.stop();
        
        //assign listener and flush digit
        detector.addListener(this);
        detector.flushBuffer();

        //wait a bit for delivery
        Thread.sleep(200);        
        
        assertEquals("12", tone);
        
        tone="";
        detector.removeListener(this);
        //queue "1" into detector's buffer
        generator.setOOBDigit("1");
        generator.activate();
        detector.activate();
    	oobMixer.start();
        
        Thread.sleep(200);          
        
        //queue "2" into detector's buffer
        generator.setOOBDigit("2");
        generator.wakeup();
        
        Thread.sleep(200);          

        generator.deactivate();
        detector.deactivate();
        oobMixer.stop();
        
        //assign listener and flush digit
        detector.addListener(this);
        detector.flushBuffer();

        //wait a bit for delivery
        Thread.sleep(200);        
        
        assertEquals("12", tone);
    }

    @Test
    public void testDelivery() throws InterruptedException, TooManyListenersException {
        //assign listener
        detector.addListener(this);
        
        //queue "1" into detector's buffer
        generator.setDigit("1");
        generator.activate();
        detector.activate();
    	audioMixer.start();
        
        Thread.sleep(200);          

        generator.deactivate();
        detector.deactivate();
    	audioMixer.stop();    	

        //queue "2" into detector's buffer
        generator.setDigit("2");
        generator.activate();
        detector.activate();
    	audioMixer.start();
        
        Thread.sleep(200);          

        generator.deactivate();
        detector.deactivate();
    	audioMixer.stop();
        
        assertEquals("12", tone);
        
        //assign listener and flush digit
        detector.flushBuffer();

        //wait a bit for delivery
        Thread.sleep(200);        
        
        assertEquals("12", tone);
        
        tone="";
        //queue "1" into detector's buffer
        generator.setOOBDigit("1");
        generator.activate();
        detector.activate();
    	oobMixer.start();
        
        Thread.sleep(200);          

        generator.deactivate();
        detector.deactivate();
        oobMixer.stop();    	

        //queue "2" into detector's buffer
        generator.setOOBDigit("2");
        generator.activate();
        detector.activate();
        oobMixer.start();
        
        Thread.sleep(200);          

        generator.deactivate();
        detector.deactivate();
        oobMixer.stop();
        
        assertEquals("12", tone);
        
        //assign listener and flush digit
        detector.flushBuffer();

        //wait a bit for delivery
        Thread.sleep(200);        
        
        assertEquals("12", tone);
    }
    
    @Test
    public void testClear() throws InterruptedException, TooManyListenersException {
        //queue "1" into detector's buffer
        generator.setDigit("1");
        generator.activate();
        detector.activate();
    	audioMixer.start();
        
        Thread.sleep(200);          

        generator.deactivate();
        detector.deactivate();
    	audioMixer.stop();

        //queue "2" into detector's buffer
        generator.setDigit("2");
        generator.activate();
        detector.activate();
    	audioMixer.start();
        
        Thread.sleep(200);          

        generator.deactivate();
        detector.deactivate();
    	audioMixer.stop();
        
        //assign listener and flush digit
        detector.addListener(this);
        detector.clearBuffer();
        detector.flushBuffer();

        //wait a bit for delivery
        Thread.sleep(200);        
        
        assertEquals("", tone);
        
        detector.removeListener(this);
        //queue "1" into detector's buffer
        generator.setOOBDigit("1");
        generator.activate();
        detector.activate();
    	oobMixer.start();
        
        Thread.sleep(200);          

        generator.deactivate();
        detector.deactivate();
        oobMixer.stop();

        //queue "2" into detector's buffer
        generator.setOOBDigit("2");
        generator.activate();
        detector.activate();
        oobMixer.start();
        
        Thread.sleep(200);          

        generator.deactivate();
        detector.deactivate();
        oobMixer.stop();
        
        //assign listener and flush digit
        detector.addListener(this);
        detector.clearBuffer();
        detector.flushBuffer();

        //wait a bit for delivery
        Thread.sleep(200);        
        
        assertEquals("", tone);
    }        
    	
    public void process(DtmfEvent event) {
        tone += event.getTone();
    }
}
