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
import static org.junit.Assert.*;

/**
 *
 * @author kulikov
 */
public class FFTTest {

    private FFT fft = new FFT();

    public FFTTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of fft method, of class FFT.
     */
    @Test
    public void testFft() {
        System.out.println("fft");
        Complex[] s = new Complex[8192];
        for (int i = 0; i < s.length; i++) {
            s[i] = new Complex(0,0);
        }
        Complex[] r = fft.fft(s);
        r = fft.fft(s);
        long st = System.nanoTime();
        r = fft.fft(s);
        long f = System.nanoTime();
        System.out.println("Duration=" + (f-st));
    }

}