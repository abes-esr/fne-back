package fr.fne.core.entities.resApiWikibase;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Cette class utilise pour creer les nouveau Item dans Wikibase
 * Utiliser dan l'objet labels et description
 * data={"labels":{"en-gb":{"language":"en-gb","value":"Propertylabel"}},"descriptions":{"en-gb":{"language":"en-gb","value":"Propertydescription"}},"datatype":"string"}
 * plus info : http://fne-test.abes.fr/w/api.php?action=help&modules=wbeditentity
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PropertyWikibaseValuefr {

    private String language;
    private String value;
}
