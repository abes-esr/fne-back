package fr.fne.core.entities.resApiWikibase;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * Cette classe utilise pour récuperer la Datavalue dans chaque Property d'un Item Wikibase
 * Plus info : http://fne-test.abes.fr/w/api.php?action=wbgetclaims&format=json&entity=Q8
 * Il dépende de chaque type de Datavalue dans Wikibase (String , wikibase-entityid ou time)
 */
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class DatavalueWikibase {

    private String type;
    private String id;


    // c'est ici que l'on change les valeurs de time
    @JsonSetter("value")
    public void setValueInternal(JsonNode valueInternal) {

        if (valueInternal != null) {

            if (valueInternal.findValue("time") != null) {
                this.id = StringUtils.substringBetween(valueInternal
                        .get("time").asText(), "+", "T")
                        .replaceAll("-", "");
                if (id.length() > 4 && id.substring(4, 8).equals("0000")) {
                    this.id = this.id.substring(0, 4);
                }
            }
            if (valueInternal.findValue("id") != null) {
                this.id = valueInternal.get("id").asText();
            }
            if (valueInternal.isTextual()) {
                this.id = valueInternal.asText();
            }
        }

    }

}
