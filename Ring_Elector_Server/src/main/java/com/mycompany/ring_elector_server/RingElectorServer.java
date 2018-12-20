package com.mycompany.ring_elector_server;

import java.io.IOException;
import static java.lang.System.exit;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

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
 * Le réseau est considéré comme infaillible pour ce labo
 * 
 * @author Jimmy Verdasca et Nathan Gonzales
 */
public class RingElectorServer {
    private final Thread electionManagerThread;
    private final Thread pingCoordinatorManager;
    private final ElectionManager electionManager;
    private final PingCoordinatorManager pingCoordinator;

    /**
     * constructeur
     * 
     * @param ownServer ServerDAO représentant notre instance
     * @param servers table de correspondance id->ServerDAO devant contenir 
     * ServerDAO aussi
     * @throws SocketException Si la création de notre propre socket échoue
     */
    public RingElectorServer(ServerDAO ownServer, ServerDAO[] servers)
            throws SocketException {
        this.electionManager = new ElectionManager(ownServer, servers);
        this.electionManagerThread = new Thread(electionManager);
        this.pingCoordinator = new PingCoordinatorManager(ownServer,
                                                        electionManager);
        this.pingCoordinatorManager = new Thread(pingCoordinator);
    }
    
    /**
     * lance les threads et démmarre une élection
     */
    public void start() {
        electionManagerThread.start();
        pingCoordinatorManager.start();
        electionManager.startNewElection();
    }
    
    public void stop() {
        electionManager.stop();
        pingCoordinator.stop();
    }
    
    /**
     * main lançant le serveur et donc ces threads
     * Lit le fichier structure.txt pour y récupérer les informations sur les
     * serveurs existant
     * 
     * @param args ID du serveur qu'on lance
     * @throws SocketException S'il est impossible de créer les sockets de ce serveur
     */
    public static void main (String[] args) throws SocketException{
        final String FILE_NAME = "./structure.txt";
        
        Scanner in = new Scanner(System.in);
        System.out.println("Select a from server 0 to 3: ");
        int id = in.nextInt();
        
        ServerDAO servers[];
        List<ServerDAO> sList = new ArrayList<>();
        
        try (Stream<String> stream = Files.lines(Paths.get(FILE_NAME))) {
            long count = stream.count();
            servers = new ServerDAO[(int) count];
            
            try (Stream<String> stream2 = Files.lines(Paths.get(FILE_NAME))) {
            stream2.forEach(line -> {
                try {
                    String[] infosServer = line.split(" ");
                    sList.add(new ServerDAO(InetAddress.getByName(infosServer[0]),
                                        Integer.parseInt(infosServer[1]),
                                        Integer.parseInt(infosServer[2])));
                } catch (UnknownHostException ex) {
                    Logger.getLogger(ServerDAO.class.getName())
                          .log(Level.SEVERE, null, ex);
                    exit(1);
                }
            });
            
            for (int cnt = 0; cnt < sList.size(); cnt++) {
                servers[cnt] = sList.get(cnt);
            }
        
            ServerDAO mySelf = servers[id];
            RingElectorServer server = new RingElectorServer(mySelf, servers);
            server.start();

            } catch (IOException e) {
                    e.printStackTrace();
            }
        } catch (IOException e) {
                e.printStackTrace();
        }
        
        
    }
}
