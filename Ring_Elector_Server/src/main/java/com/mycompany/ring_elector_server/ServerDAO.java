/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
    
    public ServerDAO(InetAddress ipAdress, int port) {
        this.ipAdress = ipAdress;
        this.port = port;    
    }

    public InetAddress getIpAdress() {
        return ipAdress;
    }

    public int getPort() {
        return port;
    }
    
    public int getAptitude() {
        return ipAdress.getAddress()[3] + port;
    }
    
    @Override
    public int hashCode() {
        int hash = 31;
        int result = hash * port;
        result += hash * ipAdress.hashCode();
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        return getClass().isInstance(obj) &&
            ipAdress == ((ServerDAO)obj).ipAdress &&
            port == ((ServerDAO)obj).port;
    }
}
