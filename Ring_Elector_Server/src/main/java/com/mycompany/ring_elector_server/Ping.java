/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.ring_elector_server;

/**
 *
 * @author jimmy
 */
public enum Ping {
    SEND ((byte)0),
    RECEIVE ((byte)1);
    
    protected byte value;
    
    Ping(byte value) {
        this.value = value;
    }
}
