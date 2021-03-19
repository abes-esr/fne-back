package fr.fne.core.entities.resApiWikibase;

import lombok.*;

/**
 * Cette classe utilise pour envoyer un object de type Time dans Wikibase
 * lors de la création d'un nouveau Item ou Property dans Wikibase
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DataTime {

    private String time ;

    private final int timezone = 0;

    private final int before = 0;

    private final int after = 0;

    private int precision;

    private final String calendarmodel = "http://www.wikidata.org/entity/Q1985727";

}
