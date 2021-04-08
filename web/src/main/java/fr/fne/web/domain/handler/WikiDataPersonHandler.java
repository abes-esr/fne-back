package fr.fne.web.domain.handler;

import com.google.common.base.Strings;
import fr.fne.services.domain.WikiDataServicePersonNotice;
import fr.fne.services.domain.entities.WikibaseCountries;
import fr.fne.services.domain.entities.WikiDataPersonNotice;
import fr.fne.services.domain.entities.WikibaseLangues;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class WikiDataPersonHandler {

    private final WikiDataServicePersonNotice wikiDataServicePersonNotice;

    @NonNull
    public Mono<ServerResponse> createWikiBaseItem(ServerRequest serverRequest) {

        Mono<WikiDataPersonNotice> wikibaseItemMono = serverRequest.bodyToMono(WikiDataPersonNotice.class);

        return wikibaseItemMono.log()
                .flatMap(item -> {
                    try {
                        return (
                                Strings.isNullOrEmpty(item.getDateBirth())
                                || Strings.isNullOrEmpty(item.getFirstName())
                                || Strings.isNullOrEmpty(item.getLastName())
                                || Strings.isNullOrEmpty(item.getInstantOf())
                                )
                                ? ServerResponse.badRequest().build()
                                : ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(wikiDataServicePersonNotice.save(item), WikiDataPersonNotice.class)
                            .switchIfEmpty(ServerResponse.notFound().build())
                            .onErrorResume(e -> ServerResponse.badRequest().build() );
                    } catch (Exception e) {
                        e.printStackTrace();
                        return Mono.empty();
                    }
                }
        );

    }

    @NonNull
    public Mono<ServerResponse> getPersonByItemId(ServerRequest serverRequest) {

        String itemName = serverRequest.pathVariable("term");
        return wikiDataServicePersonNotice.findPersonNoticeByItemId(itemName).flatMap(v ->
            ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(wikiDataServicePersonNotice.findPersonNoticeByItemId(itemName), WikiDataPersonNotice.class)

        )
        .switchIfEmpty(ServerResponse.notFound().build());
    }

    @NonNull
    public Mono<ServerResponse> getPersonLangues(ServerRequest serverRequest) {

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(wikiDataServicePersonNotice.findAllLangues(), WikibaseLangues.class);
    }

    @NonNull
    public Mono<ServerResponse> getPersonCountries(ServerRequest serverRequest) {

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(wikiDataServicePersonNotice.findAllCountries(), WikibaseCountries.class);
    }

    @NonNull
    public Mono<ServerResponse> updateWikiBaseItem(ServerRequest serverRequest) {

        Mono<WikiDataPersonNotice> wikibaseItemMono = serverRequest.bodyToMono(WikiDataPersonNotice.class);

        return wikibaseItemMono.log()
            .flatMap(item -> {

                try {
                    return (
                        Strings.isNullOrEmpty(item.getFirstName())
                        || Strings.isNullOrEmpty(item.getLastName())
                        )
                        ? ServerResponse.badRequest().build()
                        : ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(wikiDataServicePersonNotice.update(item), WikiDataPersonNotice.class)
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
