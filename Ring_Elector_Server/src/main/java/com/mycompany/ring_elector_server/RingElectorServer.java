package com.mycompany.ring_elector_server;

/**
 * classe servant de serveur,
 * implémentant l'algorithme d'élection en anneaux avec gestion des pannes.
 * 
 * Le serveur élu ne fait que répondre qu'il est présent aux serveurs qui l'appel.
 * Tandis que les serveurs non-élus vont régulièrement demander
 * au serveur élu s'il est toujours en fonction.
 * 
 * Lors de l'initialisation d'un tel serveur, le serveur lance une election.
 * Lors d'une panne, Si la machine en panne est l'élu,
 * alors le premier serveur qui se rend compte de la panne, lance une election.
 * Si une panne survient sur une machine non-élu, cela n'a pas d'impact,
 * jusqu'à son reboot qui lance une election.
 * 
 * Pour ce laboratoire, nous considérons que le réseau ne pause aucun soucit.
 * 
 * @author Jimmy Verdasca et Nathan Gonzales
 */
public class RingElectorServer {
    
}
