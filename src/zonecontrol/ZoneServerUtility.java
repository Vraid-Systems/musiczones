/*
 * a singleton class for doing various utlity functions
 */
package zonecontrol;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import musiczones.FileSystemType;

/**
 * @author Jason Zerbe
 */
public class ZoneServerUtility {

    private static ZoneServerUtility zsu_SingleInstance = null;
    private Preferences zsu_Preferences = null;
    private boolean debugMessagesOn = false;
    public static final String mediaNameSplitStr = ":&&:";
    public static final String prefixUriStr = "://";

    protected ZoneServerUtility(boolean theDebugIsOn) {
        debugMessagesOn = theDebugIsOn;
        zsu_Preferences = Preferences.userNodeForPackage(getClass());
    }

    public static ZoneServerUtility getInstance() {
        if (zsu_SingleInstance == null) {
            zsu_SingleInstance = new ZoneServerUtility(false);
        }
        return zsu_SingleInstance;
    }

    public static ZoneServerUtility getInstance(boolean theDebugIsOn) {
        if (zsu_SingleInstance == null) {
            zsu_SingleInstance = new ZoneServerUtility(theDebugIsOn);
        }
        return zsu_SingleInstance;
    }

    /**
     * gets the correct LAN IPv4 address of the local machine
     * @return String, null on error
     */
    public String getIPv4LanAddress() {
        Enumeration<NetworkInterface> aNetworkInterfaceEnumeration = null;
        try {
            aNetworkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException ex) {
            Logger.getLogger(ZoneServerUtility.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
        while (aNetworkInterfaceEnumeration.hasMoreElements()) {
            NetworkInterface currentNetworkInterface = aNetworkInterfaceEnumeration.nextElement();
            try {
                if (currentNetworkInterface.isLoopback()) {
                    continue;
                }
            } catch (SocketException ex) {
                Logger.getLogger(ZoneServerUtility.class.getName()).log(Level.SEVERE, null, ex);
                return null;
            }

            for (InterfaceAddress currentNetworkInterfaceAddress : currentNetworkInterface.getInterfaceAddresses()) {
                InetAddress currentNetworkInterfaceInetAddress = currentNetworkInterfaceAddress.getAddress();

                if (!(currentNetworkInterfaceInetAddress instanceof Inet4Address)) {
                    continue;
                }

                return currentNetworkInterfaceInetAddress.getHostAddress();
            }
        }

        return null;
    }

    public void saveIntPref(String theIntPrefKey, int theIntPrefValue) {
        zsu_Preferences.putInt(theIntPrefKey, theIntPrefValue);
    }

    public void saveStringPref(String theStrPrefKey, String theStrPrefValue) {
        zsu_Preferences.put(theStrPrefKey, theStrPrefValue);
    }

    public int loadIntPref(String theIntPrefKey, int theIntPrefDefaultValue) {
        return zsu_Preferences.getInt(theIntPrefKey, theIntPrefDefaultValue);
    }

    public String loadStringPref(String theStrPrefKey, String theStrPrefDefaultValue) {
        return zsu_Preferences.get(theStrPrefKey, theStrPrefDefaultValue);
    }

    public String getFileNameFromUrlStr(String theUrlString) {
        if (debugMessagesOn) {
            System.out.println("url to format: " + theUrlString);
        }
        String returnStr = null;
        if (theUrlString.contains(FileSystemType.radio.toString().concat(prefixUriStr))) {
            if (theUrlString.contains(mediaNameSplitStr)) {
                String[] theUrlStringArray = theUrlString.split(mediaNameSplitStr);
                returnStr = theUrlStringArray[0];
            } else {
                returnStr = theUrlString;
            }
        } else {
            returnStr = theUrlString.substring((theUrlString.lastIndexOf("/") + 1));
        }
        return returnStr;
    }

    public boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return (os.indexOf("win") >= 0);
    }

    public boolean isMac() {
        String os = System.getProperty("os.name").toLowerCase();
        return (os.indexOf("mac") >= 0);
    }

    public boolean isUnix() {
        String os = System.getProperty("os.name").toLowerCase();
        return (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0);
    }
}
