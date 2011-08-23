/*
 * main class for application
 */
package multicastmusiccontroller;

import contrib.JettyWebServer;
import zoneserver.ZoneServerLogic;
import zoneserver.ZoneMulticastServer;
import java.util.logging.Level;
import java.util.logging.Logger;
import zoneserver.ZoneServerUtility;

/**
 *
 * @author Jason Zerbe
 */
public class MulticastMusicController implements ProgramConstants {

    protected static String global_ZoneName = null;
    protected static int global_webInterfacePortInt = 80;
    protected static String global_MPlayerBinPath = "mplayer";
    protected static final String global_usageStr = "usage: java -jar mmc.jar "
            + "--zone-name=[zone controller's name] "
            + "--web-port=[web interface port number (default=80)]"
            + "--mplayer-bin-path=[path to mplayer (default=mplayer)]";

    /**
     * master start of application
     * 
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        for (String currentArg : args) {
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
            } else if (currentArg.contains("--mplayer-bin-path=")) {
                String currentArgArray[] = currentArg.split("=");
                if (!currentArgArray[1].isEmpty()) {
                    global_MPlayerBinPath = currentArgArray[1];
                }
            }
        } //done processing arguments

        //save certain arguments to prefs
        ZoneServerUtility.getInstance().saveStringPref(prefMediaPlayerPathKeyStr, global_MPlayerBinPath);

        //first intialize jetty in-case using custom webserver port
        JettyWebServer theWebServer = JettyWebServer.getInstance(global_webInterfacePortInt);
        theWebServer.startServer();

        //then get bring up the zone controller logic
        ZoneServerLogic mainServerLogic = ZoneServerLogic.getInstance();
        if ((global_ZoneName != null) && (!global_ZoneName.isEmpty())) {
            mainServerLogic.setZoneName(global_ZoneName);
        }

        //pause just enough (1/10 second) so that we don't get any own init messages from group
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Logger.getLogger(MulticastMusicController.class.getName()).log(Level.SEVERE, null, ex);
        }

        //after everything else is in place, finally ready to start accepting control packets
        ZoneMulticastServer theZoneServer = new ZoneMulticastServer();
        theZoneServer.startServer();
    }
}
