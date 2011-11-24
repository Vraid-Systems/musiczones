/*
 * multicast client used for sending control datagrams to LAN group
 */
package zonecontrol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jason Zerbe
 */
public class ZoneMulticastClient {

    private MulticastSocket clientSocket = null;

    public ZoneMulticastClient() {
        try {
            clientSocket = new MulticastSocket();
        } catch (IOException ex) {
            Logger.getLogger(
                    ZoneMulticastClient.class.getName()).log(
                    Level.SEVERE, null, ex);
        }

        try {
            clientSocket.setTimeToLive(
                    ZoneConstants.getInstance().getGroupTTLInt());
        } catch (IOException ex) {
            Logger.getLogger(
                    ZoneMulticastClient.class.getName()).log(
                    Level.SEVERE, null, ex);
        }

        System.out.println("ZMC socket ready");
    }

    /**
     * send a network command (string) to the currently connected multicast group
     * @param theNetworkCommand String
     * @param printNetworkCommandToTerminal boolean
     * @return boolean - was the command sent?
     */
    public boolean sendNetworkCommand(final String theNetworkCommand,
            boolean printNetworkCommandToTerminal) {
        final InetAddress groupAddress;
        try {
            groupAddress = InetAddress.getByName(
                    ZoneConstants.getInstance().getGroupAddressStr());
        } catch (UnknownHostException ex) {
            Logger.getLogger(
                    ZoneMulticastClient.class.getName()).log(
                    Level.SEVERE, null, ex);
            return false;
        }

        final byte[] buffer = theNetworkCommand.getBytes();
        final DatagramPacket pack = new DatagramPacket(
                buffer, buffer.length, groupAddress,
                ZoneConstants.getInstance().getGroupPortInt());
        try {
            clientSocket.send(pack);
        } catch (IOException ex) {
            Logger.getLogger(
                    ZoneMulticastClient.class.getName()).log(
                    Level.SEVERE, null, ex);
            return false;
        }

        if (printNetworkCommandToTerminal) {
            System.out.println(new Date().toString()
                    + " - sent:\n" + theNetworkCommand);
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
