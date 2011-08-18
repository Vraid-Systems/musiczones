/*
 * a singleton class for doing various network related utlity functions
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

/**
 * @author Jason Zerbe
 */
public class ZoneServerUtility {

    private static ZoneServerUtility zsu_SingleInstance = null;

    private ZoneServerUtility() {
    }

    public static ZoneServerUtility getInstance() {
        if (zsu_SingleInstance == null) {
            zsu_SingleInstance = new ZoneServerUtility();
        }
        return zsu_SingleInstance;
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
}
