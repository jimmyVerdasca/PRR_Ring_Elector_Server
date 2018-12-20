package com.mycompany.ring_elector_server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import static com.mycompany.ring_elector_server.Phase.RESULT_PHASE;
import java.net.InetAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gestionnaire d'élection implémentant l'algorithme d'élection avec panne
 * Nous nous sommes basé sur l'algorithme de la dernière page du pdf chapitre 4
 * 
 * @author Jimmy Verdasca et Nathan Gonzales
 */
public class ElectionManager implements Runnable {

    private final int TIME_OUT = 2000;
    private final long AVERAGE_ELECTION_TIME = 300;
    private final ServerDAO mySelf;
    private final ServerDAO[] servers;
    private final DatagramSocket socket;
    private final byte[] buffer;
    private final int MAX_NB_SERVER;
    private boolean running;
    private int nextServerAvailable;
    private Phase phase;
    private ServerDAO elected;
    
    /**
     * constructeur
     * 
     * @param ownServer informations sur notre propre serveur
     * @param servers table de correspondance id->serveur
     * @throws SocketException Si nous ne parvenons pas à créer la socket
     */
    public ElectionManager(ServerDAO ownServer, ServerDAO[] servers)
            throws SocketException {
        this.mySelf = ownServer;
        this.servers = servers;
        this.MAX_NB_SERVER = servers.length;
        socket = new DatagramSocket(mySelf.getPort());
        buffer = new byte[1 + MAX_NB_SERVER];
        phase = Phase.ELECTION_PHASE;
        running = true;
    }
    
    /**
     * Méthode utilitaire permettant de retrouver l'index dans la table de
     * correspondance d'un serveur donné
     * 
     * @param server dont on souhaite trouver l'index
     * @return l'index dans la table de correspondance du serveur donné
     */
    private int findIndexOf(ServerDAO server) {
        for (int index = 0; index < servers.length; index++) {
            if(server.equals(servers[index])) {
                return index;
            }
        }
        return -1;
    }
    
    /**
     * remet à jour le serveur suivant
     */
    private void updateNextServer() {
        int myIndex = findIndexOf(mySelf);
        
        if(myIndex == -1){
            throw new RuntimeException("ownServer not contained in servers list");
        } else {
            nextServerAvailable = (myIndex + 1) % servers.length;
        }
    }
    
    /**
     * met à jour les variable nécessaire pour une nouvelle élection
     */
    public void initialize() {
        elected = null;
        updateNextServer();
    }
    
    /**
     * méthode avec boucle infini qui est à l'écoute en permanence
     * des message reçu et y réagit correctement
     */
    @Override
    public void run() {
        while (running) {
            try {
                System.out.println("WAIT MESSAGE");
                processMessage(receiveMessage(TIME_OUT));
            } catch (IOException ex) {
                System.out.println(ex);
            }
            
        }
    }
    
    /**
     * Méthode implémentant la logique de réponse en fonction d'un message reçu
     * 
     * @param message reçu auquel il faut réagir
     * @throws ProtocolException On ne devrait jamais recevoir de RESPONSE ici
     *         car l'aquittement est géré dans la méthode d'envoi (sendMessage)
     * @throws IOException Si un soucis de réseau survient
     */
    private void processMessage(Message message) throws ProtocolException,
                                                        IOException {
        switch (message.getMessageType()) {
            case ELECTION:
                System.out.println("ELECTION");
                electionReceived(servers[message.getCandidat()]);
                break;
            case RESPONSE:
                System.out.println("RESPONSE");
                throw new ProtocolException("Received a RESPONSE without"
                        + " sending any ELECTION or RESULT");
            case RESULT:
                System.out.println("RESULT");
                resultReceived(servers[message.getCandidat()]);
                break;
        }
    }
    
    /**
     * méthode implémentant la logique de réception et
     * de réaction à un message de type ELECTION
     * 
     * @param candidat candidat reçu du message ELECTION
     * @throws IOException Si un soucis de réseau survient
     */
    private void electionReceived(ServerDAO candidat) throws IOException {
        if (candidat == mySelf) {
            elected = mySelf;
            sendResult(elected);
            phase = RESULT_PHASE;
        } else {
            ServerDAO favorite = calculateElected(candidat);
            sendElection(favorite);
            phase = Phase.ELECTION_PHASE;
        }
    }
    
