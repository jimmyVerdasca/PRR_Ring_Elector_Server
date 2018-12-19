package com.mycompany.ring_elector_server;

import java.net.InetAddress;

/**
 * class représentant un serveur et ces points d'accès
 * Nous y avons redéfinit les méthode hashMap et equals afin de pouvoir
 * comparer facilement deux ServerDAO.
 * 
 * @author Jimmy Verdasca et Nathan Gonzales
 */
public class ServerDAO {
    private final InetAddress ipAdress;
    private final int port;
    private final int id;
    
    /**
     * constructeur
     * 
     * @param ipAdress adresse ip du serveur représenté
     * @param port du serveur représenté
     * @param id identifiant du serveur représenté
     */
    public ServerDAO(InetAddress ipAdress, int port, int id) {
        this.ipAdress = ipAdress;
        this.port = port; 
        this.id = id;
    }

    /**
     * retourne l'adresse ip du serveur représenté
     * 
     * @return l'adresse ip du serveur représenté
     */
    public InetAddress getIpAdress() {
        return ipAdress;
    }

    /**
     * retourne le port du serveur représenté
     * 
     * @return le port du serveur représenté
     */
    public int getPort() {
        return port;
    }
    
    /**
     * retourne l'identifiant du serveur représenté
     * 
     * @return l'identifiant du serveur représenté
     */
    public int getId() {
        return id;
    }
    
    /**
     * calcul et retourne l'aptitude du serveur représenté
     * L'aptitude s'obtient avec la 4ème partie de l'adresse ip
     * additionné au port.
     * 
     * @return l'aptitude du serveur représenté
     */
    public byte getAptitude() {
        /* 
         * On se permet un cast avec perte d'information car
         * en cas d'égalité l'algorithme départagera avec l'adresse ip
         */
        return (byte)(ipAdress.getAddress()[3] + port);
    }
    
    /**
     * retourne le hash de cette instance
     * 
     * @return le hash de cette instance
     */
    @Override
    public int hashCode() {
        int hash = 31;
        int result = hash * port;
        result += hash * id;
        result += hash * ipAdress.hashCode();
        return result;
    }
    
    /**
     * compare cette instance à un autre objet
     * 
     * @param obj objet auquel on compare cet instance
     * @return true si les deux instance possèdent les mêmes valeurs, false sinon
     */
    @Override
    public boolean equals(Object obj) {
        return getClass().isInstance(obj) &&
            ipAdress == ((ServerDAO)obj).ipAdress &&
            id == ((ServerDAO)obj).id &&
            port == ((ServerDAO)obj).port;
    }
}
