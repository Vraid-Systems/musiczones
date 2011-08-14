/*
 * class for a multicast server that accepts and processes
 * UDP datagrams for controlling VLC
 * 
 * for more on IPv4 Multicast addresses see
 * http://www.iana.org/assignments/multicast-addresses/multicast-addresses.xml
 * 
 * this class is largely based off of
 * http://www.roseindia.net/java/example/java/net/udp/UDPMulticastServer.shtml
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
public class MulticastServer {

    protected Thread serverThread = null;
    protected MulticastSocket serverSocket = null;
    protected ServerLogic serverLogic = null;

    public MulticastServer(ServerLogic theServerLogic) {
        serverLogic = theServerLogic;
    }

    public void StopServer() {
        serverThread.interrupt();
        try {
            serverThread.join();
        } catch (InterruptedException ex) {
            Logger.getLogger(MulticastServer.class.getName()).log(Level.SEVERE, null, ex);
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
            int port = 28845;
            String groupAddressStr = "224.0.0.198";
            try {
                serverSocket = new MulticastSocket(port);
            } catch (IOException ex) {
                Logger.getLogger(MulticastServer.class.getName()).log(Level.SEVERE, null, ex);
            }
            InetAddress groupAddress = null;
            try {
                groupAddress = InetAddress.getByName(groupAddressStr);
            } catch (UnknownHostException ex) {
                Logger.getLogger(MulticastServer.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                serverSocket.joinGroup(groupAddress);
            } catch (IOException ex) {
                Logger.getLogger(MulticastServer.class.getName()).log(Level.SEVERE, null, ex);
            }
            while (true) {
                // receive request from client
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, groupAddress, port);
                try {
                    serverSocket.receive(packet);
                } catch (IOException ex) {
                    Logger.getLogger(MulticastServer.class.getName()).log(Level.SEVERE, null, ex);
                }

                // process said request
                String clientAddressStr = packet.getAddress().toString();
                String recievedStr = new String(buffer).trim();

                InetAddress client = packet.getAddress();
                int client_port = packet.getPort();
                // send information to the client
                String message = "your request\n ";
                buffer = message.getBytes();
                packet = new DatagramPacket(buffer, buffer.length, client, client_port);
                try {
                    serverSocket.send(packet);
                } catch (IOException ex) {
                    Logger.getLogger(MulticastServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
}
