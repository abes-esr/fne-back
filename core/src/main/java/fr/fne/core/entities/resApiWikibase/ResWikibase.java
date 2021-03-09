package fr.fne.core.entities.resApiWikibase;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
