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

/**
 *
 * @author Jimmy Verdasca et Nathan Gonzales
 */
public class ElectionManager implements Runnable {

    private final int TIME_OUT = 1000;
    private final ServerDAO mySelf;
    private final ServerDAO[] servers;
    private final DatagramSocket socket;
    private final byte[] buffer;
    private final int MAX_NB_SERVER;
    private boolean running;
    private int nextServerAvailable;
    private Phase phase;
    
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
    
    private void processMessage(Message message) throws ProtocolException {
        switch (message.getMessageType()) {
            case ELECTION:
                List<ServerDAO> candidats = new ArrayList<>();
                byte[] byteMessage = message.getMessage();
                for (int i = 0; i < message.getLength(); i++) {
                    candidats.add(servers[byteMessage[i + 1]]);
                }
                electionReceived(candidats);
                break;
            case RESPONSE:
                throw new ProtocolException("Received a RESPONSE without sending any ELECTION or RESULT");
            case RESULT:
                ServerDAO elected = servers[message.getMessage()[1]];
                resultReceived(elected);
                break;
        }
    }
    
    private void electionReceived(List<ServerDAO> candidats) {
        if (candidats.contains(mySelf)) {
            ServerDAO elected = calculateElected(candidats);
            sendResultTo(elected);
            phase = RESULT_PHASE;
        } else {
            candidats.add(mySelf);
            sendElection(candidats);
            phase = Phase.ELECTION_PHASE;
        }
    }
    
    private ServerDAO calculateElected(List<ServerDAO> candidats) {
        ServerDAO bestCandidat = candidats.get(0);
        InetAddressComparator compInet = new InetAddressComparator();
        
        for (ServerDAO candidat : candidats) {
            if(bestCandidat.getAptitude() > candidat.getAptitude() 
                || (bestCandidat.getAptitude() == candidat.getAptitude() 
                && compInet.compare(bestCandidat.getIpAdress(), candidat.getIpAdress()) < 0)) {
                bestCandidat = candidat;
            }
        }
        return bestCandidat;
    }
    
    private void resultReceived(ServerDAO elected) {
        
    }
    
    private boolean waitForResponse() {
        return false;
    }
    
    private void responseNotReceived() {
        
    }
    
    
    private void sendResultTo(ServerDAO elected) {
        
    }
    
    private void sendElection(List<ServerDAO> candidats) {
        
    }
    
    private void sendResponseTo(ServerDAO to) {
        
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
        //TODO
        return servers[0];
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

}
