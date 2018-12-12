package com.mycompany.ring_elector_server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * classe servant de serveur,
 * implémentant l'algorithme d'élection en anneaux avec gestion des pannes.
 * 
 * Le serveur élu ne fait que répondre qu'il est présent aux serveurs qui l'appel.
 * Tandis que les serveurs non-élus vont régulièrement demander
 * au serveur élu s'il est toujours en fonction.
 * 
 * Lors de l'initialisation d'un tel serveur, le serveur lance une election.
 * Lors d'une panne, Si la machine en panne est l'élu,
 * alors le premier serveur qui se rend compte de la panne, lance une election.
 * Si une panne survient sur une machine non-élu, cela n'a pas d'impact,
 * jusqu'à son reboot qui lance une election.
 * 
 * Pour ce laboratoire, nous considérons que le réseau ne pause aucun soucit.
 * 
 * @author Jimmy Verdasca et Nathan Gonzales
 */
public class RingElectorServer {
    private final ServerDAO mySelf;
    private ServerDAO nextServerAvailable;
    private final ServerDAO[] servers;

    public RingElectorServer(ServerDAO ownServer, ServerDAO[] servers) {
        this.mySelf = ownServer;
        this.servers = servers;
        
        
    }
    
    private int findIndexOf(ServerDAO server) {
        for (int index = 0; index < servers.length; index++) {
            if(server.equals(servers[index])) {
                return index;
            }
        }
        return -1;
    }
    
    private void updateNextServer() {
        int myIndex = findIndexOf(mySelf);
        
        if(myIndex == -1){
            throw new RuntimeException("ownServer not contained in servers list");
        } else {
            nextServerAvailable = servers[(myIndex + 1) % servers.length];
        }
    }

    public void initialize() {
        updateNextServer();
    }
    
    public void startElection() {
        // envoyer au suivant soit-même ou meilleur élu que soit
        DatagramSocket udpSocket = null;

        try {
            udpSocket = new DatagramSocket();
            udpSocket.bind(new InetSocketAddress(nextServerAvailable.getPort()));

            InetAddress ipAddress = nextServerAvailable.getIpAdress();
            /**
             * 3 types de messages sont possibles :
             * 
             * ELECTION + liste des candidats
             * lorsqu'un nouveau serveur est disponible on fait un tour
             * des serveurs pour savoir qui est disponible et qui devrait être l'élu.
             * 
             * RESPONSE
             * Lorsqu'un serveur reçoit un message que ce soit ELECTION ou RESULT,
             * Il répond par un RESPONSE sans paramètres après avoir transmis
             * le message plus loin dans la chaine.
             * Uniquement pour que le serveur le précédent dans l'anneau,
             * sache que le message à bien été transmit.
             * 
             * RESULT + élu final
             * lorsque le message ELECTION parvient jusqu'à un serveur qui se considère comme élu
             * pour se considérer comme élu :
             * 1) il doit se trouver dans la liste
             * 2) il doit avoir le plus grande aptitude
             * 3) en cas d'égalité avoir la plus petite adress IP
             * Lorsque le message RESULT revient à l'élu, alors l'élection est
             * terminé et l'unicité et la sélectivité sont respectés.
             */
            //DatagramPacket udpSendPacket = new DatagramPacket(sendMessage, sendMessage.length, ipAddress, SEND_PORT);

            // udp send
            //udpSocket.send(udpSendPacket);


            // udp receive
            byte[] receiveMessage = new byte[100];
            DatagramPacket udpReceivePacket = new DatagramPacket(receiveMessage, receiveMessage.length);

            long startTime = System.currentTimeMillis();
            while ((System.currentTimeMillis() - startTime) < 5000) {
                udpSocket.receive(udpReceivePacket);
            }
            udpSocket.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (udpSocket != null) {
                udpSocket.close();
            }
        }
    }
    
    public void sendMessage() {
        
    }
    
    public void receiveMessage() {
        
    }
    
}
