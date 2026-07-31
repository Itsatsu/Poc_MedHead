package com.medhead.poc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée de l'application. Le POC suit une architecture hexagonale :
 * le domaine (paquet {@code domain}) contient les règles métier pures et ne dépend
 * d'aucun framework, tandis que le paquet {@code infrastructure} regroupe les adaptateurs
 * (web, persistance en mémoire, appels externes) qui implémentent les ports du domaine.
 */
@SpringBootApplication
public class MedHeadPocApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedHeadPocApplication.class, args);
    }
}
