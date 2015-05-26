/*
 * 15/07/13 - Change notice:
 * This file has been modified by Mobius Software Ltd.
 * For more information please visit http://www.mobius.ua
 */
package ua.mobius.media.server.impl.resource.audio;

import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ua.mobius.media.server.component.audio.AudioInput;
import ua.mobius.media.server.component.audio.AudioOutput;
import ua.mobius.media.server.component.audio.AudioComponent;
import ua.mobius.media.server.component.audio.AudioMixer;
import ua.mobius.media.server.component.audio.Sine;
import ua.mobius.media.server.scheduler.Clock;
import ua.mobius.media.server.scheduler.DefaultClock;
import ua.mobius.media.server.scheduler.DefaultScheduler;
import ua.mobius.media.server.scheduler.Scheduler;

/**
 *
 * @author yulian oifa
 */
public class AudioRecorderImplTest {
    
    private Clock clock;
    private Scheduler scheduler;

    private Sine sine;
    private AudioRecorderImpl recorder;
    
    private AudioComponent sineComponent;
    private AudioComponent recorderComponent;
    
    private AudioMixer mixer;
    
    public AudioRecorderImplTest() {
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

        sine = new Sine(scheduler); 
        sine.setFrequency(250);
        
        recorder = new AudioRecorderImpl(scheduler);
        
        sineComponent=new AudioComponent(1);
        recorderComponent=new AudioComponent(2);
        
        sineComponent.updateMode(true,true);
        recorderComponent.updateMode(true,true);
        
        sineComponent.addInput(sine.getAudioInput());
        recorderComponent.addOutput(recorder.getAudioOutput());
        
        mixer=new AudioMixer(scheduler);
        mixer.addComponent(sineComponent);
        mixer.addComponent(recorderComponent);               
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of start method, of class AudioRecorderImpl.
     */
    @Test
    public void testConvert() {
        System.out.println("======" + Integer.toHexString(8000));
    }

    /**
     * Test of stop method, of class AudioRecorderImpl.
     * Check it manually
     */
//    @Test
    public void testRecording() throws InterruptedException, IOException {
        recorder.setRecordFile("file:///home/kulikov/record-test.wav", false);
        recorder.setPostSpeechTimer(5000000000L);
        
        sine.start();  
        mixer.start();
        
        Thread.sleep(5000);        
        sine.setAmplitude((short)0);
        
        Thread.sleep(7000);
        sine.stop();
        mixer.stop();
    }

}
