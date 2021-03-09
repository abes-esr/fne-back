package fr.fne.core.config;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
//@PropertySource(factory=YamlFileApplication.class, value="classpath:wikibase.yml", encoding="UTF-8")
//@ConfigurationProperties(prefix = "wikibase")
//@Setter
//@Getter
public class WikibaseProperties {

    private final Map<String, String> zones = new HashMap<>();

    public Map<String, String> getZones() {

        zones.put("Nom", "200##$a");
        zones.put("Prénom", "200##$b");
        zones.put("Date de naissance", "103##$a");
        zones.put("Date de décès", "103##$b");
        zones.put("Langue de la personne", "101##$a");
        zones.put("Libellé ISO 639-2", "101##$a");
        zones.put("Pays associé à la personne", "102##$a");
        zones.put("Libéllé ISO 3166-1", "102##$a");
        zones.put("Source", "810##$a");
        zones.put("PPN", "001");
        return zones;
    }
}
