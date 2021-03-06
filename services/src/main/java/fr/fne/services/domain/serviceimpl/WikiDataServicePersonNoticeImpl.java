package fr.fne.services.domain.serviceimpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import fr.fne.core.config.WikibaseProperties;
import fr.fne.core.entities.resApiWikibase.DataTime;
import fr.fne.core.entities.resApiWikibase.PropertyWikibaseValuefr;
import fr.fne.core.utils.OAuthHttp;
import fr.fne.core.utils.mapper.ResponseToAutorite;
import fr.fne.services.domain.WikiDataService;
import fr.fne.services.domain.WikiDataServicePersonNotice;
import fr.fne.services.domain.entities.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * Cett classe contient toutes les actions avec Wikibase,
 * elle utilise les méthodes de la classe OAuthHttp
 */

@RequiredArgsConstructor
@Slf4j
@Service
public class WikiDataServicePersonNoticeImpl implements WikiDataServicePersonNotice {

    @Value("${wikibase.urls.fne}")
    private String urlFneApi;
    @Value("${wikibase.urls.property-search}")
    private String propertySearch;
    @Value("${wikibase.urls.item-search}")
    private String itemSearch;
    @Value("${wikibase.urls.sparql}")
    private String urlsSparql;
    @Value("${wikibase.urls.property-detail}")
    private String propertyDetail;


    private final WebClient.Builder webClientBuilder;
    private final OAuthHttp oAuthHttp;
    private final ResponseToAutorite responseToAutorite;
    private final WikibaseProperties wikibaseProperties;
    private final WikiDataService wikiDataService;

