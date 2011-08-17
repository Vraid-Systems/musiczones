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
    public static final int maxByteSize = 65535;
    //zone controller misc
    public static final int allNodesPingInterval = 30; //number of seconds between existance notify
    public static final int allNodesExpireInterval = 15; //seconds before node record allowed to be overwritten
    //jetty web server
    public static final String webAppContextPathStr = "/";
    public static final String webAppDirStr = "../webapp"; //root is the src directory
}
