/*
 * logic for processing the commands send over the network that the
 * server recieves.
 * 
 * THIS IS A SINGLETON IMPLEMENTATION see:
 * http://www.javaworld.com/javaworld/jw-04-2003/jw-0425-designpatterns.html
 */
package zoneserver;

import contrib.Mp3Audio;
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

    private static ZoneServerLogic instance = null;
    protected String global_NodeUUID = null;
    protected String global_NodeName = null;
    protected MulticastClient mcc = null;
    protected HashMap<String, String> global_NodeInfoMap = null;
    protected HashMap<String, Calendar> global_NodeExpireMap = null;
    protected HashMap<String, String> global_NodeDashBoardMap = null;
    protected Timer global_Timer = null;

    protected ZoneServerLogic() {
        global_NodeName = "zone controller ";
        Random generator = new Random(19580427);
        int randomIndex = generator.nextInt(19580427);
        global_NodeName += String.valueOf(randomIndex);
        System.out.println("default zone-name = " + global_NodeName);
        global_NodeUUID = generateNodeUUID();

        global_NodeInfoMap = new HashMap<String, String>();
        global_NodeExpireMap = new HashMap<String, Calendar>();
        global_NodeDashBoardMap = new HashMap<String, String>();
        mcc = new MulticastClient();
        System.out.println("ZSL started");

        global_Timer = new Timer();
        global_Timer.schedule(new AllNodesPingTimerTask(), allNodesPingInterval * 1000);
        System.out.println("ZSL timed events added");
    }

    public static ZoneServerLogic getInstance() {
        if (instance == null) {
            instance = new ZoneServerLogic();
        }
        return instance;
    }

    @Override
    public void finalize() throws Throwable {
        mcc.closeClient();
        super.finalize();
    }

    /**
     * allow for external setting of the zone name and refresh the UUID
     * @param theZoneName String
     */
    public void setZoneName(String theZoneName) {
        global_NodeName = theZoneName;
        global_NodeUUID = generateNodeUUID();
    }

    /**
     * master abstraction for processing any old command that comes in off
     * the multicast group
     * @param theNetworkCommand String
     */
    public void processNetworkCommand(String theNetworkCommand) {
        System.err.println("ZSL recieved network comand to process:\n" + theNetworkCommand + "\n");

        if (!theNetworkCommand.isEmpty() && theNetworkCommand.contains("\n")) {
            String[] theNetworkCommandArray = theNetworkCommand.split("\n");
            if (theNetworkCommandArray.length == 2) { //1-line command
                String[] singleLineStrArray = theNetworkCommandArray[0].split("=");
                if (singleLineStrArray[0].equals("zone")) {
                    if (singleLineStrArray[1].equals("")) { //requires full ID response
                        doNodeUUIDResponse();
                    }
                }
            } else if (theNetworkCommandArray.length == 3) { //2-line command
                String[] firstLineArray = theNetworkCommandArray[0].split("=");
                String[] secondLineArray = theNetworkCommandArray[1].split("=");
                if (firstLineArray[0].equals("zone")) {
                    if (firstLineArray[1].equals(global_NodeUUID)
                            && secondLineArray[0].equals("playurl")
                            && !secondLineArray[1].isEmpty()) { //play an audio URL
                        Mp3Audio aMp3Audio = new Mp3Audio();
                        doNodePlayUrl(secondLineArray[1], aMp3Audio.playURL(secondLineArray[1])); //notify the group of what is playing
                    } else if (!firstLineArray[1].equals(global_NodeUUID)
                            && secondLineArray[0].equals("name")
                            && !secondLineArray[1].isEmpty()) { //store full ID response in table
                        if (!global_NodeInfoMap.containsKey(firstLineArray[1]) || nodeIsExpired(firstLineArray[1])) {
                            global_NodeInfoMap.put(firstLineArray[1], secondLineArray[1]);
                        }
                    }
                }
            }
        }
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
        return global_NodeInfoMap;
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
        return global_NodeDashBoardMap;
    }

    /**
     * constructs and sends a datagram to the multicast group with
     * all relevant identifiers
     */
    private void doNodeUUIDResponse() {
        String theResponseStr = "zone=" + global_NodeUUID + "\n"
                + "name=" + global_NodeName + "\n";
        mcc.sendNetworkCommand(theResponseStr);
    }

    /**
     * constructs and sends a datagram that notifies the group that the Zone
     * Controller is currently playing a certain URL
     * @param thePlayingUrlStr String
     * @param theUrlIsPlaying boolean
     */
    private void doNodePlayUrl(String thePlayingUrlStr, boolean theUrlIsPlaying) {
        int status = 1;
        if (theUrlIsPlaying) {
            status = 0;
        }

        String theResponseStr = "zone=" + global_NodeUUID + "\n"
                + "playurl=" + thePlayingUrlStr + "\n"
                + "status=" + String.valueOf(status) + "\n";
        mcc.sendNetworkCommand(theResponseStr);
    }

    /**
     * check if the give node (via UUID) is past the expiration time (TTL)
     * @param theNodeUUID String
     * @return boolean - is the node record past expiration?
     */
    private boolean nodeIsExpired(String theNodeUUID) {
        Calendar theCurrentCalendar = new GregorianCalendar();
        theCurrentCalendar.setTimeInMillis(theCurrentCalendar.getTimeInMillis() - (allNodesExpireInterval * 1000));
        for (String aNodeUUIDStr : global_NodeExpireMap.keySet()) {
            if (global_NodeExpireMap.get(aNodeUUIDStr).before(theCurrentCalendar)) {
                return true;
            }
        }
        return false;
    }

    /**
     * generates a UUID from the startup time, and the name of node
     * if they are available
     * @return String - the node UUID
     */
    private String generateNodeUUID() {
        Date aDate = new Date();
        String stringToHash = global_NodeName + " was started on "
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
        System.out.println("Node UUID = " + hashedString);

        return hashedString;
    }

    /**
     * private TimeTask class that is scheduled to notify all the nodes in the
     * multicast group about the existence of this Zone Controller
     */
    private class AllNodesPingTimerTask extends TimerTask {

        public void run() {
            doNodeUUIDResponse();
        }
    }
}
