package fr.fne.web.domain.handler;

import fr.fne.services.domain.WikiDataService;
import fr.fne.services.domain.entities.WikiDataSearchItem;
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
public class WikiDataSearchHandler {

    private final WikiDataService wikiDataService;

    @NonNull
    public Mono<ServerResponse> searchItemByName(ServerRequest serverRequest) {
        String itemName = serverRequest.pathVariable("term");
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(wikiDataService.findItemByName(itemName), WikiDataSearchItem.class);
    }
}
