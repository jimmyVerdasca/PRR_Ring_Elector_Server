package com.mycompany.ring_elector_server;

import java.net.ProtocolException;

/**
 * classe représentant le protocol durant l'élection
 * 
 * message contient au premier byte le type de message,
 * au second byte l'identifiant du candidat et au 3ème byte son aptitude.
 * 
 * Il existe 3 type de messages, 4 identifiant maximum et donc 4 aptitude 
 * qui sont déductible de l'identifiant de la machine.
 * 
 * Il aurait donc été possible de faire une table de correspondance avec 3*4=12
 * entrée et donc de n'avoir qu'un seul byte de message. Mais nous avons préféré
 * garder l'extensibilité du protocol à plus de 4 machines et rendre l'aptitude
 * possiblement non dépendant de l'identifiant de la machine.
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
        this(MessageType.RESPONSE, (byte)0, (byte)0);
    }
    
    /**
     * constructeur pour construire un message de type Election ou Result
     * nécessite le candidat favorit afin d'y extraire son id et son aptitude
     * 
     * @param messageType permet de différencier si on souhaite
     *                    envoyer un message RESULT ou ELECTION
     * @param candidat actuel favori lors de l'envoi du message
     */
    public Message(MessageType messageType, ServerDAO candidat) {
        /*
         * On se permet le cast car Il n'y aura que 4 id selon la donnée
         */
        this(messageType, (byte)candidat.getId(), candidat.getAptitude());
    }
    
    /**
     * constructeur pouvant construire tous les types de messages.
     * 
     * @param messageType permet de différencier si on souhaite
     *                    envoyer un message RESULT ou ELECTION ou RESPONSE
     * @param idCandidat actuel favori lors de l'envoi du message
     * @param aptitudeCandidat aptitude de l'actuel favori
     */
    private Message(MessageType messageType, byte idCandidat, byte aptitudeCandidat) {
        if (messageType.value != MessageType.RESPONSE.value) {
            this.length = 3;
            message = new byte[length];
            message[1] = idCandidat;
            message[2] = aptitudeCandidat;
        } else {
            this.length = 1;
            message = new byte[length];
        }
        message[0] = messageType.value;
    }
    
    /**
     * Méthode permettant de reconstruire un Message à partir de son buffer et
     * de la table des correspondance des machines
     * (pour y récupérer le candidat).
     * 
     * @param buffer reçu depuis un datagram qui contenait un Message
     * @param servers table des correspondance id->machine
     * @return un Message construit à partir des informations données
     * @throws ProtocolException Si le buffer ne contient
     *         pas des valeurs correspondant au protocol
     */
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
    
    /**
     * retourne le buffer qui sert à être envoyé dans le réseau
     * 
     * @return le buffer qui sert à être envoyé dans le réseau
     */
    public byte[] getMessage() {
        return message;
    }

    /**
     * retourne la taille du buffer qui varie entre les type de message (1-3)
     * 
     * @return la taille du buffer qui varie entre les type de message (1-3)
     */
    public int getLength() {
        return length;
    }
    
    /**
     * retourne le type de message que contient la classe
     * 
     * @return le type de message que contient la classe
     */
    MessageType getMessageType() {
        return MessageType.get(message[0]);
    }
    
    /**
     * retourne l'identifiant du candidat
     * 
     * @returnl'identifiant du candidat
     * @throws IllegalStateException un message de type RESPONSE
     *         ne contient pas de candidat
     */
    byte getCandidat() throws IllegalStateException {
        if (getMessageType() == MessageType.RESPONSE) {
            throw new IllegalStateException("mesage de type RESPONSE ne contient pas de candidat");
        }
        return message[1];
    }
}
