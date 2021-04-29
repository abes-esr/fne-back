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
import org.springframework.http.HttpStatus;
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
                        if(wikibaseProperties.getLabels().containsKey(v.getPropertyName()))
                        {
                            zone.setLabel(wikibaseProperties.getLabels().get(v.getPropertyName()));
                        } else {
                            zone.setLabel(v.getPropertyName());
                        }
                        zone.setZoneNumber(v.getZoneNumber());
                        if (v.getDatavalueWikibase().getTimePrecision() == 8) {
                            if (v.getDatavalueWikibase().getId().contains("000")) {
                                zone.setSubZones(replaceLast(v.getDatavalueWikibase().getId(),"000","XXX"));
                            } else if (v.getDatavalueWikibase().getId().contains("00")) {
                                zone.setSubZones(replaceLast(v.getDatavalueWikibase().getId(),"00","XX"));
                            } else if (v.getDatavalueWikibase().getId().contains("0")) {
                                zone.setSubZones(replaceLast(v.getDatavalueWikibase().getId(),"0","X"));
                            }
                        } else {
                            zone.setSubZones(v.getDatavalueWikibase().getId());
                        }
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
                .onErrorResume(e -> Flux.empty())
                .subscribe(v -> log.info("Subscriber on thread: " + Thread.currentThread().getName()),
                        e -> log.error(e.getMessage()),
                        countDownLatch::countDown);

        countDownLatch.await();
        return autorite;
    }

    public Flux<MainsnakWikibase> propertiesMapper(String item) {

        List<String> properties = new ArrayList<>();
        String url = this.uriProperty + item;
        WebClient webClient = webClientBuilder.baseUrl(url).build();

        return webClient.get().retrieve()
                .onStatus(HttpStatus::is4xxClientError, response -> Mono.empty())
                .bodyToMono(JsonNode.class)
                .onErrorResume(e -> Mono.empty())
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
                .flatMap(v -> getPropertyName(v.getProperty(), v))
                .onErrorResume(e -> Flux.empty());
    }


    private Mono<MainsnakWikibase> getPropertyValue(String item, String property) {

        String url = this.uriProperty + item + "&property=" + property;
        ObjectMapper mapper = new ObjectMapper();
        WebClient webClient = webClientBuilder.baseUrl(url).build();

        return webClient.get().retrieve()
                .onStatus(HttpStatus::is4xxClientError, response -> Mono.empty())
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
                })
                .onErrorResume(e -> Mono.empty());

    }

    private Mono<MainsnakWikibase> getPropertyName(String propertyValue, MainsnakWikibase mainsnakWikibase) {

        String url = this.uriPropertyValue + propertyValue;
        WebClient webClient = webClientBuilder.baseUrl(url).build();

        return webClient.get().retrieve()
                .onStatus(HttpStatus::is4xxClientError, response -> Mono.empty())
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
                })
                .onErrorResume(e -> Mono.empty());
    }


    private Zone addNewZoneWith200f(List<Zone> zones) {
        Zone zone = new Zone();
        StringBuilder stringBuilder = new StringBuilder();

        zones.forEach(v -> {
            if (v.getZoneNumber().equals("103##$a")) {
                stringBuilder.append(v.getSubZones().replaceAll("X", "."), 0, 4);
            }
            if (v.getZoneNumber().equals("103##$b")) {
                stringBuilder.append(v.getSubZones().replaceAll("X", "."), 0, 4);
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

    private static String replaceLast(String text, String regex, String replacement) {
        return text.replaceFirst("(?s)"+regex+"(?!.*?"+regex+")", replacement);
    }
}