    /*
     * Insert in urlWikiBase, the record, with the properties defined (props)
     */
    @Override
    public void insertPpn(String urlWikiBase, String csrftoken, String idItemQ, String ppn) {
        try {
            //String data = "{\"claims\":[{\"mainsnak\":{\"snaktype\":\"value\",\"property\":\"P166\",\"datavalue\":{\"value\":\""+ppn+"\",\"type\":\"string\"}},\"type\":\"statement\",\"rank\":\"normal\"}]}";
            Map<String, String> params = new LinkedHashMap<>();

            params.put("action", "wbcreateclaim");
            params.put("format", "json");
            params.put("entity", idItemQ);
            params.put("snaktype", "value");
            params.put("property", "P23");
            params.put("value", "\"" + ppn + "\"");
            params.put("token", csrftoken);

            JsonNode json = oAuthHttp.httpOAuthPost(urlWikiBase, params);
            log.info("Réponse de WIKIBASE  ===> " + json.toString());

        } catch (Exception e) {
            log.error("Error on the id , ppn : " + idItemQ + " : " + ppn + " :" + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public String getCsrfToken(String urlWikibase) {
        return oAuthHttp.getCsrfToken(urlWikibase);
    }

    @Override
    public Mono<WikiDataPersonNotice> save(WikiDataPersonNotice wikibaseItem) throws Exception {

        Map<String, String> params = new LinkedHashMap<>();
        String token = getCsrfToken(urlFneApi);
        ObjectMapper objectMapper = new ObjectMapper();

        Mono<WikiDataPersonNotice> wikibaseItemMono = Mono.just(wikibaseItem);

        return wikibaseItemMono.flatMap(v -> {

            String value = titlteConvert(wikibaseItem);
            PropertyWikibaseValuefr propertyWikibaseValuefr = new PropertyWikibaseValuefr("fr", value);
            PropertyWikibaseValuefr propertyWikibaseValuefrDescription = new PropertyWikibaseValuefr("fr", v.getInstantOf().strip());
            try {
                String jsonString = objectMapper.writeValueAsString(propertyWikibaseValuefr);
                String jsonStringDescription = objectMapper.writeValueAsString(propertyWikibaseValuefrDescription);
                params.put("action", "wbeditentity");
                params.put("format", "json");
                params.put("new", "item");
                params.put("data", "{\"labels\":{\"fr\":"+jsonString+"},\"descriptions\":{\"fr\":"+jsonStringDescription+"}}");
                params.put("token", token);

                JsonNode json = oAuthHttp.httpOAuthPost(urlFneApi, params);
                if (json.has("error")) {
                    wikibaseItem.setStatus("Doublons");
                    String doublonsMessage = json.findValue("info").asText();
                    wikibaseItem.setItemId(doublonsMessage);
                } else if (json.has("entity")) {
                    wikibaseItem.setStatus("OK");
                    wikibaseItem.setItemId(json.findValue("id").asText());
                }

                log.info("Réponse de WIKIBASE  ===> " + json.toString());
                return Mono.just(wikibaseItem);
            } catch (Exception e) {
                e.printStackTrace();
                return Mono.empty();
            }

        })
        .map(v -> {

            Scheduler singleThread = Schedulers.single();
            if (v.getStatus().equals("OK")) {

                if(!Strings.isNullOrEmpty(v.getInstantOf())) {
                    getPropertyName("Est une instance de", "property").publishOn(singleThread).subscribe(s -> {
                        getPropertyName(v.getInstantOf().strip(), "item").publishOn(singleThread).subscribe(t -> {
                            try {
                                createPropertyItem(v.getItemId().strip(), s.strip(), t.strip());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    });
                }

                if(!Strings.isNullOrEmpty(v.getFirstName())) {
                    getPropertyName("Nom", "property").publishOn(singleThread).subscribe(s -> {
                        try {
                            createProperty(v.getFirstName().strip(), v.getItemId().strip(), s.strip());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    });
                }

                if(!Strings.isNullOrEmpty(v.getLastName())) {
                    getPropertyName("Prénom", "property").publishOn(singleThread).subscribe(s -> {
                        try {
                            createProperty(v.getLastName().strip(), v.getItemId().strip(), s.strip());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
                if(!Strings.isNullOrEmpty(v.getDateBirth())) {
                    getPropertyName("Date de naissance", "property").publishOn(singleThread).subscribe(s -> {
                        try {
                            createPropertyTime(v.getDateBirth().strip(), v.getItemId().strip(), s.strip());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }

                if(!Strings.isNullOrEmpty(v.getDateDead())) {
                    getPropertyName("Date de décès", "property").publishOn(singleThread).subscribe(s -> {
                        try {
                            createPropertyTime(v.getDateDead().strip(), v.getItemId().strip(), s.strip());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }

                if(!Strings.isNullOrEmpty(v.getLangue())) {
                    getPropertyName("Langue de la personne", "property").publishOn(singleThread).subscribe(s -> {
                        getPropertyName(v.getLangue().strip(), "item").publishOn(singleThread).subscribe(t -> {
                            try {
                                createPropertyItem(v.getItemId().strip(), s.strip(), t.strip());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    });
                }

                if(!Strings.isNullOrEmpty(v.getCountry())) {
                    getPropertyName("Pays associé à la personne", "property").publishOn(singleThread).subscribe(s -> {
                        getPropertyName(v.getCountry().strip(), "item").publishOn(singleThread).subscribe(t -> {
                            try {
                                createPropertyItem(v.getItemId().strip(), s.strip(), t.strip());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    });
                }

                if(!Strings.isNullOrEmpty(v.getSource())) {
                    getPropertyName("Source", "property").publishOn(singleThread).subscribe(s -> {
                        try {
                            createProperty(v.getSource().strip(), v.getItemId().strip(), s.strip());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    });
                }

            }

            return v;
        });


    }

    @Override
    public Mono<WikiDataPersonNotice> update(WikiDataPersonNotice wikibaseItem) throws Exception {

        Map<String, String> params = new LinkedHashMap<>();
        String token = getCsrfToken(urlFneApi);
        ObjectMapper objectMapper = new ObjectMapper();

        Mono<WikiDataPersonNotice> wikibaseItemMono = Mono.just(wikibaseItem);

        return wikibaseItemMono.flatMap(v -> {

            String value = titlteConvert(wikibaseItem);
            PropertyWikibaseValuefr propertyWikibaseValuefr = new PropertyWikibaseValuefr("fr", value);
            PropertyWikibaseValuefr propertyWikibaseValuefrDescription = new PropertyWikibaseValuefr("fr", "Personne - RDA");
            try {
                String jsonString = objectMapper.writeValueAsString(propertyWikibaseValuefr);
                String jsonStringDescription = objectMapper.writeValueAsString(propertyWikibaseValuefrDescription);
                params.put("action", "wbeditentity");
                params.put("format", "json");
                params.put("id", v.getItemId());
                params.put("data", "{\"labels\":{\"fr\":"+jsonString+"},\"descriptions\":{\"fr\":"+jsonStringDescription+"}}");
                params.put("token", token);

                JsonNode json = oAuthHttp.httpOAuthPost(urlFneApi, params);
                if (json.has("error")) {
                    wikibaseItem.setStatus("Doublons");
                    String doublonsMessage = json.findValue("info").asText();
                    wikibaseItem.setItemId(doublonsMessage);
                } else if (json.has("entity")) {
                    wikibaseItem.setStatus("OK");
                }

                log.info("Réponse de WIKIBASE  ===> " + json.toString());
                return Mono.just(wikibaseItem);
            } catch (Exception e) {
                e.printStackTrace();
                return Mono.empty();
            }

        })
        .map(v -> {

            if (v.getItemId() != null && v.getStatus().equals("OK")) {

                Scheduler singleThread = Schedulers.single();

                if(v.getFirstName() != null) {
                    getPropertyName("Nom", "property").publishOn(singleThread).subscribe(s -> {
                        getPropertyGuid(v.getItemId(), s).subscribe(t -> {
                            try {
                                updateProperty(v.getFirstName().strip(), t);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    });
                }

                if(v.getLastName() != null) {
                    getPropertyName("Prénom", "property").publishOn(singleThread).subscribe(s -> {
                        getPropertyGuid(v.getItemId(), s).subscribe(t -> {
                            try {
                                updateProperty(v.getLastName().strip(), t);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    });
                }
                if(!Strings.isNullOrEmpty(v.getDateBirth())) {
                    getPropertyName("Date de naissance", "property").publishOn(singleThread).subscribe(s -> {
                        getPropertyGuid(v.getItemId(), s).subscribe(t -> {
                            try {
                                if(Strings.isNullOrEmpty(t)) {
                                    createPropertyTime(v.getDateBirth().strip(), v.getItemId().strip(), s.strip());
                                } else {
                                    updatePropertyTime(v.getDateBirth().strip(), t);
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    });
                } else {
                    getPropertyName("Date de naissance", "property").publishOn(singleThread).subscribe(s -> {
                        getPropertyGuid(v.getItemId(), s).subscribe(t -> {
                            try {
                                if(!Strings.isNullOrEmpty(t)) {
                                    removeProperty(t);
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    });
                }

                if(!Strings.isNullOrEmpty(v.getDateDead())) {
                    getPropertyName("Date de décès", "property").publishOn(singleThread).subscribe(s -> {
                        getPropertyGuid(v.getItemId(), s).subscribe(t -> {

                            try {
                                if(Strings.isNullOrEmpty(t)) {
                                    createPropertyTime(v.getDateDead().strip(), v.getItemId().strip(), s.strip());
                                } else {
                                    updatePropertyTime(v.getDateDead().strip(), t);
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    });
                } else {
                    getPropertyName("Date de décès", "property").publishOn(singleThread).subscribe(s -> {
                        getPropertyGuid(v.getItemId(), s).subscribe(t -> {
                            try {
                                if(!Strings.isNullOrEmpty(t)) {
                                    removeProperty(t);
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    });
                }

                if(!Strings.isNullOrEmpty(v.getLangue())) {
                    getPropertyName("Langue de la personne", "property").publishOn(singleThread).subscribe(s -> {
                        getPropertyName(v.getLangue().strip(), "item").publishOn(singleThread).subscribe(t -> {
                            getPropertyGuid(v.getItemId(), s).subscribe(i -> {
                                try {
                                    if (Strings.isNullOrEmpty(i)) {
                                        createPropertyItem(v.getItemId().strip(), s.strip(), t.strip());
                                    } else {
                                        updatePropertyItem(t, i);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                        });
                    });
                } else {
                    getPropertyName("Langue de la personne", "property").publishOn(singleThread).subscribe(s -> {
                        getPropertyGuid(v.getItemId(), s).subscribe(i -> {
                            try {
                                if (!Strings.isNullOrEmpty(i)) {
                                    removeProperty(i);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    });
                }

                if(v.getCountry() != null) {
                    getPropertyName("Pays associé à la personne", "property").publishOn(singleThread).subscribe(s -> {
                        getPropertyName(v.getCountry().strip(), "item").publishOn(singleThread).subscribe(t -> {
                            getPropertyGuid(v.getItemId(), s).subscribe(i -> {
                                try {
                                    if (Strings.isNullOrEmpty(i)) {
                                        createPropertyItem(v.getItemId().strip(), s.strip(), t.strip());
                                    } else {
                                        updatePropertyItem(t, i);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            });
                        });
                    });
                } else {
                    getPropertyName("Pays associé à la personne", "property").publishOn(singleThread).subscribe(s -> {
                        getPropertyGuid(v.getItemId(), s).subscribe(i -> {
                            try {
                                if (!Strings.isNullOrEmpty(i)) {
                                    removeProperty(i);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    });
                }

                if(!Strings.isNullOrEmpty(v.getSource())) {
                    getPropertyName("Source", "property").publishOn(singleThread).subscribe(s -> {
                        getPropertyGuid(v.getItemId(), s).subscribe(t -> {
                            try {
                                if(Strings.isNullOrEmpty(t)) {
                                    createProperty(v.getSource().strip(), v.getItemId().strip(), s.strip());
                                } else {
                                    updateProperty(v.getSource().strip(), t);
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    });
                }

            }

            return v;

        })
        .onErrorResume(v -> Mono.empty());
    }

    @Override
    public Flux<WikibaseLangues> findAllLangues() {

        WebClient webClient = webClientBuilder.baseUrl(urlsSparql).build();
        String queryLangues = "SELECT ?item ?itemLabel\n" +
                "WHERE \n" +
                "{\n" +
                "  ?item wdt:P1 wd:Q6.\n" +
                "  ?item wdt:P13 ?b.\n" +
                "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"[AUTO_LANGUAGE],fr\". }\n" +
                "}";

        List<WikibaseLangues> wikibaseLanguesList = new ArrayList<>();
        return webClient.get()
                .uri(builder -> builder
                        .path("/proxy/wdqs/bigdata/namespace/wdq/sparql")
                        .queryParam("query", "{queryLangues}")
                        .queryParam("format", "json")
                        .build(queryLangues)
                )
                .retrieve()
                .bodyToMono(JsonNode.class)
                .flatMap(v -> {
                    for (JsonNode j: v.findValue("bindings")) {
                        WikibaseLangues wikibaseLangues = new WikibaseLangues();
                        wikibaseLangues.setLangueName(j.findValue("itemLabel").findValue("value").asText());
                        wikibaseLanguesList.add(wikibaseLangues);
                    }
                    return Mono.just(wikibaseLanguesList);
                })
                .flatMapMany(Flux::fromIterable);
    }

    @Override
    public Flux<WikibaseCountries> findAllCountries() {

        WebClient webClient = webClientBuilder.baseUrl(urlsSparql).build();
        String queryCountries = "SELECT ?item ?itemLabel\n" +
                "WHERE \n" +
                "{\n" +
                "  ?item wdt:P1 wd:Q6.\n" +
                "  ?item wdt:P14 ?b.\n" +
                "  SERVICE wikibase:label { bd:serviceParam wikibase:language \"[AUTO_LANGUAGE],fr\". }\n" +
                "}";

        List<WikibaseCountries> wikibaseCountriesList = new ArrayList<>();
        return webClient.get()
                .uri(builder -> builder
                        .path("/proxy/wdqs/bigdata/namespace/wdq/sparql")
                        .queryParam("query", "{queryCountries}")
                        .queryParam("format", "json")
                        .build(queryCountries)
                )
                .retrieve()
                .bodyToMono(JsonNode.class)
                .onErrorResume(e -> Mono.empty())
                .flatMap(v -> {
                    for (JsonNode j: v.findValue("bindings")) {
                        WikibaseCountries wikibaseCountries = new WikibaseCountries();
                        wikibaseCountries.setCountryName(j.findValue("itemLabel").findValue("value").asText());
                        wikibaseCountriesList.add(wikibaseCountries);
                    }
                    return Mono.just(wikibaseCountriesList);
                })
                .flatMapMany(Flux::fromIterable)
                .onErrorResume(e -> Flux.empty());
    }


    @Override
    public Mono<WikiDataPersonNotice> findPersonNoticeByItemId(String itemId) {

        WikiDataPersonNotice wikiDataPersonNotice = new WikiDataPersonNotice();

        return responseToAutorite.propertiesMapper(itemId)
            .doOnError(e -> log.info("Error on fetching search detail {}", e.getMessage()))
            .parallel().runOn(Schedulers.boundedElastic())
            .map(v -> {

                if (v.getPropertyName().equals("PPN")) {
                    wikiDataPersonNotice.setPpn(v.getDatavalueWikibase().getId());
                }
                if ((v.getPropertyName().equals("Nom"))){
                    wikiDataPersonNotice.setFirstName(v.getDatavalueWikibase().getId());
                    wikiDataPersonNotice.setInstantOf("Personne - RDA");
                    wikiDataPersonNotice.setStatus("OK");
                    wikiDataPersonNotice.setItemId(itemId);
                }
                if ((v.getPropertyName().equals("Prénom"))){
                    wikiDataPersonNotice.setLastName(v.getDatavalueWikibase().getId());
                }
                if ((v.getPropertyName().equals("Source"))){
                    wikiDataPersonNotice.setSource(v.getDatavalueWikibase().getId());
                }

                // Corresponde au language de la notice
                if ((v.getPropertyName().equals("Libellé ISO 639-2"))){
                    getPropertyName("Libellé ISO 639-2", "property").flatMap(s ->
                        wikiDataService.findItemByName("haswbstatement:" + s + "=" + v.getDatavalueWikibase().getId())
                            .sequential()
                            .onErrorResume(e -> Flux.empty())
                            .next()
                            .flatMap(t -> Mono.just(t.getSnippet()))
                            .doOnNext(wikiDataPersonNotice::setLangue)
                            .onErrorResume(t -> Mono.empty())
                    )
                    .block();
                }

                // Corresponde au pays de la notice
                if ((v.getPropertyName().equals("Libéllé ISO 3166-1"))){
                    getPropertyName("Libéllé ISO 3166-1", "property").flatMap(s ->
                        wikiDataService.findItemByName("haswbstatement:"+ s +"="+v.getDatavalueWikibase().getId())
                            .sequential()
                            .onErrorResume(e -> Flux.empty())
                            .next()
                            .flatMap(t -> Mono.just(t.getSnippet()))
                            .doOnSuccess(wikiDataPersonNotice::setCountry)
                            .onErrorResume(t -> Mono.empty())
                    )
                    .block();
                }


                if ((v.getPropertyName().equals("Date de naissance"))){
                    if(v.getDatavalueWikibase().getId().length() > 4) {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyMMdd", Locale.ENGLISH);
                        LocalDate dateTime = LocalDate.parse(v.getDatavalueWikibase().getId(), formatter);
                        wikiDataPersonNotice.setDateBirth(dateTime.toString());
                    } else {
                        if(v.getDatavalueWikibase().getTimePrecision() == 8) {
                            if (v.getDatavalueWikibase().getId().contains("000")) {
                                wikiDataPersonNotice.setDateBirth(replaceLast(v.getDatavalueWikibase().getId(), "000", "XXX"));
                            } else if(v.getDatavalueWikibase().getId().contains("00")) {
                                wikiDataPersonNotice.setDateBirth(replaceLast(v.getDatavalueWikibase().getId(), "00", "XX"));
                            } else if(v.getDatavalueWikibase().getId().contains("0")) {
                                wikiDataPersonNotice.setDateBirth(replaceLast(v.getDatavalueWikibase().getId(), "0", "X"));
                            }
                        } else {
                            wikiDataPersonNotice.setDateBirth(v.getDatavalueWikibase().getId());
                        }

                    }

                }
                if ((v.getPropertyName().equals("Date de décès"))){
                    if(v.getDatavalueWikibase().getId().length() > 4) {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyMMdd", Locale.ENGLISH);
                        LocalDate dateTime = LocalDate.parse(v.getDatavalueWikibase().getId(), formatter);
                        wikiDataPersonNotice.setDateDead(dateTime.toString());
                    } else {
                        if(v.getDatavalueWikibase().getTimePrecision() == 8) {
                            if (v.getDatavalueWikibase().getId().contains("000")) {
                                wikiDataPersonNotice.setDateDead(replaceLast(v.getDatavalueWikibase().getId(), "000", "XXX"));
                            } else if(v.getDatavalueWikibase().getId().contains("00")) {
                                wikiDataPersonNotice.setDateDead(replaceLast(v.getDatavalueWikibase().getId(), "00", "XX"));
                            } else if(v.getDatavalueWikibase().getId().contains("0")) {
                                wikiDataPersonNotice.setDateDead(replaceLast(v.getDatavalueWikibase().getId(), "0", "X"));
                            }
                        } else {
                            wikiDataPersonNotice.setDateDead(v.getDatavalueWikibase().getId());
                        }
                    }
                }

                return wikiDataPersonNotice;

            })
            .flatMap(v -> {
                if (Strings.isNullOrEmpty(v.getStatus())) {
                    return Mono.empty();
                }
                return Mono.just(v);
            })
            .sequential()
            .last()
            .onErrorResume(e -> Mono.empty());
}

    private Mono<String> getPropertyName(String name, String type) {

        String url = null;
        switch (type) {
            case "property": url = this.propertySearch+name;
            break;
            case "item": url = this.itemSearch+name;
            break;
        }

        assert url != null;
        WebClient webClient = webClientBuilder.baseUrl(url).build();

        return webClient.get().retrieve()
            .onStatus(HttpStatus::is4xxClientError, response -> Mono.empty())
            .bodyToMono(JsonNode.class)
            .flatMap(v -> {
                if(v.findValue("success").asText().equals("1") && v.findValue("id") != null) {
                    return Mono.just(v.findValue("id").asText());
                } else if (v.findValue("success").asText().equals("1") && v.findValue("id") == null) {
                    log.info("Probleme indexation dans Wikibase pour l'item avec le nom "+ name);
                    return Mono.empty();
                } else {
                    return Mono.empty();
                }
            })
            .onErrorResume(v -> Mono.empty());
    }

    private Mono<String> getPropertyGuid(String itemId, String propertyId) {
        String url = propertyDetail + itemId;
        WebClient webClient = webClientBuilder.baseUrl(url).build();

        return webClient.get().uri(uriBuilder -> uriBuilder
            .queryParam("property", propertyId)
            .build()
            )
            .retrieve()
            .onStatus(HttpStatus::is4xxClientError, response -> Mono.empty())
            .bodyToMono(JsonNode.class)
            .flatMap(v -> Mono.just(v.findValue(propertyId).get(0).get("id").asText()))
            .onErrorResume(v -> Mono.just(""));
    }

    private void createProperty(String value, String itemName, String propertyName) throws Exception {

        Map<String, String> params = new LinkedHashMap<>();
        String token = getCsrfToken(urlFneApi);

        params.put("action", "wbcreateclaim");
        params.put("format", "json");
        params.put("entity", itemName);
        params.put("snaktype", "value");
        params.put("property", propertyName);
        params.put("value", "\"" + value + "\"");
        params.put("token", token);

        JsonNode json = oAuthHttp.httpOAuthPost(urlFneApi, params);
        System.out.println("Réponse de WIKIBASE  ===> " + json.toString());

    }

    private void createPropertyTime(String time, String itemId, String propertyId) {
        try {
            String date = null;
            DataTime dataTime;
            if (time.length() > 4 ) {
                date = "+"+time+"T00:00:00Z";
                dataTime = new DataTime(date, 11);
            } else {
                if (time.contains("X")) {
                    time = time.replaceAll("X", "0");
                    date = "+"+time+"-00-00T00:00:00Z";
                    dataTime = new DataTime(date, 8);
                } else {
                    date = "+"+time+"-00-00T00:00:00Z";
                    dataTime = new DataTime(date, 9);
                }
            }
//            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("+yyyy-MM-dd'T'HH:mm:ss'Z'");
//            LocalDateTime now = LocalDateTime.parse(date);


            ObjectMapper objectMapper = new ObjectMapper();

            String jsonString = objectMapper.writeValueAsString(dataTime);

            Map<String, String> params = new LinkedHashMap<>();
            String token = getCsrfToken(urlFneApi);

            params.put("action", "wbcreateclaim");
            params.put("format", "json");
            params.put("entity", itemId);
            params.put("snaktype", "value");
            params.put("property", propertyId);
            params.put("value", jsonString);
            params.put("token", token);

            JsonNode json = oAuthHttp.httpOAuthPost(urlFneApi, params);
            System.out.println("Réponse de WIKIBASE  ===> " + json.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createPropertyItem(String itemId, String propertyId, String numericId) {
        try {
            Map<String, String> params = new LinkedHashMap<>();
            String token = getCsrfToken(urlFneApi);

            params.put("action", "wbcreateclaim");
            params.put("format", "json");
            params.put("entity", itemId);
            params.put("snaktype", "value");
            params.put("property", propertyId);
            params.put("value", "{\"entity-type\":\"item\",\"numeric-id\":"+numericId.substring(1)+"}");
            params.put("token", token);

            JsonNode json = oAuthHttp.httpOAuthPost(urlFneApi, params);
            System.out.println("Réponse de WIKIBASE  ===> " + json.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateProperty(String value, String itemGUID) throws Exception {

        Map<String, String> params = new LinkedHashMap<>();
        String token = getCsrfToken(urlFneApi);

        params.put("action", "wbsetclaimvalue");
        params.put("format", "json");
        params.put("claim", itemGUID);
        params.put("snaktype", "value");
        params.put("value", "\"" + value + "\"");
        params.put("token", token);

        JsonNode json = oAuthHttp.httpOAuthPost(urlFneApi, params);
        System.out.println("Réponse de WIKIBASE  ===> " + json.toString());

    }

    private void updatePropertyTime(String time, String itemGUID) {
        try {
            String date = null;
            DataTime dataTime;
            if (time.length() > 4 ) {
                date = "+"+time+"T00:00:00Z";
                dataTime = new DataTime(date, 11);
            } else {
                if (time.contains("X")) {
                    time = time.replaceAll("X", "0");
                    date = "+"+time+"-00-00T00:00:00Z";
                    dataTime = new DataTime(date, 8);
                } else {
                    date = "+"+time+"-00-00T00:00:00Z";
                    dataTime = new DataTime(date, 9);
                }
            }

            ObjectMapper objectMapper = new ObjectMapper();

            String jsonString = objectMapper.writeValueAsString(dataTime);

            Map<String, String> params = new LinkedHashMap<>();
            String token = getCsrfToken(urlFneApi);

            params.put("action", "wbsetclaimvalue");
            params.put("format", "json");
            params.put("claim", itemGUID);
            params.put("snaktype", "value");
            params.put("value", jsonString);
            params.put("token", token);

            JsonNode json = oAuthHttp.httpOAuthPost(urlFneApi, params);
            System.out.println("Réponse de WIKIBASE  ===> " + json.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updatePropertyItem(String numericId, String itemGUID) {
        try {
            Map<String, String> params = new LinkedHashMap<>();
            String token = getCsrfToken(urlFneApi);

            params.put("action", "wbsetclaimvalue");
            params.put("format", "json");
            params.put("claim", itemGUID);
            params.put("snaktype", "value");
            params.put("value", "{\"entity-type\":\"item\",\"numeric-id\":"+numericId.substring(1)+"}");
            params.put("token", token);

            JsonNode json = oAuthHttp.httpOAuthPost(urlFneApi, params);
            System.out.println("Réponse de WIKIBASE  ===> " + json.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void removeProperty(String itemGUID) {
        try {
            Map<String, String> params = new LinkedHashMap<>();
            String token = getCsrfToken(urlFneApi);

            params.put("action", "wbremoveclaims");
            params.put("format", "json");
            params.put("claim", itemGUID);
            params.put("token", token);

            JsonNode json = oAuthHttp.httpOAuthPost(urlFneApi, params);
            System.out.println("Réponse de WIKIBASE  ===> " + json.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String titlteConvert(WikiDataPersonNotice wikibaseItem) {

        StringBuilder title = new StringBuilder();
        String dateBirthWithX = null;
        String dateDeadWithX = null;

        title.append(wikibaseItem.getFirstName().strip());
        title.append(", ");
        title.append(wikibaseItem.getLastName().strip());
        if ( !Strings.isNullOrEmpty(wikibaseItem.getDateBirth())) {
            title.append(" (");
            if(wikibaseItem.getDateBirth().contains("X")) {
                dateBirthWithX = wikibaseItem.getDateBirth().replaceAll("X", ".");
                title.append(dateBirthWithX.strip(),0,4);
            } else {
                title.append(wikibaseItem.getDateBirth().strip(),0,4);
            }

            if ( !Strings.isNullOrEmpty(wikibaseItem.getDateDead()) ) {
                if(wikibaseItem.getDateDead().contains("X")) {
                    dateDeadWithX = wikibaseItem.getDateDead().replaceAll("X", ".");
                    title.append("-");
                    title.append(dateDeadWithX.strip(),0,4);
                } else {
                    title.append("-");
                    title.append(wikibaseItem.getDateDead().strip(),0,4);
                }

            } else {
                title.append("-...");
            }
            title.append(")");
        }

        return title.toString();
    }

    private static String replaceLast(String text, String regex, String replacement) {
        return text.replaceFirst("(?s)"+regex+"(?!.*?"+regex+")", replacement);
    }
}
