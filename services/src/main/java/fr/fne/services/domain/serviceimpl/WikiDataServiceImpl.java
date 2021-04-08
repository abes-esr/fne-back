package fr.fne.services.domain.serviceimpl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.fne.core.entities.resApiWikibase.MainsnakWikibase;
import fr.fne.core.entities.resApiWikibase.PropertyWikibaseValuefr;
import fr.fne.services.domain.WikiDataService;
import fr.fne.services.domain.entities.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ParallelFlux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;

/**
 * Cett classe contient toutes les actions avec Wikibase,
 * elle utilise les méthodes de la classe OAuthHttp
 */

@RequiredArgsConstructor
@Slf4j
@Service
public class WikiDataServiceImpl implements WikiDataService {

    @Value("${wikibase.urls.cirrus-search}")
    private String itemSearch;
    @Value("${wikibase.urls.item-description}")
    private String itemDescriptionSearch;

    private final WebClient.Builder webClientBuilder;


    @Override
    public ParallelFlux<WikiDataSearchItem> findItemByNameWithDescription(String itemName) {
        WebClient webClient = webClientBuilder.baseUrl(itemSearch+itemName+"*+hasdescription:fr").build();
        return requeteFindItemByName(webClient);
    }

    @Override
    public ParallelFlux<WikiDataSearchItem> findItemByName(String itemName) {
        WebClient webClient = webClientBuilder.baseUrl(itemSearch+itemName).build();
        return requeteFindItemByName(webClient);
    }

    private ParallelFlux<WikiDataSearchItem> requeteFindItemByName(WebClient webClient) {
        return webClient.get().retrieve()
                .onStatus(HttpStatus::is4xxClientError, response -> Mono.empty())
                .bodyToMono(WikiDataQuerySearch.class)
                .onErrorResume(e -> Mono.empty())
                .doOnError(e -> log.info("Error on fetching search detail {}", e.getMessage()))
                .flatMap(v -> Mono.just(v.getQuery().getWikiDataSearchItemList()))
                .flatMapMany(Flux::fromIterable)
                .parallel().runOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<PropertyWikibaseValuefr> getItemDescriptionById(String itemId) {
        WebClient webClient = webClientBuilder.baseUrl(itemDescriptionSearch+itemId).build();
        ObjectMapper mapper = new ObjectMapper();
        return webClient.get().retrieve()
                .onStatus(HttpStatus::is4xxClientError, response -> Mono.empty())
                .bodyToMono(JsonNode.class)
                .map(v -> v.findValue("fr"))
                .map(s -> {
                    try {
                        return mapper.readValue(s.traverse(), new TypeReference<PropertyWikibaseValuefr>() {
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                        return new PropertyWikibaseValuefr();
                    }
                })
                .onErrorResume(e -> Mono.empty());

    }

}
