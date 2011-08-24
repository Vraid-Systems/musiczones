/*
 * interface for storing various program wide constants
 */
package multicastmusiccontroller;

/**
 *
 * @author Jason Zerbe
 */
public interface ProgramConstants {

    //multicast zone controller
    public static final String groupAddressStr = "224.0.0.198";
    public static final int groupPortInt = 28845;
    public static final int groupTTL = 4; //see http://tldp.org/HOWTO/Multicast-HOWTO-2.html#ss2.3
    public static final int groupMaxByteSize = 65535;
    public static final String defaultMPlayerBinPath = "/usr/bin/mplayer";
    public static final int defaultWebServerPort = 80;
    //zone controller misc
    public static final int allNodesPingInterval = 25; //number of seconds between existence notify
    public static final int allNodesExpireInterval = 15; //seconds before node record allowed to be overwritten
    public static final int allNodesHardExpire = 40; //seconds before node is considered offline
    //jetty web server
    public static final String webAppContextPathStr = "/";
    public static final String webAppDirStr = "../webapp"; //root is the directory or the Jetty calling class
    //for saving/loading prefs
    public static final String prefMediaPlayerPathKeyStr = "media-player-bin-path";
    public static final String prefMediaDirNumberKeyStr = "media-dir-count";
    public static final String prefMediaDirPrefixKeyStr = "media-dir-";
    //for network media
    public static final String prefixUriStr = "://";
    public static enum ServerType {smb}
}
