/*
 * main class for JAR application
 */
package musiczones;

import audio.MediaPlayerImpl;
import contrib.JettyWebServer;
import java.io.File;
import netutil.HttpCmdClient;
import netutil.IpAddressType;
import netutil.Layer3Info;
import zonecontrol.ZoneLibraryIndex;
import zonecontrol.ZoneMulticastServer;
import zonecontrol.ZoneServerLogic;
import zonecontrol.ZoneServerUtility;

/**
 * @author Jason Zerbe
 */
public class MusicZones {

    protected static String global_ZoneName = null;
    protected static int global_webInterfacePortInt = 2320;
    protected static String global_MPlayerBinPath = null;
    protected static int global_ScanMinInt = 1;
    protected static int global_ScanMaxInt = 10;
    protected static boolean global_IsLowMem = false;
    protected static boolean global_IsDebugOn = false;
    protected static boolean global_IsOnline = true;
    protected static boolean global_IndexLocalHost = false;
    protected static String global_MPlayerNotFoundStr = "unable to find mplayer executable, please use --mplayer-bin-path=";
    protected static final String global_usageStr = "usage: java -jar mmc.jar "
            + "--zone-name=[zone controller's name] "
            + "--web-port=[web interface port number (default=2320)] "
            + "--mplayer-bin-path=[path to mplayer] "
            + "--set-scan-min=[last octet of IPv4 in int] "
            + "--set-scan-max=[last octent of IPv4 in int] "
            + "--low-mem (do not build metadata indexes or other memory intensive tasks) "
            + "--debug-on (output debug information) "
            + "--offline (no LAN/WAN route) "
            + "--localhost (force index of localhost)";

    public static boolean getIsDebugOn() {
        return global_IsDebugOn;
    }

    public static boolean getIsLowMem() {
        return global_IsLowMem;
    }

    public static boolean getIsOnline() {
        return global_IsOnline;
    }

    public static boolean getIsIndexLocalHost() {
        return global_IndexLocalHost;
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
                if (!"".equals(currentArgArray[1])) {
                    global_webInterfacePortInt = Integer.valueOf(currentArgArray[1]);
                }
            } else if (currentArg.contains("--mplayer-bin-path=")) {
                String currentArgArray[] = currentArg.split("=");
                if (!"".equals(currentArgArray[1])) {
                    global_MPlayerBinPath = currentArgArray[1];
                }
            } else if (currentArg.contains("--set-scan-min=")) {
                String currentArgArray[] = currentArg.split("=");
                if (!"".equals(currentArgArray[1])) {
                    global_ScanMinInt = Integer.valueOf(currentArgArray[1]);
                }
            } else if (currentArg.contains("--set-scan-max=")) {
                String currentArgArray[] = currentArg.split("=");
                if (!"".equals(currentArgArray[1])) {
                    global_ScanMaxInt = Integer.valueOf(currentArgArray[1]);
                }
            } else if (currentArg.contains("--low-mem")) {
                global_IsLowMem = true;
            } else if (currentArg.contains("--debug-on")) {
                global_IsDebugOn = true;
            } else if (currentArg.contains("--offline")) {
                global_IsOnline = false;
            } else if (currentArg.contains("--localhost")) {
                global_IndexLocalHost = true;
            }
        } //done processing arguments

        //shutdown hook after arg proc, otherwise do un-needed traffic
        Runtime.getRuntime().addShutdownHook(new RunWhenShuttingDown());

        //save mplayer path to prefs if it can be found and not already saved
        String savedMPlayerBinPath = ZoneServerUtility.getInstance().loadStringPref(MediaPlayerImpl.prefMediaPlayerPathKeyStr, "");
        File aExistsFile = new File(savedMPlayerBinPath);
        if (aExistsFile.exists()) {
            global_MPlayerBinPath = savedMPlayerBinPath;
        } else if ((!aExistsFile.exists()) && ((global_MPlayerBinPath == null) || ("".equals(global_MPlayerBinPath)))) {
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
        ZoneServerUtility.getInstance().saveStringPref(MediaPlayerImpl.prefMediaPlayerPathKeyStr, global_MPlayerBinPath);

        //wait until network route is up
        if (getIsOnline()) {
            String aIPv4Address = null;
            while (aIPv4Address == null) {
                aIPv4Address = Layer3Info.getInstance().getValidIPAddress(IpAddressType.IPv4);
                if (aIPv4Address == null) {
                    try {
                        if (getIsDebugOn()) {
                            System.out.println("route not ready, sleeping 2 seconds ...");
                        }
                        Thread.sleep((2 * 1000)); //2 seconds
                    } catch (InterruptedException ex) {
                        System.err.println(ex);
                    }
                }
            }
            System.out.println("route is ready");
        }

        //first initialize jetty in-case using custom webserver port
        JettyWebServer theWebServer = JettyWebServer.getInstance(global_webInterfacePortInt);
        theWebServer.startServer();

        //create system-wide MediaPlayer instance
        MediaPlayerImpl.getInstance(getIsDebugOn());

        //then bring up the zone controller logic
        ZoneServerLogic mainServerLogic = ZoneServerLogic.getInstance();
        if ((global_ZoneName != null) && (!"".equals(global_ZoneName))) {
            mainServerLogic.setZoneName(global_ZoneName);
        }

        //pause just enough (1/10 second) to prevent receiving own init messages from group
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {
            System.err.println(ex);
        }

        //after networking is in place, finally ready to start accepting control packets
        if (getIsOnline()) {
            ZoneMulticastServer theZoneServer = new ZoneMulticastServer(getIsDebugOn());
            theZoneServer.startServer();
        }

        //start up the library indexing service
        ZoneLibraryIndex.getInstance(getIsDebugOn());
        ZoneLibraryIndex.getInstance().setScanMin(global_ScanMinInt);
        ZoneLibraryIndex.getInstance().setScanMax(global_ScanMaxInt);

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
