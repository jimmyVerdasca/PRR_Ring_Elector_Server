package com.mycompany.ring_elector_server;

import java.net.InetAddress;

/**
 * class représentant un serveur et ces points d'accès
 * 
 * @author Jimmy Verdasca et Nathan Gonzales
 */
public class ServerDAO {
    private final InetAddress ipAdress;
    private final int port;
    private final int id;
    
    public ServerDAO(InetAddress ipAdress, int port, int id) {
        this.ipAdress = ipAdress;
        this.port = port; 
        this.id = id;
    }

    public InetAddress getIpAdress() {
        return ipAdress;
    }

    public int getPort() {
        return port;
    }
    
    public byte getAptitude() {
        /* 
         * On se permet un cast avec perte d'information car
         * en cas d'égalité l'algorithme départagera avec l'adresse ip
         */
        return (byte)(ipAdress.getAddress()[3] + port);
    }

    public int getId() {
        return id;
    }
    
    
    
    @Override
    public int hashCode() {
        int hash = 31;
        int result = hash * port;
        result += hash * id;
        result += hash * ipAdress.hashCode();
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        return getClass().isInstance(obj) &&
            ipAdress == ((ServerDAO)obj).ipAdress &&
            id == ((ServerDAO)obj).id &&
            port == ((ServerDAO)obj).port;
    }
}
