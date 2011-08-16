/*
 * main class for application
 */
package multicastmusiccontroller;

import contrib.JettyWebServer;
import zoneserver.ZoneServerLogic;
import zoneserver.ZoneMulticastServer;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jason Zerbe
 */
public class MulticastMusicController {

    protected static String global_nodeName = "zone controller ";
    protected static int global_webInterfacePortInt = 80;
    protected static final String global_usageStr = "usage: java -jar mmc.jar "
            + "--zone-name=[zone controller's name] "
            + "--web-port=[web interface port number (default=8080)]";

    /**
     * master start of application
     * 
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Random generator = new Random(19580427);
        int randomIndex = generator.nextInt(19580427);
        global_nodeName += String.valueOf(randomIndex);
        System.out.println("default global_nodeName = " + global_nodeName);

        if (args.length > 1) { //need to process arguments
            for (int i = 0; i < args.length; i++) {
                String currentArg = args[i];
                if (currentArg.contains("--zone-name=")) {
                    String currentArgArray[] = currentArg.split("=");
                    global_nodeName = currentArgArray[1];
                } else if (currentArg.contains("--web-port=")) {
                    String currentArgArray[] = currentArg.split("=");
                    if (!currentArgArray[1].isEmpty()) {
                        global_webInterfacePortInt = Integer.valueOf(currentArgArray[1]);
                    }
                }
            }
        } //done processing arguments

        ZoneServerLogic mainServerLogic = new ZoneServerLogic(generateNodeUUID(), global_nodeName);

        try {
            Thread.sleep(100); //pause just enough (1/10 second) so that we don't get any own init messages from group
        } catch (InterruptedException ex) {
            Logger.getLogger(MulticastMusicController.class.getName()).log(Level.SEVERE, null, ex);
        }

        ZoneMulticastServer theZoneServer = new ZoneMulticastServer(mainServerLogic);

        JettyWebServer theWebServer = new JettyWebServer(global_webInterfacePortInt);
        theWebServer.startServer();
    }

    /**
     * generates a UUID from the startup time, and the name of node + vlcpath
     * if they are available
     * @return String - the node UUID
     */
    public static String generateNodeUUID() {
        Date aDate = new Date();
        String stringToHash = global_nodeName + " was started on "
                + aDate.toString() + ".";
        System.out.println(stringToHash);

        String hashedString = null;
        try {
            hashedString = contrib.AeSimpleSHA1.SHA1(stringToHash);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(MulticastMusicController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(MulticastMusicController.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("hashed = " + hashedString);

        return hashedString;
    }
}
