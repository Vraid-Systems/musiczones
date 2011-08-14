/*
 * logic for processing the commands send over the network that the
 * server recieves
 */
package multicastmusiccontroller;

/**
 *
 * @author Jason Zerbe
 */
public class ServerLogic {

    protected String global_NodeUUID = null;
    protected VlcController global_VlcController = null;
    protected String global_NodeName = null;

    public ServerLogic(String theNodeUUID, VlcController theVlcController, String theNodeName) {
        global_NodeUUID = theNodeUUID;
        global_VlcController = theVlcController;
        global_NodeName = theNodeName;
    }
}
