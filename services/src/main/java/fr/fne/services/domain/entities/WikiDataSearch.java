package fr.fne.services.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Cette classe utilise pour mapper l'objet Json (Search) dans un objet de Java
 * Ici on obtient une liste d'objet de Java avec le nom search d'objet JSON
 * Plus info: http://fne-test.abes.fr/w/api.php?action=query&format=json&prop=&list=search&srnamespace=*&srprop=snippet&srsearch=alle*
 * On utilise CirrusSearch Etx de Wikidata
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WikiDataSearch {

    @JsonProperty("search")
    private List<WikiDataSearchItem> wikiDataSearchItemList;
}
