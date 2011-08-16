/*
 * logic for processing the commands send over the network that the
 * server recieves
 */
package zoneserver;

import contrib.Mp3Audio;
import multicastmusiccontroller.MulticastClient;

/**
 *
 * @author Jason Zerbe
 */
public class ZoneServerLogic {

    protected String global_NodeUUID = null;
    protected String global_NodeName = null;
    protected MulticastClient mcc = null;

    public ZoneServerLogic(String theNodeUUID, String theNodeName) {
        global_NodeUUID = theNodeUUID;
        global_NodeName = theNodeName;
        mcc = new MulticastClient();
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
        System.err.println("recieved network comand to process:\n" + theNetworkCommand + "\n");

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
                if (firstLineArray[0].equals("zone")
                        && firstLineArray[1].equals(global_NodeUUID)
                        && secondLineArray[0].equals("playurl")
                        && !secondLineArray[1].isEmpty()) { //get the url and play it
                    doNodePlayUrl(secondLineArray[1]); //notify the group of what is playing

                    Mp3Audio aMp3Audio = new Mp3Audio();
                    aMp3Audio.playURL(secondLineArray[1]);
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
     * constructs and sends a datagram to the multicast group with
     * all relevant identifiers
     */
    private void doNodeUUIDResponse() {
        String theResponseStr = "zone=" + global_NodeUUID + "\n"
                + "name=" + global_NodeName + "\n";
        mcc.sendNetworkCommand(theResponseStr);
    }

    private void doNodePlayUrl(String thePlayingUrlStr) {
        String theResponseStr = "zone=" + global_NodeUUID + "\n"
                + "playurl=" + thePlayingUrlStr + "\n";
        mcc.sendNetworkCommand(theResponseStr);
    }
}
