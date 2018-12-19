package com.mycompany.ring_elector_server;

/**
 * énum servant de protocol pour l'échange des message de type ping
 * (qui se font sur une socket séparé aux messages d'élection)
 * 
 * -SEND est utilisé par les clients cherchant à atteindre l'élu
 * -RECEIVE est envoyé par l'élu pour répondre à un SEND et donc valider sa présence
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
