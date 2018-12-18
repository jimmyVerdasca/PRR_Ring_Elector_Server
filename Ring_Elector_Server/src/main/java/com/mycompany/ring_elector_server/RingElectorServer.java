package com.mycompany.ring_elector_server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProtocolException;
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
    private final Thread electionManagerThread;
    private final Thread pingCoordinatorManager;
    private final ElectionManager electionManager;

    public RingElectorServer(ServerDAO ownServer, ServerDAO[] servers) throws SocketException {
        this.electionManager = new ElectionManager(ownServer, servers);
        this.electionManagerThread = new Thread(electionManager);
        this.pingCoordinatorManager = new Thread(new PingCoordinatorManager(ownServer, electionManager));
    }
    
    public void start() {
        electionManagerThread.start();
        pingCoordinatorManager.start();
        electionManager.startNewElection();
        
    }
}
