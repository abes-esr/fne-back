package fr.fne.core.entities.resApiWikibase;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Mono;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MainsnakWikibase {

    private String zoneNumber;
    private String PropertyName;
    private String property;
    private String datatype;
    @JsonProperty("datavalue")
    private DatavalueWikibase datavalueWikibase;

}
