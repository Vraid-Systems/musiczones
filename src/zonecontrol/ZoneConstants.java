/*
 * singelton class for managing Zone group constants
 */
package zonecontrol;

/**
 * @author Jason Zerbe
 */
public class ZoneConstants {

    private static ZoneConstants zc_SingleInstance = null;

    protected ZoneConstants() {
    }

    public static ZoneConstants getInstance() {
        if (zc_SingleInstance == null) {
            zc_SingleInstance = new ZoneConstants();
        }
        return zc_SingleInstance;
    }

    public String getGroupAddressStr() {
        return "224.0.0.198";
    }

    public int getGroupMaxByte() {
        return 65535;
    }

    public int getGroupPortInt() {
        return 28845;
    }

    public int getGroupTTLInt() {
        return 4;
    }
}
