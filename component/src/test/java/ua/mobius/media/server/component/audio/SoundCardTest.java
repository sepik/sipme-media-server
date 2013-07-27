/*
 * 15/07/13 - Change notice:
 * This file has been modified by Mobius Software Ltd.
 * For more information please visit http://www.mobius.ua
 */
package ua.mobius.media.server.component.audio;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ua.mobius.media.server.scheduler.Clock;
import ua.mobius.media.server.scheduler.DefaultClock;
import ua.mobius.media.server.scheduler.Scheduler;

/**
 *
 * @author oifa yulian
 */
public class SoundCardTest {
    
    private Clock clock;
    private Scheduler scheduler;
    
    private Sine sine;
    private SoundCard soundCard;
    
    private AudioComponent sineComponent;
    private AudioComponent soundCardComponent;
    
    private AudioMixer audioMixer;
    
    public SoundCardTest() {
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

        scheduler = new Scheduler();
        scheduler.setClock(clock);
        scheduler.start();

        sine = new Sine(scheduler);
        sine.setFrequency(200);
        
        soundCard = new SoundCard(scheduler);
        
        sineComponent=new AudioComponent(1);
        sineComponent.addInput(sine.getAudioInput());
        sineComponent.updateMode(true,false);
        
        soundCardComponent=new AudioComponent(2);
        soundCardComponent.addOutput(soundCard.getAudioOutput());
        soundCardComponent.updateMode(false,true);
        
        audioMixer=new AudioMixer(scheduler);
        audioMixer.addComponent(soundCardComponent);
        audioMixer.addComponent(sineComponent);               
    }
    
    @After
    public void tearDown() {
    	sine.stop();
    	audioMixer.stop();
    	audioMixer.release(sineComponent);
    	audioMixer.release(soundCardComponent);
    	
        scheduler.stop();
    }

    /**
     * Test of start method, of class SoundCard.
     */
    @Test
    public void testSignal() throws InterruptedException {
    	sine.start();        
    	audioMixer.start();
        Thread.sleep(3000);        
        sine.stop();
        audioMixer.stop();
    }
}
