package fr.fne.core.entities;

import lombok.Data;

import java.util.List;

@Data
public class Autorite {

    List<Zone> zones;
    // Check s'il existe déja le PPN dans wikibase
    Boolean existe = false;

    String ppn;
    // check si objet est une notice dans Wikibase

    Boolean isNotice = false;
}
