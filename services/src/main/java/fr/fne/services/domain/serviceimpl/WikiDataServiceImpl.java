package fr.fne.services.domain.serviceimpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.fne.core.entities.resApiWikibase.DataTime;
import fr.fne.core.entities.resApiWikibase.PropertyWikibaseValuefr;
import fr.fne.core.utils.OAuthHttp;
import fr.fne.services.domain.WikibaseDataService;
import fr.fne.services.event.entities.WikibaseItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cett classe contient toutes les actions avec Wikibase,
 * elle utilise les méthodes de la classe OAuthHttp
 */

@RequiredArgsConstructor
@Slf4j
@Service
public class WikiDataServiceImpl implements WikibaseDataService {

    @Value("${wikibase.urls.fne}")
    private String urlFneApi;
    @Value("${wikibase.urls.property-search}")
    private String propertySearch;
    @Value("${wikibase.urls.item-search}")
    private String itemSearch;


    private final WebClient.Builder webClientBuilder;
    private final OAuthHttp oAuthHttp;

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
    public Mono<WikibaseItem> save(WikibaseItem wikibaseItem) throws Exception {

        Map<String, String> params = new LinkedHashMap<>();
        String token = getCsrfToken(urlFneApi);
        ObjectMapper objectMapper = new ObjectMapper();

        Mono<WikibaseItem> wikibaseItemMono = Mono.just(wikibaseItem);

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

                if(v.getInstantOf() != null) {
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

                if(v.getFirstName() != null) {
                    getPropertyName("Nom", "property").publishOn(singleThread).subscribe(s -> {
                        try {
                            createProperty(wikibaseItem.getFirstName().strip(), wikibaseItem.getItemId().strip(), s.strip());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    });
                }

                if(v.getLastName() != null) {
                    getPropertyName("Prénom", "property").publishOn(singleThread).subscribe(s -> {
                        try {
                            createProperty(wikibaseItem.getLastName().strip(), wikibaseItem.getItemId().strip(), s.strip());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
                if(v.getDateBirth() != null) {
                    getPropertyName("Date de naissance", "property").publishOn(singleThread).subscribe(s -> {
                        try {
                            createPropertyTime(v.getDateBirth().strip(), v.getItemId().strip(), s.strip());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
                if(v.getDateDead() != null) {
                    getPropertyName("Date de décès", "property").publishOn(singleThread).subscribe(s -> {
                        try {
                            createPropertyTime(v.getDateDead().strip(), v.getItemId().strip(), s.strip());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }

                if(v.getLangue() != null) {
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

                if(v.getCountry() != null) {
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

                if(v.getSource() != null) {
                    getPropertyName("Source", "property").publishOn(singleThread).subscribe(s -> {
                        try {
                            createProperty(wikibaseItem.getSource().strip(), wikibaseItem.getItemId().strip(), s.strip());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    });
                }

            }

            return v;
        });


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

        return webClient.get().retrieve().bodyToMono(JsonNode.class)
                        .flatMap(v -> {
                            if(v.findValue("success").asText().equals("1") && v.findValue("id") != null) {
                                return Mono.just(v.findValue("id").asText());
                            } else if (v.findValue("success").asText().equals("1") && v.findValue("id") == null) {
                                log.info("Probleme indexation dans Wikibase pour l'item avec le nom "+ name);
                                return Mono.empty();
                            } else {
                                return Mono.empty();
                            }
                        });
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
                date = "+"+time+"-00-00T00:00:00Z";
                dataTime = new DataTime(date, 9);
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

    private String titlteConvert(WikibaseItem wikibaseItem) {

        StringBuilder title = new StringBuilder();

        title.append(wikibaseItem.getFirstName().strip());
        title.append(", ");
        title.append(wikibaseItem.getLastName().strip());
        title.append(" (");
        title.append(wikibaseItem.getDateBirth().strip(),0,4);
        if (StringUtils.hasText( wikibaseItem.getDateDead().strip() ) ) {
            title.append("-");
            title.append(wikibaseItem.getDateDead().strip(),0,4);

        } else {
            title.append("-...");
        }
        title.append(")");

        return title.toString();
    }
}
