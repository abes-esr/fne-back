package fr.fne.core.entities.resApiWikibase;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cette classe de Java représente l'objet JSON mainsnak dans Wikibase
 * Plus info : http://fne-test.abes.fr/w/api.php?action=wbgetclaims&format=json&entity=Q8
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MainsnakWikibase {

    private String zoneNumber;
    private String PropertyName;
    private String property;
    private String datatype;
    private String snaktype;
    @JsonProperty("datavalue")
    private DatavalueWikibase datavalueWikibase;

}
