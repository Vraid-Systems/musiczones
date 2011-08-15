/*
 * main class for application
 */
package multicastmusiccontroller;

import zoneserver.ZoneServerLogic;
import zoneserver.ZoneMulticastServer;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jason Zerbe
 */
public class MulticastMusicController {

    protected static String global_nodeName = "";
    protected static final String global_usageStr = "usage: java -jar mmc.jar --name=[zone controller's name]";

    /**
     * master start of application
     * 
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length > 1) { //need to process the additional arguments
            for (int i = 0; i < args.length; i++) {
                String currentArg = args[i];
                if (currentArg.contains("--name=")) {
                    String currentArgArray[] = currentArg.split("=");
                    global_nodeName = currentArgArray[1];
                }
            } //done processing arguments

            if (global_nodeName.isEmpty()) {
                System.out.println(global_usageStr);
            } else {
                ZoneServerLogic mainServerLogic = new ZoneServerLogic(generateNodeUUID(), global_nodeName);

                try {
                    Thread.sleep(100); //pause just enough (1/10 second) so that we don't get any own init messages from group
                } catch (InterruptedException ex) {
                    Logger.getLogger(MulticastMusicController.class.getName()).log(Level.SEVERE, null, ex);
                }

                ZoneMulticastServer theServer = new ZoneMulticastServer(mainServerLogic);
            }
        } else {
            System.out.println(global_usageStr);
        }
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
        System.out.println(hashedString);

        return hashedString;
    }
}
