package fr.fne.services.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Cette classe utilise pour mapper l'objet Json dans un objet de Java
 * Ici on obtient un objet de Java, on garde seulement les zones sont utiles pour la recherche ( title et snippet )
 * Plus info: http://fne-test.abes.fr/w/api.php?action=query&format=json&prop=&list=search&srnamespace=*&srprop=snippet&srsearch=alle*
 * On utilise CirrusSearch Etx de Wikidata
 */

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WikiDataSearchItem {

    private String title;
    private String snippet;
}
