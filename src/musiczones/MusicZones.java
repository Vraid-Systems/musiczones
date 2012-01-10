/*
 * main class for application
 */
package musiczones;

import contrib.JettyWebServer;
import contrib.MediaPlayer;
import java.io.File;
import zonecontrol.ZoneServerLogic;
import zonecontrol.ZoneMulticastServer;
import java.util.logging.Level;
import java.util.logging.Logger;
import zonecontrol.ZoneServerUtility;

/**
 * @author Jason Zerbe
 */
public class MusicZones {

    protected static String global_ZoneName = null;
    protected static int global_webInterfacePortInt = 80;
    protected static String global_MPlayerBinPath = null;
    protected static int global_ScanMinInt = 5;
    protected static int global_ScanMaxInt = 15;
    protected static boolean global_IsLowMem = false;
    protected static boolean global_IsDebugOn = false;
    protected static boolean global_IsOnline = true;
    protected static String global_MPlayerNotFoundStr = "unable to find mplayer executable, please use --mplayer-bin-path=";
    protected static final String global_usageStr = "usage: java -jar mmc.jar "
            + "--zone-name=[zone controller's name] "
            + "--web-port=[web interface port number (default=80)] "
            + "--mplayer-bin-path=[path to mplayer] "
            + "--set-scan-min=[last octet of IPv4 in int] "
            + "--set-scan-max=[last octent of IPv4 in int] "
            + "--low-mem (do not build metadata indexes or other memory intensive tasks) "
            + "--debug-on (output debug information) "
            + "--offline (no LAN/WAN route)";

    public static boolean getIsDebugOn() {
        return global_IsDebugOn;
    }

    public static boolean getIsLowMem() {
        return global_IsLowMem;
    }

    public static boolean getIsOnline() {
        return global_IsOnline;
    }

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
            } else if (currentArg.contains("--set-scan-min=")) {
                String currentArgArray[] = currentArg.split("=");
                if (!currentArgArray[1].isEmpty()) {
                    global_ScanMinInt = Integer.valueOf(currentArgArray[1]);
                }
            } else if (currentArg.contains("--set-scan-max=")) {
                String currentArgArray[] = currentArg.split("=");
                if (!currentArgArray[1].isEmpty()) {
                    global_ScanMaxInt = Integer.valueOf(currentArgArray[1]);
                }
            } else if (currentArg.contains("--low-mem")) {
                global_IsLowMem = true;
            } else if (currentArg.contains("--debug-on")) {
                global_IsDebugOn = true;
            } else if (currentArg.contains("--offline")) {
                global_IsOnline = false;
            }
        } //done processing arguments

        //shutdown hook after arg proc, otherwise do un-needed traffic
        Runtime.getRuntime().addShutdownHook(new RunWhenShuttingDown());

        //save mplayer path to prefs if it can be found and not already saved
        String savedMPlayerBinPath = ZoneServerUtility.getInstance().loadStringPref(MediaPlayer.prefMediaPlayerPathKeyStr, "");
        File aExistsFile = new File(savedMPlayerBinPath);
        if (aExistsFile.exists()) {
            global_MPlayerBinPath = savedMPlayerBinPath;
        } else if ((!aExistsFile.exists()) && ((global_MPlayerBinPath == null) || (global_MPlayerBinPath.isEmpty()))) {
            if (ZoneServerUtility.getInstance().isWindows()) {
                global_MPlayerBinPath = "C:\\Program Files\\SMPlayer\\mplayer\\mplayer.exe";
                aExistsFile = new File(global_MPlayerBinPath);
                if (!aExistsFile.exists()) {
                    global_MPlayerBinPath = "C:\\Program Files (x86)\\SMPlayer\\mplayer\\mplayer.exe";
                    aExistsFile = new File(global_MPlayerBinPath);
                    if (!aExistsFile.exists()) {
                        MPlayerNotFound();
                    }
                }
            } else if (ZoneServerUtility.getInstance().isUnix()) {
                global_MPlayerBinPath = "/usr/bin/mplayer";
                aExistsFile = new File(global_MPlayerBinPath);
                if (!aExistsFile.exists()) {
                    MPlayerNotFound();
                }
            } else if (ZoneServerUtility.getInstance().isMac()) {
                global_MPlayerBinPath = "/Applications/MPlayer\\ OSX.app/Contents/Resources/External_Binaries/mplayer_intel.app/Contents/MacOS/mplayer";
                aExistsFile = new File(global_MPlayerBinPath);
                if (!aExistsFile.exists()) {
                    MPlayerNotFound();
                }
            }
        } else if (!aExistsFile.exists()) {
            aExistsFile = new File(global_MPlayerBinPath);
            if (!aExistsFile.exists()) {
                MPlayerNotFound();
            }
        }
        ZoneServerUtility.getInstance().saveStringPref(MediaPlayer.prefMediaPlayerPathKeyStr, global_MPlayerBinPath);

        //wait until network route is up
        String aIPv4Address = null;
        while (aIPv4Address == null) {
            aIPv4Address = ZoneServerUtility.getInstance().getIPv4LanAddress();
            if (aIPv4Address == null) {
                try {
                    if (getIsDebugOn()) {
                        System.out.println("route not ready, sleeping 2 seconds ...");
                    }
                    Thread.sleep((2 * 1000)); //2 seconds
                } catch (InterruptedException ex) {
                    Logger.getLogger(MusicZones.class.getName()).log(Level.WARNING, null, ex);
                }
            }
        }
        System.out.println("route is ready");

        //first intialize jetty in-case using custom webserver port
        JettyWebServer theWebServer = JettyWebServer.getInstance(global_webInterfacePortInt);
        theWebServer.startServer();

        //create system-wide MediaPlayer instance
        MediaPlayer.getInstance(getIsDebugOn());

        //then bring up the zone controller logic
        ZoneServerLogic mainServerLogic = ZoneServerLogic.getInstance();
        if ((global_ZoneName != null) && (!global_ZoneName.isEmpty())) {
            mainServerLogic.setZoneName(global_ZoneName);
        }

        //pause just enough (1/10 second) to prevent receiving own init messages from group
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            Logger.getLogger(MusicZones.class.getName()).log(Level.WARNING, null, ex);
        }

        //after networking is in place, finally ready to start accepting control packets
        if (getIsOnline()) {
            ZoneMulticastServer theZoneServer = new ZoneMulticastServer(getIsDebugOn());
            theZoneServer.startServer();
        }

        //start up the library indexing service
        if (getIsOnline()) {
            ZoneLibraryIndex.getInstance(getIsDebugOn());
            ZoneLibraryIndex.getInstance().setScanMin(global_ScanMinInt);
            ZoneLibraryIndex.getInstance().setScanMax(global_ScanMaxInt);
        }

        //start the master server notification point
        if (getIsOnline()) {
            HttpCmdClient.getInstance(getIsDebugOn());
        }
    }

    /**
     * stop all relevant services in order of graceful shutdown needs
     */
    public static class RunWhenShuttingDown extends Thread {

        @Override
        public void run() {
            //1. remove node from master server - will just timeout on server if this is not done
            if (getIsOnline()) {
                HttpCmdClient.getInstance().notifyDown();
            }

            //2. shutdown jetty -  does not really matter if aborted abruptly
            JettyWebServer.getInstance().stopServer();
        }
    }

    /**
     * dump error and exit when unable to find MPlayer
     */
    public static void MPlayerNotFound() {
        System.out.println(global_MPlayerNotFoundStr);
        System.out.println(global_usageStr);
        System.exit(1);
    }
}
