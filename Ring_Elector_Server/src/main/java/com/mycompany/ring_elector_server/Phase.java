package com.mycompany.ring_elector_server;

/**
 * énum représentant les différente phase de l'élection en anneau avec panne
 * 
 * -ELECTION_PHASE phase d'annonce du favori jusqu'à un premier tour complet
 * -RESULT_PHASE phase d'annonce du resultat de l'élection correspondant
 *               au "second" tour de l'anneau
 * -ELECTED_PHASE phase terminale durant laquelle l'élu est considéré comme
 *                valide par toutes les machines du système réparti
 * 
 * @author Jimmy Verdasca et Nathan Gonzales
 */
public enum Phase {
    ELECTION_PHASE,
    RESULT_PHASE,
    ELECTED_PHASE
}
