package com.mycompany.ring_elector_server;

import java.net.ProtocolException;

/**
 * classe permettant de simplifier la construction d'un message
 * selon la protocol choisit
 * 
 * @author Jimmy Verdasca et Nathan Gonzales
 */
public class Message {
    private byte[] message;
    private int length;
    
    /**
     * Constructeur pour construire un message de type RESPONSE
     * Ne nécessite donc pas de paramètres
     */
    public Message() {
        this(MessageType.RESPONSE, 1, (byte)0, (byte)0);
    }
    
    /**
     * constructeur pour construire un message de type Election ou Result
     * nécessite le candidat favorit afin d'y extraire son id et son aptitude
     * 
     * @param messageType permet de différencier si on souhaite
     *                    envoyer un message RESULT ou ELECTION
     * @param candidat actuel favori lors de l'envoi du message
     * @throws java.net.ProtocolException
     */
    public Message(MessageType messageType, ServerDAO candidat) {
        /*
         * On se permet le cast car Il n'y aura que 4 id selon la donnée
         */
        this(messageType, 3, (byte)candidat.getId(), candidat.getAptitude());
    }
    
    private Message(MessageType messageType, int length, byte idCandidat, byte aptitudeCandidat) {
        this.length = length;
        message = new byte[length];
        message[0] = messageType.value;
        
        if (messageType.value != MessageType.RESPONSE.value) {
            message[1] = idCandidat;
            message[2] = aptitudeCandidat;
        }
    }
    
    public static Message BuildMessage(byte[] buffer, ServerDAO[] servers) throws ProtocolException {
        Message message = null;
        if (buffer[0] == MessageType.RESPONSE.value) {
            message = new Message();
        } else if (buffer[0] == MessageType.RESULT.value) {
            message = new Message(MessageType.RESULT, servers[buffer[1]]);
        } else if (buffer[0] == MessageType.ELECTION.value){
            message = new Message(MessageType.ELECTION, servers[buffer[1]]);
        } else {
            throw new ProtocolException("Le type de message ne correspond à rien de connu : " + buffer[0]);
        }
        return message;
    }
    
    public byte[] getMessage() {
        return message;
    }

    public int getLength() {
        return length;
    }
    
    MessageType getMessageType() {
        return MessageType.get(message[0]);
    }
    
    byte getCandidat() throws IllegalStateException {
        if (getMessageType() == MessageType.RESPONSE) {
            throw new IllegalStateException("mesage de type RESPONSE ne contient pas de candidat");
        }
        return message[1];
    }
}
