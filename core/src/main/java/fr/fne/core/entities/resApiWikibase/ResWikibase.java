package fr.fne.core.entities.resApiWikibase;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cette classe utilise dans la list recent change de Wikibase
 * Plus info : http://fne-test.abes.fr/w/api.php?action=query&list=recentchanges&format=json&rclimit=500&rctoponly=1&rcend=20210201090000
 * Utiliser pour convertir directement en objet de Java à partir d'une liste d'objet JSON
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResWikibase {
    @JsonProperty("type")
    private String type;
    @JsonProperty("ns")
    private Integer ns;
    @JsonProperty("title")
    private String title;
    @JsonProperty("pageid")
    private Integer pageId;
    @JsonProperty("revid")
    private Integer revid;
    @JsonProperty("old_revid")
    private Integer old_revid;
    @JsonProperty("rcid")
    private Integer rcid;
    @JsonProperty("timestamp")
    private String timestamp;
}
