package fr.fne.web.domain.handler;

import fr.fne.services.domain.WikibaseDataService;
import fr.fne.services.event.entities.WikibaseItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class WikibasePersonHandler {

    private final WikibaseDataService wikibaseDataService;

    @NonNull
    public Mono<ServerResponse> createWikiBaseItem(ServerRequest serverRequest) {

        Mono<WikibaseItem> wikibaseItemMono = serverRequest.bodyToMono(WikibaseItem.class);

        return wikibaseItemMono.log()
                .flatMap(item -> {
                    try {
                        return ( item.getFirstName() == null || item.getLastName() == null || item.getDateBirth() == null || item.getInstantOf() == null )
                                ? ServerResponse.badRequest().build()
                                : ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(wikibaseDataService.save(item), WikibaseItem.class)
                            .switchIfEmpty(ServerResponse.notFound().build())
                            .onErrorResume(e -> ServerResponse.badRequest().build() );
                    } catch (Exception e) {
                        e.printStackTrace();
                        return Mono.empty();
                    }
                }
        );

    }
}
