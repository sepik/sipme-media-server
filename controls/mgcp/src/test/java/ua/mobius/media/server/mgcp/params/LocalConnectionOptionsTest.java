/*
 * 15/07/13 - Change notice:
 * This file has been modified by Mobius Software Ltd.
 * For more information please visit http://www.mobius.ua
 */
package ua.mobius.media.server.mgcp.params;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import ua.mobius.media.server.utils.Text;

/**
 *
 * @author kulikov
 */
public class LocalConnectionOptionsTest {
    
    private LocalConnectionOptions lcOptions = new LocalConnectionOptions();
    
    public LocalConnectionOptionsTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
        lcOptions.setValue(new Text("gc:10, a:PCMU"));
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getGain method, of class LocalConnectionOptions.
     */
    @Test
    public void testGetGain() {
        assertEquals(10, lcOptions.getGain());
    }
}
