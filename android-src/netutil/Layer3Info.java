/*
 * singleton utility class for getting (IP) Network Layer information
 */
package netutil;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jason Zerbe
 */
public class Layer3Info {

	private static Layer3Info l3i_singleInstance = null;
	private boolean l3i_DebugOn = false;

	protected Layer3Info(boolean theDebugOn) {
		l3i_DebugOn = theDebugOn;
	}

	public static Layer3Info getInstance() {
		if (l3i_singleInstance == null) {
			l3i_singleInstance = new Layer3Info(false);
		}
		return l3i_singleInstance;
	}

	public static Layer3Info getInstance(boolean theDebugOn) {
		if (l3i_singleInstance == null) {
			l3i_singleInstance = new Layer3Info(theDebugOn);
		}
		return l3i_singleInstance;
	}

	/**
	 * gets a valid IP Address from the local machine that is able to be routed
	 * 
	 * @see http://www.droidnova.com/get-the-ip-address-of-your-device,304.html
	 * @param theIpAddressType IpAddressType - NOT USED in Android IMPL
	 * @return String
	 */
	public String getValidIPAddress(IpAddressType theIpAddressType) {
		Enumeration<NetworkInterface> aNetworkInterfaceEnumeration = null;
		try {
			aNetworkInterfaceEnumeration = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException ex) {
			if (l3i_DebugOn) {
				Logger.getLogger(Layer3Info.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		while (aNetworkInterfaceEnumeration.hasMoreElements()) {
			NetworkInterface aNetworkInterface = aNetworkInterfaceEnumeration.nextElement();
			Enumeration<InetAddress> aInetAddressEnumeration = aNetworkInterface.getInetAddresses();
			while (aInetAddressEnumeration.hasMoreElements()) {
				InetAddress inetAddress = aInetAddressEnumeration.nextElement();
				if (!inetAddress.isLoopbackAddress()) {
					return inetAddress.getHostAddress().toString();
				}
			}
		}

		return null;
	}
}
