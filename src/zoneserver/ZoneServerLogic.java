/*
 * logic for processing the commands send over the network that the
 * server recieves
 * 
 * THIS IS A SINGLETON IMPLEMENTATION see:
 * http://www.javaworld.com/javaworld/jw-04-2003/jw-0425-designpatterns.html
 */
package zoneserver;

import contrib.JettyWebServer;
import contrib.VLCMediaPlayer;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import multicastmusiccontroller.MulticastClient;
import multicastmusiccontroller.ProgramConstants;

/**
 *
 * @author Jason Zerbe
 */
public class ZoneServerLogic implements ProgramConstants {

    private static ZoneServerLogic zsl_SingleInstance = null;
    protected String zsl_ZoneUUID = null;
    protected String zsl_ZoneName = null;
    protected MulticastClient zsl_MulticastClient = null;
    protected HashMap<String, String> zsl_ZoneInfoMap = null;
    protected HashMap<String, Calendar> zsl_ZoneExpireMap = null;
    protected HashMap<String, String> zsl_ZoneDashBoardMap = null;
    protected Timer zsl_Timer = null;

    protected ZoneServerLogic() {
        zsl_ZoneName = "zone controller ";
        Random generator = new Random(new GregorianCalendar().getTimeInMillis());
        int randomIndex = generator.nextInt(19580427);
        zsl_ZoneName += String.valueOf(randomIndex);
        System.out.println("default zone-name = " + zsl_ZoneName);
        zsl_ZoneUUID = generateZoneUUID();

        zsl_ZoneInfoMap = new HashMap<String, String>();
        zsl_ZoneExpireMap = new HashMap<String, Calendar>();
        zsl_ZoneDashBoardMap = new HashMap<String, String>();
        zsl_MulticastClient = new MulticastClient();
        System.out.println("ZSL started");

        zsl_Timer = new Timer();
        zsl_Timer.schedule(new AllNodesPingTimerTask(), 0, allNodesPingInterval * 1000);
        zsl_Timer.schedule(new RemoveHardExpiredNodesTimerTask(), 0, allNodesHardExpire * 1000);
        System.out.println("ZSL timed events added");
    }

    public static ZoneServerLogic getInstance() {
        if (zsl_SingleInstance == null) {
            zsl_SingleInstance = new ZoneServerLogic();
        }
        return zsl_SingleInstance;
    }

    @Override
    public void finalize() throws Throwable {
        zsl_MulticastClient.closeClient();
        super.finalize();
    }

    /**
     * allow for external setting of the zone name and refresh the UUID
     * @param theZoneName String
     */
    public void setZoneName(String theZoneName) {
        zsl_ZoneName = theZoneName;
        zsl_ZoneUUID = generateZoneUUID();
    }

    /**
     * master abstraction for processing any old command that comes in off
     * the multicast group
     * @param theNetworkCommand String
     */
    public void processNetworkCommand(String theNetworkCommand) {
        if (!theNetworkCommand.isEmpty() && theNetworkCommand.contains("\n")) {
            String[] theNetworkCommandArray = theNetworkCommand.split("\n");
            if (theNetworkCommandArray.length == 1) {
                String[] singleLineStrArray = theNetworkCommandArray[0].split("=");
                if (singleLineStrArray[0].equals("zone")) {
                    if (singleLineStrArray[1].equals("")) { //requires full ID response
                        doZoneUUIDResponse();
                    }
                }
            } else if (theNetworkCommandArray.length == 2) {
                String[] firstLineArray = theNetworkCommandArray[0].split("=");
                String[] secondLineArray = theNetworkCommandArray[1].split("=");
                if (firstLineArray[0].equals("zone")) {
                    if (firstLineArray[1].equals(zsl_ZoneUUID)
                            && secondLineArray[0].equals("mediaurl")
                            && !secondLineArray[1].isEmpty()) { //play an audio URL
                        VLCMediaPlayer.getInstance().addMediaUrl(secondLineArray[1]);
                    }
                }
            } else if (theNetworkCommandArray.length == 3) {
                String[] firstLineArray = theNetworkCommandArray[0].split("=");
                String[] secondLineArray = theNetworkCommandArray[1].split("=");
                String[] thirdLineArray = theNetworkCommandArray[2].split("=");
                if ((!firstLineArray[1].equals(zsl_ZoneUUID))
                        && secondLineArray[0].equals("name")
                        && thirdLineArray[0].equals("dashboard")) { //store Zone info
                    if ((!zsl_ZoneInfoMap.containsKey(firstLineArray[1]))
                            || isExpiredZone(firstLineArray[1], allNodesExpireInterval)) {
                        zsl_ZoneInfoMap.put(firstLineArray[1], secondLineArray[1]);
                    }
                    if ((!zsl_ZoneDashBoardMap.containsKey(firstLineArray[1]))
                            || isExpiredZone(firstLineArray[1], allNodesExpireInterval)) {
                        zsl_ZoneDashBoardMap.put(firstLineArray[1], thirdLineArray[1]);
                    }
                }
            }
        }
    }

