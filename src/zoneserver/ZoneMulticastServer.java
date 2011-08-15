/*
 * class for a multicast server that accepts and processes
 * UDP datagrams for control logic
 * 
 * for more on IPv4 Multicast addresses see
 * http://www.iana.org/assignments/multicast-addresses/multicast-addresses.xml
 * 
 * this class is largely based off of
 * http://www.roseindia.net/java/example/java/net/udp/UDPMulticastServer.shtml
 */
package zoneserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import multicastmusiccontroller.ProgramConstants;

/**
 *
 * @author Jason Zerbe
 */
public class ZoneMulticastServer implements ProgramConstants {

    protected Thread serverThread = null;
    protected MulticastSocket serverSocket = null;
    protected ZoneServerLogic serverLogic = null;

    public ZoneMulticastServer(ZoneServerLogic theServerLogic) {
        serverLogic = theServerLogic;
    }

    public void StopServer() {
        serverThread.interrupt();
        try {
            serverThread.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(ZoneMulticastServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        serverSocket.close();

        serverSocket = null;
        serverThread = null;
    }

    public class StartServer implements Runnable {

        StartServer() {
            serverThread = new Thread(this);
            serverThread.start();
        }

        @Override
        public void run() {
            byte[] buffer = new byte[65535];

            try {
                serverSocket = new MulticastSocket(groupPortInt);
            } catch (IOException ex) {
                Logger.getLogger(ZoneMulticastServer.class.getName()).log(Level.SEVERE, null, ex);
            }

            try {
                serverSocket.setTimeToLive(groupTTL);
            } catch (IOException ex) {
                Logger.getLogger(ZoneMulticastServer.class.getName()).log(Level.SEVERE, null, ex);
            }

            InetAddress groupAddress = null;
            try {
                groupAddress = InetAddress.getByName(groupAddressStr);
            } catch (UnknownHostException ex) {
                Logger.getLogger(ZoneMulticastServer.class.getName()).log(Level.SEVERE, null, ex);
            }

            try {
                serverSocket.joinGroup(groupAddress);
            } catch (IOException ex) {
                Logger.getLogger(ZoneMulticastServer.class.getName()).log(Level.SEVERE, null, ex);
            }

            System.out.println("MulticastServer started and joined group");

            while (true) {
                // receive request from client
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, groupAddress, groupPortInt);
                try {
                    serverSocket.receive(packet);
                } catch (IOException ex) {
                    Logger.getLogger(ZoneMulticastServer.class.getName()).log(Level.SEVERE, null, ex);
                }

                // process said request
                String netCmd = new String(buffer).trim().toLowerCase();
                if (!serverLogic.processNetworkCommand(netCmd)) {
                    System.err.println("failed to process network comand:\n" + netCmd + "\n");
                }
            }
        }
    }
}
