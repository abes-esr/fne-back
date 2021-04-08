package fr.fne.services.domain;

import fr.fne.services.domain.entities.WikibaseCountries;
import fr.fne.services.domain.entities.WikiDataPersonNotice;
import fr.fne.services.domain.entities.WikibaseLangues;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface WikiDataServicePersonNotice {

    void insertPpn(String urlWikiBase, String csrftoken, String id, String ppn);

    String getCsrfToken(String urlWikibase);
    Mono<WikiDataPersonNotice> save(WikiDataPersonNotice wikibaseItem) throws Exception;
    Mono<WikiDataPersonNotice> update(WikiDataPersonNotice wikibaseItem) throws Exception;
    Flux<WikibaseLangues> findAllLangues();
    Flux<WikibaseCountries> findAllCountries();
    Mono<WikiDataPersonNotice> findPersonNoticeByItemId(String ItemId);
}