    /**
     * send a packet to the multicast group telling this node to add a certain
     * URL string of a media resource to the list of media URLs
     * @param theMediaUrlStr String
     */
    public void sendAddMediaUrlStr(String theMediaUrlStr) {
        String theMediaUrlPacketStr = "zone=" + zsl_ZoneUUID + "\n"
                + "mediaurl=" + theMediaUrlStr + "\n";
        zsl_MulticastClient.sendNetworkCommand(theMediaUrlPacketStr);
    }

    /**
     * returns HashMap<String, String> that contains the current Zone Controller
     * information of said Zone Controllers that are connected to the multicast
     * group
     * 
     * TODO: perform a deep copy so that unsuspecting people won't screw up internal references
     * 
     * @return HashMap<String, String>
     */
    public HashMap<String, String> getNodeInfoMap() {
        return zsl_ZoneInfoMap;
    }

    /**
     * returns HashMap<String, String> that contains the web DashBoard LAN
     * addresses for the various Zone Controllers
     * 
     * TODO: perform a deep copy so that unsuspecting people won't screw up internal references
     * 
     * @return HashMap<String, String>
     */
    public HashMap<String, String> getNodeDashBoardMap() {
        return zsl_ZoneDashBoardMap;
    }

    /**
     * constructs and sends a datagram to the multicast group with
     * all relevant identifiers
     */
    private void doZoneUUIDResponse() {
        String aZoneDashBoardStr = "http://"
                + ZoneServerUtility.getInstance().getIPv4LanAddress()
                + ":" + String.valueOf(JettyWebServer.getInstance().getServerPortInt());
        String theResponseStr = "zone=" + zsl_ZoneUUID + "\n"
                + "name=" + zsl_ZoneName + "\n"
                + "dashboard=" + aZoneDashBoardStr + "\n";
        zsl_MulticastClient.sendNetworkCommand(theResponseStr);
    }

    /**
     * check if the given zone (via UUID) is past the expiration time (TTL) passed in
     * @param theZoneUUID String
     * @param timeLapseInSecondsInt integer
     * @return boolean - is the node record past expiration?
     */
    private boolean isExpiredZone(String theZoneUUID, int timeLapseInSecondsInt) {
        Calendar theCurrentCalendar = new GregorianCalendar();
        theCurrentCalendar.setTimeInMillis(theCurrentCalendar.getTimeInMillis() - (timeLapseInSecondsInt * 1000));
        if (zsl_ZoneExpireMap.containsKey(theZoneUUID)) {
            if (zsl_ZoneExpireMap.get(theZoneUUID).before(theCurrentCalendar)) {
                return true;
            }
        }
        return false;
    }

    /**
     * generates a UUID from the startup time, and the name of zone
     * if they are available
     * @return String - the Zone UUID
     */
    private String generateZoneUUID() {
        Date aDate = new Date();
        String stringToHash = zsl_ZoneName + " was started on "
                + aDate.toString() + ".";
        System.out.println(stringToHash);

        String hashedString = null;
        try {
            hashedString = contrib.AeSimpleSHA1.SHA1(stringToHash);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(ZoneServerLogic.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(ZoneServerLogic.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Zone UUID = " + hashedString);

        return hashedString;
    }

    /**
     * private TimeTask class that is scheduled to notify all the nodes in the
     * multicast group about the existence of this Zone Controller
     */
    private class AllNodesPingTimerTask extends TimerTask {

        public void run() {
            doZoneUUIDResponse();
        }
    }

    /**
     * internal task for removing all node information once said node has been
     * unreachable for a certain time
     */
    private class RemoveHardExpiredNodesTimerTask extends TimerTask {

        public void run() {
            for (String aNodeUUID : zsl_ZoneExpireMap.keySet()) {
                if (isExpiredZone(aNodeUUID, allNodesHardExpire)) {
                    zsl_ZoneInfoMap.remove(aNodeUUID);
                    zsl_ZoneDashBoardMap.remove(aNodeUUID);
                    zsl_ZoneExpireMap.remove(aNodeUUID);
                }
            }
        }
    }
}