    /**
     * compare le candidat et mySelf pour déterminer
     * lequel est le plus apte à être élu puis retourne le résultat.
     * 
     * @param candidat à comparer avec soi-même
     * @return le candidat avec la plus grande
     *         aptitude et la plus petite adresse ip
     */
    private ServerDAO calculateElected(ServerDAO candidat) {
        InetAddressComparator compInet = new InetAddressComparator();
        if(mySelf.getAptitude() > candidat.getAptitude() 
            || (mySelf.getAptitude() == candidat.getAptitude() 
            && compInet.compare(mySelf.getIpAdress(), candidat.getIpAdress()) < 0)) {
            candidat = mySelf;
        }
        return candidat;
    }
    
    /**
     * méthode implémentant la logique de réception et
     * de réaction à un message de type RESULT
     * 
     * @param elected élu devant être transmit aux autres serveurs
     * @throws IOException Si un soucis de réseau survient
     */
    private void resultReceived(ServerDAO elected) throws IOException {
        if (phase == Phase.ELECTION_PHASE) {
            this.elected = elected;
            sendResult(elected);
            phase = Phase.RESULT_PHASE;
        } else if (phase == Phase.RESULT_PHASE && elected != this.elected) {
            sendElection(mySelf);
            phase = Phase.ELECTION_PHASE;
        } else {
            phase = Phase.ELECTED_PHASE;
            System.out.println("Élection TERMINÉE");
        }
    }
    
    /**
     * méthode permettant aux classe externe de connaître l'élu
     * 
     * @return l'élu
     * @throws IllegalStateException Tant que l'élu n'est pas validé par l'algorithme
     */
    public ServerDAO getElected() throws IllegalStateException {
        if (phase == Phase.ELECTED_PHASE && elected != null) {
            return elected;
        } else {
            throw new IllegalStateException("getElected should be call only when"
                    + " election is complete");
        }
    }
    
    /**
     * envoie au prochain serveur de l'anneau disponible un message RESULT
     * considérant elected comme l'élu
     * 
     * @param elected élu devant être envoyé aux autres serveurs pour validation
     * @throws IOException Si un soucis de réseau survient
     */
    private void sendResult(ServerDAO elected) throws IOException {
        sendMessage(new Message(MessageType.RESULT, elected),
                    servers[nextServerAvailable]);
    }
    
    /**
     * Envoie au prochain serveur de l'anneau disponible un message ELECTION
     * considérant candidat comme l'actuel favorit
     * 
     * @param candidat l'actuel favorit
     * @throws IOException Si un soucis de réseau survient
     */
    private void sendElection(ServerDAO candidat) throws IOException {
        sendMessage(new Message(MessageType.ELECTION, candidat),
                    servers[nextServerAvailable], true);
    }
    
    /**
     * Envoie un Message au ServerDAO destinateur. Si le boolean d'aquitement
     * est activé, on attend de lire un message de type RESPONSE avant de sortir
     * de la méthode.
     * 
     * Si aucune RESPONSE n'est délivré après TIME_OUT milliseconds,
     * on cherche le serveur suivant dans la liste et on retente d'envoyer.
     * 
     * @param message à envoyer
     * @param destServer server à qui l'on souhaite envoyer le message
     * @param aquitmentRequired boolean a true si on veut un accusé de réception
     * @throws SocketException en cas de problème lors de la création de la socket
     * @throws IOException en cas de soucis lors de l'envoie du paquet
     */
    private void sendMessage(Message message,
                            ServerDAO destServer,
                            boolean aquitmentRequired)
                            throws SocketException, IOException {
        List<Message> messageStock = new ArrayList<>();
        DatagramPacket datagram = new DatagramPacket(message.getMessage(),
                                                    message.getLength(),
                                                    destServer.getIpAdress(),
                                                    destServer.getPort());
        if (aquitmentRequired) {
            boolean aquitmentReceived = false;
            while (!aquitmentReceived) {
                socket.send(datagram);
                System.out.println("Message AVEC aquittement " + message
                                + " envoyé au serveur ip " + destServer.getIpAdress()
                                + " port " + destServer.getPort());
                try {
                    Message newMessage = receiveMessage(TIME_OUT);
                    if (newMessage.getMessageType().value
                            == MessageType.RESPONSE.value) {
                        System.out.println("Aquittement reçu");
                        aquitmentReceived = true;
                    } else {
                        messageStock.add(newMessage);
                    }
                } catch (SocketTimeoutException ex) {
                    System.out.println("ERROR");
                    destServer = this.getNextServer(destServer);
                    if(destServer == mySelf) {
                        // Si on est tout seul, on devient l'élu
                        phase = Phase.ELECTED_PHASE;
                        this.elected = mySelf;
                        aquitmentReceived = true;
                    }
                }
            }

            for (Message messageToProcess : messageStock) {
                processMessage(messageToProcess);
            }
        } else {
            socket.send(datagram);
            System.out.println("Message SANS aquittement " + message
                            + " envoyé au serveur ip " + destServer.getIpAdress()
                            + " port " + destServer.getPort());
        }
    }
    
    
    /**
     * Par défaut un envoi ce fait sans demande d'aquitement. 
     * cf. sendMessage(Message, ServerDAO, boolean)
     * 
     * @param message à envoyer
     * @param destServer server à qui l'on souhaite envoyer le message
     * @throws SocketException en cas de problème lors de la création de la socket
     * @throws IOException en cas de soucis lors de l'envoie du paquet
     */
    private void sendMessage(Message message, ServerDAO destServer)
            throws SocketException, IOException {
        this.sendMessage(message, destServer, false);
    }
    
