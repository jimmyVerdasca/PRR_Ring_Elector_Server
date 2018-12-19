package com.mycompany.ring_elector_server;

/**
 *
 * @author Jimmy Verdasca et Nathan Gonzales
 */
public enum Ping {
    SEND ((byte)0),
    RECEIVE ((byte)1);
    
    protected byte value;
    
    Ping(byte value) {
        this.value = value;
    }
}
