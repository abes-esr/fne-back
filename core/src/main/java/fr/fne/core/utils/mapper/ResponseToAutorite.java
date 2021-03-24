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
import reactor.core.scheduler.Schedulers;

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


    public Autorite toObjectReactive() throws InterruptedException {

        CountDownLatch countDownLatch = new CountDownLatch(1);
        Autorite autorite = new Autorite();
        List<Zone> zones = new ArrayList<>();

        propertiesMapper(item).parallel().runOn(Schedulers.boundedElastic())
                .map(v -> {
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
                    return v;
                }
                )
                .sequential()
                .doOnComplete(() -> {
                    Zone zone = addNewZoneWith200f(zones);
                    zones.add(zone);
                    if (autorite.getIsNotice()) {
                        List<Zone> sortedList = zones.stream()
                                //.sorted(Comparator.comparing(Zone::getPos))
                                .sorted(Comparator.comparing(Zone::getZoneNumber))
                                .collect(Collectors.toList());

                        autorite.setZones(sortedList);
                    }
                })
                .subscribe(v -> log.info("Subscriber on thread: " + Thread.currentThread().getName()),
                        e -> log.error(e.getMessage()),
                        countDownLatch::countDown);

        countDownLatch.await();
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
