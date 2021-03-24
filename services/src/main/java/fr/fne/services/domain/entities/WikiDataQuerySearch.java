package fr.fne.services.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Cette classe utilise pour mapper l'objet Json (query) dans un objet de Java
 * Ici on obtient un objet de Java avec le nom query dans objet de JSON
 * Plus info: http://fne-test.abes.fr/w/api.php?action=query&format=json&prop=&list=search&srnamespace=*&srprop=snippet&srsearch=alle*
 * on utilise CirrusSearch ext de Wikidata
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WikiDataQuerySearch {

    private WikiDataSearch query;
}
