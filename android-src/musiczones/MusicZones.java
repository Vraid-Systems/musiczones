/**
 * main class for storing run-time parameters
 */
package musiczones;

/**
 * @author Jason Zerbe
 */
public class MusicZones {
	protected static boolean isDebugOn = false;
	protected static boolean isLowMem = false;
	protected static boolean isOnline = true;
	protected static boolean isIndexLocalHost = false;
	protected static int webInterfacePortInt = 80;

	public static boolean getIsDebugOn() {
		return isDebugOn;
	}

	public static void setIsDebugOn(boolean theDebugIsOn) {
		isDebugOn = theDebugIsOn;
	}

	public static boolean getIsLowMem() {
		return isLowMem;
	}

	public static void setIsLowMem(boolean theIsLowMemOn) {
		isLowMem = theIsLowMemOn;
	}

	public static boolean getIsOnline() {
		return isOnline;
	}

	public static void setIsOnline(boolean theIsOnlineFlag) {
		isOnline = theIsOnlineFlag;
	}

	public static boolean getIsIndexLocalHost() {
		return isIndexLocalHost;
	}

	public static void setIndexLocalHost(boolean theIndexLocalHostFlag) {
		isIndexLocalHost = theIndexLocalHostFlag;
	}

	public static int getWebInterfacePort() {
		return webInterfacePortInt;
	}

	public static void setWebInterfacePort(int theWebInterfacePortInt) {
		webInterfacePortInt = theWebInterfacePortInt;
	}
}
