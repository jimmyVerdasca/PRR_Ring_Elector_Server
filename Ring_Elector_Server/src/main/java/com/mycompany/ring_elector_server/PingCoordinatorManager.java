package com.mycompany.ring_elector_server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jimmy Verdasca et Nathan Gonzales
 */
public class PingCoordinatorManager implements Runnable {

    private boolean running;
    private final ElectionManager electionManager;
    private final int PORT_PING = 2000;
    private ServerDAO mySelf;
    private final DatagramSocket socket;
    private byte[] buffer;
    

    public PingCoordinatorManager(ServerDAO ownServer, ElectionManager electionManager) throws SocketException {
        mySelf = ownServer;
        buffer = new byte[1];
        running = true;
        socket = new DatagramSocket(PORT_PING + ownServer.getId());
        this.electionManager = electionManager;
    }

    @Override
    public void run() {
        ServerDAO coordinator;
        while(running) {
            try {
                coordinator = electionManager.getElected();
                if (mySelf == coordinator) {
                    receivePingFromOthers();
                } else {
                    pingCoordinator(coordinator);
                }
            } catch (IllegalStateException ex) {
                try {
                    // une élection devrait être en cours, il ne sert a rien de pinger le serveur
                    // le mieux est donc de ne pas surcharger le traffic et d'attendre quelques ms que l'élection se termine.
                    Thread.sleep(electionManager.getAverageElectionTime());
                } catch (InterruptedException ex1) {
                    Logger.getLogger(PingCoordinatorManager.class.getName()).log(Level.SEVERE, null, ex1);
                    // ne devrait jamais arriver
                }
            }
        }
    }
    
    public void stop() {
        running = false;
    }

    private void pingCoordinator(ServerDAO coordinator) {
        try {
            byte[] message = new byte[1];
            message[0] = Ping.SEND.value;
            DatagramPacket datagram = new DatagramPacket(message, 1, coordinator.getIpAdress(), PORT_PING + coordinator.getId());
            socket.send(datagram);
        } catch (IOException ex) {
            Logger.getLogger(PingCoordinatorManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        DatagramPacket packet = new DatagramPacket(buffer, 1);
        try {
            socket.setSoTimeout(1000);
            socket.receive(packet);
        } catch (SocketTimeoutException ex) {
            electionManager.startNewElection();
        } catch (SocketException ex) {
            Logger.getLogger(PingCoordinatorManager.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PingCoordinatorManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void receivePingFromOthers() {
        DatagramPacket packet = new DatagramPacket(buffer, 1);
        try {
            socket.setSoTimeout(0);
            socket.receive(packet);
            
            byte[] message = new byte[1];
            message[0] = Ping.RECEIVE.value;
            DatagramPacket datagram = new DatagramPacket(message, 1, packet.getAddress(), packet.getPort());
        } catch (SocketException ex) {
            Logger.getLogger(PingCoordinatorManager.class.getName()).log(Level.SEVERE, null, ex);
        }catch (IOException ex) {
            Logger.getLogger(PingCoordinatorManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
