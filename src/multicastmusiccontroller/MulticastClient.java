/*
 * multicast client used for sending control datagrams to LAN group
 */
package multicastmusiccontroller;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jason Zerbe
 */
public class MulticastClient implements ProgramConstants {

    private MulticastSocket clientSocket = null;

    public MulticastClient() {
        try {
            clientSocket = new MulticastSocket();
        } catch (IOException ex) {
            Logger.getLogger(MulticastClient.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        try {
            clientSocket.setTimeToLive(groupTTL);
        } catch (IOException ex) {
            Logger.getLogger(MulticastClient.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
    }

    /**
     * send a network command (string) to the currently connected multicast group
     * @param theNetworkCommand String
     * @return boolean - was the command sent?
     */
    public boolean sendNetworkCommand(String theNetworkCommand) {
        byte[] buffer = new byte[65535];
        buffer = theNetworkCommand.getBytes();

        InetAddress groupAddress;
        try {
            groupAddress = InetAddress.getByName(groupAddressStr);
        } catch (UnknownHostException ex) {
            Logger.getLogger(MulticastClient.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        DatagramPacket pack = new DatagramPacket(buffer, buffer.length, groupAddress, groupPortInt);
        try {
            clientSocket.send(pack);
        } catch (IOException ex) {
            Logger.getLogger(MulticastClient.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        return true;
    }

    /**
     * close up the socket and free up the memory reference
     * @return boolean - did the client close?
     */
    public boolean closeClient() {
        clientSocket.close();
        clientSocket = null;
        return true;
    }
}
