package fr.fne.services.domain.serviceimpl;

import fr.fne.services.domain.WikiDataService;
import fr.fne.services.domain.entities.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.ParallelFlux;
import reactor.core.scheduler.Schedulers;

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

    private final WebClient.Builder webClientBuilder;


    @Override
    public ParallelFlux<WikiDataSearchItem> findItemByName(String itemName) {
        WebClient webClient = webClientBuilder.baseUrl(itemSearch+itemName+"*+hasdescription:fr").build();

        return webClient.get().retrieve()
                .bodyToMono(WikiDataQuerySearch.class)
                .flatMap(v -> Mono.just(v.getQuery().getWikiDataSearchItemList()))
                .flatMapMany(Flux::fromIterable)
                .parallel().runOn(Schedulers.boundedElastic());
    }

}
