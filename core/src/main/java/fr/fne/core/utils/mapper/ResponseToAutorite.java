package fr.fne.core.utils.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.fne.core.config.WikibaseProperties;
import fr.fne.core.entities.Autorite;
import fr.fne.core.entities.Zone;
import fr.fne.core.entities.resApiWikibase.MainsnakWikibase;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * Cette classe est utilisée pour convertir la réponse qui vient de Wikibase (Q item page) en objet java
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class ResponseToAutorite {

    @Setter
    private String item;
    @Value("${wikibase.urls.property-detail}")
    private String uriProperty;
    @Value("${wikibase.urls.property-value}")
    private String uriPropertyValue;

    private final WebClient.Builder webClientBuilder;
    private final WikibaseProperties wikibaseProperties;

/*
    public Autorite toObject() {
        Autorite autorite = new Autorite();
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String,String> mapper = new LinkedHashMap<>();
        String url = this.uriProperty+item;

        System.out.printf("============ Get all properties of item %s =================%n",item);

        try{
            // Ici on retourne une chaine avec tous les property d'un item dans wikibase (P...)
            ResponseEntity<String> response
                    = restTemplate.getForEntity(url, String.class);
            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            // On boucle sur la liste de tous les P qu'on a trouvé
            for(JsonNode j: jsonNode.get("claims")) {

                // A chaque tour de boucle, on va aller dans la page de ce property afin de récupérer sa valeur en utilisant sa clé P...
                String urlP = this.uriPropertyValue+j.findValue("property").asText();
                ResponseEntity<String> responseP
                        = restTemplate.getForEntity(urlP, String.class);

                JsonNode jsonNodeP = objectMapper.readTree(responseP.getBody());

                System.out.println(jsonNodeP);

                if(jsonNodeP.findValue("datatype").asText().equals("string")) {

                    mapper.put(jsonNodeP.findValue("value").asText(),j.findValue("value").asText());
                }
            }
            List<Zone> zones = new ArrayList<>();

            System.out.println("============ Mapping with key and value for each property =================");

            // Ici on va faire les traitements afin de récupérer l'objet java Autorite avec sa liste de zone
            // Ainsi que la position et les tags de chaque zone afin de pouvoir ajouter les SubZones dans la même ligne de CBS
            // ex: si on a les memes zone number avec les memes positions, on va les mettre dans la même ligne avant d'insérer dans le CBS
            // Zone(zoneNumber=033, pos=0, tag=##, subZones=$ahttp://catalogue.bnf.fr/ark:/12148/cb119351030), Zone(zoneNumber=033, pos=0, tag=##, subZones=$2BNF), Zone(zoneNumber=033, pos=0, tag=##, subZones=$d20150918)
            // Une liste de zone 033 avec les memes positions = 0
            // ==> dans CBS on a une seule ligne = 033 $ahttp://catalogue.bnf.fr/ark:/12148/cb119351030$2BNF$d20150918
            mapper.forEach( (k,v) -> {
                log.info("Key = "+ k + " Value = " + v);
                Zone zone = new Zone();
                if(StringUtils.isNumeric(k.substring(k.length()-1)) && k.substring(k.length()-2).startsWith("_")) {
                    int pos = Integer.parseInt(k.substring(k.length()-1));
                    zone.setPos(pos);
                }
                // si on ne trouve pas la zone 001 dans wikibase (PPN)
                if(!k.substring(0,3).equals("001")) {
                    zone.setZoneNumber(k.substring(0,3));
                    if(k.length() > 3 && k.charAt(3) == '#') {
                        zone.setTag(k.substring(3,5));
                    } else {
                        zone.setTag("##");
                    }
                    if(zone.getPos() > 0) {
                        zone.setSubZones(k.substring(k.indexOf('$'),k.indexOf('$')+2) + v);
                    } else if(k.contains("$")) {
                        zone.setSubZones(k.substring(k.indexOf('$')) + v);
                    } else {
                        zone.setSubZones(v);
                    }

                    zones.add(zone);
                }else {
                    autorite.setExiste(true);
                    autorite.setPpn(v);
                }
            });

            List<Zone> sortedList = zones.stream()
                    .sorted(Comparator.comparing(Zone::getPos))
                    .sorted(Comparator.comparing(Zone::getZoneNumber))
                    .collect(Collectors.toList());

            autorite.setZones(sortedList);

        } catch(Exception ex) {
            log.error(ex.getMessage());
        }

        return autorite;
    }

*/


    public Autorite toObjectReactive() throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(1);
        Autorite autorite = new Autorite();
        List<Zone> zones = new ArrayList<>();

        propertiesMapper(item).subscribe(v -> {
            System.out.println(v);
            Zone zone = new Zone();
            if (!v.getZoneNumber().equals("001")) {
                zone.setZoneNumber(v.getZoneNumber());
                zone.setSubZones(v.getDatavalueWikibase().getId());
                zone.setTag("##");
                zones.add(zone);
            } else {
                autorite.setExiste(true);
                autorite.setPpn(v.getDatavalueWikibase().getId());
            }
            if (v.getZoneNumber().startsWith("200")) {
                autorite.setIsNotice(true);
            }

        }, e -> log.error(e.getMessage()), countDownLatch::countDown);

        countDownLatch.await();

        Zone zone = addNewZoneWith200f(zones);
        zones.add(zone);
        if (autorite.getIsNotice()) {
            List<Zone> sortedList = zones.stream()
                    //.sorted(Comparator.comparing(Zone::getPos))
                    .sorted(Comparator.comparing(Zone::getZoneNumber))
                    .collect(Collectors.toList());

            autorite.setZones(sortedList);
        }
        return autorite;
    }

    private Flux<MainsnakWikibase> propertiesMapper(String item) {

        List<String> properties = new ArrayList<>();
        String url = this.uriProperty + item;
        WebClient webClient = webClientBuilder.baseUrl(url).build();

        return webClient.get().retrieve()
                .bodyToMono(JsonNode.class)
                .map(v -> v.findValue("claims"))
                .map(v -> {
                    for (JsonNode j : v) {
                        properties.add((j.findValue("property").asText()));
                    }
                    return properties;
                })
                .flatMapMany(Flux::fromIterable)
                .flatMap(v -> getPropertyValue(item, v))
                .expand(v -> {
                    if (v.getDatatype().equals("wikibase-item")) {
                        return propertiesMapper(v.getDatavalueWikibase().getId());
                    }
                    return Flux.empty();
                })
                .filter(v -> !v.getDatatype().equals("wikibase-item"))
                .flatMap(v -> getPropertyName(v.getProperty(), v));


    }


    private Mono<MainsnakWikibase> getPropertyValue(String item, String property) {

        String url = this.uriProperty + item + "&property=" + property;
        ObjectMapper mapper = new ObjectMapper();
        WebClient webClient = webClientBuilder.baseUrl(url).build();

        return webClient.get().retrieve()
                .bodyToMono(JsonNode.class)
                .map(v -> v.findValue("mainsnak"))
                .map(s -> {
                    try {
                        return mapper.readValue(s.traverse(), new TypeReference<MainsnakWikibase>() {
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                        return new MainsnakWikibase();
                    }
                });

    }

    private Mono<MainsnakWikibase> getPropertyName(String propertyValue, MainsnakWikibase mainsnakWikibase) {

        String url = this.uriPropertyValue + propertyValue;
        WebClient webClient = webClientBuilder.baseUrl(url).build();

        return webClient.get().retrieve()
                .bodyToMono(JsonNode.class)
                .map(v -> v.findValue("fr"))
                .map(s -> {
                    String pName = s.findValue("value").asText();
                    mainsnakWikibase.setPropertyName(pName);
                    if (wikibaseProperties.getZones().containsKey(pName)) {
                        String pTag = wikibaseProperties.getZones().get(pName);
                        mainsnakWikibase.setZoneNumber(pTag);
                    }
                    return mainsnakWikibase;
                });
    }


    private Zone addNewZoneWith200f(List<Zone> zones) {
        Zone zone = new Zone();
        StringBuilder stringBuilder = new StringBuilder();

        zones.forEach(v -> {
            if (v.getZoneNumber().equals("103##$a")) {
                stringBuilder.append(v.getSubZones(), 0, 4);
            }
            if (v.getZoneNumber().equals("103##$b")) {
                stringBuilder.append(v.getSubZones(), 0, 4);
            }
        });

        if (org.springframework.util.StringUtils.hasText(stringBuilder)) {
            zone.setZoneNumber("200##$f");
            if (stringBuilder.length() > 4) {
                zone.setSubZones(stringBuilder.substring(0, 4) + "-" + stringBuilder.substring(4, 8));
            } else {
                zone.setSubZones(stringBuilder.substring(0, 4) + "-...");
            }
        }

        return zone;
    }

}
