/*
 * 15/07/13 - Change notice:
 * This file has been modified by Mobius Software Ltd.
 * For more information please visit http://www.mobius.ua
 */
package ua.mobius.media.server.impl.resource.dtmf;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import ua.mobius.media.server.component.audio.AudioComponent;
import ua.mobius.media.server.component.audio.AudioMixer;

import ua.mobius.media.server.component.oob.OOBComponent;
import ua.mobius.media.server.component.oob.OOBMixer;

import ua.mobius.media.server.scheduler.Clock;
import ua.mobius.media.server.scheduler.DefaultClock;
import ua.mobius.media.server.scheduler.DefaultScheduler;
import ua.mobius.media.server.scheduler.Scheduler;
import ua.mobius.media.server.spi.dtmf.DtmfDetectorListener;
import ua.mobius.media.server.spi.dtmf.DtmfEvent;
import ua.mobius.media.server.spi.listener.TooManyListenersException;

/**
 *
 * @author yulian oifa
 */
public class DtmfTest implements DtmfDetectorListener {
    
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
    
    public DtmfTest() {
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

        scheduler = new DefaultScheduler();
        scheduler.setClock(clock);
        scheduler.start();
        
        generator = new GeneratorImpl("dtmf", scheduler,false,3);
        generator.setToneDuration(500);
        generator.setVolume(-20);
        
        detector = new DetectorImpl("dtmf", scheduler);
        detector.setVolume(-35);
        detector.setDuration(40);
        
        detector.addListener(this);
        
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
    public void testDigit1() throws InterruptedException {
    	generator.setDigit("1");
        generator.activate();
        detector.activate();
    	audioMixer.start();
        
        Thread.sleep(1000);
        
        generator.deactivate();
        detector.deactivate();
    	audioMixer.stop();
    	
        assertEquals("1", tone);    	
        
        tone="";
        generator.setOOBDigit("1");
        generator.activate();
        detector.activate();
    	oobMixer.start();
        
        Thread.sleep(1000);
        
        generator.deactivate();
        detector.deactivate();
    	oobMixer.stop();
    	
        assertEquals("1", tone);
    }

    @Test
    public void testDigit2() throws InterruptedException {
        generator.setDigit("2");
        generator.activate();
        detector.activate();
    	audioMixer.start();
        
        Thread.sleep(1000);
        
        generator.deactivate();
        detector.deactivate();
    	audioMixer.stop();
    	
        assertEquals("2", tone);
        
        tone="";
        generator.setOOBDigit("2");
        generator.activate();
        detector.activate();
    	oobMixer.start();
        
        Thread.sleep(1000);
        
        generator.deactivate();
        detector.deactivate();
    	oobMixer.stop();
    	
        assertEquals("2", tone);
    }
    
    @Test
    public void testDigit3() throws InterruptedException {
        generator.setDigit("3");
        generator.activate();
        detector.activate();
    	audioMixer.start();
        
        Thread.sleep(1000);
        
        generator.deactivate();
        detector.deactivate();
    	audioMixer.stop();
    	
        assertEquals("3", tone);
        
        tone="";
        generator.setOOBDigit("3");
        generator.activate();
        detector.activate();
    	oobMixer.start();
        
        Thread.sleep(1000);
        
        generator.deactivate();
        detector.deactivate();
    	oobMixer.stop();
    	
        assertEquals("3", tone);
    }

    @Test
    public void testDigit4() throws InterruptedException {
        generator.setDigit("4");
        generator.activate();
        detector.activate();
    	audioMixer.start();
        
        Thread.sleep(1000);
        
        generator.deactivate();
        detector.deactivate();
    	audioMixer.stop();
        
        assertEquals("4", tone);
        
        tone="";
        generator.setOOBDigit("4");
        generator.activate();
        detector.activate();
    	oobMixer.start();
        
        Thread.sleep(1000);
        
        generator.deactivate();
        detector.deactivate();
    	oobMixer.stop();
    	
        assertEquals("4", tone);
    }
    
    @Test
    public void testDigit5() throws InterruptedException {
        generator.setDigit("5");
        generator.activate();
        detector.activate();
    	audioMixer.start();
        
        Thread.sleep(1000);
        
        generator.deactivate();
        detector.deactivate();
    	audioMixer.stop();
        
        assertEquals("5", tone);
        
        tone="";
        generator.setOOBDigit("5");
        generator.activate();
        detector.activate();
    	oobMixer.start();
        
        Thread.sleep(1000);
        
        generator.deactivate();
        detector.deactivate();
    	oobMixer.stop();
    	
        assertEquals("5", tone);
    }
    
    @Test
    public void testDigit6() throws InterruptedException {
        generator.setDigit("6");
        generator.activate();
        detector.activate();
    	audioMixer.start();
        
        Thread.sleep(1000);
        
        generator.deactivate();
        detector.deactivate();
    	audioMixer.stop();
        
        assertEquals("6", tone);  
        
        tone="";
        generator.setOOBDigit("6");
        generator.activate();
        detector.activate();
    	oobMixer.start();
        
        Thread.sleep(1000);
        
        generator.deactivate();
        detector.deactivate();
    	oobMixer.stop();
    	
        assertEquals("6", tone);
    }
    
    @Test
    public void testDigit7() throws InterruptedException {
        generator.setDigit("7");
        generator.activate();
        detector.activate();
    	audioMixer.start();
        
        Thread.sleep(1000);
        
        generator.deactivate();
        detector.deactivate();
    	audioMixer.stop();
        
        assertEquals("7", tone);
        
        tone="";
        generator.setOOBDigit("7");
        generator.activate();
        detector.activate();
    	oobMixer.start();
        
        Thread.sleep(1000);
        
        generator.deactivate();
        detector.deactivate();
    	oobMixer.stop();
    	
        assertEquals("7", tone);
    }
    
    @Test
    public void testDigit8() throws InterruptedException {
        generator.setDigit("8");
        generator.activate();
        detector.activate();
    	audioMixer.start();
        
        Thread.sleep(1000);
        
        generator.deactivate();
        detector.deactivate();
    	audioMixer.stop();
        
        assertEquals("8", tone);
        
        tone="";
        generator.setOOBDigit("8");
        generator.activate();
        detector.activate();
    	oobMixer.start();
        
        Thread.sleep(1000);
        
        generator.deactivate();
        detector.deactivate();
    	oobMixer.stop();
    	
        assertEquals("8", tone);
    }
    
    @Test
    public void testDigit9() throws InterruptedException {
        generator.setDigit("9");
        generator.activate();
        detector.activate();
    	audioMixer.start();
        
        Thread.sleep(1000);
        
        generator.deactivate();
        detector.deactivate();
    	audioMixer.stop();
        
        assertEquals("9", tone);
        
        tone="";
        generator.setOOBDigit("9");
        generator.activate();
        detector.activate();
    	oobMixer.start();
        
        Thread.sleep(1000);
        
        generator.deactivate();
        detector.deactivate();
    	oobMixer.stop();
    	
        assertEquals("9", tone);
    }
    
    @Test
    public void testDigit0() throws InterruptedException {
        generator.setDigit("0");
        generator.activate();
        detector.activate();
    	audioMixer.start();
        
        Thread.sleep(1000);
        
        generator.deactivate();
        detector.deactivate();
    	audioMixer.stop();
        
        assertEquals("0", tone);
        
        tone="";
        generator.setOOBDigit("0");
        generator.activate();
        detector.activate();
    	oobMixer.start();
        
        Thread.sleep(1000);
        
        generator.deactivate();
        detector.deactivate();
    	oobMixer.stop();
    	
        assertEquals("0", tone);
    }
    
    public void process(DtmfEvent event) {
        tone = event.getTone();
    }
}
