package com.mycompany.ring_elector_server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runnable qui une fois lancé va pinguer le serveur que electionManager considère comme l'élu.
 * 
 * Si l'élu n'est pas encore choisit alors on attend quelques ms avant l'envoi du prochain ping
 * Si l'élu est cette instance même alors elle ne fait que répondre aux ping
 * 
 * Si un "client" ne reçoit aucune réponse du serveur après un certain temps,
 * on considère que l'élu est en panne et on lance une élection.
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
    

    /**
     * constructeur
     * 
     * @param ownServer représente notre serveur
     * @param electionManager gestionnaire d'élection qui
     * permet de lancer une nouvelle élection
     * @throws SocketException Si on ne parvient pas à créer la socket
     */
    public PingCoordinatorManager(ServerDAO ownServer, ElectionManager electionManager) throws SocketException {
        mySelf = ownServer;
        buffer = new byte[1];
        running = true;
        socket = new DatagramSocket(PORT_PING + ownServer.getId());
        this.electionManager = electionManager;
    }

    /**
     * Méthode tournant en boucle et essayant régulièrement de récupérer
     * l'élu et s'il existe de lui envoyer un ping
     * S'il n'existe pas, lance une nouvelle élection
     */
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
                // une élection devrait être en cours, il ne sert a rien de pinger le serveur
                // le mieux est donc de ne pas surcharger le traffic et d'attendre quelques ms que l'élection se termine.
                
            }
            
            try {
                Thread.sleep(electionManager.getAverageElectionTime());
            } catch (InterruptedException ex1) {
                Logger.getLogger(PingCoordinatorManager.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
    }
    
    /**
     * permet d'arrêter proprement se Runnable
     */
    public void stop() {
        running = false;
    }

    /**
     * Envoi un ping au serveur donné en paramètre et attend une réponse,
     * Si la réponse met trop de temps, lance une élection
     * 
     * @param coordinator serveur que l'on considère comme élu
     */
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

    /**
     * reçoit les pings et y répond immédiatement
     */
    private void receivePingFromOthers() {
        DatagramPacket packet = new DatagramPacket(buffer, 1);
        try {
            socket.setSoTimeout(0);
            socket.receive(packet);
            
            byte[] message = new byte[1];
            message[0] = Ping.RECEIVE.value;
            DatagramPacket datagram = new DatagramPacket(message, 1, packet.getAddress(), packet.getPort());
        } catch (IOException ex) {
            Logger.getLogger(PingCoordinatorManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
