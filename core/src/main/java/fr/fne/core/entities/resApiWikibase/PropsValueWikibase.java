package fr.fne.core.entities.resApiWikibase;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PropsValueWikibase {

    @JsonProperty("mainsnak")
    private MainsnakWikibase mainsnakWikibase;
}
