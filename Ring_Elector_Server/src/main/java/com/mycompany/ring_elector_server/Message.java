package com.mycompany.ring_elector_server;

/**
 *
 * @author jimmy
 */
public class Message {
    byte[] message;
    int length;
    
    /**
     * Constructeur pour construire un message de type RESPONSE
     * Ne nécessite donc pas de paramètres
     */
    public Message() {
        length = 1;
        message = new byte[length];
        message[0] = MessageType.RESPONSE.value;
    }
    
    /**
     * constructeur pour construire un message de type Election
     * nécessite la liste des candidats déjà validés.
     * 
     * @param candidats liste des identifiant des candidats déjà validés 
     */
    public Message(byte[] candidats) {
        length = message.length + 1;
        message = new byte[length];
        message[0] = MessageType.ELECTION.value;
        for (int i = 0; i < candidats.length; i++) {
            message[i + 1] = candidats[i];
        }
    }
    
    /**
     * Constructeur pour construire un message de type Result
     * @param elected le candidat choisit comme élu
     */
    public Message(byte elected) {
        length = 2;
        message = new byte[length];
        message[0] = MessageType.RESULT.value;
        message[1] = elected;
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
}
