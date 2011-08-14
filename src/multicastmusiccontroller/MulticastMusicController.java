/*
 * main class for application
 */
package multicastmusiccontroller;

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
    protected static String global_vlcPath = "";

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
                } else if (currentArg.contains("--vlc-path=")) {
                    String currentArgArray[] = currentArg.split("=");
                    global_vlcPath = currentArgArray[1];
                }
            } //done processing arguments

            if (global_nodeName.isEmpty() || global_vlcPath.isEmpty()) {
                System.out.println("usage: java -jar mmc.jar --vlc-path=[full path to vlc executable]");
            } else {
                MulticastServer theServer = new MulticastServer(
                        new ServerLogic(generateNodeUUID(),
                        new VlcController(global_vlcPath), global_nodeName));
            }
        } else {
            System.out.println("usage: java -jar mmc.jar --vlc-path=[full path to vlc executable]");
        }
    }

    /**
     * generates a UUID from the startup time, and the name of node + vlcpath
     * if they are available
     * @return String - the node UUID
     */
    protected static String generateNodeUUID() {
        Date aDate = new Date();
        String stringToHash = global_nodeName + " was started on "
                + aDate.toString() + " with " + global_vlcPath
                + " as the location of the VLC executable.";
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
