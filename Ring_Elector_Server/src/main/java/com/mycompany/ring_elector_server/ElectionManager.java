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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jimmy Verdasca et Nathan Gonzales
 */
public class ElectionManager implements Runnable {

    private final int TIME_OUT = 1000;
    private long AVERAGE_ELECTION_TIME = 300;
    private final ServerDAO mySelf;
    private final ServerDAO[] servers;
    private final DatagramSocket socket;
    private final byte[] buffer;
    private final int MAX_NB_SERVER;
    private boolean running;
    private int nextServerAvailable;
    private Phase phase;
    private ServerDAO elected;
    
    public ElectionManager(ServerDAO ownServer, ServerDAO[] servers) throws SocketException {
        this.mySelf = ownServer;
        this.servers = servers;
        this.MAX_NB_SERVER = servers.length;
        // on écoute sur le port + MAX_NB_SERVER pour les élections
        // ainsi le socket est définit avec un setSoTimeOut lors de la durée
        // de l'élection afin de détecter si un serveur tombe en panne
        socket = new DatagramSocket(mySelf.getPort() + MAX_NB_SERVER);
        buffer = new byte[1 + MAX_NB_SERVER];
        phase = Phase.ELECTION_PHASE;
        running = true;
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
            nextServerAvailable = (myIndex + 1) % servers.length;
        }
    }
    
    public void initialize() {
        elected = null;
        updateNextServer();
    }
    
    @Override
    public void run() {
        while (running) {
            try {
                processMessage(receiveMessage(0));
            } catch (IOException ex) {
                System.out.println(ex);
            }
            
        }
    }
    
    private void processMessage(Message message) throws ProtocolException, IOException {
        switch (message.getMessageType()) {
            case ELECTION:
                ServerDAO candidat = servers[message.getCandidat()];
                electionReceived(candidat);
                break;
            case RESPONSE:
                throw new ProtocolException("Received a RESPONSE without sending any ELECTION or RESULT");
            case RESULT:
                ServerDAO elected = servers[message.getCandidat()];
                resultReceived(elected);
                break;
        }
    }
    
    private void electionReceived(ServerDAO candidat) throws IOException {
        if (candidat == mySelf) {
            elected = mySelf;
            sendResult(elected);
            phase = RESULT_PHASE;
        } else {
            ServerDAO favorit = calculateElected(candidat);
            sendElection(favorit);
            phase = Phase.ELECTION_PHASE;
        }
    }
    
    /**
     * compare le candidat et mySelf pour déterminer
     * lequel est le plus apte à être élu puis retourne le résultat.
     * 
     * @param candidat
     * @return 
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
        }
    }
    
    public ServerDAO getElected() throws IllegalStateException {
        if (phase == Phase.ELECTED_PHASE && elected != null) {
            return elected;
        } else {
            throw new IllegalStateException("getElected should be call only when election is complete");
        }
    }
    
    private void sendResult(ServerDAO elected) throws IOException {
        sendMessage(new Message(MessageType.RESULT, elected), servers[nextServerAvailable]);
    }
    
    private void sendElection(ServerDAO candidat) throws IOException {
        sendMessage(new Message(MessageType.ELECTION, candidat), servers[nextServerAvailable], true);
    }
    
    /**
     * Envoie un Message au ServerDAO destinateur. Si le boolean d'aquitement est activé,
     * on attend de lire un message de type RESPONSE avant de sortir de la méthode.
     * 
     * Si aucune RESPONSE n'est délivré après TIME_OUT milliseconds,
     * on cherche le serveur suivant dans la liste et on retente d'envoyer.
     * 
     * @param message à envoyer
     * @param destServer server à qui l'on souhaite envoyer le message
     * @throws SocketException en cas de problème lors de la création de la socket
     * @throws IOException en cas de soucis lors de l'envoie du paquet
     */
    private void sendMessage(Message message, ServerDAO destServer, boolean aquitmentRequired) throws SocketException, IOException {
        List<Message> messageStock = new ArrayList<>();
        DatagramPacket datagram = new DatagramPacket(message.getMessage(), message.getLength(), destServer.getIpAdress(), destServer.getPort());
        if (aquitmentRequired) {
            boolean aquitmentReceived = false;
            while (!aquitmentReceived) {
                socket.send(datagram);
                try {
                    Message newMessage = receiveMessage(TIME_OUT);
                    if (newMessage.getMessageType().value == MessageType.RESPONSE.value) {
                        aquitmentReceived = true;
                    } else {
                        messageStock.add(newMessage);
                    }
                } catch (SocketTimeoutException ex) {
                    destServer = this.getNextServer(destServer);
                }
            }

            for (Message messageToProcess : messageStock) {
                processMessage(messageToProcess);
            }
        } else {
            socket.send(datagram);
        }
    }
    
    
    /**
     * Par défaut un envoi ce fait sans demande d'aquitement. 
     * cf. sendMessage(Message, ServerDAO, boolean)
     */
    private void sendMessage(Message message, ServerDAO destServer) throws SocketException, IOException {
        this.sendMessage(message, destServer, false);
    }
    
    /**
     * renvoie le serverDAO suivant (avec modulo) parmis les serveurs connus
     * 
     * @param server
     * @return 
     */
    private ServerDAO getNextServer(ServerDAO server) {
        return servers[(server.getId() + 1) % servers.length];
    }
    
    public void stop() {
        running = false;
    }
    
    /**
     * attend de recevoir un Message
     * 
     * @return une classe Message construit à partir du paquet reçu
     * @throws SocketException
     * @throws IOException 
     * @throws SocketTimeoutException si la réception du message prend trop de temps
     */
    private Message receiveMessage(int timeOut) throws IOException {
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.setSoTimeout(timeOut);
        socket.receive(packet);
        Message message = Message.BuildMessage(buffer, servers);
        cleanBuffer();
        return message;
    }
    
    private void cleanBuffer() {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = 0;
        }
    }

    long getAverageElectionTime() {
        // plutôt que de retourner un temps fixe, il serait possible de
        // calculer une moyenne des temps des élections.
        return AVERAGE_ELECTION_TIME;
    }

    void startNewElection() {
        initialize();
        try {
            sendElection(mySelf);
        } catch (IOException ex) {
            Logger.getLogger(ElectionManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
