/*
 * logic for processing the commands send over the network that the
 * server recieves
 */
package zoneserver;

import contrib.Mp3Audio;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import multicastmusiccontroller.MulticastClient;
import multicastmusiccontroller.ProgramConstants;

/**
 *
 * @author Jason Zerbe
 */
public class ZoneServerLogic implements ProgramConstants {

    protected String global_NodeUUID = null;
    protected String global_NodeName = null;
    protected MulticastClient mcc = null;
    protected HashMap<String, String> global_NodeInfoMap = null;
    protected HashMap<String, Calendar> global_NodeExpireMap = null;
    protected Timer global_Timer = null;

    public ZoneServerLogic(String theNodeUUID, String theNodeName) {
        global_NodeInfoMap = new HashMap<String, String>();
        global_NodeExpireMap = new HashMap<String, Calendar>();
        global_NodeUUID = theNodeUUID;
        global_NodeName = theNodeName;
        mcc = new MulticastClient();
        System.out.println("ZMS Logic started");

        global_Timer = new Timer();
        global_Timer.schedule(new AllNodesPingTimerTask(), allNodesPingInterval * 1000);
        System.out.println("ZMS Logic timed events added");
    }

    @Override
    public void finalize() throws Throwable {
        mcc.closeClient();
        super.finalize();
    }

    /**
     * master abstraction for processing any old command that comes in off
     * the multicast group
     * @param theNetworkCommand String
     * @return boolean - were we able to process the command?
     */
    public boolean processNetworkCommand(String theNetworkCommand) {
        System.err.println("ZMS Logic recieved network comand to process:\n" + theNetworkCommand + "\n");

        if (!theNetworkCommand.isEmpty() && theNetworkCommand.contains("\n")) {
            String[] theNetworkCommandArray = theNetworkCommand.split("\n");
            if (theNetworkCommandArray.length == 2) { //single-line command
                String[] singleLineStrArray = theNetworkCommandArray[0].split("=");
                if (singleLineStrArray[0].equals("zone")) {
                    if (singleLineStrArray[1].equals("")) { //reply with my ID
                        doNodeUUIDResponse();
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else if (theNetworkCommandArray.length == 3) { //2-line command
                String[] firstLineArray = theNetworkCommandArray[0].split("=");
                String[] secondLineArray = theNetworkCommandArray[1].split("=");
                if (firstLineArray[0].equals("zone")) {
                    if (firstLineArray[1].equals(global_NodeUUID)
                            && secondLineArray[0].equals("playurl")
                            && !secondLineArray[1].isEmpty()) { //get the url and play it
                        doNodePlayUrl(secondLineArray[1]); //notify the group of what is playing

                        Mp3Audio aMp3Audio = new Mp3Audio();
                        aMp3Audio.playURL(secondLineArray[1]);
                    } else if (!firstLineArray[1].equals(global_NodeUUID)
                            && secondLineArray[0].equals("name")
                            && !secondLineArray[1].isEmpty()) { //store in node lookup table
                        if (!global_NodeInfoMap.containsKey(firstLineArray[1]) || nodeIsExpired(firstLineArray[1])) {
                            global_NodeInfoMap.put(firstLineArray[1], secondLineArray[1]); //set or update the record
                        }
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }

        return true;
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
     */
    private void doNodePlayUrl(String thePlayingUrlStr) {
        String theResponseStr = "zone=" + global_NodeUUID + "\n"
                + "playurl=" + thePlayingUrlStr + "\n"
                + "status=0\n";
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
     * private TimeTask class that is scheduled to notify all the nodes in the
     * multicast group about the existence of this Zone Controller
     */
    private class AllNodesPingTimerTask extends TimerTask {

        public void run() {
            doNodeUUIDResponse();
        }
    }
}
