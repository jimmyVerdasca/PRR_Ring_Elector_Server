package com.mycompany.ring_elector_server;

import java.net.SocketException;

/**
 * classe servant de serveur se connectant à d'autres serveurs et implémentant
 * un système d'élection en anneaux et pinguant régulièrement l'élu
 * 
 * Elle lance deux threads
 * Le premier implémente l'algorithme d'élection en anneaux avec panne
 * Le second sert uniquement à pinguer régulièrement le serveur élu par
 * le précédent algorithme
 * 
 * L'élection est donc lancée au démarrage de ce serveur
 * 
 * @author Jimmy Verdasca et Nathan Gonzales
 */
public class RingElectorServer {
    private final Thread electionManagerThread;
    private final Thread pingCoordinatorManager;
    private final ElectionManager electionManager;

    /**
     * constructeur
     * 
     * @param ownServer ServerDAO représentant notre instance
     * @param servers table de correspondance id->ServerDAO devant contenir ServerDAO aussi
     * @throws SocketException Si la création de notre propre socket échoue
     */
    public RingElectorServer(ServerDAO ownServer, ServerDAO[] servers) throws SocketException {
        this.electionManager = new ElectionManager(ownServer, servers);
        this.electionManagerThread = new Thread(electionManager);
        this.pingCoordinatorManager = new Thread(new PingCoordinatorManager(ownServer, electionManager));
    }
    
    /**
     * lance les threads et démmarre une élection
     */
    public void start() {
        electionManagerThread.start();
        pingCoordinatorManager.start();
        electionManager.startNewElection();
        
    }
}
