/*
 * a singleton class for doing various utlity functions
 */
package zoneserver;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * @author Jason Zerbe
 */
public class ZoneServerUtility {

    private static ZoneServerUtility zsu_SingleInstance = null;
    private Preferences zsu_Preferences = null;

    private ZoneServerUtility() {
        zsu_Preferences = Preferences.userNodeForPackage(getClass());
    }

    public static ZoneServerUtility getInstance() {
        if (zsu_SingleInstance == null) {
            zsu_SingleInstance = new ZoneServerUtility();
        }
        return zsu_SingleInstance;
    }

    public void saveStringPref(String theStrPrefKey, String theStrPrefValue) {
        zsu_Preferences.put(theStrPrefKey, theStrPrefValue);
    }

    public String loadStringPref(String theStrPrefKey, String theStrPrefDefaultValue) {
        return zsu_Preferences.get(theStrPrefKey, theStrPrefDefaultValue);
    }

    public String getIPv4LanAddress() {
        Enumeration<NetworkInterface> aNetworkInterfaceEnumeration = null;
        try {
            aNetworkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException ex) {
            Logger.getLogger(ZoneServerUtility.class.getName()).log(Level.SEVERE, null, ex);
        }
        while (aNetworkInterfaceEnumeration.hasMoreElements()) {
            NetworkInterface currentNetworkInterface = aNetworkInterfaceEnumeration.nextElement();
            try {
                if (currentNetworkInterface.isLoopback()) {
                    continue;
                }
            } catch (SocketException ex) {
                Logger.getLogger(ZoneServerUtility.class.getName()).log(Level.SEVERE, null, ex);
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

    public String getFileNameFromUrlStr(String theUrlString) {
        return theUrlString.substring(theUrlString.lastIndexOf("/"));
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
