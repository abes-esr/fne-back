package fr.fne.core.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.fne.core.entities.Recentchange;
import fr.fne.core.entities.resApiWikibase.ResWikibase;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Convertir tous les items qui se trouvent dans la page "recent change" de Wikibase avec un temps précis
 * Ici nous prenons que la dernière version de chaque item dans la liste afin d'éviter les doublons
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class GetRecentChangeListToObject {

    @Setter
    private String timeEnd;
    @Value("${wikibase.urls.recent-item}")
    private String uriRecentItem;

    private final WebClient.Builder webClientBuilder;


    // Spring classic
    public List<Recentchange> toObjects() {
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper objectMapper = new ObjectMapper();
        List<Recentchange> recentchanges = new ArrayList<>();

        String url = this.uriRecentItem + this.timeEnd;
        try {
            /*
             * Aller  dans la page recent change de wikibase
             * Return 1 string avec la liste de tous les items Q
             */
            ResponseEntity<String> response
                    = restTemplate.getForEntity(url, String.class);
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            jsonNode = jsonNode.findValue("recentchanges");
            /*
            Ici on va boucler sur la liste et convertir en objet java (recentchange)
             */
            for (JsonNode j : jsonNode) {
                // Convertir en objet java à chaque tour de boucle
                if (j.findValue("title").asText().contains(":")) {
                    Recentchange recentchange = new Recentchange(j.findValue("title").asText().split(":")[1], j.findValue("timestamp").asText());
                    if (recentchanges.stream().noneMatch(t -> t.getTitle().equals(recentchange.getTitle())) &&
                            !recentchange.getTitle().startsWith("P")) {
                        recentchanges.add(recentchange); // chaque tour de boucle on rajoute l'objet dans la liste d'objet
                    }
                }
            }
        } catch (JsonProcessingException e) {
            log.debug(e.getMessage());
        }
        // Retourne la liste avec tous les objets java (recentchange)
        return recentchanges;
    }

    // Spring Reactor
    public Flux<Recentchange> toObjectReactive() {

        String url = this.uriRecentItem + this.timeEnd;
        WebClient webClient = webClientBuilder.baseUrl(url).build();
        ObjectMapper objectMapper = new ObjectMapper();
        return webClient.get()
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(v -> v.findValue("recentchanges"))
                .map(s -> {
                    try {
                        return objectMapper.readValue(s.traverse(), new TypeReference<List<ResWikibase>>() {
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                        return new ArrayList<ResWikibase>();
                    }
                })
                .flatMapMany(Flux::fromIterable)
                .parallel()
                .runOn(Schedulers.boundedElastic())
                .filter(v -> v.getTitle().startsWith("Item") )
                .map(v -> new Recentchange(v.getTitle().split(":")[1], v.getTimestamp()) )
                .sequential();

    }
}
