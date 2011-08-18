/*
 * main class for application
 */
package multicastmusiccontroller;

import contrib.JettyWebServer;
import zoneserver.ZoneServerLogic;
import zoneserver.ZoneMulticastServer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jason Zerbe
 */
public class MulticastMusicController {

    protected static String global_ZoneName = null;
    protected static int global_webInterfacePortInt = 80;
    protected static final String global_usageStr = "usage: java -jar mmc.jar "
            + "--zone-name=[zone controller's name] "
            + "--web-port=[web interface port number (default=80)]";

    /**
     * master start of application
     * 
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length > 1) { //need to process arguments
            for (int i = 0; i < args.length; i++) {
                String currentArg = args[i];
                if (currentArg.contains("-h")) {
                    System.out.println(global_usageStr);
                    System.exit(0);
                } else if (currentArg.contains("--zone-name=")) {
                    String currentArgArray[] = currentArg.split("=");
                    global_ZoneName = currentArgArray[1];
                } else if (currentArg.contains("--web-port=")) {
                    String currentArgArray[] = currentArg.split("=");
                    if (!currentArgArray[1].isEmpty()) {
                        global_webInterfacePortInt = Integer.valueOf(currentArgArray[1]);
                    }
                }
            }
        } //done processing arguments

        ZoneServerLogic mainServerLogic = ZoneServerLogic.getInstance();
        if ((global_ZoneName != null) && (!global_ZoneName.isEmpty())) {
            mainServerLogic.setZoneName(global_ZoneName);
        }

        try {
            Thread.sleep(100); //pause just enough (1/10 second) so that we don't get any own init messages from group
        } catch (InterruptedException ex) {
            Logger.getLogger(MulticastMusicController.class.getName()).log(Level.SEVERE, null, ex);
        }

        ZoneMulticastServer theZoneServer = new ZoneMulticastServer();
        theZoneServer.startServer();

        JettyWebServer theWebServer = JettyWebServer.getInstance(global_webInterfacePortInt);
        theWebServer.startServer();
    }
}
