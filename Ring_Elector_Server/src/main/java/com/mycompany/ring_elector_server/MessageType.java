package com.mycompany.ring_elector_server;

import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author jimmy
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
 
    MessageType(byte value) {
        this.value = value;
    }
    
    public byte getValue() {
        return this.value;
    }
    
    public static MessageType get(byte messageValue) {
          return reverseTable.get(messageValue); 
     }
}
