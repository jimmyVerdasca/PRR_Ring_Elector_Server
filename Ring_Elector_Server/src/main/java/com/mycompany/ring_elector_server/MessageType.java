package com.mycompany.ring_elector_server;

import java.util.HashMap;
import java.util.Map;

/**
 * enum utilisé comme premier byte du protocol lors des élections
 * pour différencier les type de messages.
 * 
 * -ELECTION sert à annoncer le candidat actuel favori
 * -RESULT sert à communiqué à tous le choix d'élu
 * -RESPONSE sert d'aquittement aux deux type précédent
 * 
 * Nous nous servons d'une hashmap afin de pouvoir récupérer l'enum aussi
 * à partir de sa valeur puisque nous devrons lire des datagrams.
 * 
 * @author Jimmy Verdasca et Nathan Gonzales
 */
public enum MessageType {
    ELECTION ((byte)0),
    RESPONSE ((byte)1),
    RESULT ((byte)2);
    
    protected byte value;
    
    private static final Map<Byte, MessageType> reverseTable = new HashMap<>();
     static {
         //Create reverse lookup hash map 
         for(MessageType d : MessageType.values())
             reverseTable.put(d.getValue(), d);
     }
 
     /**
      * constructeur
      * 
      * @param value valeur du type de message
      */
    MessageType(byte value) {
        this.value = value;
    }
    
    /**
     * retourne la valeur de l'enum
     * 
     * @return la valeur de l'enum
     */
    public byte getValue() {
        return this.value;
    }
    
    /**
     * permet de récupérer l'enum à partir de sa valeur
     * 
     * @param messageValue valeur de l'enum recherché
     * @return l'enum dont la valeur vaut messageValue
     */
    public static MessageType get(byte messageValue) {
          return reverseTable.get(messageValue); 
     }
}