    /**
     * renvoie le serverDAO suivant (avec modulo) parmis les serveurs connus
     * 
     * @param server dont on souhaite le suivant dans la table de correspondance
     * @return le serverDAO suivant (avec modulo) parmis les serveurs connus
     */
    private ServerDAO getNextServer(ServerDAO server) {
        return servers[(server.getId() + 1) % servers.length];
    }
    
    /**
     * permet de stopper proprement cette classe et donc d'arrêter
     * l'écoute de message
     */
    public void stop() {
        running = false;
    }
    
    /**
     * attend de recevoir un Message, Si on ne souhaite pas de timeOut,
     * il suffit de mettre 0 pour attendre indéfiniment le message
     * 
     * @return une classe Message construit à partir du paquet reçu
     * @throws IOException Si un soucis de réseau survient
     * @throws SocketTimeoutException si la réception du message prend trop de temps
     */
    private Message receiveMessage(int timeOut) throws SocketTimeoutException,
                                                       IOException {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        System.out.println("TIMEOUT: " + timeOut);
        socket.setSoTimeout(timeOut);
        System.out.println("WAIT TO RECEIVE");
        socket.receive(packet);
        System.out.println("DONE");
        Message message = Message.BuildMessage(buffer, servers);
        System.out.println(message + " reçu  du serveur ip : "
                            + packet.getAddress() + " port "
                            + packet.getPort());
        if(message.getMessageType() != MessageType.RESPONSE) {
            System.out.println("SENDING RESPONSE");
            sendResponse(packet.getAddress(), packet.getPort());
            System.out.println("RESPONSE SENT");
        }
        cleanBuffer();
        return message;
    }
    
    /**
     * vide le buffer en y plaçant des valeurs null
     */
    private void cleanBuffer() {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = 0;
        }
    }

    /**
     * retourne le temps moyen d'une élection, serait utile aux classe externe
     * souhaitant lancer une élection puis attendre sa résolution,
     * ni trop longtemps, ni pas assez
     * 
     * @return le temps moyen d'une élection
     */
    long getAverageElectionTime() {
        // plutôt que de retourner un temps fixe, il serait possible de
        // calculer une moyenne des temps des élections.
        return AVERAGE_ELECTION_TIME;
    }

    /**
     * Lance une nouvelle élection
     */
    void startNewElection() {
        System.out.println("Lancement d'une nouvelle élection");
        initialize();
        try {
            sendElection(mySelf);
        } catch (IOException ex) {
            Logger.getLogger(ElectionManager.class.getName())
                  .log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Envoie le message RESPONSE
     * @param address Adresse IP à qui il envoie le message
     * @param port Port à qui il envoie le message
     * @throws IOException 
     */
    private void sendResponse(InetAddress address, int port) throws IOException {
        ServerDAO choice = null;
        InetAddressComparator comp = new InetAddressComparator();
        int i = 0;
        for (ServerDAO server : servers) {
            if (comp.compare(server.getIpAdress(), address) == 0
                    && server.getPort() == port) {
                choice = new ServerDAO(address, port, i);
            }
            i++;
        }
        sendMessage(new Message(), choice);
    }

}
