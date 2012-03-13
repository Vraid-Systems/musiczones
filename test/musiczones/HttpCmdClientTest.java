/*
 * test that the Java software and the PHP master server work together
 */
package musiczones;

import netutil.HttpCmdClient;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Jason Zerbe
 */
public class HttpCmdClientTest {

    public HttpCmdClientTest() {
    }

    /**
     * check to make sure lower level HTTP processing works
     */
    @Test
    public void testNotifyDown() {
        System.out.println("notifyDown");
        HttpCmdClient instance = HttpCmdClient.getInstance(true);
        boolean expResult = true;
        boolean result = instance.notifyDown();
        assertEquals(expResult, result);
    }

    /**
     * does not check result, checks to be sure low-level HTTP works
     */
    @Test
    public void testNotifyUp() {
        System.out.println("notifyUp");
        HttpCmdClient instance = HttpCmdClient.getInstance(true);
        boolean expResult = true;
        boolean result = instance.notifyUp(false, 1);
        instance.notifyDown();
        assertEquals(expResult, result);
    }
}
